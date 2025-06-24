from langchain_community.vectorstores import FAISS
from langchain_huggingface.embeddings import HuggingFaceEmbeddings
from langchain_community.llms import HuggingFacePipeline
from transformers import AutoTokenizer, AutoModelForCausalLM, pipeline
from langgraph.graph import START, END, StateGraph
from ml.agent.schemas.rag_state import RAGState
from ml.agent.nodes import retrieve, query_rag_llm, query_llm, route_rag_usage
from langgraph.checkpoint.postgres import AsyncPostgresSaver
from ml.agent.config import \
    RAG_DB_PATH, \
    EMBEDDING_MODEL_NAME, \
    LLM_MODEL_NAME, \
    DEVICE, \
    SCORE_THRESHOLD
from ml.rag_backend.config import POSTGRES_URL
import asyncpg



class HelperAgent:
    # TODO: add logging

    def __init__(self):
        self._load_vector_storage()
        self._load_llm()
        self._load_graph_builder()
        self._check_postgres()

    async def _check_postgres(self):
        try:
            conn = await asyncpg.connect(POSTGRES_URL)
            await conn.close()
            return True
        except Exception as e:
            print(f"Postgres connection failed: {e}")
            return False


    def _load_vector_storage(self) -> None:
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
                # "filter": {"assignment_id": "1"} TODO: add argument when retrieving
            }
        )

    def _load_llm(self):
        tokenizer = AutoTokenizer.from_pretrained(
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
            tokenizer=tokenizer,
            max_new_tokens=1024
        )

        self._llm = HuggingFacePipeline(pipeline=text_gen)

    def _load_graph_builder(self) -> None:
        self._graph_builder = StateGraph(RAGState)

        self._graph_builder.add_node("retrieve", retrieve)
        self._graph_builder.add_node("query_rag_llm", query_rag_llm)
        self._graph_builder.add_node("query_llm", query_llm)

        self._graph_builder.add_conditional_edges("retrieve", route_rag_usage)
        self._graph_builder.add_edge("query_rag_llm", END)
        self._graph_builder.add_edge("query_llm", END)

        self._graph_builder.set_entry_point("retrieve")

    async def async_prompt(self, input_state: RAGState, config: dict):
        async with AsyncPostgresSaver.from_conn_string(POSTGRES_URL) as saver:
            graph = self._graph_builder.compile(checkpointer=saver)
            response_state = await graph.ainvoke(input_state, config=config)
            return response_state