from pydantic import BaseModel

class AgentResponse(BaseModel):
    assignment_id: str
    content: str