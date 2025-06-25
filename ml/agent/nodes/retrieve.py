from agent.schemas.rag_state import RAGState
from langchain_core.retrievers import BaseRetriever


def retrieve(state: RAGState, retriever: BaseRetriever) -> str:
    """Retrieve relevant (< threshold) information related to a query."""
    retrieved_docs = retriever.invoke(
        state.query,
        filter={"assignment_id": state.assignment_id}
    )
    serialized = "\n\n".join(
        (f"{doc.page_content}\n")
        for doc in retrieved_docs
    )
    return {"docs": serialized}