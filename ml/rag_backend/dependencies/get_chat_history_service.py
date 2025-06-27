from rag_backend.services import ChatHistoryService
from fastapi import Request


def get_chat_history_service(request: Request) -> ChatHistoryService:
    return ChatHistoryService(agent=request.app.state.agent)