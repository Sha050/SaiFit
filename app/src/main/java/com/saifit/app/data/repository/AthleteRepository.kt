package com.saifit.app.data.repository

import com.saifit.app.data.model.Gender
import com.saifit.app.data.model.User
import com.saifit.app.data.model.UserRole

class AthleteRepository(
    private val userRepository: UserRepository? = null
) {

    fun getMockAthletes(): List<User> {
        return userRepository?.getAllAthletes() ?: emptyList()
    }

    fun getAthleteById(id: String): User? =
        userRepository?.getUserById(id)

    fun getAthletesByRegion(region: String): List<User> =
        getMockAthletes().filter { it.region.equals(region, ignoreCase = true) }

    fun getAllRegions(): List<String> = getMockAthletes().mapNotNull { it.region }.distinct().sorted()
}
