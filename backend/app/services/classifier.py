import os
import re
import logging
from typing import Dict, Any, List, Optional
import numpy as np

from app.models.classifier import STANFORD_CARS_CLASSES, VehicleMakeModelClassifier
from app.core.config import settings

logger = logging.getLogger(__name__)

class MakeModelService:
    def __init__(self):
        self.classes: List[str] = STANFORD_CARS_CLASSES
        self.weights_loaded: bool = False
        self.model = None
        self.device = "cpu"
        self._init_model()

    def _init_model(self):
        try:
            import torch
            self.device = "cuda" if torch.cuda.is_available() else "cpu"
            weights_path = settings.classifier_weights_file

            if os.path.exists(weights_path):
                logger.info(f"Loading vehicle classifier weights from: {weights_path}")
                self.model = VehicleMakeModelClassifier(num_classes=len(self.classes))
                state_dict = torch.load(weights_path, map_location=self.device)
                self.model.load_state_dict(state_dict)
                self.model.to(self.device)
                self.model.eval()
                self.weights_loaded = True
                logger.info("Classifier weights loaded successfully.")
            else:
                logger.warning(
                    f"Classifier weights file not found at '{weights_path}'. "
                    "Make/model identification will return 'Unable to identify confidently' "
                    "until weights are trained or provided via scripts/train_classifier.py."
                )
                self.weights_loaded = False
        except Exception as e:
            logger.error(f"Error initializing classifier: {e}")
            self.weights_loaded = False

    @staticmethod
    def parse_make_and_model(class_name: str) -> tuple[str, str, Optional[str]]:
        """
        Parses class label like 'Audi TT RS Coupe 2012' into (Make: 'Audi', Model: 'TT RS Coupe', Year: '2012').
        """
        parts = class_name.strip().split()
        if not parts:
            return "Unknown", "Unknown", None

        # Check if last element is a 4-digit year
        year = None
        if len(parts) > 1 and re.match(r"^\d{4}$", parts[-1]):
            year = parts[-1]
            parts = parts[:-1]

        # Multi-word makes (Aston Martin, Land Rover, Alfa Romeo, Mercedes-Benz)
        if len(parts) >= 2 and parts[0] in ["Aston", "Land", "Alfa", "AM"]:
            make = f"{parts[0]} {parts[1]}"
            model = " ".join(parts[2:]) if len(parts) > 2 else "Model"
        else:
            make = parts[0]
            model = " ".join(parts[1:]) if len(parts) > 1 else "Unknown"

        return make, model, year

    def predict(self, vehicle_bgr: np.ndarray) -> Dict[str, Any]:
        """
        Predicts vehicle make and model from cropped vehicle image.
        If weights are not available, returns explicit fallback status without fabricating results.
        """
        if not self.weights_loaded or self.model is None:
            return {
                "make": "Unable to identify confidently",
                "model": "Weights not configured",
                "classification_confidence": 0.0,
                "status": "weights_not_configured",
                "top_candidates": []
            }

        try:
            import cv2
            import torch
            from torchvision import transforms

            preprocess = transforms.Compose([
                transforms.ToPILImage(),
                transforms.Resize((224, 224)),
                transforms.ToTensor(),
                transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                     std=[0.229, 0.224, 0.225])
            ])

            rgb = cv2.cvtColor(vehicle_bgr, cv2.COLOR_BGR2RGB)
            input_tensor = preprocess(rgb).unsqueeze(0).to(self.device)

            with torch.no_grad():
                logits = self.model(input_tensor)
                probs = torch.softmax(logits, dim=1)[0]
                topk_probs, topk_indices = torch.topk(probs, k=min(3, len(self.classes)))

            top_prob = float(topk_probs[0].item())
            top_idx = int(topk_indices[0].item())
            predicted_label = self.classes[top_idx]

            make, model, _ = self.parse_make_and_model(predicted_label)

            candidates = []
            for prob, idx in zip(topk_probs, topk_indices):
                c_label = self.classes[int(idx.item())]
                c_make, c_model, _ = self.parse_make_and_model(c_label)
                candidates.append({
                    "make": c_make,
                    "model": c_model,
                    "confidence": round(float(prob.item()), 3)
                })

            status = "identified" if top_prob >= settings.confidence_threshold else "unconfident"
            if status == "unconfident":
                make = "Unable to identify confidently"
                model = f"Candidate: {model}"

            return {
                "make": make,
                "model": model,
                "classification_confidence": round(top_prob, 3),
                "status": status,
                "top_candidates": candidates
            }

        except Exception as e:
            logger.error(f"Inference error in classifier: {e}")
            return {
                "make": "Unknown",
                "model": "Inference error",
                "classification_confidence": 0.0,
                "status": "error",
                "top_candidates": []
            }

classifier_service = MakeModelService()
