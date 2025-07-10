#!/bin/bash
set -e

TARGET_ENV=$1

if [ -z "$TARGET_ENV" ]; then
    echo "Usage: ./scripts/switch_traffic.sh <blue|green>"
    exit 1
fi

echo "Switching traffic to $TARGET_ENV environment"

# Update the main haproxy.cfg
cp "haproxy/haproxy.$TARGET_ENV.cfg" "haproxy/haproxy.cfg"

# Send a HUP signal to the haproxy container to gracefully reload the config
docker-compose kill -s HUP haproxy

echo "Traffic switched to $TARGET_ENV"
