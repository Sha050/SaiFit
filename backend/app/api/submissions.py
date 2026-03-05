from fastapi import APIRouter, status, HTTPException
from typing import List, Optional
from app.models.submission import SubmissionCreate, SubmissionResponse, TestResultResponse, TestResultCreate
from app.db.mongodb import db
from datetime import datetime

router = APIRouter()


@router.post("/", response_model=dict, status_code=status.HTTP_201_CREATED)
async def create_submission(submission: SubmissionCreate):
    """
    Create a new video test submission.
    Optionally embeds a test result in the results collection.
    """
    sub_dict = submission.model_dump()

    # Extract embedded result data if present
    test_result_data = sub_dict.pop("test_result_data", None)

    if test_result_data:
        # Ensure timestamp as int milliseconds
        if "timestamp" not in test_result_data or not isinstance(test_result_data["timestamp"], int):
            test_result_data["timestamp"] = int(datetime.utcnow().timestamp() * 1000)
        res = await db.db.results.insert_one(test_result_data)
        sub_dict["result_id"] = str(res.inserted_id)

    # Add created_at
    sub_dict["created_at"] = datetime.utcnow().isoformat()

    # Insert submission
    result = await db.db.submissions.insert_one(sub_dict)

    # Return it
    created_sub = await db.db.submissions.find_one({"_id": result.inserted_id})
    if created_sub:
        # Ensure _id is string for JSON serialization
        created_sub["_id"] = str(created_sub["_id"])
    return created_sub or sub_dict


@router.get("/")
async def list_submissions(athlete_id: Optional[str] = None):
    """
    List submissions, optionally filtered by athlete_id.
    """
    query = {}
    if athlete_id:
        query = {"athlete_id": athlete_id}

    submissions = await db.db.submissions.find(query).to_list(100)
    # Ensure _id is serializable to id
    for sub in submissions:
        if "_id" in sub:
            sub["id"] = str(sub.pop("_id"))
    return submissions


@router.get("/results")
async def list_results(athlete_id: Optional[str] = None):
    """
    List all test results.
    """
    query = {}
    if athlete_id:
        query = {"athlete_id": athlete_id}

    results = await db.db.results.find(query).to_list(100)
    out = []
    for res in results:
        res["id"] = str(res.pop("_id", ""))
        
        ts = res.get("timestamp")
        if isinstance(ts, str):
            try:
                from datetime import timezone
                # handle isoformat that might lack tzinfo
                clean_ts = ts.replace("Z", "+00:00")
                res["timestamp"] = int(datetime.fromisoformat(clean_ts).timestamp() * 1000)
            except Exception as e:
                res["timestamp"] = int(datetime.utcnow().timestamp() * 1000)
        elif isinstance(ts, (int, float)):
            res["timestamp"] = int(ts)
        else:
            res["timestamp"] = int(datetime.utcnow().timestamp() * 1000)
            
        out.append(res)
    return out


@router.post("/result", status_code=status.HTTP_201_CREATED)
async def create_result(result: TestResultCreate):
    """
    Store a test result directly (without a full submission wrapper).
    Useful for saving evaluation results from the app.
    """
    result_dict = result.model_dump()
    result_dict["timestamp"] = int(datetime.utcnow().timestamp() * 1000)

    res = await db.db.results.insert_one(result_dict)
    created = await db.db.results.find_one({"_id": res.inserted_id})
    if created:
        created["id"] = str(created.pop("_id", ""))
    return created or result_dict
