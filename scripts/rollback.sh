#!/bin/bash
set -e

FAILED_ENV=$1
if [ -z "$FAILED_ENV" ]; then
    echo "Usage: $0 <failed_environment_color>"
    exit 1
fi

if [ "$FAILED_ENV" == "green" ]; then
    ROLLBACK_TO="blue"
else
    ROLLBACK_TO="green"
fi

echo "Rollback initiated. Failed environment: $FAILED_ENV"
echo "Switching traffic back to $ROLLBACK_TO"

# The switch_traffic script handles stopping the old (failed) environment
./scripts/switch_traffic.sh $ROLLBACK_TO

echo "Rollback complete. Traffic is now on $ROLLBACK_TO."
