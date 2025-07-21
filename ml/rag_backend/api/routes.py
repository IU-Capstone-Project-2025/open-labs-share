from fastapi import APIRouter, Form, Depends, HTTPException, Request, Response
from rag_backend.schemas import \
    AgentResponse,\
    AskRequest,\
    ChatHistoryRequest,\
    ChatHistory, \
    AutoGradingResponse, \
    AutoGradingRequest,\
    AutoGradingTaskResponse
from rag_backend.services import AskService, ChatHistoryService, TasksService
from rag_backend.dependencies import(
    get_ask_service,
    get_chat_history_service,
    get_tasks_service,
    get_qdrant_repository
)
from rag_backend.services.auto_grading_service import AutoGradingService
from celery_broker.tasks.grade import grade_submission_task
from rag_backend.repositories import QdrantRepository

router = APIRouter(tags=["Model"])


@router.get("/health")
async def health_check():
    return {"status": "ok"}


@router.post("/ask", response_model=AgentResponse)
async def ask(
    request: AskRequest,
    ask_service: AskService = Depends(get_ask_service)
):
    try:
        return await ask_service.ask(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    

@router.get("/get_chat_history", response_model=ChatHistory)
async def get_chat_history(
    request: ChatHistoryRequest = Depends(),
    chat_history_service: ChatHistoryService = Depends(get_chat_history_service)
):
    try:
        return await chat_history_service.get_chat_history(request)
    except ValueError as ve:
        raise HTTPException(status_code=404, detail=str(ve))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    

@router.post("/auto_grade_submission", response_model=AutoGradingTaskResponse)
async def auto_grade_submission(
    request: AutoGradingRequest,
    tasks_service: TasksService = Depends(get_tasks_service)
):
    try:
        result = grade_submission_task.delay( # type: ignore
            request.model_dump()
        )
        tasks_service.save_mapping(request, result.id)

        return AutoGradingTaskResponse(
            task_id=result.id,
            status=result.status
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    

@router.post("/")
async def webhook_listener(request: Request):
    data = await request.json()
    print("Webhook received:", data)
    return {"status": "received"}


@router.get("/get_auto_grade_result", response_model=AutoGradingResponse)
async def get_auto_grade_result(
    request: AutoGradingRequest = Depends(),
    tasks_service: TasksService = Depends(get_tasks_service)
):
    try:
        result = tasks_service.get_task_result(request)
        if not result:
            raise HTTPException(status_code=404, detail="Result not ready or not found")
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
@router.get("/get_auto_grade_status", response_model=AutoGradingTaskResponse)
async def get_auto_grade_status(
    request: AutoGradingRequest = Depends(),
    tasks_service: TasksService = Depends(get_tasks_service)
):
    try:
        response = tasks_service.get_task_status(request)
        if not response:
            raise HTTPException(status_code=404, detail="Result not found")
        return response
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    
@router.post("/index_assignment")
async def index_assignment(
    assignment_id: str = Form(...),
    qdrant_repo: QdrantRepository = Depends(get_qdrant_repository)
):
    try:
        qdrant_repo.index_assignment(assignment_id)
        return Response(status_code=204)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))