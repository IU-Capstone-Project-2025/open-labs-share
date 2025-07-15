# Database: `marimo_service`

## Core Design Principles

1. **Component Independence**: Each component is self-contained with its own lifecycle, assets, and session management
2. **Session Isolation**: Multiple users can interact with the same component simultaneously without interference
3. **Asset Decoupling**: Files and resources are stored in MinIO with database references, avoiding blob storage in PostgreSQL
4. **Stateless Components**: Component definitions are immutable; state is maintained separately in sessions

## Table Architecture

### 1. `components` - Component Registry & Metadata

**Purpose**: Central registry for all interactive components embedded in labs and articles. This table serves as the **source of truth** for component definitions and ownership.

```sql
CREATE TABLE components (
    id VARCHAR(255) PRIMARY KEY,                    -- Format: comp-{uuid} for global uniqueness
    name VARCHAR(255) NOT NULL,                     -- Human-readable component name
    content_type VARCHAR(50) CHECK (content_type IN ('article', 'lab')), -- Parent content type
    content_id VARCHAR(255) NOT NULL,               -- Reference to parent article/lab ID
    owner_id VARCHAR(255) NOT NULL,                 -- User ID of component creator
    notebook_path VARCHAR(500) NOT NULL,            -- MinIO path to .py notebook file
    metadata JSONB DEFAULT '{}',                    -- Extensible component configuration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true                  -- Soft delete flag
);

-- Performance indexes for common access patterns
CREATE INDEX idx_components_content_lookup ON components (content_type, content_id) WHERE is_active = true;
CREATE INDEX idx_components_owner ON components (owner_id);
CREATE INDEX idx_components_created ON components (created_at DESC);
```

**Common Query Patterns**:

```sql
-- Get all components for a specific lab
SELECT * FROM components 
WHERE content_type = 'lab' AND content_id = 'lab-123' AND is_active = true;

-- Find components by owner with recent activity
SELECT c.*, COUNT(cs.id) as active_sessions
FROM components c
LEFT JOIN component_sessions cs ON c.id = cs.component_id AND cs.status = 'active'
WHERE c.owner_id = 'user-456' AND c.is_active = true
GROUP BY c.id
ORDER BY c.created_at DESC;
```

### 2. `component_sessions` - Active Execution Context

**Purpose**: Manages **running instances** of components with isolated execution environments. Each session represents a separate Python process serving the component to one or more users.

```sql
CREATE TABLE component_sessions (
    id VARCHAR(255) PRIMARY KEY,                    -- Format: sess-{uuid}
    component_id VARCHAR(255) REFERENCES components(id) ON DELETE CASCADE,
    session_token VARCHAR(500) UNIQUE NOT NULL,     -- Secure token for session access
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'expired', 'failed')),
    state_data JSONB DEFAULT '{}',                  -- Current component state (slider values, etc.)
    python_process_id VARCHAR(100),                 -- OS process ID for Python service management
    expires_at TIMESTAMP NOT NULL,                  -- Automatic session expiration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- Auto-updated by trigger on row changes
);

-- PostgreSQL trigger to automatically update last_activity on any row update
CREATE OR REPLACE FUNCTION update_last_activity_trigger() RETURNS TRIGGER AS $$
BEGIN
    NEW.last_activity = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auto_update_last_activity
    BEFORE UPDATE ON component_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_last_activity_trigger();

-- Performance and cleanup indexes
CREATE INDEX idx_sessions_component_active ON component_sessions (component_id) WHERE status = 'active';
CREATE INDEX idx_sessions_expiration ON component_sessions (expires_at) WHERE status = 'active';
CREATE INDEX idx_sessions_token ON component_sessions (session_token);
CREATE INDEX idx_sessions_process ON component_sessions (python_process_id) WHERE python_process_id IS NOT NULL;
```

**Session Lifecycle Management**:

```sql
-- Create new session (typically called by Java service)
INSERT INTO component_sessions (id, component_id, session_token, expires_at)
VALUES ('sess-' || gen_random_uuid(), 'comp-123', generate_secure_token(), NOW() + INTERVAL '4 hours');

-- Update session activity and state
-- NOTE: last_activity is automatically updated by PostgreSQL trigger on any row update
UPDATE component_sessions 
SET state_data = '{"slider_value": 42, "plot_type": "scatter"}'
WHERE session_token = 'secure-token-here' AND status = 'active';

-- Alternative: Explicit last_activity update when needed
UPDATE component_sessions 
SET last_activity = NOW(), state_data = '{"slider_value": 42, "plot_type": "scatter"}'
WHERE session_token = 'secure-token-here' AND status = 'active';

-- Cleanup expired sessions (automated background task)
DELETE FROM component_sessions 
WHERE expires_at < NOW() OR status IN ('expired', 'failed');
```

### 3. `component_assets` - File & Resource Management

**Purpose**: Tracks **all files and resources** associated with components, maintaining references to MinIO storage locations. This table serves as an inventory for component dependencies.

```sql
CREATE TABLE component_assets (
    id VARCHAR(255) PRIMARY KEY,                    -- Format: asset-{uuid}
    component_id VARCHAR(255) REFERENCES components(id) ON DELETE CASCADE,
    asset_type VARCHAR(50) CHECK (asset_type IN ('data', 'image', 'notebook', 'config')),
    file_path VARCHAR(500) NOT NULL,                -- Full MinIO path: marimo/components/{comp-id}/data/dataset.csv
    original_filename VARCHAR(255),                 -- User-uploaded filename
    mime_type VARCHAR(100),                         -- Content type for proper serving
    file_size BIGINT,                              -- Size in bytes for quota management
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'                     -- File-specific metadata (dimensions, encoding, etc.)
);

-- Asset management indexes
CREATE INDEX idx_assets_component ON component_assets (component_id, asset_type);
CREATE INDEX idx_assets_path ON component_assets (file_path);
CREATE INDEX idx_assets_size ON component_assets (file_size DESC);
CREATE INDEX idx_assets_upload ON component_assets (upload_timestamp DESC);
```

**Asset Management Patterns**:

```sql
-- Get all data files for a component
SELECT file_path, original_filename, file_size 
FROM component_assets 
WHERE component_id = 'comp-123' AND asset_type = 'data'
ORDER BY upload_timestamp DESC;

-- Calculate storage usage by component
SELECT component_id, SUM(file_size) as total_bytes, COUNT(*) as file_count
FROM component_assets 
GROUP BY component_id;

-- Find large assets for cleanup
SELECT ca.*, c.name as component_name
FROM component_assets ca
JOIN components c ON ca.component_id = c.id
WHERE ca.file_size > 10 * 1024 * 1024  -- Files larger than 10MB
ORDER BY ca.file_size DESC;
```

## Data Flow & Relationships

### Component Creation Workflow

```-
1. Java Service receives component creation request
2. Generates component ID and creates MinIO directory structure
3. Uploads notebook file to MinIO (marimo/components/{id}/notebook.py)
4. Creates component record in database
5. Creates asset records for uploaded files
6. Python Service validates notebook syntax via gRPC
```

### Session Execution Workflow

```-
1. Frontend requests component interaction
2. Java Service checks for active session or creates new one
3. Java Service calls Python Service via gRPC to start/resume session
4. Python Service launches isolated Marimo process
5. Session token enables direct frontend-to-Python communication
6. State changes are persisted to component_sessions.state_data
7. Session expires automatically or via explicit cleanup
```

### Asset Management Integration

```-
MinIO Structure: marimo/components/{component-id}/
├── notebook.py              (tracked in components.notebook_path)
├── data/
│   ├── dataset.csv         (tracked in component_assets)
│   └── parameters.json     (tracked in component_assets)
├── assets/
│   ├── plot_config.json    (tracked in component_assets)
│   └── images/
│       └── background.png  (tracked in component_assets)
└── config/
    └── settings.yml        (tracked in component_assets)
```

## Monitoring & Maintenance

### Health Metrics

```sql
-- Session health dashboard
SELECT 
    status,
    COUNT(*) as session_count,
    AVG(EXTRACT(EPOCH FROM (NOW() - created_at))/60) as avg_duration_minutes
FROM component_sessions 
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY status;

-- Storage utilization by component type
SELECT 
    c.content_type,
    COUNT(DISTINCT c.id) as component_count,
    SUM(ca.file_size) as total_storage_bytes
FROM components c
LEFT JOIN component_assets ca ON c.id = ca.component_id
WHERE c.is_active = true
GROUP BY c.content_type;
```

### Automated Cleanup Jobs

```sql
-- Daily cleanup of expired sessions
DELETE FROM component_sessions 
WHERE expires_at < NOW() - INTERVAL '1 day';

-- Weekly cleanup of inactive components (soft delete recovery period)
UPDATE components 
SET is_active = false 
WHERE is_active = true 
  AND created_at < NOW() - INTERVAL '90 days'
  AND id NOT IN (
    SELECT DISTINCT component_id 
    FROM component_sessions 
    WHERE created_at > NOW() - INTERVAL '30 days'
  );
```
