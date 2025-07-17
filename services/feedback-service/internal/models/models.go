package models

import (
	"time"

	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

// Feedback represents a feedback entry - simplified structure
// PostgreSQL storage for metadata, MongoDB for content
type Feedback struct {
	ID           uuid.UUID `json:"id" db:"id"`
	ReviewerID   int64     `json:"reviewer_id" db:"reviewer_id"`
	StudentID    int64     `json:"student_id" db:"student_id"`
	SubmissionID int64     `json:"submission_id" db:"submission_id"`
	Title        string    `json:"title" db:"title"`
	Content      string    `json:"content"` // Markdown content stored in MongoDB
	CreatedAt    time.Time `json:"created_at" db:"created_at"`
	UpdatedAt    time.Time `json:"updated_at" db:"updated_at"`
}

// FeedbackContent represents the MongoDB document for feedback content
type FeedbackContent struct {
	ID      string `bson:"_id"` // Same as Feedback.ID
	Content string `bson:"content"`
}

// Comment represents a comment - stored in MongoDB
type Comment struct {
	ID        primitive.ObjectID `bson:"_id,omitempty" json:"id"`
	ContentID int64              `bson:"content_id" json:"content_id"`
	UserID    int64              `bson:"user_id" json:"user_id"`
	ParentID  *string            `bson:"parent_id,omitempty" json:"parent_id,omitempty"`
	Content   string             `bson:"content" json:"content"` // Markdown content
	CreatedAt time.Time          `bson:"created_at" json:"created_at"`
	UpdatedAt time.Time          `bson:"updated_at" json:"updated_at"`
	Type      string             `bson:"type" json:"type"` // Type of content (e.g., "lab", "article")
}

// AttachmentInfo represents metadata about attachments stored in MinIO
type AttachmentInfo struct {
	Filename    string    `json:"filename"`
	Size        int64     `json:"size"`
	ContentType string    `json:"content_type"`
	UploadedAt  time.Time `json:"uploaded_at"`
}

// AttachmentLocationInfo represents location metadata for MinIO direct access
type AttachmentLocationInfo struct {
	Filename        string    `json:"filename"`
	Size            int64     `json:"size"`
	ContentType     string    `json:"content_type"`
	UploadedAt      time.Time `json:"uploaded_at"`
	MinioBucket     string    `json:"minio_bucket"`
	MinioObjectPath string    `json:"minio_object_path"`
	MinioEndpoint   string    `json:"minio_endpoint"`
	UseSSL          bool      `json:"use_ssl"`
}

// FeedbackFilter represents filtering options for feedback queries
type FeedbackFilter struct {
	ReviewerID   *int64 `json:"reviewer_id,omitempty"`
	StudentID    *int64 `json:"student_id,omitempty"`
	SubmissionID *int64 `json:"submission_id,omitempty"`
	Page         int    `json:"page"`
	Limit        int    `json:"limit"`
}

// CanModify checks if a reviewer can modify the feedback (only reviewers who created it)
func (f *Feedback) CanModify(reviewerID int64) bool {
	return f.ReviewerID == reviewerID
}

// CanView checks if a user can view the feedback
func (f *Feedback) CanView(userID int64, isReviewer bool) bool {
	if isReviewer {
		return f.ReviewerID == userID
	}
	return f.StudentID == userID
}

// CommentFilter represents filtering options for comment queries
type CommentFilter struct {
	ContentID int64
	ParentID  *string
	Page      int32
	Limit     int32
	Type      string
}
