from agents.helper_agent.agent import HelperAgent
from rag_backend.schemas import AskRequest, AgentResponse
from langchain_core.runnables import RunnableConfig
from agents.helper_agent.schemas import RAGState
from langgraph.graph import MessagesState
from agents.helper_agent.prompts import SYSTEM_PROMPT
from langchain_core.messages import SystemMessage, AIMessage
import logging
import typing as tp


logger = logging.getLogger(__name__)


class AskService:
    def __init__(self, agent: HelperAgent):
        self._agent = agent


    async def _preprocess(self, request: AskRequest) -> tp.Optional[tp.Tuple[RAGState, RunnableConfig]]:
        try:
            config: RunnableConfig = {
                "configurable": {
                    "thread_id": f"{request.assignment_id}_{request.uuid}"
                }
            }

            last_state = await self._agent.get_last_state(config=config)
            logger.info("Last state retrieved")

            if not last_state.values:
                input_state = RAGState(
                    uuid=request.uuid,
                    assignment_id=request.assignment_id,
                    query=request.content,
                    docs='',
                    msg_state=MessagesState(
                        thread_id=config["configurable"]["thread_id"],
                        messages=[
                            SystemMessage(content=SYSTEM_PROMPT)
                        ]
                    ) # type: ignore
                )
            else:
                input_state = RAGState(
                    uuid=request.uuid,
                    assignment_id=request.assignment_id,
                    query=request.content,
                    docs='',
                    msg_state=last_state.values["msg_state"]
                )

            return input_state, config
        except Exception as e:
            logger.error(f"Preprocessing error {e}")
    

    def _postprocess(self, request: AskRequest, response: AIMessage) -> tp.Optional[AgentResponse]:
        try:
            return AgentResponse(
                assignment_id=request.assignment_id,
                content=response.content
            )
        except Exception as e:
            logger.error(f"Postprocess error: {e}")


    async def ask(self, request: AskRequest) -> tp.Optional[AgentResponse]:
        input_state, config = await self._preprocess(request)
        logger.info("Last state restored")

        response_msg = await self._agent.prompt(
            input_state=input_state,
            config=config
        )
        logger.info("Model responded")

        return self._postprocess(request, response_msg)

