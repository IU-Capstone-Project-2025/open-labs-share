from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from agents.helper_agent.agent import HelperAgent
from agents.auto_grading_agent.agent import AutoGradingAgent
from rag_backend.utils import check_postgres, setup_logging
from rag_backend.api.routes import router
import logging

setup_logging()
logger = logging.getLogger(__name__)

@asynccontextmanager
async def startup_events(app: FastAPI):
    await check_postgres()
    app.state.agent = HelperAgent()
    logger.info("RAG Agent successfully loaded")
    app.state.auto_grading_agent = AutoGradingAgent()
    logger.info("Auto Grading Agent successfully loaded")
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
