package service

import (
	"context"
	"fmt"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/repository"
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
func (s *CommentService) CreateComment(ctx context.Context, contentID, userID int64, parentID *string, content string) (*models.Comment, error) {
	// Validate input
	if contentID <= 0 {
		return nil, fmt.Errorf("invalid content ID")
	}
	if userID <= 0 {
		return nil, fmt.Errorf("invalid user ID")
	}
	if content == "" {
		return nil, fmt.Errorf("content is required")
	}

	// Validate parent comment exists if specified
	if parentID != nil && *parentID != "" {
		_, err := s.commentRepo.GetByID(ctx, *parentID)
		if err != nil {
			return nil, fmt.Errorf("parent comment not found: %w", err)
		}
	}

	// Create comment
	comment := &models.Comment{
		ContentID: contentID,
		UserID:    userID,
		ParentID:  parentID,
		Content:   content,
	}

	// Save to MongoDB
	if err := s.commentRepo.Create(ctx, comment); err != nil {
		return nil, fmt.Errorf("failed to create comment: %w", err)
	}

	return comment, nil
}

// GetComment retrieves a comment by ID
func (s *CommentService) GetComment(ctx context.Context, id string) (*models.Comment, error) {
	comment, err := s.commentRepo.GetByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("failed to get comment: %w", err)
	}

	return comment, nil
}

// UpdateComment updates an existing comment
func (s *CommentService) UpdateComment(ctx context.Context, id, content string) (*models.Comment, error) {
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

	// Save changes
	if err := s.commentRepo.Update(ctx, comment); err != nil {
		return nil, fmt.Errorf("failed to update comment: %w", err)
	}

	return comment, nil
}

// DeleteComment deletes a comment and all its replies
func (s *CommentService) DeleteComment(ctx context.Context, id string) error {
	// Check if comment exists
	_, err := s.commentRepo.GetByID(ctx, id)
	if err != nil {
		return fmt.Errorf("failed to get comment: %w", err)
	}

	// Delete comment and all replies (handled by repository)
	if err := s.commentRepo.Delete(ctx, id); err != nil {
		return fmt.Errorf("failed to delete comment: %w", err)
	}

	return nil
}

// ListComments lists comments by content ID
func (s *CommentService) ListComments(ctx context.Context, contentID int64, parentID *string, page, limit int32) ([]*models.Comment, int32, error) {
	if contentID <= 0 {
		return nil, 0, fmt.Errorf("invalid content ID")
	}
	if page <= 0 {
		page = 1
	}
	if limit <= 0 || limit > 100 {
		limit = 20
	}

	filter := models.CommentFilter{
		ContentID: contentID,
		ParentID:  parentID,
		Page:      page,
		Limit:     limit,
	}

	comments, totalCount, err := s.commentRepo.ListByContext(ctx, filter)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to list comments: %w", err)
	}

	return comments, totalCount, nil
}

// GetCommentReplies gets replies to a specific comment
func (s *CommentService) GetCommentReplies(ctx context.Context, commentID string, page, limit int32) ([]*models.Comment, int32, error) {
	// Check if parent comment exists
	_, err := s.commentRepo.GetByID(ctx, commentID)
	if err != nil {
		return nil, 0, fmt.Errorf("parent comment not found: %w", err)
	}

	if page <= 0 {
		page = 1
	}
	if limit <= 0 || limit > 100 {
		limit = 20
	}

	replies, totalCount, err := s.commentRepo.ListReplies(ctx, commentID, page, limit)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to get comment replies: %w", err)
	}

	return replies, totalCount, nil
}
