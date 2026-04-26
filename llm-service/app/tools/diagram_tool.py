"""
Diagram tool — generates Mermaid architecture diagrams via LLM.
"""
import logging
from app.llm.gemini_client import gemini_client
from app.llm.prompts import DIAGRAM_PROMPT
from app.models.schemas import DiagramRequest, DiagramResponse

logger = logging.getLogger(__name__)


class DiagramTool:
    """MCP Tool: Generates Mermaid architecture diagrams from system descriptions."""

    async def generate(self, request: DiagramRequest) -> DiagramResponse:
        """Generate a Mermaid diagram for the given system."""
        try:
            components_text = ", ".join(request.components) if request.components else "standard web architecture components"

            prompt = DIAGRAM_PROMPT.format(
                system_description=request.system_description,
                components=components_text
            )

            result = await gemini_client.generate(prompt, temperature=0.3)

            # Clean up — remove markdown fences if present
            diagram = result.strip()
            if diagram.startswith("```mermaid"):
                diagram = diagram[len("```mermaid"):].strip()
            if diagram.startswith("```"):
                diagram = diagram[3:].strip()
            if diagram.endswith("```"):
                diagram = diagram[:-3].strip()

            # Validate it starts with a valid Mermaid keyword
            valid_starts = ("graph ", "flowchart ", "sequenceDiagram", "classDiagram", "erDiagram")
            if not any(diagram.startswith(s) for s in valid_starts):
                diagram = f"graph TB\n    Client[Client] --> LB[Load Balancer]\n    LB --> API[API Server]\n    API --> DB[(Database)]"

            logger.info(f"Generated diagram for: {request.system_description[:50]}...")
            return DiagramResponse(diagram=diagram)
        except Exception as e:
            logger.error(f"Diagram generation failed: {e}")
            return DiagramResponse(
                diagram="graph TB\n    Client[Client] --> API[API Gateway]\n    API --> Service[Service]\n    Service --> DB[(Database)]"
            )


diagram_tool = DiagramTool()
