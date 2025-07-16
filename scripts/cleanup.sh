#!/bin/bash
set -e

# Default to not removing volumes
REMOVE_VOLUMES=""
if [ "$1" == "--volumes" ]; then
    REMOVE_VOLUMES="-v"
    echo "Will remove volumes."
fi

echo "Stopping and removing all containers and networks..."
# This will stop and remove containers for all profiles
docker-compose down $REMOVE_VOLUMES

echo "Cleanup complete."
