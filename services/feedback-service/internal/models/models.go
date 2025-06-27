package models

import (
	"time"

	"github.com/google/uuid"
)

// Feedback represents a feedback entry
type Feedback struct {
	ID        uuid.UUID `json:"id" db:"id"`
	UserID    int64     `json:"user_id" db:"user_id"`
	StudentID int64     `json:"student_id" db:"student_id"`
	LabID     int64     `json:"lab_id" db:"lab_id"`
	Title     string    `json:"title" db:"title"`
	CreatedAt time.Time `json:"created_at" db:"created_at"`
	UpdatedAt time.Time `json:"updated_at" db:"updated_at"`
}

// FeedbackAsset represents an asset attached to feedback
type FeedbackAsset struct {
	ID          uuid.UUID `json:"id" db:"id"`
	FeedbackID  uuid.UUID `json:"feedback_id" db:"feedback_id"`
	Filename    string    `json:"filename" db:"filename"`
	FileSize    int64     `json:"file_size" db:"file_size"`
	ContentType string    `json:"content_type" db:"content_type"`
	CreatedAt   time.Time `json:"created_at" db:"created_at"`
}

// LabComment represents a comment on a lab
type LabComment struct {
	ID        uuid.UUID  `json:"id" db:"id"`
	LabID     int64      `json:"lab_id" db:"lab_id"`
	UserID    int64      `json:"user_id" db:"user_id"`
	ParentID  *uuid.UUID `json:"parent_id,omitempty" db:"parent_id"`
	Content   string     `json:"content" db:"content"`
	CreatedAt time.Time  `json:"created_at" db:"created_at"`
	UpdatedAt time.Time  `json:"updated_at" db:"updated_at"`
}

// FeedbackFilter represents filtering options for feedback queries
type FeedbackFilter struct {
	UserID    int64
	StudentID int64
	LabID     *int64
	Page      int32
	Limit     int32
}

// CommentFilter represents filtering options for comment queries
type CommentFilter struct {
	LabID    int64
	ParentID *uuid.UUID
	Page     int32
	Limit    int32
}
