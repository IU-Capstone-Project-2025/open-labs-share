# Dockerfiles Configuration

This directory contains Dockerfiles for containerizing different components of the application.

## Available Dockerfiles

### Frontend (`Dockerfile.frontend`)
- **Base Image**: `node:18-alpine`
- **Purpose**: Containerizes the React + Vite frontend application
- **Port**: 5173

### Hello World App (`Dockerfile.helloworld`)
- **Base Image**: Java-based (Spring Boot application)
- **Purpose**: Containerizes the backend hello-world service
- **Port**: 8080