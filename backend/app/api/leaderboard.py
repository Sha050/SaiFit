from fastapi import APIRouter
from typing import List, Dict, Any
from app.db.mongodb import db

router = APIRouter()


@router.get("/")
async def get_leaderboard(
    test_name: str = "Vertical Jump",
    gender: str = "All",
    age_category: str = "All",
):
    """
    Get aggregated leaderboard from results by joining with user data.
    """
    
    # Calculate sort order based on test_name (lower is better for runs)
    lower_is_better = test_name in ["Shuttle Run (10 \u00d7 4m)", "Endurance Run (800 m)", "Endurance Run (1600 m)", "shuttle_run", "endurance_run_800m", "endurance_run_1600m"]
    sort_dir = 1 if lower_is_better else -1
    
    match_stage = {"$match": {"test_name": test_name, "status": "VALID"}}
    
    pipeline = [
        match_stage,
        {"$sort": {"value": sort_dir}},
        {"$group": {
            "_id": "$athlete_id",
            "best_result": {"$first": "$$ROOT"}
        }},
        {"$replaceRoot": {"newRoot": "$best_result"}},
        {"$sort": {"value": sort_dir}},
        {"$limit": 50},
        # Join with users collection
        {
            "$lookup": {
                "from": "users",
                "localField": "athlete_id",
                "foreignField": "_id",
                "as": "athlete_info"
            }
        },
        {
            "$unwind": {
                "path": "$athlete_info",
                "preserveNullAndEmptyArrays": True
            }
        }
    ]
    
    results = await db.db.results.aggregate(pipeline).to_list(50)

    # Filter after join if needed (aggregation lookup match could be slow depending on index, so doing in python if dataset small, or add match filter to pipeline)
    filtered_results = []
    for res in results:
        user_info = res.get("athlete_info", {})
        
        user_gender = user_info.get("gender", "Male")
        user_age = user_info.get("age", 20)
        
        # Apply gender filter
        if gender != "All" and user_gender.lower() != gender.lower():
            continue
            
        # Apply age filter (assuming simple string equal for now or skip)
        
        filtered_results.append(res)
        
    leaderboard = []
    for index, res in enumerate(filtered_results):
        user_info = res.get("athlete_info", {})
        leaderboard.append(
            {
                "rank": index + 1,
                "athleteId": res.get("athlete_id", "unknown"),
                "athleteName": res.get("athlete_name", "Unknown Athlete"),
                "region": user_info.get("region", "Unknown"),
                "age": user_info.get("age", 18),
                "gender": user_info.get("gender", "MALE"),
                "value": res.get("value", 0.0),
                "unit": res.get("unit", ""),
                "tier": "ELITE" if index < 5 else "ADVANCED", # TODO: dynamic tier
            }
        )

    return leaderboard
