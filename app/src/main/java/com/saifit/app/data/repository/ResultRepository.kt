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
        upsertResult(result)
    }

    suspend fun saveResult(result: TestResult): TestResult {
        return try {
            val saved = ApiClient.api.createResult(result)
            upsertResult(saved)
            saved
        } catch (e: Exception) {
            e.printStackTrace()
            upsertResult(result)
            result
        }
    }

    suspend fun fetchResults(athleteId: String? = null) {
        try {
            val fetched = ApiClient.api.getResults(athleteId)
            val merged = _results.value.associateBy { it.id }.toMutableMap()
            fetched.forEach { result ->
                merged[result.id] = result
            }
            _results.value = merged.values.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getResultsForAthlete(athleteId: String): List<TestResult> =
        _results.value.filter { it.athleteId == athleteId }

    fun getResultsForTest(testId: String): List<TestResult> =
        _results.value.filter { it.testId == testId }

    fun getAllResults(): List<TestResult> = _results.value

    private fun upsertResult(result: TestResult) {
        _results.update { current ->
            (listOf(result) + current.filterNot { it.id == result.id })
                .sortedByDescending { it.timestamp }
        }
    }
}
