"""
Pinecone-based retriever for RAG (Retrieval-Augmented Generation).
"""
import logging
from pinecone import Pinecone
from app.config import settings
from app.rag.embeddings import embedding_engine

logger = logging.getLogger(__name__)


class PineconeRetriever:
    """Retrieves relevant knowledge chunks from a Pinecone vector index."""

    def __init__(self):
        self._index = None

    @property
    def index(self):
        if self._index is None:
            logger.info(f"Connecting to Pinecone index: {settings.pinecone_index_name}")
            pc = Pinecone(api_key=settings.pinecone_api_key)
            self._index = pc.Index(settings.pinecone_index_name)
            logger.info("Pinecone index connected successfully")
        return self._index

    def query(self, query_text: str, top_k: int = 5) -> list[dict]:
        """Query Pinecone for the most relevant chunks."""
        try:
            query_vector = embedding_engine.embed_query(query_text).tolist()
            results = self.index.query(
                vector=query_vector,
                top_k=top_k,
                include_metadata=True
            )
            chunks = []
            for match in results.get("matches", []):
                chunks.append({
                    "text": match["metadata"].get("text", ""),
                    "source": match["metadata"].get("source", "unknown"),
                    "score": match["score"]
                })
            return chunks
        except Exception as e:
            logger.error(f"Pinecone query failed: {e}")
            return []


retriever = PineconeRetriever()
