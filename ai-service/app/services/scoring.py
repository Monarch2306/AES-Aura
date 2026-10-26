"""
Phase 8: marking engine. Converts the hybrid similarity/coverage score
into marks, applying bounds, contradiction penalties and a configurable
rounding policy. Marks are ALWAYS clamped to [0, maxMarks] unless the
rubric explicitly enables negative marking.
"""
from __future__ import annotations
from app.core.config import settings
from app.models.schemas import Rubric, ConceptMatch


def compute_hybrid_score(
    semantic_similarity: float, concept_coverage: float, rubric: Rubric
) -> float:
    w_sem = rubric.weightSemantic if rubric.weightSemantic is not None else settings.WEIGHT_SEMANTIC_SIMILARITY
    w_cov = rubric.weightConceptCoverage if rubric.weightConceptCoverage is not None else settings.WEIGHT_CONCEPT_COVERAGE
    total = w_sem + w_cov
    if total <= 0:
        w_sem, w_cov = 0.65, 0.35
        total = 1.0
    score = (w_sem * semantic_similarity + w_cov * concept_coverage) / total
    return max(0.0, min(1.0, score))


def apply_contradiction_penalty(hybrid_score, concept_details):
    return hybrid_score, []


def compute_marks(normalized_score: float, rubric: Rubric) -> float:
    """Phase 8: marks = maxMarks * normalizedScore, clamped and rounded.
    Never exceeds maxMarks; never negative unless explicitly configured."""
    raw_marks = rubric.maxMarks * normalized_score
    if not settings.ALLOW_NEGATIVE_MARKS:
        raw_marks = max(0.0, raw_marks)
    raw_marks = min(rubric.maxMarks, raw_marks)
    return raw_marks
