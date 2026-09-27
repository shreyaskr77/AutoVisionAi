import io
import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "AutoVision AI" in data["app"]

def test_health_endpoint():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert "status" in data
    assert "detector_ready" in data

def test_models_endpoint():
    response = client.get("/api/v1/models")
    assert response.status_code == 200
    data = response.json()
    assert "num_classes" in data
    assert len(data["color_labels"]) > 5

def test_analyze_empty_file():
    response = client.post(
        "/api/v1/analyze",
        files={"file": ("empty.jpg", b"", "image/jpeg")}
    )
    assert response.status_code == 400

def test_analyze_invalid_mime():
    response = client.post(
        "/api/v1/analyze",
        files={"file": ("test.txt", b"hello world", "text/plain")}
    )
    assert response.status_code == 415

def test_analyze_synthetic_car_image():
    # Create synthetic test car image (600x400)
    img = np.full((400, 600, 3), (35, 35, 35), dtype=np.uint8)
    # Draw blue car body rectangle in center
    cv2.rectangle(img, (150, 150), (450, 280), (220, 90, 30), -1)
    # Windshield
    cv2.rectangle(img, (220, 100), (380, 150), (10, 10, 10), -1)
    # Wheels
    cv2.circle(img, (200, 280), 30, (15, 15, 15), -1)
    cv2.circle(img, (400, 280), 30, (15, 15, 15), -1)

    _, buffer = cv2.imencode(".jpg", img)
    img_bytes = buffer.tobytes()

    response = client.post(
        "/api/v1/analyze",
        files={"file": ("test_car.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert len(data["vehicles"]) >= 1
    v = data["vehicles"][0]
    assert "colour" in v
    assert "bounding_box" in v
    assert "detection_confidence" in v
