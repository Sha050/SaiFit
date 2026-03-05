package com.saifit.app.data.repository

import com.saifit.app.data.model.ResultStatus
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.api.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ResultRepository {

    private val _results = MutableStateFlow<List<TestResult>>(emptyList())
    val results: StateFlow<List<TestResult>> = _results.asStateFlow()

    fun addResult(result: TestResult) {
        _results.update { current -> listOf(result) + current }
    }

    suspend fun fetchResults(athleteId: String? = null) {
        try {
            val fetched = ApiClient.api.getResults(athleteId)
            _results.value = fetched
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getResultsForAthlete(athleteId: String): List<TestResult> =
        _results.value.filter { it.athleteId == athleteId }

    fun getResultsForTest(testId: String): List<TestResult> =
        _results.value.filter { it.testId == testId }

    fun getAllResults(): List<TestResult> = _results.value
}
