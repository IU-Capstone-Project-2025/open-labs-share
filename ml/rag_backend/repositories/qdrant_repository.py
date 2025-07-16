from qdrant_client import QdrantClient, models
from .minio_repository import MinioRepository
from langchain.text_splitter import MarkdownTextSplitter
from langchain_huggingface.embeddings import HuggingFaceEmbeddings
import os
import logging
import uuid

logger = logging.getLogger(__name__)


class QdrantRepository:
    def __init__(self, minio_repo: MinioRepository):
        self._qdrant = QdrantClient(
            host=os.getenv("QDRANT_HOST"),
            port=int(os.getenv("QDRANT_PORT", 6333)),
        )
        self.collection_name = "labs_collection"
        self._minio_repo = minio_repo
        self._splitter = MarkdownTextSplitter(
            chunk_size=5000,
            chunk_overlap=200
        )
        self._embedding_model = HuggingFaceEmbeddings(
            model_name=os.getenv("EMBEDDING_MODEL_NAME", "BAAI/bge-small-en-v1.5"),
            model_kwargs={"device": os.getenv("DEVICE", "cpu")},
            encode_kwargs={"normalize_embeddings": True}
        )

        if self.collection_name not in [c.name for c in self._qdrant.get_collections().collections]:
            self._qdrant.create_collection(
                collection_name=self.collection_name,
                vectors_config=models.VectorParams(size=384, distance=models.Distance.COSINE)
            )

        logger.info("Qdrant repository initialized successfully")

    def index_assignment(self, assignment_id: str):
        assignment_bytes = self._minio_repo.get_assignment(assignment_id)
        if assignment_bytes is None:
            raise ValueError(
                f"Assignment with id {assignment_id} not found"
            )
        
        assignment = "\n\n".join(
            [file_content.decode("utf-8") for _, file_content in assignment_bytes]
        )

        chunks = self._splitter.split_text(assignment)
        embeddings = self._embedding_model.embed_documents(chunks)

        points = []
        for idx, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
            payload = {
                "metadata": {
                    "assignment_id": assignment_id,
                    "chunk_id": idx,
                },
                "text": chunk  
            }
            points.append(
                models.PointStruct(
                    id=str(uuid.uuid4()),
                    vector=embedding,
                    payload=payload
                )
            )

        self._qdrant.upsert(
            collection_name=self.collection_name,
            points=points
        )


        logger.info(f"Indexed assignment {assignment_id} with {len(chunks)} chunks")


    @property
    def qdrant_client(self) -> QdrantClient:
        return self._qdrant
    
    @property
    def embedding_model(self) -> HuggingFaceEmbeddings:
        return self._embedding_model
