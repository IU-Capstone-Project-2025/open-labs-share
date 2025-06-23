CREATE TABLE feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE feedback_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL REFERENCES feedbacks(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE lab_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id UUID REFERENCES lab_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_feedbacks_user_id ON feedbacks(user_id);
CREATE INDEX idx_feedbacks_lab_id ON feedbacks(lab_id);
CREATE INDEX idx_feedbacks_created_at ON feedbacks(created_at);
CREATE INDEX idx_feedback_assets_feedback_id ON feedback_assets(feedback_id);
CREATE INDEX idx_lab_comments_lab_id ON lab_comments(lab_id);
CREATE INDEX idx_lab_comments_user_id ON lab_comments(user_id);
CREATE INDEX idx_lab_comments_parent_id ON lab_comments(parent_id);
CREATE INDEX idx_lab_comments_created_at ON lab_comments(created_at);