from pydantic import BaseModel

class AutoGradingResponse(BaseModel):
    code_elegance_grade: int
    correctness_grade: int
    documentation_grade: int
    readability_grade: int

    code_elegance_feedback: str
    correctness_feedback: str
    documentation_feedback: str
    readability_feedback: str