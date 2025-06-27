# Dockerfiles Configuration

This directory contains Dockerfiles for containerizing all components of the Open Labs Share application.

## Available Dockerfiles

### Frontend (`Dockerfile.frontend`)
- **Base Image**: `node:18-alpine`
- **Purpose**: Containerizes the React + Vite frontend application
- **Port**: 5173
- **Technology**: Node.js, React, Vite

### API Gateway (`Dockerfile.api-gateway`)
- **Base Image**: `gradle:8-jdk21` (build), `eclipse-temurin:21-jre-jammy` (runtime)
- **Purpose**: Containerizes the API Gateway service with Spring Boot and gRPC
- **Port**: 8080
- **Technology**: Java 21, Spring Boot, gRPC, Gradle

### Auth Service (`Dockerfile.auth-service`)
- **Base Image**: `gradle:8-jdk21` (build), `eclipse-temurin:21-jre-jammy` (runtime)
- **Purpose**: Containerizes the Authentication Service
- **Ports**: 8081 (HTTP), 9092 (gRPC)
- **Technology**: Java 21, Spring Boot, gRPC, JWT, Gradle

### Users Service (`Dockerfile.users-service`)
- **Base Image**: `gradle:8-jdk17` (build), `eclipse-temurin:17-jre-jammy` (runtime)
- **Purpose**: Containerizes the Users Management Service
- **Port**: 9093 (gRPC)
- **Technology**: Java 17, Spring Boot, gRPC, PostgreSQL, Gradle

### Articles Service (`Dockerfile.articles-service`)
- **Base Image**: `python:3.12-slim`
- **Purpose**: Containerizes the Articles Management Service
- **Port**: 9092 (gRPC)
- **Technology**: Python 3.12, gRPC, PostgreSQL, MinIO

### Labs Service (`Dockerfile.labs-service`)
- **Base Image**: `python:3.12-slim`
- **Purpose**: Containerizes the Labs Management Service
- **Port**: 9091 (gRPC)
- **Technology**: Python 3.12, gRPC, PostgreSQL, MinIO

### Feedback Service (`Dockerfile.feedback-service`)
- **Base Image**: `golang:1.24-alpine` (build), `gcr.io/distroless/static-debian11` (runtime)
- **Purpose**: Containerizes the Feedback Management Service
- **Port**: 9090 (gRPC)
- **Technology**: Go 1.24, gRPC, PostgreSQL, MinIO

## Multi-Stage Build Strategy

Most services use multi-stage builds for optimal image size and security:

1. **Build Stage**: Compiles the application with full SDK/build tools
2. **Runtime Stage**: Minimal runtime image with only necessary dependencies

## Security Features

- **Non-root users**: All services run as non-root users for security
- **Minimal base images**: Uses Alpine, distroless, or slim variants
- **Custom JRE**: Java services use `jlink` to create minimal JRE
- **Health checks**: Built-in health monitoring for all services

## Port Configuration

| Service | HTTP Port | gRPC Port | External Port |
|---------|-----------|-----------|---------------|
| Frontend | 5173 | - | 5173 |
| API Gateway | 8080 | - | 8080 |
| Auth Service | 8081 | 9092 | 8081, 9092 |
| Users Service | - | 9093 | 9093 |
| Articles Service | - | 9092 | 9094 |
| Labs Service | - | 9091 | 9095 |
| Feedback Service | - | 9090 | 9090 |
| Hello World App | 8080 | - | 8082 |

## Usage

All Dockerfiles are designed to be used with the root `docker-compose.yml`:

```bash
# Build and start all services
docker-compose up --build

# Start specific service
docker-compose up --build <service-name>

# View logs
docker-compose logs <service-name>
```

## Dependencies

### Database Services
- **PostgreSQL**: Multiple instances for different services
  - `postgres-users` (port 5432) - Users Service
  - `postgres-articles` (port 5433) - Articles Service
  - `postgres-labs` (port 5434) - Labs Service
  - `postgres-feedback` (port 5435) - Feedback Service

### Storage Services
- **MinIO**: Object storage for file management (ports 9000, 9001)

### Network
- **app-network**: Bridge network connecting all services

## Environment Variables

Each service uses environment variables for configuration. Key variables include:

- Database connection strings
- gRPC service endpoints
- Authentication keys
- MinIO configuration
- Application profiles

See `docker-compose.yml` for complete environment configuration.

## Notes

- **Python Services**: Articles and Labs services are placeholder implementations
- **Service Discovery**: Services communicate via Docker network hostnames
- **Health Checks**: All services include health monitoring
- **Data Persistence**: Database and storage volumes are persisted