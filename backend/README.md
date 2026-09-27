# AutoVision AI — Computer Vision Backend

AutoVision AI backend provides high-performance car recognition via FastAPI, PyTorch, and OpenCV.

## Pipeline Architecture
1. **Image Preprocessing**: EXIF orientation normalization, dimension scaling up to 1920px preserving aspect ratio.
2. **Vehicle Detection**: Object detection (YOLOv8 / TorchVision MobileNet SSDLite / Salient region detector) identifying bounding boxes for `car`, `truck`, `bus`.
3. **Make & Model Classification**: Deep Convolutional Neural Network (ResNet-50 backbone) evaluated on 196 vehicle classes from the Stanford Cars benchmark. Returns top predictions and confidence. When weights are not installed, returns explicit status without fabricated predictions.
4. **Exterior Colour Estimation**: OpenCV color analysis in HSV and CIELAB color spaces. Crops vehicle center, excludes windshield/roof, tyres, and road shadow artifacts. Matches dominant hue & saturation to standard paint finishes (Black, White, Silver, Grey, Red, Electric Blue, Navy, etc.) with representative hex codes.
5. **Annotation & Response**: Generates normalized bounding box coordinates and returns an annotated image base64 data URI alongside structured vehicle metadata.

---

## Quickstart

### 1. Requirements
- Python 3.9+
- pip

```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Run Server
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Interactive API documentation available at: `http://localhost:8000/docs`

---

## API Endpoints

### `POST /api/v1/analyze`
Accepts `multipart/form-data` with `file`:
```json
{
  "success": true,
  "vehicles": [
    {
      "vehicle_id": 1,
      "bounding_box": {
        "ymin": 0.22,
        "xmin": 0.15,
        "ymax": 0.78,
        "xmax": 0.85
      },
      "detection_confidence": 0.94,
      "make": "BMW",
      "model": "3 Series Sedan",
      "classification_confidence": 0.89,
      "colour": "Electric Blue",
      "colour_hex": "#2563EB",
      "status": "identified"
    }
  ],
  "annotated_image": "data:image/jpeg;base64,...",
  "processing_time_ms": 142.5
}
```

### `GET /api/v1/health`
Checks server readiness, device (CPU/CUDA), and model weights status.

### `GET /api/v1/models`
Returns list of supported classes and active detector configuration.

---

## Dataset Preparation & Model Training

To fine-tune the classifier on the Stanford Cars dataset:
```bash
# 1. Download & extract dataset
python scripts/prepare_dataset.py --download

# 2. Train model
python scripts/train_classifier.py --epochs 15 --batch-size 32
```
Trained weights will be saved to `models/car_classifier_resnet50.pth`.

---

## Android Device Connection Guide

### Android Emulator
The Android emulator connects to your host machine's localhost via the special IP alias:
```
http://10.0.2.2:8000
```
This is configured as the default URL in the AutoVision AI Android application.

### Physical Android Device
1. Connect your Android phone and host development machine to the same Wi-Fi network.
2. Find your host machine's local IP address (`ipconfig` on Windows, `ifconfig` or `ip a` on macOS/Linux), e.g. `192.168.1.50`.
3. In the AutoVision AI Android app, tap the Settings icon on the top bar and enter:
```
http://192.168.1.50:8000
```
4. Tap **Test Ping** to confirm connectivity.

---

## Licenses & Attributions
- **Stanford Cars Dataset**: Jonathan Krause, Michael Stark, Jia Deng, Li Fei-Fei. 3D Object Representations for Fine-Grained Categorization. 4th IEEE Workshop on 3D Representation and Recognition (3dRR-11), ICCV 2011.
- **YOLOv8**: Ultralytics AGPL-3.0 License.
- **PyTorch & Torchvision**: BSD-style license.
- **OpenCV**: Apache 2.0 License.
