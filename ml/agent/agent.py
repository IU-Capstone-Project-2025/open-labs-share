from langchain_community.vectorstores import FAISS
from langchain_huggingface.embeddings import HuggingFaceEmbeddings
from langchain_huggingface.llms import HuggingFacePipeline
from transformers import AutoTokenizer, AutoModelForCausalLM, pipeline
from langchain_core.runnables import RunnableConfig
from langgraph.graph import StateGraph
from typing import Any
from agent.schemas.rag_state import RAGState
from langchain_core.messages import AIMessage
from functools import partial
from agent.nodes import retrieve, query_rag_llm, query_llm, route_rag_usage
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from agent.config import \
    RAG_DB_PATH, \
    EMBEDDING_MODEL_NAME, \
    LLM_MODEL_NAME, \
    DEVICE, \
    SCORE_THRESHOLD
from rag_backend.config import POSTGRES_URL
import logging


logger = logging.getLogger(__name__)

class HelperAgent:
    def __init__(self):
        self._load_vector_storage()
        self._load_llm()
        self._load_graph_builder()

    def _load_vector_storage(self) -> None:
        try:
            self._embedding_model = HuggingFaceEmbeddings(
                model_name=EMBEDDING_MODEL_NAME,
                model_kwargs={"device": DEVICE},
                encode_kwargs={"normalize_embeddings": True}
            )
            self._db = FAISS.load_local(
                RAG_DB_PATH,
                self._embedding_model,
                allow_dangerous_deserialization=True
            )
            self._retriever = self._db.as_retriever(
                search_type="similarity",
                k=3,
                search_kwargs={
                    "score_threshold": SCORE_THRESHOLD,
                }
            )

            logger.info("Vector storage loaded successfully")
        except Exception as e:
            logger.error(f"Vector storage load failed: {e}")
            raise RuntimeError("Vector storage load failed") from e
        

    def _load_llm(self):
        try:
            self._tokenizer = AutoTokenizer.from_pretrained(
                LLM_MODEL_NAME,
                trust_remote_code=True
            )
            model = AutoModelForCausalLM.from_pretrained(
                LLM_MODEL_NAME,
                trust_remote_code=True,
                device_map=DEVICE
            )
            text_gen = pipeline(
                "text-generation",
                model=model,
                tokenizer=self._tokenizer,
                max_new_tokens=1024
            )

            self._llm = HuggingFacePipeline(pipeline=text_gen)
            logger.info("LLM loaded successfully")
        except Exception as e:
            logger.error(f"LLM load failed: {e}")

    def _load_graph_builder(self) -> None:
        try:
            self._graph_builder = StateGraph(RAGState)

            self._graph_builder.add_node(
                "retrieve",
                partial(
                    retrieve,
                    retriever=self._retriever
                )
            )
            self._graph_builder.add_node(
                "query_rag_llm",
                partial(
                    query_rag_llm,
                    llm=self._llm,
                    tokenizer=self._tokenizer
                )
            )
            self._graph_builder.add_node(
                "query_llm",
                partial(
                    query_llm,
                    llm=self._llm,
                    tokenizer=self._tokenizer
                )
            )

            self._graph_builder.add_conditional_edges("retrieve", route_rag_usage)
            self._graph_builder.add_edge("query_rag_llm", "__end__")
            self._graph_builder.add_edge("query_llm", "__end__")

            self._graph_builder.set_entry_point("retrieve")
            logger.info("Graph loaded successfully")
        except Exception as e:
            logger.error(f"Graph load failed: {e}")

    async def get_last_state(self, config: RunnableConfig) -> Any:
        async with AsyncPostgresSaver.from_conn_string(POSTGRES_URL) as saver:
            graph = self._graph_builder.compile(checkpointer=saver)
            return await graph.aget_state(config=config)

    async def prompt(self, input_state: RAGState, config: RunnableConfig) -> AIMessage:
        async with AsyncPostgresSaver.from_conn_string(POSTGRES_URL) as saver:
            graph = self._graph_builder.compile(checkpointer=saver)
            response_state = await graph.ainvoke(input_state, config=config)
            return response_state['msg_state']["messages"][-1]