"""
Central, editable configuration for the AI evaluation pipeline.

Every threshold / weight here is a TUNABLE DEFAULT, not a claim of
universal optimality. Teachers / researchers are expected to calibrate
these against real, teacher-verified ground truth for their subject
and student population (see docs/RESEARCH_METHODOLOGY.md).
"""
import os
import shutil
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


def _find_default_tesseract() -> str | None:
    found = shutil.which("tesseract")
    if found:
        return found
    if os.name == "nt":
        candidates = [
            r"C:\Program Files\Tesseract-OCR\tesseract.exe",
            r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
            os.path.expandvars(r"%LOCALAPPDATA%\Programs\Tesseract-OCR\tesseract.exe"),
        ]
        for c in candidates:
            if Path(c).is_file():
                return c
    return None


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AES_")
    # --- Service ---
    SERVICE_NAME: str = "ai-answer-evaluation-service"
    ENV: str = "development"
    CORS_ALLOWED_ORIGINS: str = ""

    # --- OCR ---
    TESSERACT_CMD: str | None = Field(
        default_factory=_find_default_tesseract,
        description="Path to tesseract binary, auto-detected if omitted",
    )
    OCR_LOW_CONFIDENCE_THRESHOLD: float = Field(
        default=55.0,
        description="Tesseract mean word confidence (0-100) below which the "
                    "sheet/answer is flagged NEEDS_REVIEW regardless of any "
                    "downstream score.",
    )
    OCR_PSM_CANDIDATES: list[int] = Field(default_factory=lambda: [6])

    # --- Hybrid scoring weights (Phase 7) ---
    WEIGHT_SEMANTIC_SIMILARITY: float = 0.65
    WEIGHT_CONCEPT_COVERAGE: float = 0.35
    # NOTE: 0.65 / 0.35 is a starting default, explicitly NOT claimed to be
    # universally optimal. Override via env vars or the /config endpoint.

    # --- Concept matching ---
    CONCEPT_FUZZY_MATCH_THRESHOLD: int = Field(
        default=82, description="RapidFuzz partial-ratio (0-100) threshold "
                                 "for treating an OCR'd token as matching a "
                                 "rubric concept (tolerates OCR noise)."
    )
    CONTRADICTION_WINDOW_TOKENS: int = 4  # negation-word proximity window

    # --- Marking ---
    ALLOW_NEGATIVE_MARKS: bool = False
    MARK_ROUNDING: str = "nearest_half"  # nearest_half | nearest_int | none

    # --- Confidence engine weights (Phase 9), must sum to 1.0 ---
    CONF_WEIGHT_OCR: float = 0.30
    CONF_WEIGHT_SEMANTIC: float = 0.20
    CONF_WEIGHT_CONCEPT_RELIABILITY: float = 0.20
    CONF_WEIGHT_ANSWER_LENGTH: float = 0.10
    CONF_WEIGHT_SEGMENTATION: float = 0.10
    CONF_WEIGHT_CONTRADICTION: float = 0.10

    CONFIDENCE_HIGH_THRESHOLD: float = 0.80
    CONFIDENCE_MEDIUM_THRESHOLD: float = 0.55
    # >= HIGH -> HIGH ; >= MEDIUM -> MEDIUM ; else -> LOW

    # --- Answer length heuristics (used only as a confidence signal) ---
    MIN_EXPECTED_WORDS_PER_MARK: float = 3.0



settings = Settings()
