#!/bin/bash
set -e

# Default to green environment
TARGET_ENV="green"
# Timeout for health checks in seconds
HEALTH_CHECK_TIMEOUT=300

# Get the list of BASE services to deploy (e.g., api-gateway, auth-service)
BASE_SERVICES_INPUT=($@)

SERVICES_TO_DEPLOY=()
if [ ${#BASE_SERVICES_INPUT[@]} -eq 0 ]; then
    # If no services are specified, get all services from docker-compose for the green profile.
    SERVICES_TO_DEPLOY=($(docker-compose --profile $TARGET_ENV config --services))
else
    # If services are specified, construct the full service names for docker-compose
    for service in "${BASE_SERVICES_INPUT[@]}"; do
        SERVICES_TO_DEPLOY+=("${service}-${TARGET_ENV}")
    done
fi

echo "Deploying services: ${SERVICES_TO_DEPLOY[@]} to $TARGET_ENV environment"

# For CI/CD, images are pulled. For local testing, this step is skipped.
if [ "$LOCAL_TESTING" != "true" ]; then
    echo "Pulling images with tag: ${IMAGE_TAG:-latest}..."
    docker-compose --profile $TARGET_ENV pull ${SERVICES_TO_DEPLOY[@]}
else
    echo "LOCAL_TESTING is true, skipping image pull."
fi

# The --build flag is removed to ensure we use the images from the registry.
IMAGE_TAG=${IMAGE_TAG:-latest} docker-compose --profile $TARGET_ENV up -d --force-recreate --no-deps ${SERVICES_TO_DEPLOY[@]}

# Health check loop
echo "Waiting for $TARGET_ENV environment to be healthy..."
SECONDS=0
while [ $SECONDS -lt $HEALTH_CHECK_TIMEOUT ]; do
    ALL_HEALTHY=true
    # Now we iterate over the full service names
    for service_name in "${SERVICES_TO_DEPLOY[@]}"; do
        # docker-compose ps -q <service_name> might return an empty string if the container is not found yet, so we guard it.
        container_id=$(docker-compose --profile $TARGET_ENV ps -q $service_name)
        if [ -z "$container_id" ]; then
            ALL_HEALTHY=false
            echo "Service $service_name is not running yet."
            break
        fi

        # Check if a health check is configured for the container
        HAS_HEALTH_CHECK=$(docker inspect --format '{{if .State.Health}}true{{else}}false{{end}}' $container_id)

        if [ "$HAS_HEALTH_CHECK" == "true" ]; then
            # If a health check is configured, check its status
            HEALTH_STATUS=$(docker inspect --format '{{.State.Health.Status}}' $container_id)
            if [ "$HEALTH_STATUS" != "healthy" ]; then
                ALL_HEALTHY=false
                echo "Service $service_name is not healthy yet (Status: $HEALTH_STATUS)."
                break
            fi
        else
            # If no health check is configured, just check if the container is running
            IS_RUNNING=$(docker inspect --format '{{.State.Running}}' $container_id)
            if [ "$IS_RUNNING" != "true" ]; then
                ALL_HEALTHY=false
                echo "Service $service_name is not running yet."
                break
            fi
        fi
    done

    if $ALL_HEALTHY; then
        echo "$TARGET_ENV environment is healthy."
        # If all services are healthy, switch traffic
        ./scripts/switch_traffic.sh $TARGET_ENV
        exit 0
    fi

    sleep 10
    SECONDS=$((SECONDS + 10))
done

echo "Timeout reached. $TARGET_ENV environment is not healthy."
# If timeout is reached, rollback
./scripts/rollback.sh $TARGET_ENV
exit 1

