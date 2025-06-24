import asyncpg
from rag_backend.config import POSTGRES_URL
import logging


logger = logging.getLogger(__name__)

async def check_postgres() -> bool:
    try:
        conn = await asyncpg.connect(POSTGRES_URL)
        await conn.close()
        logger.info("Postgres connection is alive")
        return True
    except Exception as e:
        logger.error(f"Postgres connection failed: {e}")
        raise RuntimeError("PostgreSQL not available") from e