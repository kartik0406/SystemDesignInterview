"""
MCP Tools Router — exposes all MCP tool endpoints.
Follows MCP (Model Context Protocol) patterns for tool discovery and invocation.
"""
import json
import logging
from fastapi import APIRouter, HTTPException

from app.llm.gemini_client import gemini_client
from app.llm.prompts import QUESTION_GENERATOR_PROMPT
from app.models.schemas import (
    RAGQueryRequest, RAGQueryResponse,
    QuestionRequest, QuestionResponse,
    ScoreRequest, ScoreResponse,
    DiagramRequest, DiagramResponse,
    HintRequest, HintResponse,
    ToolManifest, ToolDefinition
)
from app.tools.rag_tool import rag_tool
from app.tools.scoring_tool import scoring_tool
from app.tools.diagram_tool import diagram_tool
from app.tools.hint_tool import hint_tool

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/tools", tags=["MCP Tools"])


# ─── MCP Tool Manifest (Discovery) ───────────────────────────

@router.get("/manifest", response_model=ToolManifest)
async def get_tool_manifest():
    """MCP tool discovery endpoint. Lists all available tools."""
    return ToolManifest(
        tools=[
            ToolDefinition(
                name="rag_query",
                description="Retrieve relevant system design knowledge from vector database",
                endpoint="/tools/rag/query",
                parameters={"query": "string", "top_k": "int (1-20)"}
            ),
            ToolDefinition(
                name="generate_question",
                description="Generate an adaptive interview question based on context",
                endpoint="/tools/generate-question",
                parameters={"topic": "string", "difficulty": "int (1-10)", "company_mode": "string"}
            ),
            ToolDefinition(
                name="score",
                description="Evaluate a candidate answer with structured rubric scoring",
                endpoint="/tools/score",
                parameters={"question": "string", "answer": "string", "company_mode": "string"}
            ),
            ToolDefinition(
                name="diagram",
                description="Generate a Mermaid architecture diagram",
                endpoint="/tools/diagram",
                parameters={"system_description": "string"}
            ),
            ToolDefinition(
                name="hint",
                description="Generate a progressive hint (levels 1-3)",
                endpoint="/tools/hint",
                parameters={"question": "string", "hint_level": "int (1-3)"}
            ),
        ],
        version="1.0.0"
    )


# ─── RAG Tool ────────────────────────────────────────────────

@router.post("/rag/query", response_model=RAGQueryResponse)
async def rag_query(request: RAGQueryRequest):
    """Retrieve relevant knowledge chunks from the FAISS vector store."""
    return await rag_tool.query(request)


# ─── Question Generation ────────────────────────────────────

@router.post("/generate-question", response_model=QuestionResponse)
async def generate_question(request: QuestionRequest):
    """Generate an adaptive interview question using RAG + LLM."""
    try:
        # Format conversation history
        history_text = "\n".join([
            f"{entry.get('role', 'unknown')}: {entry.get('content', '')}"
            for entry in request.conversation_history[-6:]
        ]) if request.conversation_history else "This is the first question."

        # Format previous questions
        prev_q_text = "\n".join([f"- {q}" for q in request.previous_questions]) if request.previous_questions else "None yet."

        # Format RAG context
        rag_text = "\n---\n".join(request.rag_context[:3]) if request.rag_context else "No reference context."

        # Format focus areas
        focus_text = ", ".join(request.focus_areas) if request.focus_areas else "general system design"

        prompt = QUESTION_GENERATOR_PROMPT.format(
            company_mode=request.company_mode,
            topic=request.topic,
            difficulty=request.difficulty,
            previous_questions=prev_q_text,
            conversation_history=history_text,
            rag_context=rag_text,
            focus_areas=focus_text
        )

        result = await gemini_client.generate_json(prompt)

        return QuestionResponse(
            question=result.get("question", f"Tell me about how you would design {request.topic}."),
            topic_area=result.get("topic_area", "general"),
            expected_depth=result.get("expected_depth", "overview")
        )
    except Exception as e:
        logger.error(f"Question generation failed: {e}")
        return QuestionResponse(
            question=f"Walk me through the high-level architecture for {request.topic}. What are the main components?",
            topic_area="high-level design",
            expected_depth="overview"
        )


# ─── Scoring Tool ────────────────────────────────────────────

@router.post("/score", response_model=ScoreResponse)
async def score_answer(request: ScoreRequest):
    """Evaluate a candidate answer with structured rubric scoring."""
    return await scoring_tool.score(request)


# ─── Diagram Tool ────────────────────────────────────────────

@router.post("/diagram", response_model=DiagramResponse)
async def generate_diagram(request: DiagramRequest):
    """Generate a Mermaid architecture diagram."""
    return await diagram_tool.generate(request)


# ─── Hint Tool ───────────────────────────────────────────────

@router.post("/hint", response_model=HintResponse)
async def generate_hint(request: HintRequest):
    """Generate a progressive hint (nudge → direction → partial solution)."""
    return await hint_tool.generate_hint(request)
