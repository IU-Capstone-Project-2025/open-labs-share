package service

import (
	"context"
	"fmt"

	"github.com/Ravwvil/feedback/internal/models"
	"github.com/Ravwvil/feedback/internal/repository"
	"github.com/google/uuid"
)

// CommentService handles comment business logic
type CommentService struct {
	commentRepo repository.CommentRepository
}

// NewCommentService creates a new comment service
func NewCommentService(commentRepo repository.CommentRepository) *CommentService {
	return &CommentService{
		commentRepo: commentRepo,
	}
}

// CreateComment creates a new comment
func (s *CommentService) CreateComment(ctx context.Context, labID, userID int64, parentID *uuid.UUID, content string) (*models.LabComment, error) {
	// Validate input
	if labID <= 0 {
		return nil, fmt.Errorf("invalid lab ID")
	}
	if userID <= 0 {
		return nil, fmt.Errorf("invalid user ID")
	}
	if content == "" {
		return nil, fmt.Errorf("content is required")
	}

	// If parent ID is provided, verify it exists
	if parentID != nil {
		parentComment, err := s.commentRepo.GetByID(ctx, *parentID)
		if err != nil {
			return nil, fmt.Errorf("parent comment not found: %w", err)
		}
		// Ensure parent comment is for the same lab
		if parentComment.LabID != labID {
			return nil, fmt.Errorf("parent comment is not for the same lab")
		}
	}

	// Create comment
	comment := &models.LabComment{
		LabID:    labID,
		UserID:   userID,
		ParentID: parentID,
		Content:  content,
	}

	if err := s.commentRepo.Create(ctx, comment); err != nil {
		return nil, fmt.Errorf("failed to create comment: %w", err)
	}

	return comment, nil
}

// GetComment retrieves a comment by ID
func (s *CommentService) GetComment(ctx context.Context, id uuid.UUID) (*models.LabComment, error) {
	comment, err := s.commentRepo.GetByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("failed to get comment: %w", err)
	}

	return comment, nil
}

// UpdateComment updates a comment
func (s *CommentService) UpdateComment(ctx context.Context, id uuid.UUID, content string) (*models.LabComment, error) {
	// Validate input
	if content == "" {
		return nil, fmt.Errorf("content is required")
	}

	// Get existing comment
	comment, err := s.commentRepo.GetByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("failed to get comment: %w", err)
	}

	// Update content
	comment.Content = content

	if err := s.commentRepo.Update(ctx, comment); err != nil {
		return nil, fmt.Errorf("failed to update comment: %w", err)
	}

	return comment, nil
}

// DeleteComment deletes a comment (and all its replies)
func (s *CommentService) DeleteComment(ctx context.Context, id uuid.UUID) error {
	// Check if comment exists
	_, err := s.commentRepo.GetByID(ctx, id)
	if err != nil {
		return fmt.Errorf("failed to get comment: %w", err)
	}

	if err := s.commentRepo.Delete(ctx, id); err != nil {
		return fmt.Errorf("failed to delete comment: %w", err)
	}

	return nil
}

// ListLabComments lists comments for a lab with pagination
func (s *CommentService) ListLabComments(ctx context.Context, labID int64, parentID *uuid.UUID, page, limit int32) ([]*models.LabComment, int32, error) {
	if labID <= 0 {
		return nil, 0, fmt.Errorf("invalid lab ID")
	}
	if page <= 0 {
		page = 1
	}
	if limit <= 0 {
		limit = 20
	}
	if limit > 100 {
		limit = 100 // Prevent excessive memory usage
	}

	filter := models.CommentFilter{
		LabID:    labID,
		ParentID: parentID,
		Page:     page,
		Limit:    limit,
	}

	return s.commentRepo.ListByLab(ctx, filter)
}

// GetCommentReplies gets replies to a specific comment
func (s *CommentService) GetCommentReplies(ctx context.Context, commentID uuid.UUID, page, limit int32) ([]*models.LabComment, int32, error) {
	// Get the parent comment to get the lab ID
	parentComment, err := s.commentRepo.GetByID(ctx, commentID)
	if err != nil {
		return nil, 0, fmt.Errorf("parent comment not found: %w", err)
	}

	return s.ListLabComments(ctx, parentComment.LabID, &commentID, page, limit)
}
