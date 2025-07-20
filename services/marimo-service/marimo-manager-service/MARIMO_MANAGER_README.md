# Marimo Java Service

## Purpose

This service provides the backend functionality for managing and interacting with Marimo notebooks. It serves as the primary interface for the frontend, handling component creation, session management, code execution, and asset storage. It orchestrates communication with the `marimo-executor-service` for code execution and persists all state to a PostgreSQL database.

## Database Entities

### `components`

Stores the core information about a Marimo notebook component.

```sql
CREATE TABLE components (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    content_id VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    notebook_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
```

### `component_sessions`

Manages active and historical user sessions for each component.

```sql
CREATE TABLE component_sessions (
    id VARCHAR(255) PRIMARY KEY,
    component_id VARCHAR(255) NOT NULL REFERENCES components(id) ON DELETE CASCADE,
    session_name VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    state_data JSONB,
    python_process_id VARCHAR(255),
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    last_accessed TIMESTAMP WITHOUT TIME ZONE,
    expires_at TIMESTAMP WITHOUT TIME ZONE
);
```

### `component_assets`

Tracks all assets (e.g., data files, images) associated with a component.

```sql
CREATE TABLE component_assets (
    id VARCHAR(255) PRIMARY KEY,
    component_id VARCHAR(255) NOT NULL REFERENCES components(id) ON DELETE CASCADE,
    asset_type VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255),
    file_size BIGINT,
    metadata JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE
);
```

### `execution_history`

Logs every cell execution for auditing and history purposes.

```sql
CREATE TABLE execution_history (
    id VARCHAR(255) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL REFERENCES component_sessions(id) ON DELETE CASCADE,
    cell_id VARCHAR(255),
    code TEXT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    execution_time_ms BIGINT,
    output_count INTEGER,
    created_at TIMESTAMP WITHOUT TIME ZONE
);
```

### `widget_state`

Stores the current state and values of interactive widgets within sessions.

```sql
CREATE TABLE widget_state (
    id VARCHAR(255) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL REFERENCES component_sessions(id) ON DELETE CASCADE,
    widget_id VARCHAR(255) NOT NULL,
    widget_type VARCHAR(255) NOT NULL,
    current_value JSONB,
    constraints_data JSONB,
    metadata JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);
``` 