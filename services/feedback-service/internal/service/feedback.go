package service

import (
	"context"
	"fmt"
	"io"
	"log/slog"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/repository"
	"github.com/google/uuid"
)

// FeedbackService handles feedback business logic
type FeedbackService struct {
	feedbackRepo   repository.FeedbackRepository
	attachmentRepo repository.AttachmentRepository
	logger         *slog.Logger
}

// NewFeedbackService creates a new feedback service
func NewFeedbackService(feedbackRepo repository.FeedbackRepository, attachmentRepo repository.AttachmentRepository, logger *slog.Logger) *FeedbackService {
	return &FeedbackService{
		feedbackRepo:   feedbackRepo,
		attachmentRepo: attachmentRepo,
		logger:         logger,
	}
}

// CreateFeedback creates a new feedback entry (reviewer only)
func (s *FeedbackService) CreateFeedback(ctx context.Context, reviewerID, studentID, submissionID int64, title, content string) (*models.Feedback, error) {
	s.logger.Info("Creating new feedback",
		"reviewer_id", reviewerID,
		"student_id", studentID,
		"submission_id", submissionID,
		"title", title,
	)

	// Validate input
	if reviewerID <= 0 {
		return nil, fmt.Errorf("invalid reviewer ID")
	}
	if studentID <= 0 {
		return nil, fmt.Errorf("invalid student ID")
	}
	if submissionID <= 0 {
		return nil, fmt.Errorf("invalid submission ID")
	}
	if title == "" {
		return nil, fmt.Errorf("title is required")
	}

	// Create feedback entry
	feedback := &models.Feedback{
		ReviewerID:   reviewerID,
		StudentID:    studentID,
		SubmissionID: submissionID,
		Title:        title,
		Content:      content,
	}

	// Save to repository (handles both PostgreSQL and MongoDB)
	if err := s.feedbackRepo.Create(ctx, feedback); err != nil {
		s.logger.Error("Failed to create feedback", "error", err)
		return nil, fmt.Errorf("failed to create feedback: %w", err)
	}

	s.logger.Info("Feedback created successfully", "feedback_id", feedback.ID)
	return feedback, nil
}

// UpdateFeedback updates an existing feedback entry (reviewer only)
func (s *FeedbackService) UpdateFeedback(ctx context.Context, id uuid.UUID, reviewerID int64, title, content *string) (*models.Feedback, error) {
	s.logger.Info("Updating feedback",
		"feedback_id", id,
		"reviewer_id", reviewerID,
	)

	if id == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}
	if reviewerID <= 0 {
		return nil, fmt.Errorf("invalid reviewer ID")
	}

	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		s.logger.Error("Failed to get feedback for update", "feedback_id", id, "error", err)
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	// Check if the reviewer is the author
	if !feedback.CanModify(reviewerID) {
		s.logger.Warn("Access denied to update feedback",
			"feedback_id", id,
			"owner_id", feedback.ReviewerID,
			"attempted_by_id", reviewerID,
		)
		return nil, fmt.Errorf("access denied: only the feedback author can update it")
	}

	// Update fields if provided
	if title != nil {
		feedback.Title = *title
	}
	if content != nil {
		feedback.Content = *content
	}

	// Save changes
	if err := s.feedbackRepo.Update(ctx, feedback); err != nil {
		s.logger.Error("Failed to update feedback", "feedback_id", id, "error", err)
		return nil, fmt.Errorf("failed to update feedback: %w", err)
	}

	s.logger.Info("Feedback updated successfully", "feedback_id", id)
	return feedback, nil
}

// DeleteFeedback deletes a feedback entry (reviewer only)
func (s *FeedbackService) DeleteFeedback(ctx context.Context, id uuid.UUID, reviewerID int64) error {
	s.logger.Info("Deleting feedback",
		"feedback_id", id,
		"reviewer_id", reviewerID,
	)

	if id == uuid.Nil {
		return fmt.Errorf("invalid feedback ID")
	}
	if reviewerID <= 0 {
		return fmt.Errorf("invalid reviewer ID")
	}

	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		s.logger.Error("Failed to get feedback for deletion", "feedback_id", id, "error", err)
		return fmt.Errorf("failed to get feedback: %w", err)
	}

	// Check if the reviewer is the author
	if !feedback.CanModify(reviewerID) {
		s.logger.Warn("Access denied to delete feedback",
			"feedback_id", id,
			"owner_id", feedback.ReviewerID,
			"attempted_by_id", reviewerID,
		)
		return fmt.Errorf("access denied: only the feedback author can delete it")
	}

	// Delete feedback and all associated attachments
	if err := s.attachmentRepo.DeleteAll(ctx, id); err != nil {
		s.logger.Error("Failed to delete feedback attachments", "feedback_id", id, "error", err)
		return fmt.Errorf("failed to delete feedback attachments: %w", err)
	}

	if err := s.feedbackRepo.Delete(ctx, id); err != nil {
		s.logger.Error("Failed to delete feedback from repository", "feedback_id", id, "error", err)
		return fmt.Errorf("failed to delete feedback: %w", err)
	}

	s.logger.Info("Feedback deleted successfully", "feedback_id", id)
	return nil
}

// GetStudentFeedback retrieves feedback for a student by submission ID
func (s *FeedbackService) GetStudentFeedback(ctx context.Context, studentID, submissionID int64) ([]*models.Feedback, error) {
	s.logger.Info("Getting student feedback",
		"student_id", studentID,
		"submission_id", submissionID,
	)

	if studentID <= 0 {
		return nil, fmt.Errorf("invalid student ID")
	}
	if submissionID <= 0 {
		return nil, fmt.Errorf("invalid submission ID")
	}

	filter := models.FeedbackFilter{
		StudentID:    &studentID,
		SubmissionID: &submissionID,
		Page:         1,
		Limit:        100, // Get all feedback for this submission
	}

	feedbacks, _, err := s.feedbackRepo.ListByStudent(ctx, filter)
	if err != nil {
		s.logger.Error("Failed to get student feedback",
			"student_id", studentID,
			"submission_id", submissionID,
			"error", err,
		)
		return nil, fmt.Errorf("failed to get student feedback: %w", err)
	}

	s.logger.Info("Student feedback retrieved successfully",
		"student_id", studentID,
		"submission_id", submissionID,
		"count", len(feedbacks),
	)
	return feedbacks, nil
}

// ListReviewerFeedbacks lists feedbacks created by a specific reviewer
func (s *FeedbackService) ListReviewerFeedbacks(ctx context.Context, reviewerID int64, submissionID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
	s.logger.Info("Listing reviewer feedbacks",
		"reviewer_id", reviewerID,
		"submission_id", submissionID,
		"page", page,
		"limit", limit,
	)

	if reviewerID <= 0 {
		return nil, 0, fmt.Errorf("invalid reviewer ID")
	}
	if page < 1 {
		page = 1
	}
	if limit < 1 || limit > 100 {
		limit = 20
	}

	filter := models.FeedbackFilter{
		ReviewerID:   &reviewerID,
		SubmissionID: submissionID,
		Page:         int(page),
		Limit:        int(limit),
	}

	feedbacks, totalCount, err := s.feedbackRepo.ListByUser(ctx, filter)
	if err != nil {
		s.logger.Error("Failed to list reviewer feedbacks", "reviewer_id", reviewerID, "error", err)
		return nil, 0, fmt.Errorf("failed to list reviewer feedbacks: %w", err)
	}

	s.logger.Info("Reviewer feedbacks listed successfully",
		"reviewer_id", reviewerID,
		"count", len(feedbacks),
		"total_count", totalCount,
	)
	return feedbacks, int32(totalCount), nil
}

// ListStudentFeedbacks lists feedbacks for a specific student
func (s *FeedbackService) ListStudentFeedbacks(ctx context.Context, studentID int64, submissionID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
	s.logger.Info("Listing student feedbacks",
		"student_id", studentID,
		"submission_id", submissionID,
		"page", page,
		"limit", limit,
	)

	if studentID <= 0 {
		return nil, 0, fmt.Errorf("invalid student ID")
	}
	if page < 1 {
		page = 1
	}
	if limit < 1 || limit > 100 {
		limit = 20
	}

	filter := models.FeedbackFilter{
		StudentID:    &studentID,
		SubmissionID: submissionID,
		Page:         int(page),
		Limit:        int(limit),
	}

	feedbacks, totalCount, err := s.feedbackRepo.ListByStudent(ctx, filter)
	if err != nil {
		s.logger.Error("Failed to list student feedbacks", "student_id", studentID, "error", err)
		return nil, 0, fmt.Errorf("failed to list student feedbacks: %w", err)
	}

	s.logger.Info("Student feedbacks listed successfully",
		"student_id", studentID,
		"count", len(feedbacks),
		"total_count", totalCount,
	)
	return feedbacks, int32(totalCount), nil
}

// UploadAttachment uploads an attachment file for a feedback
func (s *FeedbackService) UploadAttachment(ctx context.Context, feedbackID uuid.UUID, filename, contentType string, data io.Reader, size int64) error {
	s.logger.Info("Uploading attachment",
		"feedback_id", feedbackID,
		"filename", filename,
		"content_type", contentType,
		"size", size,
	)

	if feedbackID == uuid.Nil {
		return fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return fmt.Errorf("filename is required")
	}
	if size <= 0 {
		return fmt.Errorf("invalid file size")
	}

	// Check if context is already cancelled
	select {
	case <-ctx.Done():
		return fmt.Errorf("upload cancelled: %w", ctx.Err())
	default:
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Feedback not found for attachment upload", "feedback_id", feedbackID, "error", err)
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Upload attachment with context monitoring
	if err := s.attachmentRepo.Upload(ctx, feedbackID, filename, contentType, data, size); err != nil {
		s.logger.Error("Failed to upload attachment",
			"feedback_id", feedbackID,
			"filename", filename,
			"error", err,
		)
		return fmt.Errorf("failed to upload attachment: %w", err)
	}

	s.logger.Info("Attachment uploaded successfully",
		"feedback_id", feedbackID,
		"filename", filename,
	)
	return nil
}

// DownloadAttachment downloads an attachment file for a feedback
func (s *FeedbackService) DownloadAttachment(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *models.AttachmentInfo, error) {
	s.logger.Info("Downloading attachment",
		"feedback_id", feedbackID,
		"filename", filename,
	)

	if feedbackID == uuid.Nil {
		return nil, nil, fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return nil, nil, fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Feedback not found for attachment download", "feedback_id", feedbackID, "error", err)
		return nil, nil, fmt.Errorf("feedback not found: %w", err)
	}

	// Download attachment
	reader, attachmentInfo, err := s.attachmentRepo.Download(ctx, feedbackID, filename)
	if err != nil {
		s.logger.Error("Failed to download attachment",
			"feedback_id", feedbackID,
			"filename", filename,
			"error", err,
		)
		return nil, nil, fmt.Errorf("failed to download attachment: %w", err)
	}

	s.logger.Info("Attachment download started",
		"feedback_id", feedbackID,
		"filename", filename,
		"size", attachmentInfo.Size,
		"content_type", attachmentInfo.ContentType,
	)
	return reader, attachmentInfo, nil
}

// ListAttachments lists all attachments for a feedback
func (s *FeedbackService) ListAttachments(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentInfo, error) {
	s.logger.Info("Listing attachments", "feedback_id", feedbackID)

	if feedbackID == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Feedback not found for listing attachments", "feedback_id", feedbackID, "error", err)
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// List attachments
	attachments, err := s.attachmentRepo.List(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Failed to list attachments", "feedback_id", feedbackID, "error", err)
		return nil, fmt.Errorf("failed to list attachments: %w", err)
	}

	s.logger.Info("Attachments listed successfully",
		"feedback_id", feedbackID,
		"count", len(attachments),
	)
	return attachments, nil
}

// DeleteAttachment deletes a specific attachment
func (s *FeedbackService) DeleteAttachment(ctx context.Context, feedbackID uuid.UUID, filename string) error {
	s.logger.Info("Deleting attachment",
		"feedback_id", feedbackID,
		"filename", filename,
	)

	if feedbackID == uuid.Nil {
		return fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Feedback not found for attachment deletion", "feedback_id", feedbackID, "error", err)
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Delete attachment
	if err := s.attachmentRepo.Delete(ctx, feedbackID, filename); err != nil {
		s.logger.Error("Failed to delete attachment",
			"feedback_id", feedbackID,
			"filename", filename,
			"error", err,
		)
		return fmt.Errorf("failed to delete attachment: %w", err)
	}

	s.logger.Info("Attachment deleted successfully",
		"feedback_id", feedbackID,
		"filename", filename,
	)
	return nil
}

// GetFeedbackByID retrieves feedback by its ID
func (s *FeedbackService) GetFeedbackByID(ctx context.Context, id uuid.UUID) (*models.Feedback, error) {
	s.logger.Info("Getting feedback by ID", "feedback_id", id)

	if id == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}

	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		s.logger.Error("Failed to get feedback by ID", "feedback_id", id, "error", err)
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	s.logger.Info("Feedback retrieved successfully by ID", "feedback_id", id)
	return feedback, nil
}

// GetAttachmentLocation gets location information for a specific attachment
func (s *FeedbackService) GetAttachmentLocation(ctx context.Context, feedbackID uuid.UUID, filename string) (*models.AttachmentLocationInfo, error) {
	s.logger.Info("Getting attachment location",
		"feedback_id", feedbackID,
		"filename", filename,
	)

	if feedbackID == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return nil, fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Feedback not found for getting attachment location", "feedback_id", feedbackID, "error", err)
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// Get attachment location info
	locationInfo, err := s.attachmentRepo.GetLocationInfo(ctx, feedbackID, filename)
	if err != nil {
		s.logger.Error("Failed to get attachment location",
			"feedback_id", feedbackID,
			"filename", filename,
			"error", err,
		)
		return nil, fmt.Errorf("failed to get attachment location: %w", err)
	}

	s.logger.Info("Attachment location retrieved successfully",
		"feedback_id", feedbackID,
		"filename", filename,
	)
	return locationInfo, nil
}

// ListAttachmentLocations lists location information for all attachments of a feedback
func (s *FeedbackService) ListAttachmentLocations(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentLocationInfo, error) {
	s.logger.Info("Listing attachment locations", "feedback_id", feedbackID)

	if feedbackID == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Feedback not found for listing attachment locations", "feedback_id", feedbackID, "error", err)
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// List attachment location info
	locationInfos, err := s.attachmentRepo.ListLocationInfo(ctx, feedbackID)
	if err != nil {
		s.logger.Error("Failed to list attachment locations", "feedback_id", feedbackID, "error", err)
		return nil, fmt.Errorf("failed to list attachment locations: %w", err)
	}

	s.logger.Info("Attachment locations listed successfully",
		"feedback_id", feedbackID,
		"count", len(locationInfos),
	)
	return locationInfos, nil
}
