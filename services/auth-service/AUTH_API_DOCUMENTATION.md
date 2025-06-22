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
    "role": "ROLE_USER"
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
    "role": "ROLE_USER"
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

### 7. Change Password

**Endpoint:** `PUT /change-password`

**Description:** Changes user's password by delegating to users-service. Requires valid authentication token.

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

- `400 Bad Request` - Invalid current password (validation failed in users-service)
- `401 Unauthorized` - Invalid or missing token
- `404 Not Found` - User not found in users-service

---

### 8. Get User Profile

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
    "role": "ROLE_USER"
  },
  "status": "ACTIVE"
}
```

**Error Responses:**

- `401 Unauthorized` - Invalid or missing token
- `404 Not Found` - User not found in users-service

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


