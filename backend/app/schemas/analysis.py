from typing import List, Optional
from pydantic import BaseModel, Field

class BoundingBox(BaseModel):
    """Normalized coordinates: 0.0 to 1.0 relative to image dimensions."""
    ymin: float = Field(..., description="Top coordinate normalized [0, 1]")
    xmin: float = Field(..., description="Left coordinate normalized [0, 1]")
    ymax: float = Field(..., description="Bottom coordinate normalized [0, 1]")
    xmax: float = Field(..., description="Right coordinate normalized [0, 1]")

class CandidateModel(BaseModel):
    make: str
    model: str
    confidence: float

class Vehicle(BaseModel):
    vehicle_id: int
    bounding_box: BoundingBox
    detection_confidence: float
    make: str
    model: str
    classification_confidence: float
    colour: str
    colour_hex: str
    status: str = Field("detected", description="Status e.g. identified, unconfident, no_weights")
    top_candidates: List[CandidateModel] = Field(default_factory=list)
    body_type: Optional[str] = None

class AnalysisResponse(BaseModel):
    success: bool
    vehicles: List[Vehicle] = Field(default_factory=list)
    annotated_image: Optional[str] = None
    processing_time_ms: float
    image_width: int = 0
    image_height: int = 0
    error: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    version: str
    device: str
    detector_ready: bool
    classifier_ready: bool
    models_dir_exists: bool
    weights_path: str

class ModelsResponse(BaseModel):
    detector: str
    classifier: str
    weights_loaded: bool
    num_classes: int
    classes_sample: List[str]
    color_labels: List[str]
