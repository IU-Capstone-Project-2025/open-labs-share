#!/bin/bash
set -e

NEW_COLOR=$1
if [ -z "$NEW_COLOR" ]; then
    echo "Usage: $0 <new_color>"
    exit 1
fi

# Determine the old color
if [ "$NEW_COLOR" == "green" ]; then
    OLD_COLOR="blue"
else
    OLD_COLOR="green"
fi

echo "Switching traffic from $OLD_COLOR to $NEW_COLOR"

# Enable new color servers and disable old color servers via HAProxy stats socket
echo "Enabling $NEW_COLOR servers..."
docker-compose exec haproxy sh -c "echo 'enable server frontend/frontend-${NEW_COLOR}-server' | socat stdio /tmp/haproxy.sock"
docker-compose exec haproxy sh -c "echo 'enable server api_gateway/api-gateway-${NEW_COLOR}-server' | socat stdio /tmp/haproxy.sock"
docker-compose exec haproxy sh -c "echo 'enable server auth_service/auth-service-${NEW_COLOR}-server' | socat stdio /tmp/haproxy.sock"
docker-compose exec haproxy sh -c "echo 'enable server ml_service/ml-service-${NEW_COLOR}-server' | socat stdio /tmp/haproxy.sock"

echo "Disabling $OLD_COLOR servers..."
docker-compose exec haproxy sh -c "echo 'disable server frontend/frontend-${OLD_COLOR}-server' | socat stdio /tmp/haproxy.sock"
docker-compose exec haproxy sh -c "echo 'disable server api_gateway/api-gateway-${OLD_COLOR}-server' | socat stdio /tmp/haproxy.sock"
docker-compose exec haproxy sh -c "echo 'disable server auth_service/auth-service-${OLD_COLOR}-server' | socat stdio /tmp/haproxy.sock"
docker-compose exec haproxy sh -c "echo 'disable server ml_service/ml-service-${OLD_COLOR}-server' | socat stdio /tmp/haproxy.sock"

echo "Traffic switched to $NEW_COLOR"

# Stop the old environment
echo "Stopping old environment: $OLD_COLOR"
docker-compose --profile $OLD_COLOR stop

echo "Old environment $OLD_COLOR stopped."
