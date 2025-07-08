import redis
from redis.typing import ResponseT
import os
import typing as tp

class RedisRepository:
    def __init__(self, db: int = 1):
        self.client = redis.Redis(
            host=os.getenv("REDIS_HOST", "ml-redis-broker"),
            port=int(os.getenv("REDIS_PORT", 6379)),
            db=db,
            decode_responses=True
        )
        
        try:
            self.client.ping()
        except redis.ConnectionError as e:
            raise RuntimeError("Could not connect to Redis") from e

    def make_key(self, uuid: str, assignment_id: str, submission_id: str) -> str:
        return f"uuid:{uuid}:lab_id{assignment_id}:sub_id{submission_id}"

    def save_mapping(self, uuid: str, assignment_id: str, submission_id: str, task_id: str):
        key = self.make_key(uuid, assignment_id, submission_id)
        self.client.set(key, task_id)

    def get_task_id(self, uuid: str, assignment_id: str, submission_id: str) -> tp.Optional[ResponseT]:
        key = self.make_key(uuid, assignment_id, submission_id)
        return self.client.get(key)