-- Create tables for Labs Service
CREATE TABLE labs (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    abstract TEXT,
    views BIGINT DEFAULT 0,
    submissions BIGINT DEFAULT 0,
    stars BIGINT DEFAULT 0,
    people_rated BIGINT DEFAULT 0
);

CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    lab_id BIGINT NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    points INTEGER DEFAULT 0
);

CREATE TABLE article_relations (
    lab_id BIGINT NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL,
    PRIMARY KEY (lab_id, article_id)
);

CREATE TABLE lab_assets (
    id BIGSERIAL PRIMARY KEY,
    lab_id BIGINT NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    filesize BIGINT NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE submission_assets (
    id BIGSERIAL PRIMARY KEY,
    solution_id BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    filesize BIGINT NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_labs_owner_id ON labs(owner_id);
CREATE INDEX idx_submissions_lab_id ON submissions(lab_id);
CREATE INDEX idx_submissions_owner_id ON submissions(owner_id);
CREATE INDEX idx_lab_assets_lab_id ON lab_assets(lab_id);
CREATE INDEX idx_submission_assets_solution_id ON submission_assets(solution_id);