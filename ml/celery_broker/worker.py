from celery import Celery
import os

celery_app = Celery(
    "auto_grading_broker",
    broker=os.getenv("REDIS_BROKER_URL"),
    backend=os.getenv("REDIS_BROKER_URL")
)

import celery_broker.tasks.grade