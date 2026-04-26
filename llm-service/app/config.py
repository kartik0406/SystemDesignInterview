from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    debug: bool = False
    
    # ─── LLM Provider ─────────────────────────────────────────
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    
    # ─── Redis ────────────────────────────────────────────────
    redis_host: str = "localhost"
    redis_port: int = 6379

    # ─── Pinecone ─────────────────────────────────────────────
    pinecone_api_key: str = ""
    pinecone_index_name: str = "sdi-knowledge"
    knowledge_dir: str = "./data/knowledge"

    # ─── Embeddings ───────────────────────────────────────────
    embedding_model: str = "all-MiniLM-L6-v2"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
