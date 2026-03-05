package com.saifit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saifit.app.data.model.FitnessTest
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.model.VideoSegment
import com.saifit.app.data.repository.ResultRepository
import com.saifit.app.data.repository.TestRepository
import com.saifit.app.data.repository.UserRepository
import com.saifit.app.data.api.ApiClient
import com.saifit.app.data.api.EvaluationRequestDto
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import com.saifit.app.data.model.ResultStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordingUiState(
    val test: FitnessTest? = null,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val isEvaluating: Boolean = false,
    val recordingStopped: Boolean = false,
    val videoUri: String? = null,
    val result: TestResult? = null,
    val error: String? = null,

    val mockSegments: List<VideoSegment> = emptyList()
)

class RecordingViewModel(
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    fun loadTest(testId: String) {
        val test = testRepository.getTestById(testId)
        _uiState.update { it.copy(test = test) }
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true, recordingDurationMs = 0L, recordingStopped = false) }
    }

    fun requestStopRecording() {
        _uiState.update { it.copy(isRecording = false) }
    }

    fun updateDuration(ms: Long) {
        _uiState.update { it.copy(recordingDurationMs = ms) }
    }

    fun stopRecording(videoUri: String? = null) {
        val test = _uiState.value.test ?: return
        val durationMs = _uiState.value.recordingDurationMs

        val segments = generateMockReviewSegments(test.id, durationMs)

        _uiState.update {
            it.copy(
                isRecording = false,
                recordingStopped = true,
                videoUri = videoUri,
                mockSegments = segments
            )
        }
    }

    fun confirmAndEvaluate() {
        val test = _uiState.value.test ?: return
        val user = userRepository.currentUser.value ?: return
        val localVideoUri = _uiState.value.videoUri
        val durationMs = _uiState.value.recordingDurationMs

        _uiState.update { it.copy(isEvaluating = true, recordingStopped = false) }

        viewModelScope.launch {
            try {

                var remoteVideoUri: String? = null
                if (localVideoUri != null) {
                    val uri = Uri.parse(localVideoUri)
                    val path = uri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists()) {
                            val requestBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
                            val uploadResponse = ApiClient.api.uploadVideo(multipartBody)
                            remoteVideoUri = uploadResponse.video_uri
                        }
                    }
                }

                val request = EvaluationRequestDto(
                    test_id = test.id,
                    test_name = test.name,
                    unit = test.unit,
                    athlete_id = user.id,
                    athlete_name = user.name,
                    video_uri = remoteVideoUri ?: localVideoUri,
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
                    status = try { ResultStatus.valueOf(response.status.uppercase()) } catch(e: Exception) { ResultStatus.RETRY },
                    timestamp = response.timestamp ?: System.currentTimeMillis(),
                    videoUri = response.video_uri ?: localVideoUri,
                    suggestedSport = response.suggested_sport,
                    verification = response.verification
                )

                resultRepository.addResult(result)
                _uiState.update { it.copy(isEvaluating = false, result = result, error = null) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isEvaluating = false, error = e.message) }
            }
        }
    }

    fun stopRecordingAndEvaluate(videoUri: String? = null) {
        stopRecording(videoUri)
        confirmAndEvaluate()
    }

    fun reset() {
        _uiState.value = RecordingUiState()
    }

    private fun generateMockReviewSegments(testId: String, durationMs: Long): List<VideoSegment> {
        val effectiveDuration = if (durationMs > 0) durationMs else 30000L

        return when (testId) {
            "situps" -> {
                val repCount = (20 + (Math.random() * 20)).toInt()
                val repDuration = effectiveDuration / repCount
                (1..minOf(repCount, 8)).map { i ->
                    VideoSegment(
                        label = "Rep $i",
                        startTimeMs = (i - 1) * repDuration,
                        endTimeMs = i * repDuration,
                        confidence = (85 + Math.random() * 15).toInt()
                    )
                }
            }
            "vertical_jump" -> listOf(
                VideoSegment("Setup", 0, effectiveDuration / 4, 95),
                VideoSegment("Jump", effectiveDuration / 4, effectiveDuration / 2, (90 + Math.random() * 10).toInt()),
                VideoSegment("Landing", effectiveDuration / 2, (effectiveDuration * 0.75).toLong(), 92)
            )
            "shuttle_run" -> (1..minOf(10, (effectiveDuration / 2000).toInt().coerceAtLeast(2))).map { i ->
                val segLen = effectiveDuration / 10
                VideoSegment(
                    label = "Shuttle $i",
                    startTimeMs = (i - 1) * segLen,
                    endTimeMs = i * segLen,
                    confidence = (88 + Math.random() * 12).toInt()
                )
            }
            "endurance_run_800m" -> listOf(
                VideoSegment("Lap 1", 0, effectiveDuration / 2, (90 + Math.random() * 10).toInt()),
                VideoSegment("Lap 2", effectiveDuration / 2, effectiveDuration, (88 + Math.random() * 12).toInt())
            )
            "endurance_run_1600m" -> (1..4).map { i ->
                val segLen = effectiveDuration / 4
                VideoSegment(
                    label = "Lap $i",
                    startTimeMs = (i - 1) * segLen,
                    endTimeMs = i * segLen,
                    confidence = (85 + Math.random() * 15).toInt()
                )
            }
            else -> listOf(
                VideoSegment("Full Recording", 0, effectiveDuration, (90 + Math.random() * 10).toInt())
            )
        }
    }
}
