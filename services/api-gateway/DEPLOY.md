# API Gateway Deployment Guide

## Prerequisites

### Required Software
- **Docker**: Version 20.0 or higher
    - [Install Docker Desktop](https://www.docker.com/products/docker-desktop/) for Windows/Mac
    - [Install Docker Engine](https://docs.docker.com/engine/install/) for Linux
- **Docker Compose**: Usually included with Docker Desktop
- **Git**: For cloning the repository

### Verify Installation
```bash
# Check Docker version
docker --version

# Check Docker Compose version
docker compose version

# Verify Docker is running
docker ps
```

## Network Setup

### Create Docker Network
Before deploying, ensure the gRPC network exists:

```bash
# Create external network for gRPC services
docker network create grpc-network

# Verify network creation
docker network ls | grep grpc-network
```

**Note**: This network should be shared with your auth-service and user-service containers.

## Environment Configuration

### Copy Environment Template

#### Windows (Command Prompt)
```cmd
copy .env.example .env
```

#### Windows (PowerShell)
```powershell
Copy-Item .env.example .env
```

#### Mac/Unix/Linux
```bash
cp .env.example .env
```

### Environment Variables Configuration

Edit the `.env` file based on the [README](./README.md) and update these key variables as needed:

#### Logging Configuration
```env
# Adjust logging levels as needed
LOG_LEVEL_ROOT=INFO      # Options: DEBUG, INFO, WARN, ERROR
LOG_LEVEL_APP=DEBUG      # For development, use INFO for production
LOG_LEVEL_GRPC=INFO      # gRPC-specific logging
```

#### Network Configuration
```env
# Should match the Docker network name you created
NETWORK_NAME=grpc-network
```

## Build and Deploy

### 1. Build the Application
```bash
# Build the Docker image
docker-compose build -d

# Build with no cache (if you need fresh build)
docker-compose build --no-cache
```

### 2. Start the Service
```bash
# Start in background
docker compose up -d

# Start with logs visible
docker compose up

# Start and rebuild if needed
docker compose up --build -d
```

## Service Dependencies

### Required External Services
Ensure these services are running before starting the API Gateway:

1. **Auth Service** - Should be accessible at `grpc-auth-service:9090`
2. **User Service** - Should be accessible at `grpc-user-service:9091`

### Check Service Connectivity
The name `api-gateway` is defined in your `.env` file.
```bash
# Test if gRPC services are reachable
docker exec api-gateway ping grpc-auth-service
docker exec api-gateway ping grpc-user-service
```
