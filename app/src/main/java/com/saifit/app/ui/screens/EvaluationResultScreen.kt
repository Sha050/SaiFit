package com.saifit.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saifit.app.data.model.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.saifit.app.data.api.ApiClient

@Composable
fun EvaluationResultScreen(
    result: TestResult?,
    benchmarkComparison: BenchmarkComparison? = null,
    onBackToDashboard: () -> Unit,
    onViewHistory: () -> Unit,
    onRetryTest: () -> Unit
) {
    if (result == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No result available")
        }
        return
    }

    val context = LocalContext.current

    val scaleAnim = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f))
    }

    val isValid = result.status == ResultStatus.VALID
    val statusColor = if (isValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val statusText = if (isValid) "VALID" else "RETRY"
    val statusIcon = if (isValid) Icons.Default.CheckCircle else Icons.Default.Refresh

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(scaleAnim.value)
                ) {

                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                statusIcon,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${result.value}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 56.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = result.unit,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = result.testName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Athlete: ${result.athleteName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                result.verification?.let { verification ->
                    VerificationCard(verification)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                result.suggestedSport?.let { sport ->
                    SportRecommendationCard(sport)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (benchmarkComparison != null) {
                    BenchmarkCard(benchmarkComparison)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (result.videoUri != null && result.videoUri.startsWith("/uploads")) {
                    val fullVideoStr = "http://${ApiClient.currentIp}:8000${result.videoUri}"
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(fullVideoStr), "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Watch AI Analysis (Pose Skeleton)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (result.verification != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AI Confidence",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { result.confidencePercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = if (result.confidencePercent >= 80)
                                    MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${result.confidencePercent}% confidence",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (result.confidencePercent >= 80)
                                    "High confidence — result is reliable."
                                else "Low confidence — consider retaking the test.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Completed just now",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isValid) {
                    OutlinedButton(
                        onClick = onRetryTest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry This Test")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Dashboard")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onViewHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View All Results")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun VerificationCard(verification: VerificationResult) {
    val authColor = if (verification.isAuthentic)
        MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.error

    val animatedScore by animateFloatAsState(
        targetValue = verification.authenticityScore / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "auth_score_anim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = authColor.copy(alpha = 0.06f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (verification.isAuthentic) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                    contentDescription = null,
                    tint = authColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = authColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (verification.isAuthentic) "AUTHENTIC" else "FLAGGED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = authColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Authenticity Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { animatedScore },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = authColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${verification.authenticityScore}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = authColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Movement Quality: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    verification.movementQuality.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (verification.movementQuality) {
                        MovementQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                        MovementQuality.GOOD -> MaterialTheme.colorScheme.secondary
                        MovementQuality.ACCEPTABLE -> MaterialTheme.colorScheme.onSurface
                        MovementQuality.POOR -> MaterialTheme.colorScheme.error
                        MovementQuality.INVALID -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (verification.tamperDetected) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (verification.tamperDetected) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (verification.tamperDetected) "Tampering detected!"
                           else "No tampering detected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (verification.tamperDetected) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (verification.flags.isNotEmpty() && verification.flags.any { it.type != FlagType.NONE }) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Flags",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                verification.flags.filter { it.type != FlagType.NONE }.forEach { flag ->
                    val flagColor = when (flag.severity) {
                        FlagSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                        FlagSeverity.WARNING -> MaterialTheme.colorScheme.secondary
                        FlagSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(flagColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            flag.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = flagColor
                        )
                    }
                }
            }

            if (verification.segments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Auto-Detected: ${verification.segments.size} segments",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(verification.segments) { seg ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "${seg.label} (${seg.confidence}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SportRecommendationCard(sport: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "AI Sport Recommendation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sport,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun BenchmarkCard(comparison: BenchmarkComparison) {
    val tierColor = when (comparison.tier) {
        PerformanceTier.ELITE -> MaterialTheme.colorScheme.primary
        PerformanceTier.EXCELLENT -> MaterialTheme.colorScheme.secondary
        PerformanceTier.GOOD -> MaterialTheme.colorScheme.tertiary
        PerformanceTier.AVERAGE -> MaterialTheme.colorScheme.onSurfaceVariant
        PerformanceTier.BELOW_AVERAGE -> MaterialTheme.colorScheme.error
    }

    val animatedPercentile by animateFloatAsState(
        targetValue = comparison.percentile / 100f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "percentile_anim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = tierColor.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Insights,
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Performance Benchmark",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    comparison.tier.emoji,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    comparison.tier.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = tierColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Percentile Rank",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedPercentile },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.small),
                color = tierColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${comparison.percentile}th percentile",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = tierColor
                )
                Text(
                    "for your age & gender",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (comparison.benchmark != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                val b = comparison.benchmark
                val direction = if (comparison.lowerIsBetter) "lower is better" else "higher is better"
                Text(
                    "Benchmarks ($direction):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BenchmarkThreshold("Avg", b.average, comparison.unit)
                    BenchmarkThreshold("Good", b.good, comparison.unit)
                    BenchmarkThreshold("Excl", b.excellent, comparison.unit)
                    BenchmarkThreshold("Elite", b.elite, comparison.unit)
                }
            }
        }
    }
}

@Composable
private fun BenchmarkThreshold(label: String, value: Double, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${value.let { if (it == it.toLong().toDouble()) it.toLong().toString() else String.format("%.1f", it) }}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
