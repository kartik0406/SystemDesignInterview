"""
Google Gemini client wrapper.
Handles all LLM interactions with structured output parsing.
"""
import json
import logging
from google import genai
from google.genai import types
from app.config import settings

logger = logging.getLogger(__name__)


class GeminiClient:
    """Wrapper around Google Gemini API for structured generation."""

    def __init__(self):
        self.client = genai.Client(api_key=settings.gemini_api_key)
        self.model = settings.gemini_model

    async def generate(self, prompt: str, temperature: float = 0.7) -> str:
        """Generate text from a prompt."""
        try:
            response = self.client.models.generate_content(
                model=self.model,
                contents=prompt,
                config=types.GenerateContentConfig(
                    temperature=temperature,
                    max_output_tokens=2048,
                )
            )
            return response.text.strip()
        except Exception as e:
            logger.error(f"Gemini generation failed: {e}")
            raise

    async def generate_json(self, prompt: str, temperature: float = 0.4) -> dict:
        """Generate and parse JSON from a prompt."""
        try:
            response = self.client.models.generate_content(
                model=self.model,
                contents=prompt,
                config=types.GenerateContentConfig(
                    temperature=temperature,
                    max_output_tokens=2048,
                    response_mime_type="application/json",
                )
            )
            text = response.text.strip()

            # Clean up potential markdown code fences
            if text.startswith("```"):
                text = text.split("\n", 1)[-1]
                if text.endswith("```"):
                    text = text[:-3].strip()

            return json.loads(text)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from Gemini response: {e}")
            logger.debug(f"Raw response: {response.text[:500] if response else 'None'}")
            raise
        except Exception as e:
            logger.error(f"Gemini JSON generation failed: {e}")
            raise


# Singleton instance
gemini_client = GeminiClient()
