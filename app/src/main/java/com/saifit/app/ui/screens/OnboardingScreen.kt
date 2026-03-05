package com.saifit.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.FitnessCenter,
            title = "Welcome to SAI FIT",
            subtitle = "AI-Powered Fitness Assessment",
            description = "Perform standardised fitness tests from anywhere in India. " +
                    "Our AI evaluates your performance and benchmarks you against national standards.",
            accentBrush = Brush.horizontalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(0xFF1565C0),
                    androidx.compose.ui.graphics.Color(0xFF42A5F5)
                )
            )
        ),
        OnboardingPage(
            icon = Icons.Default.CameraAlt,
            title = "Camera Positioning",
            subtitle = "Get the Best Results",
            description = "Place your phone 2-3 metres away on a stable surface. " +
                    "Ensure your full body is visible in the frame. " +
                    "Record in a well-lit area with a plain background for optimal AI analysis.",
            accentBrush = Brush.horizontalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    androidx.compose.ui.graphics.Color(0xFF66BB6A)
                )
            )
        ),
        OnboardingPage(
            icon = Icons.Default.AutoAwesome,
            title = "AI-Powered Analysis",
            subtitle = "Automatic & Fair",
            description = "Our AI automatically detects key movements — sit-up reps, jump height, run speed. " +
                    "It also verifies video authenticity with cheat detection to ensure fair assessment for all athletes.",
            accentBrush = Brush.horizontalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(0xFFE65100),
                    androidx.compose.ui.graphics.Color(0xFFFF9800)
                )
            )
        ),
        OnboardingPage(
            icon = Icons.Default.EmojiEvents,
            title = "Track & Compete",
            subtitle = "Leaderboards & Badges",
            description = "Compare your performance against athletes across India. " +
                    "Earn badges, climb the leaderboard, and get recommended for sports that match your strengths. " +
                    "All data is securely submitted to SAI.",
            accentBrush = Brush.horizontalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(0xFF6A1B9A),
                    androidx.compose.ui.graphics.Color(0xFFAB47BC)
                )
            )
        ),
        OnboardingPage(
            icon = Icons.Default.WifiOff,
            title = "Works Offline",
            subtitle = "No Internet? No Problem!",
            description = "Record tests even without internet connectivity. " +
                    "Your results are securely stored and automatically uploaded when you're back online. " +
                    "Perfect for rural and remote areas across India.",
            accentBrush = Brush.horizontalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color(0xFF00897B),
                    androidx.compose.ui.graphics.Color(0xFF4DB6AC)
                )
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (pagerState.currentPage == index) 24.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(onClick = onComplete) {
                            Text("Skip")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    if (pagerState.currentPage < pages.size - 1) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {

                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Get Started!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentBrush: Brush
)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {

    val scaleAnim = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 32.dp, end = 32.dp, bottom = 120.dp)
            .scale(scaleAnim.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Surface(
            shape = CircleShape,
            modifier = Modifier.size(120.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )
    }
}
