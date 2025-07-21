import re
import typing as tp
from agents.auto_grading_agent.schemas import AutoGradingState


def summarize_feedbacks(state: AutoGradingState, generate_groq: tp.Callable) -> dict:
    response = generate_groq(state.summary_prompt)
    response.content = re.sub(r"<think>.*?</think>", "", response.content, flags=re.DOTALL).strip()

    return {
        "final_feedback": response
    }