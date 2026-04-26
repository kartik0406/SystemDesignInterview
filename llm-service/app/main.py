import logging
from fastapi import FastAPI
from app.routers import mcp_tools

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="SDI LLM Service",
    description="Python FastAPI service handling LLM and Vector DB operations for the System Design Interview platform",
    version="1.0.0"
)

# Include the MCP tool endpoints
app.include_router(mcp_tools.router)

@app.get("/health")
async def health_check():
    """Health check endpoint used by Render."""
    return {"status": "ok"}
