package com.saifit.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import com.saifit.app.data.model.*
import com.saifit.app.data.repository.TestCompletionStatus
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    tests: List<FitnessTest>,
    testStatuses: Map<String, TestCompletionStatus>,
    progress: AthleteProgress,

    onTestClick: (FitnessTest) -> Unit,
    onHistoryClick: () -> Unit,
    onLogout: () -> Unit,

    leaderboardContent: @Composable () -> Unit,
    badgesContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAI FIT", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(badge = {}) {
                            Icon(Icons.Default.Leaderboard, contentDescription = null)
                        }
                    },
                    label = { Text("Rank", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        val earnedCount = progress.badges.count { it.earned }
                        BadgedBox(
                            badge = {
                                if (earnedCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("$earnedCount", color = Color.White) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null)
                        }
                    },
                    label = { Text("Badges", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text("Profile", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> HomeTab(user, tests, testStatuses, progress, onTestClick)
                1 -> leaderboardContent()
                2 -> badgesContent()
                3 -> profileContent()
            }
        }
    }
}

@Composable
private fun HomeTab(
    user: User,
    tests: List<FitnessTest>,
    testStatuses: Map<String, TestCompletionStatus>,
    progress: AthleteProgress,
    onTestClick: (FitnessTest) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            val scaleAnim = remember { Animatable(0.9f) }
            LaunchedEffect(Unit) {
                scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim.value),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(72.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f }, 
                                modifier = Modifier.size(72.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 6.dp
                            )
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress.completionPercent,
                                animationSpec = tween(1500, easing = FastOutSlowInEasing),
                                label = "progressAnim"
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(72.dp),
                                color = Color.White,
                                strokeWidth = 6.dp,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                        Column {
                            Text(
                                text = "Welcome back,\n${user.firstName}!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${progress.completedTests}/${progress.totalTests} Tests • ${progress.badges.count { it.earned }} Badges",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Your Assessments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Select a test to record and evaluate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(tests.size) { index ->
            val test = tests[index]
            val status = testStatuses[test.id] ?: TestCompletionStatus.NOT_STARTED

            val enterAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 100L)
                enterAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(enterAnim.value)
                    .graphicsLayer { alpha = enterAnim.value }
            ) {
                TestCard(test = test, status = status, onClick = { onTestClick(test) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TestCard(test: FitnessTest, status: TestCompletionStatus, onClick: () -> Unit) {
    val icon = when (test.category) {
        TestCategory.ANTHROPOMETRIC -> Icons.Default.Straighten
        TestCategory.STRENGTH       -> Icons.Default.FitnessCenter
        TestCategory.POWER          -> Icons.Default.ArrowUpward
        TestCategory.AGILITY        -> Icons.Default.DirectionsRun
        TestCategory.ENDURANCE      -> Icons.Default.Timer
    }
    val categoryColor = when (test.category) {
        TestCategory.ANTHROPOMETRIC -> MaterialTheme.colorScheme.tertiary
        TestCategory.STRENGTH       -> MaterialTheme.colorScheme.primary
        TestCategory.POWER          -> MaterialTheme.colorScheme.secondary
        TestCategory.AGILITY        -> MaterialTheme.colorScheme.error
        TestCategory.ENDURANCE      -> MaterialTheme.colorScheme.primary
    }

    val statusIcon = when (status) {
        TestCompletionStatus.COMPLETED   -> Icons.Default.CheckCircle
        TestCompletionStatus.NEEDS_RETRY -> Icons.Default.Refresh
        TestCompletionStatus.PENDING     -> Icons.Default.HourglassBottom
        TestCompletionStatus.NOT_STARTED -> Icons.Default.PlayArrow
    }
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            TestCompletionStatus.COMPLETED   -> MaterialTheme.colorScheme.tertiary
            TestCompletionStatus.NEEDS_RETRY -> MaterialTheme.colorScheme.error
            TestCompletionStatus.PENDING     -> MaterialTheme.colorScheme.secondary
            TestCompletionStatus.NOT_STARTED -> MaterialTheme.colorScheme.primary
        },
        label = "status_color"
    )
    val statusLabel = when (status) {
        TestCompletionStatus.COMPLETED   -> "Done"
        TestCompletionStatus.NEEDS_RETRY -> "Retry"
        TestCompletionStatus.PENDING     -> "Pending"
        TestCompletionStatus.NOT_STARTED -> "Record"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = categoryColor.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(test.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${test.category.name.lowercase().replaceFirstChar { it.uppercase() }}  •  Unit: ${test.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        statusIcon,
                        contentDescription = statusLabel,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
