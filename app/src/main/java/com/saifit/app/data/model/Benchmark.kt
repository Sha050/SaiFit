package com.saifit.app.data.model

data class Benchmark(
    val testId: String,
    val gender: Gender,
    val ageMin: Int,
    val ageMax: Int,
    val poor: Double,       
    val average: Double,    
    val good: Double,       
    val excellent: Double,  
    val elite: Double       
)

enum class PerformanceTier(val label: String, val emoji: String) {
    BELOW_AVERAGE("Below Average", "🔴"),
    AVERAGE("Average", "🟡"),
    GOOD("Good", "🟢"),
    EXCELLENT("Excellent", "🔵"),
    ELITE("Elite", "🏆")
}

data class BenchmarkComparison(
    val testId: String,
    val testName: String,
    val athleteValue: Double,
    val unit: String,
    val tier: PerformanceTier,
    val percentile: Int,             
    val benchmark: Benchmark?,

    val lowerIsBetter: Boolean = false
)
