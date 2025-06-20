# Agenda

- [Purpose](#purpose)
- [Functionality](#functionality)
- [Entities](#entities)
- [gRPC Contract](#grpc-contract)
- [Integrations](#integrations)
- [User Stories](#user-stories)
- [Technical Details](#technical-details)

# Purpose  

The **Labs** service on the **Open Labs Share** platform is the central repository of all laboratory work in the system. It provides a single point of management for educational content and access to file resources. It allows:  
- **Teachers** to create, publish, and review lab assignments
- **Students** to upload solutions, receive grades and feedback
- **Administrators** to control the quality of the content and the verification process

# Functionality 

1. **General**
- CRUD for labs
- Data storage
- Categorization and tagging of labs
- Control of access to labs

2. **For teachers:**
- Creation of laboratory papers with descriptions, evaluation criteria and other details
- Uploading assignment files in Markdown format
- Checking students' decisions with the possibility of commenting

3. **For students:**
- View available lab work
   - Downloading lab solutions
   - Getting ratings and detailed feedback
   - The ability to refine the solution after verification

4. **Additional functions:**
- Connection with theoretical materials (articles, lectures)

# Entities

The service works with the following entities:  

1. **Lab (Laboratory work):**

| Field            | Type          |
| ---------------- | ------------- |
| id (PK)          | UUID / long   |
| title            | string        |
| owner_id         | UUID / long   |
| publication_date | datestamp     |
| files            | list\<string> |
| short_desc       | string        |
| view             | integer       |
| submissions      | integer       |

2. **Solution (Student's solution):**

| Field           | Type          |
| --------------- | ------------- |
| id (PK)         | UUID / long   |
| lab_id          | UUID / long   |
| user_id         | UUID / long   |
| files           | list\<string> |
| submission_date | datestamp     |
| status          | string        |
| points          | integer       |

3. Article relations

| Field           | Type        |
| --------------- | ----------- |
| lab_id (PK)     | UUID / long |
| article_id (PK) | UUID / long |

4. Lab Tags:

| Field       | Type        |
| ----------- | ----------- |
| lab_id (PK) | UUID / long |
| tag (PK)    | string      |


# gRPC Contract

## Labs Management

- `CreateLab`: Creates a new lab entry
- `GetLab`: Retrieves complete lab information by UUID
- `UpdateLab`: Modifies existing lab properties and content
- `DeleteLab`: Permanently removes a lab and its assets from the system

## Solutions Management

- `CreateSolution`: Creates a new solution submission for a lab
- `GetSolution`: Retrieves solution details and metadata
- `UpdateSolution`: Modifies existing solution properties
- `DeleteSolution`: Permanently removes a solution submission

## Asset Management

- `UploadAsset` **(Streaming)**: Uploads files to the lab in chunks via stream
- `DownloadAsset` **(Streaming)**: Downloads stored files in streaming chunks
- `DeleteAsset`: Removes a specific file attachment from storage
- `ListAssets`: Returns all files associated with a particular lab

# Integrations

1. **User Service:**
- Getting user data

2. **API Gateway:**
- A single entry point for all requests

3. **MinIO:**
- Storing task and solution files

# User Stories

1. **The teacher publishes the lab:**
- Creates a task in Markdown format, uploads it via `/labs`
- Sets the evaluation criteria.  
- Students receive notification of a new lab

2. **The student submits the solution:**
- Uploads the solution and report via `/labs/{lab_id}/submit`
- Receives a rating and comments after verification

# Technical Details

- Technological stack:  
	- Backend: **Python 3.12**
	- Integration with external services: **gRPC**
- **Database:** **PostgreSQL**  
- **Deployment:** **Docker**
- **File Storage:** **MinIO**

```
Bucket:
labs
└── lab_id
	├── task.md
	├── example.png
    └── cute_cat.png

submissions
└── submission_id
    └── solution.pdf
```


