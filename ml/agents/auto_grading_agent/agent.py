from langchain_core.messages import AIMessage
from langchain_core.runnables import RunnableConfig
from agents.base import BaseAgent
from .schemas import AutoGradingState
from .utils import GroqKeyManager
from langchain_groq import ChatGroq
import logging
from langchain_core.messages import BaseMessage
from langgraph.types import StateSnapshot
from groq import APIStatusError
import typing as tp
from langgraph.graph import END, StateGraph
from .nodes import (
    format_grading_prompt,
    format_summary_prompt,
    get_feedbacks,
    summarize_feedbacks,
)
from functools import partial


logger = logging.getLogger(__name__)

class AutoGradingAgent(BaseAgent):
    def __init__(self) -> None:
        self.model_name = "deepseek-r1-distill-llama-70b"
        self._key_manager = GroqKeyManager()

        self._load_llm()
        self._load_graph_builder()

    def _generate_groq(self, prompt: list[BaseMessage]) -> tp.Optional[BaseMessage]:
        llm = ChatGroq(
            model=self.model_name,
            api_key=self._key_manager.get_key(),
        )
        try:
            response = llm.invoke(prompt)
            return response
        except APIStatusError as e:
            if e.status_code == 413 and "TPD" in str(e):
                self._key_manager.switch_key()
                logger.info(
                    f"TPD error encountered, switching Groq API key {self._key_manager.idx}..."
                )
                return self._generate_groq(prompt)
            elif e.status_code == 413 and "TPM" in str(e):
                logger.error("TPM error encountered, request too large")
                raise RuntimeError("TPM error encountered, request too large")
            else:
                raise RuntimeError(f"Groq API error: {e.status_code} - {e.message}") from e

    def _load_llm(self) -> None:
        llm = ChatGroq(
            model=self.model_name,
            api_key=self._key_manager.get_key(),
        )

        try:
            llm.invoke("ping")
            logger.info("Gorq LLM is available")
            return None
        except Exception as e:
                self._key_manager.switch_key()
                logger.info(f"Key invalid, switched to next key {self._key_manager.idx}...")
                return self._load_llm()
        

    def _load_graph_builder(self) -> None:
        self._graph_builder = StateGraph(AutoGradingState)

        self._graph_builder.add_node("format_grading_prompt", format_grading_prompt)
        self._graph_builder.add_node(
            "get_feedbacks",
            partial(
                get_feedbacks,
                generate_groq=self._generate_groq
            )
        )
        self._graph_builder.add_node("format_summary_prompt", format_summary_prompt)
        self._graph_builder.add_node(
            "summarize_feedbacks",
            partial(
                summarize_feedbacks,
                generate_groq=self._generate_groq
            )
        )


        self._graph_builder.add_edge("format_grading_prompt", "get_feedbacks")
        self._graph_builder.add_edge("get_feedbacks", "format_summary_prompt")
        self._graph_builder.add_edge("format_summary_prompt", "summarize_feedbacks")
        self._graph_builder.add_edge("summarize_feedbacks", END)

        self._graph_builder.set_entry_point("format_grading_prompt")
        logger.info("Graph loaded successfully")


    def prompt(self, input_state: AutoGradingState, config: RunnableConfig) -> AIMessage: # type: ignore[override]
        graph = self._graph_builder.compile()
        response = graph.invoke(input_state, config=config)
        return response["final_feedback"]


    async def get_last_state(self, config: RunnableConfig) -> StateSnapshot:
        graph = self._graph_builder.compile()
        return await graph.aget_state(config=config)