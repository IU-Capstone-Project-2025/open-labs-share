import logging
import typing as tp
from agents.auto_grading_agent.agent import AutoGradingAgent
from agents.auto_grading_agent.schemas.auto_grading_state import AutoGradingState
from rag_backend.schemas import AutoGradingRequest, AutoGradingResponse
from rag_backend.repositories.minio_repository import MinioRepository
from langchain_core.runnables import RunnableConfig
from langchain_core.messages import AIMessage
from langchain_core.output_parsers import JsonOutputParser
import os
import re

logger = logging.getLogger(__name__)

class AutoGradingService:
    def __init__(self, agent: AutoGradingAgent, minio: MinioRepository):
        self._repository = minio
        self._agent = agent


    async def _preprocess(
            self,
            request: AutoGradingRequest,
            assignment_bytes: tp.List[tp.Tuple[str, bytes]],
            submission_bytes: tp.List[tp.Tuple[str, bytes]]
        ) -> tuple[AutoGradingState, RunnableConfig]:
        
        assignment = "\n\n".join(
            [file_content.decode("utf-8") for _, file_content in assignment_bytes]
        )
        submissions = [
            f"{filename.split(os.sep)[1]}\n\n" + file_content.decode("utf-8")
            for filename, file_content in submission_bytes
        ]

        state = AutoGradingState(
            uuid=request.uuid,
            assignment_id=request.assignment_id,
            assignment=assignment,
            submission_code=submissions
        )
        config: RunnableConfig = {
            "configurable": {
                "thread_id": f"{request.assignment_id}_{request.uuid}_{request.submission_id}"
            }
        }

        logger.info(
            f"Preprocessed request: {request.assignment_id}, {request.uuid}, {request.submission_id}"
        )


        return state, config
        

    async def _postprocess(
            self,
            model_response: AIMessage
    ) -> AutoGradingResponse:
        content = str(model_response.content)

        parser = JsonOutputParser(pydantic_object=AutoGradingResponse)

        return await parser.aparse(content)


    async def grade(self, request: AutoGradingRequest) -> AutoGradingResponse:
        submission_bytes = self._repository.get_submission(request.submission_id)
        if submission_bytes is None:
            raise ValueError(
                f"Submission with id {request.submission_id} not found"
            )

        assignment_bytes = self._repository.get_assignment(request.assignment_id)
        if assignment_bytes is None:
            raise ValueError(
                f"Assignment with id {request.assignment_id} not found"
            )
        
        state, config = await self._preprocess(
            request=request,
            assignment_bytes=assignment_bytes,
            submission_bytes=submission_bytes
        )

        try:
            response = self._agent.prompt(state, config)
        except Exception as e:
            logger.error(f"Error during auto grading: {e}")
            raise RuntimeError("Auto grading failed") from e
        
        logger.info(
            f"Auto grading completed for {request.assignment_id}, {request.uuid}, {request.submission_id}"
        )


        return await self._postprocess(
            model_response=response
        )

    