from fastapi import FastAPI
from contextlib import asynccontextmanager
from agent.agent import HelperAgent


@asynccontextmanager
async def startup_events(app: FastAPI):
    app.state.agent = HelperAgent()
    yield

app = FastAPI(docs_url="/")
# app.include_router(router)
