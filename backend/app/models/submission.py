from pydantic import BaseModel, Field
from typing import Optional, List, Annotated
from pydantic.functional_validators import BeforeValidator
from datetime import datetime

PyObjectId = Annotated[str, BeforeValidator(str)]

class VerificationFlag(BaseModel):
    type: str # 'TAMPER_DETECTED', 'FRAME_SKIP', etc.
    description: str
    severity: str # 'CRITICAL', 'WARNING', 'INFO'

class VideoSegment(BaseModel):
    label: str
    start_time_ms: int
    end_time_ms: int
    confidence: int

class VerificationResult(BaseModel):
    is_authentic: bool
    authenticity_score: int
    tamper_detected: bool
    movement_quality: str # 'EXCELLENT', 'GOOD', etc.
    flags: List[VerificationFlag] = []
    segments: List[VideoSegment] = []

class TestResultBase(BaseModel):
    test_id: str
    test_name: str
    athlete_id: str
    athlete_name: str
    value: float
    unit: str
    confidence_percent: int
    status: str # 'VALID', 'RETRY', 'PENDING'
    video_uri: Optional[str] = None
    suggested_sport: Optional[str] = None
    verification: Optional[VerificationResult] = None

class TestResultCreate(TestResultBase):
    pass

class TestResultResponse(TestResultBase):
    id: Optional[PyObjectId] = Field(alias="_id", default=None)
    timestamp: datetime = Field(default_factory=datetime.utcnow)

    model_config = {
        "populate_by_name": True
    }

class SubmissionBase(BaseModel):
    result_id: str
    test_name: str
    athlete_id: str
    athlete_name: str
    video_uri: Optional[str] = None
    status: str # 'QUEUED', 'UPLOADING', 'SUBMITTED', 'VERIFIED', 'FAILED'
    progress: float = 0.0
    error_message: Optional[str] = None

class SubmissionCreate(SubmissionBase):
    test_result_data: Optional[TestResultCreate] = None

class SubmissionResponse(SubmissionBase):
    id: Optional[PyObjectId] = Field(alias="_id", default=None)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    
    model_config = {
        "populate_by_name": True
    }
