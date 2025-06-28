-- Create tables for Article Service
CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    abstract TEXT,
    views BIGINT DEFAULT 0,
    stars BIGINT DEFAULT 0,
    people_rated BIGINT DEFAULT 0
);

CREATE TABLE article_assets (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    filesize BIGINT NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_articles_owner_id ON articles(owner_id);
CREATE INDEX idx_articles_created_at ON articles(created_at);
CREATE INDEX idx_article_assets_article_id ON article_assets(article_id);