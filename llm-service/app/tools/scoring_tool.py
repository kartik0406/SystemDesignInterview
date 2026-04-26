"""
Scoring tool — evaluates candidate answers using structured rubric + LLM.
"""
import json
import logging
from app.llm.gemini_client import gemini_client
from app.llm.prompts import EVALUATION_PROMPT
from app.models.schemas import ScoreRequest, ScoreResponse

logger = logging.getLogger(__name__)


class ScoringTool:
    """MCP Tool: Evaluates candidate answers with structured rubric scoring."""

    async def score(self, request: ScoreRequest) -> ScoreResponse:
        """Evaluate a candidate answer and return structured scoring."""
        try:
            # Format conversation history
            history_text = "\n".join([
                f"{entry.get('role', 'unknown')}: {entry.get('content', '')}"
                for entry in request.conversation_history[-6:]  # last 3 exchanges
            ]) if request.conversation_history else "No previous conversation."

            # Format RAG context
            rag_text = "\n---\n".join(request.rag_context[:5]) if request.rag_context else "No reference context available."

            # Format rubric weights
            weights_text = json.dumps(request.rubric_weights) if request.rubric_weights else "Equal weights (2.0 each)"

            prompt = EVALUATION_PROMPT.format(
                company_mode=request.company_mode,
                question=request.question,
                answer=request.answer,
                rag_context=rag_text,
                conversation_history=history_text,
                rubric_weights=weights_text
            )

            result = await gemini_client.generate_json(prompt)

            return ScoreResponse(
                score=float(result.get("score", 5.0)),
                maxScore=float(result.get("maxScore", 10.0)),
                strengths=result.get("strengths", []),
                weaknesses=result.get("weaknesses", []),
                suggestions=result.get("suggestions", []),
                rubricBreakdown=result.get("rubricBreakdown", {}),
                difficultyAdjustment=result.get("difficultyAdjustment", "maintain")
            )
        except Exception as e:
            logger.error(f"Scoring failed: {e}")
            return ScoreResponse(
                score=5.0,
                maxScore=10.0,
                strengths=["Answer received and processed"],
                weaknesses=["Evaluation encountered an issue — please try again"],
                suggestions=["Ensure your answer covers the key system design aspects"],
                rubricBreakdown={
                    "scalability": 5.0, "database_design": 5.0,
                    "api_design": 5.0, "tradeoffs": 5.0, "clarity": 5.0
                },
                difficultyAdjustment="maintain"
            )


scoring_tool = ScoringTool()
