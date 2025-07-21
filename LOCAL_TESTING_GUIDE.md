# Local Blue/Green Deployment Testing

This guide will help you test the full Blue/Green deployment cycle on your local machine using `test` Docker Compose profile.

## Step 1: Initial Setup

Before the first launch, you need to perform a few setup steps.

### 1. Install Docker Desktop

Make sure you have [Docker](https://www.docker.com/get-started/) installed and running, as it includes `docker` and `docker-compose`.

### 2. Create a `.env` file

In the root of the project, create a file named `.env`. It will store all your secrets and passwords. Docker Compose will automatically load these variables. Example `.env` file is provided in the `.env.example` file.

### 3. Create a `ml/groq_keys.txt` file

In the ML folder of the project, create a file named `groq_keys.txt`. It will store all your Groq API keys. Example `groq_keys.txt` file is provided in the `groq_keys.txt.example` file.

## Step 2: Cold Start (First Launch)

This is a "cold" start of your system, which is performed once before you begin working.

**Note: This is used also for testing Docker stability locally in even the next starts.**

1. Open a terminal in the project's root folder.
2. Run the command to start **only the "test"** environment and all shared services (databases, HAProxy, etc.):

    ```bash
    docker-compose --profile test up -d --build
    ```

3. Run the command to start **ML** environment and all ML-related services (can be used as addiion to "test" profile):

    ```bash
    docker-compose --profile ml up -d --build
    ```

4. **Launch Verification:**
    * **Frontend:** Open `http://localhost/` in your browser. You should see your interface.
    * **HAProxy Stats:** Open `http://localhost:8404/` and make sure that traffic (session) is active on the `test` backends.

## Step 3: Cleanup

1. Run the command to clean **test** environment and all shared services (databases, HAProxy, etc.):

    ```bash
    docker-compose --profile test down -v # --volumes (ARE IMPORTANT)
    ```

2. Run the command to clean **ML** environment and all ML-related services:

    ```bash
    docker-compose --profile ml down -v # --volumes (ARE IMPORTANT)
    ```

## Step 4.1: Full Cleanup (Local Testing)

**Important: This is used for testing Docker ONLY locally.**

Use `docker system prune -af` to remove all unused images and containers with its volumes.

## Step 4.2: Full Cleanup (Server Side)

**Important: This is used for testing Docker ONLY on the server side.**

When you are done with testing, this step will allow you to completely stop and remove everything that was launched.

1. Run the `cleanup.sh` script. It calls `docker-compose down`, which stops and removes all containers and networks created by `docker-compose`.

    ```bash
    ./scripts/cleanup.sh
    ```

2. If you also want to remove **all data** (database contents, files in MinIO), use the `--volumes` flag.

    ```bash
    ./scripts/cleanup.sh --volumes
    ```
