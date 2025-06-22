# Auth Service Documentation

## 1. Description and Purpose

The Authentication Service is a **stateless microservice** responsible for centralized authentication and authorization in the Open Labs Share system. It implements JWT (JSON Web Token) based authentication without storing any user data locally. 

The service acts as the security gateway for the system, ensuring that only authenticated and authorized users can access protected resources. **All user data is retrieved from the users-service via gRPC calls.**

### Separation of Concerns

This service follows a strict separation of concerns:

- **Auth-Service**: Responsible ONLY for authentication and authorization (JWT token management, validation)
- **Users-Service**: Single source of truth for ALL user data (usernames, passwords, emails, profiles, roles, etc.)

The auth-service operates as a **stateless service** that:
- Generates and validates JWT tokens
- Authenticates users by calling users-service via gRPC
- Provides user information for token validation
- Manages token blacklisting for logout functionality

## 2. Architecture

### Stateless Design

The auth-service **does not have its own database** and maintains no persistent state. It operates as a pure authentication service that:

1. **Authentication Flow**: Validates credentials by calling users-service gRPC endpoints
2. **Token Generation**: Creates JWT tokens containing user information from users-service
3. **Token Validation**: Validates tokens and optionally fetches fresh user data from users-service
4. **User Operations**: All user-related operations are delegated to users-service

### Dependencies

- **Users-Service**: Primary dependency for all user data operations via gRPC
- **JWT Library**: For token generation, validation, and parsing
- **Spring Security**: For security configuration and authentication framework
- **In-Memory Token Blacklist**: For logout functionality (tokens are blacklisted in memory)

### Data Storage

**No Database**: The auth-service does not use any persistent storage. All user data is retrieved on-demand from users-service.

**In-Memory Storage**:
- Blacklisted tokens (for logout functionality)
- Application cache (if needed for performance optimization)

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
- `UpdatePassword`: For password change operations
- `UpdateUserLastLogin`: For tracking login times
- `CheckUsernameExists`: For username availability validation
- `CheckEmailExists`: For email availability validation

### gRPC Client Configuration

```yaml
grpc:
  users-service:
    host: ${USERS_SERVICE_HOST:localhost}
    port: ${USERS_SERVICE_PORT:9093}
```

## 5. Connection with Other Services

### Token Validation Flow (API Gateway Integration)

1. Client sends request with JWT token to API Gateway
2. API Gateway calls auth-service `/validate` endpoint
3. Auth service validates token cryptographically
4. If needed, auth service fetches user data from users-service
5. API Gateway forwards request to target service with user context

### Frontend Integration

The service provides REST endpoints for frontend applications while maintaining backend integration via gRPC.

## 6. Data Required from Other Services

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

## 7. Environment Configuration

### Environment Variables

| Variable                      | Description                                   | Example                                       |
|-------------------------------|-----------------------------------------------|-----------------------------------------------|
| JWT_SIGNING_KEY               | Secret key for signing JWT tokens             | E0A2D3B5F2D7845...                            |
| ACCESS_TOKEN_EXPIRATION       | Expiration time for access tokens in ms       | 144000000 (24 hours)                          |
| REFRESH_TOKEN_EXPIRATION      | Expiration time for refresh tokens in ms      | 1008000000 (7 days)                           |
| PORT                          | Server port                                   | 8081                                          |
| USERS_SERVICE_HOST            | Hostname of Users Service                     | localhost                                     |
| USERS_SERVICE_PORT            | Port of Users Service gRPC server             | 9093                                          |

### Application Configuration

```yaml
spring:
  application:
    name: auth-service
  jackson:
    default-property-inclusion: non_null

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
    host: ${USERS_SERVICE_HOST:localhost}
    port: ${USERS_SERVICE_PORT:9093}
```

## 8. Docker Deployment

### Building the Docker Image

```bash
# From the auth-service directory
docker build -t auth-service .
```

### Running with Docker Compose

```bash
# From the project root
docker-compose up -d auth-service users-service
```

### Docker Dependencies

The auth-service requires:
- **users-service**: Must be running and accessible via gRPC
- **No database**: Auth-service is database-free

## 9. Monitoring and Health Checks

### Health Check Endpoint

- `GET /api/v1/auth/health`: Returns service health status
- Includes connectivity status to users-service
- Available via Spring Boot Actuator endpoints

### Metrics

Available metrics include:
- JWT token generation/validation rates
- gRPC call success/failure rates to users-service
- Authentication success/failure rates
- Active token count (via blacklist size)

## 10. Security Considerations

### Token Security
- JWT tokens are signed with a strong secret key
- Tokens include user ID, role, and other claims
- Refresh tokens have longer expiration times
- Blacklisted tokens are stored in memory for logout functionality

### gRPC Security
- All user data transmission occurs over gRPC between internal services
- No sensitive data is stored in auth-service
- Users-service is the single source of truth for all user information

### Production Recommendations
- Use strong, randomly generated JWT signing keys
- Configure appropriate token expiration times
- Monitor gRPC connectivity to users-service
- Implement proper logging for security events
- Consider implementing distributed token blacklisting for multi-instance deployments
