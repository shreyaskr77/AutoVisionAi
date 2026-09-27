import os
from pydantic import BaseModel

class Settings(BaseModel):
    app_name: str = "AutoVision AI Backend"
    version: str = "1.0.0"
    host: str = os.getenv("HOST", "0.0.0.0")
    port: int = int(os.getenv("PORT", "8000"))
    debug: bool = os.getenv("DEBUG", "False").lower() in ("true", "1", "yes")
    models_dir: str = os.getenv("MODELS_DIR", "models")
    classifier_weights_file: str = os.getenv("CLASSIFIER_WEIGHTS_FILE", "models/car_classifier_resnet50.pth")
    confidence_threshold: float = float(os.getenv("CONFIDENCE_THRESHOLD", "0.30"))
    max_image_size_mb: int = int(os.getenv("MAX_IMAGE_SIZE_MB", "15"))
    allowed_origins: str = os.getenv("ALLOWED_ORIGINS", "*")

settings = Settings()
