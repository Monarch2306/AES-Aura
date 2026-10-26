from __future__ import annotations
import time
from fastapi import APIRouter, UploadFile, File, HTTPException, Form
from typing import Optional
import json

from app.models.schemas import (
    OcrResult, SegmentationResult, ProcessSheetResponse,
    EvaluateAnswerRequest, EvaluationResult, BatchEvaluateRequest, BatchEvaluateResponse,
)
from app.services import preprocessing, ocr as ocr_service, segmentation, evaluation
from app.core.config import settings

router = APIRouter()


@router.get("/health")
def health():
    return {
        "status": "ok",
        "service": settings.SERVICE_NAME,
        "env": settings.ENV,
        "time": time.time(),
    }


@router.get("/config")
def get_config():
    """Exposes the current tunable configuration (Phase 7 requirement:
    weights must be configurable and visible, never hidden magic numbers)."""
    return settings.model_dump()


def _read_and_preprocess(raw_bytes: bytes):
    try:
        img = preprocessing.validate_image(raw_bytes)
    except preprocessing.ImageValidationError as e:
        raise HTTPException(status_code=422, detail=str(e))
    quality = preprocessing.analyze_image_quality(img)
    variants, was_corrected = preprocessing.build_preprocessing_variants(img)
    return variants, was_corrected, quality


@router.post("/ocr", response_model=OcrResult)
async def ocr_endpoint(file: UploadFile = File(...)):
    raw = await file.read()
    variants, _, _ = _read_and_preprocess(raw)
    return ocr_service.run_ocr_best_of(variants)


@router.post("/process-answer-sheet", response_model=ProcessSheetResponse)
async def process_answer_sheet(
    file: UploadFile = File(...),
    expectedQuestions: Optional[str] = Form(None),
):
    raw = await file.read()
    variants, was_corrected, quality = _read_and_preprocess(raw)
    ocr_result = ocr_service.run_ocr_best_of(variants)

    expected = None
    if expectedQuestions:
        try:
            expected = json.loads(expectedQuestions)
        except json.JSONDecodeError:
            expected = None

    seg_result = segmentation.segment_answers(ocr_result, expected)
    return ProcessSheetResponse(ocr=ocr_result, segmentation=seg_result, imageQuality=quality)


@router.post("/evaluate-answer", response_model=EvaluationResult)
def evaluate_answer_endpoint(req: EvaluateAnswerRequest):
    if req.rubric.maxMarks <= 0:
        raise HTTPException(status_code=422, detail="rubric.maxMarks must be > 0")
    return evaluation.evaluate_answer(req)


@router.post("/evaluate-batch", response_model=BatchEvaluateResponse)
def evaluate_batch_endpoint(req: BatchEvaluateRequest):
    results = [evaluation.evaluate_answer(item) for item in req.items]
    return BatchEvaluateResponse(results=results)
