# Articles Service

## Agenda

- [Purpose](#purpose)
- [Functionality](#functionality)
- [Entities](#entities)
- [gRPC Contract](#grpc-contract)
- [Integrations](#integrations)
- [User Stories](#user-stories)
- [Technical Details](#technical-details)

## Purpose

The **Articles** service on the **Open Labs Share** platform is the central repository of all scientific articles in the system. It provides a single point of management for scientific content and access to file resources. It allows:
- **Authors** to publish their scientific articles and research papers
- **Teachers** to link their lab work with theoretical materials and articles
- **Students** to read additional educational literature and research materials

## Functionality

### 1. **General**
- CRUD operations for articles and assets
- Data storage with PostgreSQL
- File storage with MinIO
- Control of access to articles

### 2. **For authors:**
- Publication of articles in PDF format
- Management of publications
- Upload and management of article assets

### 3. **For teachers:**
- Linking lab assignments with relevant scientific articles
- Providing students with theoretical background materials

### 4. **For students:**
- Access to scientific articles and research papers
- Downloading article assets for offline study
- Searching and filtering articles by content

## Entities

The service works with the following entities:

### 1. **Article:**

| Field        | Type      | Description |
|--------------|-----------|-------------|
| id (PK)      | BIGSERIAL | Auto-generated primary key |
| owner_id     | BIGINT    | ID of the article creator |
| title        | VARCHAR(255) | Article title |
| created_at   | TIMESTAMP WITH TIME ZONE | Creation timestamp |
| updated_at   | TIMESTAMP WITH TIME ZONE | Last update timestamp |
| abstract     | TEXT      | Article description/summary |
| views        | BIGINT    | Number of views (default: 0) |
| stars        | BIGINT    | Rating stars (default: 0) |
| people_rated | BIGINT    | Number of people who rated (default: 0) |

### 2. **Article Assets:**

| Field       | Type      | Description |
|-------------|-----------|-------------|
| id (PK)     | BIGSERIAL | Auto-generated primary key |
| article_id  | BIGINT    | Foreign key to articles table |
| filename    | VARCHAR(255) | File name |
| filesize    | BIGINT    | File size in bytes |
| upload_date | TIMESTAMP WITH TIME ZONE | Upload timestamp |

## gRPC Contract

The service provides a single gRPC service: `ArticleService`.

### ArticleService

**Articles Management:**
- `CreateArticle`: Creates a new article entry with title, abstract, and owner
- `GetArticle`: Retrieves complete article information by ID
- `GetArticles`: Retrieves a paginated list of articles with optional text search
- `UpdateArticle`: Modifies existing article properties (title, abstract)
- `DeleteArticle`: Permanently removes an article and its assets from the system
- `GetArticlesByUserId`: Retrieves articles owned by a specific user
- `GetArticlesCount`: Returns total number of articles

**Assets Management:**
- `UploadAsset` **(Streaming)**: Uploads article files in chunks via stream
- `UpdateAsset` **(Streaming)**: Updates existing article assets with new files
- `DownloadAsset` **(Streaming)**: Downloads stored files in streaming chunks
- `DeleteAsset`: Removes a specific file asset from storage
- `ListAssets`: Returns all files associated with a particular article

## Integrations

### 1. **API Gateway:**
- A single entry point for all requests

### 2. **MinIO:**
- Storing article files (primarily PDFs)
- Organized bucket structure:
  - `articles/` bucket for article assets

### 3. **PostgreSQL:**
- Primary database for articles and assets metadata
- Handles relationships and constraints

## User Stories

### 1. **The scientist publishes a material:**
- Creates an article with title, abstract, and content
- Uploads a PDF file via streaming upload
- Several other scientists find the material through search functionality

### 2. **The researcher is looking for materials:**
- Uses text search for filtering articles by title and abstract
- Finds several relevant articles
- Downloads article files for offline study

### 3. **The student accesses educational content:**
- Browses articles linked to lab assignments
- Downloads article assets for additional reading
- Accesses theoretical background for practical work

## Technical Details

### Technology Stack:
- **Programming Language:** Python 3.12
- **Inter-service Communication:** gRPC (`grpcio`, `grpcio-tools` libraries)
- **Database:** PostgreSQL via SQLAlchemy (`sqlalchemy`, `sqlalchemy-serializer` libraries)
- **Object Storage:** MinIO (`minio` library)
- **Containerization:** Docker, Docker Compose
- **Config Management:** `python-dotenv`, Environment Variables
- **Testing:** Pytest unit-testing (`pytest` library)
- **Logging:** Python logging (built-in `logging` library)

### Service Architecture:
- Single gRPC service: `ArticleService`
- Streaming support for large file uploads/downloads
- Health check integration for monitoring

### File Storage Structure:
```
Bucket: articles
└── article_id/
    └── article.pdf
```

### Error Handling:
- Comprehensive gRPC status codes
- Detailed error messages and logging
- Graceful handling of file operations
- Validation for required fields and business rules