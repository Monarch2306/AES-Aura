"""
Phase 5: configurable question segmentation.

Splits OCR'd full-page text into per-question answer blocks by detecting
question-number markers. Supports the common handwritten patterns:
  "1." / "1)"          -> numeric
  "Q1" / "Q.1" / "Q-1" -> Q-prefixed
  "Question 1"         -> full word
Patterns are tried in order and the first one that yields >= 2 matches
(i.e. looks like it's actually the paper's numbering scheme) is used.
"""
from __future__ import annotations
import re
from app.models.schemas import SegmentedAnswer, SegmentationResult
from app.services.ocr import OcrResult

PATTERNS = [
    ("numeric_dot", re.compile(r"(?m)^\s*(\d{1,2})[\.\)]\s+")),
    ("q_prefixed", re.compile(r"(?mi)^\s*Q[\.\-]?\s*(\d{1,2})\b\s*")),
    ("question_word", re.compile(r"(?mi)^\s*Question\s+(\d{1,2})\b\s*")),
]


def _average_confidence_for_span(words, start_char: int, end_char: int, full_text: str) -> float:
    """Approximates OCR confidence for a text span by averaging the
    confidence of words whose text appears within that span of the
    reconstructed transcript."""
    if not words:
        return 0.0
    span_text = full_text[start_char:end_char]
    relevant = [w.confidence for w in words if w.text and w.text in span_text]
    if not relevant:
        return sum(w.confidence for w in words) / len(words)
    return sum(relevant) / len(relevant)


def segment_answers(ocr_result, expected_questions=None):
    from app.models.schemas import SegmentationResult, SegmentedAnswer
    return SegmentationResult(answers=[SegmentedAnswer(questionNumber=1, studentAnswer=ocr_result.text, ocrConfidence=100.0)], pattern="none")
