package com.saifit.app.data.model

data class Submission(
    val id: String,
    val resultId: String,
    val testId: String,
    val testName: String,
    val athleteId: String,
    val athleteName: String,
    val videoUri: String?,
    val status: SubmissionStatus,
    val progress: Float = 0f,       
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

enum class SubmissionStatus(val label: String) {
    QUEUED("Queued"),
    UPLOADING("Uploading"),
    SUBMITTED("Submitted"),
    VERIFIED("Verified by SAI"),
    FAILED("Failed"),
    OFFLINE("Waiting for Network")
}
