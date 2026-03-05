from fastapi import APIRouter
from typing import List, Dict, Any

router = APIRouter()

@router.get("/")
async def get_benchmarks():
    """
    Get AI evaluation benchmarks and performance criteria tiers.
    These are the configuration bounds that define 'Elite', 'Advanced', etc.
    """
    benchmarks = [
        {"test_id": "situps", "gender": "male", "elite_min": 50, "advanced_min": 40, "unit": "reps"},
        {"test_id": "situps", "gender": "female", "elite_min": 45, "advanced_min": 35, "unit": "reps"},
        {"test_id": "vertical_jump", "gender": "male", "elite_min": 60.0, "advanced_min": 50.0, "unit": "cm"},
        {"test_id": "vertical_jump", "gender": "female", "elite_min": 50.0, "advanced_min": 40.0, "unit": "cm"},
        {"test_id": "shuttle_run", "gender": "male", "elite_min": 9.5, "advanced_min": 10.5, "unit": "s", "lower_is_better": True},
        {"test_id": "shuttle_run", "gender": "female", "elite_min": 10.5, "advanced_min": 11.5, "unit": "s", "lower_is_better": True},
    ]
    return {"benchmarks": benchmarks}
