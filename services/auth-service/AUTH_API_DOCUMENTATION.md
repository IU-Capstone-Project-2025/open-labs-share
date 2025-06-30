# Authentication Service API Documentation

## Base URL `/api/v1/auth`

## Authentication Endpoints

### 1. User Registration

**Endpoint:** `POST /register`

**Description:** Creates a new user account and returns JWT tokens.

**Request Body:**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "email": "johndoe@example.com",
  "password": "securePassword123"
}
```

**Response (201 Created):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2025-06-17T10:30:00",
  "userInfo": {
    "userId": 123,
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "ROLE_USER",
    "labsSolved": 0,
    "labsReviewed": 0,
    "balance": 10
  }
}
```

**Error Responses:**

- `400 Bad Request` - Invalid request data or validation errors
- `409 Conflict` - Username or email already exists (checked via users-service)

---

### 2. User Login

**Endpoint:** `POST /login`

**Description:** Authenticates user credentials via users-service and returns JWT tokens. No local authentication is performed - all credential validation is delegated to users-service.

**Request Body:**

```json
{
  "usernameOrEmail": "johndoe",
  "password": "securePassword123"
}
```

**Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2025-06-17T10:30:00",
  "userInfo": {
    "userId": 123,
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "ROLE_USER"
  }
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid credentials (authentication failed in users-service)
- `404 Not Found` - User not found in users-service

---

### 3. Token Refresh

**Endpoint:** `POST /refresh`

**Description:** Generates new access token using refresh token.

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2025-06-17T10:30:00",
  "userInfo": {
    "userId": 123,
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "ROLE_USER",
    "labsSolved": 0,
    "labsReviewed": 0,
    "balance": 10
  }
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid, expired, or blacklisted refresh token
- `404 Not Found` - User no longer exists in users-service

---

### 4. User Logout

**Endpoint:** `POST /logout`

**Description:** Invalidates the current JWT token by adding it to an in-memory blacklist. The token remains cryptographically valid but is rejected by the auth-service.

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Successfully logged out",
  "data": null
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid or missing token

---

### 5. Password Reset Request ⚠️ NOT YET IMPLEMENTED

**Endpoint:** `POST /password-reset`

**Description:** Will delegate password reset functionality to users-service when implemented.

---

### 6. Password Reset Confirmation ⚠️ NOT YET IMPLEMENTED

**Endpoint:** `POST /password-reset/confirm`

**Description:** Will delegate password reset confirmation to users-service when implemented.

---

### 7. Get User Profile

**Endpoint:** `GET /profile`

**Description:** Retrieves authenticated user's profile information from users-service via gRPC.

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**

```json
{
  "userInfo": {
    "userId": 123,
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "johndoe@example.com",
    "role": "ROLE_USER",
    "labsSolved": 3,
    "labsReviewed": 2,
    "balance": 45
  },
  "status": "ACTIVE"
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid or missing token
- `404 Not Found` - User not found in users-service

---

### 8. Update User Profile

**Endpoint:** `PUT /profile`

**Description:** Updates authenticated user's profile information including password changes. Returns new JWT tokens if username was changed, same tokens otherwise. All fields are optional - only provided fields will be updated.

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body:**

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "username": "janesmith",
  "email": "jane.smith@example.com",
  "password": "newSecurePassword123"
}
```

**Response (200 OK) - Username Changed:**

```json
{
  "userInfo": {
    "userId": 123,
    "username": "janesmith",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com",
    "role": "ROLE_USER"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2025-06-17T10:30:00",
  "usernameChanged": true,
  "message": "Profile updated successfully"
}
```

**Response (200 OK) - Username NOT Changed:**

```json
{
  "userInfo": {
    "userId": 123,
    "username": "johndoe",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com",
    "role": "ROLE_USER",
    "labsSolved": 3,
    "labsReviewed": 2,
    "balance": 45
  },
  "accessToken": null,
  "refreshToken": null,
  "tokenType": null,
  "expiresAt": null,
  "usernameChanged": false,
  "message": "Profile updated successfully"
}
```

**Error Responses:**

- `400 Bad Request` - Invalid request data or validation errors
- `401 Unauthorized` - Invalid or missing token
- `404 Not Found` - User not found in users-service
- `409 Conflict` - Username or email already exists

**Field Constraints:**

- `firstName`: Maximum 50 characters
- `lastName`: Maximum 50 characters
- `username`: 3-20 characters, alphanumeric and underscores only
- `email`: Valid email format, maximum 100 characters
- `password`: 8-100 characters (optional, leave blank to keep current password)

---

### 9. Email Verification ⚠️ NOT YET IMPLEMENTED

**Endpoint:** `GET /verify-email/{token}`

**Description:** Will delegate email verification to users-service when implemented.

---

## gRPC methods

- **Token Validation**: `ValidateToken` gRPC method (used by API Gateway)
- **Health Check**: `HealthCheck` gRPC method (used for service monitoring)

These gRPC services are documented in the main service documentation and are accessible via gRPC port `9092`.

---

## Error Response Format

All error responses follow the format:

```json
{
  "timestamp": "2025-06-17T10:30:00Z",
  "status": 400,
  "message": "Validation failed for field 'password'"
}
```

## Swagger UI

Swagger UI is available at: `/swagger`


