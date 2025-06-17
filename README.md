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
3. Start our app (*hello-world* for now) using Docker Compose: `docker-compose up --build -d`
4. Check `http://localhost:5173/`, there will be `Hello World` text rendered by the frontend.
5. After it, you can visit `http://localhost:8080/api/v1/hello`, there will be phrase - "Hello world! This is Capstone =)"

Table with all the services after successful Docker run:
| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:5173/ | Frontend (React + Vite) |
| Backend API | http://localhost:8080/api/v1/ | Backend (Spring Boot) |


## Contributing

Please read [Contributing Guidelines](CONTRIBUTING.md) for details on our code of conduct and the process for submitting commits and pull requests.

## License

Copyright (c) 2025 Open Labs Share. All Rights Reserved.
