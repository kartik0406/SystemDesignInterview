"""
Embedding wrapper using Sentence Transformers.
"""
import logging
import numpy as np
from sentence_transformers import SentenceTransformer
from app.config import settings

logger = logging.getLogger(__name__)


class EmbeddingEngine:
    """Wraps sentence-transformers for text embedding."""

    def __init__(self):
        self._model = None

    @property
    def model(self):
        if self._model is None:
            logger.info(f"Loading embedding model: {settings.embedding_model}")
            self._model = SentenceTransformer(settings.embedding_model)
            logger.info("Embedding model loaded successfully")
        return self._model

    def embed(self, texts: list[str]) -> np.ndarray:
        """Embed a list of texts into vectors."""
        embeddings = self.model.encode(texts, normalize_embeddings=True, show_progress_bar=False)
        return np.array(embeddings, dtype=np.float32)

    def embed_query(self, query: str) -> np.ndarray:
        """Embed a single query."""
        return self.embed([query])[0]

    @property
    def dimension(self) -> int:
        """Get embedding dimension."""
        return self.model.get_sentence_embedding_dimension()


embedding_engine = EmbeddingEngine()
