package com.saifit.app.data.model

import com.google.gson.annotations.SerializedName

data class TestResult(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String,
    @SerializedName("test_id")
    val testId: String,
    @SerializedName("test_name")
    val testName: String,
    @SerializedName("athlete_id")
    val athleteId: String,
    @SerializedName("athlete_name")
    val athleteName: String,
    val value: Double,           
    val unit: String,
    @SerializedName("confidence_percent")
    val confidencePercent: Int,  
    val status: ResultStatus,
    @SerializedName("video_uri")
    val videoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),

    val verification: VerificationResult? = null,

    @SerializedName("suggested_sport")
    val suggestedSport: String? = null
)

enum class ResultStatus {
    VALID,
    RETRY,
    PENDING
}
