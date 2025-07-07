from pydantic import BaseModel

class AutoGradingRequest(BaseModel):
    uuid: str
    assignment_id: str
    submission_id: str