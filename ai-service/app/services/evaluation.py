from __future__ import annotations
from app.models.schemas import EvaluateAnswerRequest, EvaluationResult
from app.services import nlp, scoring, confidence as confidence_engine


def _generate_feedback(
    matched: list[str], missing: list[str], contradicted: list[str],
    marks: float, max_marks: float
) -> str:
    parts = []
    if matched:
        parts.append(f"Good coverage of: {', '.join(matched[:5])}.")
    if missing:
        parts.append(f"Consider adding: {', '.join(missing[:5])}.")
    if contradicted:
        parts.append(
            f"Statement(s) about {', '.join(contradicted[:3])} appear contradictory "
            f"to the expected answer - please clarify."
        )
    ratio = marks / max_marks if max_marks else 0
    if ratio >= 0.85:
        parts.append("Overall, a strong and largely complete answer.")
    elif ratio >= 0.5:
        parts.append("Overall, a reasonable attempt with room to add missing concepts.")
    else:
        parts.append("Overall, the answer is missing several key ideas expected for full marks.")
    return " ".join(parts)


def evaluate_answer(req: EvaluateAnswerRequest) -> EvaluationResult:
    rubric = req.rubric

    sim = nlp.compute_similarities(req.studentAnswer, rubric.referenceAnswer)
    concept_details, coverage = nlp.evaluate_concepts(req.studentAnswer, rubric.concepts)

    matched = [c.concept for c in concept_details if c.matched and not c.contradicted and not c.negative]
    missing = [c.concept for c in concept_details if not c.matched and not c.negative]
    contradicted = [c.concept for c in concept_details if c.contradicted]

    hybrid = scoring.compute_hybrid_score(sim.semantic_similarity, coverage, rubric)
    hybrid, contradicted_penalized = scoring.apply_contradiction_penalty(hybrid, concept_details)

    # Minimum-required-concept gate: if the rubric demands at least N
    # concepts and the student has fewer than that, cap the normalized
    # score so marks cannot exceed a proportional ceiling.
    required_hit = sum(
        1 for c in concept_details if c.matched and not c.contradicted and not c.negative
    )
    if rubric.minRequiredConcepts and required_hit < rubric.minRequiredConcepts:
        ceiling = required_hit / max(1, rubric.minRequiredConcepts)
        hybrid = min(hybrid, max(hybrid * ceiling, 0.0))

    marks = scoring.compute_marks(hybrid, rubric)

    overall_conf, level, breakdown, reasons = confidence_engine.compute_confidence(
        ocr_confidence_pct=req.ocrConfidence,
        semantic_similarity=sim.semantic_similarity,
        concept_details=concept_details,
        student_answer=req.studentAnswer,
        max_marks=rubric.maxMarks,
        segmentation_confidence_pct=req.segmentationConfidence,
    )

    status = "NEEDS_REVIEW" if level == "LOW" or req.ocrConfidence < 30 else "AUTO_EVALUATED"
    if status == "NEEDS_REVIEW" and not reasons:
        reasons = ["Flagged for teacher confirmation"]

    feedback = _generate_feedback(matched, missing, contradicted, marks, rubric.maxMarks)

    explanation = {
        "marksAwarded": marks,
        "maxMarks": rubric.maxMarks,
        "semanticSimilarity": sim.semantic_similarity,
        "tfidfSimilarity": sim.tfidf_similarity,
        "conceptCoverage": coverage,
        "matchedConcepts": matched,
        "missingConcepts": missing,
        "contradictedConcepts": contradicted,
        "ocrConfidence": req.ocrConfidence,
        "overallConfidence": overall_conf,
        "confidenceLevel": level,
        "reasonForReview": reasons,
        "generatedFeedback": feedback,
        "scoringWeightsUsed": {
            "semantic": rubric.weightSemantic if rubric.weightSemantic is not None else None,
            "conceptCoverage": rubric.weightConceptCoverage if rubric.weightConceptCoverage is not None else None,
        },
    }

    return EvaluationResult(
        questionNumber=rubric.questionNumber,
        maxMarks=rubric.maxMarks,
        marksAwarded=marks,
        normalizedScore=round(hybrid, 4),
        tfidfSimilarity=sim.tfidf_similarity,
        semanticSimilarity=sim.semantic_similarity,
        conceptCoverage=coverage,
        hybridScore=round(hybrid, 4),
        matchedConcepts=matched,
        missingConcepts=missing,
        contradictedConcepts=contradicted,
        conceptDetails=concept_details,
        ocrConfidence=req.ocrConfidence,
        segmentationConfidence=req.segmentationConfidence,
        overallConfidence=overall_conf,
        confidenceLevel=level,
        confidenceBreakdown=breakdown,
        status=status,
        reviewReasons=reasons,
        feedback=feedback,
        explanation=explanation,
    )
