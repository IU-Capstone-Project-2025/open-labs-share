from rag_backend.repositories.redis_repository import RedisRepository
from celery.result import AsyncResult
from celery_broker.worker import celery_app
from rag_backend.schemas import AutoGradingRequest, AutoGradingResponse, AutoGradingTaskResponse
import typing as tp

class TasksService:
    def __init__(self, redis_repo: RedisRepository):
        self.redis_repo = redis_repo

    def get_task_status(self, request: AutoGradingRequest) -> AutoGradingTaskResponse | None:
        task_id = self.redis_repo.get_task_id(
            request.uuid,
            request.assignment_id,
            request.submission_id
        )
        if not task_id:
            return None
        result = AsyncResult(task_id, app=celery_app)
        return AutoGradingTaskResponse(
            task_id=result.task_id,
            status=result.status
        )

    def get_task_result(self, request: AutoGradingRequest) -> tp.Optional[AutoGradingResponse]:
        task_id = self.redis_repo.get_task_id(
            request.uuid,
            request.assignment_id,
            request.submission_id
        )
        if not task_id:
            return None
        
        result = AsyncResult(task_id, app=celery_app)

        if not result:
            return None

        if result.ready():
            print(result.get())
            return AutoGradingResponse(**result.get())
        

        return None
    
    def save_mapping(self, request: AutoGradingRequest, task_id: str) -> None:
        self.redis_repo.save_mapping(
            request.uuid,
            request.assignment_id,
            request.submission_id,
            task_id
        )