from fastapi import APIRouter, status, HTTPException
from typing import List
from app.models.user import UserCreate, UserResponse
from app.db.mongodb import db

router = APIRouter()


@router.post("/", status_code=status.HTTP_201_CREATED)
async def create_user(user: UserCreate):
    """
    Create a new user (athlete or admin) in the database.
    """
    user_dict = user.model_dump()

    # Check if user already exists
    existing_user = await db.db.users.find_one({"email": user.email})
    if existing_user:
        raise HTTPException(
            status_code=400,
            detail="User with this email already exists",
        )

    # Insert new user
    result = await db.db.users.insert_one(user_dict)

    # Fetch the newly created user to return it
    created_user = await db.db.users.find_one({"_id": result.inserted_id})
    if created_user:
        created_user["id"] = str(created_user.pop("_id", ""))
    return created_user or user_dict


@router.get("/")
async def list_users():
    """
    List all users in the database.
    """
    users = await db.db.users.find().to_list(100)
    for u in users:
        if "_id" in u:
            u["id"] = str(u.pop("_id", ""))
    return users


@router.get("/{user_id}")
async def get_user(user_id: str):
    """
    Get a single user by ID.
    """
    user = await db.db.users.find_one({"_id": user_id})
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    user["id"] = str(user.pop("_id", ""))
    return user

@router.put("/{user_id}")
async def update_user(user_id: str, update_data: dict):
    """
    Update user profile data.
    """
    # Remove None values
    filtered_data = {k: v for k, v in update_data.items() if v is not None}
    
    if not filtered_data:
        raise HTTPException(status_code=400, detail="No data provided to update")
        
    result = await db.db.users.update_one(
        {"_id": user_id},
        {"$set": filtered_data}
    )
    
    if result.matched_count == 0:
        raise HTTPException(status_code=404, detail="User not found")
        
    updated_user = await db.db.users.find_one({"_id": user_id})
    if updated_user:
        updated_user["id"] = str(updated_user.pop("_id", ""))
    return updated_user
