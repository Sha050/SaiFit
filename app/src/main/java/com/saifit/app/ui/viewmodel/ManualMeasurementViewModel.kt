package com.saifit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saifit.app.data.model.FitnessTest
import com.saifit.app.data.model.ResultStatus
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.repository.ResultRepository
import com.saifit.app.data.repository.TestRepository
import com.saifit.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualMeasurementUiState(
    val test: FitnessTest? = null,
    val isSubmitting: Boolean = false,
    val result: TestResult? = null,
    val error: String? = null
)

class ManualMeasurementViewModel(
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualMeasurementUiState())
    val uiState: StateFlow<ManualMeasurementUiState> = _uiState.asStateFlow()

    fun loadTest(testId: String) {
        val test = testRepository.getTestById(testId)
        _uiState.update { it.copy(test = test) }
    }

    fun submitMeasurement(value: Double) {
        val test = _uiState.value.test ?: return
        val user = userRepository.currentUser.value ?: return

        _uiState.update { it.copy(isSubmitting = true, error = null) }

        viewModelScope.launch {
            try {

                val result = TestResult(
                    id = "res_${System.currentTimeMillis()}",
                    testId = test.id,
                    testName = test.name,
                    athleteId = user.id,
                    athleteName = user.name,
                    value = value,
                    unit = test.unit,
                    confidencePercent = 100, 
                    status = ResultStatus.VALID,
                    timestamp = System.currentTimeMillis(),
                    videoUri = null,
                    suggestedSport = null,
                    verification = null
                )

                resultRepository.addResult(result)
                _uiState.update { it.copy(isSubmitting = false, result = result) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    fun reset() {
        _uiState.value = ManualMeasurementUiState()
    }
}
