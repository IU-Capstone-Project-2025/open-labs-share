# Import downloaded modules
from dotenv import load_dotenv

# Import built-in modules
import os

# Check if there's .env file
dotenv_path = os.path.join(os.path.dirname(__file__), '.env')
if os.path.exists(dotenv_path):
    # Load environment variables
    load_dotenv(dotenv_path)

class Config:
    # General config
    SERVICE_HOST = os.getenv("SERVICE_HOST", "localhost")
    SERVICE_PORT = os.getenv("SERVICE_PORT", "50051")

    # MinIO config
    MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
    MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
    MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin")
    MINIO_ROOT_USER = os.getenv("MINIO_ROOT_USER", "minioadmin")
    MINIO_ROOT_PASSWORD = os.getenv("MINIO_ROOT_PASSWORD", "minioadmin")

    # PostgreSQL config
    POSTGRESQL_USER = os.getenv("POSTGRESQL_USER", "postgres")
    POSTGRESQL_PASSWORD = os.getenv("POSTGRESQL_PASSWORD", "postgres")
    POSTGRESQL_HOST = os.getenv("POSTGRESQL_HOST", "postgres")
    POSTGRESQL_PORT = os.getenv("POSTGRESQL_PORT", "5433")
    POSTGRESQL_NAME = os.getenv("POSTGRESQL_NAME", "labs")

    # MongoDB config
    MONGODB_USER = os.getenv("MONGODB_USER", "mongo")
    MONGODB_PASSWORD = os.getenv("MONGODB_PASSWORD", "mongo")
    MONGODB_HOST = os.getenv("MONGODB_HOST", "mongodb")
    MONGODB_PORT = os.getenv("MONGODB_PORT", "27017")
    MONGODB_NAME = os.getenv("MONGODB_NAME", "mongodb")