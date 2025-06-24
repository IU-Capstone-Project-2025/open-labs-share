from fastapi import FastAPI
from contextlib import asynccontextmanager


# @asynccontextmanager
# async def startup_events(app: FastAPI):
#     app.state.classifier = XGBoostModel()
#     app.state.credit_application_db = ShelveDB("credit_application")
#     app.state.user_data_db = ShelveDB("user_data")
#     app.state.model_report_db = ShelveDB("model_report")
#     yield

app = FastAPI(docs_url="/")
# app.include_router(router)
