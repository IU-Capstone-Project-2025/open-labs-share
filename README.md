
<div align="center">
    <h1><strong>Open Labs Share</strong></h1>
    <p>A peer-to-peer educational platform connecting experts with learners through hands-on, practical learning experiences.</p>
  <a href="https://github.com/IU-Capstone-Project-2025/open-labs-share">
    <img src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" alt="Build Status"/>
  </a>
  <a href="https://github.com/IU-Capstone-Project-2025/open-labs-share/blob/main/LICENSE.md">
    <img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License"/>
  </a>
  <a href="https://github.com/IU-Capstone-Project-2025/open-labs-share/issues">
    <img src="https://img.shields.io/github/issues/IU-Capstone-Project-2025/open-labs-share?style=flat-square" alt="GitHub issues"/>
  </a>
  <a href="https://github.com/IU-Capstone-Project-2025/open-labs-share/stargazers">
    <img src="https://img.shields.io/github/stars/IU-Capstone-Project-2025/open-labs-share?style=flat-square" alt="GitHub stars"/>
  </a>

</div>

## 🧭 Table of Contents

- [🧭 Table of Contents](#-table-of-contents)
- [🔭 Overview](#-overview)
- [✨ Key Features](#-key-features)
- [💡 Core Functionality](#-core-functionality)
- [🚀 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [🛠️ Technology Stack](#️-technology-stack)
  - [Core Technologies](#core-technologies)
  - [Service-Specific Technologies](#service-specific-technologies)
- [🔵🟢 Blue/Green Deployment](#-bluegreen-deployment)
- [📖 Documentation](#-documentation)
  - [General Documentation](#general-documentation)
  - [Frontend Documentation](#frontend-documentation)
  - [Core Services Documentation](#core-services-documentation)
    - [API Gateway](#api-gateway)
    - [Authentication \& Users](#authentication--users)
    - [Learning Platform](#learning-platform)
    - [AI \& Machine Learning](#ai--machine-learning)
    - [Interactive Notebooks](#interactive-notebooks)
- [🤝 Contributing](#-contributing)
- [📜 License](#-license)

## 🔭 Overview

Open Labs Share is a collaborative learning platform that combines practical lab exercises with community feedback. It enables subject-matter-experts to create and share practical learning materials while allowing students to submit their work and receive peer reviews. The platform leverages **Marimo for interactive notebooks** along with **AI-powered assistants and code reviews**, creating a dynamic and engaging learning experience.

## ✨ Key Features

- **Interactive Notebooks**: Create and share labs with Python code cells and **Marimo**, allowing for live, interactive data science and coding exercises.
- **Interactive Widget Library**: Build dynamic lab environments with essential marimo widgets including sliders, checkboxes, switchers, number and text fields, dropdown and multiselect. Mathematical visualizations are available through matplotlib, numpy, pandas, and other Python libraries. Upload and use assets like txt and csv files directly in your code.
- **AI-powered assistants and code reviews**: Get help with your lab assignments and automatic code reviews.
- **Peer-to-Peer Feedback**: A comprehensive peer review system allows for community-driven feedback and evaluation of lab submissions.
- **Content Creation & Management**: Users can create, publish, and manage their own articles and labs.
- **Knowledge Sharing**: A collaborative space for sharing practical knowledge and expertise.

## 💡 Core Functionality

- **User Accounts**: Create and manage personal user profiles.
- **Lab Management**: Publish and manage lab materials, including interactive notebooks and assets.
- **Article Publishing**: Write and publish articles to share knowledge with the community.
- **Submissions & Reviews**: Submit completed work and provide structured feedback on peer submissions.
- **Interactive Learning**: Engage with hands-on exercises and interactive tasks directly in the browser.

## 🚀 Getting Started

### Prerequisites

- [git](https://git-scm.com/)
- [Docker](https://www.docker.com/)

### Installation

1. Clone our project: `git clone https://github.com/IU-Capstone-Project-2025/open-labs-share.git`
2. Go into project folder on your system: `cd open-labs-share`
3. Copy `.env.example` to `.env` and fill in the values.
4. Start our app using Docker Compose with the **test** profile: `docker-compose --profile test up --build -d`
5. *For Ml features:* copy `ml/groq_keys.txt.example` to `ml/groq_keys.txt` and fill in the values.
6. *For Ml features:* run `docker-compose --profile ml up --build -d`
7. Check `http://localhost:80` for the frontend.

**For more detailed instructions, please refer to the [Local Testing Guide](LOCAL_TESTING_GUIDE.md).**

## 🛠️ Technology Stack

Our platform is built with a modern, microservices-based architecture. Here's a detailed breakdown of the technologies used across our services:

### Core Technologies

| Category | Technologies |
|---|---|
| **Frontend** | <img src="https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB" alt="React"/> <img src="https://img.shields.io/badge/vite-%23646CFF.svg?style=for-the-badge&logo=vite&logoColor=white" alt="Vite"/> <img src="https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white" alt="Tailwind CSS"/> <img src="https://img.shields.io/badge/PostCSS-DD3A0A?style=for-the-badge&logo=postcss&logoColor=white" alt="PostCSS"/> |
| **Backend** | <img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/> <img src="https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54" alt="Python"/> <img src="https://img.shields.io/badge/go-%2300ADD8.svg?style=for-the-badge&logo=go&logoColor=white" alt="Go"/> <img src="https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot"/> <img src="https://img.shields.io/badge/gRPC-00ADD8?style=for-the-badge&logo=grpc&logoColor=white" alt="gRPC"/> |
| **Databases** | <img src="https://img.shields.io/badge/postgresql-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/> <img src="https://img.shields.io/badge/MongoDB-%234ea94b.svg?style=for-the-badge&logo=mongodb&logoColor=white" alt="MongoDB"/> <img src="https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"/> |
| **Storage** | <img src="https://img.shields.io/badge/MinIO-C30F19?style=for-the-badge&logo=minio&logoColor=white" alt="MinIO"/> |
| **AI/ML** | <img src="https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi" alt="FastAPI"/> <img src="https://img.shields.io/badge/LangChain-00863D?style=for-the-badge&logo=langchain" alt="LangChain"/> <img src="https://img.shields.io/badge/Qdrant-AC1F43?style=for-the-badge&logo=qdrant&logoColor=white" alt="Qdrant"/> <img src="https://img.shields.io/badge/Celery-3781d0?style=for-the-badge&logo=celery&logoColor=white" alt="Celery"/> |
| **Deployment** | <img src="https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/> <img src="https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions"/> <img src="https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white" alt="NGINX"/> <img src="https://img.shields.io/badge/HAProxy-2D65B2?style=for-the-badge&logo=haproxy&logoColor=white" alt="HAProxy"/> |

### Service-Specific Technologies

| Service | Technology Stack |
|---|---|
| **API Gateway** | `Java 21`, `Spring Boot 3`, `Spring Web`, `Spring AOP`, `gRPC`, `SpringDoc OpenAPI`, `Lombok`, `Gradle` |
| **Auth Service** | `Java 21`, `Spring Boot 3`, `Spring Security`, `JWT`, `Spring Data JPA`, `PostgreSQL`, `gRPC`, `Lombok`, `Gradle` |
| **Users Service** | `Java 21`, `Spring Boot 3`, `Spring Security`, `Spring Data JPA`, `PostgreSQL`, `gRPC`, `Lombok`, `Gradle` |
| **Articles Service** | `Python 3.12`, `gRPC`, `SQLAlchemy`, `PostgreSQL`, `MinIO`, `Docker` |
| **Labs Service** | `Python 3.12`, `gRPC`, `SQLAlchemy`, `PyMongo`, `PostgreSQL`, `MongoDB`, `MinIO`, `Docker` |
| **Feedback Service** | `Go 1.24`, `gRPC`, `PostgreSQL`, `MongoDB`, `MinIO` |
| **Marimo Manager** | `Java 21`, `Spring Boot 3`, `gRPC`, `PostgreSQL`, `MinIO` |
| **Marimo Executor** | `Python 3.12`, `gRPC` |
| **ML Service** | `Python 3.12`, `FastAPI`, `LangChain`, `LangGraph`, `Hugging Face`, `Groq`, `Qdrant`, `PostgreSQL`, `Celery` |

## 🔵🟢 Blue/Green Deployment

We use a blue/green deployment strategy to ensure zero-downtime releases. This is orchestrated using Docker Compose profiles and HAProxy.

- **Blue Environment:** The current live environment.
- **Green Environment:** The new version, deployed for testing.

To switch between environments, server updates the `ACTIVE_ENV` variable in the `haproxy/active_env` file and restarts the `haproxy` service.

You can check it now on <https://open-labs-share.online>

## 📖 Documentation

For more detailed information about the project, please refer to the documentation for each service:

### General Documentation

- [Contributing Guidelines](CONTRIBUTING.md) - How to contribute to the project
- [Services Overview](services/README.md) - High-level overview of all services

### Frontend Documentation

- [Frontend Documentation](frontend/README.md) - Main frontend setup and development guide
- [Frontend Pages Documentation](frontend/docs/pages.md) - Detailed documentation for each page

### Core Services Documentation

#### API Gateway

- [API Gateway Documentation](services/api-gateway/README.md) - Main documentation and setup
- [API Gateway API Documentation](services/api-gateway/API_README.md) - API endpoints and usage
- [API Gateway Deployment Guide](services/api-gateway/DEPLOY.md) - Production deployment guide

#### Authentication & Users

- [Auth Service Documentation](services/auth-service/AUTH_README.md) - Authentication and JWT management
- [Users Service Documentation](services/users-service/USERS_README.md) - User profiles and data management

#### Learning Platform

- [Labs Service Documentation](services/labs-service/LABS_README.md) - Lab assignments and submissions
- [Feedback Service Documentation](services/feedback-service/FEEDBACK_README.md) - Lab feedback and comments
- [Articles Service Documentation](services/articles-service/ARTICLES_README.md) - Articles and publications management

#### AI & Machine Learning

- [ML Service Documentation](ml/README.md) - AI assistant and machine learning features

#### Interactive Notebooks

- [Marimo Service Documentation](services/marimo-service/MARIMO_README.md) - Overview of Marimo integration
- [Marimo Manager Service Documentation](services/marimo-service/marimo-manager-service/MARIMO_MANAGER_README.md) - Session and notebook management
- [Marimo Executor Service Documentation](services/marimo-service/marimo-executor-service/MARIMO_EXECUTOR_README.md) - Notebook execution engine

## 🤝 Contributing

Please read [Contributing Guidelines](CONTRIBUTING.md) for details on our code of conduct and the process for submitting commits and pull requests.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
