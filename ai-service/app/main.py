from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import logging
import time
import uuid

from app.api.routes import router
from app.core.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(settings.SERVICE_NAME)

app = FastAPI(
    title="AI Smart Answer Sheet Evaluation Service",
    description="Computer Vision + OCR + NLP evaluation pipeline. "
                 "Teacher-assisted, not a teacher replacement. "
                 "No accuracy figure here is claimed to be 100%.",
    version="1.0.0",
)

cors_origins = [x.strip() for x in settings.CORS_ALLOWED_ORIGINS.split(",") if x.strip()]
if cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=cors_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Content-Type", "X-Request-Id"],
    )


@app.middleware("http")
async def add_request_id_and_logging(request: Request, call_next):
    request_id = request.headers.get("X-Request-Id", str(uuid.uuid4()))
    start = time.time()
    try:
        response = await call_next(request)
    except Exception:
        logger.exception("Unhandled error request_id=%s path=%s", request_id, request.url.path)
        return JSONResponse(
            status_code=500,
            content={"error": "internal_error", "requestId": request_id},
        )
    duration_ms = round((time.time() - start) * 1000, 2)
    response.headers["X-Request-Id"] = request_id
    logger.info("request_id=%s path=%s status=%s duration_ms=%s",
                request_id, request.url.path, response.status_code, duration_ms)
    return response


app.include_router(router, prefix="")


@app.get("/")
def root():
    return {"service": settings.SERVICE_NAME, "status": "running"}
