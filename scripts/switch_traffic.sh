#!/bin/bash
set -e

NEW_COLOR=$1
if [ -z "$NEW_COLOR" ]; then
    echo "Usage: $0 <blue|green>"
    exit 1
fi

if [ "$NEW_COLOR" != "blue" ] && [ "$NEW_COLOR" != "green" ]; then
    echo "Invalid color: $NEW_COLOR. Must be 'blue' or 'green'."
    exit 1
fi

OLD_COLOR=$(cat haproxy/active_env)

echo "Switching traffic from $OLD_COLOR to $NEW_COLOR"

# Copy the new configuration and reload HAProxy
cp "haproxy/haproxy.${NEW_COLOR}.cfg" "haproxy/haproxy.cfg"
docker-compose exec haproxy haproxy -sf $(cat /var/run/haproxy.pid)

# Update the active environment state
echo "$NEW_COLOR" > haproxy/active_env

echo "Traffic switched to $NEW_COLOR"

# Stop the old environment's services
if [ -n "$OLD_COLOR" ] && [ "$OLD_COLOR" != "$NEW_COLOR" ]; then
    echo "Stopping old environment: $OLD_COLOR"
    SERVICES_TO_STOP=($(docker-compose --profile "$OLD_COLOR" config --services))
    if [ ${#SERVICES_TO_STOP[@]} -gt 0 ]; then
        docker-compose --profile "$OLD_COLOR" stop
        echo "Old environment $OLD_COLOR stopped."
    else
        echo "No services found for the old environment: $OLD_COLOR. Nothing to stop."
    fi
fi
