group "test-build" {
  targets = [
    "frontend",
    "api-gateway",
    "auth-service",
    "users-service",
    "labs-service",
    "articles-service",
    "feedback-service"
  ]
}

group "production-build" {
  targets = [
    "frontend",
    "api-gateway",
    "auth-service",
    "users-service",
    "labs-service",
    "articles-service",
    "feedback-service",
    "ml-service"
  ]
}

variable "REGISTRY" {
  default = "ghcr.io/iu-capstone-project-2025"
}

target "frontend" {
  dockerfile = "docker/Dockerfile.frontend"
  tags       = ["${REGISTRY}/open-labs-share-frontend:latest"]
}

target "api-gateway" {
  dockerfile = "docker/Dockerfile.api-gateway"
  tags       = ["${REGISTRY}/open-labs-share-api-gateway:latest"]
}

target "auth-service" {
  dockerfile = "docker/Dockerfile.auth-service"
  tags       = ["${REGISTRY}/open-labs-share-auth-service:latest"]
}

target "users-service" {
  dockerfile = "docker/Dockerfile.users-service"
  tags       = ["${REGISTRY}/open-labs-share-users-service:latest"]
}

target "labs-service" {
  dockerfile = "docker/Dockerfile.labs-service"
  tags       = ["${REGISTRY}/open-labs-share-labs-service:latest"]
}

target "articles-service" {
  dockerfile = "docker/Dockerfile.articles-service"
  tags       = ["${REGISTRY}/open-labs-share-articles-service:latest"]
}

target "feedback-service" {
  dockerfile = "docker/Dockerfile.feedback-service"
  tags       = ["${REGISTRY}/open-labs-share-feedback-service:latest"]
}

target "ml-service" {
  dockerfile = "docker/Dockerfile.ml-service"
  tags       = ["${REGISTRY}/open-labs-share-ml-service:latest"]
} 