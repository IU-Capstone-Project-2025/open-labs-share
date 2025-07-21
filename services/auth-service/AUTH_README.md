# Auth Service Documentation

## 1. Description and Purpose

The Authentication Service is a **stateless microservice** responsible for centralized authentication and authorization in the Open Labs Share system. It implements JWT (JSON Web Token) based authentication without storing any user data locally. 

The service acts as the security gateway for the system, ensuring that only authenticated and authorized users can access protected resources. **All user data is retrieved from the users-service via gRPC calls.**

## 2. Architecture

### Stateless Design

The auth-service **does not have its own database** and maintains no persistent state. It operates as a pure authentication service that:

- Generates and validates JWT tokens
- Authenticates users by calling users-service via gRPC
- Provides user information for token validation
- Manages token blacklisting for logout functionality

### Dependencies

- **Users-Service**: Primary dependency for all user data operations via gRPC
- **JWT Library**: For token generation, validation, and parsing
- **Spring Security**: For security configuration and authentication framework
- **Spring Web**: For REST API endpoints for frontend interactions
- **gRPC**: 
  - **Server**: For validating requests from API Gateway Service
  - **Client**: For Users Service operations with users data 

### Data Storage

**No Database**: The auth-service does not use any persistent storage. All user data is retrieved on-demand from users-service.

## 3. Business Logic

### Core Services

- **AuthenticationService**: Orchestrates authentication flows by calling users-service
- **JwtService**: Handles token generation, validation, parsing, and blacklisting
- **UserService**: Facade for users-service gRPC calls, implements UserDetailsService for Spring Security
- **UserProfileService**: Retrieves user profile data from users-service
- **SecurityConfig**: Security configuration and authentication filters

### Authentication Flow

1. **Sign Up**:
   - Validates input data
   - Calls users-service to create user account
   - Generates JWT tokens
   - Returns authentication response

2. **Sign In**:
   - Calls users-service to authenticate credentials
   - Generates JWT tokens with user information
   - Updates last login time in users-service
   - Returns authentication response

3. **Token Validation**:
   - Validates JWT signature and expiration
   - Checks token blacklist
   - Optionally fetches fresh user data from users-service
   - Returns user information

4. **Token Refresh**:
   - Validates refresh token
   - Fetches current user data from users-service
   - Generates new access and refresh tokens

## 4. gRPC Integration with Users-Service

The auth-service heavily relies on users-service gRPC endpoints:

### Used gRPC Endpoints

- `CreateUser`: For user registration
- `AuthenticateUser`: For credential validation during login
- `FindUserByUsername`: For loading user data by username
- `FindUserByEmail`: For loading user data by email
- `GetUserInfo`: For retrieving user information by ID
- `GetUserProfile`: For complete user profile data
- `UpdateUserProfile`: For updating user profile information
- `SearchUsers`: For searching users by username or name
- `UpdatePassword`: For password change operations
- `UpdateUserLastLogin`: For tracking login times
- `CheckUsernameExists`: For username availability validation
- `CheckEmailExists`: For email availability validation
- `DeleteUser`: For user deletion (used for rollback in distributed transaction failures)

### gRPC Client Configuration

```yaml
grpc:
  users-service:
    host: ${USERS_SERVICE_HOST:127.0.0.1}
    port: ${USERS_SERVICE_PORT:9093}
```

## 5. gRPC API

The auth-service provides a gRPC API for API Gateway token validation.

### gRPC Methods

#### ValidateToken
- **Purpose**: Validates JWT tokens for API Gateway integration
- **Request**: `ValidateTokenRequest { string token }`
- **Response**: `ValidateTokenResponse { bool valid, UserInfo user_info, int64 expiration_time, string error_message }`
- **Usage**: Called by API Gateway to validate incoming requests

#### HealthCheck
- **Purpose**: Service health monitoring
- **Request**: `HealthCheckRequest {}` (empty)
- **Response**: `HealthCheckResponse { bool success, string message, HealthData data }`
- **Usage**: Monitoring and service discovery

### gRPC Server Configuration

```yaml
spring:
  grpc:
    server:
      port: ${GRPC_PORT:9092}
      reflection:
        enabled: true
```

## 6. Connection with Other Services

### Token Validation Flow (API Gateway Integration)

1. Client sends request with JWT token to API Gateway
2. API Gateway calls auth-service `ValidateToken` gRPC method
3. Auth service validates token cryptographically
4. If needed, auth service fetches user data from users-service
5. API Gateway forwards request to target service with user context

### Frontend Integration

The service provides REST endpoints for frontend applications while maintaining backend integration via gRPC.

## 7. Data Required from Other Services

### From Users-Service (via gRPC):
- User authentication credentials
- User profile information
- User existence validation
- Password management operations

### From Frontend (via REST):
- User credentials for login
- User registration data
- Token refresh requests
- Password change requests

## 8. Environment Configuration

### Environment Variables

| Variable                      | Description                                   | Example                                       |
|-------------------------------|-----------------------------------------------|-----------------------------------------------|
| JWT_SIGNING_KEY               | Secret key for signing JWT tokens             | E0A2D3B5F2D7845...                            |
| ACCESS_TOKEN_EXPIRATION       | Expiration time for access tokens in ms       | 144000000 (24 hours)                          |
| REFRESH_TOKEN_EXPIRATION      | Expiration time for refresh tokens in ms      | 1008000000 (7 days)                           |
| PORT                          | HTTP server port                              | 8081                                          |
| GRPC_PORT                     | gRPC server port                              | 9092                                          |
| USERS_SERVICE_HOST            | Hostname of Users Service                     | 127.0.0.1                                     |
| USERS_SERVICE_PORT            | Port of Users Service gRPC server             | 9093                                          |

### Application Configuration

```yaml
spring:
  application:
    name: auth-service
  jackson:
    default-property-inclusion: non_null
  grpc:
    server:
      port: ${GRPC_PORT:9092}
      reflection:
        enabled: true

server:
  port: ${PORT:8081}

token:
  signing:
    key: ${JWT_SIGNING_KEY:defaultSigningKey...}
  access:
    expiration: ${ACCESS_TOKEN_EXPIRATION:144000000}
  refresh:
    expiration: ${REFRESH_TOKEN_EXPIRATION:1008000000}

grpc:
  users-service:
    host: ${USERS_SERVICE_HOST:127.0.0.1}
    port: ${USERS_SERVICE_PORT:9093}
```
