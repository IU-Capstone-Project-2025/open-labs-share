package repository

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
)

// commentRepository implements CommentRepository
type commentRepository struct {
	db *sql.DB
}

// NewCommentRepository creates a new comment repository
func NewCommentRepository(db *sql.DB) CommentRepository {
	return &commentRepository{db: db}
}

// Create creates a new comment
func (r *commentRepository) Create(ctx context.Context, comment *models.LabComment) error {
	query := `
		INSERT INTO lab_comments (id, lab_id, user_id, parent_id, content, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
	`

	now := time.Now()
	comment.ID = uuid.New()
	comment.CreatedAt = now
	comment.UpdatedAt = now

	_, err := r.db.ExecContext(ctx, query,
		comment.ID, comment.LabID, comment.UserID, comment.ParentID,
		comment.Content, comment.CreatedAt, comment.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to create comment: %w", err)
	}

	return nil
}

// GetByID retrieves a comment by ID
func (r *commentRepository) GetByID(ctx context.Context, id uuid.UUID) (*models.LabComment, error) {
	query := `
		SELECT id, lab_id, user_id, parent_id, content, created_at, updated_at
		FROM lab_comments
		WHERE id = $1
	`

	comment := &models.LabComment{}
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&comment.ID, &comment.LabID, &comment.UserID, &comment.ParentID,
		&comment.Content, &comment.CreatedAt, &comment.UpdatedAt,
	)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("comment not found")
		}
		return nil, fmt.Errorf("failed to get comment: %w", err)
	}

	return comment, nil
}

// Update updates a comment
func (r *commentRepository) Update(ctx context.Context, comment *models.LabComment) error {
	query := `
		UPDATE lab_comments
		SET content = $2, updated_at = $3
		WHERE id = $1
	`

	comment.UpdatedAt = time.Now()

	result, err := r.db.ExecContext(ctx, query,
		comment.ID, comment.Content, comment.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to update comment: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("comment not found")
	}

	return nil
}

// Delete deletes a comment (and all its replies due to cascade)
func (r *commentRepository) Delete(ctx context.Context, id uuid.UUID) error {
	query := `DELETE FROM lab_comments WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		return fmt.Errorf("failed to delete comment: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("comment not found")
	}

	return nil
}

// ListByLab lists comments for a lab with optional parent filter and pagination
func (r *commentRepository) ListByLab(ctx context.Context, filter models.CommentFilter) ([]*models.LabComment, int32, error) {
	// Build the base query
	baseQuery := `
		FROM lab_comments
		WHERE lab_id = $1
	`
	args := []interface{}{filter.LabID}
	argCount := 1

	// Add parent filter if specified
	if filter.ParentID != nil {
		argCount++
		baseQuery += fmt.Sprintf(" AND parent_id = $%d", argCount)
		args = append(args, *filter.ParentID)
	} else {
		// If no parent filter is specified, get only top-level comments
		baseQuery += " AND parent_id IS NULL"
	}

	// Count total records
	countQuery := "SELECT COUNT(*) " + baseQuery
	var totalCount int32
	err := r.db.QueryRowContext(ctx, countQuery, args...).Scan(&totalCount)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count comments: %w", err)
	}

	// Add pagination
	offset := (filter.Page - 1) * filter.Limit
	argCount++
	baseQuery += fmt.Sprintf(" ORDER BY created_at ASC LIMIT $%d", argCount)
	args = append(args, filter.Limit)

	argCount++
	baseQuery += fmt.Sprintf(" OFFSET $%d", argCount)
	args = append(args, offset)

	// Build the select query
	selectQuery := `
		SELECT id, lab_id, user_id, parent_id, content, created_at, updated_at
	` + baseQuery

	rows, err := r.db.QueryContext(ctx, selectQuery, args...)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to list comments: %w", err)
	}
	defer rows.Close()

	var comments []*models.LabComment
	for rows.Next() {
		comment := &models.LabComment{}
		err := rows.Scan(
			&comment.ID, &comment.LabID, &comment.UserID, &comment.ParentID,
			&comment.Content, &comment.CreatedAt, &comment.UpdatedAt,
		)
		if err != nil {
			return nil, 0, fmt.Errorf("failed to scan comment: %w", err)
		}
		comments = append(comments, comment)
	}

	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("error iterating comments: %w", err)
	}

	return comments, totalCount, nil
}
