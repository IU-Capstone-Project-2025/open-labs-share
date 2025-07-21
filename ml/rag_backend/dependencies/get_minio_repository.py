from fastapi import Request
from rag_backend.repositories import MinioRepository

def get_minio_repository(request: Request) -> MinioRepository:
    return MinioRepository()
