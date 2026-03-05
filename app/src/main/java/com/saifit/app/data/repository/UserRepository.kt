package com.saifit.app.data.repository

import com.saifit.app.data.model.Gender
import com.saifit.app.data.model.User
import com.saifit.app.data.model.UserRole
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserRepository(private val context: android.content.Context) {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val prefs = context.getSharedPreferences("saifit_prefs", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()

    private val users = mutableMapOf<String, User>()

    init {
        val savedUserJson = prefs.getString("logged_in_user", null)
        if (savedUserJson != null) {
            try {
                val user = gson.fromJson(savedUserJson, User::class.java)
                _currentUser.value = user
                users[user.email.lowercase()] = user
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            fetchUsers()
        }
    }

    suspend fun fetchUsers() {
        try {
            val allUsers = com.saifit.app.data.api.ApiClient.api.getUsers()
            allUsers.forEach { user ->
                users[user.email.lowercase()] = user
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun login(email: String, password: String): User? {
        return try {
            val allUsers = com.saifit.app.data.api.ApiClient.api.getUsers()
            val user = allUsers.find { it.email.equals(email.trim(), ignoreCase = true) }
            if (user != null) {

                users[user.email.lowercase()] = user
                _currentUser.value = user
                prefs.edit().putString("logged_in_user", gson.toJson(user)).apply()
            }
            user
        } catch (e: Exception) {
            e.printStackTrace()

            val localUser = users[email.trim().lowercase()]
            if (localUser != null) {
                _currentUser.value = localUser
                prefs.edit().putString("logged_in_user", gson.toJson(localUser)).apply()
            }
            localUser
        }
    }

    suspend fun register(
        firstName: String, lastName: String, age: Int, gender: Gender, role: UserRole,
        email: String, phoneNumber: String, aadhaarNumber: String, region: String,
        profileImageUri: String? = null
    ): User {
        val user = User(
            id = "", 
            firstName = firstName,
            lastName = lastName,
            age = age,
            gender = gender,
            role = role,
            email = email,
            phoneNumber = phoneNumber,
            aadhaarNumber = aadhaarNumber,
            region = region,
            profileImageUri = profileImageUri
        )

        try {
            val savedUser = com.saifit.app.data.api.ApiClient.api.registerUser(user)
            users[savedUser.email.lowercase()] = savedUser
            _currentUser.value = savedUser
            prefs.edit().putString("logged_in_user", gson.toJson(savedUser)).apply()
            return savedUser
        } catch (e: Exception) {
            e.printStackTrace()

            val localSave = user.copy(id = "user_${System.currentTimeMillis()}")
            users[email.trim().lowercase()] = localSave
            _currentUser.value = localSave
            prefs.edit().putString("logged_in_user", gson.toJson(localSave)).apply()
            return localSave
        }
    }

    fun getUserById(id: String): User? = users.values.find { it.id == id }

    fun getAllAthletes(): List<User> = users.values.filter { it.role == UserRole.ATHLETE }

    fun updateProfile(user: User) {
        _currentUser.value = user
        users[user.email.trim().lowercase()] = user
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user").apply()
    }
}
