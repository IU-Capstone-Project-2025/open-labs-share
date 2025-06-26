from langgraph.checkpoint.postgres import PostgresSaver
from dotenv import load_dotenv
import os

# Only load .env file if it exists (for local development)
env_file_path = "rag_backend/.env"
if os.path.exists(env_file_path):
    load_dotenv(env_file_path)

PG_USER = os.getenv("POSTGRES_USER", "postgres")
PG_PASS = os.getenv("POSTGRES_PASSWORD", "postgres")
PG_HOST = os.getenv("POSTGRES_HOST", "localhost")
PG_PORT = os.getenv("POSTGRES_PORT", "5432")
PG_DB   = os.getenv("POSTGRES_DB", "chat_history_db")

POSTGRES_URL = f"postgresql://{PG_USER}:{PG_PASS}@{PG_HOST}:{PG_PORT}/{PG_DB}?sslmode=disable"

with PostgresSaver.from_conn_string(POSTGRES_URL) as saver:
    saver.setup()

print("Pg storage is set up")