package server

import (
	"context"
	"fmt"
	"io"
	"log"
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
}

// RegisterFeedbackServer registers the feedback server with gRPC
func RegisterFeedbackServer(s *grpc.Server, feedbackService *service.FeedbackService) {
	server := &FeedbackServer{
		feedbackService: feedbackService,
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
	log.Printf("gRPC CreateFeedback received: ReviewerId=%d, StudentId=%d, SubmissionId=%d, Title=%q, Content=%q", req.ReviewerId, req.StudentId, req.SubmissionId, req.Title, req.Content)

	// Validate request
	if req.ReviewerId <= 0 {
		log.Printf("gRPC CreateFeedback error: reviewer_id is required")
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.StudentId <= 0 {
		log.Printf("gRPC CreateFeedback error: student_id is required")
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}
	if req.SubmissionId <= 0 {
		log.Printf("gRPC CreateFeedback error: submission_id is required")
		return nil, status.Error(codes.InvalidArgument, "submission_id is required")
	}
	if req.Title == "" {
		log.Printf("gRPC CreateFeedback error: title is required")
		return nil, status.Error(codes.InvalidArgument, "title is required")
	}

	// Create feedback
	feedback, err := s.feedbackService.CreateFeedback(ctx, req.ReviewerId, req.StudentId, req.SubmissionId, req.Title, req.Content)
	if err != nil {
		log.Printf("gRPC CreateFeedback error: failed to create feedback: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to create feedback: %v", err))
	}

	response := convertToProtoFeedback(feedback)
	log.Printf("gRPC CreateFeedback response: Id=%s, ReviewerId=%d, StudentId=%d, SubmissionId=%d", response.Id, response.ReviewerId, response.StudentId, response.SubmissionId)
	return response, nil
}

// UpdateFeedback updates an existing feedback (reviewer only)
func (s *FeedbackServer) UpdateFeedback(ctx context.Context, req *pb.UpdateFeedbackRequest) (*pb.Feedback, error) {
	log.Printf("gRPC UpdateFeedback received: Id=%s, ReviewerId=%d, Title=%v, Content=%v", req.Id, req.ReviewerId, req.Title, req.Content)

	// Validate request
	if req.ReviewerId <= 0 {
		log.Printf("gRPC UpdateFeedback error: reviewer_id is required")
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.Id == "" {
		log.Printf("gRPC UpdateFeedback error: feedback ID is required")
		return nil, status.Error(codes.InvalidArgument, "feedback ID is required")
	}

	id, err := uuid.Parse(req.Id)
	if err != nil {
		log.Printf("gRPC UpdateFeedback error: invalid feedback ID format: %v", err)
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
		log.Printf("gRPC UpdateFeedback error: failed to update feedback: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to update feedback: %v", err))
	}

	response := convertToProtoFeedback(feedback)
	log.Printf("gRPC UpdateFeedback response: Id=%s, ReviewerId=%d, UpdatedAt=%v", response.Id, response.ReviewerId, response.UpdatedAt.AsTime())
	return response, nil
}

// DeleteFeedback deletes a feedback (reviewer only)
func (s *FeedbackServer) DeleteFeedback(ctx context.Context, req *pb.DeleteFeedbackRequest) (*pb.DeleteFeedbackResponse, error) {
	log.Printf("gRPC DeleteFeedback received: Id=%s, ReviewerId=%d", req.Id, req.ReviewerId)

	// Validate request
	if req.ReviewerId <= 0 {
		log.Printf("gRPC DeleteFeedback error: reviewer_id is required")
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.Id == "" {
		log.Printf("gRPC DeleteFeedback error: feedback ID is required")
		return nil, status.Error(codes.InvalidArgument, "feedback ID is required")
	}

	id, err := uuid.Parse(req.Id)
	if err != nil {
		log.Printf("gRPC DeleteFeedback error: invalid feedback ID format: %v", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	err = s.feedbackService.DeleteFeedback(ctx, id, req.ReviewerId)
	if err != nil {
		log.Printf("gRPC DeleteFeedback error: failed to delete feedback: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete feedback: %v", err))
	}

	response := &pb.DeleteFeedbackResponse{Success: true}
	log.Printf("gRPC DeleteFeedback response: Success=%t", response.Success)
	return response, nil
}

// ListReviewerFeedbacks lists feedbacks created by a reviewer
func (s *FeedbackServer) ListReviewerFeedbacks(ctx context.Context, req *pb.ListReviewerFeedbacksRequest) (*pb.ListReviewerFeedbacksResponse, error) {
	log.Printf("gRPC ListReviewerFeedbacks received: ReviewerId=%d, SubmissionId=%v, Page=%d, Limit=%d", req.ReviewerId, req.SubmissionId, req.Page, req.Limit)

	if req.ReviewerId <= 0 {
		log.Printf("gRPC ListReviewerFeedbacks error: reviewer_id is required")
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}

	var submissionID *int64
	if req.SubmissionId != nil {
		submissionID = req.SubmissionId
	}

	feedbacks, totalCount, err := s.feedbackService.ListReviewerFeedbacks(ctx, req.ReviewerId, submissionID, req.Page, req.Limit)
	if err != nil {
		log.Printf("gRPC ListReviewerFeedbacks error: failed to list reviewer feedbacks: %v", err)
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

	log.Printf("gRPC ListReviewerFeedbacks response: %d feedbacks, totalCount=%d", len(feedbacks), totalCount)
	return response, nil
}

// Student Operations

// GetStudentFeedback retrieves feedback for a student by submission
func (s *FeedbackServer) GetStudentFeedback(ctx context.Context, req *pb.GetStudentFeedbackRequest) (*pb.Feedback, error) {
	log.Printf("gRPC GetStudentFeedback received: StudentId=%d, SubmissionId=%d", req.StudentId, req.SubmissionId)

	if req.StudentId <= 0 {
		log.Printf("gRPC GetStudentFeedback error: student_id is required")
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}
	if req.SubmissionId <= 0 {
		log.Printf("gRPC GetStudentFeedback error: submission_id is required")
		return nil, status.Error(codes.InvalidArgument, "submission_id is required")
	}

	feedbacks, err := s.feedbackService.GetStudentFeedback(ctx, req.StudentId, req.SubmissionId)
	if err != nil {
		log.Printf("gRPC GetStudentFeedback error: failed to get student feedback: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get student feedback: %v", err))
	}

	if len(feedbacks) == 0 {
		log.Printf("gRPC GetStudentFeedback error: no feedback found for StudentId=%d, SubmissionId=%d", req.StudentId, req.SubmissionId)
		return nil, status.Error(codes.NotFound, "no feedback found for this submission")
	}

	// Return the first feedback (assuming one feedback per submission)
	// If multiple feedbacks are expected, this should be changed to return all
	response := convertToProtoFeedback(feedbacks[0])
	log.Printf("gRPC GetStudentFeedback response: Id=%s, ReviewerId=%d, StudentId=%d, SubmissionId=%d", response.Id, response.ReviewerId, response.StudentId, response.SubmissionId)
	return response, nil
}

// ListStudentFeedbacks lists all feedbacks for a student
func (s *FeedbackServer) ListStudentFeedbacks(ctx context.Context, req *pb.ListStudentFeedbacksRequest) (*pb.ListStudentFeedbacksResponse, error) {
	log.Printf("gRPC ListStudentFeedbacks received: StudentId=%d, SubmissionId=%v, Page=%d, Limit=%d", req.StudentId, req.SubmissionId, req.Page, req.Limit)

	if req.StudentId <= 0 {
		log.Printf("gRPC ListStudentFeedbacks error: student_id is required")
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}

	var submissionID *int64
	if req.SubmissionId != nil {
		submissionID = req.SubmissionId
	}

	feedbacks, totalCount, err := s.feedbackService.ListStudentFeedbacks(ctx, req.StudentId, submissionID, req.Page, req.Limit)
	if err != nil {
		log.Printf("gRPC ListStudentFeedbacks error: failed to list student feedbacks: %v", err)
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

	log.Printf("gRPC ListStudentFeedbacks response: %d feedbacks, totalCount=%d", len(feedbacks), totalCount)
	return response, nil
}

// GetFeedbackById retrieves feedback by its ID
func (s *FeedbackServer) GetFeedbackById(ctx context.Context, req *pb.GetFeedbackByIdRequest) (*pb.Feedback, error) {
	log.Printf("gRPC GetFeedbackById received: Id=%s", req.Id)

	if req.Id == "" {
		log.Printf("gRPC GetFeedbackById error: feedback id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback id is required")
	}

	feedbackID, err := uuid.Parse(req.Id)
	if err != nil {
		log.Printf("gRPC GetFeedbackById error: invalid feedback ID format: %v", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	feedback, err := s.feedbackService.GetFeedbackByID(ctx, feedbackID)
	if err != nil {
		log.Printf("gRPC GetFeedbackById error: failed to get feedback: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get feedback: %v", err))
	}

	if feedback == nil {
		log.Printf("gRPC GetFeedbackById error: feedback not found for Id=%s", req.Id)
		return nil, status.Error(codes.NotFound, "feedback not found")
	}

	response := convertToProtoFeedback(feedback)
	log.Printf("gRPC GetFeedbackById response: Id=%s, ReviewerId=%d, StudentId=%d, SubmissionId=%d", response.Id, response.ReviewerId, response.StudentId, response.SubmissionId)
	return response, nil
}

// Attachment Operations

// UploadAttachment uploads an attachment to a feedback (reviewer only)
func (s *FeedbackServer) UploadAttachment(stream pb.FeedbackService_UploadAttachmentServer) error {
	log.Printf("gRPC UploadAttachment: starting stream upload")

	// Create context with cancellation for proper cleanup
	ctx, cancel := context.WithCancel(stream.Context())
	defer cancel()

	// Receive first chunk with metadata
	req, err := stream.Recv()
	if err != nil {
		if err == io.EOF {
			log.Printf("gRPC UploadAttachment error: no metadata received - stream closed immediately")
			return status.Error(codes.InvalidArgument, "no metadata received - stream closed immediately")
		}
		log.Printf("gRPC UploadAttachment error: failed to receive metadata: %v", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to receive metadata: %v", err))
	}

	metadata := req.GetMetadata()
	if metadata == nil {
		log.Printf("gRPC UploadAttachment error: metadata is required in first chunk")
		return status.Error(codes.InvalidArgument, "metadata is required in first chunk")
	}
	
	log.Printf("gRPC UploadAttachment received: ReviewerID=%d, FeedbackID=%s, Filename=%s, Size=%d, ContentType=%s", 
		metadata.ReviewerId, metadata.FeedbackId, metadata.Filename, metadata.TotalSize, metadata.ContentType)
	
	if metadata.ReviewerId <= 0 {
		log.Printf("gRPC UploadAttachment error: reviewer_id is required")
		return status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if metadata.FeedbackId == "" {
		log.Printf("gRPC UploadAttachment error: feedback_id is required")
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if metadata.Filename == "" {
		log.Printf("gRPC UploadAttachment error: filename is required")
		return status.Error(codes.InvalidArgument, "filename is required")
	}
	if metadata.TotalSize <= 0 {
		log.Printf("gRPC UploadAttachment error: total_size must be positive")
		return status.Error(codes.InvalidArgument, "total_size must be positive")
	}

	feedbackID, err := uuid.Parse(metadata.FeedbackId)
	if err != nil {
		log.Printf("gRPC UploadAttachment error: invalid feedback ID format: %v", err)
		return status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	// Check attachment count limit
	existingAttachments, err := s.feedbackService.ListAttachments(ctx, feedbackID)
	if err != nil {
		log.Printf("gRPC UploadAttachment error: failed to check existing attachments: %v", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to check existing attachments: %v", err))
	}
	if len(existingAttachments) >= config.MaxAttachmentsPerFeedback {
		log.Printf("gRPC UploadAttachment error: maximum %d attachments allowed per feedback", config.MaxAttachmentsPerFeedback)
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
				log.Printf("gRPC UploadAttachment: upload panic: %v", r)
				uploadErrCh <- fmt.Errorf("upload panic: %v", r)
			}
		}()
		
		log.Printf("gRPC UploadAttachment: starting upload goroutine")
		err := s.feedbackService.UploadAttachment(ctx, feedbackID, metadata.Filename, metadata.ContentType, pipeReader, metadata.TotalSize)
		log.Printf("gRPC UploadAttachment: upload goroutine finished with error: %v", err)
		uploadErrCh <- err
	}()

	// Stream data from client to pipe
	var totalReceived int64
	var streamErr error

	// Check if the first request also contains chunk data
	firstChunk := req.GetChunk()
	
	// Read chunks from stream
	log.Printf("gRPC UploadAttachment: starting to read chunks from stream")
	
	func() {
		// Ensure pipe writer is closed when we exit this function
		defer func() {
			log.Printf("gRPC UploadAttachment: closing pipe writer")
			if closeErr := pipeWriter.Close(); closeErr != nil {
				log.Printf("gRPC UploadAttachment: error closing pipe writer: %v", closeErr)
				if streamErr == nil {
					streamErr = fmt.Errorf("failed to close pipe writer: %v", closeErr)
				}
			}
		}()

		// Handle first chunk if it exists
		if firstChunk != nil && len(firstChunk) > 0 {
			log.Printf("gRPC UploadAttachment: processing first chunk with %d bytes", len(firstChunk))
			
			// Validate total size
			if int64(len(firstChunk)) > metadata.TotalSize {
				streamErr = fmt.Errorf("first chunk larger than total size: %d > %d", len(firstChunk), metadata.TotalSize)
				log.Printf("gRPC UploadAttachment: size validation failed: %v", streamErr)
				return
			}

			// Write first chunk to pipe
			n, writeErr := pipeWriter.Write(firstChunk)
			if writeErr != nil {
				streamErr = fmt.Errorf("failed to write first chunk: %v", writeErr)
				log.Printf("gRPC UploadAttachment: write error: %v", writeErr)
				return
			}
			totalReceived += int64(n)
			log.Printf("gRPC UploadAttachment: written first chunk %d bytes, total received: %d/%d", n, totalReceived, metadata.TotalSize)
			
			// Check if we've received all expected data from first chunk
			if totalReceived >= metadata.TotalSize {
				log.Printf("gRPC UploadAttachment: received all expected data from first chunk (%d bytes), ending stream", totalReceived)
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

			log.Printf("gRPC UploadAttachment: waiting for next chunk...")
			req, err := stream.Recv()
			if err == io.EOF {
				// End of stream - this is expected and normal
				log.Printf("gRPC UploadAttachment: received EOF, stream ended normally. Total received: %d bytes", totalReceived)
				return
			}
			if err != nil {
				// Check if it's a context cancellation error
				if ctx.Err() != nil {
					streamErr = fmt.Errorf("stream cancelled: %v", ctx.Err())
				} else {
					streamErr = fmt.Errorf("failed to receive chunk: %v", err)
				}
				log.Printf("gRPC UploadAttachment: error receiving chunk: %v", err)
				return
			}

			chunk := req.GetChunk()
			if chunk == nil {
				// Skip empty chunks - this means we got a metadata packet or empty chunk
				log.Printf("gRPC UploadAttachment: received empty chunk or metadata packet, skipping")
				continue
			}

			log.Printf("gRPC UploadAttachment: received chunk of %d bytes", len(chunk))

			// Validate total size
			if totalReceived+int64(len(chunk)) > metadata.TotalSize {
				streamErr = fmt.Errorf("received more data than expected: %d + %d > %d", totalReceived, len(chunk), metadata.TotalSize)
				log.Printf("gRPC UploadAttachment: size validation failed: %v", streamErr)
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
				log.Printf("gRPC UploadAttachment: write error: %v", writeErr)
				return
			}
			totalReceived += int64(n)
			log.Printf("gRPC UploadAttachment: written %d bytes, total received: %d/%d", n, totalReceived, metadata.TotalSize)
			
			// Check if we've received all expected data
			if totalReceived >= metadata.TotalSize {
				log.Printf("gRPC UploadAttachment: received all expected data (%d bytes), ending stream", totalReceived)
				return
			}
		}
	}()

	// Check for streaming errors
	if streamErr != nil {
		log.Printf("gRPC UploadAttachment: stream error detected: %v", streamErr)
		cancel() // Cancel main context to stop upload
		// Wait for upload to finish with a timeout
		select {
		case uploadErr := <-uploadErrCh:
			// Upload finished
			log.Printf("gRPC UploadAttachment: upload finished after stream error with result: %v", uploadErr)
		case <-time.After(5 * time.Second):
			// Timeout waiting for upload to finish
			log.Printf("gRPC UploadAttachment: timeout waiting for upload to finish after stream error")
		}
		return status.Error(codes.Internal, streamErr.Error())
	}

	// Validate that we received all expected data
	if totalReceived != metadata.TotalSize {
		log.Printf("gRPC UploadAttachment: size mismatch - received %d bytes, expected %d bytes", totalReceived, metadata.TotalSize)
		cancel() // Cancel main context to stop upload
		return status.Error(codes.InvalidArgument, fmt.Sprintf("received %d bytes, expected %d bytes", totalReceived, metadata.TotalSize))
	}

	log.Printf("gRPC UploadAttachment: waiting for upload to complete...")
	// Wait for upload to complete
	select {
	case uploadErr := <-uploadErrCh:
		if uploadErr != nil {
			log.Printf("gRPC UploadAttachment: upload failed: %v", uploadErr)
			return status.Error(codes.Internal, fmt.Sprintf("failed to upload attachment: %v", uploadErr))
		}
		log.Printf("gRPC UploadAttachment: upload completed successfully")
	case <-ctx.Done():
		log.Printf("gRPC UploadAttachment: upload cancelled due to context")
		return status.Error(codes.Canceled, "upload cancelled")
	case <-time.After(30 * time.Second): // Add reasonable timeout
		log.Printf("gRPC UploadAttachment: upload timed out")
		cancel() // Cancel context to stop any ongoing operations
		return status.Error(codes.DeadlineExceeded, "upload timeout")
	}

	log.Printf("gRPC UploadAttachment response: filename=%s, size=%d", metadata.Filename, totalReceived)
	return stream.SendAndClose(&pb.UploadAttachmentResponse{
		Filename: metadata.Filename,
		Size:     totalReceived,
		Success:  true,
	})
}

// DeleteAttachment deletes an attachment (reviewer only)
func (s *FeedbackServer) DeleteAttachment(ctx context.Context, req *pb.DeleteAttachmentRequest) (*pb.DeleteAttachmentResponse, error) {
	log.Printf("gRPC DeleteAttachment received: ReviewerId=%d, FeedbackId=%s, Filename=%s", req.ReviewerId, req.FeedbackId, req.Filename)

	// Validate request
	if req.ReviewerId <= 0 {
		log.Printf("gRPC DeleteAttachment error: reviewer_id is required")
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.FeedbackId == "" {
		log.Printf("gRPC DeleteAttachment error: feedback_id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		log.Printf("gRPC DeleteAttachment error: filename is required")
		return nil, status.Error(codes.InvalidArgument, "filename is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		log.Printf("gRPC DeleteAttachment error: invalid feedback ID format: %v", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	err = s.feedbackService.DeleteAttachment(ctx, feedbackID, req.Filename)
	if err != nil {
		log.Printf("gRPC DeleteAttachment error: failed to delete attachment: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete attachment: %v", err))
	}

	response := &pb.DeleteAttachmentResponse{Success: true}
	log.Printf("gRPC DeleteAttachment response: Success=%t", response.Success)
	return response, nil
}

// DownloadAttachment downloads an attachment (both roles)
func (s *FeedbackServer) DownloadAttachment(req *pb.DownloadAttachmentRequest, stream pb.FeedbackService_DownloadAttachmentServer) error {
	log.Printf("gRPC DownloadAttachment received: FeedbackId=%s, Filename=%s", req.FeedbackId, req.Filename)

	if req.FeedbackId == "" {
		log.Printf("gRPC DownloadAttachment error: feedback_id is required")
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		log.Printf("gRPC DownloadAttachment error: filename is required")
		return status.Error(codes.InvalidArgument, "filename is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		log.Printf("gRPC DownloadAttachment error: invalid feedback ID format: %v", err)
		return status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	// Get attachment info first
	attachments, err := s.feedbackService.ListAttachments(stream.Context(), feedbackID)
	if err != nil {
		log.Printf("gRPC DownloadAttachment error: failed to get attachment info: %v", err)
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
		log.Printf("gRPC DownloadAttachment error: attachment not found for filename=%s", req.Filename)
		return status.Error(codes.NotFound, "attachment not found")
	}

	log.Printf("gRPC DownloadAttachment: found attachment - filename=%s, size=%d, contentType=%s", attachmentInfo.Filename, attachmentInfo.Size, attachmentInfo.ContentType)

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
		log.Printf("gRPC DownloadAttachment error: failed to send attachment info: %v", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to send attachment info: %v", err))
	}

	// Download and stream file content
	reader, _, err := s.feedbackService.DownloadAttachment(stream.Context(), feedbackID, req.Filename)
	if err != nil {
		log.Printf("gRPC DownloadAttachment error: failed to download attachment: %v", err)
		return status.Error(codes.Internal, fmt.Sprintf("failed to download attachment: %v", err))
	}
	defer reader.Close()

	log.Printf("gRPC DownloadAttachment: starting to stream file content")
	var totalSent int64
	buffer := make([]byte, 32*1024) // 32KB chunks
	for {
		n, err := reader.Read(buffer)
		if err == io.EOF {
			break
		}
		if err != nil {
			log.Printf("gRPC DownloadAttachment error: failed to read attachment: %v", err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to read attachment: %v", err))
		}

		err = stream.Send(&pb.DownloadAttachmentResponse{
			Data: &pb.DownloadAttachmentResponse_Chunk{
				Chunk: buffer[:n],
			},
		})
		if err != nil {
			log.Printf("gRPC DownloadAttachment error: failed to send chunk: %v", err)
			return status.Error(codes.Internal, fmt.Sprintf("failed to send chunk: %v", err))
		}
		totalSent += int64(n)
	}

	log.Printf("gRPC DownloadAttachment response: successfully streamed %d bytes", totalSent)
	return nil
}

// ListAttachments lists attachments for a feedback (both roles)
func (s *FeedbackServer) ListAttachments(ctx context.Context, req *pb.ListAttachmentsRequest) (*pb.ListAttachmentsResponse, error) {
	log.Printf("gRPC ListAttachments received: FeedbackId=%s", req.FeedbackId)

	if req.FeedbackId == "" {
		log.Printf("gRPC ListAttachments error: feedback_id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		log.Printf("gRPC ListAttachments error: invalid feedback ID format: %v", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	attachments, err := s.feedbackService.ListAttachments(ctx, feedbackID)
	if err != nil {
		log.Printf("gRPC ListAttachments error: failed to list attachments: %v", err)
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
	log.Printf("gRPC ListAttachments response: %d attachments found", len(attachments))
	return response, nil
}

// GetAttachmentLocation returns location information for attachments (both roles)
func (s *FeedbackServer) GetAttachmentLocation(ctx context.Context, req *pb.GetAttachmentLocationRequest) (*pb.GetAttachmentLocationResponse, error) {
	log.Printf("gRPC GetAttachmentLocation received: FeedbackId=%s, Filename=%v", req.FeedbackId, req.Filename)

	if req.FeedbackId == "" {
		log.Printf("gRPC GetAttachmentLocation error: feedback_id is required")
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		log.Printf("gRPC GetAttachmentLocation error: invalid feedback ID format: %v", err)
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	var locationInfos []*models.AttachmentLocationInfo

	if req.Filename != nil && *req.Filename != "" {
		log.Printf("gRPC GetAttachmentLocation: getting location for specific file: %s", *req.Filename)
		// Get location info for specific attachment
		locationInfo, err := s.feedbackService.GetAttachmentLocation(ctx, feedbackID, *req.Filename)
		if err != nil {
			log.Printf("gRPC GetAttachmentLocation error: failed to get attachment location: %v", err)
			return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get attachment location: %v", err))
		}
		locationInfos = []*models.AttachmentLocationInfo{locationInfo}
	} else {
		log.Printf("gRPC GetAttachmentLocation: getting locations for all attachments")
		// Get location info for all attachments
		infos, err := s.feedbackService.ListAttachmentLocations(ctx, feedbackID)
		if err != nil {
			log.Printf("gRPC GetAttachmentLocation error: failed to list attachment locations: %v", err)
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
	log.Printf("gRPC GetAttachmentLocation response: %d attachment locations returned", len(locationInfos))
	return response, nil
}
