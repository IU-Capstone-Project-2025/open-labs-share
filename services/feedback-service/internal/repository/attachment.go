package repository

import (
	"context"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"github.com/google/uuid"
	"github.com/minio/minio-go/v7"
)

// attachmentRepository implements AttachmentRepository using MinIO
type attachmentRepository struct {
	minioClient   *minio.Client
	bucketName    string
	minioEndpoint string
	useSSL        bool
}

// NewAttachmentRepository creates a new attachment repository
func NewAttachmentRepository(minioClient *minio.Client, bucketName, endpoint string, useSSL bool) AttachmentRepository {
	return &attachmentRepository{
		minioClient:   minioClient,
		bucketName:    bucketName,
		minioEndpoint: endpoint,
		useSSL:        useSSL,
	}
}

// Upload uploads an attachment file to MinIO
func (r *attachmentRepository) Upload(ctx context.Context, feedbackID uuid.UUID, filename string, contentType string, data io.Reader, size int64) error {
	// Create object name: feedback{feedbackID}/filename
	objectName := fmt.Sprintf("feedback%s/%s", feedbackID.String(), filename)

	// Set metadata
	metaData := map[string]string{
		"Content-Type":  contentType,
		"X-Feedback-ID": feedbackID.String(),
		"X-Uploaded-At": time.Now().Format(time.RFC3339),
	}

	// Upload object
	_, err := r.minioClient.PutObject(ctx, r.bucketName, objectName, data, size, minio.PutObjectOptions{
		ContentType:  contentType,
		UserMetadata: metaData,
	})
	if err != nil {
		return fmt.Errorf("failed to upload attachment: %w", err)
	}

	return nil
}

// Download downloads an attachment file from MinIO
func (r *attachmentRepository) Download(ctx context.Context, feedbackID uuid.UUID, filename string) (io.ReadCloser, *models.AttachmentInfo, error) {
	// Create object name: feedback{feedbackID}/filename
	objectName := fmt.Sprintf("feedback%s/%s", feedbackID.String(), filename)

	// Get object info first
	objInfo, err := r.minioClient.StatObject(ctx, r.bucketName, objectName, minio.StatObjectOptions{})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get attachment info: %w", err)
	}

	// Get object
	object, err := r.minioClient.GetObject(ctx, r.bucketName, objectName, minio.GetObjectOptions{})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get attachment: %w", err)
	}

	// Parse uploaded time from metadata
	uploadedAt := time.Now()
	if uploadedAtStr, ok := objInfo.UserMetadata["X-Uploaded-At"]; ok {
		if parsedTime, err := time.Parse(time.RFC3339, uploadedAtStr); err == nil {
			uploadedAt = parsedTime
		}
	}

	attachmentInfo := &models.AttachmentInfo{
		Filename:    filename,
		Size:        objInfo.Size,
		ContentType: objInfo.ContentType,
		UploadedAt:  uploadedAt,
	}

	return object, attachmentInfo, nil
}

// List lists all attachments for a specific feedback
func (r *attachmentRepository) List(ctx context.Context, feedbackID uuid.UUID) ([]*models.AttachmentInfo, error) {
	// Create prefix for this feedback: feedback{feedbackID}/
	prefix := fmt.Sprintf("feedback%s/", feedbackID.String())

	// List objects with the prefix
	objectCh := r.minioClient.ListObjects(ctx, r.bucketName, minio.ListObjectsOptions{
		Prefix:    prefix,
		Recursive: true,
	})

	var attachments []*models.AttachmentInfo
	for object := range objectCh {
		if object.Err != nil {
			return nil, fmt.Errorf("failed to list attachments: %w", object.Err)
		}

		// Extract filename from object key (remove prefix)
		filename := strings.TrimPrefix(object.Key, prefix)
		if filename == "" {
			continue // Skip if no filename (shouldn't happen)
		}

		// Get additional metadata
		objInfo, err := r.minioClient.StatObject(ctx, r.bucketName, object.Key, minio.StatObjectOptions{})
		if err != nil {
			return nil, fmt.Errorf("failed to get attachment metadata: %w", err)
		}

		// Parse uploaded time from metadata
		uploadedAt := object.LastModified
		if uploadedAtStr, ok := objInfo.UserMetadata["X-Uploaded-At"]; ok {
			if parsedTime, err := time.Parse(time.RFC3339, uploadedAtStr); err == nil {
				uploadedAt = parsedTime
			}
		}

		attachmentInfo := &models.AttachmentInfo{
			Filename:    filename,
			Size:        object.Size,
			ContentType: objInfo.ContentType,
			UploadedAt:  uploadedAt,
		}

		attachments = append(attachments, attachmentInfo)
	}

	return attachments, nil
}

// Delete deletes a specific attachment
func (r *attachmentRepository) Delete(ctx context.Context, feedbackID uuid.UUID, filename string) error {
	// Create object name: feedback{feedbackID}/filename
	objectName := fmt.Sprintf("feedback%s/%s", feedbackID.String(), filename)

	// Remove object
	err := r.minioClient.RemoveObject(ctx, r.bucketName, objectName, minio.RemoveObjectOptions{})
	if err != nil {
		return fmt.Errorf("failed to delete attachment: %w", err)
	}

	return nil
}

// DeleteAll deletes all attachments for a specific feedback
func (r *attachmentRepository) DeleteAll(ctx context.Context, feedbackID uuid.UUID) error {
	// Create prefix for this feedback: feedback{feedbackID}/
	prefix := fmt.Sprintf("feedback%s/", feedbackID.String())

	// List all objects with the prefix
	objectCh := r.minioClient.ListObjects(ctx, r.bucketName, minio.ListObjectsOptions{
		Prefix:    prefix,
		Recursive: true,
	})

	// Collect object names to delete
	var objectNames []string
	for object := range objectCh {
		if object.Err != nil {
			return fmt.Errorf("failed to list attachments for deletion: %w", object.Err)
		}
		objectNames = append(objectNames, object.Key)
	}

	// Delete all objects
	for _, objectName := range objectNames {
		err := r.minioClient.RemoveObject(ctx, r.bucketName, objectName, minio.RemoveObjectOptions{})
		if err != nil {
			return fmt.Errorf("failed to delete attachment %s: %w", objectName, err)
		}
	}

	return nil
}
