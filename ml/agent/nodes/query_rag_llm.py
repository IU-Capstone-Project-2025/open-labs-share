from ml.agent.schemas.rag_state import RAGState
from langchain_core.messages import HumanMessage, AIMessage
from langgraph.graph import MessagesState
from langchain_core.language_models import BaseLanguageModel
from ml.agent.utils import format_model_response, format_prompt
from transformers import PreTrainedTokenizerBase


def query_rag_llm(
        state: RAGState,
        llm: BaseLanguageModel,
        tokenizer: PreTrainedTokenizerBase
    ) -> dict:

    messages = state.msg_state["messages"]
    
    prompt = format_prompt(
        tokenizer=tokenizer,
        user_message=state.query,
        chat_history=state.msg_state["messages"],
        context=state.docs
    )
    response = llm.invoke(prompt)

    new_messages = messages + [
        HumanMessage(content=state.query),
        AIMessage(content=format_model_response(response))
    ]
    
    return {
        "msg_state": MessagesState(
            thread_id=state.msg_state["thread_id"],
            messages=new_messages
        )
    }