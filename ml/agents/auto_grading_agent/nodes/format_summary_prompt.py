from agents.auto_grading_agent.schemas import AutoGradingState
from agents.auto_grading_agent.prompts import SUMMARY_SYSTEM_PROMPT
from langchain_core.messages import SystemMessage, HumanMessage

def format_summary_prompt(state: AutoGradingState) -> dict:
    prompt = []

    prompt.append(SystemMessage(
        content=SUMMARY_SYSTEM_PROMPT
    ))

    prompt.append(HumanMessage(
        content=f"Received feedbacks:\n\n" + "\n\n".join(state.feedbacks)
    ))

    return {
        "summary_prompt": prompt
    }
