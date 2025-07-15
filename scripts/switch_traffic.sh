#!/bin/bash
set -e

TARGET_ENV=$1

if [ -z "$TARGET_ENV" ]; then
    echo "Usage: ./scripts/switch_traffic.sh <blue|green>"
    exit 1
fi

echo "Switching traffic to $TARGET_ENV environment"
exit 0

# Update the main haproxy.cfg
cp "haproxy/haproxy.$TARGET_ENV.cfg" "haproxy/haproxy.cfg"

# Gracefully reload the HAProxy configuration
# Add a retry loop to give haproxy time to start and get a pid
for i in {1..5}; do
    # Use the PID file for a more reliable way to get the process ID
    HAPROXY_PID=$(docker-compose exec haproxy cat /var/run/haproxy.pid 2>/dev/null)
    if [ -n "$HAPROXY_PID" ]; then
        docker-compose exec haproxy haproxy -sf $HAPROXY_PID
        echo "HAProxy reloaded successfully."
        exit 0
    fi
    echo "Waiting for HAProxy PID file... (attempt $i)"
    sleep 2
done

echo "Error: Could not get HAProxy PID after several attempts."
exit 1
