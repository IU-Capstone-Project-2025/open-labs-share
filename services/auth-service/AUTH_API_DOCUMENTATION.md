# Authentication Service API Documentation

## Base URL `/api/v1/auth`

## Authentication Endpoints

### 1. User Registration

**Endpoint:** `POST /register`

**Description:** Creates a new user account and returns JWT tokens.

**Request Body:**

```json
{
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
  "userId": 123,
  "username": "johndoe",
  "role": "ROLE_USER"
}
```

**Error Responses:**

- `400 Bad Request` - Invalid request data
- `409 Conflict` - User already exists

---

### 2. User Login

**Endpoint:** `POST /login`

**Description:** Authenticates user credentials and returns JWT tokens.

**Request Body:**

```json
{
  "username": "johndoe",
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
  "userId": 123,
  "username": "johndoe",
  "role": "ROLE_USER"
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid credentials
- `423 Locked` - Account locked

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
  "userId": 123,
  "username": "johndoe",
  "role": "ROLE_USER"
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid or expired refresh token

---

### 4. Token Validation (API Gateway Use)

**Endpoint:** `POST /validate`

**Description:** Validates JWT token and returns user information - primarily used by API Gateway.

**Request Body:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**

```json
{
  "valid": true,
  "userId": 123,
  "username": "johndoe",
  "role": "ROLE_USER",
  "expirationTime": 1622506800,
  "errorMessage": null
}
```

**Invalid Token Response:**

```json
{
  "valid": false,
  "userId": null,
  "username": null,
  "role": null,
  "expirationTime": null,
  "errorMessage": "Token has expired"
}
```

---

### 5. User Logout

**Endpoint:** `POST /logout`

**Description:** Invalidates the current JWT token and logs out the user.

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

### 6. Password Reset Request ⚠️ NOT YET IMPLEMENTED

**Endpoint:** `POST /password-reset`

**Description:** Sends password reset instructions to user's email.

**Request Body:**

```json
{
  "email": "johndoe@example.com"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Password reset instructions sent to your email",
  "data": null
}
```

**Error Responses:**

- `404 Not Found` - User not found

---

### 7. Password Reset Confirmation ⚠️ NOT YET IMPLEMENTED

**Endpoint:** `POST /password-reset/confirm`

**Description:** Resets user password using reset token.

**Request Body:**

```json
{
  "token": "abc123xyz789",
  "newPassword": "newSecurePassword123"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null
}
```

**Error Responses:**

- `400 Bad Request` - Invalid or expired reset token

---

### 8. Change Password

**Endpoint:** `PUT /change-password`

**Description:** Changes user's password (requires authentication).

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body:**

```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword123"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null
}
```

**Error Responses:**

- `400 Bad Request` - Invalid current password
- `401 Unauthorized` - Invalid or missing token

---

### 9. Get User Profile

**Endpoint:** `GET /profile`

**Description:** Retrieves authenticated user's profile information.

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**

```json
{
  "id": 123,
  "username": "johndoe",
  "email": "johndoe@example.com",
  "role": "USER",
  "createdAt": "2023-01-15T10:30:00",
  "lastLoginAt": "2023-01-20T14:45:00",
  "status": "ACTIVE"
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid or missing token

---

### 10. Email Verification ⚠️ NOT YET IMPLEMENTED

**Endpoint:** `GET /verify-email/{token}`

**Description:** Verifies user's email address using verification token.

**Path Parameters:**

- `token` (string) - Email verification token

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": null
}
```

**Error Responses:**

- `400 Bad Request` - Invalid or expired verification token

---

### 11. Health Check

**Endpoint:** `GET /health`

**Description:** Returns the health status of the authentication service.

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Auth service is healthy",
  "data": {
    "timestamp": "2025-06-17T10:30:00Z",
    "service": "auth-service",
    "version": "1.0.0"
  }
}
```

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


