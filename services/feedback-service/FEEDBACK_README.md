# Feedback Service

## Agenda

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Communication](#communication)
4. [Storage Architecture](#storage-architecture)
5. [Business Logic](#business-logic)
6. [Proto Contract Summary](#proto-contract-summary)


## Overview

The **Feedback Service** handles the creation, retrieval, and management of feedback for labs. Feedback entries are stored as Markdown documents and can include attached files (assets). The service also supports comments for discussion.

---

## Tech Stack

The Feedback Service is built with the following technologies:

- **Language**: Go
- **Framework**: gRPC for building the microservice API.
- **Databases**:
  - **PostgreSQL**: Stores structured data like feedback metadata.
  - **MongoDB**: Used for storing unstructured data such as comments and feedback content.
- **Object Storage**:
  - **MinIO**: Used for storing file attachments associated with feedback.

---

## Communication

The Feedback Service communicates with other services in the ecosystem through gRPC.

### Inbound Communication

The service exposes gRPC endpoints defined in the `.proto` files (`feedback_service.proto`, `comment_service.proto`). Other services, such as the **API Gateway**, consume these endpoints to interact with the feedback and comment functionalities.

### Outbound Communication

The Feedback Service is self-contained in terms of business logic and does not make any outbound calls to other microservices. Its external dependencies are limited to its data stores: PostgreSQL, MongoDB, and MinIO.

---

## Storage Architecture

### PostgreSQL Database

The PostgreSQL database stores metadata for feedback entries, establishing relationships between users, labs, and submissions.

#### Tables

- **`feedbacks`**
  - `id` (UUID): Primary key, auto-generated.
  - `reviewer_id` (BIGINT): The ID of the user who created the feedback.
  - `student_id` (BIGINT): The ID of the student whose submission is being reviewed.
  - `submission_id` (BIGINT): The ID of the submission being reviewed.
  - `title` (VARCHAR): The title of the feedback.
  - `created_at` (TIMESTAMP): The timestamp of when the feedback was created.
  - `updated_at` (TIMESTAMP): The timestamp of the last update.

### MongoDB

MongoDB is used for storing comments and feedback content due to its flexible schema, which is well-suited for unstructured text data.

-   **`feedback_content` Collection**: Stores the Markdown content of each feedback entry, linked by the feedback UUID.
    -   `_id` (string): The feedback UUID.
    -   `content` (string): The Markdown content of the feedback.
-   **`comments` Collection**: Stores comments for labs and articles, supporting threaded discussions.
    -   `_id` (ObjectID): The unique identifier for the comment.
    -   `content_id` (BIGINT): The ID of the content (e.g., lab or article) the comment belongs to.
    -   `user_id` (BIGINT): The ID of the user who created the comment.
    -   `parent_id` (string, nullable): The ID of the parent comment for threaded replies.
    -   `content` (string): The Markdown content of the comment.
    -   `created_at` (TIMESTAMP): The timestamp of when the comment was created.
    -   `updated_at` (TIMESTAMP): The timestamp of the last update.
    -   `type` (string): The type of content the comment belongs to (e.g., "lab", "article").

### Object Storage (MinIO)

MinIO is used for storing attachments (e.g., images, documents) associated with feedback. Each file is stored in a structured path within the `feedback` bucket.

-   **Bucket**: `feedback`
-   **Object Path**: `feedback_id/filename`

The structure for storing feedback attachments is as follows:
```
feedback/
└── {feedback_id}/
    ├── diagram.jpg
    └── report.pdf
```

---

## Business Logic

### Feedback Management

The feedback management system allows reviewers to create, update, and delete feedback for student submissions. Students can view their feedback, and both students and reviewers can list feedback entries with pagination.

-   **`CreateFeedback`**: Creates a new feedback entry for a specific submission, including a title and Markdown content.
-   **`GetFeedbackById`**: Retrieves a single feedback entry by its unique ID.
-   **`UpdateFeedback`**: Allows reviewers to update the title or content of feedback they have created.
-   **`DeleteFeedback`**: Removes a feedback entry and all associated attachments from MinIO.
-   **`ListReviewerFeedbacks`**: Lists all feedback created by a specific reviewer, with optional filtering by submission and pagination.
-   **`GetStudentFeedback`**: Retrieves feedback for a student for a specific submission.
-   **`ListStudentFeedbacks`**: Lists all feedback for a specific student, with optional filtering by submission and pagination.

### Attachment Management

The attachment management system allows reviewers to upload and delete files associated with feedback. Both students and reviewers can download and list attachments.

-   **`UploadAttachment`**: Uploads a file to MinIO and associates it with a feedback entry. This is a streaming RPC that accepts a metadata header followed by binary chunks.
-   **`DownloadAttachment`**: Downloads an attachment from MinIO. This is a streaming RPC that returns attachment metadata followed by binary chunks.
-   **`ListAttachments`**: Lists all attachments associated with a feedback entry.
-   **`DeleteAttachment`**: Deletes an attachment from MinIO.
-   **`GetAttachmentLocation`**: Retrieves the MinIO location information for an attachment, including the bucket, object path, and endpoint.

### Comment Management

The comment management system supports threaded discussions on labs and articles. Users can create, view, update, and delete comments.

-   **`CreateComment`**: Creates a new comment on a lab or article, with support for threaded replies by specifying a `parent_id`.
-   **`GetComment`**: Retrieves a single comment by its unique ID.
-   **`UpdateComment`**: Allows users to update the content of their own comments.
-   **`DeleteComment`**: Deletes a comment and all of its replies in a cascading manner.
-   **`ListComments`**: Lists all top-level comments for a specific lab or article, with pagination.
-   **`GetCommentReplies`**: Retrieves all replies to a specific comment, with pagination.

---

## Proto Contract Summary

The gRPC services are defined in `feedback_service.proto` and `comment_service.proto`.

### Feedback Service

-   **`CreateFeedback`**: Creates a new feedback entry.
-   **`GetFeedbackById`**: Retrieves a feedback entry by its unique ID.
-   **`UpdateFeedback`**: Updates an existing feedback entry.
-   **`DeleteFeedback`**: Deletes a feedback entry.
-   **`ListReviewerFeedbacks`**: Lists feedback created by a specific reviewer.
-   **`GetStudentFeedback`**: Retrieves feedback for a student for a specific submission.
-   **`ListStudentFeedbacks`**: Lists all feedback for a specific student.

### Attachment Operations

-   **`UploadAttachment`**: Uploads an attachment in a streaming RPC.
-   **`DownloadAttachment`**: Downloads an attachment in a streaming RPC.
-   **`ListAttachments`**: Lists all attachments for a feedback entry.
-   **`DeleteAttachment`**: Deletes an attachment.
-   **`GetAttachmentLocation`**: Retrieves the MinIO location information for an attachment.

### Comment Service

-   **`CreateComment`**: Creates a new comment.
-   **`GetComment`**: Retrieves a comment by its unique ID.
-   **`UpdateComment`**: Updates an existing comment.
-   **`DeleteComment`**: Deletes a comment.
-   **`ListComments`**: Lists comments for a lab or article.
-   **`GetCommentReplies`**: Retrieves replies to a specific comment.

---