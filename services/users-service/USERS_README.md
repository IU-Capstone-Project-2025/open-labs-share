# Users Service Documentation

## 1. Description and Purpose

The Users Service is the **single source of truth** for all user-related data in the Open Labs Share microservice ecosystem. It manages user profiles, authentication credentials, and all personal data through a gRPC-only API. 

This service stores **ALL user information** including:
- Authentication credentials (usernames, passwords, roles)
- Personal information (names, emails, profiles)
- User metadata (creation dates, last login times)
- User preferences and settings

## 2. Architecture

### Single Source of Truth Design

The Users Service follows the principle of being the **single source of truth** for user data:

- **Centralized Storage**: All user data is stored in one place
- **gRPC-Only API**: No REST endpoints, only gRPC for internal service communication
- **Authentication Support**: Handles password hashing, validation, and user authentication
- **Profile Management**: Complete user profile CRUD operations

## 3. Database Schema

### User Entity

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- Bcrypt hashed
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    last_login_at TIMESTAMP,
    labs_solved INTEGER DEFAULT 0,    -- Points system
    labs_reviewed INTEGER DEFAULT 0,  -- Points system
    balance INTEGER DEFAULT 10        -- Points system
);
```

### Fields Description

- **id**: Primary key, auto-generated
- **username**: Unique username for login
- **email**: Unique email address for login and communication
- **password**: Bcrypt-hashed password
- **first_name/last_name**: User's personal information
- **role**: User role (ROLE_USER, ROLE_ADMIN, etc.)
- **created_at**: Account creation timestamp
- **last_login_at**: Last successful login timestamp
- **labs_solved**: Number of laboratory assignments completed by the user (points system)
- **labs_reviewed**: Number of laboratory assignments reviewed by the user (points system)
- **balance**: Current point balance available for solving new labs (points system)

## 4. gRPC API


### User Management

- `CreateUser`: Create a new user account with all information
- `GetUserInfo`: Get basic user information by ID
- `GetUserProfile`: Get complete user profile by ID
- `UpdateUserProfile`: Update user profile information
- `DeleteUser`: Delete a user account

### Authentication Support

- `AuthenticateUser`: Validate username/email and password
- `FindUserByUsername`: Find user by username
- `FindUserByEmail`: Find user by email
- `UpdatePassword`: Change user password
- `UpdateUserLastLogin`: Update last login timestamp

### User Discovery

- `SearchUsers`: Search users by query (username or name)
- `CheckUsernameExists`: Check if username is available
- `CheckEmailExists`: Check if email is available

### Points System

- `IncrementLabsSolved`: Increment labs solved counter and update balance
- `IncrementLabsReviewed`: Increment labs reviewed counter and update balance

### System Support

- `HealthCheck`: Service health status

## 5. Proto Definitions

### Key Message Types

```proto
message UserInfo {
  int64 user_id = 1;
  string username = 2;
  string first_name = 3;
  string last_name = 4;
  string role = 5;
  string email = 6;
  int32 labs_solved = 7;
  int32 labs_reviewed = 8;
  int32 balance = 9;
}

message UserProfileResponse {
  UserInfo user_info = 1;
  string status = 2;
}

message CreateUserRequest {
  string username = 1;
  string first_name = 2;
  string last_name = 3;
  string email = 4;
  string role = 5;
  string password = 6;
}

message AuthenticateUserRequest {
  string username = 1; // Can be username or email
  string password = 2;
  bool using_email = 3;
}

message IncrementLabsSolvedRequest {
  int64 user_id = 1;
}

message IncrementLabsReviewedRequest {
  int64 user_id = 1;
}

message IncrementStatsResponse {
  UserInfo user_info = 1;
  string message = 2;
}
```

### Service Definition

```proto
service UsersService {
  // User Management
  rpc CreateUser (CreateUserRequest) returns (UserProfileResponse) {}
  rpc GetUserProfile (GetUserProfileRequest) returns (UserProfileResponse) {}
  rpc UpdateUserProfile (UpdateUserProfileRequest) returns (UserProfileResponse) {}
  rpc DeleteUser (DeleteUserRequest) returns (DeleteUserResponse) {}
  
  // Authentication Support
  rpc AuthenticateUser (AuthenticateUserRequest) returns (UserInfoResponse) {}
  rpc FindUserByUsername (FindUserByUsernameRequest) returns (UserInfoResponse) {}
  rpc FindUserByEmail (FindUserByEmailRequest) returns (UserInfoResponse) {}
  rpc UpdatePassword (UpdatePasswordRequest) returns (UpdatePasswordResponse) {}
  rpc UpdateUserLastLogin (UpdateUserLastLoginRequest) returns (UpdateUserLastLoginResponse) {}
  
  // User Discovery
  rpc SearchUsers (SearchUsersRequest) returns (SearchUsersResponse) {}
  rpc CheckUsernameExists (FindUserByUsernameRequest) returns (ExistsResponse) {}
  rpc CheckEmailExists (FindUserByEmailRequest) returns (ExistsResponse) {}
  
  // Points System
  rpc IncrementLabsSolved (IncrementLabsSolvedRequest) returns (IncrementStatsResponse) {}
  rpc IncrementLabsReviewed (IncrementLabsReviewedRequest) returns (IncrementStatsResponse) {}
  
  // System Support
  rpc GetUserInfo (GetUserInfoRequest) returns (UserInfoResponse) {}
  rpc HealthCheck (HealthCheckRequest) returns (HealthCheckResponse) {}
}
```

## 6. Integration with Auth Service

The Users Service is the **primary dependency** of the Auth Service. All authentication and user operations are delegated to this service:

### Authentication Flow

1. **User Registration**:
   - Auth Service validates request
   - Calls `CreateUser` to store user data
   - Auth Service generates JWT tokens

2. **User Login**:
   - Auth Service calls `AuthenticateUser` with credentials
   - Users Service validates password and returns user data
   - Auth Service calls `UpdateUserLastLogin`
   - Auth Service generates JWT tokens

3. **Token Validation**:
   - Auth Service validates JWT cryptographically
   - Optionally calls `GetUserInfo` for fresh user data

4. **Password Management**:
   - Auth Service calls `UpdatePassword` for password changes
   - Users Service handles password hashing and validation

5. **Points System Integration**:
   - Labs Service calls `IncrementLabsSolved` when users complete labs
   - Labs Service calls `IncrementLabsReviewed` when users review labs

### gRPC Client Configuration (Auth Service)

```yaml
grpc:
  users-service:
    host: ${USERS_SERVICE_HOST:localhost}
    port: ${USERS_SERVICE_PORT:9093}
```

## 7. Points System

### Overview

- **Initial Balance**: New users start with 10 points (configurable via `POINTS_INITIAL_BALANCE`)
- **Solving Labs**: Costs 1 points per lab assignment (configurable via `POINTS_SOLVE_COST`)
- **Reviewing Labs**: Earns 3 points per lab reviewed (configurable via `POINTS_SOLVE_COSE` * `POINTS_REVIEW_MULTIPLIER`)
- **Balance Validation**: Users cannot solve labs if their balance is insufficient

### Points Configuration

| Variable                    | Description                           | Default |
|-----------------------------|---------------------------------------|---------|
| POINTS_INITIAL_BALANCE      | Starting points for new users         | 10      |
| POINTS_SOLVE_COST           | Points deducted when solving labs     | 1       |
| POINTS_REVIEW_MULTIPLIER    | Multiplier for points earned reviewing| 3       |

### Points Operations

#### IncrementLabsSolved
- **Purpose**: Called when a user completes a lab assignment
- **Business Logic**: 
  - Validates user has sufficient balance (>= solve cost)
  - Increments `labs_solved` counter
  - Deducts points from user balance (solve_cost)
  - Operation is atomic and transactional
- **Error Cases**: Returns `INSUFFICIENT_BALANCE` if user cannot afford the lab

#### IncrementLabsReviewed
- **Purpose**: Called when a user reviews a lab assignment
- **Business Logic**:
  - Increments `labs_reviewed` counter
  - Adds points to user balance (solve_cost × review_multiplier)
  - Operation is atomic and transactional
- **Error Cases**: None (reviewing always succeeds)

### Integration with Labs Service

The points system is designed to integrate with the labs-service:
- Labs-service calls `IncrementLabsSolved` when users submit labs
- Labs-service calls `IncrementLabsReviewed` when users complete reviews
- All user profile responses include current points data

## 8. Configuration

### Environment Variables

| Variable                    | Description                           | Default                                          |
|-----------------------------|---------------------------------------|--------------------------------------------------|
| DB_URL                      | JDBC URL for PostgreSQL database     | jdbc:postgresql://localhost:5432/users_service   |
| DB_USERNAME                 | Database username                     | postgres                                         |
| DB_PASSWORD                 | Database password                     | postgres                                         |
| GRPC_PORT                   | gRPC server port                      | 9093                                             |
| HIBERNATE_DDL_AUTO          | Hibernate DDL auto mode               | update                                           |
| SHOW_SQL                    | Show SQL queries in logs              | false                                            |
| LOG_LEVEL                   | Application log level                 | INFO                                             |
| POINTS_INITIAL_BALANCE      | Starting points for new users         | 10                                               |
| POINTS_SOLVE_COST           | Points deducted when solving labs     | 1                                                |
| POINTS_REVIEW_MULTIPLIER    | Multiplier for points earned reviewing| 3                                                |
