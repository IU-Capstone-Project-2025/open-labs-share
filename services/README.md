# Services

This directory contains the backend microservices for the Open Labs Share platform. Each service is designed to handle a specific domain of functionality, communicating with each other via gRPC.

## Service Overview

| Service | Description | Documentation |
|---|---|---|
| **API Gateway** | The central entry point for all client requests. It routes traffic to the appropriate downstream services and handles cross-cutting concerns like authentication and rate limiting. | [API Gateway Docs](./api-gateway/README.md) |
| **Auth Service** | Manages user authentication, including registration, login, and JWT token management. It works closely with the Users Service to handle user-related data. | [Auth Service Docs](./auth-service/AUTH_README.md) |
| **Users Service** | Responsible for managing user profiles, including personal data, roles, points system, and permissions. | [Users Service Docs](./users-service/USERS_README.md) |
| **Articles Service**| Handles the creation, publication, and management of articles and other educational content. | [Articles Service Docs](./articles-service/ARTICLES_README.md) |
| **Labs Service** | Manages lab assignments, including creation, submissions, and grading. | [Labs Service Docs](./labs-service/LABS_README.md) |
| **Feedback Service**| Powers the peer-review system, handling feedback, comments, and ratings for lab submissions. | [Feedback Service Docs](./feedback-service/FEEDBACK_README.md) |
| **Marimo Service** | A two-part service that integrates interactive Python notebooks into the platform. | [Marimo Service Docs](./marimo-service/MARIMO_README.md) |
| &nbsp;&nbsp;&nbsp;*Marimo Manager* | Manages the lifecycle of Marimo notebooks, including creation, session management, and persistence. | [Marimo Manager Docs](./marimo-service/marimo-manager-service/MARIMO_MANAGER_README.md) |
| &nbsp;&nbsp;&nbsp;*Marimo Executor*| Executes the code within Marimo notebooks, handling the runtime environment and returning the output. | [Marimo Executor Docs](./marimo-service/marimo-executor-service/MARIMO_EXECUTOR_README.md) |