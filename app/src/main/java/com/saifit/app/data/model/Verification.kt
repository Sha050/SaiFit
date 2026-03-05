package com.saifit.app.data.model

import com.google.gson.annotations.SerializedName

data class VerificationResult(
    @SerializedName("is_authentic")
    val isAuthentic: Boolean,
    @SerializedName("authenticity_score")
    val authenticityScore: Int,        
    @SerializedName("tamper_detected")
    val tamperDetected: Boolean,
    @SerializedName("movement_quality")
    val movementQuality: MovementQuality,
    val flags: List<VerificationFlag>,

    val segments: List<VideoSegment>
) 

enum class MovementQuality(val label: String) {
    EXCELLENT("Excellent Form"),
    GOOD("Good Form"),
    ACCEPTABLE("Acceptable"),
    POOR("Poor Form"),
    INVALID("Invalid Attempt")
}

data class VerificationFlag(
    val type: FlagType,
    val description: String,
    val severity: FlagSeverity
)

enum class FlagType {
    TAMPER_DETECTED,          
    FRAME_SKIP,               
    ENVIRONMENT_MISMATCH,     
    MOVEMENT_ANOMALY,         
    LOW_VISIBILITY,           
    DEVICE_MOTION,            
    MULTIPLE_PERSONS,         
    NONE                      
}

enum class FlagSeverity {
    CRITICAL,    
    WARNING,     
    INFO         
}

data class VideoSegment(
    val label: String,         
    @SerializedName("start_time_ms")
    val startTimeMs: Long,
    @SerializedName("end_time_ms")
    val endTimeMs: Long,
    val confidence: Int        
)
