#!/bin/bash
set -e

# Default to green environment
TARGET_ENV="green"
# Timeout for health checks in seconds
HEALTH_CHECK_TIMEOUT=300

# Get the list of BASE services to deploy (e.g., api-gateway, auth-service)
BASE_SERVICES=($@)
if [ ${#BASE_SERVICES[@]} -eq 0 ]; then
    # If no services are specified, get all services from docker-compose for the green profile and strip the -green suffix
    BASE_SERVICES=($(docker-compose --profile green config --services | sed 's/-green$//'))
fi

# Construct the full service names for docker-compose
SERVICES_TO_DEPLOY=()
for service in "${BASE_SERVICES[@]}"; do
    SERVICES_TO_DEPLOY+=("${service}-${TARGET_ENV}")
done

echo "Deploying services: ${SERVICES_TO_DEPLOY[@]} to $TARGET_ENV environment"

# For CI/CD, images are pulled. For local testing, this step is skipped.
# To skip, run with LOCAL_TESTING=true (e.g., LOCAL_TESTING=true ./scripts/deploy_green.sh)
if [ "$LOCAL_TESTING" != "true" ]; then
    echo "Pulling latest images from registry..."
    docker-compose --profile $TARGET_ENV pull ${SERVICES_TO_DEPLOY[@]}
else
    echo "LOCAL_TESTING is true, skipping image pull."
fi

# The --build flag will rebuild only the specified services
docker-compose --profile $TARGET_ENV up -d --no-deps --build ${SERVICES_TO_DEPLOY[@]}

# Health check loop
echo "Waiting for $TARGET_ENV environment to be healthy..."
SECONDS=0
while [ $SECONDS -lt $HEALTH_CHECK_TIMEOUT ]; do
    ALL_HEALTHY=true
    # # Now we iterate over the full service names
    # for service_name in "${SERVICES_TO_DEPLOY[@]}"; do
    #     # docker-compose ps -q <service_name> might return an empty string if the container is not found yet, so we guard it.
    #     container_id=$(docker-compose ps -q $service_name)
    #     if [ -z "$container_id" ]; then
    #         ALL_HEALTHY=false
    #         echo "Service $service_name is not running yet."
    #         break
    #     fi

    #     HEALTH_STATUS=$(docker inspect --format '{{.State.Health.Status}}' $container_id 2>/dev/null || echo "unhealthy")

    #     if [ "$HEALTH_STATUS" != "healthy" ]; then
    #         ALL_HEALTHY=false
    #         echo "Service $service_name is not healthy yet (Status: $HEALTH_STATUS)."
    #         break
    #     fi
    # done

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
