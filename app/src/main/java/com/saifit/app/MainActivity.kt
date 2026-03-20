package com.saifit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saifit.app.data.model.Submission
import com.saifit.app.data.model.SubmissionStatus
import com.saifit.app.data.model.UserRole
import com.saifit.app.navigation.Route
import com.saifit.app.ui.screens.*
import com.saifit.app.ui.theme.SaiFitTheme
import com.saifit.app.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SaiFitApplication).container

        setContent {
            SaiFitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SaiFitApp(appContainer)
                }
            }
        }
    }
}

@Composable
private fun SaiFitApp(container: com.saifit.app.di.AppContainer) {
    val navController = rememberNavController()

    var hasCompletedOnboarding by remember { mutableStateOf(false) }

    val authViewModel = remember {
        AuthViewModel(container.userRepository)
    }
    val dashboardViewModel = remember {
        DashboardViewModel(
            container.userRepository,
            container.testRepository,
            container.resultRepository,
            container.badgeRepository,
            container.benchmarkRepository,
            container.athleteRepository
        )
    }
    val recordingViewModel = remember {
        RecordingViewModel(
            navController.context.applicationContext,
            container.userRepository,
            container.testRepository,
            container.resultRepository
        )
    }
    val historyViewModel = remember {
        HistoryViewModel(container.userRepository, container.resultRepository)
    }
    val manualMeasurementViewModel = remember {
        ManualMeasurementViewModel(
            container.userRepository,
            container.testRepository,
            container.resultRepository
        )
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash.path
    ) {

        composable(Route.Splash.path) {
            SplashScreen(
                onSplashComplete = {
                    val currentUser = container.userRepository.currentUser.value
                    val destination = when {
                        currentUser != null -> {
                            if (currentUser.role == UserRole.ADMIN) Route.AdminDashboard.path
                            else Route.AthleteDashboard.path
                        }
                        !hasCompletedOnboarding -> Route.Onboarding.path
                        else -> Route.Auth.path
                    }
                    navController.navigate(destination) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onComplete = {
                    hasCompletedOnboarding = true

                    val currentUser = container.userRepository.currentUser.value
                    if (currentUser != null) {
                        if (currentUser.role == UserRole.ADMIN) navController.navigate(Route.AdminDashboard.path) { popUpTo(0) }
                        else navController.navigate(Route.AthleteDashboard.path) { popUpTo(0) }
                    } else {
                        navController.navigate(Route.Auth.path) {
                            popUpTo(Route.Onboarding.path) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Route.Auth.path) {
            val authState by authViewModel.uiState.collectAsState()

            LaunchedEffect(authState.currentUser) {
                val user = authState.currentUser
                if (user != null) {
                    val route = if (user.role == UserRole.ATHLETE) {
                        dashboardViewModel.loadDashboard()
                        Route.AthleteDashboard.path
                    } else {
                        Route.AdminDashboard.path
                    }
                    navController.navigate(route) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                }
            }

            AuthScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.error,
                onLogin = { email, password ->
                    authViewModel.login(email, password)
                },
                onRegister = { firstName, lastName, age, gender, role, email, phone, aadhaar, region, profileImageUri ->
                    authViewModel.register(firstName, lastName, age, gender, role, email, phone, aadhaar, region, profileImageUri)
                },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable(Route.AthleteDashboard.path) {
            val uiState by dashboardViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { dashboardViewModel.loadDashboard() }

            if (uiState.user != null) {
                DashboardScreen(
                    user = uiState.user!!,
                    tests = uiState.tests,
                    testStatuses = uiState.testStatuses,
                    progress = uiState.progress,
                    onTestClick = { test ->
                        navController.navigate(Route.TestInstruction.createRoute(test.id))
                    },
                    onHistoryClick = {
                        historyViewModel.loadHistory()
                        navController.navigate(Route.History.path)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Route.Auth.path) {
                            popUpTo(0) { inclusive = true }
                        }
                    },

                    leaderboardContent = {
                        LeaderboardScreen(
                            currentUserId = uiState.user!!.id,
                            entries = uiState.leaderboardEntries,
                            availableTests = uiState.availableTests,
                            availableRegions = uiState.availableRegions,
                            selectedTest = uiState.selectedTest,
                            selectedRegion = uiState.selectedRegion,
                            onTestFilter = { test ->
                                dashboardViewModel.filterLeaderboard(test, uiState.selectedRegion)
                            },
                            onRegionFilter = { region ->
                                dashboardViewModel.filterLeaderboard(uiState.selectedTest, region)
                            }
                        )
                    },

                    badgesContent = {
                        BadgesScreen(
                            progress = uiState.progress,
                            athleteName = uiState.user!!.name
                        )
                    },

                    profileContent = {
                        ProfileScreenContent(
                            user = uiState.user!!,
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate(Route.Auth.path) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onSettingsClick = {
                                navController.navigate(Route.Settings.path)
                            },
                            onSubmissionQueueClick = {
                                navController.navigate(Route.SubmissionQueue.path)
                            }
                        )
                    }
                )
            }
        }

        composable(Route.AdminDashboard.path) {
            val currentUser by container.userRepository.currentUser.collectAsState()

            LaunchedEffect(Unit) {
                container.userRepository.fetchUsers()
                container.resultRepository.fetchResults()
            }

            if (currentUser != null) {
                val allResults by container.resultRepository.results.collectAsState()
                val availableTests = allResults.map { it.testName }.distinct().sorted()
                val availableRegions = container.athleteRepository.getAllRegions()

                AdminDashboardScreen(
                    user = currentUser!!,
                    allResults = allResults,
                    availableTests = availableTests,
                    availableRegions = availableRegions,
                    allAthletes = container.athleteRepository.getMockAthletes(),
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Route.Auth.path) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onCandidateClick = { athleteId ->
                        navController.navigate(Route.AdminAthleteProfile.createRoute(athleteId))
                    }
                )
            }
        }

        composable(
            route = Route.AdminAthleteProfile.path,
            arguments = listOf(navArgument("athleteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val athleteId = backStackEntry.arguments?.getString("athleteId") ?: return@composable
            val allResults by container.resultRepository.results.collectAsState()
            val results = allResults.filter { it.athleteId == athleteId }
            val athlete = container.athleteRepository.getAthleteById(athleteId)

            AdminAthleteProfileScreen(
                athlete = athlete,
                results = results,
                onBack = { navController.popBackStack() },
                onSendToSaiClick = {
                    navController.navigate(Route.AdminToSai.createRoute(athleteId))
                }
            )
        }

        composable(
            route = Route.AdminToSai.path,
            arguments = listOf(navArgument("athleteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val athleteId = backStackEntry.arguments?.getString("athleteId") ?: return@composable
            val allResults by container.resultRepository.results.collectAsState()
            val results = allResults.filter { it.athleteId == athleteId }
            val athlete = container.athleteRepository.getAthleteById(athleteId)

            AdminToSaiScreen(
                athlete = athlete,
                results = results,
                onBack = { navController.popBackStack() },
                onSendSuccess = {
                    navController.navigate(Route.AdminDashboard.path) {
                        popUpTo(Route.AdminDashboard.path) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.TestInstruction.path,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
            val test = dashboardViewModel.getTestById(testId) ?: return@composable

            TestInstructionScreen(
                test = test,
                onStartRecording = {
                    if (test.category == com.saifit.app.data.model.TestCategory.ANTHROPOMETRIC) {
                        manualMeasurementViewModel.loadTest(testId)
                        navController.navigate(Route.ManualMeasurement.createRoute(testId))
                    } else {
                        recordingViewModel.loadTest(testId)
                        navController.navigate(Route.VideoRecording.createRoute(testId))
                    }
                },
                onUploadVideo = { uri ->
                    val context = navController.context
                    val tempFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.mp4")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        recordingViewModel.loadTest(testId)
                        recordingViewModel.updateDuration(30000L) 

                        recordingViewModel.startRecording() 
                        recordingViewModel.stopRecording(videoUri = tempFile.absolutePath)
                        navController.navigate(Route.VideoReview.createRoute(testId))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.VideoRecording.path,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
            val uiState by recordingViewModel.uiState.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(testId) { recordingViewModel.loadTest(testId) }

            LaunchedEffect(uiState.isRecording) {
                if (uiState.isRecording) {
                    while (true) {
                        delay(100)
                        recordingViewModel.updateDuration(uiState.recordingDurationMs + 100)
                    }
                }
            }

            LaunchedEffect(uiState.recordingStopped) {
                if (uiState.recordingStopped) {
                    navController.navigate(Route.VideoReview.createRoute(testId)) {
                        popUpTo(Route.VideoRecording.path) { inclusive = true }
                    }
                }
            }

            VideoRecordingScreen(
                test = uiState.test,
                isRecording = uiState.isRecording,
                isEvaluating = uiState.isEvaluating,
                recordingDurationMs = uiState.recordingDurationMs,
                onStartRecording = { recordingViewModel.startRecording() },
                onUpdateDuration = { ms -> recordingViewModel.updateDuration(ms) },
                onRequestStopRecording = {

                    recordingViewModel.requestStopRecording()
                },
                onStopRecording = { actualVideoUri ->

                    recordingViewModel.stopRecording(videoUri = actualVideoUri)
                },
                onBack = {
                    recordingViewModel.reset()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.ManualMeasurement.path,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
            val uiState by manualMeasurementViewModel.uiState.collectAsState()

            LaunchedEffect(testId) { manualMeasurementViewModel.loadTest(testId) }

            LaunchedEffect(uiState.result) {
                uiState.result?.let { result ->

                    container.submissionRepository.addSubmission(
                        Submission(
                            id = "sub_${System.currentTimeMillis()}",
                            resultId = result.id,
                            testId = result.testId,
                            testName = result.testName,
                            athleteId = result.athleteId,
                            athleteName = result.athleteName,
                            videoUri = null,
                            status = SubmissionStatus.QUEUED
                        )
                    )
                    navController.navigate(Route.EvaluationResult.createRoute(result.id)) {
                        popUpTo(Route.AthleteDashboard.path)
                    }
                    manualMeasurementViewModel.reset()
                }
            }

            ManualMeasurementScreen(
                test = uiState.test,
                isSubmitting = uiState.isSubmitting,
                error = uiState.error,
                onSubmit = { value ->
                    manualMeasurementViewModel.submitMeasurement(value)
                },
                onBack = {
                    manualMeasurementViewModel.reset()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.VideoReview.path,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: return@composable
            val uiState by recordingViewModel.uiState.collectAsState()

            val mockSegments = uiState.mockSegments

            LaunchedEffect(uiState.result) {
                uiState.result?.let { result ->

                    container.submissionRepository.addSubmission(
                        Submission(
                            id = "sub_${System.currentTimeMillis()}",
                            resultId = result.id,
                            testId = result.testId,
                            testName = result.testName,
                            athleteId = result.athleteId,
                            athleteName = result.athleteName,
                            videoUri = result.videoUri,
                            status = SubmissionStatus.QUEUED
                        )
                    )
                    navController.navigate(Route.EvaluationResult.createRoute(result.id)) {
                        popUpTo(Route.AthleteDashboard.path)
                    }
                    recordingViewModel.reset()
                }
            }

            VideoReviewScreen(
                test = uiState.test,
                videoUri = uiState.videoUri,
                recordingDurationMs = uiState.recordingDurationMs,
                segments = mockSegments,
                isEvaluating = uiState.isEvaluating,
                errorMessage = uiState.error,
                onConfirmAndEvaluate = {
                    recordingViewModel.confirmAndEvaluate()
                },
                onDismissError = { recordingViewModel.clearError() },
                onRetake = {
                    recordingViewModel.reset()
                    recordingViewModel.loadTest(testId)
                    navController.navigate(Route.VideoRecording.createRoute(testId)) {
                        popUpTo(Route.AthleteDashboard.path)
                    }
                },
                onBack = {
                    recordingViewModel.reset()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.EvaluationResult.path,
            arguments = listOf(navArgument("resultId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId") ?: return@composable
            val result = historyViewModel.getResultById(resultId)

            val currentUser by container.userRepository.currentUser.collectAsState()
            val benchmarkComparison = result?.let { r ->
                currentUser?.let { user ->
                    container.benchmarkRepository.evaluate(
                        r.testId, r.testName, r.value, r.unit, user.age, user.gender
                    )
                }
            }

            EvaluationResultScreen(
                result = result,
                benchmarkComparison = benchmarkComparison,
                onBackToDashboard = {
                    dashboardViewModel.loadDashboard() 
                    navController.navigate(Route.AthleteDashboard.path) {
                        popUpTo(Route.AthleteDashboard.path) { inclusive = true }
                    }
                },
                onViewHistory = {
                    historyViewModel.loadHistory()
                    navController.navigate(Route.History.path)
                },
                onRetryTest = {
                    result?.testId?.let { testId ->
                        navController.navigate(Route.VideoRecording.createRoute(testId)) {
                            popUpTo(Route.AthleteDashboard.path)
                        }
                    }
                }
            )
        }

        composable(Route.History.path) {
            val uiState by historyViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { historyViewModel.loadHistory() }

            HistoryScreen(
                results = uiState.results,
                athleteName = uiState.athleteName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onClearData = {

                    navController.popBackStack()
                }
            )
        }

        composable(Route.SubmissionQueue.path) {
            val submissions by container.submissionRepository.submissions.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            SubmissionQueueScreen(
                submissions = submissions,
                isOnline = true, 
                pendingCount = container.submissionRepository.getPendingCount(),
                submittedCount = container.submissionRepository.getSubmittedCount(),
                onRetry = { id -> container.submissionRepository.retrySubmission(id) },
                onUploadAll = { 
                    coroutineScope.launch {
                        container.submissionRepository.uploadAll() 
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Profile.path) {
            val currentUser by container.userRepository.currentUser.collectAsState()

            ProfileScreen(
                user = currentUser,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Route.Auth.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
