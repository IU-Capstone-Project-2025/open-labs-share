package repository

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
)

// assetRepository implements AssetRepository
type assetRepository struct {
	db *sql.DB
}

// NewAssetRepository creates a new asset repository
func NewAssetRepository(db *sql.DB) AssetRepository {
	return &assetRepository{db: db}
}

// Create creates a new asset entry
func (r *assetRepository) Create(ctx context.Context, asset *models.FeedbackAsset) error {
	query := `
		INSERT INTO feedback_assets (id, feedback_id, filename, file_size, content_type, created_at)
		VALUES ($1, $2, $3, $4, $5, $6)
	`

	asset.ID = uuid.New()
	asset.CreatedAt = time.Now()

	_, err := r.db.ExecContext(ctx, query,
		asset.ID, asset.FeedbackID, asset.Filename, asset.FileSize, asset.ContentType, asset.CreatedAt,
	)
	if err != nil {
		return fmt.Errorf("failed to create asset: %w", err)
	}

	return nil
}

// GetByFeedbackID retrieves all assets for a feedback
func (r *assetRepository) GetByFeedbackID(ctx context.Context, feedbackID uuid.UUID) ([]*models.FeedbackAsset, error) {
	query := `
		SELECT id, feedback_id, filename, file_size, content_type, created_at
		FROM feedback_assets
		WHERE feedback_id = $1
		ORDER BY created_at ASC
	`

	rows, err := r.db.QueryContext(ctx, query, feedbackID)
	if err != nil {
		return nil, fmt.Errorf("failed to get assets: %w", err)
	}
	defer rows.Close()

	var assets []*models.FeedbackAsset
	for rows.Next() {
		asset := &models.FeedbackAsset{}
		err := rows.Scan(
			&asset.ID, &asset.FeedbackID, &asset.Filename,
			&asset.FileSize, &asset.ContentType, &asset.CreatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan asset: %w", err)
		}
		assets = append(assets, asset)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating assets: %w", err)
	}

	return assets, nil
}

// GetByFilename retrieves an asset by feedback ID and filename
func (r *assetRepository) GetByFilename(ctx context.Context, feedbackID uuid.UUID, filename string) (*models.FeedbackAsset, error) {
	query := `
		SELECT id, feedback_id, filename, file_size, content_type, created_at
		FROM feedback_assets
		WHERE feedback_id = $1 AND filename = $2
	`

	asset := &models.FeedbackAsset{}
	err := r.db.QueryRowContext(ctx, query, feedbackID, filename).Scan(
		&asset.ID, &asset.FeedbackID, &asset.Filename,
		&asset.FileSize, &asset.ContentType, &asset.CreatedAt,
	)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("asset not found")
		}
		return nil, fmt.Errorf("failed to get asset: %w", err)
	}

	return asset, nil
}

// Delete deletes an asset by ID
func (r *assetRepository) Delete(ctx context.Context, id uuid.UUID) error {
	query := `DELETE FROM feedback_assets WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		return fmt.Errorf("failed to delete asset: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("asset not found")
	}

	return nil
}

// DeleteByFeedbackID deletes all assets for a feedback
func (r *assetRepository) DeleteByFeedbackID(ctx context.Context, feedbackID uuid.UUID) error {
	query := `DELETE FROM feedback_assets WHERE feedback_id = $1`

	_, err := r.db.ExecContext(ctx, query, feedbackID)
	if err != nil {
		return fmt.Errorf("failed to delete assets: %w", err)
	}

	return nil
}
