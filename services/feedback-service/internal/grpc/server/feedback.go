package server

import (
	"context"
	"fmt"
	"io"
	"time"

	pb "github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/api"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/service"
	"github.com/google/uuid"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// feedbackServer implements the FeedbackService gRPC server
type feedbackServer struct {
	pb.UnimplementedFeedbackServiceServer
	feedbackService *service.FeedbackService
	commentService  *service.CommentService
}

// RegisterFeedbackServer registers the feedback server with gRPC
func RegisterFeedbackServer(s *grpc.Server, feedbackService *service.FeedbackService, commentService *service.CommentService) {
	server := &feedbackServer{
		feedbackService: feedbackService,
		commentService:  commentService,
	}
	pb.RegisterFeedbackServiceServer(s, server)
}

// CreateFeedback creates a new feedback entry with streaming content
func (s *feedbackServer) CreateFeedback(stream pb.FeedbackService_CreateFeedbackServer) error {
	// Receive first message which should contain metadata
	req, err := stream.Recv()
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to receive create request: %v", err))
	}

	metadata := req.GetMetadata()
	if metadata == nil {
		return status.Error(codes.InvalidArgument, "first message must contain metadata")
	}
	// Validate metadata
	if metadata.UserId <= 0 {
		return status.Error(codes.InvalidArgument, "user_id is required")
	}
	if metadata.StudentId <= 0 {
		return status.Error(codes.InvalidArgument, "student_id is required")
	}
	if metadata.LabId <= 0 {
		return status.Error(codes.InvalidArgument, "lab_id is required")
	}
	if metadata.Title == "" {
		return status.Error(codes.InvalidArgument, "title is required")
	}
	if metadata.TotalSize <= 0 {
		return status.Error(codes.InvalidArgument, "total_size must be positive")
	}

	// First create the feedback entry
	createdFeedback, err := s.feedbackService.CreateFeedback(
		stream.Context(),
		metadata.UserId,
		metadata.StudentId,
		metadata.LabId,
		metadata.Title,
	)
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to create feedback: %v", err))
	}

	// Create a pipe to stream data to the upload function
	pipeReader, pipeWriter := io.Pipe()

	// Error channel for goroutine communication
	uploadErrCh := make(chan error, 1)

	// Start upload in a goroutine
	go func() {
		defer pipeReader.Close()

		// Upload the content
		err := s.feedbackService.UploadFeedbackContent(
			stream.Context(),
			createdFeedback.ID,
			pipeReader,
			metadata.TotalSize,
		)
		uploadErrCh <- err
	}()

	// Stream chunks to the pipe
	var totalReceived int64
	streamError := func() error {
		defer pipeWriter.Close() // Ensure pipe writer is closed when streaming completes

		for {
			// Check if context is canceled
			select {
			case <-stream.Context().Done():
				return stream.Context().Err()
			default:
			}

			req, err := stream.Recv()
			if err == io.EOF {
				// End of stream reached
				break
			}
			if err != nil {
				return fmt.Errorf("failed to receive chunk: %v", err)
			}

			chunk := req.GetChunk()
			if chunk == nil {
				continue
			}

			totalReceived += int64(len(chunk))
			if totalReceived > metadata.TotalSize {
				return fmt.Errorf("received more data than expected: %d > %d", totalReceived, metadata.TotalSize)
			}

			// Write chunk to pipe
			if _, err := pipeWriter.Write(chunk); err != nil {
				return fmt.Errorf("failed to write chunk to upload stream: %v", err)
			}
		}

		return nil
	}()

	// Handle streaming error
	if streamError != nil {
		pipeWriter.CloseWithError(streamError)
		// Clean up the created feedback entry on streaming error
		_ = s.feedbackService.DeleteFeedback(stream.Context(), createdFeedback.ID)

		if streamError == stream.Context().Err() {
			return status.Error(codes.Canceled, "stream canceled")
		}
		return status.Error(codes.Internal, streamError.Error())
	}

	// Verify we received all data
	if totalReceived != metadata.TotalSize {
		pipeWriter.CloseWithError(fmt.Errorf("data size mismatch"))
		_ = s.feedbackService.DeleteFeedback(stream.Context(), createdFeedback.ID)
		return status.Error(codes.InvalidArgument,
			fmt.Sprintf("received data size (%d) doesn't match declared size (%d)",
				totalReceived, metadata.TotalSize))
	}

	// Wait for upload to complete
	select {
	case uploadErr := <-uploadErrCh:
		if uploadErr != nil {
			// Clean up the created feedback entry on upload error
			_ = s.feedbackService.DeleteFeedback(stream.Context(), createdFeedback.ID)
			return status.Error(codes.Internal, fmt.Sprintf("failed to upload feedback content: %v", uploadErr))
		}
	case <-stream.Context().Done():
		// Clean up on context cancellation
		_ = s.feedbackService.DeleteFeedback(stream.Context(), createdFeedback.ID)
		return status.Error(codes.Canceled, "stream canceled while waiting for upload")
	case <-time.After(30 * time.Second):
		// Clean up on timeout
		_ = s.feedbackService.DeleteFeedback(stream.Context(), createdFeedback.ID)
		return status.Error(codes.DeadlineExceeded, "upload timeout")
	}
	// Success - send the feedback response
	feedback := &pb.FeedbackFile{
		Id:        createdFeedback.ID.String(),
		UserId:    createdFeedback.UserID,
		StudentId: createdFeedback.StudentID,
		LabId:     createdFeedback.LabID,
		Title:     createdFeedback.Title,
		CreatedAt: timestamppb.New(createdFeedback.CreatedAt),
		UpdatedAt: timestamppb.New(createdFeedback.UpdatedAt),
	}

	return stream.SendAndClose(feedback)
}

// GetFeedback retrieves a feedback by ID with streaming content
func (s *feedbackServer) GetFeedback(req *pb.GetFeedbackRequest, stream pb.FeedbackService_GetFeedbackServer) error {
	// Validate request
	if req.Id == "" {
		return status.Error(codes.InvalidArgument, "id is required")
	}

	// Parse UUID
	id, err := uuid.Parse(req.Id)
	if err != nil {
		return status.Error(codes.InvalidArgument, "invalid id format")
	}

	// Get feedback info
	feedback, err := s.feedbackService.GetFeedback(stream.Context(), id)
	if err != nil {
		if err.Error() == "feedback not found" {
			return status.Error(codes.NotFound, "feedback not found")
		}
		return status.Error(codes.Internal, fmt.Sprintf("failed to get feedback: %v", err))
	}
	// Send feedback info first
	err = stream.Send(&pb.GetFeedbackResponse{
		Data: &pb.GetFeedbackResponse_Info{
			Info: &pb.FeedbackFile{
				Id:        feedback.ID.String(),
				UserId:    feedback.UserID,
				StudentId: feedback.StudentID,
				LabId:     feedback.LabID,
				Title:     feedback.Title,
				CreatedAt: timestamppb.New(feedback.CreatedAt),
				UpdatedAt: timestamppb.New(feedback.UpdatedAt),
			},
		},
	})
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to send feedback info: %v", err))
	} // Get and stream feedback content
	reader, _, err := s.feedbackService.DownloadFeedbackContent(stream.Context(), id)
	if err != nil {
		if err.Error() == "feedback content not found" {
			// If there's no content file, just return the info
			return nil
		}
		return status.Error(codes.Internal, fmt.Sprintf("failed to get feedback content: %v", err))
	}
	defer reader.Close()

	// Stream content in chunks
	buffer := make([]byte, 32*1024) // 32KB chunks
	for {
		n, err := reader.Read(buffer)
		if err == io.EOF {
			break
		}
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to read feedback content: %v", err))
		}

		err = stream.Send(&pb.GetFeedbackResponse{
			Data: &pb.GetFeedbackResponse_Chunk{
				Chunk: buffer[:n],
			},
		})
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to send chunk: %v", err))
		}
	}

	return nil
}

// UpdateFeedback updates a feedback entry
func (s *feedbackServer) UpdateFeedback(stream pb.FeedbackService_UpdateFeedbackServer) error {
	// Receive first message which should contain metadata
	req, err := stream.Recv()
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to receive update request: %v", err))
	}

	metadata := req.GetMetadata()
	if metadata == nil {
		return status.Error(codes.InvalidArgument, "first message must contain metadata")
	}

	// Validate metadata
	if metadata.Id == "" {
		return status.Error(codes.InvalidArgument, "id is required")
	}

	// Parse UUID
	id, err := uuid.Parse(metadata.Id)
	if err != nil {
		return status.Error(codes.InvalidArgument, "invalid id format")
	}

	// If no content is being updated, just update the title
	if metadata.TotalSize == nil || *metadata.TotalSize == 0 {
		var title *string
		if metadata.Title != nil {
			title = metadata.Title
		}
		feedback, err := s.feedbackService.UpdateFeedback(stream.Context(), id, title)
		if err != nil {
			if err.Error() == "feedback not found" {
				return status.Error(codes.NotFound, "feedback not found")
			}
			return status.Error(codes.Internal, fmt.Sprintf("failed to update feedback: %v", err))
		}
		return stream.SendAndClose(&pb.FeedbackFile{
			Id:        feedback.ID.String(),
			UserId:    feedback.UserID,
			StudentId: feedback.StudentID,
			LabId:     feedback.LabID,
			Title:     feedback.Title,
			CreatedAt: timestamppb.New(feedback.CreatedAt),
			UpdatedAt: timestamppb.New(feedback.UpdatedAt),
		})
	}
	// Create a pipe to stream data to the update function
	pipeReader, pipeWriter := io.Pipe()

	// Result channel for goroutine communication
	type result struct {
		feedback *pb.FeedbackFile
		err      error
	}
	resultCh := make(chan result, 1)

	// Start feedback update in a goroutine
	go func() {
		defer pipeReader.Close()

		// Check context before starting
		select {
		case <-stream.Context().Done():
			resultCh <- result{err: stream.Context().Err()}
			return
		default:
		}

		// First update the feedback content
		err := s.feedbackService.UpdateFeedbackContent(
			stream.Context(),
			id,
			pipeReader,
			*metadata.TotalSize,
		)
		if err != nil {
			resultCh <- result{err: err}
			return
		}

		// Then update the title if provided
		var updatedFeedback *models.Feedback
		if metadata.Title != nil {
			updatedFeedback, err = s.feedbackService.UpdateFeedback(stream.Context(), id, metadata.Title)
		} else {
			updatedFeedback, err = s.feedbackService.GetFeedback(stream.Context(), id)
		}
		if err != nil {
			resultCh <- result{err: err}
			return
		}
		// Success - send the feedback response
		feedback := &pb.FeedbackFile{
			Id:        updatedFeedback.ID.String(),
			UserId:    updatedFeedback.UserID,
			StudentId: updatedFeedback.StudentID,
			LabId:     updatedFeedback.LabID,
			Title:     updatedFeedback.Title,
			CreatedAt: timestamppb.New(updatedFeedback.CreatedAt),
			UpdatedAt: timestamppb.New(updatedFeedback.UpdatedAt),
		}
		resultCh <- result{feedback: feedback}
	}()

	// Stream chunks to the pipe
	var totalReceived int64
	streamingDone := false

	for !streamingDone {
		// Check if context is canceled
		select {
		case <-stream.Context().Done():
			pipeWriter.CloseWithError(stream.Context().Err())
			return status.Error(codes.Canceled, "stream canceled")
		default:
		}

		req, err := stream.Recv()
		if err == io.EOF {
			streamingDone = true
			break
		}
		if err != nil {
			pipeWriter.CloseWithError(err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to receive chunk: %v", err))
		}

		chunk := req.GetChunk()
		if chunk == nil {
			continue
		}

		totalReceived += int64(len(chunk))
		if totalReceived > *metadata.TotalSize {
			pipeWriter.CloseWithError(fmt.Errorf("received more data than expected"))
			return status.Error(codes.InvalidArgument, "received more data than declared in metadata")
		}

		// Write chunk to pipe with error handling
		if _, err := pipeWriter.Write(chunk); err != nil {
			if err == io.ErrClosedPipe {
				return status.Error(codes.Aborted, "upload stream closed")
			}
			pipeWriter.CloseWithError(err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to write chunk: %v", err))
		}
	}

	// Close the writer to signal EOF
	if err := pipeWriter.Close(); err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to close pipe writer: %v", err))
	}

	// Verify we received all data
	if totalReceived != *metadata.TotalSize {
		return status.Error(codes.InvalidArgument,
			fmt.Sprintf("received data size (%d) doesn't match declared size (%d)",
				totalReceived, *metadata.TotalSize))
	}

	// Wait for update to complete
	select {
	case res := <-resultCh:
		if res.err != nil {
			if res.err.Error() == "feedback not found" {
				return status.Error(codes.NotFound, "feedback not found")
			}
			return status.Error(codes.Internal, fmt.Sprintf("failed to update feedback: %v", res.err))
		}
		// Send response
		return stream.SendAndClose(res.feedback)
	case <-stream.Context().Done():
		return status.Error(codes.Canceled, "stream canceled while waiting for upload")
	case <-time.After(30 * time.Second):
		return status.Error(codes.DeadlineExceeded, "upload timeout")
	}
}

// DeleteFeedback deletes a feedback entry
func (s *feedbackServer) DeleteFeedback(ctx context.Context, req *pb.DeleteFeedbackRequest) (*pb.DeleteFeedbackResponse, error) {
	// Validate request
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "id is required")
	}

	// Parse UUID
	id, err := uuid.Parse(req.Id)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid id format")
	}

	// Delete feedback
	err = s.feedbackService.DeleteFeedback(ctx, id)
	if err != nil {
		if err.Error() == "feedback not found" {
			return nil, status.Error(codes.NotFound, "feedback not found")
		}
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete feedback: %v", err))
	}

	return &pb.DeleteFeedbackResponse{
		Success: true,
	}, nil
}

// ListUserFeedbacks lists feedbacks for a user
func (s *feedbackServer) ListUserFeedbacks(ctx context.Context, req *pb.ListUserFeedbacksRequest) (*pb.ListUserFeedbacksResponse, error) {
	// Validate request
	if req.UserId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Page <= 0 {
		req.Page = 1
	}
	if req.Limit <= 0 {
		req.Limit = 10
	}

	// List feedbacks
	var labID *int64
	if req.LabId != nil {
		labID = req.LabId
	}

	feedbacks, totalCount, err := s.feedbackService.ListUserFeedbacks(ctx, req.UserId, labID, req.Page, req.Limit)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list feedbacks: %v", err))
	} // Convert to protobuf
	pbFeedbacks := make([]*pb.FeedbackFile, len(feedbacks))
	for i, feedback := range feedbacks {
		pbFeedbacks[i] = &pb.FeedbackFile{
			Id:        feedback.ID.String(),
			UserId:    feedback.UserID,
			StudentId: feedback.StudentID,
			LabId:     feedback.LabID,
			Title:     feedback.Title,
			CreatedAt: timestamppb.New(feedback.CreatedAt),
			UpdatedAt: timestamppb.New(feedback.UpdatedAt),
		}
	}

	return &pb.ListUserFeedbacksResponse{
		Feedbacks:  pbFeedbacks,
		TotalCount: totalCount,
	}, nil
}

// ListStudentFeedbacks lists feedbacks for a student
func (s *feedbackServer) ListStudentFeedbacks(ctx context.Context, req *pb.ListStudentFeedbacksRequest) (*pb.ListStudentFeedbacksResponse, error) {
	// Validate request
	if req.StudentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}
	if req.Page <= 0 {
		req.Page = 1
	}
	if req.Limit <= 0 {
		req.Limit = 10
	}

	// List feedbacks
	var labID *int64
	if req.LabId != nil {
		labID = req.LabId
	}

	feedbacks, totalCount, err := s.feedbackService.ListStudentFeedbacks(ctx, req.StudentId, labID, req.Page, req.Limit)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list student feedbacks: %v", err))
	}

	// Convert to protobuf
	pbFeedbacks := make([]*pb.FeedbackFile, len(feedbacks))
	for i, feedback := range feedbacks {
		pbFeedbacks[i] = &pb.FeedbackFile{
			Id:        feedback.ID.String(),
			UserId:    feedback.UserID,
			StudentId: feedback.StudentID,
			LabId:     feedback.LabID,
			Title:     feedback.Title,
			CreatedAt: timestamppb.New(feedback.CreatedAt),
			UpdatedAt: timestamppb.New(feedback.UpdatedAt),
		}
	}

	return &pb.ListStudentFeedbacksResponse{
		Feedbacks:  pbFeedbacks,
		TotalCount: totalCount,
	}, nil
}

// UploadAsset handles streaming asset uploads
func (s *feedbackServer) UploadAsset(stream pb.FeedbackService_UploadAssetServer) error {
	// Receive first message which should contain metadata
	req, err := stream.Recv()
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to receive upload request: %v", err))
	}

	metadata := req.GetMetadata()
	if metadata == nil {
		return status.Error(codes.InvalidArgument, "first message must contain metadata")
	}

	// Validate metadata
	if metadata.FeedbackId == "" {
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if metadata.Filename == "" {
		return status.Error(codes.InvalidArgument, "filename is required")
	}
	if metadata.TotalSize <= 0 {
		return status.Error(codes.InvalidArgument, "total_size must be positive")
	}

	// Parse feedback ID
	feedbackID, err := uuid.Parse(metadata.FeedbackId)
	if err != nil {
		return status.Error(codes.InvalidArgument, "invalid feedback_id format")
	}
	// Create a pipe to stream data to the upload function
	pipeReader, pipeWriter := io.Pipe()

	// Start upload in a goroutine
	uploadErrCh := make(chan error, 1)
	go func() {
		defer pipeReader.Close()

		// Check context before starting
		select {
		case <-stream.Context().Done():
			uploadErrCh <- stream.Context().Err()
			return
		default:
		}

		err := s.feedbackService.UploadAsset(
			stream.Context(),
			feedbackID,
			metadata.Filename,
			metadata.ContentType,
			pipeReader,
			metadata.TotalSize,
		)
		uploadErrCh <- err
	}()

	// Stream chunks to the pipe
	var totalReceived int64
	streamingDone := false

	for !streamingDone {
		// Check if context is canceled
		select {
		case <-stream.Context().Done():
			pipeWriter.CloseWithError(stream.Context().Err())
			return status.Error(codes.Canceled, "stream canceled")
		default:
		}

		req, err := stream.Recv()
		if err == io.EOF {
			streamingDone = true
			break
		}
		if err != nil {
			pipeWriter.CloseWithError(err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to receive chunk: %v", err))
		}

		chunk := req.GetChunk()
		if chunk == nil {
			continue
		}

		totalReceived += int64(len(chunk))
		if totalReceived > metadata.TotalSize {
			pipeWriter.CloseWithError(fmt.Errorf("received more data than expected"))
			return status.Error(codes.InvalidArgument, "received more data than declared in metadata")
		}

		// Write chunk to pipe with error handling
		if _, err := pipeWriter.Write(chunk); err != nil {
			if err == io.ErrClosedPipe {
				return status.Error(codes.Aborted, "upload stream closed")
			}
			pipeWriter.CloseWithError(err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to write chunk: %v", err))
		}
	}

	// Close the writer to signal EOF
	if err := pipeWriter.Close(); err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to close pipe writer: %v", err))
	}

	// Verify we received all data
	if totalReceived != metadata.TotalSize {
		return status.Error(codes.InvalidArgument,
			fmt.Sprintf("received data size (%d) doesn't match declared size (%d)",
				totalReceived, metadata.TotalSize))
	}

	// Wait for upload to complete
	select {
	case err := <-uploadErrCh:
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to upload asset: %v", err))
		}
		// Send response
		return stream.SendAndClose(&pb.UploadAssetResponse{
			Filename: metadata.Filename,
			Size:     totalReceived,
			Success:  true,
		})
	case <-stream.Context().Done():
		return status.Error(codes.Canceled, "stream canceled while waiting for upload")
	case <-time.After(30 * time.Second):
		return status.Error(codes.DeadlineExceeded, "upload timeout")
	}
}

// DownloadAsset handles streaming asset downloads
func (s *feedbackServer) DownloadAsset(req *pb.DownloadAssetRequest, stream pb.FeedbackService_DownloadAssetServer) error {
	// Validate request
	if req.FeedbackId == "" {
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		return status.Error(codes.InvalidArgument, "filename is required")
	}

	// Parse feedback ID
	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		return status.Error(codes.InvalidArgument, "invalid feedback_id format")
	}

	// Download asset
	reader, fileInfo, err := s.feedbackService.DownloadAsset(stream.Context(), feedbackID, req.Filename)
	if err != nil {
		if err.Error() == "asset not found" {
			return status.Error(codes.NotFound, "asset not found")
		}
		return status.Error(codes.Internal, fmt.Sprintf("failed to download asset: %v", err))
	}
	defer reader.Close()

	// Send asset info first
	err = stream.Send(&pb.DownloadAssetResponse{
		Data: &pb.DownloadAssetResponse_Info{
			Info: &pb.AssetInfo{
				Filename:    fileInfo.Name,
				Size:        fileInfo.Size,
				ContentType: fileInfo.ContentType,
				UploadedAt:  timestamppb.New(time.Now()), // We don't store upload time in file info
			},
		},
	})
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to send asset info: %v", err))
	}

	// Stream file content in chunks
	buffer := make([]byte, 32*1024) // 32KB chunks
	for {
		n, err := reader.Read(buffer)
		if err == io.EOF {
			break
		}
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to read asset data: %v", err))
		}

		err = stream.Send(&pb.DownloadAssetResponse{
			Data: &pb.DownloadAssetResponse_Chunk{
				Chunk: buffer[:n],
			},
		})
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to send chunk: %v", err))
		}
	}

	return nil
}

// ListAssets lists all assets for a feedback
func (s *feedbackServer) ListAssets(ctx context.Context, req *pb.ListAssetsRequest) (*pb.ListAssetsResponse, error) {
	// Validate request
	if req.FeedbackId == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}

	// Parse feedback ID
	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid feedback_id format")
	}

	// List assets
	assets, err := s.feedbackService.ListAssets(ctx, feedbackID)
	if err != nil {
		if err.Error() == "feedback not found" {
			return nil, status.Error(codes.NotFound, "feedback not found")
		}
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list assets: %v", err))
	}

	// Convert to protobuf
	pbAssets := make([]*pb.AssetInfo, len(assets))
	for i, asset := range assets {
		pbAssets[i] = &pb.AssetInfo{
			Filename:    asset.Filename,
			Size:        asset.FileSize,
			ContentType: asset.ContentType,
			UploadedAt:  timestamppb.New(asset.CreatedAt),
		}
	}
	return &pb.ListAssetsResponse{
		Assets: pbAssets,
	}, nil
}

// CreateComment creates a new comment
func (s *feedbackServer) CreateComment(ctx context.Context, req *pb.CreateCommentRequest) (*pb.LabComment, error) {
	// Validate request
	if req.LabId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "lab_id is required")
	}
	if req.UserId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Content == "" {
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}

	// Parse parent ID if provided
	var parentID *uuid.UUID
	if req.ParentId != nil {
		parsed, err := uuid.Parse(*req.ParentId)
		if err != nil {
			return nil, status.Error(codes.InvalidArgument, "invalid parent_id format")
		}
		parentID = &parsed
	}

	// Create comment
	comment, err := s.commentService.CreateComment(ctx, req.LabId, req.UserId, parentID, req.Content)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to create comment: %v", err))
	}

	// Convert to protobuf
	pbComment := &pb.LabComment{
		Id:        comment.ID.String(),
		LabId:     comment.LabID,
		UserId:    comment.UserID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
	}
	if comment.ParentID != nil {
		parentIDStr := comment.ParentID.String()
		pbComment.ParentId = &parentIDStr
	}

	return pbComment, nil
}

// GetComment retrieves a comment by ID
func (s *feedbackServer) GetComment(ctx context.Context, req *pb.GetCommentRequest) (*pb.LabComment, error) {
	// Validate request
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "id is required")
	}

	// Parse UUID
	id, err := uuid.Parse(req.Id)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid id format")
	}

	// Get comment
	comment, err := s.commentService.GetComment(ctx, id)
	if err != nil {
		if err.Error() == "comment not found" {
			return nil, status.Error(codes.NotFound, "comment not found")
		}
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get comment: %v", err))
	}

	// Convert to protobuf
	pbComment := &pb.LabComment{
		Id:        comment.ID.String(),
		LabId:     comment.LabID,
		UserId:    comment.UserID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
	}
	if comment.ParentID != nil {
		parentIDStr := comment.ParentID.String()
		pbComment.ParentId = &parentIDStr
	}

	return pbComment, nil
}

// UpdateComment updates a comment
func (s *feedbackServer) UpdateComment(ctx context.Context, req *pb.UpdateCommentRequest) (*pb.LabComment, error) {
	// Validate request
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "id is required")
	}
	if req.Content == "" {
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}

	// Parse UUID
	id, err := uuid.Parse(req.Id)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid id format")
	}

	// Update comment
	comment, err := s.commentService.UpdateComment(ctx, id, req.Content)
	if err != nil {
		if err.Error() == "comment not found" {
			return nil, status.Error(codes.NotFound, "comment not found")
		}
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to update comment: %v", err))
	}

	// Convert to protobuf
	pbComment := &pb.LabComment{
		Id:        comment.ID.String(),
		LabId:     comment.LabID,
		UserId:    comment.UserID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
	}
	if comment.ParentID != nil {
		parentIDStr := comment.ParentID.String()
		pbComment.ParentId = &parentIDStr
	}

	return pbComment, nil
}

// DeleteComment deletes a comment
func (s *feedbackServer) DeleteComment(ctx context.Context, req *pb.DeleteCommentRequest) (*pb.DeleteCommentResponse, error) {
	// Validate request
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "id is required")
	}

	// Parse UUID
	id, err := uuid.Parse(req.Id)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid id format")
	}

	// Delete comment
	err = s.commentService.DeleteComment(ctx, id)
	if err != nil {
		if err.Error() == "comment not found" {
			return nil, status.Error(codes.NotFound, "comment not found")
		}
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete comment: %v", err))
	}

	return &pb.DeleteCommentResponse{
		Success: true,
	}, nil
}

// ListLabComments lists comments for a lab
func (s *feedbackServer) ListLabComments(ctx context.Context, req *pb.ListLabCommentsRequest) (*pb.ListLabCommentsResponse, error) {
	// Validate request
	if req.LabId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "lab_id is required")
	}
	if req.Page <= 0 {
		req.Page = 1
	}
	if req.Limit <= 0 {
		req.Limit = 20
	}

	// Parse parent ID if provided
	var parentID *uuid.UUID
	if req.ParentId != nil {
		parsed, err := uuid.Parse(*req.ParentId)
		if err != nil {
			return nil, status.Error(codes.InvalidArgument, "invalid parent_id format")
		}
		parentID = &parsed
	}

	// List comments
	comments, totalCount, err := s.commentService.ListLabComments(ctx, req.LabId, parentID, req.Page, req.Limit)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list comments: %v", err))
	}

	// Convert to protobuf
	pbComments := make([]*pb.LabComment, len(comments))
	for i, comment := range comments {
		pbComment := &pb.LabComment{
			Id:        comment.ID.String(),
			LabId:     comment.LabID,
			UserId:    comment.UserID,
			Content:   comment.Content,
			CreatedAt: timestamppb.New(comment.CreatedAt),
			UpdatedAt: timestamppb.New(comment.UpdatedAt),
		}
		if comment.ParentID != nil {
			parentIDStr := comment.ParentID.String()
			pbComment.ParentId = &parentIDStr
		}
		pbComments[i] = pbComment
	}

	return &pb.ListLabCommentsResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}, nil
}

// GetCommentReplies gets replies to a specific comment
func (s *feedbackServer) GetCommentReplies(ctx context.Context, req *pb.GetCommentRepliesRequest) (*pb.GetCommentRepliesResponse, error) {
	// Validate request
	if req.CommentId == "" {
		return nil, status.Error(codes.InvalidArgument, "comment_id is required")
	}
	if req.Page <= 0 {
		req.Page = 1
	}
	if req.Limit <= 0 {
		req.Limit = 20
	}

	// Parse comment ID
	commentID, err := uuid.Parse(req.CommentId)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid comment_id format")
	}
	// Get replies
	comments, totalCount, err := s.commentService.GetCommentReplies(ctx, commentID, req.Page, req.Limit)
	if err != nil {
		if err.Error() == "parent comment not found" {
			return nil, status.Error(codes.NotFound, "parent comment not found")
		}
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get comment replies: %v", err))
	}

	// Convert to protobuf
	pbComments := make([]*pb.LabComment, len(comments))
	for i, comment := range comments {
		pbComment := &pb.LabComment{
			Id:        comment.ID.String(),
			LabId:     comment.LabID,
			UserId:    comment.UserID,
			Content:   comment.Content,
			CreatedAt: timestamppb.New(comment.CreatedAt),
			UpdatedAt: timestamppb.New(comment.UpdatedAt),
		}
		if comment.ParentID != nil {
			parentIDStr := comment.ParentID.String()
			pbComment.ParentId = &parentIDStr
		}
		pbComments[i] = pbComment
	}

	return &pb.GetCommentRepliesResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}, nil
}
