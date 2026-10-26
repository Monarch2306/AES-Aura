"""
Phase 4: OCR via Tesseract, run across several preprocessing variants
(Phase 3 output), selecting the highest-confidence result. Never silently
accepts a poor OCR result - callers get `needsReview=True` when mean
confidence is below the configured threshold.
"""
from __future__ import annotations
import pytesseract
from pytesseract import Output
import numpy as np

from app.core.config import settings
from app.models.schemas import OcrResult, WordBox

if settings.TESSERACT_CMD:
    pytesseract.pytesseract.tesseract_cmd = settings.TESSERACT_CMD


def _run_tesseract(image: np.ndarray, psm: int) -> dict:
    config = f"--oem 1 --psm {psm} -c tessedit_do_invert=0"
    return pytesseract.image_to_data(image, config=config, output_type=Output.DICT)


def _score_result(data: dict) -> float:
    confs = [float(c) for c in data.get("conf", []) if c not in ("-1", -1)]
    if not confs:
        return 0.0
    return sum(confs) / len(confs)


def _extract(data: dict) -> tuple[str, list[str], list[WordBox]]:
    words: list[WordBox] = []
    n = len(data.get("text", []))
    lines_map: dict[tuple, list[str]] = {}
    for i in range(n):
        txt = data["text"][i].strip()
        conf = data["conf"][i]
        if txt == "" or conf in ("-1", -1):
            continue
        conf_f = float(conf)
        words.append(WordBox(
            text=txt, confidence=conf_f,
            left=int(data["left"][i]), top=int(data["top"][i]),
            width=int(data["width"][i]), height=int(data["height"][i]),
        ))
        key = (data["block_num"][i], data["par_num"][i], data["line_num"][i])
        lines_map.setdefault(key, []).append(txt)

    lines = [" ".join(v) for v in lines_map.values()]
    full_text = "\n".join(lines)
    return full_text, lines, words


def run_ocr_best_of(variants: dict[str, np.ndarray]) -> OcrResult:
    """Runs OCR against each preprocessing variant x candidate PSM mode and
    keeps the highest-confidence result (Phase 4)."""
    best_score = -1.0
    best_payload = None
    best_label = ""

    from concurrent.futures import ThreadPoolExecutor, as_completed

    tasks = []
    for variant_name, image in variants.items():
        for psm in settings.OCR_PSM_CANDIDATES:
            tasks.append((variant_name, psm, image))
            
    with ThreadPoolExecutor(max_workers=4) as executor:
        future_to_info = {
            executor.submit(_run_tesseract, img, psm): (vname, psm) 
            for vname, psm, img in tasks
        }
        
        for future in as_completed(future_to_info):
            vname, psm = future_to_info[future]
            try:
                data = future.result()
                score = _score_result(data)
                if score > best_score:
                    best_score = score
                    best_payload = data
                    best_label = f"{vname}@psm{psm}"
            except pytesseract.TesseractError:
                continue

    if best_payload is None:
        return OcrResult(
            text="", confidence=0.0, lines=[], words=[],
            bestPreprocessing="none", needsReview=True,
        )

    full_text, lines, words = _extract(best_payload)
    needs_review = best_score < settings.OCR_LOW_CONFIDENCE_THRESHOLD or not full_text.strip()

    return OcrResult(
        text=full_text,
        confidence=round(best_score, 2),
        lines=lines,
        words=words,
        bestPreprocessing=best_label,
        needsReview=needs_review,
    )
