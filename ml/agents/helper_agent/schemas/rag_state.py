from langchain_core.documents import Document
import typing as tp
from agents.schemas import AgentState

class RAGState(AgentState):
    docs: tp.Optional[str] = None
    