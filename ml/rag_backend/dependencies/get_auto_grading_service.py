from rag_backend.services import AutoGradingService
from rag_backend.repositories import MinioRepository
from fastapi import Request, Depends
from rag_backend.dependencies.get_minio_repository import get_minio_repository


def get_auto_grading_service(
        request: Request,
        minio: MinioRepository = Depends(get_minio_repository)
) -> AutoGradingService:
    return AutoGradingService(
        agent=request.app.state.auto_grading_agent,
        minio=minio
    )
