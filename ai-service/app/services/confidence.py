"""
Phase 9: confidence engine - a score SEPARATE from the marks, describing
how much a teacher should trust the automated evaluation.
"""
from __future__ import annotations
from app.core.config import settings
from app.models.schemas import ConceptMatch





def _concept_reliability_signal(concept_details: list[ConceptMatch]) -> float:
    """Concepts matched with high fuzzy scores (clear, unambiguous matches)
    increase reliability; borderline fuzzy matches near the threshold
    reduce it."""
    if not concept_details:
        return 0.5
    scores = []
    for c in concept_details:
        if c.fuzzyScore is None:
            continue
        # distance from threshold, normalized: matches far above threshold
        # (or clearly absent, far below) are reliable; borderline calls are not.
        dist = abs(c.fuzzyScore - settings.CONCEPT_FUZZY_MATCH_THRESHOLD)
        scores.append(min(1.0, dist / 30.0))
    return sum(scores) / len(scores) if scores else 0.5





def compute_confidence(
    ocr_confidence_pct: float,
    semantic_similarity: float,
    concept_details: list[ConceptMatch],
    student_answer: str,
    max_marks: float,
    segmentation_confidence_pct: float,
) -> tuple[float, str, dict, list[str]]:
    semantic_signal = max(0.0, min(1.0, semantic_similarity))
    concept_reliability = _concept_reliability_signal(concept_details)

    weights = {
        "semantic": settings.CONF_WEIGHT_SEMANTIC,
        "conceptReliability": settings.CONF_WEIGHT_CONCEPT_RELIABILITY,
    }
    signals = {
        "semantic": semantic_signal,
        "conceptReliability": concept_reliability,
    }

    total_weight = sum(weights.values()) or 1.0
    overall = sum(signals[k] * weights[k] for k in weights) / total_weight
    overall = round(max(0.0, min(1.0, overall)), 4)

    if overall >= settings.CONFIDENCE_HIGH_THRESHOLD:
        level = "HIGH"
    elif overall >= settings.CONFIDENCE_MEDIUM_THRESHOLD:
        level = "MEDIUM"
    else:
        level = "LOW"

    reasons = []
    if concept_reliability < 0.4:
        reasons.append("Several concept matches were borderline/fuzzy")
    if level == "LOW":
        reasons.append("Overall confidence below the mandatory-review threshold")
    elif level == "MEDIUM":
        reasons.append("Overall confidence in the teacher-confirmation range")

    breakdown = {"signals": signals, "weights": weights, "overall": overall}
    return overall, level, breakdown, reasons
