"""
Pydantic schemas for request/response models.
"""
from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


# ─── Enums ────────────────────────────────────────────────────

class DifficultyLevel(str, Enum):
    BEGINNER = "BEGINNER"
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    HARD = "HARD"
    EXPERT = "EXPERT"


class CompanyMode(str, Enum):
    GOOGLE = "GOOGLE"
    AMAZON = "AMAZON"
    GENERAL = "GENERAL"


# ─── RAG ──────────────────────────────────────────────────────

class RAGQueryRequest(BaseModel):
    query: str
    top_k: int = Field(default=5, ge=1, le=20)


class RAGQueryResponse(BaseModel):
    chunks: list[str]
    sources: list[str] = []
    scores: list[float] = []


# ─── Question Generation ─────────────────────────────────────

class QuestionRequest(BaseModel):
    topic: str
    company_mode: str = "GENERAL"
    difficulty: int = Field(default=5, ge=1, le=10)
    previous_questions: list[str] = []
    conversation_history: list[dict] = []
    rag_context: list[str] = []
    focus_areas: list[str] = []


class QuestionResponse(BaseModel):
    question: str
    topic_area: str = ""
    expected_depth: str = ""


# ─── Scoring / Evaluation ────────────────────────────────────

class ScoreRequest(BaseModel):
    question: str
    answer: str
    company_mode: str = "GENERAL"
    rag_context: list[str] = []
    conversation_history: list[dict] = []
    rubric_weights: dict[str, float] = {}


class ScoreResponse(BaseModel):
    score: float
    maxScore: float = 10.0
    strengths: list[str] = []
    weaknesses: list[str] = []
    suggestions: list[str] = []
    rubricBreakdown: dict[str, float] = {}
    difficultyAdjustment: str = "maintain"


# ─── Diagram ─────────────────────────────────────────────────

class DiagramRequest(BaseModel):
    system_description: str
    components: list[str] = []


class DiagramResponse(BaseModel):
    diagram: str


# ─── Hint ─────────────────────────────────────────────────────

class HintRequest(BaseModel):
    question: str
    hint_level: int = Field(default=1, ge=1, le=3)
    conversation_history: list[dict] = []
    rag_context: list[str] = []


class HintResponse(BaseModel):
    hint: str
    level: int


# ─── MCP Tool Manifest ───────────────────────────────────────

class ToolDefinition(BaseModel):
    name: str
    description: str
    endpoint: str
    method: str = "POST"
    parameters: dict = {}


class ToolManifest(BaseModel):
    tools: list[ToolDefinition]
    version: str = "1.0.0"
