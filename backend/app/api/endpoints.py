import os
import logging
from fastapi import APIRouter, File, UploadFile, HTTPException, status
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.schemas.analysis import AnalysisResponse, HealthResponse, ModelsResponse
from app.services.pipeline import pipeline
from app.services.detector import detector_service
from app.services.classifier import classifier_service
from app.services.color_extractor import COLOR_PALETTE

logger = logging.getLogger(__name__)
router = APIRouter()

ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/webp", "image/bmp"}

@router.post(
    "/analyze",
    response_model=AnalysisResponse,
    summary="Analyze vehicle image",
    description="Upload a car photo to detect vehicles, estimate exterior colour, and classify make and model."
)
async def analyze_vehicle_image(file: UploadFile = File(...)):
    # Validate content type
    if file.content_type not in ALLOWED_MIME_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"Unsupported file type '{file.content_type}'. Supported: JPEG, PNG, WEBP."
        )

    # Read bytes with file size guard
    max_bytes = settings.max_image_size_mb * 1024 * 1024
    image_bytes = await file.read()

    if len(image_bytes) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Uploaded file is empty."
        )

    if len(image_bytes) > max_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"Image size exceeds the {settings.max_image_size_mb} MB limit."
        )

    try:
        response = pipeline.process_image(image_bytes)
        return response
    except Exception as e:
        logger.error(f"Unexpected error analyzing image: {e}", exc_info=True)
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "success": False,
                "vehicles": [],
                "processing_time_ms": 0.0,
                "error": "Internal AI pipeline error processing image. Please try again."
            }
        )

@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health and AI models readiness check"
)
async def health_check():
    import torch
    device_name = "CUDA (GPU)" if torch.cuda.is_available() else "CPU"

    models_dir = settings.models_dir
    models_dir_exists = os.path.isdir(models_dir)

    return HealthResponse(
        status="ready" if detector_service.ready else "degraded",
        version=settings.version,
        device=device_name,
        detector_ready=detector_service.ready,
        classifier_ready=classifier_service.weights_loaded,
        models_dir_exists=models_dir_exists,
        weights_path=settings.classifier_weights_file
    )

@router.get(
    "/models",
    response_model=ModelsResponse,
    summary="Details on available AI models and classes"
)
async def list_models():
    return ModelsResponse(
        detector=getattr(detector_service, "model_type", "yolo_detector"),
        classifier="VehicleMakeModelClassifier (ResNet50 Backbone)",
        weights_loaded=classifier_service.weights_loaded,
        num_classes=len(classifier_service.classes),
        classes_sample=classifier_service.classes[:10],
        color_labels=list(COLOR_PALETTE.keys())
    )
