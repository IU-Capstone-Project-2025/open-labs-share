from pydantic import BaseModel
from typing import List, Optional

class PromptMessage(BaseModel):
    role: str  # "system", "user", or "assistant"
    content: str
