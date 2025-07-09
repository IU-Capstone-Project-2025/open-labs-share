# Local Blue/Green Deployment Testing

This guide will help you test the full Blue/Green deployment cycle on your local machine using `test` Docker Compose profile.

## Step 1: Initial Setup

Before the first launch, you need to perform a few setup steps.

### 1. Install Docker Desktop
Make sure you have [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running, as it includes `docker` and `docker-compose`.

### 2. Create a `.env` file
In the root of the project, create a file named `.env`. It will store all your secrets and passwords. Docker Compose will automatically load these variables. Example `.env` file is provided in the `.env.example` file.

## Step 2: Cold Start (First Launch)

This is a "cold" start of your system, which is performed once before you begin working.

**Note: This is used also for testing Docker stability locally in even the next starts.**

1.  Open a terminal in the project's root folder.
2.  Run the command to start **only the "test"** environment and all shared services (databases, HAProxy, etc.):
    ```bash
    docker-compose --profile test up -d --build
    ```
    This command will build all the necessary Docker images for the first time (this may take a while) and run the containers in the background.

3.  **Launch Verification:**
    *   **Frontend:** Open `http://localhost/` in your browser. You should see your interface.
    *   **HAProxy Stats:** Open `http://localhost:8404/` and make sure that traffic (session) is active on the `blue` backends.

## Step 3.1: Full Cleanup (Local Testing)

**Important: This is used for testing Docker ONLY locally.**

Use `docker system prune -af` to remove all unused images and containers with its volumes.

## Step 3.2: Full Cleanup (Server Side)

**Important: This is used for testing Docker ONLY on the server side.**

When you are done with testing, this step will allow you to completely stop and remove everything that was launched.

1.  Run the `cleanup.sh` script. It calls `docker-compose down`, which stops and removes all containers and networks created by `docker-compose`.

    ```bash
    ./scripts/cleanup.sh
    ```

2.  If you also want to remove **all data** (database contents, files in MinIO), use the `--volumes` flag.

    ```bash
    ./scripts/cleanup.sh --volumes
    ``` 