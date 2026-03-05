package com.saifit.app.navigation

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Onboarding : Route("onboarding")
    data object Auth : Route("auth")
    data object AthleteDashboard : Route("athlete_dashboard")
    data object AdminDashboard : Route("admin_dashboard")
    data object Profile : Route("profile")
    data object History : Route("history")
    data object Settings : Route("settings")
    data object SubmissionQueue : Route("submission_queue")

    data object TestInstruction : Route("test_instruction/{testId}") {
        fun createRoute(testId: String) = "test_instruction/$testId"
    }

    data object VideoRecording : Route("video_recording/{testId}") {
        fun createRoute(testId: String) = "video_recording/$testId"
    }

    data object ManualMeasurement : Route("manual_measurement/{testId}") {
        fun createRoute(testId: String) = "manual_measurement/$testId"
    }

    data object VideoReview : Route("video_review/{testId}") {
        fun createRoute(testId: String) = "video_review/$testId"
    }

    data object EvaluationResult : Route("evaluation_result/{resultId}") {
        fun createRoute(resultId: String) = "evaluation_result/$resultId"
    }

    data object AdminAthleteProfile : Route("admin_athlete_profile/{athleteId}") {
        fun createRoute(athleteId: String) = "admin_athlete_profile/$athleteId"
    }

    data object AdminToSai : Route("admin_to_sai/{athleteId}") {
        fun createRoute(athleteId: String) = "admin_to_sai/$athleteId"
    }
}
