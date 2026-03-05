package com.saifit.app.data.repository

import com.saifit.app.data.model.Submission
import com.saifit.app.data.model.SubmissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SubmissionRepository {

    private val _submissions = MutableStateFlow<List<Submission>>(emptyList())
    val submissions: StateFlow<List<Submission>> = _submissions.asStateFlow()

    fun addSubmission(submission: Submission) {
        _submissions.update { listOf(submission) + it }
    }

    fun updateStatus(submissionId: String, status: SubmissionStatus, progress: Float = 0f) {
        _submissions.update { list ->
            list.map {
                if (it.id == submissionId) it.copy(
                    status = status,
                    progress = progress,
                    lastAttemptAt = System.currentTimeMillis()
                ) else it
            }
        }
    }

    fun markFailed(submissionId: String, error: String) {
        _submissions.update { list ->
            list.map {
                if (it.id == submissionId) it.copy(
                    status = SubmissionStatus.FAILED,
                    errorMessage = error,
                    retryCount = it.retryCount + 1,
                    lastAttemptAt = System.currentTimeMillis()
                ) else it
            }
        }
    }

    fun retrySubmission(submissionId: String) {
        updateStatus(submissionId, SubmissionStatus.QUEUED)
    }

    fun getPendingCount(): Int =
        _submissions.value.count { it.status in listOf(SubmissionStatus.QUEUED, SubmissionStatus.UPLOADING, SubmissionStatus.OFFLINE) }

    fun getSubmittedCount(): Int =
        _submissions.value.count { it.status in listOf(SubmissionStatus.SUBMITTED, SubmissionStatus.VERIFIED) }

    suspend fun uploadAll() {

        val pending = _submissions.value.filter { 
            it.status in listOf(SubmissionStatus.QUEUED, SubmissionStatus.OFFLINE, SubmissionStatus.FAILED) 
        }

        for (submission in pending) {
            updateStatus(submission.id, SubmissionStatus.UPLOADING, 0.5f)
            try {
                val request = com.saifit.app.data.api.SubmissionRequestDto(
                    result_id = submission.resultId ?: "",
                    test_name = submission.testName,
                    athlete_id = submission.athleteId,
                    athlete_name = submission.athleteName,
                    status = "submitted",
                    video_uri = submission.videoUri,
                    test_result_data = null
                )
                val responseDto = com.saifit.app.data.api.ApiClient.api.createSubmission(request)

                _submissions.update { list ->
                    list.map {
                        if (it.id == submission.id) {
                            it.copy(
                                status = SubmissionStatus.SUBMITTED,
                                progress = 1f,
                                errorMessage = null,
                                lastAttemptAt = System.currentTimeMillis()
                            )
                        } else it
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                markFailed(submission.id, e.message ?: "Upload failed")
            }
        }
    }
}
