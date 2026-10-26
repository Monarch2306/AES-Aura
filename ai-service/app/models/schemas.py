from pydantic import BaseModel, Field, field_validator, model_validator
from typing import Optional, Literal


# ---------- Rubric definitions (Phase 6) ----------

class RubricConcept(BaseModel):
    concept: str = Field(min_length=1, max_length=200)
    weight: float = Field(1.0, gt=0, le=100, description="Relative importance, > 0")
    required: bool = Field(False, description="Minimum-required concept")
    negative: bool = Field(False, description="Contradictory/penalty concept")


class Rubric(BaseModel):
    questionNumber: int = Field(gt=0)
    maxMarks: float = Field(gt=0, le=1000)
    referenceAnswer: str = Field(min_length=1, max_length=20000)
    concepts: list[RubricConcept] = Field(min_length=1, max_length=100)
    minRequiredConcepts: int = Field(0, ge=0, le=100)
    weightSemantic: Optional[float] = None   # override global default
    weightConceptCoverage: Optional[float] = Field(None, ge=0, le=1)

    @field_validator("weightSemantic")
    @classmethod
    def validate_semantic_weight(cls, value):
        if value is not None and not 0 <= value <= 1:
            raise ValueError("weightSemantic must be between 0 and 1")
        return value

    @model_validator(mode="after")
    def validate_concept_requirements(self):
        if self.minRequiredConcepts > len(self.concepts):
            raise ValueError("minRequiredConcepts cannot exceed number of concepts")
        return self


# ---------- OCR (Phase 4) ----------

class WordBox(BaseModel):
    text: str
    confidence: float
    left: int
    top: int
    width: int
    height: int


class OcrResult(BaseModel):
    text: str
    confidence: float
    lines: list[str]
    words: list[WordBox]
    bestPreprocessing: str
    needsReview: bool


# ---------- Segmentation (Phase 5) ----------

class SegmentedAnswer(BaseModel):
    questionNumber: int
    studentAnswer: str
    ocrConfidence: float
    boundingBox: Optional[dict] = None


class SegmentationResult(BaseModel):
    answers: list[SegmentedAnswer]
    pattern: str


# ---------- Image quality ----------

class ImageQuality(BaseModel):
    qualityScore: float = Field(ge=0, le=100)
    blurScore: float = Field(ge=0, le=100)
    brightnessScore: float = Field(ge=0, le=100)
    contrastScore: float = Field(ge=0, le=100)
    resolutionWidth: int = Field(gt=0)
    resolutionHeight: int = Field(gt=0)
    rotationDetected: bool
    warnings: list[str]


# ---------- Evaluation (Phases 7-10) ----------

class EvaluateAnswerRequest(BaseModel):
    studentAnswer: str = Field(max_length=50000)
    ocrConfidence: float = Field(100.0, ge=0, le=100)
    rubric: Rubric
    segmentationConfidence: float = Field(100.0, ge=0, le=100)


class ConceptMatch(BaseModel):
    concept: str
    matched: bool
    contradicted: bool
    weight: float
    negative: bool = False
    matchedTextSpan: Optional[str] = None
    fuzzyScore: Optional[float] = None


class EvaluationResult(BaseModel):
    questionNumber: int
    maxMarks: float
    marksAwarded: float
    normalizedScore: float

    tfidfSimilarity: float
    semanticSimilarity: float
    conceptCoverage: float
    hybridScore: float

    matchedConcepts: list[str]
    missingConcepts: list[str]
    contradictedConcepts: list[str]
    conceptDetails: list[ConceptMatch]

    ocrConfidence: float
    segmentationConfidence: float
    overallConfidence: float
    confidenceLevel: Literal["HIGH", "MEDIUM", "LOW"]
    confidenceBreakdown: dict

    status: Literal["AUTO_EVALUATED", "NEEDS_REVIEW"]
    reviewReasons: list[str]
    feedback: str
    explanation: dict


class BatchEvaluateRequest(BaseModel):
    items: list[EvaluateAnswerRequest] = Field(min_length=1, max_length=100)


class BatchEvaluateResponse(BaseModel):
    results: list[EvaluationResult]


# ---------- Full pipeline (Phase 3, /process-answer-sheet) ----------

class ProcessSheetResponse(BaseModel):
    ocr: OcrResult
    segmentation: SegmentationResult
    imageQuality: ImageQuality
