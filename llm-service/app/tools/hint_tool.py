"""
Hint tool — generates progressive hints without revealing full solutions.
"""
import logging
from app.llm.gemini_client import gemini_client
from app.llm.prompts import HINT_PROMPTS
from app.models.schemas import HintRequest, HintResponse

logger = logging.getLogger(__name__)


class HintTool:
    """MCP Tool: Generates progressive hints (nudge → direction → partial solution)."""

    async def generate_hint(self, request: HintRequest) -> HintResponse:
        """Generate a hint at the specified level."""
        try:
            level = min(max(request.hint_level, 1), 3)

            # Format conversation history
            history_text = "\n".join([
                f"{entry.get('role', 'unknown')}: {entry.get('content', '')}"
                for entry in request.conversation_history[-4:]
            ]) if request.conversation_history else "No previous conversation."

            # Format RAG context
            rag_text = "\n---\n".join(request.rag_context[:3]) if request.rag_context else ""

            prompt_template = HINT_PROMPTS[level]
            prompt = prompt_template.format(
                question=request.question,
                conversation_history=history_text,
                rag_context=rag_text
            )

            hint = await gemini_client.generate(prompt, temperature=0.6)

            logger.info(f"Generated level-{level} hint for: {request.question[:50]}...")
            return HintResponse(hint=hint.strip(), level=level)
        except Exception as e:
            logger.error(f"Hint generation failed: {e}")
            fallback_hints = {
                1: "Think about what happens as the number of users grows significantly.",
                2: "Consider how you would handle data consistency across multiple services. What patterns exist for this?",
                3: "You might want to consider using a message queue (like Kafka) for async processing, and a cache layer (like Redis) in front of your database to reduce read latency."
            }
            return HintResponse(
                hint=fallback_hints.get(request.hint_level, fallback_hints[1]),
                level=request.hint_level
            )


hint_tool = HintTool()
