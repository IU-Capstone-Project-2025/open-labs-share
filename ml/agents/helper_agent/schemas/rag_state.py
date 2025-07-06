from langchain_core.documents import Document
import typing as tp
from agents.schemas import AgentState

class RAGState(AgentState):
    query: str
    msg_state: dict
    docs: tp.Optional[str] = None
    