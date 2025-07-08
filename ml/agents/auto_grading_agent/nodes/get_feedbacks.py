from tqdm import tqdm
import typing as tp
import re
from agents.auto_grading_agent.schemas import AutoGradingState

def get_feedbacks(state: AutoGradingState, generate_groq: tp.Callable) -> dict:
    feedbacks = []
    for prompt in tqdm(state.grading_prompts):
        response = generate_groq(prompt)
        response = re.sub(r"<think>.*?</think>", "", response.content, flags=re.DOTALL).strip()
        feedbacks.append(response)

    return {
        "feedbacks": feedbacks
    }