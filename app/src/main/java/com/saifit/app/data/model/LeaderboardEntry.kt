package com.saifit.app.data.model

data class LeaderboardEntry(
    val rank: Int,
    val athleteId: String,
    val athleteName: String,
    val region: String,
    val age: Int,
    val gender: Gender,
    val value: Double,
    val unit: String,
    val tier: PerformanceTier,
    val isCurrentUser: Boolean = false
)
