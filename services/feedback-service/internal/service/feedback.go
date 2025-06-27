package service

import (
	"context"
	"fmt"
	"io"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/repository"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/storage"
	"github.com/google/uuid"
)

// FeedbackService handles feedback business logic
type FeedbackService struct {
	feedbackRepo repository.FeedbackRepository
	assetRepo    repository.AssetRepository
	storage      *storage.MinIOStorage
}

// NewFeedbackService creates a new feedback service
func NewFeedbackService(feedbackRepo repository.FeedbackRepository, assetRepo repository.AssetRepository, storage *storage.MinIOStorage) *FeedbackService {
	return &FeedbackService{
		feedbackRepo: feedbackRepo,
		assetRepo:    assetRepo,
		storage:      storage,
	}
}

// CreateFeedback creates a new feedback entry
func (s *FeedbackService) CreateFeedback(ctx context.Context, userID, studentID, labID int64, title string) (*models.Feedback, error) {
	// Validate input
	if userID <= 0 {
		return nil, fmt.Errorf("invalid user ID")
	}
	if studentID <= 0 {
		return nil, fmt.Errorf("invalid student ID")
	}
	if labID <= 0 {
		return nil, fmt.Errorf("invalid lab ID")
	}
	if title == "" {
		return nil, fmt.Errorf("title is required")
	}

	// Create feedback entry
	feedback := &models.Feedback{
		UserID:    userID,
		StudentID: studentID,
		LabID:     labID,
		Title:     title,
	}

	if err := s.feedbackRepo.Create(ctx, feedback); err != nil {
		return nil, fmt.Errorf("failed to create feedback: %w", err)
	}

	return feedback, nil
}

// GetFeedback retrieves a feedback by ID
func (s *FeedbackService) GetFeedback(ctx context.Context, id uuid.UUID) (*models.Feedback, error) {
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	return feedback, nil
}

// UpdateFeedback updates a feedback entry
func (s *FeedbackService) UpdateFeedback(ctx context.Context, id uuid.UUID, title *string) (*models.Feedback, error) {
	// Get existing feedback
	feedback, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	// Update fields if provided
	if title != nil {
		feedback.Title = *title
	}

	// Update in database
	if err := s.feedbackRepo.Update(ctx, feedback); err != nil {
		return nil, fmt.Errorf("failed to update feedback: %w", err)
	}

	return feedback, nil
}

// DeleteFeedback deletes a feedback entry and all associated assets
func (s *FeedbackService) DeleteFeedback(ctx context.Context, id uuid.UUID) error {
	// Check if feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, id)
	if err != nil {
		return fmt.Errorf("failed to get feedback: %w", err)
	}

	// Delete from storage
	if err := s.storage.DeleteFeedback(ctx, id); err != nil {
		return fmt.Errorf("failed to delete feedback from storage: %w", err)
	}

	// Delete assets from database
	if err := s.assetRepo.DeleteByFeedbackID(ctx, id); err != nil {
		return fmt.Errorf("failed to delete assets: %w", err)
	}

	// Delete feedback from database
	if err := s.feedbackRepo.Delete(ctx, id); err != nil {
		return fmt.Errorf("failed to delete feedback: %w", err)
	}

	return nil
}

// ListUserFeedbacks lists feedbacks by user with optional filters
func (s *FeedbackService) ListUserFeedbacks(ctx context.Context, userID int64, labID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
	if userID <= 0 {
		return nil, 0, fmt.Errorf("invalid user ID")
	}
	if page <= 0 {
		page = 1
	}
	if limit <= 0 {
		limit = 10
	}
	if limit > 100 {
		limit = 100 // Prevent excessive memory usage
	}

	filter := models.FeedbackFilter{
		UserID: userID,
		LabID:  labID,
		Page:   page,
		Limit:  limit,
	}
	return s.feedbackRepo.ListByUser(ctx, filter)
}

// ListStudentFeedbacks lists feedbacks for a student with optional filters
func (s *FeedbackService) ListStudentFeedbacks(ctx context.Context, studentID int64, labID *int64, page, limit int32) ([]*models.Feedback, int32, error) {
	if studentID <= 0 {
		return nil, 0, fmt.Errorf("invalid student ID")
	}
	if page <= 0 {
		page = 1
	}
	if limit <= 0 {
		limit = 10
	}
	if limit > 100 {
		limit = 100 // Prevent excessive memory usage
	}

	filter := models.FeedbackFilter{
		StudentID: studentID,
		LabID:     labID,
		Page:      page,
		Limit:     limit,
	}

	return s.feedbackRepo.ListByStudent(ctx, filter)
}

// UploadAsset uploads an asset for a feedback
func (s *FeedbackService) UploadAsset(ctx context.Context, feedbackID uuid.UUID, filename, contentType string, data io.Reader, size int64) error {
	// Validate input
	if filename == "" {
		return fmt.Errorf("filename is required")
	}
	if size <= 0 {
		return fmt.Errorf("invalid file size")
	}

	// Check if feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Upload to MinIO
	if err := s.storage.UploadAsset(ctx, feedbackID, filename, data, size, contentType); err != nil {
		return fmt.Errorf("failed to upload asset: %w", err)
	}

	// Create asset record in database
	asset := &models.FeedbackAsset{
		FeedbackID:  feedbackID,
		Filename:    filename,
		FileSize:    size,
		ContentType: contentType,
	}

	if err := s.assetRepo.Create(ctx, asset); err != nil {
		// If database operation fails, we should clean up the uploaded file
		_ = s.storage.DeleteAsset(ctx, feedbackID, filename)
		return fmt.Errorf("failed to create asset record: %w", err)
	}

	return nil
}

// DownloadAsset downloads an asset
func (s *FeedbackService) DownloadAsset(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *storage.FileInfo, error) {
	// Verify asset exists in database
	asset, err := s.assetRepo.GetByFilename(ctx, feedbackID, filename)
	if err != nil {
		return nil, nil, fmt.Errorf("asset not found: %w", err)
	}

	// Get from MinIO
	reader, fileInfo, err := s.storage.GetAsset(ctx, feedbackID, filename)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to download asset: %w", err)
	}

	// Update file info with database information
	fileInfo.Size = asset.FileSize
	fileInfo.ContentType = asset.ContentType

	return reader, fileInfo, nil
}

// ListAssets lists all assets for a feedback
func (s *FeedbackService) ListAssets(ctx context.Context, feedbackID uuid.UUID) ([]*models.FeedbackAsset, error) {
	// Check if feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return nil, fmt.Errorf("feedback not found: %w", err)
	}

	return s.assetRepo.GetByFeedbackID(ctx, feedbackID)
}

// UploadFeedbackContent uploads feedback content from a stream
func (s *FeedbackService) UploadFeedbackContent(ctx context.Context, feedbackID uuid.UUID, data io.Reader, size int64) error {
	// Check if feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Upload to MinIO
	if err := s.storage.UploadFeedbackContent(ctx, feedbackID, data, size); err != nil {
		return fmt.Errorf("failed to upload feedback content: %w", err)
	}

	return nil
}

// DownloadFeedbackContent downloads feedback content as a stream
func (s *FeedbackService) DownloadFeedbackContent(ctx context.Context, feedbackID uuid.UUID) (io.ReadCloser, *storage.FileInfo, error) {
	// Check if feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return nil, nil, fmt.Errorf("feedback not found: %w", err)
	}

	// Download from MinIO
	reader, fileInfo, err := s.storage.GetFeedbackContent(ctx, feedbackID)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to download feedback content: %w", err)
	}

	return reader, fileInfo, nil
}

// UpdateFeedbackContent updates feedback content from a stream
func (s *FeedbackService) UpdateFeedbackContent(ctx context.Context, feedbackID uuid.UUID, data io.Reader, size int64) error {
	// Check if feedback exists
	_, err := s.feedbackRepo.GetByID(ctx, feedbackID)
	if err != nil {
		return fmt.Errorf("feedback not found: %w", err)
	}

	// Update content in MinIO
	if err := s.storage.UploadFeedbackContent(ctx, feedbackID, data, size); err != nil {
		return fmt.Errorf("failed to update feedback content: %w", err)
	}

	return nil
}
