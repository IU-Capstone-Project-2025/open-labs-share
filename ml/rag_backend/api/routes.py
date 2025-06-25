from fastapi import APIRouter, Form, Depends, HTTPException, Request, Response
from rag_backend.schemas import \
    AgentResponse,\
    AskRequest,\
    ChatHistoryRequest,\
    ChatHistory
from rag_backend.services import AskService, ChatHistoryService
from rag_backend.dependencies import get_ask_service, get_chat_history_service
import logging

logger = logging.getLogger(__name__)


router = APIRouter(tags=["Model"])


@router.post("/ask", response_model=AgentResponse)
async def ask(
    request: AskRequest,
    ask_service: AskService = Depends(get_ask_service)
):
    try:
        logger.info(f"Received ask request: uuid={request.uuid}, assignment_id={request.assignment_id}, content_length={len(request.content)}")
        result = await ask_service.ask(request)
        logger.info("Successfully processed ask request")
        return result
    except Exception as e:
        logger.error(f"Error processing ask request: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
    

@router.get("/get_chat_history", response_model=ChatHistory)
async def get_chat_history(
    uuid: str,
    assignment_id: str,
    chat_history_service: ChatHistoryService = Depends(get_chat_history_service)
):
    try:
        request = ChatHistoryRequest(uuid=uuid, assignment_id=assignment_id)
        return await chat_history_service.get_chat_history(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))