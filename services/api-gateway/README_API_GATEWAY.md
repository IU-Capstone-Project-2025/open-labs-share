# API Gateway Documentation

This document represents documentation for Frontend requests via REST. Each endpoint has its path, request and response body, types of errors.

**Base URL:** `address.com/api/v1`

All endpoints require JWT authentication unless specified otherwise.  
**Error response** for no authentication:  `401 Unauthorized`

## Agenda

1. [User Service](#user-service)
2. [Labs Service](#labs-service)
3. [Submissions Service](#submissions-service)
4. [Comments Service](#comments-service)
5. [Articles Service](#articles-service)
6. [Common Response Codes](#common-response-codes)
7. [Authentication Headers](#authentication-headers)
8. [Error Response Format](#error-response-format)

---  

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
  "surname": "string",
  "email": "string"
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
| [`GET /articles/{article_id}`](#get-article-by-id)  | GET    | Get specified article by ID           |
| [`POST /articles/{article_id}/update`](#update-article) | POST   | Update the article by its ID          |
| [`DELETE /articles/{article_id}`](#delete-article) | DELETE | Delete specific article by its ID     |

### Create Article

**Create new article**
- **Endpoint:** `POST /articles`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Creation of new article in PDF format

**Request Body (Form Data):**
```json  
title: string (required) - Article title  
short_desc: string (required) - Article description  
pdf_file: file (required) - PDF document file  
```  

**Response:**
- **Status:** `201 Created`
- **Body:**
```json  
{  
  "id": number,
  "message": "Article created successfully"  
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
- **Description:** Get list of articles

**Query Parameters:**
- `page` (integer, optional) - Page number (default: 1)
- `limit` (integer, optional) - Items per page (default: 20, max: 100)

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{
  "articles": [
    {
      "id": "number",
      "title": "string",
      "short_desc": "string",
      "created_at": "string (ISO 8601)",
      "views": "number",
      "author_id": "number",
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
  "id": "number",
  "title": "string",
  "short_desc": "string",
  "created_at": "string (ISO 8601)",
  "views": "number",
  "author_id": "number",
  "author_name": "string",
  "author_surname": "string"
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Article not found

---  

### Update Article

**Update the article by its ID**
- **Endpoint:** `POST /articles/{article_id}/update`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Update article with new content

**Request Body (Form Data):**
```json  
title: string (required) - Article title  
short_desc: string (required) - Article description  
pdf_file: file (required) - PDF document file  
```  

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{  
  "id": number,
  "message": "Article updated successfully"
}
```  

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to update the article
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
```json  
title: string (required) - Lab title  
short_desc: string (required) - Lab description  
md_file: file (required) - Markdown document file  
assets: file[] (optional) - Supporting assets (images, etc.)  
```  

**Response:**
- **Status:** `201 Created`
- **Body:**
```json  
{  
  "id": number,
  "message": "Lab created successfully"  
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
      "id": "number",
      "title": "string",
      "short_desc": "string",
      "created_at": "string (ISO 8601)",
      "views": "number",
      "submissions": "number",
      "author_id": "number",
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
  "id": "number",
  "title": "string",
  "short_desc": "string",
  "created_at": "string (ISO 8601)",
  "views": "number",
  "submissions": "number",
  "author_id": "number",
  "author_name": "string",
  "author_surname": "string",
  "content": "string (markdown content)",
  "assets": [
    {
      "asset_id": "string",
      "filename": "string",
      "url": "string"
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
```json  
title: string (required) - Lab title  
short_desc: string (required) - Lab description  
md_file: file (required) - Markdown document file  
assets: file[] (optional) - Supporting assets (images, etc.)  
```  

**Response:**
- **Status:** `200 OK`
- **Body:**
```json  
{  
  "id": number,
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
  "message": "Lab deleted successfully"
}  
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to delete the lab
- `404 Not Found` - Lab not found

---

## Submissions Service

| Endpoint                                                      | Type   | Description                                               |
| ------------------------------------------------------------- | ------ | --------------------------------------------------------- |
| [`POST /submissions`](#create-submission)                     | POST   | Create new submission for a lab                          |
| [`GET /submissions/{submission_id}`](#get-submission-by-id)   | GET    | Get submission by ID                                      |
| [`GET /submissions/lab/{lab_id}`](#get-lab-submissions)       | GET    | Get all submissions for a specific lab                    |
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
  "submissionId": number,
  "labId": number,
  "ownerId": number,
  "username": "string",
  "ownerName": "string",
  "ownerSurname": "string",
  "text": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "status": "string",
  "assets": [
    {
      "assetId": number,
      "submissionId": number,
      "filename": "string",
      "totalSize": number,
      "uploadDate": "string (ISO 8601)"
    }
  ]
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
  "submissionId": number,
  "labId": number,
  "ownerId": number,
  "username": "string",
  "ownerName": "string",
  "ownerSurname": "string",
  "text": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)",
  "status": "string",
  "assets": [
    {
      "assetId": number,
      "submissionId": number,
      "filename": "string",
      "totalSize": number,
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
      "submissionId": number,
      "labId": number,
      "ownerId": number,
      "username": "string",
      "ownerName": "string",
      "ownerSurname": "string",
      "text": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)",
      "status": "string",
      "assets": [
        {
          "assetId": number,
          "submissionId": number,
          "filename": "string",
          "totalSize": number,
          "uploadDate": "string (ISO 8601)"
        }
      ]
    }
  ],
  "totalCount": number
}
```  

**Error Responses:**
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - You have no access to view lab submissions
- `404 Not Found` - Lab not found

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

| Endpoint                                                      | Type   | Description                                               |
| ------------------------------------------------------------- | ------ | --------------------------------------------------------- |
| [`POST /labs/{labId}/comments`](#create-comment)               | POST   | Create a new comment or reply on a lab                    |
| [`GET /labs/{labId}/comments`](#get-lab-comments)             | GET    | Get list of comments for a specific lab                   |
| [`GET /comments/{commentId}`](#get-comment-by-id)             | GET    | Get a single comment by ID                                |
| [`GET /comments/{commentId}/replies`](#get-comment-replies)    | GET    | Get list of replies for a specific comment                |
| [`PUT /comments/{commentId}`](#update-comment)                 | PUT    | Update an existing comment                                |
| [`DELETE /comments/{commentId}`](#delete-comment)              | DELETE | Delete a specific comment                                 |

### Create Comment

**Create a new comment or reply on a lab**
- **Endpoint:** `POST /labs/{labId}/comments`
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
  "labId": number,
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
- **Endpoint:** `GET /labs/{labId}/comments`
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
      "labId": number,
      "userId": number,
      "firstName": "string",
      "lastName": "string",
      "parentId": "string (UUID, optional)",
      "content": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)"
    }
  ],
  "pagination": {
    "currentPage": number,
    "totalPages": number,
    "totalItems": number
  }
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
  "labId": number,
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
      "labId": number,
      "userId": number,
      "firstName": "string",
      "lastName": "string",
      "parentId": "string (UUID)",
      "content": "string",
      "createdAt": "string (ISO 8601)",
      "updatedAt": "string (ISO 8601)"
    }
  ],
  "pagination": {
    "currentPage": number,
    "totalPages": number,
    "totalItems": number
  }
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
  "labId": number,
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
    }}  
```
