# API Gateway Documentation

This document represents documentation for [[Frontend]] requests via REST. Each endpoint has its path, request and response body, types of errors.

**Base URL:** `address.com/api/v1`

All endpoints require JWT authentication unless specified otherwise.
**Error response** for no authentication:  `401 Unauthorized`

---


## User Service

| Endpoint                                                 | Type | Description                                                   |
| -------------------------------------------------------- | ---- | ------------------------------------------------------------- |
| [`GET /users/{user_id}`](#get-user-data)                 | GET  | Get user primary data by user ID                              |
| [`GET /users/profile/{user_id}`](#get-user-profile-data) | GET  | Get user profile (personal + labs + articles) data by user ID |

### Get-User-Data

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
- `404 Not Found` - User not found

---

### Get-User-Profile-Data

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
	"labs": [
		"lab_id": number,
		"title": "string",
		"short_desc": "string",
		"submissioins": number,
		"views": number
			],
	"articles": [
		"article_id": number,
		"title": "string",
		"short_desc": "string"
		"views": number
		]
}

```

**Error Responses:**
- `404 Not Found` - User not found

---

## Articles Service

| Endpoint | Type | Description |
|----------|------|-------------|
| [`POST /articles/new`](#create-article) | POST | Creation of new article in PDF format |
| [`GET /articles/get`](#get-articles-list) | GET | Get list of articles |
| [`GET /articles/get/{article_id}`](#get-article-by-id) | GET | Get specified article by ID |

### Create-Article

**Create new article**
- **Endpoint:** `POST /articles/new`
- **Authentication:** Required
- **Content-Type:** `multipart/form-data`
- **Description:** Creation of new article in PDF format

**Request Body (From Data):**
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

---

### Get-Articles-List

**Retrieve articles**
- **Endpoint:** `GET /articles/get`
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
      "id": number,
      "title": "string",
      "short_desc": "string",
      "created_at": "string (ISO 8601)",
      "views": number,
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

---

### Get-Article-by-ID

**Retrieve articles**
- **Endpoint:** `GET /articles/get/{article_id}`
- **Authentication:** Required
- **Description:** Get specified article by ID

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
	"author_id": number,
	"author_name": "string",
	"author_surname": "string"
}
```

**Error Responses:**
- `404 Not Found` - Article not found

---

## Labs Service

| Endpoint                                                      | Type | Description                                               |
| ------------------------------------------------------------- | ---- | --------------------------------------------------------- |
| [`POST /labs`](#create-lab)                                   | POST | Create new lab with markdown and supporting files         |
| [`GET /labs/get`](#get-labs-list)                             | GET  | Get list of labs with pagination                          |
| [`GET /labs/get/{lab_id}`](#get-lab-by-id)                    | GET  | Get lab by ID                                             |
| [`POST /labs/{lab_id}/sumbit`](#submit-lab-solution)          | POST | Submit a solution of the lab as PDF file                  |
| [`GET /labs/{lab_id}/sumbissions`](#get-lab-sumbissions-list) | GET  | Get list of submissions for specified lab with pagination |

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

---

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

---

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

---

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

---
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

# Authentication Headers

All authenticated endpoints require: `Authorization: Bearer <jwt_token>`

# Error Response Format

All error responses follow this format:
```json
{   
	"error": {    
		"code": "string",    
		"message": "string",    
		"details": "object (optional)"
	}
}
```


