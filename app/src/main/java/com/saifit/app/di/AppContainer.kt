package com.saifit.app.di

import com.saifit.app.data.repository.AthleteRepository
import com.saifit.app.data.repository.BadgeRepository
import com.saifit.app.data.repository.BenchmarkRepository
import com.saifit.app.data.repository.ResultRepository
import com.saifit.app.data.repository.SubmissionRepository
import com.saifit.app.data.repository.TestRepository
import com.saifit.app.data.repository.UserRepository

class AppContainer(private val context: android.content.Context) {
    val userRepository = UserRepository(context)
    val testRepository = TestRepository()
    val resultRepository = ResultRepository()
    val athleteRepository = AthleteRepository(userRepository)
    val benchmarkRepository = BenchmarkRepository()
    val badgeRepository = BadgeRepository(testRepository, resultRepository, benchmarkRepository)
    val submissionRepository = SubmissionRepository()
}
