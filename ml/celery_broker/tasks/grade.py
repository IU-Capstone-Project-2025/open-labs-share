from celery_broker.worker import celery_app
from rag_backend.services.auto_grading_service import AutoGradingService
from rag_backend.schemas import AutoGradingRequest
from agents.auto_grading_agent.agent import AutoGradingAgent
from rag_backend.repositories.minio_repository import MinioRepository
import httpx
import asyncio
import time

def get_service():
    agent = AutoGradingAgent()
    repo = MinioRepository()
    return AutoGradingService(agent, repo)

@celery_app.task
def grade_submission_task(request: dict):
    service = get_service()
    try:
        result = asyncio.run(service.grade(AutoGradingRequest(**request)))
        return result
    except Exception as e:
        print(f"Error during grading: {e}")
