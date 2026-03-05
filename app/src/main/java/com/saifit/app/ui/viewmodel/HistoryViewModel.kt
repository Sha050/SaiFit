package com.saifit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.repository.ResultRepository
import com.saifit.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryUiState(
    val results: List<TestResult> = emptyList(),
    val athleteName: String = ""
)

class HistoryViewModel(
    private val userRepository: UserRepository,
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        val user = userRepository.currentUser.value ?: return
        val results = resultRepository.getResultsForAthlete(user.id)
        _uiState.value = HistoryUiState(
            results = results.sortedByDescending { it.timestamp },
            athleteName = user.name
        )
    }

    fun getResultById(resultId: String): TestResult? =
        resultRepository.getAllResults().find { it.id == resultId }
}
