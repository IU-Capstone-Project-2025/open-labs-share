from agents.schemas import AgentState
from langchain_core.messages import BaseMessage, AIMessage
import typing as tp

class AutoGradingState(AgentState):
    assignment: str
    submission_code: list[str]
    feedbacks: tp.Optional[list[str]] = None
    grading_prompts: tp.Optional[list[list[BaseMessage]]] = None
    summary_prompt: tp.Optional[list[BaseMessage]] = None
    final_feedback: tp.Optional[AIMessage] = None
