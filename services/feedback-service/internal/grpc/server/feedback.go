package server

import (
	"context"
	"fmt"
	"io"

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
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to create feedback: %v", err))
	}

	return convertToProtoFeedback(feedback), nil
}

// UpdateFeedback updates an existing feedback (reviewer only)
func (s *FeedbackServer) UpdateFeedback(ctx context.Context, req *pb.UpdateFeedbackRequest) (*pb.Feedback, error) {
	// Validate request
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback ID is required")
	}

	id, err := uuid.Parse(req.Id)
	if err != nil {
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
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to update feedback: %v", err))
	}

	return convertToProtoFeedback(feedback), nil
}

// DeleteFeedback deletes a feedback (reviewer only)
func (s *FeedbackServer) DeleteFeedback(ctx context.Context, req *pb.DeleteFeedbackRequest) (*pb.DeleteFeedbackResponse, error) {
	// Validate request
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback ID is required")
	}

	id, err := uuid.Parse(req.Id)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	err = s.feedbackService.DeleteFeedback(ctx, id, req.ReviewerId)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete feedback: %v", err))
	}

	return &pb.DeleteFeedbackResponse{Success: true}, nil
}

// ListReviewerFeedbacks lists feedbacks created by a reviewer
func (s *FeedbackServer) ListReviewerFeedbacks(ctx context.Context, req *pb.ListReviewerFeedbacksRequest) (*pb.ListReviewerFeedbacksResponse, error) {
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}

	var submissionID *int64
	if req.SubmissionId != nil {
		submissionID = req.SubmissionId
	}

	feedbacks, totalCount, err := s.feedbackService.ListReviewerFeedbacks(ctx, req.ReviewerId, submissionID, req.Page, req.Limit)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list reviewer feedbacks: %v", err))
	}

	pbFeedbacks := make([]*pb.Feedback, len(feedbacks))
	for i, feedback := range feedbacks {
		pbFeedbacks[i] = convertToProtoFeedback(feedback)
	}

	return &pb.ListReviewerFeedbacksResponse{
		Feedbacks:  pbFeedbacks,
		TotalCount: totalCount,
	}, nil
}

// Student Operations

// GetStudentFeedback retrieves feedback for a student by submission
func (s *FeedbackServer) GetStudentFeedback(ctx context.Context, req *pb.GetStudentFeedbackRequest) (*pb.Feedback, error) {
	if req.StudentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}
	if req.SubmissionId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "submission_id is required")
	}

	feedbacks, err := s.feedbackService.GetStudentFeedback(ctx, req.StudentId, req.SubmissionId)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get student feedback: %v", err))
	}

	if len(feedbacks) == 0 {
		return nil, status.Error(codes.NotFound, "no feedback found for this submission")
	}

	// Return the first feedback (assuming one feedback per submission)
	// If multiple feedbacks are expected, this should be changed to return all
	return convertToProtoFeedback(feedbacks[0]), nil
}

// ListStudentFeedbacks lists all feedbacks for a student
func (s *FeedbackServer) ListStudentFeedbacks(ctx context.Context, req *pb.ListStudentFeedbacksRequest) (*pb.ListStudentFeedbacksResponse, error) {
	if req.StudentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "student_id is required")
	}

	var submissionID *int64
	if req.SubmissionId != nil {
		submissionID = req.SubmissionId
	}

	feedbacks, totalCount, err := s.feedbackService.ListStudentFeedbacks(ctx, req.StudentId, submissionID, req.Page, req.Limit)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list student feedbacks: %v", err))
	}

	pbFeedbacks := make([]*pb.Feedback, len(feedbacks))
	for i, feedback := range feedbacks {
		pbFeedbacks[i] = convertToProtoFeedback(feedback)
	}

	return &pb.ListStudentFeedbacksResponse{
		Feedbacks:  pbFeedbacks,
		TotalCount: totalCount,
	}, nil
}

// GetFeedbackById retrieves feedback by its ID
func (s *FeedbackServer) GetFeedbackById(ctx context.Context, req *pb.GetFeedbackByIdRequest) (*pb.Feedback, error) {
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback id is required")
	}

	feedbackID, err := uuid.Parse(req.Id)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	feedback, err := s.feedbackService.GetFeedbackByID(ctx, feedbackID)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get feedback: %v", err))
	}

	if feedback == nil {
		return nil, status.Error(codes.NotFound, "feedback not found")
	}

	return convertToProtoFeedback(feedback), nil
}

// Attachment Operations

// UploadAttachment uploads an attachment to a feedback (reviewer only)
func (s *FeedbackServer) UploadAttachment(stream pb.FeedbackService_UploadAttachmentServer) error {
	// Receive first chunk with metadata
	req, err := stream.Recv()
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to receive metadata: %v", err))
	}

	metadata := req.GetMetadata()
	if metadata == nil {
		return status.Error(codes.InvalidArgument, "metadata is required in first chunk")
	}
	if metadata.ReviewerId <= 0 {
		return status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if metadata.FeedbackId == "" {
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if metadata.Filename == "" {
		return status.Error(codes.InvalidArgument, "filename is required")
	}
	if metadata.TotalSize <= 0 {
		return status.Error(codes.InvalidArgument, "total_size must be positive")
	}

	feedbackID, err := uuid.Parse(metadata.FeedbackId)
	if err != nil {
		return status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	// Check attachment count limit
	existingAttachments, err := s.feedbackService.ListAttachments(stream.Context(), feedbackID)
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to check existing attachments: %v", err))
	}
	if len(existingAttachments) >= config.MaxAttachmentsPerFeedback {
		return status.Error(codes.FailedPrecondition, fmt.Sprintf("maximum %d attachments allowed per feedback", config.MaxAttachmentsPerFeedback))
	}

	// Create pipe for streaming data
	pipeReader, pipeWriter := io.Pipe()
	defer pipeReader.Close()

	// Upload in goroutine
	uploadErrCh := make(chan error, 1)
	go func() {
		defer pipeWriter.Close()
		err := s.feedbackService.UploadAttachment(stream.Context(), feedbackID, metadata.Filename, metadata.ContentType, pipeReader, metadata.TotalSize)
		uploadErrCh <- err
	}()

	// Stream chunks
	var totalReceived int64
	for {
		req, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to receive chunk: %v", err))
		}

		chunk := req.GetChunk()
		if chunk == nil {
			continue
		}

		if _, err := pipeWriter.Write(chunk); err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to write chunk: %v", err))
		}

		totalReceived += int64(len(chunk))
		if totalReceived > metadata.TotalSize {
			return status.Error(codes.InvalidArgument, "received more data than expected")
		}
	}

	pipeWriter.Close()

	// Wait for upload to complete
	if err := <-uploadErrCh; err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to upload attachment: %v", err))
	}

	return stream.SendAndClose(&pb.UploadAttachmentResponse{
		Filename: metadata.Filename,
		Size:     totalReceived,
		Success:  true,
	})
}

// DeleteAttachment deletes an attachment (reviewer only)
func (s *FeedbackServer) DeleteAttachment(ctx context.Context, req *pb.DeleteAttachmentRequest) (*pb.DeleteAttachmentResponse, error) {
	// Validate request
	if req.ReviewerId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "reviewer_id is required")
	}
	if req.FeedbackId == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		return nil, status.Error(codes.InvalidArgument, "filename is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	err = s.feedbackService.DeleteAttachment(ctx, feedbackID, req.Filename)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete attachment: %v", err))
	}

	return &pb.DeleteAttachmentResponse{Success: true}, nil
}

// DownloadAttachment downloads an attachment (both roles)
func (s *FeedbackServer) DownloadAttachment(req *pb.DownloadAttachmentRequest, stream pb.FeedbackService_DownloadAttachmentServer) error {
	if req.FeedbackId == "" {
		return status.Error(codes.InvalidArgument, "feedback_id is required")
	}
	if req.Filename == "" {
		return status.Error(codes.InvalidArgument, "filename is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		return status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	// Get attachment info first
	attachments, err := s.feedbackService.ListAttachments(stream.Context(), feedbackID)
	if err != nil {
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
		return status.Error(codes.NotFound, "attachment not found")
	}

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
		return status.Error(codes.Internal, fmt.Sprintf("failed to send attachment info: %v", err))
	}

	// Download and stream file content
	reader, _, err := s.feedbackService.DownloadAttachment(stream.Context(), feedbackID, req.Filename)
	if err != nil {
		return status.Error(codes.Internal, fmt.Sprintf("failed to download attachment: %v", err))
	}
	defer reader.Close()

	buffer := make([]byte, 32*1024) // 32KB chunks
	for {
		n, err := reader.Read(buffer)
		if err == io.EOF {
			break
		}
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to read attachment: %v", err))
		}

		err = stream.Send(&pb.DownloadAttachmentResponse{
			Data: &pb.DownloadAttachmentResponse_Chunk{
				Chunk: buffer[:n],
			},
		})
		if err != nil {
			return status.Error(codes.Internal, fmt.Sprintf("failed to send chunk: %v", err))
		}
	}

	return nil
}

// ListAttachments lists attachments for a feedback (both roles)
func (s *FeedbackServer) ListAttachments(ctx context.Context, req *pb.ListAttachmentsRequest) (*pb.ListAttachmentsResponse, error) {
	if req.FeedbackId == "" {
		return nil, status.Error(codes.InvalidArgument, "feedback_id is required")
	}

	feedbackID, err := uuid.Parse(req.FeedbackId)
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "invalid feedback ID format")
	}

	attachments, err := s.feedbackService.ListAttachments(ctx, feedbackID)
	if err != nil {
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

	return &pb.ListAttachmentsResponse{Attachments: pbAttachments}, nil
}
