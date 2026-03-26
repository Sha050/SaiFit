package com.saifit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.saifit.app.data.model.*
import com.saifit.app.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class DashboardUiState(
    val user: User? = null,
    val tests: List<FitnessTest> = emptyList(),
    val testStatuses: Map<String, TestCompletionStatus> = emptyMap(),
    val progress: AthleteProgress = AthleteProgress(0, 0, 0, 0, emptyList(), emptyMap()),

    val leaderboardEntries: List<LeaderboardEntry> = emptyList(),
    val selectedTest: String = "All Tests",
    val selectedRegion: String = "All Regions",
    val availableTests: List<String> = emptyList(),
    val availableRegions: List<String> = emptyList()
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val resultRepository: ResultRepository = ResultRepository(),
    private val badgeRepository: BadgeRepository? = null,
    private val benchmarkRepository: BenchmarkRepository? = null,
    private val athleteRepository: AthleteRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        val user = userRepository.currentUser.value ?: return

        viewModelScope.launch {
            userRepository.fetchUsers()
            resultRepository.fetchResults()

            val tests = testRepository.getAllTests()

            val statuses = tests.associate { test ->
                test.id to (badgeRepository?.getTestStatus(user.id, test.id) ?: TestCompletionStatus.NOT_STARTED)
            }

            val progress = badgeRepository?.getProgress(user.id, user) ?: AthleteProgress(
                totalTests = tests.size, completedTests = 0, validResults = 0, retryResults = 0,
                badges = emptyList(), bestResults = emptyMap()
            )

            val allResults = resultRepository.getAllResults()
            val availableTests = allResults.map { it.testName }.distinct().sorted()
            val availableRegions = athleteRepository?.getAllRegions() ?: emptyList()

            val leaderboard = computeLeaderboard(user, allResults, "All Tests", "All Regions")

            _uiState.value = DashboardUiState(
                user = user,
                tests = tests,
                testStatuses = statuses,
                progress = progress,
                leaderboardEntries = leaderboard,
                availableTests = availableTests,
                availableRegions = availableRegions
            )
        }
    }

    fun getTestById(testId: String): FitnessTest? = testRepository.getTestById(testId)

    fun filterLeaderboard(testFilter: String, regionFilter: String) {
        val user = _uiState.value.user ?: return
        val allResults = resultRepository.getAllResults()
        val entries = computeLeaderboard(user, allResults, testFilter, regionFilter)
        _uiState.value = _uiState.value.copy(
            leaderboardEntries = entries,
            selectedTest = testFilter,
            selectedRegion = regionFilter
        )
    }

    private fun computeLeaderboard(
        currentUser: User,
        allResults: List<TestResult>,
        testFilter: String,
        regionFilter: String
    ): List<LeaderboardEntry> {
        val filtered = allResults
            .filter { it.status == ResultStatus.VALID }
            .filter { if (testFilter != "All Tests") it.testName == testFilter else true }

        if (testFilter != "All Tests") {

            val bestByAthlete = filtered.groupBy { it.athleteId }
                .mapValues { (_, results) ->
                    val lowerIsBetter = results.first().testId in setOf("shuttle_run")
                    if (lowerIsBetter) results.minByOrNull { it.value } else results.maxByOrNull { it.value }
                }
                .mapNotNull { (id, result) -> result?.let { id to it } }

            val sorted = bestByAthlete.let { pairs ->
                val lowerIsBetter = pairs.firstOrNull()?.second?.testId in setOf("shuttle_run")
                if (lowerIsBetter) pairs.sortedBy { it.second.value }
                else pairs.sortedByDescending { it.second.value }
            }

            return sorted.mapIndexed { idx, (athleteId, result) ->
                val athlete = athleteRepository?.getAthleteById(athleteId)
                val tier = athlete?.let {
                    benchmarkRepository?.evaluate(result.testId, result.testName, result.value, result.unit, it.age, it.gender)?.tier
                } ?: PerformanceTier.AVERAGE

                LeaderboardEntry(
                    rank = idx + 1,
                    athleteId = athleteId,
                    athleteName = result.athleteName,
                    region = athlete?.region ?: "Unknown",
                    age = athlete?.age ?: 18,
                    gender = athlete?.gender ?: Gender.MALE,
                    value = result.value,
                    unit = result.unit,
                    tier = tier,
                    isCurrentUser = athleteId == currentUser.id
                )
            }
        }

        val byAthlete = filtered.groupBy { it.athleteId }
        val scores = byAthlete.map { (id, results) ->
            val athlete = athleteRepository?.getAthleteById(id)
            val avgConf = results.map { it.confidencePercent }.average()
            Triple(id, results.first().athleteName, avgConf) to athlete
        }.sortedByDescending { it.first.third }

        return scores.mapIndexed { idx, (triple, athlete) ->
            LeaderboardEntry(
                rank = idx + 1,
                athleteId = triple.first,
                athleteName = triple.second,
                region = athlete?.region ?: "Unknown",
                age = athlete?.age ?: 18,
                gender = athlete?.gender ?: Gender.MALE,
                value = Math.round(triple.third * 10.0) / 10.0,
                unit = "% avg",
                tier = when {
                    triple.third >= 90 -> PerformanceTier.ELITE
                    triple.third >= 80 -> PerformanceTier.EXCELLENT
                    triple.third >= 70 -> PerformanceTier.GOOD
                    triple.third >= 60 -> PerformanceTier.AVERAGE
                    else -> PerformanceTier.BELOW_AVERAGE
                },
                isCurrentUser = triple.first == currentUser.id
            )
        }
    }
}
