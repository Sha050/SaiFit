from fastapi import APIRouter, HTTPException
from typing import List, Dict, Any
from app.db.mongodb import db

router = APIRouter()

@router.get("/{athlete_id}/achievements")
async def get_achievements(athlete_id: str):
    """
    Get badges and progress for an athlete based on their valid test results.
    """
    # Fetch user data to verify existence
    user = await db.db.users.find_one({"_id": athlete_id})
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    # Fetch all results for the user
    results = await db.db.results.find({"athlete_id": athlete_id}).to_list(100)
    
    badges = []
    completed_tests = set()
    best_results = {}
    valid_results_count = 0
    retry_results_count = 0
    
    for res in results:
        status = res.get("status", "RETRY")
        if status == "VALID":
            valid_results_count += 1
            test_id = res.get("test_id")
            completed_tests.add(test_id)
            
            # Simple logic for determining what tier this is 
            # In a real app we'd cross-reference with actual benchmark standards
            tier = "ADVANCED" 
            if res.get("confidence_percent", 0) > 90:
                tier = "ELITE"
                
            val = res.get("value", 0.0)
            
            # Keep track of best result per test
            # Note: what is 'best' depends on the test (lower is better for some)
            lower_is_better = test_id in ["shuttle_run"]
            
            if test_id not in best_results:
                best_results[test_id] = res
            else:
                current_best = best_results[test_id].get("value", 0.0)
                if lower_is_better and val < current_best:
                    best_results[test_id] = res
                elif not lower_is_better and val > current_best:
                    best_results[test_id] = res
                    
            badges.append({
                "id": f"badge_{res['_id']}",
                "name": f"{tier} in {res.get('test_name')}",
                "description": f"Achieved {val} {res.get('unit')}",
                "iconName": "star",
                "earnedDate": res.get("timestamp"),
                "tier": tier,
                "testId": test_id
            })
        else:
            retry_results_count += 1
            
    # For simplicity, returning the total predefined tests as 6, as we know there are 6 tests in TestRepository
    total_tests = 6
            
    return {
        "totalTests": total_tests,
        "completedTests": len(completed_tests),
        "validResults": valid_results_count,
        "retryResults": retry_results_count,
        "badges": badges,
        # Formatting best results
        "bestResults": {k: {"value": v.get("value"), "unit": v.get("unit"), "tier": "ADVANCED"} for k, v in best_results.items()}
    }
