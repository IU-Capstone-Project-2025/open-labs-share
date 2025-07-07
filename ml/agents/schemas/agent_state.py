from pydantic import BaseModel
import typing as tp

class AgentState(BaseModel):
    uuid: str
    assignment_id: str