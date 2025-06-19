from pydantic import BaseModel
from langgraph.graph import MessagesState
from langchain_core.documents import Document
import typing as tp

class RAGState(BaseModel):
    query: str
    docs: str = None
    msg_state: dict
    