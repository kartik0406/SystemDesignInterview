"""
RAG tool — retrieves relevant system design knowledge from Pinecone.
"""
import logging
from app.rag.retriever import retriever
from app.models.schemas import RAGQueryRequest, RAGQueryResponse

logger = logging.getLogger(__name__)

# ─── Hardcoded fallback knowledge map ────────────────────────
FALLBACK_KNOWLEDGE = {
    "load balancing": [
        "Load balancing distributes traffic across servers using algorithms like round-robin, least connections, or consistent hashing. Key considerations: health checks, session stickiness, L4 vs L7 balancing.",
    ],
    "caching": [
        "Caching strategies: Cache-aside (lazy loading), Write-through, Write-behind. Tools: Redis, Memcached. Consider cache invalidation (TTL, event-based), cache stampede prevention, and hot key handling.",
    ],
    "database": [
        "Database design considerations: SQL vs NoSQL trade-offs, indexing strategies, replication (leader-follower, multi-leader), sharding (hash-based, range-based), ACID vs BASE properties.",
    ],
    "microservices": [
        "Microservices patterns: API Gateway, Service Discovery, Circuit Breaker, Saga Pattern for distributed transactions, Event Sourcing, CQRS. Consider inter-service communication (sync REST/gRPC vs async messaging).",
    ],
    "scaling": [
        "Scaling strategies: Vertical scaling (bigger machines) vs Horizontal scaling (more machines). Stateless services for easy horizontal scaling. Use CDNs for static content. Database read replicas for read-heavy workloads.",
    ],
}


class RAGTool:
    """MCP Tool: Retrieves relevant knowledge using Pinecone vector search with fallback."""

    async def query(self, request: RAGQueryRequest) -> RAGQueryResponse:
        """Query the knowledge base for relevant chunks."""
        try:
            results = retriever.query(request.query, top_k=request.top_k)

            if results:
                return RAGQueryResponse(
                    chunks=[r["text"] for r in results],
                    sources=[r["source"] for r in results],
                    scores=[r["score"] for r in results]
                )

            # Fallback to hardcoded knowledge if Pinecone returns nothing
            logger.warning("Pinecone returned no results, using fallback knowledge")
            return self._fallback_query(request.query)
        except Exception as e:
            logger.error(f"RAG query failed: {e}, falling back to hardcoded knowledge")
            return self._fallback_query(request.query)

    def _fallback_query(self, query: str) -> RAGQueryResponse:
        """Fallback to hardcoded knowledge when vector DB is unavailable."""
        query_lower = query.lower()
        chunks = []
        sources = []

        for keyword, knowledge_chunks in FALLBACK_KNOWLEDGE.items():
            if keyword in query_lower:
                chunks.extend(knowledge_chunks)
                sources.extend([f"fallback:{keyword}"] * len(knowledge_chunks))

        if not chunks:
            # Return general knowledge if no keyword match
            for keyword, knowledge_chunks in FALLBACK_KNOWLEDGE.items():
                chunks.extend(knowledge_chunks)
                sources.extend([f"fallback:{keyword}"] * len(knowledge_chunks))

        return RAGQueryResponse(
            chunks=chunks[:5],
            sources=sources[:5],
            scores=[0.5] * min(len(chunks), 5)
        )


rag_tool = RAGTool()
