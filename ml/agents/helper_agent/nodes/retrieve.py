from agents.helper_agent.schemas.rag_state import RAGState
from langchain_core.retrievers import BaseRetriever


def retrieve(state: RAGState, retriever: BaseRetriever) -> dict[str, str]:
    """Retrieve relevant (< threshold) information related to a query."""
    retrieved_docs = retriever.invoke(
        state.query,
    )
    serialized = "\n\n".join(
        (f"{doc.page_content}\n")
        for doc in retrieved_docs if doc.metadata.get("assignment_id") == state.assignment_id
    )
    print("RETRIEVED", serialized)
    return {"docs": serialized}