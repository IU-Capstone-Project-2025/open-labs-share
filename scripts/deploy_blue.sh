#!/bin/bash
set -e

# Target the blue environment
TARGET_ENV="blue"
# Timeout for health checks in seconds
HEALTH_CHECK_TIMEOUT=300

# Get the list of BASE services to deploy (e.g., api-gateway, auth-service)
BASE_SERVICES=($@)
if [ ${#BASE_SERVICES[@]} -eq 0 ]; then
    # If no services are specified, get all services from docker-compose for the blue profile and strip the -blue suffix
    BASE_SERVICES=($(docker-compose --profile blue config --services | sed 's/-blue$//'))
fi

# Construct the full service names for docker-compose
SERVICES_TO_DEPLOY=()
for service in "${BASE_SERVICES[@]}"; do
    SERVICES_TO_DEPLOY+=("${service}-${TARGET_ENV}")
done

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
        container_id=$(docker-compose --profile $TARGET_ENV ps -q $service_name)
        if [ -z "$container_id" ]; then
            ALL_HEALTHY=false
            echo "Service $service_name is not running yet."
            break
        fi

        HEALTH_STATUS=$(docker inspect --format '{{.State.Health.Status}}' $container_id 2>/dev/null || echo "unhealthy")

        if [ "$HEALTH_STATUS" != "healthy" ]; then
            ALL_HEALTHY=false
            echo "Service $service_name is not healthy yet (Status: $HEALTH_STATUS)."
            break
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