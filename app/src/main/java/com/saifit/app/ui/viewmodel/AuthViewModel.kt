package com.saifit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.saifit.app.data.model.Gender
import com.saifit.app.data.model.User
import com.saifit.app.data.model.UserRole
import com.saifit.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = userRepository.currentUser

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and Password are required") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val user = userRepository.login(email, password)
            if (user != null) {
                _uiState.update { it.copy(isLoading = false, currentUser = user, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "No account found. Please sign up first.") }
            }
        }
    }

    fun register(
        firstName: String, lastName: String, age: Int, gender: Gender, role: UserRole,
        email: String, phone: String, aadhaar: String, region: String,
        profileImageUri: String? = null
    ) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
            _uiState.update { it.copy(error = "First name, last name, and email are required") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val user = userRepository.register(
                firstName = firstName,
                lastName = lastName,
                age = age,
                gender = gender,
                role = role,
                email = email,
                phoneNumber = phone,
                aadhaarNumber = aadhaar,
                region = region,
                profileImageUri = profileImageUri
            )
            _uiState.update { it.copy(isLoading = false, currentUser = user, error = null) }
        }
    }

    fun logout() {
        userRepository.logout()
        _uiState.update { AuthUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
