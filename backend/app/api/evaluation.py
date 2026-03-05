from fastapi import APIRouter
from pydantic import BaseModel
import asyncio
import random
import time
import uuid
from typing import Optional
import os
from .pose_evaluator import process_video

router = APIRouter()

class EvaluationRequest(BaseModel):
    test_id: str
    test_name: str
    unit: str
    athlete_id: str
    athlete_name: str
    video_uri: Optional[str] = None
    recording_duration_ms: int = 0

@router.post("/evaluate")
async def evaluate_video(request: EvaluationRequest):
    """
    Simulate AI evaluation over a video matching the Android MockEvaluationService,
    but use actual AI if a video is provided.
    """
    # Simulate processing time slightly if we aren't doing actual heavy lifting
    test_id = request.test_id
    
    value = 0.0
    segments = []
    processed = False
    
    # Real AI Processing
    annotated_uri = request.video_uri
    if request.video_uri and request.video_uri.startswith("/uploads/"):
        upload_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")
        filename = request.video_uri.split("/")[-1]
        video_path = os.path.join(upload_dir, filename)
        
        if os.path.exists(video_path):
            try:
                # Run the heavy CV model in a separate thread so we don't block FastAPI
                val, segs, ann_uri = await asyncio.to_thread(process_video, video_path, test_id)
                value = float(val)
                segments = segs
                if ann_uri:
                    annotated_uri = ann_uri
                processed = True
            except Exception as e:
                import traceback
                traceback.print_exc()
                print(f"AI Processing failed: {e}")

    # Fallback to mock values if no video or if the AI failed to process it
    if not processed:
        await asyncio.sleep(2)  # fake delay
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
        elif test_id == "endurance_run_800m":
            value = 150.0 + (random.random() * 70.0)
        elif test_id == "endurance_run_1600m":
            value = 320.0 + (random.random() * 140.0)
        else:
            value = random.random() * 100.0
            
        duration = request.recording_duration_ms if request.recording_duration_ms > 0 else 30000
        segments = _generate_mock_segments(test_id, duration)

    value = round(value, 1)        
    confidence = int(75 + (random.random() * 25))
    status = "VALID" if confidence >= 80 else "RETRY"
    
    # Generate mock verification
    authenticity_score = int(80 + random.random() * 20)
    is_authentic = authenticity_score >= 85
    
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
    if random.random() < 0.1:
        flags.append({"type": "LOW_VISIBILITY", "description": "Lighting conditions could be improved", "severity": "INFO"})
    if random.random() < 0.05:
        flags.append({"type": "DEVICE_MOTION", "description": "Camera movement detected during recording", "severity": "WARNING"})
    if not flags:
        flags.append({"type": "NONE", "description": "No issues detected", "severity": "INFO"})
        
    # If we generated actual segments, use them (already handled above fallback loop)
    
    suggested_sport = _suggest_sport(test_id, value)
    
    result_dict = {
        "id": f"res_{uuid.uuid4().hex[:8]}",
        "timestamp": int(time.time() * 1000),
        "test_id": test_id,
        "test_name": request.test_name,
        "athlete_id": request.athlete_id,
        "athlete_name": request.athlete_name,
        "value": value,
        "unit": request.unit,
        "confidence_percent": confidence,
        "status": status,
        "video_uri": request.video_uri,
        "suggested_sport": suggested_sport,
        "verification": {
            "is_authentic": is_authentic,
            "authenticity_score": authenticity_score,
            "tamper_detected": False,
            "movement_quality": movement_quality,
            "flags": flags,
            "segments": segments
        }
    }
    
    # Save the result to MongoDB
    from app.db.mongodb import db
    try:
        res = await db.db.results.insert_one(result_dict)
        if "_id" in result_dict:
            result_dict["_id"] = str(result_dict["_id"])
    except Exception as e:
        print(f"Error saving result to DB: {e}")
        
    return result_dict

def _generate_mock_segments(test_id: str, duration_ms: int):
    segments = []
    if test_id == "situps":
        rep_count = int(20 + (random.random() * 30))
        rep_duration = duration_ms // max(rep_count, 1)
        for i in range(1, rep_count + 1):
            segments.append({
                "label": f"Rep {i}",
                "start_time_ms": (i - 1) * rep_duration,
                "end_time_ms": i * rep_duration,
                "confidence": int(85 + random.random() * 15)
            })
    elif test_id == "vertical_jump":
        segments = [
            {"label": "Setup", "start_time_ms": 0, "end_time_ms": duration_ms // 4, "confidence": 95},
            {"label": "Jump Apex", "start_time_ms": duration_ms // 4, "end_time_ms": duration_ms // 2, "confidence": int(90 + random.random() * 10)},
            {"label": "Landing", "start_time_ms": duration_ms // 2, "end_time_ms": int(duration_ms * 0.75), "confidence": 92},
        ]
    elif test_id == "shuttle_run":
        for i in range(1, 11):
            seg_len = duration_ms // 10
            segments.append({
                "label": f"Shuttle {i}",
                "start_time_ms": (i - 1) * seg_len,
                "end_time_ms": i * seg_len,
                "confidence": int(88 + random.random() * 12)
            })
    elif test_id == "endurance_run_800m":
         segments = [
            {"label": "Lap 1", "start_time_ms": 0, "end_time_ms": duration_ms // 2, "confidence": int(90 + random.random() * 10)},
            {"label": "Lap 2", "start_time_ms": duration_ms // 2, "end_time_ms": duration_ms, "confidence": int(88 + random.random() * 12)},
        ]
    else:
        segments = [
            {"label": "Full Recording", "start_time_ms": 0, "end_time_ms": duration_ms, "confidence": int(90 + random.random() * 10)}
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
    if test_id == "endurance_run_800m":
        return "Middle Distance Running" if value < 160 else "Team Sports"
    if test_id == "endurance_run_1600m":
        return "Long Distance Running / Cycling" if value < 350 else "Team Sports"
    return "General Athletics"
