# Agenda

- [Purpose](#Purpose)
- [Functionality](#Functionality)
- [Entities](#Entities)
- [API](#API)
- [Integrations](#Integrations)
- [User Stories](#User%20Stories)
- [Technical Details](#Technical%20Details)

# Purpose

Article Service is the central repository of all scientific articles on the Open Labs Share platform. It provides a single point of management for scientific content and access to file resources. The service provides opportunities for:
- **Authors** to publish their scientific articles
- **Teachers** link their lab work with articles
- **Students** read additional educational literature

# Functionality

1. General
- CRUD for articles
- Data Storage
- Categorization and tagging of articles
- Control of access to articles

2. For authors:
- Publication of articles in PDF format
- Manage your publications (update versions)

3. For users:
- Search and filter articles by topic, author, and publication date

# Entities

1. **Article:**

| Field                             | Type           |
| --------------------------------- | -------------- |
| id (PK)                           | UUID / long    |
| title                             | string         |
| owner_id                          | UUID / long    |
| publication_date                  | datestamp      |
| short_desc                        | string         |
| views                             | integer        |
| status (draft/published/archived) | string         |
| stars                             | integer / long |
| people_rated                      | integer / long |
2. Article tags:

| Field           | Type        |
| --------------- | ----------- |
| article_id (PK) | UUID / long |
| tag (PK)        | string      |


# API

Done, according with [Articles Service API Docs](https://github.com/LuminiteTime/Open-Labs-Share-Docs/blob/main/Backend/API%20Endpoints.md#articles-service)

| Endpoint                                               | Type | Description                           |
| ------------------------------------------------------ | ---- | ------------------------------------- |
| [`POST /articles`](#create-article)                    | POST | Creation of new article in PDF format |
| [`GET /articles/get`](#get-articles-list)              | GET  | Get list of articles                  |
| [`GET /articles/get/{article_id}`](#get-article-by-id) | GET  | Get specified article by ID           |

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

# Integrations

1. **User Service:**
- Obtaining data about authors and reviewers
- Checking access rights

2. **API Gateway:**
- A single entry point for all requests

3. **S3 Storage:**
- Storing PDF and other article files
# User Stories

1. **The teacher publishes a tutorial:**
- Uploads a PDF file via POST /articles
- Specifies the tags "teaching materials", "physics"
- Students find the material through a search

2. **The researcher is looking for materials:**
- Uses filters by topic and date
- Finds several relevant articles
- Saves them to bookmarks for further study

# Technical Details
- Technological stack:`
	- **Backend:** Python 3.12
	- **Internal Communications:** gRPC
- **Database:** PostgreSQL
- **Deployment:** Docker
- **File Storage:** S3
```
Bucket:
articles
└── article_id
    └── article.pdf
```