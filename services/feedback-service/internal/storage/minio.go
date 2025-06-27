package storage

import (
	"context"
	"fmt"
	"io"
	"log"
	"strings"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
	"github.com/google/uuid"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

// MinIOStorage handles file operations with MinIO
type MinIOStorage struct {
	client     *minio.Client
	bucketName string
}

// FileInfo represents information about a stored file
type FileInfo struct {
	Name        string
	Size        int64
	ContentType string
}

// NewMinIOStorage creates a new MinIO storage instance
func NewMinIOStorage(cfg config.MinIOConfig) (*MinIOStorage, error) {
	client, err := minio.New(cfg.Endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.AccessKey, cfg.SecretKey, ""),
		Secure: cfg.UseSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create MinIO client: %w", err)
	}

	storage := &MinIOStorage{
		client:     client,
		bucketName: cfg.BucketName,
	}

	// Create bucket if it doesn't exist and if configured to do so
	if cfg.CreateBucket {
		if err := storage.ensureBucketExists(context.Background()); err != nil {
			return nil, fmt.Errorf("failed to ensure bucket exists: %w", err)
		}
	}

	return storage, nil
}

// ensureBucketExists creates the bucket if it doesn't exist
func (s *MinIOStorage) ensureBucketExists(ctx context.Context) error {
	exists, err := s.client.BucketExists(ctx, s.bucketName)
	if err != nil {
		return fmt.Errorf("failed to check if bucket exists: %w", err)
	}

	if !exists {
		err := s.client.MakeBucket(ctx, s.bucketName, minio.MakeBucketOptions{})
		if err != nil {
			return fmt.Errorf("failed to create bucket: %w", err)
		}
		log.Printf("Created bucket: %s", s.bucketName)
	}

	return nil
}

// UploadFeedbackContent uploads feedback content from a stream
func (s *MinIOStorage) UploadFeedbackContent(ctx context.Context, feedbackID uuid.UUID, data io.Reader, size int64) error {
	objectName := fmt.Sprintf("feedback/%s/content.md", feedbackID)

	_, err := s.client.PutObject(ctx, s.bucketName, objectName, data, size, minio.PutObjectOptions{
		ContentType: "text/markdown",
	})
	if err != nil {
		return fmt.Errorf("failed to upload feedback content: %w", err)
	}

	return nil
}

// GetFeedbackContent retrieves feedback content as a stream
func (s *MinIOStorage) GetFeedbackContent(ctx context.Context, feedbackID uuid.UUID) (io.ReadCloser, *FileInfo, error) {
	objectName := fmt.Sprintf("feedback/%s/content.md", feedbackID)

	// Get object info first
	objInfo, err := s.client.StatObject(ctx, s.bucketName, objectName, minio.StatObjectOptions{})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get feedback content info: %w", err)
	}

	// Get the object
	object, err := s.client.GetObject(ctx, s.bucketName, objectName, minio.GetObjectOptions{})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get feedback content: %w", err)
	}

	fileInfo := &FileInfo{
		Name:        "content.md",
		Size:        objInfo.Size,
		ContentType: "text/markdown",
	}

	return object, fileInfo, nil
}

// UploadAsset uploads an asset file
func (s *MinIOStorage) UploadAsset(ctx context.Context, feedbackID uuid.UUID, filename string, data io.Reader, size int64, contentType string) error {
	objectName := fmt.Sprintf("feedback/%s/assets/%s", feedbackID, filename)

	_, err := s.client.PutObject(ctx, s.bucketName, objectName, data, size, minio.PutObjectOptions{
		ContentType: contentType,
	})
	if err != nil {
		return fmt.Errorf("failed to upload asset: %w", err)
	}

	return nil
}

// GetAsset retrieves an asset file
func (s *MinIOStorage) GetAsset(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *FileInfo, error) {
	objectName := fmt.Sprintf("feedback/%s/assets/%s", feedbackID, filename)

	// Get object info first
	objInfo, err := s.client.StatObject(ctx, s.bucketName, objectName, minio.StatObjectOptions{})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get asset info: %w", err)
	}

	// Get the object
	object, err := s.client.GetObject(ctx, s.bucketName, objectName, minio.GetObjectOptions{})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get asset: %w", err)
	}

	fileInfo := &FileInfo{
		Name:        filename,
		Size:        objInfo.Size,
		ContentType: objInfo.ContentType,
	}

	return object, fileInfo, nil
}

// ListAssets lists all assets for a feedback
func (s *MinIOStorage) ListAssets(ctx context.Context, feedbackID uuid.UUID) ([]FileInfo, error) {
	prefix := fmt.Sprintf("feedback/%s/assets/", feedbackID)

	objectCh := s.client.ListObjects(ctx, s.bucketName, minio.ListObjectsOptions{
		Prefix:    prefix,
		Recursive: true,
	})

	var assets []FileInfo
	for object := range objectCh {
		if object.Err != nil {
			return nil, fmt.Errorf("failed to list assets: %w", object.Err)
		}

		// Extract filename from the full object name
		filename := strings.TrimPrefix(object.Key, prefix)
		if filename == "" {
			continue // Skip if it's just the directory
		}

		assets = append(assets, FileInfo{
			Name:        filename,
			Size:        object.Size,
			ContentType: "", // We'll need to get this separately if needed
		})
	}

	return assets, nil
}

// DeleteFeedback deletes all files associated with a feedback
func (s *MinIOStorage) DeleteFeedback(ctx context.Context, feedbackID uuid.UUID) error {
	prefix := fmt.Sprintf("feedback/%s/", feedbackID)

	objectCh := s.client.ListObjects(ctx, s.bucketName, minio.ListObjectsOptions{
		Prefix:    prefix,
		Recursive: true,
	})

	// Collect all object names to delete
	var objectNames []string
	for object := range objectCh {
		if object.Err != nil {
			return fmt.Errorf("failed to list objects for deletion: %w", object.Err)
		}
		objectNames = append(objectNames, object.Key)
	}

	// Delete all objects
	for _, objectName := range objectNames {
		err := s.client.RemoveObject(ctx, s.bucketName, objectName, minio.RemoveObjectOptions{})
		if err != nil {
			return fmt.Errorf("failed to delete object %s: %w", objectName, err)
		}
	}

	return nil
}

// DeleteAsset deletes a specific asset
func (s *MinIOStorage) DeleteAsset(ctx context.Context, feedbackID uuid.UUID, filename string) error {
	objectName := fmt.Sprintf("feedback/%s/assets/%s", feedbackID, filename)

	err := s.client.RemoveObject(ctx, s.bucketName, objectName, minio.RemoveObjectOptions{})
	if err != nil {
		return fmt.Errorf("failed to delete asset: %w", err)
	}

	return nil
}
