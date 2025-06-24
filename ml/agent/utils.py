from langchain_core.messages import BaseMessage
from ml.agent.schemas.prompt_message import PromptMessage
from transformers import PreTrainedTokenizerBase
import typing as tp
import re

def format_prompt(
        tokenizer: PreTrainedTokenizerBase,
        user_message: str, 
        chat_history: tp.List[BaseMessage],
        context: str = None,
    ) -> str:
    '''
    Formats prompt for llm
    '''

    history = []
    for message in chat_history[:-1]:
        if message.type == "human":
            role = "user"
        elif message.type == "ai":
            role = "assistant"
        elif message.type == "system":
            role = "system"

        history.append(PromptMessage(
            role=role,
            content=message.content
        ))

    if context:
        history.append(PromptMessage(
            role="system",
            content=context
        ))

    history.append(PromptMessage(
        role="user",
        content=user_message
    ))


    return tokenizer.apply_chat_template(
        history,
        tokenize=False,
        add_generation_prompt=True
    )


def format_model_response(response: str):
    matches = list(re.finditer(r"<\|im_start\|>assistant", response))
    if not matches:
        return response.strip()
    last = matches[-1].start()

    return response[last + len("<|im_start|>assistant"):].strip()