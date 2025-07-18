# API Gateway Documentation

This document represents documentation for Frontend requests via REST. Each endpoint has its path, request and response body, types of errors.
Please, consider it as a quick reference but not the exact API documentation. Some endpoints may not be implemented yet or may change in the future. Some models may not be fully described here or contain inconsistencies.

**Base URL:** `address.com/api/v1`

All endpoints require JWT authentication unless specified otherwise.  
**Error response** for no authentication:  `401 Unauthorized`

## Agenda

1. [Health Check](#health-check)
2. [Statistics](#statistics)
3. [User Service](#user-service)
4. [Labs Service](#labs-service)
5. [Tag Service](#tag-service)
6. [Submissions Service](#submissions-service)
7. [Comments Service](#comments-service)
8. [Articles Service](#articles-service)
9. [Feedback Service](#feedback-service)
10. [Common Response Codes](#common-response-codes)
11. [Authentication Headers](#authentication-headers)
12. [Error Response Format](#error-response-format)

---  

## Health Check
Made with Spring Boot Actuator.

You can check the health of the API service by sending a request to the following endpoint:
- **Endpoint:** `GET /health`

Returns a simple JSON response indicating the service is up and running.

Example response:
```json
{
  "status": "UP",
  "groups": [
    "liveness",
    "readiness"
  ]
}
```

For simpler approaches you can use `liveness` endpoint:
- **Endpoint:** `GET actuator/health/liveness`

Example response:
```json
{
  "status": "UP"
}
```

## Statistics

You can check the statistics of the system with four main parameters.
- **Endpoint:** `GET /statistics`
- **Authentication:** Not Required

**Response:**
```json
{
  "users": 1000,
  "labs": 200,
  "articles": 50,
  "submissions": 500
}
```


## User Service

| Endpoint                                                 | Type | Description                                                   |  
|----------------------------------------------------------| ---- | ------------------------------------------------------------- |  
| [`GET /users/{user_id}`](#get-user-data)                | GET  | Get user primary data by user ID                              |  
| [`GET /users/profile/{user_id}`](#get-user-profile-data) | GET  | Get user profile (personal + labs + articles) data by user ID |  

### Get User Data

**Retrieve user primary information**
- **Endpoint:** `GET /users/{user_id}`
- **Authentication:** Required
- **Description:** Get user primary data by user ID

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "id": number,
  "username": "string",
  "name": "string",
  "surname": "string",
  "email": "string",
  "labs_solved": "number",
  "labs_reviewed": "number",
  "balance": "number"
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - User not found

---  

### Get User Profile Data

**Retrieve user profile information**
- **Endpoint:** `GET /users/profile/{user_id}`
- **Authentication:** Required
- **Description:** Get user profile (personal + labs + articles) data by user ID

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "id": number,
  "username": "string",
  "surname": "string",
  "email": "string",
  "profile_photo": "string (path to MinIO database)",
  "labs": [{
    "lab_id": number,
    "title": "string",
    "short_desc": "string",
    "submissions": number,
    "views": number
  }],
  "articles": [{
    "article_id": number,
    "title": "string",
    "short_desc": "string",
    "views": number
  }]
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - User not found

---  

## Articles Service

| Endpoint                                             | Type   | Description                           |
| ---------------------------------------------------- | ------ | ------------------------------------- |
| [`POST /articles`](#create-article)                | POST   | Creation of new article in PDF format |
| [`GET /articles`](#get-articles-list)              | GET    | Get list of articles                  |
| [`GET /articles/my`](#get-my-articles)             | GET    | Get list of articles for current user |
| [`GET /articles/{article_id}`](#get-article-by-id)  | GET    | Get specified article by ID           |
| [`DELETE /articles/{article_id}`](#delete-article) | DELETE | Delete specific article by its ID     |

### Create Article

**Create new article**
- **Endpoint:** `POST /articles`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Creation of new article in PDF format

**Request Body (Form Data):**
- `title`: string (required, 10-255 chars) — Article title
- `short_desc`: string (required, 20-1000 chars) — Article description
- `pdf_file`: file (required) — PDF document file

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "id": 1,
  "message": "Article created successfully",
  "article": {
    "id": 1,
    "title": "Introduction to Machine Learning",
    "shortDesc": "A comprehensive guide to machine learning basics",
    "createdAt": "2024-03-15T14:30:00Z",
    "views": 42,
    "authorId": 123,
    "authorName": "John",
    "authorSurname": "Doe",
    "asset": {
      "assetId": 10,
      "articleId": 1,
      "filename": "ml-guide.pdf",
      "filesize": 1048576,
      "uploadDate": "2024-03-15T14:31:00Z"
    }
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required

---

### Get Articles List

**Retrieve articles**
- **Endpoint:** `GET /articles`
- **Authentication:** Required
- **Query Parameters:**
  - `page` (integer, optional, default: 1) — Page number (min: 1)
  - `limit` (integer, optional, default: 20, max: 100) — Items per page
  - `text` (string, optional) — Search text to filter articles
  - `tags` (string, optional) — Comma-separated list of tag IDs to filter by
- **Description:** Get list of articles

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "articles": [
    {
      "id": 1,
      "title": "Introduction to Machine Learning",
      "shortDesc": "A comprehensive guide to machine learning basics",
      "createdAt": "2024-03-15T14:30:00Z",
      "views": 42,
      "authorId": 123,
      "authorName": "John",
      "authorSurname": "Doe",
      "asset": {
        "assetId": 10,
        "articleId": 1,
        "filename": "ml-guide.pdf",
        "filesize": 1048576,
        "uploadDate": "2024-03-15T14:31:00Z"
      }
    }
  ],
  "count": 1
}
```

**Error Responses:**
- `400 Bad Request` - Invalid pagination parameters
- `401 Unauthorized` - Authentication required

---

### Get My Articles

**Retrieve articles for the current user**
- **Endpoint:** `GET /articles/my`
- **Authentication:** Required
- **Query Parameters:**
  - `page` (integer, optional, default: 1) — Page number (min: 1)
  - `limit` (integer, optional, default: 20, max: 100) — Items per page
- **Description:** Get paginated list of articles created by the authenticated user

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "articles": [
    {
      "id": 1,
      "title": "Introduction to Machine Learning",
      "shortDesc": "A comprehensive guide to machine learning basics",
      "createdAt": "2024-03-15T14:30:00Z",
      "views": 42,
      "authorId": 123,
      "authorName": "John",
      "authorSurname": "Doe",
      "asset": {
        "assetId": 10,
        "articleId": 1,
        "filename": "ml-guide.pdf",
        "filesize": 1048576,
        "uploadDate": "2024-03-15T14:31:00Z"
      }
    }
  ],
  "count": 1
}
```

**Error Responses:**
- `400 Bad Request` - Invalid pagination parameters
- `401 Unauthorized` - Authentication required

---

### Get Article by ID

**Retrieve article**
- **Endpoint:** `GET /articles/{article_id}`
- **Authentication:** Required
- **Description:** Get specified article by ID

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "id": 1,
  "title": "Introduction to Machine Learning",
  "shortDesc": "A comprehensive guide to machine learning basics",
  "createdAt": "2024-03-15T14:30:00Z",
  "views": 42,
  "authorId": 123,
  "authorName": "John",
  "authorSurname": "Doe",
  "asset": {
    "assetId": 10,
    "articleId": 1,
    "filename": "ml-guide.pdf",
    "filesize": 1048576,
    "uploadDate": "2024-03-15T14:31:00Z"
  }
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Article not found

---

### Delete Article

**Delete specific article by its id**
- **Endpoint:** `DELETE /articles/{article_id}`
- **Authentication:** Required
- **Description:** Delete specific article by id

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "message": "Article deleted successfully"
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to delete the article
- `404 Not Found` - Article not found

---

## Labs Service

| Endpoint                                             | Type   | Description                           |
| ---------------------------------------------------- | ------ | ------------------------------------- |
| [`POST /labs`](#create-lab)                        | POST   | Creation of new lab                   |
| [`GET /labs`](#get-labs-list)                      | GET    | Get list of labs                      |
| [`GET /labs/my`](#get-my-labs)                     | GET    | Get labs created by the current user  |
| [`GET /labs/{lab_id}`](#get-lab-by-id)            | GET    | Get specified lab by ID               |
| [`POST /labs/{lab_id}/update`](#update-lab)        | POST   | Update the lab by its ID             |
| [`DELETE /labs/{lab_id}`](#delete-lab)            | DELETE | Delete specific lab by its ID         |

### Create Lab

**Create new lab**
- **Endpoint:** `POST /labs`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Creation of new lab with main md file and supporting assets

**Request Body (Form Data):**
- `title`: string (required, 10-255 chars) — Lab title
- `short_desc`: string (required, 20-1000 chars) — Lab description
- `md_file`: file (required) — Markdown document file
- `assets`: file[] (optional) — Supporting assets (images, etc.)
- `articles`: string (optional, e.g. "[1,2,4]" or "1,2,4") — List of article IDs to associate
- `tags`: string (optional, e.g. "[5,6,7]" or "5,6,7") — Comma-separated list of tag IDs

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "id": 1,
  "message": "Lab created successfully!"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required

---

### Get Labs List

**Retrieve labs**
- **Endpoint:** `GET /labs`
- **Authentication:** Required
- **Query Parameters:**
  - `page` (integer, optional, default: 1) — Page number (min: 1)
  - `limit` (integer, optional, default: 20, max: 100) — Items per page
  - `text` (string, optional) — Search text to filter laboratory works
  - `tags` (string, optional) — Comma-separated list of tag IDs to filter by
- **Description:** Get list of labs

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 1)
- `limit` (integer, optional) - Items per page (default: 20, max: 100)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "labs": [
    {
      "id": 1,
      "title": "Introduction to Data Structures",
      "shortDesc": "Learn about basic data structures and their implementations",
      "createdAt": "2024-03-15T14:30:00Z",
      "views": 42,
      "submissions": 15,
      "authorId": 123,
      "authorName": "John",
      "authorSurname": "Doe",
      "assets": [
        {
          "assetId": 10,
          "labId": 1,
          "filename": "diagram.md",
          "totalSize": 1048576,
          "uploadDate": "2024-03-15T14:31:00Z"
        }
      ],
      "articles": [1, 2, 3],
      "tags": [5, 6, 7]
    }
  ],
  "tags": [
    {
      "id": 5,
      "name": "Java",
      "description": "Java programming language",
      "labs_count": 5
    }
  ],
  "count": 1
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required

---

### Get My Labs

**Retrieve labs created by the current user**
- **Endpoint:** `GET /labs/my`
- **Authentication:** Required
- **Query Parameters:**
  - `page` (integer, optional) - Page number (default: 1)
  - `limit` (integer, optional) - Items per page (default: 20, max: 100)
- **Description:** Get paginated list of labs created by the authenticated user

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "labs": [
    {
      "id": 1,
      "title": "Introduction to Data Structures",
      "shortDesc": "Learn about basic data structures and their implementations",
      "createdAt": "2024-03-15T14:30:00Z",
      "views": 42,
      "submissions": 15,
      "authorId": 123,
      "authorName": "John",
      "authorSurname": "Doe",
      "assets": [
        {
          "assetId": 10,
          "labId": 1,
          "filename": "diagram.md",
          "totalSize": 1048576,
          "uploadDate": "2024-03-15T14:31:00Z"
        }
      ],
      "articles": [1, 2, 3],
      "tags": [5, 6, 7]
    }
  ],
  "tags": [
    {
      "id": 5,
      "name": "Java",
      "description": "Java programming language",
      "labs_count": 5
    },
    ...
  ],
  "count": 1
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required

---

### Get Lab by ID

**Retrieve lab**
- **Endpoint:** `GET /labs/{lab_id}`
- **Authentication:** Required
- **Description:** Get specified lab by ID

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "id": 1,
  "title": "Introduction to Data Structures",
  "shortDesc": "Learn about basic data structures and their implementations",
  "createdAt": "2024-03-15T14:30:00Z",
  "views": 42,
  "submissions": 15,
  "authorId": 123,
  "authorName": "John",
  "authorSurname": "Doe",
  "assets": [
    {
      "assetId": 10,
      "labId": 1,
      "filename": "diagram.md",
      "totalSize": 1048576,
      "uploadDate": "2024-03-15T14:31:00Z"
    }
  ],
  "articles": [1, 2, 3],
  "tags": [
    {
      "id": 5,
      "name": "Java",
      "description": "Java programming language",
      "labs_count": 5
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Lab not found

---

### Update Lab

**Update the lab by its ID**
- **Endpoint:** `POST /labs/{lab_id}/update`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Update lab with main md file and supporting assets

**Request Body (Form Data):**
- `title`: string (required, 10-255 chars) — Lab title
- `short_desc`: string (required, 20-1000 chars) — Lab description
- `md_file`: file (required) — Markdown document file
- `assets`: file[] (optional) — Supporting assets (images, etc.)
- `articles`: string (optional, e.g. "[1,2,4]" or "1,2,4") — List of article IDs to associate
- `tags`: string (optional, e.g. "[5,6,7]" or "5,6,7") — Comma-separated list of tag IDs

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "id": 1,
  "message": "Lab updated successfully"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to update the lab
- `404 Not Found` - Lab not found

---

### Delete Lab

**Delete specific lab by its id**
- **Endpoint:** `DELETE /labs/{lab_id}`
- **Authentication:** Required
- **Description:** Delete specific lab by id

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "message": "Lab deleted successfully!"
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to delete the lab
- `404 Not Found` - Lab not found

---

## Tag Service

| Endpoint                                 | Type   | Description                                 |
| ---------------------------------------- | ------ | ------------------------------------------- |
| [`POST /tags`](#create-tag)              | POST   | Create a new tag                            |
| [`GET /tags/{tagId}`](#get-tag-by-id)    | GET    | Get tag by ID                               |
| [`POST /tags/by-ids`](#get-tags-by-ids)  | POST   | Get tags by a list of IDs                   |
| [`GET /tags`](#get-tags-list)            | GET    | Get paginated list of tags                  |
| [`PUT /tags/update`](#update-tag)        | PUT    | Update a tag                                |
| [`DELETE /tags/{tagId}`](#delete-tag)    | DELETE | Delete a tag by ID                          |

### Create Tag

**Create a new tag**
- **Endpoint:** `POST /tags`
- **Authentication:** Required
- **Content-Type:** `application/json`
- **Description:** Create a new tag

**Request Body:**
```json
{
  "name": "string (required)",
  "description": "string (optional)"
}
```

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "id": 1,
  "name": "Java",
  "description": "Java programming language",
  "labs_count": 5
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Authentication required

---

### Get Tag by ID

**Retrieve a tag by its ID**
- **Endpoint:** `GET /tags/{tagId}`
- **Authentication:** Not Required
- **Description:** Get tag by ID

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "id": 1,
  "name": "Java",
  "description": "Java programming language",
  "labs_count": 5
}
```

**Error Responses:**
- `404 Not Found` - Tag not found

---

### Get Tags by IDs

**Retrieve tags by a list of IDs**
- **Endpoint:** `POST /tags/by-ids`
- **Authentication:** Not Required
- **Content-Type:** `application/json`
- **Description:** Get tags by a list of IDs

**Request Body:**
```json
{
  "ids": [1, 2, 3]
}
```

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "tags": [
    {
      "id": 1,
      "name": "Java",
      "description": "Java programming language",
      "labs_count": 5
    },
    {
      "id": 2,
      "name": "Python",
      "description": "Python programming language",
      "labs_count": 8
    }
  ],
  "count": 2
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input

---

### Get Tags List

**Retrieve paginated list of tags**
- **Endpoint:** `GET /tags`
- **Authentication:** Not Required
- **Query Parameters:**
  - `page` (integer, optional, default: 0) — Page number
  - `limit` (integer, optional, default: 50) — Items per page
- **Description:** Get paginated list of tags

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "tags": [
    {
      "id": 1,
      "name": "Java",
      "description": "Java programming language",
      "labs_count": 5
    },
    {
      "id": 2,
      "name": "Python",
      "description": "Python programming language",
      "labs_count": 8
    }
  ],
  "count": 2
}
```

**Error Responses:**
- `400 Bad Request` - Invalid pagination parameters

---

### Update Tag

**Update an existing tag**
- **Endpoint:** `PUT /tags/update`
- **Authentication:** Required
- **Content-Type:** `application/json`
- **Description:** Update a tag's name and/or description

**Request Body:**
```json
{
  "id": 1,
  "name": "Java Advanced (optional)",
  "description": "Advanced Java programming language (optional)"
}
```


**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "id": 1,
  "name": "Java Advanced",
  "description": "Advanced Java programming language",
  "labs_count": 5
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input
- `404 Not Found` - Tag not found

---

### Delete Tag

**Delete a tag by its ID**
- **Endpoint:** `DELETE /tags/{tagId}`
- **Authentication:** Required
- **Description:** Delete a tag by its ID

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
true
```

**Error Responses:**
- `404 Not Found` - Tag not found

---

## Submissions Service

| Endpoint                                                      | Type   | Description                                               |
| ------------------------------------------------------------- | ------ | --------------------------------------------------------- |
| [`POST /submissions`](#create-submission)                     | POST   | Create new submission for a lab                          |
| [`GET /submissions/{submission_id}`](#get-submission-by-id)   | GET    | Get submission by ID                                      |
| [`GET /submissions/lab/{lab_id}`](#get-lab-submissions)       | GET    | Get all submissions for a specific lab                    |
| [`GET /submissions/my`](#get-my-submissions)                  | GET    | Get all submissions by the authenticated user              |
| [`GET /submissions/review`](#get-submissions-for-review)      | GET    | Get submissions that require review by the authenticated user |
| [`DELETE /submissions/{submission_id}`](#delete-submission)    | DELETE | Delete specific submission by its ID                      |

### Create Submission

**Create new submission**
- **Endpoint:** `POST /submissions`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Create new submission for a lab with supporting assets

**Request Body (Form Data):**
```json  
lab_id: number (required) - ID of the lab  
assets: file[] (required) - Submission files  
```  

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "success": "boolean",
  "message": "string",
  "submissionMetadata": {
    "submissionId": "number",
    "labId": "number",
    "owner": {
      "id": "number",
      "username": "string",
      "name": "string",
      "surname": "string",
      "email": "string",
      "labs_solved": "number",
      "labs_reviewed": "number",
      "balance": "number"
    },
    "text": "string",
    "createdAt": "string (ISO 8601)",
    "updatedAt": "string (ISO 8601)",
    "status": "string",
    "assets": [
      {
        "assetId": "number",
        "submissionId": "number",
        "filename": "string",
        "totalSize": "number",
        "uploadDate": "string (ISO 8601)"
      }
    ]
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Lab not found
  
---  

### Get Submission by ID

**Retrieve submission**
- **Endpoint:** `GET /submissions/{submission_id}`
- **Authentication:** Required
- **Description:** Get detailed information about a specific submission

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "submissionId": "number",
  "labId": "number",
  "owner": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "text": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "status": "string",
  "assets": [
    {
      "assetId": "number",
      "submissionId": "number",
      "filename": "string",
      "totalSize": "number",
      "uploadDate": "string (ISO 8601)"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to view this submission
- `404 Not Found` - Submission not found

---

### Get Lab Submissions

**Retrieve lab submissions**
- **Endpoint:** `GET /submissions/lab/{lab_id}`
- **Authentication:** Required
- **Description:** Get all submissions for a specific lab

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 1)
- `limit` (integer, optional) - Items per page (default: 20, max: 100)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "submissions": [
    {
      "submissionId": "number",
      "labId": "number",
      "owner": {
        "id": "number",
        "username": "string",
        "name": "string",
        "surname": "string",
        "email": "string",
        "labs_solved": "number",
        "labs_reviewed": "number",
        "balance": "number"
      },
      "text": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)",
      "status": "string",
      "assets": [
        {
          "assetId": "number",
          "submissionId": "number",
          "filename": "string",
          "totalSize": "number",
          "uploadDate": "string (ISO 8601)"
        }
      ]
    }
  ],
  "totalCount": "number"
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to view lab submissions
- `404 Not Found` - Lab not found

---

### Get My Submissions

**Get all submissions by the authenticated user**
- **Endpoint:** `GET /submissions/my`
- **Authentication:** Required
- **Description:** Returns a paginated list of all submissions made by the authenticated user, including their attachments.

**Query Parameters:**
- `page`: number (optional, default: 1) — Page number (starts from 1)
- `limit`: number (optional, default: 20) — Page size

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "submissions": [
    {
      "submissionId": "number",
      "labId": "number",
      "owner": "null",
      "text": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)",
      "status": "string",
      "assets": [
        {
          "assetId": "number",
          "submissionId": "number",
          "filename": "string",
          "totalSize": "number",
          "uploadDate": "string (ISO 8601)"
        }
      ]
    }
    // ...more submissions
  ],
  "totalCount": "number"
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required

---

### Get Submissions for Review

**Retrieve submissions that require review by the authenticated user**
- **Endpoint:** `GET /submissions/review`
- **Authentication:** Required
- **Query Parameters:**
  - `page` (integer, optional, default: 1) — Page number (starts from 1)
  - `limit` (integer, optional, default: 20) — Page size
- **Description:** Returns a paginated list of submissions that require review by the authenticated user, including their attachments.

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "submissions": [
    {
      "submissionId": 1,
      "labId": 1,
      "owner": {
        "id": 123,
        "username": "student1",
        "name": "John",
        "surname": "Doe",
        "email": "john.doe@example.com",
        "labs_solved": 5,
        "labs_reviewed": 2,
        "balance": 100
      },
      "text": "Here is my solution for the lab work",
      "createdAt": "2024-03-15T14:30:00Z",
      "updatedAt": "2024-03-15T14:30:00Z",
      "status": "NOT_GRADED",
      "assets": [
        {
          "assetId": 10,
          "submissionId": 1,
          "filename": "solution.java",
          "totalSize": 2048,
          "uploadDate": "2024-03-15T14:31:00Z"
        }
      ]
    }
  ],
  "totalCount": 1
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required

---

### Delete Submission

**Delete specific submission by its id**
- **Endpoint:** `DELETE /submissions/{submission_id}`
- **Authentication:** Required
- **Description:** Delete a specific submission by its ID (only available to submission owner)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "message": "Submission deleted successfully"
}  
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to delete this submission
- `404 Not Found` - Submission not found

---

## Comments Service

| Endpoint                                                    | Type   | Description                                 |
|-------------------------------------------------------------| ------ |---------------------------------------------|
| [`POST /labs/{labId}/comments`](#create-comment)            | POST   | Create a new comment or reply on a lab      |
| [`GET /labs/{labId}/comments`](#get-lab-comments)           | GET    | Get list of comments for a specific lab     |
| [`POST /articles/{articleId}/comments`](#create-comment)    | POST   | Create a new comment or reply on an article |
| [`GET /articles/{articleId}/comments`](#get-lab-comments)           | GET    | Get list of comments for a specific article |
| [`GET /comments/{commentId}`](#get-comment-by-id)           | GET    | Get a single comment by ID                  |
| [`GET /comments/{commentId}/replies`](#get-comment-replies) | GET    | Get list of replies for a specific comment  |
| [`PUT /comments/{commentId}`](#update-comment)              | PUT    | Update an existing comment                  |
| [`DELETE /comments/{commentId}`](#delete-comment)           | DELETE | Delete a specific comment                   |

### Create Comment

**Create a new comment or reply on a lab**
- **Endpoint:** `POST /labs/{labId}/comments` (or `POST /articles/{articleId}/comments`)
- **Authentication:** Required
- **Content-Type:** `application/json`
- **Description:** Creates a new top-level comment or a reply to an existing comment

**Path Parameters:**
- `labId` (number, required) - ID of the lab to comment on

**Request Body:**
```json  
{
  "content": "string (required) - The content of the comment",
  "parentId": "string (optional) - The ID of the parent comment if this is a reply"
}
```  

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "id": "string (UUID)",
  "contentId": number,
  "userId": number,
  "firstName": "string",
  "lastName": "string",
  "parentId": "string (UUID, optional)",
  "content": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input data
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Lab not found

---

### Get Lab Comments

**Get list of comments for a lab**
- **Endpoint:** `GET /labs/{labId}/comments` (or `GET /articles/{articleId}/comments`)
- **Authentication:** Not Required
- **Description:** Retrieves a paginated list of top-level comments for a specific lab

**Path Parameters:**
- `labId` (number, required) - ID of the lab

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 1, min: 1)
- `limit` (integer, optional) - Items per page (default: 20, min: 1, max: 100)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "comments": [
    {
      "id": "string (UUID)",
      "contentId": number,
      "userId": number,
      "firstName": "string",
      "lastName": "string",
      "parentId": "string (UUID, optional)",
      "content": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)"
    }
  ],
  "count": number
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Lab not found

---  

### Get Comment by ID

**Get a single comment**
- **Endpoint:** `GET /comments/{commentId}`
- **Authentication:** Not Required
- **Description:** Retrieves a single comment by its ID

**Path Parameters:**
- `commentId` (string, required) - ID of the comment in UUID format

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "id": "string (UUID)",
  "contentId": number,
  "userId": number,
  "firstName": "string",
  "lastName": "string",
  "parentId": "string (UUID, optional)",
  "content": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Comment not found

---  

### Get Comment Replies

**Get replies to a comment**
- **Endpoint:** `GET /comments/{commentId}/replies`
- **Authentication:** Not Required
- **Description:** Retrieves a paginated list of replies for a specific parent comment

**Path Parameters:**
- `commentId` (string, required) - ID of the parent comment in UUID format

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 0)
- `size` (integer, optional) - Items per page (default: 20)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "comments": [
    {
      "id": "string (UUID)",
      "contentId": number,
      "userId": number,
      "firstName": "string",
      "lastName": "string",
      "parentId": "string (UUID)",
      "content": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)"
    }
  ],
  "count": number
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Comment not found

---

### Update Comment

**Update an existing comment**
- **Endpoint:** `PUT /comments/{commentId}`
- **Authentication:** Required
- **Content-Type:** `application/json`
- **Description:** Updates the content of an existing comment (only available to comment owner)

**Path Parameters:**
- `commentId` (string, required) - ID of the comment to update in UUID format

**Request Body:**
```json  
{
  "content": "string (required) - The new content of the comment"
}
```  

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "id": "string (UUID)",
  "contentId": number,
  "userId": number,
  "firstName": "string",
  "lastName": "string",
  "parentId": "string (UUID, optional)",
  "content": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```  

**Error Responses:**
- `400 Bad Request` - Invalid input data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - User does not own this comment
- `404 Not Found` - Comment not found

---

### Delete Comment

**Delete a comment**
- **Endpoint:** `DELETE /comments/{commentId}`
- **Authentication:** Required
- **Description:** Deletes a specific comment (only available to comment owner)

**Path Parameters:**
- `commentId` (string, required) - ID of the comment to delete in UUID format

**Response:**
- **Status:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - User does not own this comment
- `404 Not Found` - Comment not found
---

## Feedback Service

| Endpoint                                                        | Type   | Description                                                      |
| --------------------------------------------------------------- | ------ | --------------------------------------------------------------- |
| [`POST /feedback`](#create-feedback)                            | POST   | Create new feedback for a submission (with optional attachments) |
| [`DELETE /feedback/{feedbackId}`](#delete-feedback)              | DELETE | Delete feedback by ID (reviewer only)                           |
| [`GET /feedback/my/{submissionId}`](#get-my-feedback)            | GET    | Get feedback for a specific submission (for current student)     |
| [`GET /feedback/my`](#list-my-feedbacks)                         | GET    | List all feedbacks for the authenticated student                 |
| [`GET /feedback/{feedbackId}`](#get-feedback-by-id)              | GET    | Get feedback by ID                                               |
| [`GET /feedback/student/{studentId}`](#list-student-feedbacks)   | GET    | List all feedbacks for a specific student                        |
| [`GET /feedback/reviewer/{reviewerId}`](#list-reviewer-feedbacks)| GET    | List all feedbacks created by a reviewer                         |

### Create Feedback

**Create new feedback for a submission**
- **Endpoint:** `POST /feedback`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Creates a new feedback for a submission with optional file attachments

**Request Body (Form Data):**
- `submissionId`: number (required) — ID of the submission
- `studentId`: number (required) — ID of the student
- `content`: string (required) — Feedback text
- `files`: file[] (optional) — Attachments (multiple files allowed)

**Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
  "id": "string (UUID)",
  "submissionId": "number",
  "content": "string",
  "student": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "reviewer": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "attachments": [
    {
      "feedback_id": "string (UUID)",
      "filename": "string",
      "content_type": "string",
      "total_size": "number"
    }
  ]
}
```

**Error Responses:**
- `400 Bad Request` — Invalid request data
- `401 Unauthorized` — Authentication required
- `403 Forbidden` — Access denied

---

### Delete Feedback

**Delete feedback by ID**
- **Endpoint:** `DELETE /feedback/{feedbackId}`
- **Authentication:** Required
- **Description:** Deletes a feedback by ID (only reviewer who created it can delete)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "success": true,
  "message": "Feedback deleted successfully."
}
```

**Error Responses:**
- `401 Unauthorized` — Authentication required
- `403 Forbidden` — Only the reviewer can delete
- `404 Not Found` — Feedback not found

---

### Get My Feedback

**Retrieve feedback for a specific submission (for current student)**
- **Endpoint:** `GET /feedback/my/{submissionId}`
- **Authentication:** Required
- **Description:** Retrieves feedback for a specific submission for the authenticated student.

**Path Parameters:**
- `submissionId`: number (required) — ID of the submission

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "id": "string (UUID)",
  "submissionId": "number",
  "content": "string",
  "student": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "reviewer": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "attachments": [
    {
      "feedback_id": "string (UUID)",
      "filename": "string",
      "content_type": "string",
      "total_size": "number"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` — Authentication required
- `404 Not Found` — Feedback not found

---

### List My Feedbacks

**List all feedbacks for the authenticated student**
- **Endpoint:** `GET /feedback/my`
- **Authentication:** Required
- **Description:** Lists all feedbacks for the authenticated student with pagination.

**Query Parameters:**
- `page`: integer (default: 1)
- `limit`: integer (default: 20)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "feedbacks": [ ... ],
  "totalCount": number
}
```

**Error Responses:**
- `401 Unauthorized` — Authentication required

---

### Get Feedback by ID

**Retrieve feedback by ID**
- **Endpoint:** `GET /feedback/{feedbackId}`
- **Authentication:** Required
- **Description:** Retrieves a specific feedback by ID

**Path Parameters:**
- `feedbackId`: string (UUID, required) — ID of the feedback

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "id": "string (UUID)",
  "submissionId": "number",
  "content": "string",
  "student": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "reviewer": {
    "id": "number",
    "username": "string",
    "name": "string",
    "surname": "string",
    "email": "string",
    "labs_solved": "number",
    "labs_reviewed": "number",
    "balance": "number"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "attachments": [
    {
      "feedback_id": "string (UUID)",
      "filename": "string",
      "content_type": "string",
      "total_size": "number"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` — Authentication required
- `404 Not Found` — Feedback not found

---

### List Student Feedbacks

**List all feedbacks for a specific student**
- **Endpoint:** `GET /feedback/student/{studentId}`
- **Authentication:** Required
- **Description:** Lists all feedbacks for a specific student.

**Path Parameters:**
- `studentId`: number (required) — ID of the student

**Query Parameters:**
- `page`: integer (default: 1)
- `limit`: integer (default: 20)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "feedbacks": [ ... ],
  "totalCount": number
}
```

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Access denied

---

### List Reviewer Feedbacks

**List all feedbacks created by a reviewer**
- **Endpoint:** `GET /feedback/reviewer/{reviewerId}`
- **Authentication:** Required
- **Description:** Lists all feedbacks created by the specified reviewer, optionally filtered by submission

**Query Parameters:**
- `page`: integer (default: 1)
- `limit`: integer (default: 20)
- `submissionId`: number (optional)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
  "feedbacks": [ ... ],
  "totalCount": number
}
```

**Error Responses:**
- `401 Unauthorized` — Authentication required
- `403 Forbidden` — Access denied

---


## Common Response Codes

| Code  | Description                             |  
| ----- | --------------------------------------- |  
| `200` | OK - Request successful                 |  
| `201` | Created - Resource created successfully |  
| `400` | Bad Request - Invalid request data      |  
| `401` | Unauthorized - Authentication required  |  
| `403` | Forbidden - Access denied               |  
| `404` | Not Found - Resource not found          |  
| `409` | Conflict - Resource already exists      |  
| `500` | Internal Server Error - Server error    |  

## Authentication Headers

All authenticated endpoints require: `Authorization: Bearer <jwt_token>`

## Error Response Format

All error responses follow this format:
```json  

{     
    "error": {     
       "message": "string",      
       "details": "string (optional)"  
    }
}
```
