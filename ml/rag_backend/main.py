from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
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

# Add custom exception handler for validation errors
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error(f"Validation error on {request.method} {request.url}: {exc.errors()}")
    logger.error(f"Request body: {await request.body()}")
    return JSONResponse(
        status_code=422,
        content={"detail": exc.errors(), "body": str(await request.body())}
    )

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)
