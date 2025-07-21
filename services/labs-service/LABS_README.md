# Labs Service

## Agenda

- [Purpose](#purpose)
- [Functionality](#functionality)
- [Entities](#entities)
- [gRPC Contract](#grpc-contract)
- [Integrations](#integrations)
- [User Stories](#user-stories)
- [Technical Details](#technical-details)

## Purpose  

The **Labs** service on the **Open Labs Share** platform is the central repository of all laboratory work in the system. It provides a single point of management for educational content and access to file resources

## Functionality 

### 1. **General**
- CRUD operations for labs, submissions, and tags
- Data storage with PostgreSQL and MongoDB
- File storage with MinIO

### 2. **For publishers:**
- Creation of laboratory papers
- Uploading assignment files
- Checking students' submissions with the grading
- Tag management for organizing labs

### 3. **For students:**
- View available lab work
- Submitting solutions with text and file attachments
- Getting grades

## Entities

The service works with the following entities:  

### 1. **Lab (Laboratory work):**

| Field        | Type                     | Description                             |
|--------------|--------------------------|-----------------------------------------|
| id (PK)      | BIGSERIAL                | Auto-generated primary key              |
| owner_id     | BIGINT                   | ID of the lab creator                   |
| title        | VARCHAR(255)             | Lab title                               |
| created_at   | TIMESTAMP WITH TIME ZONE | Creation timestamp                      |
| updated_at   | TIMESTAMP WITH TIME ZONE | Last update timestamp                   |
| abstract     | TEXT                     | Lab description/summary                 |
| views        | BIGINT                   | Number of views (default: 0)            |
| submissions  | BIGINT                   | Number of submissions (default: 0)      |
| stars        | BIGINT                   | Rating stars (default: 0)               |
| people_rated | BIGINT                   | Number of people who rated (default: 0) |

### 2. **Submission (Student's submission):**

| Field      | Type                     | Description                                                     |
|------------|--------------------------|-----------------------------------------------------------------|
| id (PK)    | BIGSERIAL                | Auto-generated primary key                                      |
| lab_id     | BIGINT                   | Foreign key to labs table                                       |
| owner_id   | BIGINT                   | ID of the submission owner                                      |
| created_at | TIMESTAMP WITH TIME ZONE | Creation timestamp                                              |
| updated_at | TIMESTAMP WITH TIME ZONE | Last update timestamp                                           |
| status     | VARCHAR(50)              | Submission status (NOT_GRADED, IN_PROGRESS, ACCEPTED, REJECTED) |
| points     | INTEGER                  | Points awarded (default: 0)                                     |
| text       | TEXT                     | Submission text content (stored in MongoDB)                     |

### 3. **Tag:**

| Field       | Type                     | Description                   |
|-------------|--------------------------|-------------------------------|
| id (PK)     | INTEGER                  | Primary key                   |
| name        | VARCHAR(255)             | Tag name (unique)             |
| description | TEXT                     | Tag description               |
| created_at  | TIMESTAMP WITH TIME ZONE | Creation timestamp            |
| updated_at  | TIMESTAMP WITH TIME ZONE | Last update timestamp         |
| labs_count  | INTEGER                  | Number of labs using this tag |

### 4. **LabTag (Many-to-many relationship):**

| Field  | Type    | Description               |
|--------|---------|---------------------------|
| lab_id | BIGINT  | Foreign key to labs table |
| tag_id | INTEGER | Foreign key to tags table |

### 5. **Article relations:**

| Field           | Type   | Description               |
|-----------------|--------|---------------------------|
| lab_id (PK)     | BIGINT | Foreign key to labs table |
| article_id (PK) | BIGINT | ID of related article     |

### 6. **Lab Assets:**

| Field       | Type                     | Description                |
|-------------|--------------------------|----------------------------|
| id (PK)     | BIGSERIAL                | Auto-generated primary key |
| lab_id      | BIGINT                   | Foreign key to labs table  |
| filename    | VARCHAR(255)             | File name                  |
| filesize    | BIGINT                   | File size in bytes         |
| upload_date | TIMESTAMP WITH TIME ZONE | Upload timestamp           |

### 7. **Submission Assets:**

| Field         | Type                     | Description                      |
|---------------|--------------------------|----------------------------------|
| id (PK)       | BIGSERIAL                | Auto-generated primary key       |
| submission_id | BIGINT                   | Foreign key to submissions table |
| filename      | VARCHAR(255)             | File name                        |
| filesize      | BIGINT                   | File size in bytes               |
| upload_date   | TIMESTAMP WITH TIME ZONE | Upload timestamp                 |

## gRPC Contract

The service provides three main gRPC services: `LabService`, `SubmissionService`, and `TagService`.

### LabService

**Labs Management:**
- `CreateLab`: Creates a new lab entry with optional tags and article relations
- `GetLab`: Retrieves complete lab information by ID
- `GetLabs`: Retrieves a paginated list of labs with optional text search and tag filtering
- `UpdateLab`: Modifies existing lab properties, tags, and article relations
- `DeleteLab`: Permanently removes a lab and its assets from the system
- `GetLabsByUserId`: Retrieves labs owned by a specific user
- `GetLabsCount`: Returns total number of labs

**Assets Management:**
- `UploadAsset` **(Streaming)**: Uploads files to the lab in chunks via stream
- `UpdateAsset` **(Streaming)**: Updates existing lab assets with new files
- `DownloadAsset` **(Streaming)**: Downloads stored files in streaming chunks
- `DeleteAsset`: Removes a specific file asset from storage
- `ListAssets`: Returns all files associated with a particular lab

### SubmissionService

**Submissions Management:**
- `CreateSubmission`: Creates a new submission for a lab (prevents lab owners from submitting to their own labs)
- `GetSubmission`: Retrieves submission details and metadata with text from MongoDB
- `GetSubmissions`: Retrieves a paginated list of submissions for a specific lab
- `UpdateSubmission`: Modifies existing submission properties and text
- `DeleteSubmission`: Permanently removes a submission and decrements lab submission count
- `GetUsersSubmissions`: Retrieves submissions for a specific user
- `GetPossibleToReviewSubmissions`: Retrieves submissions eligible for review by a user
- `GetSubmissionsCount`: Returns total number of submissions

**Assets Management:**
- `UploadAsset` **(Streaming)**: Uploads files to the submission in chunks via stream
- `UpdateAsset` **(Streaming)**: Updates existing submission assets with new files
- `DownloadAsset` **(Streaming)**: Downloads stored files in streaming chunks
- `DeleteAsset`: Removes a specific file asset from storage
- `ListAssets`: Returns all files associated with a particular submission

### TagService

**Tags Management:**
- `CreateTag`: Creates a new tag with name and description
- `GetTag`: Retrieves a tag by ID
- `GetTags`: Retrieves a paginated list of tags
- `GetTagsByIds`: Retrieves multiple tags by their IDs
- `UpdateTag`: Modifies existing tag properties
- `DeleteTag`: Permanently removes a tag

## Integrations

### 1. **API Gateway:**
- API Gateway requests gRPC methods that provided in [gRPC Contract](#grpc-contract) and receives requested data
  - For example, API Gateway requests `GetLabs` with provided page number, page size, required text matching, and required tags matching.
    Receives from the Labs Service `LabList` with amount of `Labs` and lab entities themselves
  - More about that you can find at `/app/proto/labs_service.proto`, `/app/proto/submissions_service.proto`, and `/app/proto/tags_service.proto`

### 2. **MinIO:**
- Storing labs and submissions files
- Organized bucket structure:
  - `labs/` bucket for lab assets
  - `submissions/` bucket for submission assets

### 3. **PostgreSQL:**
- Primary database for labs, submissions, tags, and assets metadata
- Handles relationships and constraints

### 4. **MongoDB:**
- Stores submission text content
- Provides flexible text storage for large submission content

## User Stories

### 1. **The author publishes the lab:**
- Creates a lab with title, abstract, and optional tags
- Uploads assignment files via streaming upload
- Students can discover the lab through search

### 2. **The student submits the solution:**
- Creates a submission with text content
- Uploads solution files via streaming upload
- Receives grading update after author's review

## Technical Details

### Technology Stack:
- **Programming Language:** Python 3.12
- **Inter-service Communication:** gRPC (`grpcio`, `grpcio-tools` libraries)
- **Database (Relational):** PostgreSQL via SQLAlchemy (`sqlalchemy`, `sqlalchemy-serializer` libraries)
- **Database (NoSQL):** MongoDB via PyMongo (`pymongo` library)
- **Object Storage:** MinIO (`minio` library)
- **Containerization:** Docker, Docker Compose
- **Config Management:** `python-dotenv`, Environment Variables
- **Testing:** Pytest unit-testing (`pytest` library)
- **Logging:** Python logging (built-in `logging` library)

### Service Architecture:

Labs Service is organized into several 

```
app/
├── db/                   # Files used for database testing purposes
├── proto/                # gRPC `.proto` files for service integration and communication
├── services/             # Splitted python services handling labs, submissions, and tags operations
├── utils/                # Service utilities
├── .dockerignore         # Excludes files and directories from Docker build context
├── .env.example          # Example of .env file with environment variables for local docker compose
├── .gitignore            # Excludes files and directories from Git context
├── client.py             # Example of client which calls service gRPC methods
├── config.py             # Config file which gets environment variables values
├── docker-compose.yml    # Docker Compose file for local build
├── Dockerfile            # Docker service build file
├── main.py               # Labs Service maintainer
├── requirements.txt      # Requirements file for docker build
└── tester.py             # Service tester using client methods to check service work correctness
```


### File Storage Structure:
```
Bucket: labs
└── lab_id/
    ├── lab.md
    ├── example.png
    └── cute_cat.png

Bucket: submissions
└── submission_id/
    └── solution.pdf
```

### Error Handling:
- Comprehensive gRPC status codes
- Detailed error messages and logging
- Graceful handling of file operations
- Validation for business rules (e.g., preventing self-submissions)
