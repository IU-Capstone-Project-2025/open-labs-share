import os
from dotenv import load_dotenv

load_dotenv("agents/helper_agent/.env")

DEVICE = os.getenv("DEVICE", "cpu")
RAG_DB_PATH = os.getenv("RAG_DB_PATH")
SCORE_THRESHOLD = float(os.getenv("SCORE_THRESHOLD"))
EMBEDDING_MODEL_NAME = os.getenv("EMBEDDING_MODEL_NAME", "BAAI/bge-small-en-v1.5")
LLM_MODEL_NAME = os.getenv("LLM_MODEL_NAME", "Qwen/Qwen2.5-Coder-1.5B-Instruct")
PDF_DIR = os.getenv("PDF_DIR", "data/predator-pray-22/pdfs")
CODE_DIR = os.getenv("CODE_DIR", "data/predator-pray-22/code")