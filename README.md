# SaiFit — AI-Powered Athlete Fitness Evaluation

> **An Android application that uses computer-vision pose estimation to automatically score, verify, and benchmark athlete fitness — no manual counting required.**

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Fitness Tests](#fitness-tests)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend Setup](#backend-setup)
  - [Android App Setup](#android-app-setup)
- [AI Pipeline](#ai-pipeline)
- [API Overview](#api-overview)
- [Screenshots](#screenshots)
- [License](#license)

---

## Overview

SaiFit is a full-stack mobile fitness benchmarking platform built for sports academies and athletic programs. Athletes record their fitness tests on an Android device; the video is uploaded to a Python backend where a **YOLOv8m-Pose** model analyses every frame, detects body keypoints, and automatically counts repetitions or measures performance — all without a human counter.

Admins can monitor athlete submissions, review AI-annotated videos, override results where necessary, and track progress via leaderboards and achievement badges.

---

## Features

### For Athletes
- 🎥 **In-app video recording** with CameraX — record directly inside the app
- 🤖 **AI evaluation** — instant rep counting and form analysis after upload
- 📋 **Skeleton overlay review** — watch the AI-annotated video before submitting
- 📊 **Personal dashboard** — view all past results and best scores
- 🏆 **Leaderboard** — ranked across all athletes
- 🎖️ **Achievement badges** — earned automatically based on performance
- 📂 **Submission queue** — offline-safe queuing with automatic sync

### For Admins
- 👤 **Athlete management** — view full profiles and all test history
- ✅ **Result review & override** — approve or reject AI results manually
- 📈 **Cross-athlete analytics** — compare performance across the entire cohort
- 🔐 **Role-based access control** — separate athlete and admin workflows

### AI & Anti-Cheat
- 🦴 **Pose estimation** using YOLOv8m-Pose (17-keypoint COCO skeleton)
- 🔍 **Authenticity scoring** — tamper detection and movement quality assessment
- 🚩 **Anomaly flagging** — flags low visibility, camera motion, or incomplete attempts
- 📐 **Per-segment confidence** — each rep or shuttle is individually scored

---

## Fitness Tests

| Test | Category | Unit | AI-Evaluated |
|------|----------|------|:---:|
| Height Measurement | Anthropometric | cm | Manual |
| Body Weight | Anthropometric | kg | Manual |
| Sit-ups | Strength | reps | ✅ |
| Push-ups | Strength | reps | ✅ |
| Bodyweight Squats | Endurance | reps | ✅ |
| Vertical Jump | Power | cm | ✅ |
| Shuttle Run (10 × 4 m) | Agility | seconds | ✅ |

All AI-evaluated tests use keypoint angles and spatial features derived from the YOLOv8m-Pose skeleton. Height and Weight are entered manually by an admin.

---

## Tech Stack

### Android App
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository |
| Camera | CameraX |
| Networking | Retrofit 2 + OkHttp |
| Local Storage | Room Database + DataStore |
| Serialisation | Gson |

### Backend
| Layer | Technology |
|-------|-----------|
| Framework | FastAPI |
| Language | Python 3 |
| Database | MongoDB (Motor async driver) |
| Pose Estimation | YOLOv8m-Pose (Ultralytics) |
| Video Processing | OpenCV, FFmpeg (imageio-ffmpeg) |
| Server | Uvicorn (ASGI) |

---

## Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog or newer |
| Python | 3.10+ |
| MongoDB | Local instance **or** MongoDB Atlas |
| Git | Any recent version |

> **YOLOv8m-Pose model weights are not included in this repository** (large binary file).  
> Download `yolov8m-pose.pt` from the [Ultralytics releases page](https://github.com/ultralytics/assets/releases) and place it at `backend/yolov8m-pose.pt` before starting the backend.

---

### Backend Setup

#### Option 1 — Windows one-click launcher (recommended)
```bat
cd backend
start_backend.bat
```
This script will automatically:
1. Create a Python virtual environment
2. Install all dependencies from `requirements.txt`
3. Copy `.env.example` → `.env` (if no `.env` exists)
4. Start the Uvicorn server on `http://0.0.0.0:8000`

#### Option 2 — Manual setup
```bash
cd backend

# Create and activate virtual environment
python -m venv venv
venv\Scripts\activate          # Windows
# source venv/bin/activate     # macOS/Linux

# Install dependencies
pip install -r requirements.txt

# Configure environment
copy .env.example .env         # Windows
# cp .env.example .env         # macOS/Linux
# Edit .env and set MONGODB_URL if using Atlas

# Download model weights
# Place yolov8m-pose.pt in backend/

# Start server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The API will be available at `http://localhost:8000`.  
Interactive docs: `http://localhost:8000/api/v1/openapi.json`

#### `.env` configuration
```env
MONGODB_URL=mongodb://localhost:27017/      # or your Atlas connection string
MONGODB_DB_NAME=saifit_db
```

---

### Android App Setup

1. Open the project root in **Android Studio**
2. Let Gradle sync complete
3. Open `ApiClient.kt` — the default IP is `10.0.2.2` (Android emulator loopback to your PC)
   - For a **physical device**, go to **Settings → Server IP** inside the app and enter your machine's local IP (e.g. `192.168.x.x`)
4. Make sure the backend is running
5. Build and run on an emulator or physical Android device

> The app has an in-app Server IP settings screen — no recompile needed when switching between emulator and physical device.

---

## AI Pipeline

The core AI evaluation runs in `backend/app/api/pose_evaluator.py`.

### How it works

```
Uploaded video
      │
      ▼
 FFmpeg transcode (H.264, OpenCV-compatible)
      │
      ▼
 YOLOv8m-Pose inference (512px, every Nth frame)
      │
      ▼
 17-keypoint skeleton extracted per frame
      │
      ▼
 Exercise-specific feature extraction
 (joint angles, spatial ratios, normalized positions)
      │
      ▼
 Rep detection / movement scoring
 (state machine with debounce + smoothing)
      │
      ▼
 Annotated video written (skeleton overlay + live counter)
      │
      ▼
 Result: { value, segments[], confidence }
```

### Exercise-specific analysis

| Exercise | Key features | Detection method |
|----------|-------------|-----------------|
| Sit-ups | Torso angle, compression ratio, extension ratio | UP/DOWN state machine |
| Push-ups | Elbow angle, back straightness | UP/DOWN state machine |
| Squats | Knee angle, hip angle | UP/DOWN state machine |
| Vertical Jump | Centre-of-mass Y, ankle liftoff, knee/hip extension | Peak detection |
| Shuttle Run | Horizontal body position (X), zone transitions | Left/Right zone state machine |

All rep detectors include:
- **Debounce** — prevents double-counting on noisy frames
- **Smoothing** — 5-tap weighted kernel on position series
- **Per-segment confidence** — each rep is scored individually
- **Bilateral keypoint fallback** — uses best available side if one is occluded

---

## API Overview

Base URL: `http://<host>:8000/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/upload/video` | Upload a video file, returns `video_uri` |
| `POST` | `/evaluation/evaluate` | Run AI evaluation on an uploaded video |
| `GET` | `/submissions/` | List all submissions |
| `GET` | `/leaderboard/` | Get ranked leaderboard |
| `GET` | `/athletes/{id}/achievements` | Get badges & progress for an athlete |
| `GET` | `/users/` | List all users |
| `POST` | `/users/` | Register a new user |
| `GET` | `/benchmarks/` | Get benchmark standards |

> Full interactive API documentation is auto-generated by FastAPI at `/api/v1/openapi.json`.

---

## Preview





## License

This project is proprietary and confidential.  
Unauthorized copying, distribution, or use of this code is strictly prohibited.

© 2026 SaiFit. All rights reserved.
