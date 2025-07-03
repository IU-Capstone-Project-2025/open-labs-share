package repository

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/database"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// feedbackRepository implements FeedbackRepository
// Handles both PostgreSQL (metadata) and MongoDB (content)
type feedbackRepository struct {
	db      *sql.DB
	mongodb *database.MongoDBClient
}

// NewFeedbackRepository creates a new feedback repository
func NewFeedbackRepository(db *sql.DB, mongodb *database.MongoDBClient) FeedbackRepository {
	return &feedbackRepository{
		db:      db,
		mongodb: mongodb,
	}
}

// Create creates a new feedback entry
func (r *feedbackRepository) Create(ctx context.Context, feedback *models.Feedback) error {
	// Generate UUID
	feedback.ID = uuid.New()
	now := time.Now()
	feedback.CreatedAt = now
	feedback.UpdatedAt = now

	// Start transaction for PostgreSQL
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	// Insert metadata into PostgreSQL
	query := `
		INSERT INTO feedbacks (id, reviewer_id, student_id, submission_id, title, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
	`
	_, err = tx.ExecContext(ctx, query,
		feedback.ID, feedback.ReviewerID, feedback.StudentID, feedback.SubmissionID, feedback.Title,
		feedback.CreatedAt, feedback.UpdatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to create feedback metadata: %w", err)
	}

	// Commit PostgreSQL transaction
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit feedback metadata: %w", err)
	}

	// Store content in MongoDB if provided
	if feedback.Content != "" {
		if err := r.SetContent(ctx, feedback.ID, feedback.Content); err != nil {
			// Rollback: delete from PostgreSQL if MongoDB fails
			r.Delete(ctx, feedback.ID)
			return fmt.Errorf("failed to store feedback content: %w", err)
		}
	}

	return nil
}

// GetByID retrieves a feedback by ID
func (r *feedbackRepository) GetByID(ctx context.Context, id uuid.UUID) (*models.Feedback, error) {
	query := `
		SELECT id, reviewer_id, student_id, submission_id, title, created_at, updated_at
		FROM feedbacks
		WHERE id = $1
	`

	feedback := &models.Feedback{}
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&feedback.ID, &feedback.ReviewerID, &feedback.StudentID, &feedback.SubmissionID,
		&feedback.Title, &feedback.CreatedAt, &feedback.UpdatedAt,
	)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("feedback not found: %w", err)
		}
		return nil, fmt.Errorf("failed to get feedback: %w", err)
	}

	// Get content from MongoDB
	content, err := r.GetContent(ctx, id)
	if err != nil && err != mongo.ErrNoDocuments {
		return nil, fmt.Errorf("failed to get feedback content: %w", err)
	}
	feedback.Content = content

	return feedback, nil
}

// Update updates an existing feedback
func (r *feedbackRepository) Update(ctx context.Context, feedback *models.Feedback) error {
	feedback.UpdatedAt = time.Now()

	// Update metadata in PostgreSQL
	query := `
		UPDATE feedbacks 
		SET title = $2, updated_at = $3
		WHERE id = $1
	`
	result, err := r.db.ExecContext(ctx, query, feedback.ID, feedback.Title, feedback.UpdatedAt)
	if err != nil {
		return fmt.Errorf("failed to update feedback metadata: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}
	if rowsAffected == 0 {
		return fmt.Errorf("feedback not found")
	}

	// Update content in MongoDB if provided
	if feedback.Content != "" {
		if err := r.SetContent(ctx, feedback.ID, feedback.Content); err != nil {
			return fmt.Errorf("failed to update feedback content: %w", err)
		}
	}

	return nil
}

// Delete deletes a feedback and its content
func (r *feedbackRepository) Delete(ctx context.Context, id uuid.UUID) error {
	// Delete from PostgreSQL
	query := `DELETE FROM feedbacks WHERE id = $1`
	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		return fmt.Errorf("failed to delete feedback metadata: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}
	if rowsAffected == 0 {
		return fmt.Errorf("feedback not found")
	}

	// Delete content from MongoDB (ignore if doesn't exist)
	r.DeleteContent(ctx, id)

	return nil
}

// ListByUser lists feedbacks created by a specific user
func (r *feedbackRepository) ListByUser(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error) {
	baseQuery := `
		SELECT id, reviewer_id, student_id, submission_id, title, created_at, updated_at
		FROM feedbacks
		WHERE reviewer_id = $1
	`
	countQuery := `SELECT COUNT(*) FROM feedbacks WHERE reviewer_id = $1`

	args := []interface{}{filter.ReviewerID}
	argIndex := 2

	// Add submission filter if specified
	if filter.SubmissionID != nil {
		baseQuery += fmt.Sprintf(" AND submission_id = $%d", argIndex)
		countQuery += fmt.Sprintf(" AND submission_id = $%d", argIndex)
		args = append(args, *filter.SubmissionID)
		argIndex++
	}

	// Get total count
	var totalCount int32
	err := r.db.QueryRowContext(ctx, countQuery, args[:len(args)]...).Scan(&totalCount)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count feedbacks: %w", err)
	}

	// Add pagination
	baseQuery += fmt.Sprintf(" ORDER BY created_at DESC LIMIT $%d OFFSET $%d", argIndex, argIndex+1)
	args = append(args, filter.Limit, (filter.Page-1)*filter.Limit)

	rows, err := r.db.QueryContext(ctx, baseQuery, args...)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to list user feedbacks: %w", err)
	}
	defer rows.Close()

	var feedbacks []*models.Feedback
	for rows.Next() {
		feedback := &models.Feedback{}
		err := rows.Scan(
			&feedback.ID, &feedback.ReviewerID, &feedback.StudentID, &feedback.SubmissionID,
			&feedback.Title, &feedback.CreatedAt, &feedback.UpdatedAt,
		)
		if err != nil {
			return nil, 0, fmt.Errorf("failed to scan feedback: %w", err)
		}
		feedbacks = append(feedbacks, feedback)
	}

	return feedbacks, totalCount, nil
}

// ListByStudent lists feedbacks for a specific student
func (r *feedbackRepository) ListByStudent(ctx context.Context, filter models.FeedbackFilter) ([]*models.Feedback, int32, error) {
	baseQuery := `
		SELECT id, reviewer_id, student_id, submission_id, title, created_at, updated_at
		FROM feedbacks
		WHERE student_id = $1
	`
	countQuery := `SELECT COUNT(*) FROM feedbacks WHERE student_id = $1`

	args := []interface{}{filter.StudentID}
	argIndex := 2

	// Add submission filter if specified
	if filter.SubmissionID != nil {
		baseQuery += fmt.Sprintf(" AND submission_id = $%d", argIndex)
		countQuery += fmt.Sprintf(" AND submission_id = $%d", argIndex)
		args = append(args, *filter.SubmissionID)
		argIndex++
	}

	// Get total count
	var totalCount int32
	err := r.db.QueryRowContext(ctx, countQuery, args[:len(args)]...).Scan(&totalCount)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count feedbacks: %w", err)
	}

	// Add pagination
	baseQuery += fmt.Sprintf(" ORDER BY created_at DESC LIMIT $%d OFFSET $%d", argIndex, argIndex+1)
	args = append(args, filter.Limit, (filter.Page-1)*filter.Limit)

	rows, err := r.db.QueryContext(ctx, baseQuery, args...)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to list student feedbacks: %w", err)
	}
	defer rows.Close()

	var feedbacks []*models.Feedback
	for rows.Next() {
		feedback := &models.Feedback{}
		err := rows.Scan(
			&feedback.ID, &feedback.ReviewerID, &feedback.StudentID, &feedback.SubmissionID,
			&feedback.Title, &feedback.CreatedAt, &feedback.UpdatedAt,
		)
		if err != nil {
			return nil, 0, fmt.Errorf("failed to scan feedback: %w", err)
		}
		feedbacks = append(feedbacks, feedback)
	}

	return feedbacks, totalCount, nil
}

// SetContent stores feedback content in MongoDB
func (r *feedbackRepository) SetContent(ctx context.Context, id uuid.UUID, content string) error {
	feedbackContent := models.FeedbackContent{
		ID:      id.String(),
		Content: content,
	}

	// Create a separate collection for feedback content
	collection := r.mongodb.Database.Collection("feedback_content")
	
	// Upsert the content
	filter := bson.M{"_id": id.String()}
	
	upsert := true
	_, err := collection.ReplaceOne(ctx, filter, feedbackContent, &options.ReplaceOptions{
		Upsert: &upsert,
	})
	if err != nil {
		return fmt.Errorf("failed to store feedback content: %w", err)
	}

	return nil
}

// GetContent retrieves feedback content from MongoDB
func (r *feedbackRepository) GetContent(ctx context.Context, id uuid.UUID) (string, error) {
	collection := r.mongodb.Database.Collection("feedback_content")
	
	var feedbackContent models.FeedbackContent
	err := collection.FindOne(ctx, bson.M{"_id": id.String()}).Decode(&feedbackContent)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return "", nil // No content stored
		}
		return "", fmt.Errorf("failed to get feedback content: %w", err)
	}

	return feedbackContent.Content, nil
}

// DeleteContent removes feedback content from MongoDB
func (r *feedbackRepository) DeleteContent(ctx context.Context, id uuid.UUID) error {
	collection := r.mongodb.Database.Collection("feedback_content")
	
	_, err := collection.DeleteOne(ctx, bson.M{"_id": id.String()})
	if err != nil {
		return fmt.Errorf("failed to delete feedback content: %w", err)
	}

	return nil
}
