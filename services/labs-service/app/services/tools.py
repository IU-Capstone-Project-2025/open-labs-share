# Import downloaded modules
import minio
from pymongo import MongoClient
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

# Import built-in modules
import logging

# Import project files
from config import Config

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)


class Tools:
    _minio_client = None
    _mongo_client = None
    _postgresql_engine = None
    _instance = None

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(Tools, cls).__new__(cls)
            cls._instance.logger = logging.getLogger(cls.__name__)
        return cls._instance

    def get_minio_client(self) -> minio.Minio:
        if self._minio_client is None:
            self.logger.info(f"Connection to MinIO at {Config.MINIO_ENDPOINT}")
            self._minio_client = minio.Minio(
                endpoint=Config.MINIO_ENDPOINT,
                access_key=Config.MINIO_ACCESS_KEY,
                secret_key=Config.MINIO_SECRET_KEY,
                secure=False
            )
        return self._minio_client

    def get_mongo_client(self) -> MongoClient:
        if self._mongo_client is None:
            # Include authentication database in connection string
            url = f"mongodb://{Config.MONGODB_USER}:{Config.MONGODB_PASSWORD}@{Config.MONGODB_HOST}:{Config.MONGODB_PORT}/{Config.MONGODB_NAME}?authSource=admin"
            
            self.logger.info(f"Connecting to MongoDB at {url}")
            self._mongo_client = MongoClient(url)
        return self._mongo_client

    def get_postgresql_engine(self):
        if self._postgresql_engine is None:
            user = Config.POSTGRESQL_USER
            password = Config.POSTGRESQL_PASSWORD
            host = Config.POSTGRESQL_HOST
            port = Config.POSTGRESQL_PORT
            db_name = Config.POSTGRESQL_NAME
            url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

            self.logger.info(f"Connecting to PostgreSQL at {url}")
            self._postgresql_engine = create_engine(url, echo=False)

            # Create tables if they don't exist
            from utils.models import Base
            Base.metadata.create_all(self._postgresql_engine)

        return self._postgresql_engine