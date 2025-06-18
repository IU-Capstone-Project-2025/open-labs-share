# Agenda

- [Purpose](#Purpose)
- [Functionality](#Functionality)
- [Entities](#Entities)
- [API](#API)
- [Integrations](#Integrations)
- [User Stories](#User%20Stories)
- [Technical Details](#Technical%20Details)

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

Note that reviews will be provided by **Feedback Service**

# API

Done, according with [Labs Service API Docs](https://github.com/LuminiteTime/Open-Labs-Share-Docs/blob/main/Backend/API%20Endpoints.md#labs-service)

| Endpoint                                                  | Request Type | Description                                               |
| --------------------------------------------------------- | ------------ | --------------------------------------------------------- |
| [`/labs`](#get-labs-list)                                 | `GET`        | Get list of labs with pagination                          |
| [`/labs`](#create-lab)                                    | `POST`       | Create new lab with markdown and supporting files         |
| [`/labs/{lab_id}`](#get-lab-by-id)                        | `GET`        | Get lab by ID                                             |
| [`/labs/{lab_id}/sumbit`](#submit-lab-solution)           | `POST`       | Submit a solution of the lab as PDF file                  |
| [`/labs/{lab_id}/sumbissions`](#get-lab-sumbissions-list) | GET          | Get list of submissions for specified lab with pagination |

### Get-Labs-List

**Retrieve available labs**
- **Endpoint:** `GET /labs/get`
- **Authentication:** Required
- **Description:** Get list of labs with pagination

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 1)
- `limit` (integer, optional) - Items per page (default: 20, max: 100)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
	"id": number,
	"title": "string",
	"short_desc": "string",
	"created_at": "string (ISO 8601)",
	"views": number,
	"submissions": number,
	"author_id": number,
	"author_name": "string",
	"author_surname": "string"
}
```

### Create-Lab

**Create new lab with markdown and supporting files**
- **Endpoint:** `POST /labs`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Create new lab with main md file and supporting assets

**Request Body (From Data):**
```json
title: string (required) - Lab title
short_desc: string (required) - Short description of the lab
md_file: file (required) - Markdown file with lab instructions
assets[]: file[] (optional) - Supporting files (images, code, etc.)
```

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
	"id": number,
	"message": "Article created successfully"
}
```

### Get-Lab-by-ID

**Get specified lab:**
- **Endpoint:** `GET /labs/get/{lab_id}`
- **Authentication:** Required
- **Description:** Get lab by ID


**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "labs": [
    {
      "id": number,
      "title": "string",
      "short_desc": "string",
      "created_at": "string (ISO 8601)",
      "views": number,
      "submissions": number,
      "author_id": number,
      "author_name": "string",
      "author_surname": "string"
    }
  ],
  "pagination": {
    "current_page": "integer",
    "total_pages": "integer",
    "total_items": "integer"
  }
}
```

**Error Responses:**
- `404 Not Found` - Lab not found


### Submit-Lab-Solution

**Submit Lab Solution as PDF file**
- **Endpoint:** `POST /labs/{lab_id}/sumbit`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Sumbit a solution of the lab as PDF file

**Request Body (From Data):**
```json
pdf_file: file (required) - PDF document file
```

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
	"submission_id": number,
	"message": "Article created successfully"
}
```

**Error Responses:**
- `404 Not Found` - Lab not found

### Get-Lab-Sumbissions-List

**Retrieve available lab submissions**
- **Endpoint:** `GET /labs/{lab_id}/sumbissions`
- **Authentication:** Required
- **Description:** Get list of sumbissions for specified lab with pagination

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 1)
- `limit` (integer, optional) - Items per page (default: 20, max: 100)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
	"lab_id": number,
	"lab_title": "string",
	"submission_id": number,
	"submitted_at": "string (ISO 8601)",
	"author_id": number,
	"author_name": "string",
	"author_surname": "string"
}
```

**Error Responses:**
- `404 Not Found` - Lab not found

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


