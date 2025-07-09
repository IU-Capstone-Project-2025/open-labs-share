from pydantic import BaseModel

class AutoGradingTaskResponse(BaseModel):
    task_id: str
    status: str