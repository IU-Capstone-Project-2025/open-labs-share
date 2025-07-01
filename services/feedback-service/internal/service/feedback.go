package service

import (
	"context"
	"fmt"
	"io"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/repository"
	"github.com/google/uuid"
)

// FeedbackService handles feedback business logic
type FeedbackService struct {
	feedbackRepo    repository.FeedbackRepository
	attachmentRepo  repository.AttachmentRepository
}

// NewFeedbackService creates a new feedback service
func NewFeedbackService(feedbackRepo repository.FeedbackRepository, attachmentRepo repository.AttachmentRepository) *FeedbackService {
	return &FeedbackService{
		feedbackRepo:   feedbackRepo,
		attachmentRepo: attachmentRepo,
	}
}

// CreateFeedback creates a new feedback entry (reviewer only)
func (s *FeedbackService) CreateFeedback(ctx context.Context, reviewerID, studentID, submissionID int64, title, content string) (*models.Feedback, error) {
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
		return nil, fmt.Errorf("failed to create feedback: %w", err)
	}

	return feedback, nil
}

// UpdateFeedback updates an existing feedback entry (reviewer only)
func (s *FeedbackService) UpdateFeedback(ctx context.Context, id uuid.UUID, reviewerID int64, title, content *string) (*models.Feedback, error) {
	if id == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}
	if reviewerID <= 0 {
		return nil, fmt.Errorf("invalid reviewer ID")
	}

	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	// Check if the reviewer is the author
	if !feedback.CanModify(reviewerID) {
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
		return nil, fmt.Errorf("failed to update feedback: %w", err)
	}

	return feedback, nil
}

// DeleteFeedback deletes a feedback entry (reviewer only)
func (s *FeedbackService) DeleteFeedback(ctx context.Context, id uuid.UUID, reviewerID int64) error {
	if id == uuid.Nil {
		return fmt.Errorf("invalid feedback ID")
	}
	if reviewerID <= 0 {
		return fmt.Errorf("invalid reviewer ID")
	}

	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		return fmt.Errorf("failed to get feedback: %w", err)
	}

	// Check if the reviewer is the author
	if !feedback.CanModify(reviewerID) {
		return fmt.Errorf("access denied: only the feedback author can delete it")
	}

	// Delete feedback
	if err := s.feedbackRepo.Delete(ctx, id); err != nil {
		return fmt.Errorf("failed to delete feedback: %w", err)
	}

	return nil
}

// GetStudentFeedback retrieves feedback for a student by submission ID
func (s *FeedbackService) GetStudentFeedback(ctx context.Context, studentID, submissionID int64) ([]*models.Feedback, error) {
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
		return nil, fmt.Errorf("failed to get student feedback: %w", err)
	}

	return feedbacks, nil
}

// ListReviewerFeedbacks lists feedbacks created by a specific reviewer
func (s *FeedbackService) ListReviewerFeedbacks(ctx context.Context, reviewerID int64, submissionID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
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
		return nil, 0, fmt.Errorf("failed to list reviewer feedbacks: %w", err)
	}

	return feedbacks, int32(totalCount), nil
}

// ListStudentFeedbacks lists feedbacks for a specific student
func (s *FeedbackService) ListStudentFeedbacks(ctx context.Context, studentID int64, submissionID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
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
		return nil, 0, fmt.Errorf("failed to list student feedbacks: %w", err)
	}

	return feedbacks, int32(totalCount), nil
}

// UploadAttachment uploads an attachment file for a feedback
func (s *FeedbackService) UploadAttachment(ctx context.Context, feedbackID uuid.UUID, filename, contentType string, data io.Reader, size int64) error {
	if feedbackID == uuid.Nil {
		return fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return fmt.Errorf("filename is required")
	}
	if size <= 0 {
		return fmt.Errorf("invalid file size")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Upload attachment
	if err := s.attachmentRepo.Upload(ctx, feedbackID, filename, contentType, data, size); err != nil {
		return fmt.Errorf("failed to upload attachment: %w", err)
	}

	return nil
}

// DownloadAttachment downloads an attachment file for a feedback
func (s *FeedbackService) DownloadAttachment(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *models.AttachmentInfo, error) {
	if feedbackID == uuid.Nil {
		return nil, nil, fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return nil, nil, fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return nil, nil, fmt.Errorf("feedback not found: %w", err)
	}

	// Download attachment
	reader, attachmentInfo, err := s.attachmentRepo.Download(ctx, feedbackID, filename)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to download attachment: %w", err)
	}

	return reader, attachmentInfo, nil
}

// ListAttachments lists all attachments for a feedback
func (s *FeedbackService) ListAttachments(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentInfo, error) {
	if feedbackID == uuid.Nil {
		return nil, fmt.Errorf("invalid feedback ID")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// List attachments
	attachments, err := s.attachmentRepo.List(ctx, feedbackID)
	if err != nil {
		return nil, fmt.Errorf("failed to list attachments: %w", err)
	}

	return attachments, nil
}

// DeleteAttachment deletes a specific attachment
func (s *FeedbackService) DeleteAttachment(ctx context.Context, feedbackID uuid.UUID, filename string) error {
	if feedbackID == uuid.Nil {
		return fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		return fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Delete attachment
	if err := s.attachmentRepo.Delete(ctx, feedbackID, filename); err != nil {
		return fmt.Errorf("failed to delete attachment: %w", err)
	}

	return nil
}
