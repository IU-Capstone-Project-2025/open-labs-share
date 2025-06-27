# Open Labs Share

A peer-to-peer educational platform connecting experts with learners through hands-on, practical learning experiences.

## Overview

Open Labs Share is a collaborative learning platform that combines practical lab exercises with community feedback. It enables subject-matter experts to create and share practical learning materials while allowing students to submit their work and receive peer reviews.

## Key Features

- **Expert-Created Labs**: Step-by-step practical exercises designed by subject-matter experts
- **Practical Learning**: Hands-on exercises focused on real-world applications
- **Community Feedback**: Peer review system for evaluating and improving submitted work
- **Knowledge Sharing**: Collaborative environment for exchanging practical knowledge

## Core Functionality

- Create and manage personal accounts
- Publish lab materials (Markdown documents with assets)
- Submit completed work in PDF format
- Review and provide feedback on submitted assignments
- Access practical exercises and hands-on tasks

## Getting Started

### Prerequisites

- [git](https://git-scm.com/)
- [Docker](https://www.docker.com/)

### Installation

1. Clone our project: `git clone https://github.com/IU-Capstone-Project-2025/open-labs-share.git`
2. Go into project folder on your system: `cd open-labs-share`
3. Start our app using Docker Compose: `docker-compose up --build -d`
4. Check `http://localhost:5173/` for the frontend.
5. The API gateway is available at `http://localhost:8080/`.

## Services Architecture

The platform consists of multiple microservices, each handling specific functionality:

| Service | URL | Description | Documentation |
|---|---|---|---|
| **Frontend** | `http://localhost:5173` | React + Vite frontend application | [Frontend Docs](frontend/README.md) |
| **API Gateway** | `http://localhost:8080` | Central routing and authentication gateway | [API Gateway Docs](services/api-gateway/DEPLOY.md) |
| **Auth Service** | `http://localhost:8081` | User authentication and JWT token management | [Auth Service](services/auth-service/AUTH_README.md) · [API Docs](services/auth-service/AUTH_API_DOCUMENTATION.md) |
| **Users Service** | gRPC on port `9093` | Manages user profiles and data | [Users Service Docs](services/users-service/README.md) |
| **Labs Service** | gRPC on port `9091` | Manages lab assignments and submissions | [Labs Service Docs](services/labs-service/LABS_README.md) |
| **Feedback Service** | gRPC on port `9090` | Lab feedback and comments system | [Feedback Service Docs](services/feedback-service/FEEDBACK_README.md) |
| **ML Service** | `http://localhost:8083` | AI assistant for lab-related questions | [ML Service Docs](ml/README.md) |

### Data Storage

| Service | URL | Description |
|---|---|---|
| **PostgreSQL (Users)** | `localhost:5432` | Stores user data |
| **PostgreSQL (Feedback)**| `localhost:5435` | Stores feedback and comments |
| **PostgreSQL (Labs)** | `localhost:5434` | Stores lab data |
| **PostgreSQL (ML)** | `localhost:5433` | Stores chat history for the ML service |
| **MinIO** | `http://localhost:9000` | S3-compatible object storage for lab assets and submissions |

## Contributing

Please read [Contributing Guidelines](CONTRIBUTING.md) for details on our code of conduct and the process for submitting commits and pull requests.

## License

Copyright (c) 2025 Open Labs Share. All Rights Reserved.
