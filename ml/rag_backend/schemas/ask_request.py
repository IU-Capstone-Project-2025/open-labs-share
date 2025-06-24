from pydantic import BaseModel


class AskRequest(BaseModel):
    uuid: str
    assignment_id: str
    content: str