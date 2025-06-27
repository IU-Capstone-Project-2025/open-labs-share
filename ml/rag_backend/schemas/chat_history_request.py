from pydantic import BaseModel

class ChatHistoryRequest(BaseModel):
    uuid: str
    assignment_id: str