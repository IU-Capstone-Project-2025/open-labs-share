#!/bin/bash
set -e

echo "Starting ML Service..."

# Set working directory
cd /app/ml

# Wait for postgres to be ready
echo "Waiting for PostgreSQL to be ready..."
python3 -c "
import psycopg2
import os
import time
import sys

for i in range(30):
    try:
        conn = psycopg2.connect(
            host=os.getenv('POSTGRES_HOST', 'localhost'),
            port=os.getenv('POSTGRES_PORT', '5432'),
            database=os.getenv('POSTGRES_DB', 'chat_history_db'),
            user=os.getenv('POSTGRES_USER', 'postgres'),
            password=os.getenv('POSTGRES_PASSWORD', 'postgres')
        )
        conn.close()
        print('PostgreSQL is ready!')
        break
    except Exception as e:
        print(f'Waiting for PostgreSQL... ({i+1}/30)')
        time.sleep(2)
else:
    print('PostgreSQL connection timeout')
    sys.exit(1)
"

# Check if database setup is needed (check if tables exist)
echo "Checking if database setup is needed..."
python3 -c "
import psycopg2
import os
import sys

try:
    conn = psycopg2.connect(
        host=os.getenv('POSTGRES_HOST', 'localhost'),
        port=os.getenv('POSTGRES_PORT', '5432'),
        database=os.getenv('POSTGRES_DB', 'chat_history_db'),
        user=os.getenv('POSTGRES_USER', 'postgres'),
        password=os.getenv('POSTGRES_PASSWORD', 'postgres')
    )
    cursor = conn.cursor()
    cursor.execute(\"SELECT to_regclass('public.checkpoints')\")
    result = cursor.fetchone()
    conn.close()
    
    if result[0] is None:
        print('Database setup needed')
        sys.exit(1)
    else:
        print('Database already set up')
        sys.exit(0)
except Exception as e:
    print(f'Database setup needed (error: {e})')
    sys.exit(1)
" || {
    echo "Running database setup..."
    python3 rag_backend/utils/storage_setup.py
    echo "Database setup completed"
}

echo "Starting FastAPI server..."
exec python -m uvicorn rag_backend.main:app --host 0.0.0.0 --port ${SERVICE_PORT:-8081} 