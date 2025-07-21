package server

import (
	"context"
	"fmt"
	"io"
	"log/slog"
	"time"

	pb "github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/api"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/service"
	"github.com/google/uuid"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// FeedbackServer implements the simplified FeedbackService gRPC server
type FeedbackServer struct {
	pb.UnimplementedFeedbackServiceServer
	feedbackService *service.FeedbackService
	logger          *slog.Logger
}

// RegisterFeedbackServer registers the feedback server with gRPC
func RegisterFeedbackServer(s *grpc.Server, feedbackService *service.FeedbackService, logger *slog.Logger) {
	server := &FeedbackServer{
		feedbackService: feedbackService,
		logger:          logger,
	}
	pb.RegisterFeedbackServiceServer(s, server)
}

// Helper function to convert model Feedback to protobuf Feedback
func convertToProtoFeedback(feedback *models.Feedback) *pb.Feedback {
	return &pb.Feedback{
		Id:           feedback.ID.String(),
		ReviewerId:   feedback.ReviewerID,
		StudentId:    feedback.StudentID,
		SubmissionId: feedback.SubmissionID,
		Title:        feedback.Title,
		Content:      feedback.Content,
		CreatedAt:    timestamppb.New(feedback.CreatedAt),
		UpdatedAt:    timestamppb.New(feedback.UpdatedAt),
	}
}

// Reviewer Operations

// CreateFeedback creates a new feedback entry (reviewer only)
func (s *FeedbackServer) CreateFeedback(ctx context.Context, req *pb.CreateFeedbackRequest) (*pb.Feedback, error) {
	s.logger.Info("gRPC CreateFeedback received",
		"reviewer_id", req.ReviewerId,
		"student_id", req.StudentId,
		"submission_id", req.SubmissionId,
	)

	// Validate request
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.StudentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}
	if req.SubmissionId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "submission_id is required")
	}
	if req.Title == "" {
		return nil, status.Error(codes.InvalidArgument, "title is required")
	}

	// Create feedback
	feedback, err := s.feedbackService.CreateFeedback(ctx, req.ReviewerId, req.StudentId, req.SubmissionId, req.Title, req.Content)
	if err != nil {
		s.logger.Error("gRPC CreateFeedback failed", "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to create feedback: %v", err))
	}

	response := convertToProtoFeedback(feedback)
	s.logger.Info("gRPC CreateFeedback completed", "id", response.Id)
	return response, nil
}

// UpdateFeedback updates an existing feedback (reviewer only)
func (s *FeedbackServer) UpdateFeedback(ctx context.Context, req *pb.UpdateFeedbackRequest) (*pb.Feedback, error) {
	s.logger.Info("gRPC UpdateFeedback received",
		"id", req.Id,
		"reviewer_id", req.ReviewerId,
	)

	// Validate request
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback ID is required")
	}

	id, err := uuid.Parse(req.Id)
	if err != nil {
		s.logger.Warn("gRPC UpdateFeedback: invalid ID format", "id", req.Id, "error", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	var title, content *string
	if req.Title != nil {
		title = req.Title
	}
	if req.Content != nil {
		content = req.Content
	}

	feedback, err := s.feedbackService.UpdateFeedback(ctx, id, req.ReviewerId, title, content)
	if err != nil {
		s.logger.Error("gRPC UpdateFeedback failed", "id", req.Id, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to update feedback: %v", err))
	}

	response := convertToProtoFeedback(feedback)
	s.logger.Info("gRPC UpdateFeedback completed", "id", response.Id)
	return response, nil
}

// DeleteFeedback deletes a feedback (reviewer only)
func (s *FeedbackServer) DeleteFeedback(ctx context.Context, req *pb.DeleteFeedbackRequest) (*pb.DeleteFeedbackResponse, error) {
	s.logger.Info("gRPC DeleteFeedback received",
		"id", req.Id,
		"reviewer_id", req.ReviewerId,
	)

	// Validate request
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback ID is required")
	}

	id, err := uuid.Parse(req.Id)
	if err != nil {
		s.logger.Warn("gRPC DeleteFeedback: invalid ID format", "id", req.Id, "error", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	err = s.feedbackService.DeleteFeedback(ctx, id, req.ReviewerId)
	if err != nil {
		s.logger.Error("gRPC DeleteFeedback failed", "id", req.Id, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete feedback: %v", err))
	}

	response := &pb.DeleteFeedbackResponse{Success: true}
	s.logger.Info("gRPC DeleteFeedback completed", "id", req.Id)
	return response, nil
}

// ListReviewerFeedbacks lists feedbacks created by a reviewer
func (s *FeedbackServer) ListReviewerFeedbacks(ctx context.Context, req *pb.ListReviewerFeedbacksRequest) (*pb.ListReviewerFeedbacksResponse, error) {
	s.logger.Info("gRPC ListReviewerFeedbacks received",
		"reviewer_id", req.ReviewerId,
		"submission_id", req.SubmissionId,
		"page", req.Page,
		"limit", req.Limit,
	)

	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}

	var submissionID *int64
	if req.SubmissionId != nil {
		submissionID = req.SubmissionId
	}

	feedbacks, totalCount, err := s.feedbackService.ListReviewerFeedbacks(ctx, req.ReviewerId, submissionID, req.Page, req.Limit)
	if err != nil {
		s.logger.Error("gRPC ListReviewerFeedbacks failed", "reviewer_id", req.ReviewerId, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list reviewer feedbacks: %v", err))
	}

	pbFeedbacks := make([]*pb.Feedback, len(feedbacks))
	for i, feedback := range feedbacks {
		pbFeedbacks[i] = convertToProtoFeedback(feedback)
	}

	response := &pb.ListReviewerFeedbacksResponse{
		Feedbacks:  pbFeedbacks,
		TotalCount: totalCount,
	}

	s.logger.Info("gRPC ListReviewerFeedbacks completed",
		"reviewer_id", req.ReviewerId,
		"count", len(feedbacks),
		"total_count", totalCount,
	)
	return response, nil
}

// Student Operations

// GetStudentFeedback retrieves feedback for a student by submission
func (s *FeedbackServer) GetStudentFeedback(ctx context.Context, req *pb.GetStudentFeedbackRequest) (*pb.Feedback, error) {
	s.logger.Info("gRPC GetStudentFeedback received",
		"student_id", req.StudentId,
		"submission_id", req.SubmissionId,
	)

	if req.StudentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}
	if req.SubmissionId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "submission_id is required")
	}

	feedbacks, err := s.feedbackService.GetStudentFeedback(ctx, req.StudentId, req.SubmissionId)
	if err != nil {
		s.logger.Error("gRPC GetStudentFeedback failed",
			"student_id", req.StudentId,
			"submission_id", req.SubmissionId,
			"error", err,
		)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get student feedback: %v", err))
	}

	if len(feedbacks) == 0 {
		s.logger.Warn("gRPC GetStudentFeedback: no feedback found",
			"student_id", req.StudentId,
			"submission_id", req.SubmissionId,
		)
		return nil, status.Error(codes.NotFound, "no feedback found for this submission")
	}

	// Return the first feedback (assuming one feedback per submission)
	// If multiple feedbacks are expected, this should be changed to return all
	response := convertToProtoFeedback(feedbacks[0])
	s.logger.Info("gRPC GetStudentFeedback completed", "id", response.Id)
	return response, nil
}

// ListStudentFeedbacks lists all feedbacks for a student
func (s *FeedbackServer) ListStudentFeedbacks(ctx context.Context, req *pb.ListStudentFeedbacksRequest) (*pb.ListStudentFeedbacksResponse, error) {
	s.logger.Info("gRPC ListStudentFeedbacks received",
		"student_id", req.StudentId,
		"submission_id", req.SubmissionId,
		"page", req.Page,
		"limit", req.Limit,
	)

	if req.StudentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}

	var submissionID *int64
	if req.SubmissionId != nil {
		submissionID = req.SubmissionId
	}

	feedbacks, totalCount, err := s.feedbackService.ListStudentFeedbacks(ctx, req.StudentId, submissionID, req.Page, req.Limit)
	if err != nil {
		s.logger.Error("gRPC ListStudentFeedbacks failed", "student_id", req.StudentId, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list student feedbacks: %v", err))
	}

	pbFeedbacks := make([]*pb.Feedback, len(feedbacks))
	for i, feedback := range feedbacks {
		pbFeedbacks[i] = convertToProtoFeedback(feedback)
	}

	response := &pb.ListStudentFeedbacksResponse{
		Feedbacks:  pbFeedbacks,
		TotalCount: totalCount,
	}

	s.logger.Info("gRPC ListStudentFeedbacks completed",
		"student_id", req.StudentId,
		"count", len(feedbacks),
		"total_count", totalCount,
	)
	return response, nil
}

// GetFeedbackById retrieves feedback by its ID
func (s *FeedbackServer) GetFeedbackById(ctx context.Context, req *pb.GetFeedbackByIdRequest) (*pb.Feedback, error) {
	s.logger.Info("gRPC GetFeedbackById received", "id", req.Id)

	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback id is required")
	}

	feedbackID, err := uuid.Parse(req.Id)
	if err != nil {
		s.logger.Warn("gRPC GetFeedbackById: invalid ID format", "id", req.Id, "error", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	feedback, err := s.feedbackService.GetFeedbackByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("gRPC GetFeedbackById failed", "id", req.Id, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get feedback: %v", err))
	}

	if feedback == nil {
		s.logger.Warn("gRPC GetFeedbackById: feedback not found", "id", req.Id)
		return nil, status.Error(codes.NotFound, "feedback not found")
	}

	response := convertToProtoFeedback(feedback)
	s.logger.Info("gRPC GetFeedbackById completed", "id", response.Id)
	return response, nil
}

// Attachment Operations

// UploadAttachment uploads an attachment to a feedback (reviewer only)
func (s *FeedbackServer) UploadAttachment(stream pb.FeedbackService_UploadAttachmentServer) error {
	s.logger.Info("gRPC UploadAttachment: starting stream upload")

	// Create context with cancellation for proper cleanup
	ctx, cancel := context.WithCancel(stream.Context())
	defer cancel()

	// Receive first chunk with metadata
	req, err := stream.Recv()
	if err != nil {
		if err == io.EOF {
			s.logger.Warn("gRPC UploadAttachment: no metadata received - stream closed immediately")
			return status.Error(codes.InvalidArgument, "no metadata received - stream closed immediately")
		}
		s.logger.Error("gRPC UploadAttachment: failed to receive metadata", "error", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to receive metadata: %v", err))
	}

	metadata := req.GetMetadata()
	if metadata == nil {
		s.logger.Error("gRPC UploadAttachment: metadata is required in first chunk")
		return status.Error(codes.InvalidArgument, "metadata is required in first chunk")
	}
	
	s.logger.Info("gRPC UploadAttachment received",
		"reviewer_id", metadata.ReviewerId,
		"feedback_id", metadata.FeedbackId,
		"filename", metadata.Filename,
		"size", metadata.TotalSize,
		"content_type", metadata.ContentType,
	)
	
	if metadata.ReviewerId <= 0 {
		s.logger.Error("gRPC UploadAttachment: reviewer_id is required")
		return status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if metadata.FeedbackId == "" {
		s.logger.Error("gRPC UploadAttachment: feedback_id is required")
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if metadata.Filename == "" {
		s.logger.Error("gRPC UploadAttachment: filename is required")
		return status.Error(codes.InvalidArgument, "filename is required")
	}
	if metadata.TotalSize <= 0 {
		s.logger.Error("gRPC UploadAttachment: total_size must be positive")
		return status.Error(codes.InvalidArgument, "total_size must be positive")
	}

	feedbackID, err := uuid.Parse(metadata.FeedbackId)
	if err != nil {
		s.logger.Warn("gRPC UploadAttachment: invalid feedback ID format", "feedback_id", metadata.FeedbackId, "error", err)
		return status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	// Check attachment count limit
	existingAttachments, err := s.feedbackService.ListAttachments(ctx, feedbackID)
	if err != nil {
		s.logger.Error("gRPC UploadAttachment: failed to check existing attachments", "error", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to check existing attachments: %v", err))
	}
	if len(existingAttachments) >= config.MaxAttachmentsPerFeedback {
		s.logger.Warn("gRPC UploadAttachment: maximum attachments reached", "max_attachments", config.MaxAttachmentsPerFeedback)
		return status.Error(codes.FailedPrecondition, fmt.Sprintf("maximum %d attachments allowed per feedback", config.MaxAttachmentsPerFeedback))
	}

	// Create pipe for streaming data
	pipeReader, pipeWriter := io.Pipe()

	// Channel for upload result
	uploadErrCh := make(chan error, 1)

	// Start upload goroutine IMMEDIATELY using the same context
	go func() {
		defer func() {
			if r := recover(); r != nil {
				s.logger.Error("gRPC UploadAttachment: upload panic", "panic", r)
				uploadErrCh <- fmt.Errorf("upload panic: %v", r)
			}
		}()
		
		s.logger.Info("gRPC UploadAttachment: starting upload goroutine")
		err := s.feedbackService.UploadAttachment(ctx, feedbackID, metadata.Filename, metadata.ContentType, pipeReader, metadata.TotalSize)
		s.logger.Info("gRPC UploadAttachment: upload goroutine finished", "feedback_id", feedbackID, "filename", metadata.Filename, "error", err)
		uploadErrCh <- err
	}()

	// Stream data from client to pipe
	var totalReceived int64
	var streamErr error

	// Check if the first request also contains chunk data
	firstChunk := req.GetChunk()
	
	// Read chunks from stream
	s.logger.Info("gRPC UploadAttachment: starting to read chunks from stream")
	
	func() {
		// Ensure pipe writer is closed when we exit this function
		defer func() {
			s.logger.Info("gRPC UploadAttachment: closing pipe writer")
			if closeErr := pipeWriter.Close(); closeErr != nil {
				s.logger.Error("gRPC UploadAttachment: error closing pipe writer", "error", closeErr)
				if streamErr == nil {
					streamErr = fmt.Errorf("failed to close pipe writer: %v", closeErr)
				}
			}
		}()

		// Handle first chunk if it exists
		if firstChunk != nil && len(firstChunk) > 0 {
			s.logger.Info("gRPC UploadAttachment: processing first chunk", "chunk_size", len(firstChunk))
			
			// Validate total size
			if int64(len(firstChunk)) > metadata.TotalSize {
				streamErr = fmt.Errorf("first chunk larger than total size: %d > %d", len(firstChunk), metadata.TotalSize)
				s.logger.Error("gRPC UploadAttachment: size validation failed", "chunk_size", len(firstChunk), "total_size", metadata.TotalSize, "error", streamErr)
				return
			}

			// Write first chunk to pipe
			n, writeErr := pipeWriter.Write(firstChunk)
			if writeErr != nil {
				streamErr = fmt.Errorf("failed to write first chunk: %v", writeErr)
				s.logger.Error("gRPC UploadAttachment: write error", "error", writeErr)
				return
			}
			totalReceived += int64(n)
			s.logger.Info("gRPC UploadAttachment: written first chunk", "chunk_size", n, "total_received", totalReceived, "total_expected", metadata.TotalSize)
			
			// Check if we've received all expected data from first chunk
			if totalReceived >= metadata.TotalSize {
				s.logger.Info("gRPC UploadAttachment: received all expected data from first chunk, ending stream", "total_received", totalReceived)
				return
			}
		}

		// Continue reading additional chunks
		for {
			select {
			case <-ctx.Done():
				streamErr = fmt.Errorf("stream context cancelled: %v", ctx.Err())
				return
			default:
			}

			if streamErr != nil {
				return
			}

			s.logger.Info("gRPC UploadAttachment: waiting for next chunk...")
			req, err := stream.Recv()
			if err == io.EOF {
				// End of stream - this is expected and normal
				s.logger.Info("gRPC UploadAttachment: received EOF, stream ended normally", "total_received", totalReceived)
				return
			}
			if err != nil {
				// Check if it's a context cancellation error
				if ctx.Err() != nil {
					streamErr = fmt.Errorf("stream cancelled: %v", ctx.Err())
				} else {
					streamErr = fmt.Errorf("failed to receive chunk: %v", err)
				}
				s.logger.Error("gRPC UploadAttachment: error receiving chunk", "error", err)
				return
			}

			chunk := req.GetChunk()
			if chunk == nil {
				// Skip empty chunks - this means we got a metadata packet or empty chunk
				s.logger.Info("gRPC UploadAttachment: received empty chunk or metadata packet, skipping")
				continue
			}

			s.logger.Info("gRPC UploadAttachment: received chunk", "chunk_size", len(chunk))

			// Validate total size
			if totalReceived+int64(len(chunk)) > metadata.TotalSize {
				streamErr = fmt.Errorf("received more data than expected: %d + %d > %d", totalReceived, len(chunk), metadata.TotalSize)
				s.logger.Error("gRPC UploadAttachment: size validation failed", "total_received", totalReceived, "chunk_size", len(chunk), "total_expected", metadata.TotalSize, "error", streamErr)
				return
			}

			// Write chunk to pipe
			n, writeErr := pipeWriter.Write(chunk)
			if writeErr != nil {
				// Check if the error is due to closed pipe
				if writeErr == io.ErrClosedPipe {
					streamErr = fmt.Errorf("pipe closed during write - upload may have failed")
				} else {
					streamErr = fmt.Errorf("failed to write chunk: %v", writeErr)
				}
				s.logger.Error("gRPC UploadAttachment: write error", "error", writeErr)
				return
			}
			totalReceived += int64(n)
			s.logger.Info("gRPC UploadAttachment: written chunk", "chunk_size", n, "total_received", totalReceived, "total_expected", metadata.TotalSize)
			
			// Check if we've received all expected data
			if totalReceived >= metadata.TotalSize {
				s.logger.Info("gRPC UploadAttachment: received all expected data, ending stream", "total_received", totalReceived)
				return
			}
		}
	}()

	// Check for streaming errors
	if streamErr != nil {
		s.logger.Error("gRPC UploadAttachment: stream error detected", "error", streamErr)
		cancel() // Cancel main context to stop upload
		// Wait for upload to finish with a timeout
		select {
		case uploadErr := <-uploadErrCh:
			// Upload finished
			s.logger.Info("gRPC UploadAttachment: upload finished after stream error", "feedback_id", feedbackID, "error", uploadErr)
		case <-time.After(5 * time.Second):
			// Timeout waiting for upload to finish
			s.logger.Warn("gRPC UploadAttachment: timeout waiting for upload to finish after stream error")
		}
		return status.Error(codes.Internal, streamErr.Error())
	}

	// Validate that we received all expected data
	if totalReceived != metadata.TotalSize {
		s.logger.Error("gRPC UploadAttachment: size mismatch", "total_received", totalReceived, "total_expected", metadata.TotalSize)
		cancel() // Cancel main context to stop upload
		return status.Error(codes.InvalidArgument, fmt.Sprintf("received %d bytes, expected %d bytes", totalReceived, metadata.TotalSize))
	}

	s.logger.Info("gRPC UploadAttachment: waiting for upload to complete...")
	// Wait for upload to complete
	select {
	case uploadErr := <-uploadErrCh:
		if uploadErr != nil {
			s.logger.Error("gRPC UploadAttachment: upload failed", "feedback_id", feedbackID, "error", uploadErr)
			return status.Error(codes.Internal, fmt.Sprintf("failed to upload attachment: %v", uploadErr))
		}
		s.logger.Info("gRPC UploadAttachment: upload completed successfully", "feedback_id", feedbackID)
	case <-ctx.Done():
		s.logger.Error("gRPC UploadAttachment: upload cancelled due to context", "feedback_id", feedbackID, "error", ctx.Err())
		return status.Error(codes.Canceled, "upload cancelled")
	case <-time.After(30 * time.Second): // Add reasonable timeout
		s.logger.Warn("gRPC UploadAttachment: upload timed out")
		cancel() // Cancel context to stop any ongoing operations
		return status.Error(codes.DeadlineExceeded, "upload timeout")
	}

	s.logger.Info("gRPC UploadAttachment response", "filename", metadata.Filename, "size", totalReceived)
	return stream.SendAndClose(&pb.UploadAttachmentResponse{
		Filename: metadata.Filename,
		Size:     totalReceived,
		Success:  true,
	})
}

// DeleteAttachment deletes an attachment (reviewer only)
func (s *FeedbackServer) DeleteAttachment(ctx context.Context, req *pb.DeleteAttachmentRequest) (*pb.DeleteAttachmentResponse, error) {
	s.logger.Info("gRPC DeleteAttachment received",
		"reviewer_id", req.ReviewerId,
		"feedback_id", req.FeedbackId,
		"filename", req.Filename,
	)

	// Validate request
	if req.ReviewerId <= 0 {
		s.logger.Error("gRPC DeleteAttachment: reviewer_id is required")
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.FeedbackId == "" {
		s.logger.Error("gRPC DeleteAttachment: feedback_id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		s.logger.Error("gRPC DeleteAttachment: filename is required")
		return nil, status.Error(codes.InvalidArgument, "filename is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		s.logger.Warn("gRPC DeleteAttachment: invalid feedback ID format", "feedback_id", req.FeedbackId, "error", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	err = s.feedbackService.DeleteAttachment(ctx, feedbackID, req.Filename)
	if err != nil {
		s.logger.Error("gRPC DeleteAttachment failed", "feedback_id", feedbackID, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete attachment: %v", err))
	}

	response := &pb.DeleteAttachmentResponse{Success: true}
	s.logger.Info("gRPC DeleteAttachment completed", "feedback_id", feedbackID)
	return response, nil
}

// DownloadAttachment downloads an attachment (both roles)
func (s *FeedbackServer) DownloadAttachment(req *pb.DownloadAttachmentRequest, stream pb.FeedbackService_DownloadAttachmentServer) error {
	s.logger.Info("gRPC DownloadAttachment received",
		"feedback_id", req.FeedbackId,
		"filename", req.Filename,
	)

	if req.FeedbackId == "" {
		s.logger.Error("gRPC DownloadAttachment: feedback_id is required")
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		s.logger.Error("gRPC DownloadAttachment: filename is required")
		return status.Error(codes.InvalidArgument, "filename is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		s.logger.Warn("gRPC DownloadAttachment: invalid feedback ID format", "feedback_id", req.FeedbackId, "error", err)
		return status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	// Get attachment info first
	attachments, err := s.feedbackService.ListAttachments(stream.Context(), feedbackID)
	if err != nil {
		s.logger.Error("gRPC DownloadAttachment: failed to get attachment info", "feedback_id", feedbackID, "error", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to get attachment info: %v", err))
	}

	var attachmentInfo *models.AttachmentInfo
	for _, attachment := range attachments {
		if attachment.Filename == req.Filename {
			attachmentInfo = attachment
			break
		}
	}

	if attachmentInfo == nil {
		s.logger.Warn("gRPC DownloadAttachment: attachment not found", "filename", req.Filename)
		return status.Error(codes.NotFound, "attachment not found")
	}

	s.logger.Info("gRPC DownloadAttachment: found attachment",
		"filename", attachmentInfo.Filename,
		"size", attachmentInfo.Size,
		"content_type", attachmentInfo.ContentType,
	)

	// Send attachment info first
	err = stream.Send(&pb.DownloadAttachmentResponse{
		Data: &pb.DownloadAttachmentResponse_Info{
			Info: &pb.AttachmentInfo{
				Filename:    attachmentInfo.Filename,
				Size:        attachmentInfo.Size,
				ContentType: attachmentInfo.ContentType,
				UploadedAt:  timestamppb.New(attachmentInfo.UploadedAt),
			},
		},
	})
	if err != nil {
		s.logger.Error("gRPC DownloadAttachment: failed to send attachment info", "error", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to send attachment info: %v", err))
	}

	// Download and stream file content
	reader, _, err := s.feedbackService.DownloadAttachment(stream.Context(), feedbackID, req.Filename)
	if err != nil {
		s.logger.Error("gRPC DownloadAttachment: failed to download attachment", "feedback_id", feedbackID, "error", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to download attachment: %v", err))
	}
	defer reader.Close()

	s.logger.Info("gRPC DownloadAttachment: starting to stream file content")
	var totalSent int64
	buffer := make([]byte, 32*1024) // 32KB chunks
	for {
		n, err := reader.Read(buffer)
		if err == io.EOF {
			break
		}
		if err != nil {
			s.logger.Error("gRPC DownloadAttachment: failed to read attachment", "error", err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to read attachment: %v", err))
		}

		err = stream.Send(&pb.DownloadAttachmentResponse{
			Data: &pb.DownloadAttachmentResponse_Chunk{
				Chunk: buffer[:n],
			},
		})
		if err != nil {
			s.logger.Error("gRPC DownloadAttachment: failed to send chunk", "error", err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to send chunk: %v", err))
		}
		totalSent += int64(n)
	}

	s.logger.Info("gRPC DownloadAttachment response", "total_sent", totalSent)
	return nil
}

// ListAttachments lists attachments for a feedback (both roles)
func (s *FeedbackServer) ListAttachments(ctx context.Context, req *pb.ListAttachmentsRequest) (*pb.ListAttachmentsResponse, error) {
	s.logger.Info("gRPC ListAttachments received", "feedback_id", req.FeedbackId)

	if req.FeedbackId == "" {
		s.logger.Error("gRPC ListAttachments: feedback_id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		s.logger.Warn("gRPC ListAttachments: invalid feedback ID format", "feedback_id", req.FeedbackId, "error", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	attachments, err := s.feedbackService.ListAttachments(ctx, feedbackID)
	if err != nil {
		s.logger.Error("gRPC ListAttachments failed", "feedback_id", feedbackID, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list attachments: %v", err))
	}

	pbAttachments := make([]*pb.AttachmentInfo, len(attachments))
	for i, attachment := range attachments {
		pbAttachments[i] = &pb.AttachmentInfo{
			Filename:    attachment.Filename,
			Size:        attachment.Size,
			ContentType: attachment.ContentType,
			UploadedAt:  timestamppb.New(attachment.UploadedAt),
		}
	}

	response := &pb.ListAttachmentsResponse{Attachments: pbAttachments}
	s.logger.Info("gRPC ListAttachments completed", "feedback_id", req.FeedbackId, "count", len(attachments))
	return response, nil
}

// GetAttachmentLocation returns location information for attachments (both roles)
func (s *FeedbackServer) GetAttachmentLocation(ctx context.Context, req *pb.GetAttachmentLocationRequest) (*pb.GetAttachmentLocationResponse, error) {
	s.logger.Info("gRPC GetAttachmentLocation received",
		"feedback_id", req.FeedbackId,
		"filename", req.Filename,
	)

	if req.FeedbackId == "" {
		s.logger.Error("gRPC GetAttachmentLocation: feedback_id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		s.logger.Warn("gRPC GetAttachmentLocation: invalid feedback ID format", "feedback_id", req.FeedbackId, "error", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	var locationInfos []*models.AttachmentLocationInfo

	if req.Filename != nil && *req.Filename != "" {
		s.logger.Info("gRPC GetAttachmentLocation: getting location for specific file", "filename", *req.Filename)
		// Get location info for specific attachment
		locationInfo, err := s.feedbackService.GetAttachmentLocation(ctx, feedbackID, *req.Filename)
		if err != nil {
			s.logger.Error("gRPC GetAttachmentLocation: failed to get attachment location", "feedback_id", feedbackID, "error", err)
			return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get attachment location: %v", err))
		}
		locationInfos = []*models.AttachmentLocationInfo{locationInfo}
	} else {
		s.logger.Info("gRPC GetAttachmentLocation: getting locations for all attachments")
		// Get location info for all attachments
		infos, err := s.feedbackService.ListAttachmentLocations(ctx, feedbackID)
		if err != nil {
			s.logger.Error("gRPC GetAttachmentLocation: failed to list attachment locations", "feedback_id", feedbackID, "error", err)
			return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list attachment locations: %v", err))
		}
		locationInfos = infos
	}

	pbLocationInfos := make([]*pb.AttachmentLocationInfo, len(locationInfos))
	for i, info := range locationInfos {
		pbLocationInfos[i] = &pb.AttachmentLocationInfo{
			Filename:         info.Filename,
			Size:             info.Size,
			ContentType:      info.ContentType,
			UploadedAt:       timestamppb.New(info.UploadedAt),
			MinioBucket:      info.MinioBucket,
			MinioObjectPath:  info.MinioObjectPath,
			MinioEndpoint:    info.MinioEndpoint,
			UseSsl:           info.UseSSL,
		}
	}

	response := &pb.GetAttachmentLocationResponse{Attachments: pbLocationInfos}
	s.logger.Info("gRPC GetAttachmentLocation completed", "feedback_id", req.FeedbackId, "count", len(locationInfos))
	return response, nil
}
