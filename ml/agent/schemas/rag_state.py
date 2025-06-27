from pydantic import BaseModel
from langgraph.graph import MessagesState
from langchain_core.documents import Document
import typing as tp

class RAGState(BaseModel):
    uuid: str
    assignment_id: str
    query: str
    docs: tp.Optional[str] = None
    msg_state: dict
    