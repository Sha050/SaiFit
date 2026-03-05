from pydantic import BaseModel, Field, EmailStr
from typing import Optional, Annotated
from pydantic.functional_validators import BeforeValidator

# Helps convert MongoDB ObjectIds to strings
PyObjectId = Annotated[str, BeforeValidator(str)]

class UserBase(BaseModel):
    first_name: str
    last_name: str
    email: EmailStr
    age: int
    gender: str
    region: str
    height_cm: Optional[float] = None
    weight_kg: Optional[float] = None
    role: str = "athlete"
    phone_number: Optional[str] = None
    aadhaar_number: Optional[str] = None
    profile_image_uri: Optional[str] = None
    sport: Optional[str] = None

class UserCreate(UserBase):
    pass

class UserUpdate(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    age: Optional[int] = None
    gender: Optional[str] = None
    region: Optional[str] = None
    height_cm: Optional[float] = None
    weight_kg: Optional[float] = None
    avatar_url: Optional[str] = None

class UserResponse(UserBase):
    id: Optional[PyObjectId] = Field(alias="_id", default=None)

    model_config = {
        "populate_by_name": True,
        "json_schema_extra": {
            "example": {
                "first_name": "Sachin",
                "last_name": "Tendulkar",
                "email": "sachin@example.com",
                "age": 25,
                "gender": "Male",
                "region": "Maharashtra",
                "height_cm": 165.0,
                "weight_kg": 65.0,
                "role": "athlete"
            }
        }
    }
