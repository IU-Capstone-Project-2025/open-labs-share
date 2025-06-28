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
    MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
    MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
    MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin")

    # DB config
    DB_USER = os.getenv("DB_USER", "postgres")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "password")
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_PORT = os.getenv("DB_PORT", "5432")
    DB_NAME = os.getenv("DB_NAME", "postgres")