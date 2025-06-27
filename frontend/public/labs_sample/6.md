# Feedback Service

## Overview

The **Feedback Service** handles the creation, retrieval, and management of feedback for labs. Feedback entries are stored as Markdown documents and can include attached files (assets). The service also supports comments for discussion.

---

## Storage Architecture

### PostgreSQL Database

#### Tables

- **`feedbacks`**
    - `id` (UUID): Primary key, auto-generated
    - `user_id` (BIGINT): Author of the feedback
    - `lab_id` (BIGINT): Related lab assignment
    - `title` (VARCHAR): Feedback title
    - `created_at` (TIMESTAMP): Creation timestamp

- **`feedback_assets`**
    - `id` (UUID): Primary key, auto-generated
    - `feedback_id` (UUID): Foreign key to `feedbacks.id`
    - `filename` (VARCHAR): File name of the uploaded asset
    - `created_at` (TIMESTAMP): Upload timestamp

- **`lab_comments`**
    - `id` (UUID): Primary key, auto-generated
    - `lab_id` (BIGINT): Target lab
    - `user_id` (BIGINT): Author of the comment
    - `parent_id` (UUID, nullable): Parent comment for threaded replies
    - `content` (TEXT): Comment content
    - `created_at` (TIMESTAMP): Comment timestamp

### Object Storage (MinIO)

- Files are stored in MinIO.
- Each asset is associated with a feedback ID and a file name.
```
feedback/
├── feedback_id/
│   ├── content.md              # Markdown feedback content
│   └── assets/                 # Associated asset files
│       ├── diagram.jpg
│       └── attachment.png
```
---

## Business Logic

### Feedback Management

- **CreateFeedback**: Stores a new feedback entry (user, lab, title, content).
- **GetFeedback**: Retrieves a feedback by UUID.
- **UpdateFeedback**: Allows partial updates (title or content).
- **DeleteFeedback**: Removes feedback and deletes associated assets.
- **ListUserFeedbacks**: Lists feedbacks by user and optionally by lab, supports pagination.

### Asset Management

- **UploadAsset (streaming)**: Upload a file using a metadata header and subsequent binary chunks.
- **DownloadAsset (streaming)**: Return asset metadata and stream the binary content.
- **ListAssets**: List all files associated with a feedback entry.

---

## External Service Dependencies

| Service         | Required Data         | Purpose                                                          |
|----------------|------------------------|------------------------------------------------------------------|
| **User Service** | `user_id` validation  | Ensures user exists and to relate userd_id with comment/feedback |
| **Lab Service**  | `lab_id` validation   | Ensure lab exists and to relate lab_id with feedback             |

---

## Proto Contract Summary

gRPC service is defined in `feedback.proto`. Main RPC methods:

- `CreateFeedback`, `GetFeedback`, `UpdateFeedback`, `DeleteFeedback`
- `ListUserFeedbacks`
- `UploadAsset`, `DownloadAsset`, `ListAssets`

---