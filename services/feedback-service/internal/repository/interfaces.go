package repository

import (
	"context"
	"io"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
)

// FeedbackRepository defines the interface for feedback data operations
// Handles PostgreSQL metadata and MongoDB content
type FeedbackRepository interface {
	Create(ctx context.Context, feedback *models.Feedback) error
	GetByID(ctx context.Context, id uuid.UUID) (*models.Feedback, error)
	Update(ctx context.Context, feedback *models.Feedback) error
	Delete(ctx context.Context, id uuid.UUID) error
	ListByUser(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error)
	ListByStudent(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error)
	
	// Content operations (MongoDB)
	SetContent(ctx context.Context, id uuid.UUID, content string) error
	GetContent(ctx context.Context, id uuid.UUID) (string, error)
	DeleteContent(ctx context.Context, id uuid.UUID) error
}

type CommentTxRepository interface {
	CommentRepository
}

// AttachmentRepository defines the interface for attachment operations in MinIO
type AttachmentRepository interface {
	Upload(ctx context.Context, feedbackID uuid.UUID, filename string, contentType string, data io.Reader, size int64) error
	Download(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *models.AttachmentInfo, error)
	List(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentInfo, error)
	Delete(ctx context.Context, feedbackID uuid.UUID, filename string) error
	DeleteAll(ctx context.Context, feedbackID uuid.UUID) error
	GetLocationInfo(ctx context.Context, feedbackID uuid.UUID, filename string) (*models.AttachmentLocationInfo, error)
	ListLocationInfo(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentLocationInfo, error)
}

// CommentRepository defines the interface for comment operations in MongoDB
type CommentRepository interface {
	Create(ctx context.Context, comment *models.Comment) error
	GetByID(ctx context.Context, id string) (*models.Comment, error)
	Update(ctx context.Context, comment *models.Comment) error
	Delete(ctx context.Context, id string) error
	DeleteReplies(ctx context.Context, parentID string) error
	ListByContext(ctx context.Context, filter models.CommentFilter) ([]*models.Comment, int32, error)
	ListReplies(ctx context.Context, parentID string, page, limit int32) ([]*models.Comment, int32, error)
	WithTransaction(ctx context.Context, fn func(CommentTxRepository) error) error
}
