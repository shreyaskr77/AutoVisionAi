import io
import time
import base64
import logging
from typing import Tuple, List, Dict, Any, Optional

import cv2
import numpy as np
from PIL import Image, ImageOps

from app.core.config import settings
from app.services.detector import detector_service
from app.services.classifier import classifier_service
from app.services.color_extractor import ColorExtractor
from app.schemas.analysis import AnalysisResponse, Vehicle, BoundingBox, CandidateModel

logger = logging.getLogger(__name__)

class VisionPipeline:
    """
    End-to-End Car Recognition Pipeline:
    1. Preprocessing (EXIF orientation, resizing, validation)
    2. Vehicle Detection (YOLO / TorchVision)
    3. Make & Model Classification (Stanford Cars ResNet)
    4. Exterior Colour Estimation (OpenCV HSV/Lab body analysis)
    5. Result Visualization & Annotation
    """

    def process_image(self, image_bytes: bytes) -> AnalysisResponse:
        start_time = time.time()

        # Step 1: Preprocessing
        try:
            pil_img = Image.open(io.BytesIO(image_bytes))
            # Auto-rotate based on EXIF tag
            pil_img = ImageOps.exif_transpose(pil_img)
            # Convert to RGB (handles RGBA, Palette, Grayscale)
            pil_img = pil_img.convert("RGB")

            # Max dimension clamp (e.g. 1920) preserving aspect ratio
            max_dim = 1920
            w, h = pil_img.size
            if max(w, h) > max_dim:
                scale = max_dim / float(max(w, h))
                new_w, new_h = int(w * scale), int(h * scale)
                pil_img = pil_img.resize((new_w, new_h), Image.Resampling.LANCZOS)

            # Convert to OpenCV BGR
            image_rgb = np.array(pil_img)
            image_bgr = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2BGR)
            img_h, img_w = image_bgr.shape[:2]

        except Exception as e:
            logger.error(f"Image preprocessing error: {e}")
            return AnalysisResponse(
                success=False,
                vehicles=[],
                processing_time_ms=round((time.time() - start_time) * 1000, 2),
                error="Invalid or corrupted image format. Please supply JPEG, PNG, or WebP."
            )

        # Step 2: Vehicle Detection
        raw_detections = detector_service.detect(image_bgr)

        if not raw_detections:
            return AnalysisResponse(
                success=True,
                vehicles=[],
                annotated_image=self._encode_image_base64(image_bgr),
                processing_time_ms=round((time.time() - start_time) * 1000, 2),
                image_width=img_w,
                image_height=img_h,
                error="No vehicles detected in this image."
            )

        annotated_bgr = image_bgr.copy()
        vehicles: List[Vehicle] = []

        # Color definitions for bounding boxes
        box_colors = [
            (59, 130, 246),  # Electric blue (BGR: 246, 130, 59 -> RGB: 59, 130, 246)
            (16, 185, 129),  # Emerald green
            (245, 158, 11),  # Amber
            (168, 85, 247),  # Purple
        ]

        # Step 3 & 4: Make/Model Classification and Exterior Colour per vehicle
        for idx, det in enumerate(raw_detections):
            v_id = det["vehicle_id"]
            box_data = det["bounding_box"]
            det_conf = det["confidence"]
            crop_bgr = det["crop"]

            # Step 3: Make & Model
            cls_result = classifier_service.predict(crop_bgr)

            # Step 4: Colour Estimation
            color_result = ColorExtractor.estimate_color(crop_bgr)

            candidates = [
                CandidateModel(
                    make=c["make"],
                    model=c["model"],
                    confidence=c["confidence"]
                ) for c in cls_result.get("top_candidates", [])
            ]

            vehicle = Vehicle(
                vehicle_id=v_id,
                bounding_box=BoundingBox(
                    ymin=box_data["ymin"],
                    xmin=box_data["xmin"],
                    ymax=box_data["ymax"],
                    xmax=box_data["xmax"]
                ),
                detection_confidence=det_conf,
                make=cls_result["make"],
                model=cls_result["model"],
                classification_confidence=cls_result["classification_confidence"],
                colour=color_result["colour"],
                colour_hex=color_result["colour_hex"],
                status=cls_result["status"],
                top_candidates=candidates
            )
            vehicles.append(vehicle)

            # Draw visual bounding box and label
            self._draw_vehicle_annotation(
                annotated_bgr,
                box_data,
                v_id,
                vehicle.make,
                vehicle.model,
                vehicle.colour,
                det_conf,
                box_colors[idx % len(box_colors)]
            )

        elapsed_ms = round((time.time() - start_time) * 1000, 2)
        annotated_base64 = self._encode_image_base64(annotated_bgr)

        return AnalysisResponse(
            success=True,
            vehicles=vehicles,
            annotated_image=annotated_base64,
            processing_time_ms=elapsed_ms,
            image_width=img_w,
            image_height=img_h
        )

    def _draw_vehicle_annotation(
        self,
        image_bgr: np.ndarray,
        box: Dict[str, float],
        v_id: int,
        make: str,
        model: str,
        colour: str,
        confidence: float,
        color_rgb: Tuple[int, int, int]
    ):
        h, w = image_bgr.shape[:2]
        x1 = int(box["xmin"] * w)
        y1 = int(box["ymin"] * h)
        x2 = int(box["xmax"] * w)
        y2 = int(box["ymax"] * h)

        bgr_color = (color_rgb[2], color_rgb[1], color_rgb[0])

        # Draw main box with rounded aesthetics or corner accents
        cv2.rectangle(image_bgr, (x1, y1), (x2, y2), bgr_color, 2, cv2.LINE_AA)

        # Draw reticle corners
        corner_len = min(24, int((x2 - x1) * 0.15))
        # Top-left
        cv2.line(image_bgr, (x1, y1), (x1 + corner_len, y1), bgr_color, 4, cv2.LINE_AA)
        cv2.line(image_bgr, (x1, y1), (x1, y1 + corner_len), bgr_color, 4, cv2.LINE_AA)
        # Top-right
        cv2.line(image_bgr, (x2, y1), (x2 - corner_len, y1), bgr_color, 4, cv2.LINE_AA)
        cv2.line(image_bgr, (x2, y1), (x2, y1 + corner_len), bgr_color, 4, cv2.LINE_AA)
        # Bottom-left
        cv2.line(image_bgr, (x1, y2), (x1 + corner_len, y2), bgr_color, 4, cv2.LINE_AA)
        cv2.line(image_bgr, (x1, y2), (x1, y2 - corner_len), bgr_color, 4, cv2.LINE_AA)
        # Bottom-right
        cv2.line(image_bgr, (x2, y2), (x2 - corner_len, y2), bgr_color, 4, cv2.LINE_AA)
        cv2.line(image_bgr, (x2, y2), (x2, y2 - corner_len), bgr_color, 4, cv2.LINE_AA)

        # Draw top label badge
        label = f"#{v_id} {colour} | {int(confidence * 100)}%"
        if make not in ["Unable to identify confidently", "Unknown"]:
            label = f"#{v_id} {make} {model} | {colour}"

        font = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 0.55
        thickness = 1
        (label_w, label_h), baseline = cv2.getTextSize(label, font, font_scale, thickness)

        badge_y1 = max(0, y1 - label_h - 10)
        badge_y2 = y1
        badge_x1 = x1
        badge_x2 = min(w, x1 + label_w + 14)

        # Dark background pill for label
        cv2.rectangle(image_bgr, (badge_x1, badge_y1), (badge_x2, badge_y2), (11, 18, 32), -1)
        cv2.rectangle(image_bgr, (badge_x1, badge_y1), (badge_x2, badge_y2), bgr_color, 1)

        # White text inside badge
        cv2.putText(
            image_bgr,
            label,
            (badge_x1 + 6, badge_y2 - 6),
            font,
            font_scale,
            (248, 250, 252),
            thickness,
            cv2.LINE_AA
        )

    @staticmethod
    def _encode_image_base64(image_bgr: np.ndarray) -> str:
        success, buffer = cv2.imencode(".jpg", image_bgr, [int(cv2.IMWRITE_JPEG_QUALITY), 88])
        if not success:
            return ""
        b64 = base64.b64encode(buffer).decode("utf-8")
        return f"data:image/jpeg;base64,{b64}"

pipeline = VisionPipeline()
