package com.saifit.app.data.model

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,        
    val category: BadgeCategory,
    val earned: Boolean = false,
    val earnedTimestamp: Long? = null,

    val progress: Float? = null
)

enum class BadgeCategory {
    MILESTONE,      
    PERFORMANCE,    
    STREAK,         
    SPECIAL         
}

data class AthleteProgress(
    val totalTests: Int,
    val completedTests: Int,
    val validResults: Int,
    val retryResults: Int,
    val badges: List<Badge>,

    val bestResults: Map<String, TestResult>
) {
    val completionPercent: Float
        get() = if (totalTests > 0) completedTests.toFloat() / totalTests else 0f
}
