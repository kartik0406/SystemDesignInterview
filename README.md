# 🧠 AI System Design Interviewer

A production-grade, cloud-deployed **multi-agent GenAI platform** that simulates adaptive system design interviews. Built with **Spring Boot 3**, **Python/FastAPI**, **Google Gemini**, **Pinecone RAG**, and **React** — deployed across **Render**, **Vercel**, **Supabase**, and **Upstash**.

> **Live Demo:** [Frontend (Vercel)](https://systemdesigninterviews-mjsoi6stq-kartik0406s-projects.vercel.app) · [API Gateway (Render)](https://sdi-api-gateway.onrender.com) · [LLM Service (Render)](https://sdi-llm-service.onrender.com/health)

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Frontend (React + Vite)                       │
│                         Hosted on Vercel                             │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │ HTTPS
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  API Gateway (Spring Boot 3 / Java 21)               │
│                        Hosted on Render                              │
│                                                                      │
│  ┌──────────────────┐  ┌─────────────────┐  ┌───────────────────┐   │
│  │ Interview Agent   │  │ Question Agent  │  │ Evaluation Agent  │   │
│  │  (Orchestrator)   │──│  (Generates Qs) │  │ (Scores Answers)  │   │
│  └──────────────────┘  └─────────────────┘  └───────────────────┘   │
│           │                                          │               │
│  ┌────────┴──────────┐                    ┌──────────┴────────┐     │
│  │   Hint Agent      │                    │  Session Service   │     │
│  │ (Progressive Hints)│                   │  (State Machine)   │     │
│  └───────────────────┘                    └───────────────────┘     │
│           │                                     │          │         │
└───────────┼─────────────────────────────────────┼──────────┼─────────┘
            │ HTTP                                │          │
            ▼                                     ▼          ▼
┌──────────────────────────┐        ┌───────────────┐  ┌──────────┐
│ LLM Service (FastAPI)    │        │   Supabase     │  │ Upstash  │
│    Hosted on Render      │        │  PostgreSQL    │  │  Redis   │
│                          │        └───────────────┘  └──────────┘
│  ┌────────────────────┐  │
│  │  MCP Tool Layer    │  │
│  │  ├─ RAG Tool       │  │───▶  Pinecone (Vector DB)
│  │  ├─ Scoring Tool   │  │
│  │  ├─ Diagram Tool   │  │───▶  Google Gemini API
│  │  └─ Hint Tool      │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

---

## ⚡ Tech Stack

| Layer | Technology | Hosting |
|-------|-----------|---------|
| Frontend | React 19, Vite 5, Vanilla CSS | **Vercel** (Free) |
| API Gateway | Spring Boot 3.3, Java 21 | **Render** (Docker, Free) |
| LLM Service | Python 3.12, FastAPI, Google Gemini | **Render** (Docker, Free) |
| Vector DB | Pinecone (Gemini Embeddings, 768d) | **Pinecone** (Starter, Free) |
| Database | PostgreSQL 15 | **Supabase** (Free) |
| Session Memory | Redis 7 | **Upstash** (Free) |
| LLM Provider | Google Gemini 2.5 Flash | **Google AI Studio** (Free) |

---

## 🎯 Key Features

- **Adaptive Difficulty** — Questions adjust based on answer quality (score ≥ 8 → harder, ≤ 4 → easier)
- **Multi-Agent Architecture (A2A)** — Specialized agents for question generation, evaluation, and hints
- **MCP Tool Layer** — LLM tools for RAG retrieval, rubric scoring, Mermaid diagrams, and progressive hints
- **RAG Knowledge Base** — Pinecone-powered retrieval with Gemini Embeddings for grounded feedback
- **Structured Rubric Scoring** — 5-dimension evaluation: Scalability, Database Design, API Design, Trade-offs, Clarity
- **Company Modes** — Tailored interviews for Google (scalability focus), Amazon (trade-offs), and General
- **Progressive Hints** — 3-level hint system: Nudge → Direction → Partial Solution
- **Architecture Diagrams** — Auto-generated Mermaid diagrams for system visualization
- **Full Cloud Deployment** — Zero infrastructure, 100% free-tier hosted

---

## 🚀 Quick Start

### Option A: Cloud (Production)

The app is fully deployed and accessible via the live demo links above. No setup required.

### Option B: Local Development

#### Prerequisites
- Java 21+
- Python 3.11+
- Node.js 20+
- Docker (for Redis, or use Upstash)
- Google Gemini API key

#### 1. Clone and configure
```bash
git clone https://github.com/kartik0406/SystemDesignInterview.git
cd SystemDesignInterview
cp .env.example .env
# Edit .env with your API keys and database credentials
```

#### 2. Start with Docker Compose
```bash
docker-compose up -d
```

#### 3. Or run services individually

**Redis:**
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

**LLM Service (Python):**
```bash
cd llm-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

**API Gateway (Spring Boot):**
```bash
cd api-gateway
mvn spring-boot:run
```

**Frontend (React):**
```bash
cd frontend
npm install && npm run dev
```

#### 4. Open in browser
Visit `http://localhost:5173`

---

## 📚 Building the RAG Index

Populate the Pinecone vector database with system design knowledge:

```bash
cd llm-service
python -m scripts.build_index
```

This reads the markdown files in `knowledge-base/` and embeds them into Pinecone using Gemini Embeddings.

---

## 🔌 API Endpoints

### Interview Flow
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/interview/start` | Start a new interview session |
| `POST` | `/api/v1/interview/answer` | Submit an answer for evaluation |
| `GET` | `/api/v1/interview/session/{id}` | Get current session state |
| `GET` | `/api/v1/interview/result/{id}` | Get final interview report |
| `POST` | `/api/v1/interview/hint` | Request a progressive hint |
| `GET` | `/api/v1/interview/topics` | List available topics |

### Agent Discovery & Tools
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/.well-known/agent-cards` | A2A agent discovery |
| `GET` | `/tools/manifest` | MCP tool manifest |
| `POST` | `/tools/rag/query` | RAG knowledge retrieval |
| `POST` | `/tools/generate-question` | Generate interview question |
| `POST` | `/tools/score` | Evaluate candidate answer |
| `POST` | `/tools/diagram` | Generate Mermaid diagram |
| `POST` | `/tools/hint` | Generate progressive hint |

---

## 📁 Project Structure

```
SystemDesignInterviewAgent/
├── api-gateway/                  # Spring Boot 3 API Gateway + Agent Orchestrator
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/sdi/gateway/
│       ├── agent/                # A2A Agents (Interview, Question, Evaluation, Hint)
│       ├── client/               # HTTP client to Python LLM Service
│       ├── config/               # Redis, CORS, WebSocket, App configs
│       ├── controller/           # REST endpoints (Interview, Health)
│       ├── exception/            # Global error handling
│       ├── model/
│       │   ├── dto/              # Request/Response DTOs
│       │   ├── entity/           # JPA Entities (InterviewSession, EvaluationResult)
│       │   └── enums/            # CompanyMode, DifficultyLevel, SessionStatus
│       ├── repository/           # JPA Repositories (Supabase PostgreSQL)
│       └── service/              # Business logic (InterviewService, SessionService)
│
├── llm-service/                  # Python FastAPI — MCP Tool Server
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── config.py             # Settings (env vars, model config)
│       ├── main.py               # FastAPI app entry point
│       ├── llm/
│       │   ├── gemini_client.py  # Gemini API wrapper (text + JSON generation)
│       │   └── prompts.py        # All prompt templates
│       ├── rag/
│       │   ├── embeddings.py     # Gemini Embedding Engine (768d, MRL)
│       │   └── retriever.py      # Pinecone vector retriever
│       ├── routers/
│       │   └── mcp_tools.py      # MCP tool endpoints
│       └── tools/
│           ├── rag_tool.py       # RAG retrieval + fallback knowledge
│           ├── scoring_tool.py   # Rubric-based evaluation
│           ├── diagram_tool.py   # Mermaid diagram generation
│           └── hint_tool.py      # Progressive hint generation
│
├── frontend/                     # React + Vite — Premium UI
│   ├── Dockerfile
│   ├── vercel.json               # Vercel SPA routing config
│   ├── package.json
│   └── src/
│       ├── App.jsx               # Router (Landing → Interview → Results)
│       ├── api.js                # API client (axios)
│       └── pages/
│           ├── LandingPage.jsx   # Topic selection + company mode
│           ├── InterviewPage.jsx # Chat interface + hint system
│           └── ResultsPage.jsx   # Scores, rubric breakdown, diagram
│
├── knowledge-base/               # System design knowledge for RAG
│   ├── patterns/                 # Caching, Sharding, CAP, Load Balancing, etc.
│   └── architectures/            # URL Shortener, Netflix, Uber designs
│
├── render.yaml                   # Render Blueprint (IaC for backend deployment)
├── docker-compose.yml            # Local development orchestration
└── .env.example                  # Environment variable template
```

---

## ☁️ Cloud Deployment

This project is deployed using **free tiers** across multiple cloud providers:

| Service | Provider | Config File |
|---------|----------|-------------|
| API Gateway | [Render](https://render.com) | `render.yaml` |
| LLM Service | [Render](https://render.com) | `render.yaml` |
| Frontend | [Vercel](https://vercel.com) | `frontend/vercel.json` |
| Database | [Supabase](https://supabase.com) | `application.yml` |
| Redis | [Upstash](https://upstash.com) | `application.yml` |
| Vector DB | [Pinecone](https://pinecone.io) | `config.py` |

### Environment Variables

```env
# LLM Provider
GEMINI_API_KEY=your_gemini_api_key

# Supabase (PostgreSQL)
DATABASE_URL=jdbc:postgresql://db.xxx.supabase.co:6543/postgres?prepareThreshold=0
DATABASE_USERNAME=postgres.xxx
DATABASE_PASSWORD=your_password

# Upstash (Redis)
REDIS_HOST=your-endpoint.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=your_upstash_password
REDIS_SSL=true

# Pinecone
PINECONE_API_KEY=your_pinecone_api_key
PINECONE_INDEX_NAME=sdi-knowledge

# Frontend (Vercel)
VITE_API_URL=https://sdi-api-gateway.onrender.com
```

---

## 🧪 Interview Flow

```
User selects topic (e.g., "Design Netflix")
        │
        ▼
InterviewAgent creates session → stores in Supabase
        │
        ▼
QuestionAgent fetches RAG context from Pinecone
        │
        ▼
Gemini generates adaptive question based on:
  • Topic + difficulty level
  • Previous Q&A history (Redis)
  • RAG knowledge context
        │
        ▼
User submits answer
        │
        ▼
EvaluationAgent scores on 5 rubric dimensions
        │
        ▼
Difficulty adjusts: score ≥ 8 → harder, ≤ 4 → easier
        │
        ▼
Repeat for 6 rounds → Final Report with:
  • Overall score
  • Rubric breakdown (radial chart)
  • Strengths / Weaknesses
  • Architecture diagram (Mermaid)
```

---

## 💡 Resume Bullet Points

- Architected a **multi-agent GenAI platform** using A2A patterns to simulate adaptive system design interviews with Google Gemini
- Implemented an **MCP-based tool layer** in Python/FastAPI enabling LLMs to perform RAG retrieval, rubric scoring, and Mermaid diagram generation
- Built a **RAG pipeline** using Pinecone + Gemini Embeddings (768d, Matryoshka) for contextual interview evaluation grounding
- Developed **Spring Boot 3 microservices** with Supabase PostgreSQL persistence, Upstash Redis session memory, and adaptive difficulty scaling
- Deployed to production using **Render (IaC via render.yaml)**, **Vercel**, **Supabase**, **Upstash**, and **Pinecone** — fully on free tiers
- Created a **premium React UI** with animated scoring, radial charts, glassmorphism design, and real-time interview flow

---

## 📄 License

MIT
