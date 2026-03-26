package com.saifit.app.data.repository

import com.saifit.app.data.model.*

class BadgeRepository(
    private val testRepository: TestRepository,
    private val resultRepository: ResultRepository,
    private val benchmarkRepository: BenchmarkRepository
) {

    fun getProgress(athleteId: String, athlete: User?): AthleteProgress {
        val allTests = testRepository.getAllTests()
        val athleteResults = resultRepository.getResultsForAthlete(athleteId)
        val validResults = athleteResults.filter { it.status == ResultStatus.VALID }

        val bestByTest = mutableMapOf<String, TestResult>()
        for (result in validResults) {
            val existing = bestByTest[result.testId]
            val lowerIsBetter = result.testId in setOf("shuttle_run")
            if (existing == null ||
                (lowerIsBetter && result.value < existing.value) ||
                (!lowerIsBetter && result.value > existing.value)
            ) {
                bestByTest[result.testId] = result
            }
        }

        val completedTestIds = validResults.map { it.testId }.distinct()

        val badges = computeBadges(
            athlete = athlete,
            allTests = allTests,
            completedTestIds = completedTestIds,
            validResults = validResults,
            bestByTest = bestByTest
        )

        return AthleteProgress(
            totalTests = allTests.size,
            completedTests = completedTestIds.size,
            validResults = validResults.size,
            retryResults = athleteResults.count { it.status == ResultStatus.RETRY },
            badges = badges,
            bestResults = bestByTest
        )
    }

    fun getCompletedTestIds(athleteId: String): Set<String> =
        resultRepository.getResultsForAthlete(athleteId)
            .filter { it.status == ResultStatus.VALID }
            .map { it.testId }
            .toSet()

    fun getTestStatus(athleteId: String, testId: String): TestCompletionStatus {
        val results = resultRepository.getResultsForAthlete(athleteId)
            .filter { it.testId == testId }
        if (results.isEmpty()) return TestCompletionStatus.NOT_STARTED
        if (results.any { it.status == ResultStatus.VALID }) return TestCompletionStatus.COMPLETED
        if (results.any { it.status == ResultStatus.PENDING }) return TestCompletionStatus.PENDING
        return TestCompletionStatus.NEEDS_RETRY
    }

    private fun computeBadges(
        athlete: User?,
        allTests: List<FitnessTest>,
        completedTestIds: List<String>,
        validResults: List<TestResult>,
        bestByTest: Map<String, TestResult>
    ): List<Badge> {
        val badges = mutableListOf<Badge>()

        badges.add(Badge(
            id = "first_test",
            name = "First Step",
            description = "Complete your first fitness test",
            iconName = "star",
            category = BadgeCategory.MILESTONE,
            earned = completedTestIds.isNotEmpty(),
            progress = if (completedTestIds.isNotEmpty()) 1f else 0f
        ))

        badges.add(Badge(
            id = "half_complete",
            name = "Halfway There",
            description = "Complete half of all fitness tests",
            iconName = "trending_up",
            category = BadgeCategory.MILESTONE,
            earned = completedTestIds.size >= allTests.size / 2,
            progress = (completedTestIds.size.toFloat() / (allTests.size / 2).coerceAtLeast(1)).coerceAtMost(1f)
        ))

        badges.add(Badge(
            id = "all_complete",
            name = "Complete Athlete",
            description = "Complete all ${allTests.size} fitness tests",
            iconName = "emoji_events",
            category = BadgeCategory.MILESTONE,
            earned = completedTestIds.distinct().size >= allTests.size,
            progress = completedTestIds.distinct().size.toFloat() / allTests.size.coerceAtLeast(1)
        ))

        badges.add(Badge(
            id = "ten_attempts",
            name = "Dedicated Athlete",
            description = "Record 10 valid test attempts",
            iconName = "repeat",
            category = BadgeCategory.MILESTONE,
            earned = validResults.size >= 10,
            progress = (validResults.size.toFloat() / 10f).coerceAtMost(1f)
        ))

        val hasElite = bestByTest.values.any { result ->
            val comparison = athlete?.let {
                benchmarkRepository.evaluate(result.testId, result.testName, result.value, result.unit, it.age, it.gender)
            }
            comparison?.tier == PerformanceTier.ELITE
        }

        badges.add(Badge(
            id = "elite_performer",
            name = "Elite Performer",
            description = "Score in the Elite tier on any test",
            iconName = "military_tech",
            category = BadgeCategory.PERFORMANCE,
            earned = hasElite
        ))

        val hasExcellent = bestByTest.values.any { result ->
            val comparison = athlete?.let {
                benchmarkRepository.evaluate(result.testId, result.testName, result.value, result.unit, it.age, it.gender)
            }
            comparison?.tier == PerformanceTier.EXCELLENT || comparison?.tier == PerformanceTier.ELITE
        }

        badges.add(Badge(
            id = "top_performer",
            name = "Top Performer",
            description = "Score Excellent or above on any test",
            iconName = "workspace_premium",
            category = BadgeCategory.PERFORMANCE,
            earned = hasExcellent
        ))

        val highConfidence = validResults.any { it.confidencePercent >= 95 }
        badges.add(Badge(
            id = "perfect_form",
            name = "Perfect Form",
            description = "Get 95%+ AI confidence on a test",
            iconName = "verified",
            category = BadgeCategory.PERFORMANCE,
            earned = highConfidence
        ))

        badges.add(Badge(
            id = "speed_demon",
            name = "Speed Demon",
            description = "Complete the Shuttle Run test",
            iconName = "speed",
            category = BadgeCategory.SPECIAL,
            earned = completedTestIds.contains("shuttle_run")
        ))

        badges.add(Badge(
            id = "iron_core",
            name = "Iron Core",
            description = "Score 40+ sit-ups in one attempt",
            iconName = "fitness_center",
            category = BadgeCategory.SPECIAL,
            earned = bestByTest["situps"]?.let { it.value >= 40.0 } ?: false
        ))

        badges.add(Badge(
            id = "sky_high",
            name = "Sky High",
            description = "Jump 50+ cm in vertical jump",
            iconName = "rocket_launch",
            category = BadgeCategory.SPECIAL,
            earned = bestByTest["vertical_jump"]?.let { it.value >= 50.0 } ?: false
        ))

        badges.add(Badge(
            id = "pushup_pro",
            name = "Push-up Pro",
            description = "Complete a Push-ups test",
            iconName = "fitness_center",
            category = BadgeCategory.SPECIAL,
            earned = completedTestIds.contains("pushups")
        ))

        return badges
    }
}

enum class TestCompletionStatus {
    NOT_STARTED,
    PENDING,
    NEEDS_RETRY,
    COMPLETED
}
