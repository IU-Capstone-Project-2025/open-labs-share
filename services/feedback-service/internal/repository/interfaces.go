package repository

import (
	"context"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
)

// FeedbackRepository defines the interface for feedback data operations
type FeedbackRepository interface {
	Create(ctx context.Context, feedback *models.Feedback) error
	GetByID(ctx context.Context, id uuid.UUID) (*models.Feedback, error)
	Update(ctx context.Context, feedback *models.Feedback) error
	Delete(ctx context.Context, id uuid.UUID) error
	ListByUser(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error)
	ListByStudent(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error)
}

// AssetRepository defines the interface for asset data operations
type AssetRepository interface {
	Create(ctx context.Context, asset *models.FeedbackAsset) error
	GetByFeedbackID(ctx context.Context, feedbackID uuid.UUID) ([]*models.FeedbackAsset, error)
	GetByFilename(ctx context.Context, feedbackID uuid.UUID, filename string) (*models.FeedbackAsset, error)
	Delete(ctx context.Context, id uuid.UUID) error
	DeleteByFeedbackID(ctx context.Context, feedbackID uuid.UUID) error
}

// CommentRepository defines the interface for comment data operations
type CommentRepository interface {
	Create(ctx context.Context, comment *models.LabComment) error
	GetByID(ctx context.Context, id uuid.UUID) (*models.LabComment, error)
	Update(ctx context.Context, comment *models.LabComment) error
	Delete(ctx context.Context, id uuid.UUID) error
	ListByLab(ctx context.Context, filter models.CommentFilter) ([]*models.LabComment, int32, error)
}
