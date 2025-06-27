from pydantic import BaseModel, Field
import typing as tp
from langchain_core.messages import BaseMessage

class ChatHistory(BaseModel):
    uuid: str
    assignment_id: str
    history: tp.List[BaseMessage] = Field(default_factory=list)