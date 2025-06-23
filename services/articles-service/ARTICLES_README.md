# Agenda

- [Purpose](#purpose)
- [Functionality](#functionality)
- [Entities](#entities)
- [gRPC Contract](#grpc-contract)
- [Integrations](#integrations)
- [User Stories](#user-stories)
- [Technical Details](#technical-details)

# Purpose

Article Service is the central repository of all scientific articles on the Open Labs Share platform. It provides a single point of management for scientific content and access to file resources. The service provides opportunities for:
- **Authors** to publish their scientific articles
- **Teachers** link their lab work with articles
- **Students** read additional educational literature

# Functionality

1. General
- CRUD for articles
- Data Storage
- Control of access to articles

2. For authors:
- Publication of articles in PDF format
- Manage your publications (update versions)

# Entities

1. **Article:**

| Field        | Type           |
|--------------|----------------|
| id (PK)      | UUID / long    |
| owner_id     | UUID / long    |
| title        | string         |
| created_at   | datestamp      |
| updated_at   | datestamp      |
| abstract     | string         |
| views        | integer        |
| stars        | integer / long |
| people_rated | integer / long |


2. **Article assets:**

| Field       | Type           |
|-------------|----------------|
| id (PK)     | UUID / long    |
| article_id  | UUID / long    |
| filename    | string         |
| total_size  | integer / long |
| uploaded_at | datestamp      |


# gRPC Contract

More gRPC details you can find in `articles.proto` file

## Articles Management

- `CreateArticle`: Creates a new article entry
- `GetArticle`: Retrieves complete article information by UUID
- `GetArticles`: Retrieves a list of articles with pagination 
- `UpdateArticle`: Modifies existing article properties and content by its UUID
- `DeleteArticle`: Permanently removes an article and its assets from the system by its UUID 

## Assets Management

- `UploadAsset` **(Streaming)**: Uploads article attachments via chunked streaming
- `DownloadAsset` **(Streaming)**: Downloads stored article files in streaming chunks
- `DeleteAsset`: Removes a specific file attachment from storage
- `ListAssets`: Returns all files associated with a particular article

# Integrations

1. **User Service:**
- Obtaining data about authors and reviewers
- Checking access rights

2. **API Gateway:**
- A single entry point for all requests

3. **MinIO Storage:**
- Storing PDF and other article files

# User Stories

1. **The teacher publishes a tutorial:**
- Uploads a PDF file via POST /articles
- Students find the material through a search

2. **The researcher is looking for materials:**
- Uses searching for filtering articles
- Finds several relevant articles
- Saves them to bookmarks for further study

# Technical Details
- Technological stack:`
	- **Backend:** Python 3.12
	- **Internal Communications:** gRPC
- **Database:** PostgreSQL
- **Deployment:** Docker
- **File Storage:** MinIO
```
Bucket:
articles
└── article_id
    └── article.pdf
```