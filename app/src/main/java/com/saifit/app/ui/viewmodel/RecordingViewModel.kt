package com.saifit.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saifit.app.data.api.ApiClient
import com.saifit.app.data.api.EvaluationRequestDto
import com.saifit.app.data.model.FitnessTest
import com.saifit.app.data.model.ResultStatus
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.repository.ResultRepository
import com.saifit.app.data.repository.TestRepository
import com.saifit.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

data class RecordingUiState(
    val test: FitnessTest? = null,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val isEvaluating: Boolean = false,
    val recordingStopped: Boolean = false,
    val videoUri: String? = null,
    val result: TestResult? = null,
    val error: String? = null
)

class RecordingViewModel(
    private val appContext: Context,
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    fun loadTest(testId: String) {
        val test = testRepository.getTestById(testId)
        _uiState.update { it.copy(test = test, error = null) }
    }

    fun startRecording() {
        _uiState.update {
            it.copy(
                isRecording = true,
                recordingDurationMs = 0L,
                recordingStopped = false,
                error = null
            )
        }
    }

    fun requestStopRecording() {
        _uiState.update { it.copy(isRecording = false) }
    }

    fun updateDuration(ms: Long) {
        _uiState.update { it.copy(recordingDurationMs = ms) }
    }

    fun stopRecording(videoUri: String? = null) {
        if (_uiState.value.test == null) return

        _uiState.update {
            it.copy(
                isRecording = false,
                recordingStopped = true,
                videoUri = videoUri,
                error = null
            )
        }
    }

    fun confirmAndEvaluate() {
        val test = _uiState.value.test ?: return
        val user = userRepository.currentUser.value ?: return
        val localVideoUri = _uiState.value.videoUri
        val durationMs = _uiState.value.recordingDurationMs

        _uiState.update { it.copy(isEvaluating = true, recordingStopped = false, error = null) }

        viewModelScope.launch {
            try {
                val remoteVideoUri = when {
                    localVideoUri.isNullOrBlank() -> null
                    localVideoUri.startsWith("/uploads/") -> localVideoUri
                    else -> uploadVideo(localVideoUri)
                }

                if (!localVideoUri.isNullOrBlank() && remoteVideoUri.isNullOrBlank()) {
                    throw IllegalStateException("Video upload failed. AI evaluation was not started.")
                }

                val request = EvaluationRequestDto(
                    test_id = test.id,
                    test_name = test.name,
                    unit = test.unit,
                    athlete_id = user.id,
                    athlete_name = user.name,
                    video_uri = remoteVideoUri ?: localVideoUri?.takeIf { it.startsWith("/uploads/") },
                    recording_duration_ms = durationMs
                )
                val response = ApiClient.api.evaluateVideo(request)

                val result = TestResult(
                    id = response.id ?: "res_${System.currentTimeMillis()}",
                    testId = response.test_id,
                    testName = response.test_name,
                    athleteId = response.athlete_id,
                    athleteName = response.athlete_name,
                    value = response.value,
                    unit = response.unit,
                    confidencePercent = response.confidence_percent,
                    status = try {
                        ResultStatus.valueOf(response.status.uppercase())
                    } catch (_: Exception) {
                        ResultStatus.RETRY
                    },
                    timestamp = response.timestamp ?: System.currentTimeMillis(),
                    videoUri = response.video_uri ?: remoteVideoUri ?: localVideoUri,
                    suggestedSport = response.suggested_sport,
                    verification = response.verification
                )

                resultRepository.addResult(result)
                _uiState.update { it.copy(isEvaluating = false, result = result, error = null) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isEvaluating = false,
                        recordingStopped = true,
                        error = e.message ?: "Evaluation failed"
                    )
                }
            }
        }
    }

    fun stopRecordingAndEvaluate(videoUri: String? = null) {
        stopRecording(videoUri)
        confirmAndEvaluate()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.value = RecordingUiState()
    }

    private suspend fun uploadVideo(videoUriString: String): String {
        val file = resolveUploadableVideoFile(videoUriString)
            ?: throw IllegalArgumentException("Unable to read the selected video file.")

        if (!file.exists() || file.length() == 0L) {
            throw IllegalArgumentException("The selected video file is empty or missing.")
        }

        val requestBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return ApiClient.api.uploadVideo(multipartBody).video_uri
    }

    private fun resolveUploadableVideoFile(videoUriString: String): File? {
        return try {
            when {
                videoUriString.startsWith("/uploads/") -> null
                videoUriString.startsWith("/") -> {
                    File(videoUriString).takeIf { it.exists() }
                }
                else -> {
                    val uri = Uri.parse(videoUriString)
                    when (uri.scheme) {
                        null -> File(videoUriString).takeIf { it.exists() }
                        "file" -> uri.path?.let { path -> File(path).takeIf { it.exists() } }
                        "content" -> copyContentUriToCache(uri)
                        else -> null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyContentUriToCache(uri: Uri): File? {
        val extension = when (appContext.contentResolver.getType(uri)) {
            "video/webm" -> ".webm"
            "video/quicktime" -> ".mov"
            else -> ".mp4"
        }
        val tempFile = File(appContext.cacheDir, "upload_${System.currentTimeMillis()}$extension")
        return appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            tempFile
        }
    }
}
