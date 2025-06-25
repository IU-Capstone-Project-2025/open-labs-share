from fastapi import APIRouter, Form, Depends, HTTPException, Request, Response
from rag_backend.schemas import AgentResponse, AskRequest
from rag_backend.services import AskService
from rag_backend.dependencies import get_ask_service


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