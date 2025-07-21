# Import downloaded modules
from dotenv import load_dotenv

# Import built-in modules
import os
import logging

# Check if there's .env file
dotenv_path = os.path.join(os.path.dirname(__file__), '.env')
if os.path.exists(dotenv_path):
    # Load environment variables
    load_dotenv(dotenv_path)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

class Config:
    # General config
    SERVICE_HOST = os.getenv("SERVICE_HOST", "127.0.0.1")
    SERVICE_PORT = os.getenv("SERVICE_PORT", "50051")
    MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "127.0.0.1:9000")
    MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
    MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin")

    # DB config
    POSTGRES_USER = os.getenv("POSTGRES_USER", "postgres")
    POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "password")
    POSTGRES_HOST = os.getenv("POSTGRES_HOST", "127.0.0.1")
    POSTGRES_PORT = os.getenv("POSTGRES_PORT", "5432")
    POSTGRES_NAME = os.getenv("POSTGRES_NAME", "postgres")

for key, value in Config.__dict__.items():
    if key.startswith("__"):
        continue
    logger.info(f"{key}: {value}")

