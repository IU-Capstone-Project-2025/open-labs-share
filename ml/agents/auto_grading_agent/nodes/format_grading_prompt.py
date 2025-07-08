from agents.auto_grading_agent.schemas import AutoGradingState
from agents.auto_grading_agent.prompts import GRADING_SYSTEM_PROMPT
from langchain_core.messages import SystemMessage, HumanMessage

def format_grading_prompt(state: AutoGradingState) -> dict:
    grading_prompts = []
    for code in state.submission_code:
        grading_prompts.append([
            SystemMessage(content=GRADING_SYSTEM_PROMPT),
            HumanMessage(content=f"Assignment: {state.assignment}\n\nStudent code: {code}")
        ])
    return {"grading_prompts": grading_prompts}