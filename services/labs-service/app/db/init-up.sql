-- Create table for labs
CREATE TABLE labs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    abstract TEXT,
    views BIGINT DEFAULT 0,
    submissions BIGINT DEFAULT 0,
    stars BIGINT DEFAULT 0,
    people_rated BIGINT DEFAULT 0
);

-- Create table for submissions
CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    points INTEGER,
    CONSTRAINT points_range CHECK (points >= 0 AND points <= 100)
);

-- Create table for lab relations
CREATE TABLE article_relations (
    lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    article_id UUID NOT NULL,
    PRIMARY KEY (lab_id, article_id)
);

-- Create table for lab assets
CREATE TABLE lab_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id UUID NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    total_size BIGINT NOT NULL,
    is_lab BOOLEAN DEFAULT false,
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create table for submission assets
CREATE TABLE submission_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solution_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    total_size BIGINT NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create basic indexes for performance
CREATE INDEX idx_labs_owner ON labs(owner_id);
CREATE INDEX idx_submissions_lab ON submissions(lab_id);
CREATE INDEX idx_submissions_owner ON submissions(owner_id);
CREATE INDEX idx_lab_assets_lab ON lab_assets(lab_id);
CREATE INDEX idx_submission_assets_solution ON submission_assets(solution_id);