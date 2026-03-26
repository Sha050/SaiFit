import asyncio
import random
import time
import uuid
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from .pose_evaluator import process_video

router = APIRouter()

UPLOAD_DIR = Path(__file__).resolve().parents[1] / "uploads"
STRICT_AI_TEST_IDS = {
    "situps",
    "vertical_jump",
    "shuttle_run",
}


class EvaluationRequest(BaseModel):
    test_id: str
    test_name: str
    unit: str
    athlete_id: str
    athlete_name: str
    video_uri: Optional[str] = None
    recording_duration_ms: int = 0


def _resolve_uploaded_video(video_uri: str) -> Optional[Path]:
    if not video_uri or not video_uri.startswith("/uploads/"):
        return None
    candidate = UPLOAD_DIR / video_uri.split("/")[-1]
    return candidate if candidate.exists() else None


def _build_processed_quality(test_id: str, value: float, segments: list[dict]):
    avg_segment_conf = (
        sum(segment.get("confidence", 85) for segment in segments) / len(segments)
        if segments
        else 0.0
    )

    if test_id == "situps" and not segments:
        return {
            "confidence_percent": 55,
            "status": "RETRY",
            "authenticity_score": 62,
            "is_authentic": False,
            "movement_quality": "POOR",
            "flags": [
                {
                    "type": "MOVEMENT_ANOMALY",
                    "description": "No full sit-up repetitions were detected. Retake with the full body visible side-on.",
                    "severity": "WARNING",
                }
            ],
        }

    if test_id == "vertical_jump" and not segments:
        return {
            "confidence_percent": 58,
            "status": "RETRY",
            "authenticity_score": 65,
            "is_authentic": False,
            "movement_quality": "POOR",
            "flags": [
                {
                    "type": "MOVEMENT_ANOMALY",
                    "description": "No complete vertical jump was detected. Retake with the full body visible and feet clearly in frame.",
                    "severity": "WARNING",
                }
            ],
        }

    if test_id == "shuttle_run" and (value <= 0 or len([s for s in segments if s.get("label", "").startswith("Shuttle")]) < 10):
        completed = len([s for s in segments if s.get("label", "").startswith("Shuttle")])
        return {
            "confidence_percent": 60 if completed > 0 else 55,
            "status": "RETRY",
            "authenticity_score": 66 if completed > 0 else 62,
            "is_authentic": completed > 0,
            "movement_quality": "ACCEPTABLE" if completed >= 4 else "POOR",
            "flags": [
                {
                    "type": "MOVEMENT_ANOMALY",
                    "description": f"Incomplete shuttle run detected ({completed}/10 shuttles). Retake with the full drill visible in frame.",
                    "severity": "WARNING",
                }
            ],
        }

    confidence = int(min(99, max(80 if test_id == "situps" else 78, round(avg_segment_conf or 86))))
    movement_quality = "EXCELLENT"
    if confidence < 95:
        movement_quality = "GOOD"
    if confidence < 88:
        movement_quality = "ACCEPTABLE"
    if confidence < 80:
        movement_quality = "POOR"

    return {
        "confidence_percent": confidence,
        "status": "VALID" if confidence >= 80 else "RETRY",
        "authenticity_score": min(99, confidence + 3),
        "is_authentic": confidence >= 75,
        "movement_quality": movement_quality,
        "flags": [
            {
                "type": "NONE",
                "description": "Pose analysis completed successfully.",
                "severity": "INFO",
            }
        ],
    }


def _build_mock_quality(processing_error: Optional[str]):
    authenticity_score = int(80 + random.random() * 20)
    if authenticity_score >= 95:
        movement_quality = "EXCELLENT"
    elif authenticity_score >= 90:
        movement_quality = "GOOD"
    elif authenticity_score >= 80:
        movement_quality = "ACCEPTABLE"
    elif authenticity_score >= 70:
        movement_quality = "POOR"
    else:
        movement_quality = "INVALID"

    flags = []
    if processing_error:
        flags.append(
            {
                "type": "MOVEMENT_ANOMALY",
                "description": "Pose model could not finish processing, so fallback scoring was used.",
                "severity": "WARNING",
            }
        )
    elif random.random() < 0.1:
        flags.append(
            {
                "type": "LOW_VISIBILITY",
                "description": "Lighting conditions could be improved.",
                "severity": "INFO",
            }
        )

    if random.random() < 0.05:
        flags.append(
            {
                "type": "DEVICE_MOTION",
                "description": "Camera movement detected during recording.",
                "severity": "WARNING",
            }
        )

    if not flags:
        flags.append({"type": "NONE", "description": "No issues detected.", "severity": "INFO"})

    confidence = int(75 + (random.random() * 25))
    return {
        "confidence_percent": confidence,
        "status": "VALID" if confidence >= 80 else "RETRY",
        "authenticity_score": authenticity_score,
        "is_authentic": authenticity_score >= 85,
        "movement_quality": movement_quality,
        "flags": flags,
    }


@router.post("/evaluate")
async def evaluate_video(request: EvaluationRequest):
    test_id = request.test_id
    value = 0.0
    segments = []
    processed = False
    processing_error = None
    resolved_video_uri = request.video_uri

    uploaded_video_path = _resolve_uploaded_video(request.video_uri or "")
    if uploaded_video_path is not None:
        try:
            value, segments, annotated_uri = await asyncio.to_thread(
                process_video,
                str(uploaded_video_path),
                test_id,
            )
            resolved_video_uri = annotated_uri or request.video_uri
            processed = True
        except Exception as exc:
            processing_error = str(exc)
            import traceback

            traceback.print_exc()
            print(f"AI processing failed: {exc}")
            if test_id in STRICT_AI_TEST_IDS:
                raise HTTPException(
                    status_code=500,
                    detail=f"{request.test_name} AI evaluation failed: {exc}",
                ) from exc
    elif request.video_uri and test_id in STRICT_AI_TEST_IDS:
        raise HTTPException(
            status_code=400,
            detail=f"{request.test_name} AI evaluation requires the uploaded server video. Upload failed or the file is not available on the backend.",
        )

    if not processed:
        await asyncio.sleep(2)
        if test_id == "height":
            value = 155.0 + (random.random() * 35.0)
        elif test_id == "weight":
            value = 48.0 + (random.random() * 30.0)
        elif test_id == "situps":
            value = 20.0 + (random.random() * 30.0)
        elif test_id == "vertical_jump":
            value = 28.0 + (random.random() * 35.0)
        elif test_id == "shuttle_run":
            value = 9.5 + (random.random() * 4.5)
        elif test_id == "pushups":
            value = 15.0 + (random.random() * 20.0)
        elif test_id == "squats":
            value = 25.0 + (random.random() * 30.0)
        else:
            value = random.random() * 100.0

        duration = request.recording_duration_ms if request.recording_duration_ms > 0 else 30000
        segments = _generate_mock_segments(test_id, duration)

    quality = _build_processed_quality(test_id, value, segments) if processed else _build_mock_quality(processing_error)
    suggested_sport = _suggest_sport(test_id, value)

    result_dict = {
        "id": f"res_{uuid.uuid4().hex[:8]}",
        "timestamp": int(time.time() * 1000),
        "test_id": test_id,
        "test_name": request.test_name,
        "athlete_id": request.athlete_id,
        "athlete_name": request.athlete_name,
        "value": round(value, 1),
        "unit": request.unit,
        "confidence_percent": quality["confidence_percent"],
        "status": quality["status"],
        "video_uri": resolved_video_uri,
        "suggested_sport": suggested_sport,
        "verification": {
            "is_authentic": quality["is_authentic"],
            "authenticity_score": quality["authenticity_score"],
            "tamper_detected": False,
            "movement_quality": quality["movement_quality"],
            "flags": quality["flags"],
            "segments": segments,
        },
    }

    from app.db.mongodb import db

    try:
        await db.db.results.insert_one(dict(result_dict))
    except Exception as exc:
        print(f"Error saving result to DB: {exc}")

    return result_dict


def _generate_mock_segments(test_id: str, duration_ms: int):
    segments = []
    if test_id == "situps":
        rep_count = int(20 + (random.random() * 30))
        rep_duration = duration_ms // max(rep_count, 1)
        for idx in range(1, rep_count + 1):
            segments.append(
                {
                    "label": f"Rep {idx}",
                    "start_time_ms": (idx - 1) * rep_duration,
                    "end_time_ms": idx * rep_duration,
                    "confidence": int(85 + random.random() * 15),
                }
            )
    elif test_id == "vertical_jump":
        segments = [
            {"label": "Setup", "start_time_ms": 0, "end_time_ms": duration_ms // 4, "confidence": 95},
            {
                "label": "Jump Apex",
                "start_time_ms": duration_ms // 4,
                "end_time_ms": duration_ms // 2,
                "confidence": int(90 + random.random() * 10),
            },
            {
                "label": "Landing",
                "start_time_ms": duration_ms // 2,
                "end_time_ms": int(duration_ms * 0.75),
                "confidence": 92,
            },
        ]
    elif test_id == "shuttle_run":
        for idx in range(1, 11):
            seg_len = duration_ms // 10
            segments.append(
                {
                    "label": f"Shuttle {idx}",
                    "start_time_ms": (idx - 1) * seg_len,
                    "end_time_ms": idx * seg_len,
                    "confidence": int(88 + random.random() * 12),
                }
            )
    elif test_id in ["pushups", "squats"]:
        rep_count = int(10 + (random.random() * 20))
        rep_duration = duration_ms // max(rep_count, 1)
        for idx in range(1, rep_count + 1):
            segments.append(
                {
                    "label": f"Rep {idx}",
                    "start_time_ms": (idx - 1) * rep_duration,
                    "end_time_ms": idx * rep_duration,
                    "confidence": int(85 + random.random() * 15),
                }
            )
    else:
        segments = [
            {
                "label": "Full Recording",
                "start_time_ms": 0,
                "end_time_ms": duration_ms,
                "confidence": int(90 + random.random() * 10),
            }
        ]
    return segments


def _suggest_sport(test_id: str, value: float):
    if test_id == "height":
        return "Basketball / Volleyball" if value > 180 else "Gymnastics / Wrestling"
    if test_id == "weight":
        return "Wrestling / Shot Put" if value > 70 else "Long Distance Running"
    if test_id == "situps":
        return "Martial Arts / Gymnastics" if value > 40 else "General Athletics"
    if test_id == "vertical_jump":
        return "Basketball / High Jump" if value > 50 else "Football / Hockey"
    if test_id == "shuttle_run":
        return "Badminton / Tennis" if value < 10.5 else "General Athletics"
    if test_id == "pushups":
        return "Gymnastics / Wrestling" if value > 30 else "General Athletics"
    if test_id == "squats":
        return "Weightlifting / Powerlifting" if value > 40 else "General Athletics"
    return "General Athletics"
