import urllib.request
import urllib.parse
import urllib.error
import json
import uuid
import os
import sys

BASE_URL = "http://localhost:8000/api/v1"
VIDEO_URL = "https://www.w3schools.com/html/mov_bbb.mp4"
LOCAL_VIDEO = "test_video.mp4"

result_obj = {}

try:
    urllib.request.urlretrieve(VIDEO_URL, LOCAL_VIDEO)
    result_obj["download"] = "Success"
except Exception as e:
    result_obj["download"] = str(e)

if "Success" in result_obj["download"]:
    boundary = uuid.uuid4().hex
    with open(LOCAL_VIDEO, "rb") as f:
        video_data = f.read()

    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    }
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="test_video.mp4"\r\n'
        "Content-Type: video/mp4\r\n\r\n".encode("utf-8") +
        video_data +
        f"\r\n--{boundary}--\r\n".encode("utf-8")
    )

    try:
        req = urllib.request.Request(f"{BASE_URL}/upload/", data=body, headers=headers, method="POST")
        with urllib.request.urlopen(req) as response:
            upload_res = response.read().decode("utf-8")
            result_obj["upload"] = {"status": response.status, "body": json.loads(upload_res)}
            video_uri = json.loads(upload_res).get("video_uri")
    except urllib.error.HTTPError as e:
        result_obj["upload"] = {"status": e.code, "error": e.read().decode('utf-8')}
        video_uri = None

    if video_uri:
        submission_data = {
            "result_id": "",
            "test_name": "Sit-ups",
            "athlete_id": "test_athlete_123",
            "athlete_name": "Test Athlete",
            "status": "submitted",
            "video_uri": video_uri,
            "test_result_data": {
                "test_id": "situps",
                "test_name": "Sit-ups",
                "athlete_id": "test_athlete_123",
                "athlete_name": "Test Athlete",
                "value": 42.0,
                "unit": "reps",
                "confidence_percent": 95,
                "status": "VALID",
                "video_uri": video_uri
            }
        }
        try:
            req = urllib.request.Request(
                f"{BASE_URL}/submissions/", 
                data=json.dumps(submission_data).encode("utf-8"), 
                headers={"Content-Type": "application/json"},
                method="POST"
            )
            with urllib.request.urlopen(req) as response:
                result_obj["submission"] = {"status": response.status, "body": json.loads(response.read().decode('utf-8'))}
        except urllib.error.HTTPError as e:
            result_obj["submission"] = {"status": e.code, "error": e.read().decode('utf-8')}


        eval_data = {
            "test_id": "squats",
            "test_name": "Squats",
            "unit": "reps",
            "athlete_id": "test_athlete_123",
            "athlete_name": "Test Athlete",
            "video_uri": video_uri,
            "recording_duration_ms": 15000
        }
        try:
            req = urllib.request.Request(
                f"{BASE_URL}/evaluation/evaluate", 
                data=json.dumps(eval_data).encode("utf-8"), 
                headers={"Content-Type": "application/json"},
                method="POST"
            )
            with urllib.request.urlopen(req) as response:
                result_obj["evaluation"] = {"status": response.status, "body": json.loads(response.read().decode('utf-8'))}
        except urllib.error.HTTPError as e:
            result_obj["evaluation"] = {"status": e.code, "error": e.read().decode('utf-8')}

    os.remove(LOCAL_VIDEO)

with open("test_results.json", "w") as f:
    json.dump(result_obj, f, indent=4)
