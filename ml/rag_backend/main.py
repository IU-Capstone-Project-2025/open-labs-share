from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from agents.helper_agent.agent import HelperAgent
from agents.auto_grading_agent.agent import AutoGradingAgent
from rag_backend.utils import check_postgres, setup_logging
from rag_backend.api.routes import router
from rag_backend.repositories import RedisRepository, MinioRepository, QdrantRepository
import logging

setup_logging()
logger = logging.getLogger(__name__)

@asynccontextmanager
async def startup_events(app: FastAPI):
    await check_postgres()
    app.state.qdrant_repository = QdrantRepository(
        minio_repo=MinioRepository()
    )
    app.state.agent = HelperAgent(
        qdrant_repo=app.state.qdrant_repository
    )
    logger.info("RAG Agent successfully loaded")
    app.state.auto_grading_agent = AutoGradingAgent()
    logger.info("Auto Grading Agent successfully loaded")
    app.state.redis_repository = RedisRepository(db=1)
    logger.info("Redis successfully loaded")
    yield

app = FastAPI(docs_url="/", lifespan=startup_events)

origins = ["*"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)
