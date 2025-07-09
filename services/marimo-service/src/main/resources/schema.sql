-- ========================================
-- Marimo Service Database Schema (PostgreSQL)
-- ========================================

-- ========================================
-- CORE TABLES
-- ========================================

-- Components table - main entity for interactive components
CREATE TABLE IF NOT EXISTS components (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('article', 'lab')),
    content_id VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    notebook_path VARCHAR(500) NOT NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    
    -- Constraints
    CONSTRAINT unique_component_per_content UNIQUE(content_type, content_id, name)
);

-- Component sessions table - manages active notebook sessions
CREATE TABLE IF NOT EXISTS component_sessions (
    id VARCHAR(255) PRIMARY KEY,
    component_id VARCHAR(255) NOT NULL,
    session_token VARCHAR(500) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'idle', 'expired')),
    state_data JSONB DEFAULT '{}',
    python_process_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_session_component 
        FOREIGN KEY (component_id) 
        REFERENCES components(id) 
        ON DELETE CASCADE
);

-- Component assets table - manages files associated with components
CREATE TABLE IF NOT EXISTS component_assets (
    id VARCHAR(255) PRIMARY KEY,
    component_id VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL CHECK (asset_type IN ('data', 'image', 'notebook', 'config')),
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_asset_component 
        FOREIGN KEY (component_id) 
        REFERENCES components(id) 
        ON DELETE CASCADE
);

-- ========================================
-- POSTGRESQL INDEXES (created separately)
-- ========================================

-- Components table indexes
CREATE INDEX IF NOT EXISTS idx_content ON components(content_type, content_id);
CREATE INDEX IF NOT EXISTS idx_owner ON components(owner_id);
CREATE INDEX IF NOT EXISTS idx_active ON components(is_active);
CREATE INDEX IF NOT EXISTS idx_notebook_path ON components(notebook_path);
CREATE INDEX IF NOT EXISTS idx_components_created_at ON components(created_at);

-- Component sessions table indexes
CREATE INDEX IF NOT EXISTS idx_session_token ON component_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_component ON component_sessions(component_id);
CREATE INDEX IF NOT EXISTS idx_status ON component_sessions(status);
CREATE INDEX IF NOT EXISTS idx_expires ON component_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_last_accessed ON component_sessions(last_accessed);
CREATE INDEX IF NOT EXISTS idx_python_process ON component_sessions(python_process_id);

-- Component assets table indexes
CREATE INDEX IF NOT EXISTS idx_asset_component ON component_assets(component_id);
CREATE INDEX IF NOT EXISTS idx_asset_type ON component_assets(asset_type);
CREATE INDEX IF NOT EXISTS idx_file_path ON component_assets(file_path);
CREATE INDEX IF NOT EXISTS idx_assets_created_at ON component_assets(created_at);

-- ========================================
-- POSTGRESQL TRIGGER FUNCTIONS
-- ========================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to automatically update last_accessed timestamp  
CREATE OR REPLACE FUNCTION update_last_accessed_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_accessed = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- POSTGRESQL TRIGGERS
-- ========================================

-- Trigger for components table
CREATE TRIGGER IF NOT EXISTS update_components_timestamp
    BEFORE UPDATE ON components
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for sessions table to auto-update last_accessed
CREATE TRIGGER IF NOT EXISTS update_sessions_last_accessed
    BEFORE UPDATE ON component_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_last_accessed_column();

-- ========================================
-- SAMPLE DATA FOR DEVELOPMENT
-- ========================================

-- Sample components (using ON CONFLICT instead of INSERT IGNORE)
INSERT INTO components (
    id, name, description, content_type, content_id, owner_id, 
    notebook_path, metadata
) VALUES 
(
    'comp-001', 
    'Data Visualization Demo', 
    'Interactive data visualization for machine learning article',
    'article', 
    'art-123', 
    'user-001',
    'marimo/components/comp-001/notebook.py',
    '{"component_type": "visualization", "tags": ["ml", "data-viz", "interactive"], "dependencies": ["plotly", "pandas"]}'::jsonb
),
(
    'comp-002', 
    'Algorithm Complexity Explorer', 
    'Interactive visualization of algorithm time complexity',
    'lab', 
    'lab-456', 
    'user-002',
    'marimo/components/comp-002/notebook.py',
    '{"component_type": "algorithm_demo", "tags": ["algorithms", "complexity", "education"], "dependencies": ["matplotlib", "numpy"]}'::jsonb
),
(
    'comp-003', 
    'Real-time Feedback System', 
    'Live feedback on student code submissions',
    'lab', 
    'lab-789', 
    'user-001',
    'marimo/components/comp-003/notebook.py',
    '{"component_type": "feedback", "tags": ["realtime", "feedback", "code-evaluation"], "dependencies": ["ast", "pylint"]}'::jsonb
)
ON CONFLICT (id) DO NOTHING;

-- Sample component assets
INSERT INTO component_assets (
    id, component_id, asset_type, file_path, mime_type, file_size
) VALUES 
(
    'asset-001', 
    'comp-001', 
    'data', 
    'marimo/components/comp-001/data/sample_dataset.csv',
    'text/csv',
    102400
),
(
    'asset-002', 
    'comp-001', 
    'config', 
    'marimo/components/comp-001/config/plot_config.json',
    'application/json',
    2048
),
(
    'asset-003', 
    'comp-002', 
    'data', 
    'marimo/components/comp-002/data/algorithm_data.json',
    'application/json',
    8192
)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- VIEWS FOR COMMON QUERIES
-- ========================================

-- Active components with session count
CREATE OR REPLACE VIEW v_active_components AS
SELECT 
    c.id,
    c.name,
    c.description,
    c.content_type,
    c.content_id,
    c.owner_id,
    c.created_at,
    c.updated_at,
    c.metadata,
    COUNT(s.id) as active_sessions_count,
    COUNT(a.id) as assets_count
FROM components c
LEFT JOIN component_sessions s ON c.id = s.component_id AND s.status = 'active'
LEFT JOIN component_assets a ON c.id = a.component_id
WHERE c.is_active = true
GROUP BY c.id, c.name, c.description, c.content_type, c.content_id, 
         c.owner_id, c.created_at, c.updated_at, c.metadata;

-- Session analytics view (PostgreSQL compatible)
CREATE OR REPLACE VIEW v_session_analytics AS
SELECT 
    c.id as component_id,
    c.name as component_name,
    c.content_type,
    COUNT(s.id) as total_sessions,
    COUNT(CASE WHEN s.status = 'active' THEN 1 END) as active_sessions,
    COUNT(CASE WHEN s.status = 'idle' THEN 1 END) as idle_sessions,
    COUNT(CASE WHEN s.status = 'expired' THEN 1 END) as expired_sessions,
    AVG(EXTRACT(EPOCH FROM (s.last_accessed - s.created_at)) / 60) as avg_session_duration_minutes,
    MAX(s.last_accessed) as last_session_activity
FROM components c
LEFT JOIN component_sessions s ON c.id = s.component_id
WHERE c.is_active = true
GROUP BY c.id, c.name, c.content_type;

-- ========================================
-- POSTGRESQL FUNCTIONS (instead of stored procedures)
-- ========================================

-- Cleanup expired sessions function
CREATE OR REPLACE FUNCTION cleanup_expired_sessions(
    expiry_threshold_minutes INTEGER DEFAULT 60
)
RETURNS INTEGER AS $$
DECLARE
    affected_rows INTEGER := 0;
BEGIN
    -- Update expired sessions
    UPDATE component_sessions 
    SET status = 'expired' 
    WHERE status IN ('active', 'idle') 
      AND expires_at IS NOT NULL 
      AND expires_at < NOW();
    
    -- Delete old expired sessions
    DELETE FROM component_sessions 
    WHERE status = 'expired' 
      AND last_accessed < (NOW() - INTERVAL '1 minute' * expiry_threshold_minutes);
    
    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    RETURN affected_rows;
END;
$$ LANGUAGE plpgsql;

-- Get component statistics function
CREATE OR REPLACE FUNCTION get_component_stats(
    component_id_param VARCHAR(255)
)
RETURNS TABLE(
    id VARCHAR(255),
    name VARCHAR(255),
    description TEXT,
    content_type VARCHAR(50),
    content_id VARCHAR(255),
    owner_id VARCHAR(255),
    notebook_path VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_active BOOLEAN,
    total_sessions BIGINT,
    active_sessions BIGINT,
    total_assets BIGINT,
    last_session_activity TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id,
        c.name,
        c.description,
        c.content_type,
        c.content_id,
        c.owner_id,
        c.notebook_path,
        c.metadata,
        c.created_at,
        c.updated_at,
        c.is_active,
        COUNT(s.id) as total_sessions,
        COUNT(CASE WHEN s.status = 'active' THEN 1 END) as active_sessions,
        COUNT(a.id) as total_assets,
        MAX(s.last_accessed) as last_session_activity
    FROM components c
    LEFT JOIN component_sessions s ON c.id = s.component_id
    LEFT JOIN component_assets a ON c.id = a.component_id
    WHERE c.id = component_id_param
    GROUP BY c.id, c.name, c.description, c.content_type, c.content_id, 
             c.owner_id, c.notebook_path, c.metadata, c.created_at, c.updated_at, c.is_active;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- PERFORMANCE OPTIMIZATIONS
-- ========================================

-- Additional composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_components_content_active 
    ON components(content_type, content_id, is_active);

CREATE INDEX IF NOT EXISTS idx_sessions_component_status 
    ON component_sessions(component_id, status);

CREATE INDEX IF NOT EXISTS idx_sessions_expires_status 
    ON component_sessions(expires_at, status);

-- ========================================
-- SECURITY AND CONSTRAINTS
-- ========================================

-- Ensure session tokens are sufficiently long
ALTER TABLE component_sessions 
ADD CONSTRAINT chk_session_token_length 
CHECK (CHAR_LENGTH(session_token) >= 32);

-- Ensure component IDs follow naming convention (PostgreSQL regex syntax)
ALTER TABLE components 
ADD CONSTRAINT chk_component_id_format 
CHECK (id ~ '^comp-[a-zA-Z0-9]{3,}$');

-- Ensure notebook paths are properly formatted
ALTER TABLE components 
ADD CONSTRAINT chk_notebook_path_format 
CHECK (notebook_path LIKE 'marimo/components/%/%.py');

-- ========================================
-- COMMENTS FOR DOCUMENTATION
-- ========================================

COMMENT ON TABLE components IS 'Core table storing interactive Marimo components linked to articles and labs';
COMMENT ON TABLE component_sessions IS 'Active and historical sessions for component execution and user interaction';
COMMENT ON TABLE component_assets IS 'Files and resources associated with components (data, configs, images, etc.)';

-- Column comments
COMMENT ON COLUMN components.metadata IS 'Component configuration, tags, dependencies, and custom properties';
COMMENT ON COLUMN component_sessions.state_data IS 'Session state data including user inputs and component values';
COMMENT ON COLUMN component_sessions.python_process_id IS 'Process ID of the Python Marimo instance serving this session'; 