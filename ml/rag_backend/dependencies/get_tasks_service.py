from fastapi import Request
from rag_backend.services import TasksService

def get_tasks_service(request: Request) -> TasksService:
    return TasksService(redis_repo=request.app.state.redis_repository)
