from agents.helper_agent.schemas.rag_state import RAGState

def route_rag_usage(state: RAGState) -> str:
    return "query_rag_llm" if state.docs else "query_llm"