CREATE TABLE feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE feedback_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (feedback_id) REFERENCES feedbacks(id) ON DELETE CASCADE
);

CREATE INDEX idx_feedbacks_reviewer_id ON feedbacks(reviewer_id);
CREATE INDEX idx_feedbacks_student_id ON feedbacks(student_id);
CREATE INDEX idx_feedbacks_submission_id ON feedbacks(submission_id);
CREATE INDEX idx_feedbacks_created_at ON feedbacks(created_at);

CREATE INDEX idx_feedback_assets_feedback_id ON feedback_assets(feedback_id);
CREATE INDEX idx_feedback_assets_filename ON feedback_assets(feedback_id, filename);