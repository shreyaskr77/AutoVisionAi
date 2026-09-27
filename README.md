# AutoVision AI — Android Car Recognition Application

AutoVision AI is a production-grade automotive computer vision platform featuring an Android client built in modern Jetpack Compose with Material 3, alongside a Python FastAPI deep learning backend for vehicle detection, make and model identification, and exterior paint colour estimation.

---

## 1. System Architecture

```
┌────────────────────────────────────────────────────────┐
│               Android Application (Client)            │
│  - Jetpack Compose + Material 3 (Deep Navy Theme)     │
│  - CameraX & Android Photo Picker (zero-perm photo)   │
│  - Retrofit + OkHttp (Multipart Image Upload)          │
│  - Interactive Bounding Box Visualizer & Multi-Car Nav │
│  - Room Database (Local Scan Persistence)              │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP Multipart POST
                            ▼
┌────────────────────────────────────────────────────────┐
│             FastAPI Python Backend (AI Engine)         │
│  1. Preprocessing: EXIF orientation & resolution scale │
│  2. Detection: YOLO / MobileNet object detector        │
│  3. Classification: Stanford Cars CNN (ResNet-50)      │
│  4. Colour: OpenCV HSV & CIELAB body panel estimation  │
│  5. Response: JSON payload + Annotated Image Base64    │
└────────────────────────────────────────────────────────┘
```

---

## 2. Features

- **Automotive Design System**: Deep Navy (`#0B1220`), Electric Blue (`#3B82F6`), Neon Cyan (`#38BDF8`), with crisp typography and subtle borders.
- **Multi-Car Support**: Detects multiple vehicles in a single frame. Provides interactive bounding boxes on canvas and car switcher chips.
- **Exterior Paint Estimation**: OpenCV isolates vehicle sheet metal, excluding windshield, wheels, and ground shadows to identify automotive paint shades (e.g. Electric Blue, Silver, Maroon, etc.) with exact RGB hexadecimal codes.
- **Fine-Grained Classification**: Stanford Cars benchmark model supporting 196 vehicle classes. Reports top predictions, confidence percentages, and top candidates.
- **Zero Mock Fabrications**: Honest AI pipeline. If weights are not installed or confidence is low, clearly states "Unable to identify confidently" rather than inventing predictions.
- **Local Persistence**: Integrated Room database persists previous vehicle scans, timestamps, color swatches, and confidence scores offline.
- **Dynamic Server URL Configuration**: Seamlessly switch between Android Emulator (`http://10.0.2.2:8000`), physical LAN IPs, or remote cloud servers with an in-app "Test Connection" ping tool.

---

## 3. Directory Layout

```
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Navigation host & edge-to-edge
│   │   ├── data/
│   │   │   ├── local/                    # Room Database, DAO, Entity
│   │   │   ├── remote/                   # Retrofit API & Moshi DTOs
│   │   │   └── repository/               # VehicleRepository & Settings
│   │   ├── domain/model/                 # Vehicle, BoundingBox, Analysis
│   │   ├── ui/
│   │   │   ├── components/               # TopBar, BoundingBoxOverlay, Gauges
│   │   │   ├── screens/                  # Splash, Home, Preview, Analysis, Results, History, About
│   │   │   └── theme/                    # Navy Color, M3 Theme, Typography
│   │   └── viewmodel/                    # AutoVisionViewModel
│   └── src/main/res/                     # Adaptive Icons, Strings, Layout XML
├── backend/
│   ├── app/
│   │   ├── api/endpoints.py              # /analyze, /health, /models
│   │   ├── core/config.py                # Environment configuration
│   │   ├── models/classifier.py          # PyTorch ResNet-50 architecture
│   │   ├── schemas/analysis.py           # Pydantic request/response schemas
│   │   ├── services/                     # Detector, Classifier, Color, Pipeline
│   │   └── main.py                       # FastAPI entrypoint
│   ├── scripts/
│   │   ├── prepare_dataset.py            # Stanford Cars downloader
│   │   └── train_classifier.py           # PyTorch fine-tuning script
│   ├── tests/                            # Pytest test suites
│   ├── requirements.txt                  # Python dependencies
│   └── README.md                         # Backend technical guide
└── metadata.json
```

---

## 4. Getting Started

### A. Run Python Backend
1. Open a terminal in `backend/`:
   ```bash
   cd backend
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   pip install -r requirements.txt
   ```
2. Start the FastAPI server:
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```
3. Verify server is running by opening:
   - Interactive Swagger API: `http://localhost:8000/docs`
   - Health check: `http://localhost:8000/api/v1/health`

### B. Device Connection Instructions

#### 1. Android Studio Emulator
The Android emulator automatically maps host machine ports through `10.0.2.2`.
- Default setting in AutoVision AI is `http://10.0.2.2:8000`. No configuration needed!

#### 2. Physical Android Device
1. Connect your Android phone and host computer to the **same Wi-Fi network**.
2. Find your computer's local IP address:
   - macOS / Linux: `ifconfig` or `ip a` (e.g. `192.168.1.55`)
   - Windows: `ipconfig` (look for IPv4 address)
3. In the AutoVision AI Android app:
   - Tap the **Settings icon** on the top bar.
   - Enter `http://192.168.1.55:8000` (replacing with your computer's actual IP).
   - Tap **Test Connection** to confirm connectivity.
   - Tap **Save**.

---

## 5. Model Training (Stanford Cars Benchmark)

To fine-tune the vehicle make/model classifier weights:
```bash
cd backend
python scripts/prepare_dataset.py --download
python scripts/train_classifier.py --epochs 15 --batch-size 32
```
Trained weights will be saved to `backend/models/car_classifier_resnet50.pth` and automatically loaded upon server startup.

---

## 6. Testing

### Run Backend Unit & Integration Tests:
```bash
cd backend
pytest
```

### Run Android Unit Tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 7. Building Release APK

```bash
gradle assembleRelease
```
Output APK location:
`app/build/outputs/apk/release/app-release-unsigned.apk`
