package repository

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
)

// feedbackRepository implements FeedbackRepository
type feedbackRepository struct {
	db *sql.DB
}

// NewFeedbackRepository creates a new feedback repository
func NewFeedbackRepository(db *sql.DB) FeedbackRepository {
	return &feedbackRepository{db: db}
}

// Create creates a new feedback entry
func (r *feedbackRepository) Create(ctx context.Context, feedback *models.Feedback) error {
	query := `
		INSERT INTO feedbacks (id, user_id, student_id, lab_id, title, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
	`

	now := time.Now()
	feedback.ID = uuid.New()
	feedback.CreatedAt = now
	feedback.UpdatedAt = now

	_, err := r.db.ExecContext(ctx, query,
		feedback.ID, feedback.UserID, feedback.StudentID, feedback.LabID, feedback.Title,
		feedback.CreatedAt, feedback.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to create feedback: %w", err)
	}

	return nil
}

// GetByID retrieves a feedback by ID
func (r *feedbackRepository) GetByID(ctx context.Context, id uuid.UUID) (*models.Feedback, error) {
	query := `
		SELECT id, user_id, student_id, lab_id, title, created_at, updated_at
		FROM feedbacks
		WHERE id = $1
	`

	feedback := &models.Feedback{}
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&feedback.ID, &feedback.UserID, &feedback.StudentID, &feedback.LabID, &feedback.Title,
		&feedback.CreatedAt, &feedback.UpdatedAt,
	)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("feedback not found")
		}
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	return feedback, nil
}

// Update updates a feedback entry
func (r *feedbackRepository) Update(ctx context.Context, feedback *models.Feedback) error {
	query := `
		UPDATE feedbacks
		SET title = $2, updated_at = $3
		WHERE id = $1
	`

	feedback.UpdatedAt = time.Now()

	result, err := r.db.ExecContext(ctx, query,
		feedback.ID, feedback.Title, feedback.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to update feedback: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("feedback not found")
	}

	return nil
}

// Delete deletes a feedback entry
func (r *feedbackRepository) Delete(ctx context.Context, id uuid.UUID) error {
	query := `DELETE FROM feedbacks WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		return fmt.Errorf("failed to delete feedback: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("feedback not found")
	}

	return nil
}

// ListByUser lists feedbacks by user with optional lab filter and pagination
func (r *feedbackRepository) ListByUser(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error) {
	// Build the base query
	baseQuery := `
		FROM feedbacks
		WHERE user_id = $1
	`
	args := []interface{}{filter.UserID}
	argCount := 1

	// Add lab filter if specified
	if filter.LabID != nil {
		argCount++
		baseQuery += fmt.Sprintf(" AND lab_id = $%d", argCount)
		args = append(args, *filter.LabID)
	}

	// Count total records
	countQuery := "SELECT COUNT(*) " + baseQuery
	var totalCount int32
	err := r.db.QueryRowContext(ctx, countQuery, args...).Scan(&totalCount)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count feedbacks: %w", err)
	}

	// Add pagination
	offset := (filter.Page - 1) * filter.Limit
	argCount++
	baseQuery += fmt.Sprintf(" ORDER BY created_at DESC LIMIT $%d", argCount)
	args = append(args, filter.Limit)

	argCount++
	baseQuery += fmt.Sprintf(" OFFSET $%d", argCount)
	args = append(args, offset) // Build the select query
	selectQuery := `
		SELECT id, user_id, student_id, lab_id, title, created_at, updated_at
	` + baseQuery

	rows, err := r.db.QueryContext(ctx, selectQuery, args...)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to list feedbacks: %w", err)
	}
	defer rows.Close()

	var feedbacks []*models.Feedback
	for rows.Next() {
		feedback := &models.Feedback{}
		err := rows.Scan(
			&feedback.ID, &feedback.UserID, &feedback.StudentID, &feedback.LabID, &feedback.Title,
			&feedback.CreatedAt, &feedback.UpdatedAt,
		)
		if err != nil {
			return nil, 0, fmt.Errorf("failed to scan feedback: %w", err)
		}
		feedbacks = append(feedbacks, feedback)
	}

	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("error iterating feedbacks: %w", err)
	}
	return feedbacks, totalCount, nil
}

// ListByStudent lists feedbacks by student with optional lab filter and pagination
func (r *feedbackRepository) ListByStudent(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error) {
	// Build the base query
	baseQuery := `
		FROM feedbacks
		WHERE student_id = $1
	`
	args := []interface{}{filter.StudentID}
	argCount := 1

	// Add lab filter if specified
	if filter.LabID != nil {
		argCount++
		baseQuery += fmt.Sprintf(" AND lab_id = $%d", argCount)
		args = append(args, *filter.LabID)
	}

	// Count total records
	countQuery := "SELECT COUNT(*) " + baseQuery
	var totalCount int32
	err := r.db.QueryRowContext(ctx, countQuery, args...).Scan(&totalCount)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count feedbacks: %w", err)
	}

	// Add pagination
	offset := (filter.Page - 1) * filter.Limit
	argCount++
	baseQuery += fmt.Sprintf(" ORDER BY created_at DESC LIMIT $%d", argCount)
	args = append(args, filter.Limit)

	argCount++
	baseQuery += fmt.Sprintf(" OFFSET $%d", argCount)
	args = append(args, offset)

	// Build the select query
	selectQuery := `
		SELECT id, user_id, student_id, lab_id, title, created_at, updated_at
	` + baseQuery

	rows, err := r.db.QueryContext(ctx, selectQuery, args...)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to list feedbacks: %w", err)
	}
	defer rows.Close()

	var feedbacks []*models.Feedback
	for rows.Next() {
		feedback := &models.Feedback{}
		err := rows.Scan(
			&feedback.ID, &feedback.UserID, &feedback.StudentID, &feedback.LabID, &feedback.Title,
			&feedback.CreatedAt, &feedback.UpdatedAt,
		)
		if err != nil {
			return nil, 0, fmt.Errorf("failed to scan feedback: %w", err)
		}
		feedbacks = append(feedbacks, feedback)
	}

	if err := rows.Err(); err != nil {
		return nil, 0, fmt.Errorf("error iterating feedbacks: %w", err)
	}

	return feedbacks, totalCount, nil
}
