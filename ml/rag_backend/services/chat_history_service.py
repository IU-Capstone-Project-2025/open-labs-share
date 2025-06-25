from agent.agent import HelperAgent
from rag_backend.schemas import ChatHistoryRequest, ChatHistory
from langchain_core.runnables import RunnableConfig
from agent.schemas import RAGState
from agent.prompts import SYSTEM_PROMPT
from langchain_core.messages import BaseMessage
import logging
import typing as tp


logger = logging.getLogger(__name__)

class ChatHistoryService:
    def __init__(self, agent: HelperAgent):
        self._agent = agent

    def _postprocess(
            self,
            request: ChatHistoryRequest,
            messages: tp.List[BaseMessage]
    ) -> ChatHistory:
        try:
            return ChatHistory(
                uuid=request.uuid,
                assignment_id=request.assignment_id,
                history=messages
            )
        except Exception as e:
            logger.error(f"Postprocess error: {e}")
            raise

    async def get_chat_history(self, request: ChatHistoryRequest) -> ChatHistory:
        try:
            config = {
                "configurable": {
                    "thread_id": f"{request.assignment_id}_{request.uuid}"
                }
            }

            last_state = await self._agent.get_last_state(config=config)
            logger.info("Last state retrieved")

            if not last_state.values:
                return self._postprocess(
                    request,
                    []
                )
            else:
                return self._postprocess(
                    request,
                    last_state.values["msg_state"]['messages']
                )
        except Exception as e:
            logger.error(f"Error getting chat history: {e}")
            raise

        