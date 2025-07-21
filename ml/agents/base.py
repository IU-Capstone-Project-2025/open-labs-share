from abc import ABC, abstractmethod
from agents.schemas import AgentState
from langchain_core.runnables import RunnableConfig
from langchain_core.messages import AIMessage
from langgraph.types import StateSnapshot


class BaseAgent(ABC):
    @abstractmethod
    def _load_llm(self) -> None:
        pass

    @abstractmethod
    def _load_graph_builder(self) -> None:
        pass

    @abstractmethod
    async def prompt(self, input_state: AgentState, config: RunnableConfig) -> AIMessage:
        pass

    @abstractmethod
    async def get_last_state(self, config: RunnableConfig) -> StateSnapshot:
        pass