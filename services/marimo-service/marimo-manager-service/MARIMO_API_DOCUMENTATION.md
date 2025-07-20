# Marimo Service API Documentation

## Base URL `/api/v1/marimo`

This API provides comprehensive endpoints for managing Marimo components, sessions, execution, and assets.

---

## 1. Component Management

### 1.1 Create Component
**Endpoint:** `POST /components`
**Description:** Creates a new Marimo component (notebook).
**Request Body:**
```json
{
  "name": "My Analysis Notebook",
  "contentType": "lab_report",
  "contentId": "lab-123",
  "ownerId": "user-abc",
  "initialCode": "import marimo as mo\n\nmo.md(\"# Welcome to Marimo!\")"
}
```
**Response (200 OK):** `ComponentDto`

### 1.2 Get Component
**Endpoint:** `GET /components/{id}`
**Description:** Retrieves a specific component by its ID.
**Response (200 OK):** `ComponentDto`

### 1.3 Update Component
**Endpoint:** `PUT /components/{id}`
**Description:** Updates a component's name.
**Request Body:**
```json
{
  "name": "My Updated Analysis Notebook"
}
```
**Response (200 OK):** `ComponentDto`

### 1.4 Delete Component
**Endpoint:** `DELETE /components/{id}`
**Description:** Deletes a component and its associated data.
**Response (204 No Content):**

### 1.5 List Components
**Endpoint:** `GET /components`
**Description:** Lists components with optional filtering and pagination.
**Query Parameters:** `contentType`, `contentId`, `ownerId`, `page`, `size`
**Response (200 OK):** `PagedResponse<ComponentDto>`

### 1.6 Search Components
**Endpoint:** `GET /components/search`
**Description:** Searches for components by a query string.
**Query Parameters:** `query`, `page`, `size`
**Response (200 OK):** `PagedResponse<ComponentDto>`

### 1.7 Get Component by Content
**Endpoint:** `GET /components/{contentType}/{contentId}`
**Description:** Retrieves a component by its content type and content ID.
**Response (200 OK):** `ComponentDto`

### 1.8 Delete Components by Content
**Endpoint:** `DELETE /components/by-content`
**Description:** Deletes components associated with specific content.
**Query Parameters:** `contentType`, `contentId`
**Response (204 No Content):**

---

## 2. Session Management

### 2.1 Start Session
**Endpoint:** `POST /sessions`
**Description:** Starts a new execution session for a component.
**Request Body:**
```json
{
  "componentId": "comp-xyz",
  "userId": "user-abc",
  "sessionName": "Data Exploration"
}
```
**Response (200 OK):** `SessionInfoDto`

### 2.2 End Session
**Endpoint:** `DELETE /sessions/{id}`
**Description:** Ends an active session.
**Response (204 No Content):**

### 2.3 Get Session Status
**Endpoint:** `GET /sessions/{id}`
**Description:** Retrieves the status and details of a specific session.
**Response (200 OK):** `SessionInfoDto`

### 2.4 List User Sessions
**Endpoint:** `GET /sessions`
**Description:** Lists sessions for a user with pagination and status filtering.
**Query Parameters:** `userId`, `status` (ACTIVE, IDLE, EXPIRED), `page`, `size`
**Response (200 OK):** `PagedResponse<SessionInfoDto>`

### 2.5 Get Execution History
**Endpoint:** `GET /sessions/{sessionId}/history`
**Description:** Retrieves the execution history for a session.
**Query Parameters:** `page`, `size`
**Response (200 OK):** `PagedResponse<ExecutionRecordDto>`

### 2.6 Get Session Variables
**Endpoint:** `GET /sessions/{sessionId}/variables`
**Description:** Retrieves the current variables and their state from the session.
**Response (200 OK):** `List<VariableInfoDto>`

---

## 3. Widget Management

### 3.1 Update Widget Value
**Endpoint:** `PUT /sessions/{sessionId}/widgets/{widgetId}/value`
**Description:** Updates the value of a specific widget in a session.
**Request Body:**
```json
{
  "value": "new_widget_value"
}
```
**Response (200 OK):** `WidgetStateDto`

### 3.2 Batch Update Widgets
**Endpoint:** `PUT /sessions/{sessionId}/widgets/batch`
**Description:** Updates multiple widget values in a single request.
**Request Body:**
```json
{
  "updates": [
    {
      "widgetId": "widget-1",
      "value": "value1"
    },
    {
      "widgetId": "widget-2", 
      "value": "value2"
    }
  ]
}
```
**Response (200 OK):** `List<WidgetStateDto>`

### 3.3 Get Widget Analytics
**Endpoint:** `GET /sessions/{sessionId}/widgets/analytics`
**Description:** Retrieves analytics data for widgets in a session.
**Response (200 OK):** `WidgetAnalyticsDto`

### 3.4 Get Widget State
**Endpoint:** `GET /sessions/{sessionId}/widgets/{widgetId}/state`
**Description:** Retrieves the current state of a specific widget.
**Response (200 OK):** `WidgetStateDto`

### 3.5 Get Widget Constraints
**Endpoint:** `GET /sessions/{sessionId}/widgets/{widgetId}/constraints`
**Description:** Retrieves the constraints for a specific widget.
**Response (200 OK):** `WidgetConstraintsDto`

---

## 4. Code Execution

### 4.1 Execute Cell
**Endpoint:** `POST /sessions/{sessionId}/execute`
**Description:** Executes a code cell within a session.
**Request Body:**
```json
{
  "cellId": "cell-123",
  "code": "x = 10\ny = 20\nx + y"
}
```
**Response (200 OK):** `ExecuteCellResponseDto`

---

## 5. Asset Management

### 5.1 Upload Asset
**Endpoint:** `POST /assets/upload`
**Description:** Uploads a file as a component asset.
**Request Type:** `multipart/form-data`
**Form Fields:**
- `file`: The file to upload.
- `componentId`: ID of the component to associate the asset with.
- `assetType`: Type of asset (e.g., `DATA`, `IMAGE`).
- `metadata` (optional): Key-value pairs.
**Response (200 OK):** `AssetInfoDto`

### 5.2 Get Asset Info
**Endpoint:** `GET /assets/{id}`
**Description:** Retrieves metadata for a specific asset.
**Response (200 OK):** `AssetInfoDto`

### 5.3 Download Asset
**Endpoint:** `GET /assets/{id}/download`
**Description:** Downloads the binary content of an asset.
**Response (200 OK):** `ByteArrayResource` (File stream)

### 5.4 List Assets
**Endpoint:** `GET /components/{componentId}/assets`
**Description:** Lists all assets associated with a component.
**Response (200 OK):** `List<AssetInfoDto>`

### 5.5 Delete Asset
**Endpoint:** `DELETE /assets/{id}`
**Description:** Deletes an asset.
**Response (204 No Content):**

---

## 6. Health and Monitoring

### 6.1 Health Check
**Endpoint:** `GET /health`
**Description:** Returns the health status of the service.
**Response (200 OK):** `HealthStatusDto` 