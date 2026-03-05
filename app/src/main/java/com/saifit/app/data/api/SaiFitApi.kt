package com.saifit.app.data.api

import com.saifit.app.data.model.LeaderboardEntry
import com.saifit.app.data.model.Submission
import com.saifit.app.data.model.User
import com.saifit.app.data.model.TestResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface SaiFitApi {
    @POST("users/")
    suspend fun registerUser(@Body user: User): User

    @GET("users/")
    suspend fun getUsers(): List<User>

    @POST("submissions/")
    suspend fun createSubmission(@Body submission: SubmissionRequestDto): SubmissionResponseDto

    @GET("submissions/")
    suspend fun getSubmissions(@Query("athlete_id") athleteId: String? = null): List<Submission>

    @GET("submissions/results")
    suspend fun getResults(@Query("athlete_id") athleteId: String? = null): List<TestResult>

    @GET("leaderboard/")
    suspend fun getLeaderboard(
        @Query("test_name") testName: String,
        @Query("gender") gender: String,
        @Query("age_category") ageCategory: String
    ): List<LeaderboardEntry>

    @POST("evaluation/evaluate")
    suspend fun evaluateVideo(@Body request: EvaluationRequestDto): EvaluationResultDto

    @Multipart
    @POST("upload/")
    suspend fun uploadVideo(@Part file: okhttp3.MultipartBody.Part): UploadResponseDto
}

data class EvaluationResultDto(
    val id: String? = null,
    val test_id: String,
    val test_name: String,
    val athlete_id: String,
    val athlete_name: String,
    val value: Double,
    val unit: String,
    val confidence_percent: Int,
    val status: String,
    val video_uri: String? = null,
    val timestamp: Long? = null,
    val suggested_sport: String? = null,
    val verification: com.saifit.app.data.model.VerificationResult? = null
)

data class EvaluationRequestDto(
    val test_id: String,
    val test_name: String,
    val unit: String,
    val athlete_id: String,
    val athlete_name: String,
    val video_uri: String?,
    val recording_duration_ms: Long
)

data class UploadResponseDto(
    val video_uri: String,
    val filename: String,
    val status: String
)

data class SubmissionRequestDto(
    val result_id: String,
    val test_name: String,
    val athlete_id: String,
    val athlete_name: String,
    val video_uri: String?,
    val status: String,
    val progress: Float = 0f,
    val error_message: String? = null,
    val test_result_data: TestResult? = null
)

data class SubmissionResponseDto(
    val id: String,
    val athlete_id: String,
    val test_id: String,
    val status: String,
    val video_uri: String?
)
