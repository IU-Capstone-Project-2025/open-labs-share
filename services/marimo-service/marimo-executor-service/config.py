import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # Service configuration
    GRPC_PORT = int(os.getenv('GRPC_PORT', '9095'))
    MAX_SESSIONS = int(os.getenv('MAX_SESSIONS', '100'))
    SESSION_TIMEOUT_MINUTES = int(os.getenv('SESSION_TIMEOUT_MINUTES', '240'))

    # MinIO configuration
    MINIO_ENDPOINT = os.getenv('MINIO_ENDPOINT', 'localhost:9000')
    MINIO_ACCESS_KEY = os.getenv('MINIO_ACCESS_KEY', 'minioadmin')
    MINIO_SECRET_KEY = os.getenv('MINIO_SECRET_KEY', 'minioadmin')
    MINIO_BUCKET = os.getenv('MINIO_BUCKET', 'marimo')
    MINIO_SECURE = os.getenv('MINIO_SECURE', 'false').lower() == 'true'

    # Security configuration
    ALLOWED_IMPORTS = {
        'numpy', 'pandas', 'matplotlib', 'plotly',
        'marimo', 'math', 'statistics', 'random',
        'datetime', 'json', 'scipy', 'seaborn'
    }
    
    BLOCKED_MODULES = {
        'os', 'subprocess', 'sys', 'socket',
        'urllib', 'requests', 'http', 'builtins'
    }

    # Performance configuration
    MAX_CODE_LENGTH = int(os.getenv('MAX_CODE_LENGTH', '25000'))
    WEBGL_THRESHOLD = int(os.getenv('WEBGL_THRESHOLD', '1000'))
    MAX_OUTPUT_SIZE_MB = int(os.getenv('MAX_OUTPUT_SIZE_MB', '50'))
