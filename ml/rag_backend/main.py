from fastapi import FastAPI
from contextlib import asynccontextmanager
from agent.agent import HelperAgent
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
    yield

app = FastAPI(docs_url="/", lifespan=startup_events)
app.include_router(router)
