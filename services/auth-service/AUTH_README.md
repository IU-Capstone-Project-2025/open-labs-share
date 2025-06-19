# Auth Service Documentation

## 1. Description and Purpose

The Authentication Service is responsible for centralized authentication and authorization pipelines. It implements
JWT (JSON Web Token) based authentication to manage security of Open Labs Share microservice system.
It provides endpoints for user registration, login, token management, and user profile retrieval.
The service acts as the security part for the system, ensuring that only authenticated and authorized
users can proceed further with accounts-sensitive requests.

## 2. Storage Architecture

### Database Design:

PostgreSQL database (`auth_service`) is used to store user information and authentication data.

#### Primary Entities:

1. **User Entity**:
    - Core user information (username, email, password hash)
    - Account status and metadata (creation date, last login)
    - Role information (using a Role enum)
    - Implements Spring Security's UserDetails for authentication (low level stuff)

    ```sql
    CREATE TABLE users (
        id BIGINT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        first_name VARCHAR(255) NOT NULL,
        second_name VARCHAR(255) NOT NULL,
        password VARCHAR(255) NOT NULL,
        email VARCHAR(255) NOT NULL UNIQUE,
        role VARCHAR(50) NOT NULL,
        created_at TIMESTAMP,
        last_login_at TIMESTAMP
    );

    CREATE SEQUENCE user_id_seq
        INCREMENT BY 1
        START WITH 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;
    ```

2. **Roles**: ROLE_USER

3. **Token Management**:
    - JWT tokens are stateless and not stored in the database
    - Token validation is performed cryptographically using the signing key

## 3. Business Logic

- **AuthenticationService**: Core authentication logic for sign-up, sign-in, token refresh
- **JwtService**: Token generation, validation, and parsing
- **UserService**: User management and retrieval
- **SecurityConfig**: Security configuration and filter chains
- **Health check**: `/api/v1/auth/health` returns a response to indicate service health

## 4. Connection with This Service

### Token Validation Flow

1. Client sends request with JWT token to API Gateway
2. API Gateway calls `/validate` endpoint
3. Auth service validates token and returns user info
4. API Gateway forwards request to target service with user context

## 5. Data Required from Other Services (see AUTH_API_DOCUMENTATION.md for detailed description of endpoints):

- Frontend:
    - User credentials (`usernameOrEmail`, `password`) for login (`/login`)
    - User profile data (`firstName`, `lastName`, `username`, `email`, `password`) for registration (`/register`)

## 6. Environment Configuration

### Environment Variables

Secrets for the service can be provided in a `.env` file in the root of the auth-service directory.

1. Create a `.env` file based on `.env.template`:
   ```
   cp .env.template .env
   ```

2. Edit the `.env` file and update the values as needed:
   ```
   # Database Configuration
   DB_URL=jdbc:postgresql://localhost:5432/auth_service
   DB_USERNAME=your_database_username
   DB_PASSWORD=your_database_password
   
   # JWT Configuration
   JWT_SIGNING_KEY=your_secure_random_signing_key
   ```

### Environment Variable Descriptions

| Variable        | Description                       | Example                                       |
|-----------------|-----------------------------------|-----------------------------------------------|
| DB_URL          | JDBC URL for PostgreSQL database  | jdbc:postgresql://localhost:5432/auth_service |
| DB_USERNAME     | Database username                 | postgres                                      |
| DB_PASSWORD     | Database password                 | postgres                                      |
| JWT_SIGNING_KEY | Secret key for signing JWT tokens | E0A2D3B5F2D7845...                            |
