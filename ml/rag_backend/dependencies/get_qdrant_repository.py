from fastapi import Request
from rag_backend.repositories import QdrantRepository

def get_qdrant_repository(request: Request) -> QdrantRepository:
    return request.app.state.qdrant_repository
