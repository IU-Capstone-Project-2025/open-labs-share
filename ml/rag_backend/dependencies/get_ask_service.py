from rag_backend.services import AskService
from fastapi import Request


def get_ask_service(request: Request) -> AskService:
    return AskService(agent=request.app.state.agent)