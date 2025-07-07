from fastapi import APIRouter, Form, Depends, HTTPException, Request, Response
from rag_backend.schemas import \
    AgentResponse,\
    AskRequest,\
    ChatHistoryRequest,\
    ChatHistory, \
    AutoGradingResponse, \
    AutoGradingRequest
from rag_backend.services import AskService, ChatHistoryService
from rag_backend.dependencies import get_ask_service, get_chat_history_service, get_auto_grading_service
from rag_backend.services.auto_grading_service import AutoGradingService


router = APIRouter(tags=["Model"])


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
    

@router.post("/auto_grade_submission", response_model=AutoGradingResponse)
async def auto_grade_submission(
    request: AutoGradingRequest,
    auto_grading_service: AutoGradingService = Depends(get_auto_grading_service)
):
    try:
        # TODO asychronous call to the agent
        return await auto_grading_service.grade(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))