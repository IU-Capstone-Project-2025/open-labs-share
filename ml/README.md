# **Setup guide**
- Create agent .env file in ``agent/.env``
- Fill file with following parameters:
```
DEVICE="cuda"
RAG_DB_PATH="faiss"
SCORE_THRESHOLD="1.0"
EMBEDDING_MODEL_NAME="BAAI/bge-small-en-v1.5"
LLM_MODEL_NAME="Qwen/Qwen2.5-Coder-1.5B-Instruct"
PDF_DIR="data/predator-pray-22/pdfs"
CODE_DIR="data/predator-pray-22/code"
```
- Create server .env file in ``rag_backend/.env``
- Fill file with following parameters:
```
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=chat_history_db
```
- Build docker container with postgres
```
sudo docker run --name chat-postgres --env-file ml/.env -v pgdata:/var/lib/postgresql/data -p 5432:5432 -d postgres
```
- Run `rag_backend/utils/storage_setup` script if it is your first app run
- Run fastapi server: `uvicorn rag_backend.main:app --host 0.0.0.0 --port 8081 `

# Handlers documentation
## **/ask** `POST` 

`Content-Type: application/json`

Request Model:
```
class AskRequest(BaseModel):
    uuid: str
    assignment_id: str
    content: str
```

Response Model:
```
class AgentResponse(BaseModel):
    assignment_id: str
    content: str
```

## **/get_chat_history** `GET`

`Content-Type: application/json`

Request Model:
```
class ChatHistoryRequest(BaseModel):
    uuid: str
    assignment_id: str
```

Response Model:
```
class ChatHistory(BaseModel):
    uuid: str
    assignment_id: str
    history: List[BaseMessage] OR Empty List
```

[BaseMessage docs](https://python.langchain.com/api_reference/core/messages/langchain_core.messages.base.BaseMessage.html)