"""
Embedding wrapper using Gemini Embeddings API.
"""
import logging
import numpy as np
from google import genai
from app.config import settings

logger = logging.getLogger(__name__)


class EmbeddingEngine:
    """Wraps Gemini API for text embedding."""

    def __init__(self):
        self._client = None
        self._model_name = settings.embedding_model
        # Gemini text-embedding-004 outputs 768 dimensions
        self._dimension = 768

    @property
    def client(self):
        if self._client is None:
            logger.info(f"Initializing Gemini Embedding client for model: {self._model_name}")
            self._client = genai.Client(api_key=settings.gemini_api_key)
        return self._client

    def embed(self, texts: list[str]) -> np.ndarray:
        """Embed a list of texts into vectors."""
        try:
            response = self.client.models.embed_content(
                model=self._model_name,
                contents=texts
            )
            # Extact embeddings and convert to numpy array
            embeddings = [emb.values for emb in response.embeddings]
            return np.array(embeddings, dtype=np.float32)
        except Exception as e:
            logger.error(f"Error calling Gemini embeddings: {e}")
            # Fallback to zero vectors if API fails so system doesn't crash completely
            return np.zeros((len(texts), self.dimension), dtype=np.float32)

    def embed_query(self, query: str) -> np.ndarray:
        """Embed a single query."""
        return self.embed([query])[0]

    @property
    def dimension(self) -> int:
        """Get embedding dimension."""
        return self._dimension


embedding_engine = EmbeddingEngine()
