package service

import (
	"context"
	"fmt"
	"io"
	"log"

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
	log.Printf("FeedbackService CreateFeedback: ReviewerId=%d, StudentId=%d, SubmissionId=%d, Title=%q", reviewerID, studentID, submissionID, title)

	// Validate input
	if reviewerID <= 0 {
		log.Printf("FeedbackService CreateFeedback error: invalid reviewer ID %d", reviewerID)
		return nil, fmt.Errorf("invalid reviewer ID")
	}
	if studentID <= 0 {
		log.Printf("FeedbackService CreateFeedback error: invalid student ID %d", studentID)
		return nil, fmt.Errorf("invalid student ID")
	}
	if submissionID <= 0 {
		log.Printf("FeedbackService CreateFeedback error: invalid submission ID %d", submissionID)
		return nil, fmt.Errorf("invalid submission ID")
	}
	if title == "" {
		log.Printf("FeedbackService CreateFeedback error: title is required")
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
		log.Printf("FeedbackService CreateFeedback error: failed to create feedback: %v", err)
		return nil, fmt.Errorf("failed to create feedback: %w", err)
	}

	log.Printf("FeedbackService CreateFeedback success: created feedback with ID=%s", feedback.ID.String())
	return feedback, nil
}

// UpdateFeedback updates an existing feedback entry (reviewer only)
func (s *FeedbackService) UpdateFeedback(ctx context.Context, id uuid.UUID, reviewerID int64, title, content *string) (*models.Feedback, error) {
	log.Printf("FeedbackService UpdateFeedback: ID=%s, ReviewerId=%d, Title=%v, Content=%v", id.String(), reviewerID, title, content)

	if id == uuid.Nil {
		log.Printf("FeedbackService UpdateFeedback error: invalid feedback ID")
		return nil, fmt.Errorf("invalid feedback ID")
	}
	if reviewerID <= 0 {
		log.Printf("FeedbackService UpdateFeedback error: invalid reviewer ID %d", reviewerID)
		return nil, fmt.Errorf("invalid reviewer ID")
	}

	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		log.Printf("FeedbackService UpdateFeedback error: failed to get feedback: %v", err)
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	// Check if the reviewer is the author
	if !feedback.CanModify(reviewerID) {
		log.Printf("FeedbackService UpdateFeedback error: access denied - reviewer %d cannot modify feedback owned by %d", reviewerID, feedback.ReviewerID)
		return nil, fmt.Errorf("access denied: only the feedback author can update it")
	}

	// Update fields if provided
	if title != nil {
		log.Printf("FeedbackService UpdateFeedback: updating title from %q to %q", feedback.Title, *title)
		feedback.Title = *title
	}
	if content != nil {
		log.Printf("FeedbackService UpdateFeedback: updating content")
		feedback.Content = *content
	}

	// Save changes
	if err := s.feedbackRepo.Update(ctx, feedback); err != nil {
		log.Printf("FeedbackService UpdateFeedback error: failed to update feedback: %v", err)
		return nil, fmt.Errorf("failed to update feedback: %w", err)
	}

	log.Printf("FeedbackService UpdateFeedback success: updated feedback ID=%s", feedback.ID.String())
	return feedback, nil
}

// DeleteFeedback deletes a feedback entry (reviewer only)
func (s *FeedbackService) DeleteFeedback(ctx context.Context, id uuid.UUID, reviewerID int64) error {
	log.Printf("FeedbackService DeleteFeedback: ID=%s, ReviewerId=%d", id.String(), reviewerID)

	if id == uuid.Nil {
		log.Printf("FeedbackService DeleteFeedback error: invalid feedback ID")
		return fmt.Errorf("invalid feedback ID")
	}
	if reviewerID <= 0 {
		log.Printf("FeedbackService DeleteFeedback error: invalid reviewer ID %d", reviewerID)
		return fmt.Errorf("invalid reviewer ID")
	}

	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		log.Printf("FeedbackService DeleteFeedback error: failed to get feedback: %v", err)
		return fmt.Errorf("failed to get feedback: %w", err)
	}

	// Check if the reviewer is the author
	if !feedback.CanModify(reviewerID) {
		log.Printf("FeedbackService DeleteFeedback error: access denied - reviewer %d cannot delete feedback owned by %d", reviewerID, feedback.ReviewerID)
		return fmt.Errorf("access denied: only the feedback author can delete it")
	}

	// Delete feedback
	if err := s.feedbackRepo.Delete(ctx, id); err != nil {
		log.Printf("FeedbackService DeleteFeedback error: failed to delete feedback: %v", err)
		return fmt.Errorf("failed to delete feedback: %w", err)
	}

	log.Printf("FeedbackService DeleteFeedback success: deleted feedback ID=%s", id.String())
	return nil
}

// GetStudentFeedback retrieves feedback for a student by submission ID
func (s *FeedbackService) GetStudentFeedback(ctx context.Context, studentID, submissionID int64) ([]*models.Feedback, error) {
	log.Printf("FeedbackService GetStudentFeedback: StudentId=%d, SubmissionId=%d", studentID, submissionID)

	if studentID <= 0 {
		log.Printf("FeedbackService GetStudentFeedback error: invalid student ID %d", studentID)
		return nil, fmt.Errorf("invalid student ID")
	}
	if submissionID <= 0 {
		log.Printf("FeedbackService GetStudentFeedback error: invalid submission ID %d", submissionID)
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
		log.Printf("FeedbackService GetStudentFeedback error: failed to get student feedback: %v", err)
		return nil, fmt.Errorf("failed to get student feedback: %w", err)
	}

	log.Printf("FeedbackService GetStudentFeedback success: found %d feedbacks for student %d, submission %d", len(feedbacks), studentID, submissionID)
	return feedbacks, nil
}

// ListReviewerFeedbacks lists feedbacks created by a specific reviewer
func (s *FeedbackService) ListReviewerFeedbacks(ctx context.Context, reviewerID int64, submissionID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
	log.Printf("FeedbackService ListReviewerFeedbacks: ReviewerId=%d, SubmissionId=%v, Page=%d, Limit=%d", reviewerID, submissionID, page, limit)

	if reviewerID <= 0 {
		log.Printf("FeedbackService ListReviewerFeedbacks error: invalid reviewer ID %d", reviewerID)
		return nil, 0, fmt.Errorf("invalid reviewer ID")
	}
	if page < 1 {
		log.Printf("FeedbackService ListReviewerFeedbacks: adjusting page from %d to 1", page)
		page = 1
	}
	if limit < 1 || limit > 100 {
		log.Printf("FeedbackService ListReviewerFeedbacks: adjusting limit from %d to 20", limit)
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
		log.Printf("FeedbackService ListReviewerFeedbacks error: failed to list reviewer feedbacks: %v", err)
		return nil, 0, fmt.Errorf("failed to list reviewer feedbacks: %w", err)
	}

	log.Printf("FeedbackService ListReviewerFeedbacks success: found %d feedbacks for reviewer %d, totalCount=%d", len(feedbacks), reviewerID, totalCount)
	return feedbacks, int32(totalCount), nil
}

// ListStudentFeedbacks lists feedbacks for a specific student
func (s *FeedbackService) ListStudentFeedbacks(ctx context.Context, studentID int64, submissionID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
	log.Printf("FeedbackService ListStudentFeedbacks: StudentId=%d, SubmissionId=%v, Page=%d, Limit=%d", studentID, submissionID, page, limit)

	if studentID <= 0 {
		log.Printf("FeedbackService ListStudentFeedbacks error: invalid student ID %d", studentID)
		return nil, 0, fmt.Errorf("invalid student ID")
	}
	if page < 1 {
		log.Printf("FeedbackService ListStudentFeedbacks: adjusting page from %d to 1", page)
		page = 1
	}
	if limit < 1 || limit > 100 {
		log.Printf("FeedbackService ListStudentFeedbacks: adjusting limit from %d to 20", limit)
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
		log.Printf("FeedbackService ListStudentFeedbacks error: failed to list student feedbacks: %v", err)
		return nil, 0, fmt.Errorf("failed to list student feedbacks: %w", err)
	}

	log.Printf("FeedbackService ListStudentFeedbacks success: found %d feedbacks for student %d, totalCount=%d", len(feedbacks), studentID, totalCount)
	return feedbacks, int32(totalCount), nil
}

// UploadAttachment uploads an attachment file for a feedback
func (s *FeedbackService) UploadAttachment(ctx context.Context, feedbackID uuid.UUID, filename, contentType string, data io.Reader, size int64) error {
	log.Printf("FeedbackService UploadAttachment: FeedbackId=%s, Filename=%s, ContentType=%s, Size=%d", feedbackID.String(), filename, contentType, size)

	if feedbackID == uuid.Nil {
		log.Printf("FeedbackService UploadAttachment error: invalid feedback ID")
		return fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		log.Printf("FeedbackService UploadAttachment error: filename is required")
		return fmt.Errorf("filename is required")
	}
	if size <= 0 {
		log.Printf("FeedbackService UploadAttachment error: invalid file size %d", size)
		return fmt.Errorf("invalid file size")
	}

	// Check if context is already cancelled
	select {
	case <-ctx.Done():
		log.Printf("FeedbackService UploadAttachment error: upload cancelled before start: %v", ctx.Err())
		return fmt.Errorf("upload cancelled: %w", ctx.Err())
	default:
	}

	log.Printf("FeedbackService UploadAttachment: starting upload for feedback %s, file %s, size %d", feedbackID, filename, size)

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService UploadAttachment error: feedback not found: %v", err)
		return fmt.Errorf("feedback not found: %w", err)
	}

	log.Printf("FeedbackService UploadAttachment: feedback verified, starting attachment upload")

	// Upload attachment with context monitoring
	if err := s.attachmentRepo.Upload(ctx, feedbackID, filename, contentType, data, size); err != nil {
		// Check if the error is due to context cancellation
		if ctx.Err() != nil {
			log.Printf("FeedbackService UploadAttachment error: upload cancelled: %v", ctx.Err())
			return fmt.Errorf("upload cancelled: %w", ctx.Err())
		}
		log.Printf("FeedbackService UploadAttachment error: upload failed: %v", err)
		return fmt.Errorf("failed to upload attachment: %w", err)
	}

	log.Printf("FeedbackService UploadAttachment success: upload completed successfully for file %s", filename)
	return nil
}

// DownloadAttachment downloads an attachment file for a feedback
func (s *FeedbackService) DownloadAttachment(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *models.AttachmentInfo, error) {
	log.Printf("FeedbackService DownloadAttachment: FeedbackId=%s, Filename=%s", feedbackID.String(), filename)

	if feedbackID == uuid.Nil {
		log.Printf("FeedbackService DownloadAttachment error: invalid feedback ID")
		return nil, nil, fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		log.Printf("FeedbackService DownloadAttachment error: filename is required")
		return nil, nil, fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService DownloadAttachment error: feedback not found: %v", err)
		return nil, nil, fmt.Errorf("feedback not found: %w", err)
	}

	// Download attachment
	reader, attachmentInfo, err := s.attachmentRepo.Download(ctx, feedbackID, filename)
	if err != nil {
		log.Printf("FeedbackService DownloadAttachment error: failed to download attachment: %v", err)
		return nil, nil, fmt.Errorf("failed to download attachment: %w", err)
	}

	log.Printf("FeedbackService DownloadAttachment success: downloading file %s, size=%d, contentType=%s", filename, attachmentInfo.Size, attachmentInfo.ContentType)
	return reader, attachmentInfo, nil
}

// ListAttachments lists all attachments for a feedback
func (s *FeedbackService) ListAttachments(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentInfo, error) {
	log.Printf("FeedbackService ListAttachments: FeedbackId=%s", feedbackID.String())

	if feedbackID == uuid.Nil {
		log.Printf("FeedbackService ListAttachments error: invalid feedback ID")
		return nil, fmt.Errorf("invalid feedback ID")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService ListAttachments error: feedback not found: %v", err)
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// List attachments
	attachments, err := s.attachmentRepo.List(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService ListAttachments error: failed to list attachments: %v", err)
		return nil, fmt.Errorf("failed to list attachments: %w", err)
	}

	log.Printf("FeedbackService ListAttachments success: found %d attachments for feedback %s", len(attachments), feedbackID.String())
	return attachments, nil
}

// DeleteAttachment deletes a specific attachment
func (s *FeedbackService) DeleteAttachment(ctx context.Context, feedbackID uuid.UUID, filename string) error {
	log.Printf("FeedbackService DeleteAttachment: FeedbackId=%s, Filename=%s", feedbackID.String(), filename)

	if feedbackID == uuid.Nil {
		log.Printf("FeedbackService DeleteAttachment error: invalid feedback ID")
		return fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		log.Printf("FeedbackService DeleteAttachment error: filename is required")
		return fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService DeleteAttachment error: feedback not found: %v", err)
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Delete attachment
	if err := s.attachmentRepo.Delete(ctx, feedbackID, filename); err != nil {
		log.Printf("FeedbackService DeleteAttachment error: failed to delete attachment: %v", err)
		return fmt.Errorf("failed to delete attachment: %w", err)
	}

	log.Printf("FeedbackService DeleteAttachment success: deleted attachment %s from feedback %s", filename, feedbackID.String())
	return nil
}

// GetFeedbackByID retrieves feedback by its ID
func (s *FeedbackService) GetFeedbackByID(ctx context.Context, id uuid.UUID) (*models.Feedback, error) {
	log.Printf("FeedbackService GetFeedbackByID: ID=%s", id.String())

	if id == uuid.Nil {
		log.Printf("FeedbackService GetFeedbackByID error: invalid feedback ID")
		return nil, fmt.Errorf("invalid feedback ID")
	}

	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		log.Printf("FeedbackService GetFeedbackByID error: failed to get feedback: %v", err)
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	log.Printf("FeedbackService GetFeedbackByID success: found feedback ID=%s, ReviewerId=%d, StudentId=%d, SubmissionId=%d", feedback.ID.String(), feedback.ReviewerID, feedback.StudentID, feedback.SubmissionID)
	return feedback, nil
}

// GetAttachmentLocation gets location information for a specific attachment
func (s *FeedbackService) GetAttachmentLocation(ctx context.Context, feedbackID uuid.UUID, filename string) (*models.AttachmentLocationInfo, error) {
	log.Printf("FeedbackService GetAttachmentLocation: FeedbackId=%s, Filename=%s", feedbackID.String(), filename)

	if feedbackID == uuid.Nil {
		log.Printf("FeedbackService GetAttachmentLocation error: invalid feedback ID")
		return nil, fmt.Errorf("invalid feedback ID")
	}
	if filename == "" {
		log.Printf("FeedbackService GetAttachmentLocation error: filename is required")
		return nil, fmt.Errorf("filename is required")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService GetAttachmentLocation error: feedback not found: %v", err)
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// Get attachment location info
	locationInfo, err := s.attachmentRepo.GetLocationInfo(ctx, feedbackID, filename)
	if err != nil {
		log.Printf("FeedbackService GetAttachmentLocation error: failed to get attachment location: %v", err)
		return nil, fmt.Errorf("failed to get attachment location: %w", err)
	}

	log.Printf("FeedbackService GetAttachmentLocation success: found location for file %s - bucket=%s, object=%s", filename, locationInfo.MinioBucket, locationInfo.MinioObjectPath)
	return locationInfo, nil
}

// ListAttachmentLocations lists location information for all attachments of a feedback
func (s *FeedbackService) ListAttachmentLocations(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentLocationInfo, error) {
	log.Printf("FeedbackService ListAttachmentLocations: FeedbackId=%s", feedbackID.String())

	if feedbackID == uuid.Nil {
		log.Printf("FeedbackService ListAttachmentLocations error: invalid feedback ID")
		return nil, fmt.Errorf("invalid feedback ID")
	}

	// Verify feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService ListAttachmentLocations error: feedback not found: %v", err)
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	// List attachment location info
	locationInfos, err := s.attachmentRepo.ListLocationInfo(ctx, feedbackID)
	if err != nil {
		log.Printf("FeedbackService ListAttachmentLocations error: failed to list attachment locations: %v", err)
		return nil, fmt.Errorf("failed to list attachment locations: %w", err)
	}

	log.Printf("FeedbackService ListAttachmentLocations success: found %d attachment locations for feedback %s", len(locationInfos), feedbackID.String())
	return locationInfos, nil
}
