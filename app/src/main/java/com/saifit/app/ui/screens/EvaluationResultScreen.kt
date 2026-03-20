package com.saifit.app.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.saifit.app.data.api.ApiClient
import com.saifit.app.data.model.BenchmarkComparison
import com.saifit.app.data.model.FlagSeverity
import com.saifit.app.data.model.FlagType
import com.saifit.app.data.model.MovementQuality
import com.saifit.app.data.model.PerformanceTier
import com.saifit.app.data.model.ResultStatus
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.model.VerificationResult
import com.saifit.app.data.model.VideoSegment
import kotlinx.coroutines.delay
import java.io.File

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

                if (result.videoUri != null) {
                    AiAnalysisPlayerCard(
                        testName = result.testName,
                        videoUri = result.videoUri,
                        segments = result.verification?.segments.orEmpty()
                    )
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
private fun AiAnalysisPlayerCard(
    testName: String,
    videoUri: String,
    segments: List<VideoSegment>
) {
    val resolvedUri = remember(videoUri) { resolveAnalysisVideoUri(videoUri) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var playbackPositionMs by remember(videoUri) { mutableLongStateOf(0L) }
    var durationMs by remember(videoUri) { mutableLongStateOf(1L) }
    var isPrepared by remember(videoUri) { mutableStateOf(false) }
    var isPlaying by remember(videoUri) { mutableStateOf(false) }
    var hasPlaybackError by remember(videoUri) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    LaunchedEffect(videoView, isPrepared) {
        while (true) {
            val currentView = videoView
            if (currentView != null && isPrepared) {
                playbackPositionMs = currentView.currentPosition.toLong()
                durationMs = currentView.duration.toLong().coerceAtLeast(1L)
                isPlaying = currentView.isPlaying
            }
            delay(120)
        }
    }

    val repSegments = remember(segments) {
        segments.filter { it.label.startsWith("Rep", ignoreCase = true) }
    }
    val shuttleSegments = remember(segments) {
        segments.filter { it.label.startsWith("Shuttle", ignoreCase = true) }
    }
    val completedSegments = remember(playbackPositionMs, segments) {
        segments.count { playbackPositionMs >= it.endTimeMs }
    }
    val completedRepSegments = remember(playbackPositionMs, repSegments) {
        repSegments.count { playbackPositionMs >= it.endTimeMs }
    }
    val completedShuttleSegments = remember(playbackPositionMs, shuttleSegments) {
        shuttleSegments.count { playbackPositionMs >= it.endTimeMs }
    }
    val activeSegment = remember(playbackPositionMs, segments) {
        segments.firstOrNull { playbackPositionMs in it.startTimeMs until it.endTimeMs }
    }
    val isRepBased = remember(testName, repSegments) {
        testName.contains("sit", ignoreCase = true) && repSegments.isNotEmpty()
    }
    val isShuttleBased = remember(testName, shuttleSegments) {
        testName.contains("shuttle", ignoreCase = true) || shuttleSegments.isNotEmpty()
    }
    val shuttleTarget = remember(isShuttleBased, shuttleSegments) {
        if (isShuttleBased) maxOf(10, shuttleSegments.size) else 0
    }
    val liveMetricLabel = when {
        isRepBased -> "Live reps"
        isShuttleBased -> "Live shuttles"
        else -> "Live stage"
    }
    val liveValueText = when {
        isRepBased -> "${completedRepSegments}/${repSegments.size}"
        isShuttleBased -> "${completedShuttleSegments}/${shuttleTarget}"
        else -> activeSegment?.label ?: if (segments.isNotEmpty() && completedSegments >= segments.size) "Complete" else "Analyzing"
    }
    val liveCaption = when {
        isRepBased && activeSegment != null -> "${activeSegment.label} in progress"
        isRepBased && completedRepSegments >= repSegments.size && repSegments.isNotEmpty() -> "All detected reps completed"
        isRepBased -> "Rep count updates as playback crosses each completed cycle"
        isShuttleBased && activeSegment != null -> "${activeSegment.label} in progress"
        isShuttleBased && completedShuttleSegments >= shuttleTarget && shuttleSegments.isNotEmpty() -> "Full shuttle drill completed"
        isShuttleBased && shuttleSegments.isNotEmpty() -> "Shuttle count updates at each completed boundary crossing"
        isShuttleBased -> "No confirmed shuttle crossings yet"
        activeSegment != null -> activeSegment.label
        else -> "Pose skeleton video rendered by the backend"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Analysis Playback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Annotated video with pose skeleton overlay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = liveMetricLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = liveValueText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (resolvedUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                tag = resolvedUri.toString()
                                videoView = this
                                val controller = MediaController(context)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoURI(resolvedUri)
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = false
                                    durationMs = duration.toLong().coerceAtLeast(1L)
                                    playbackPositionMs = 0L
                                    hasPlaybackError = false
                                    isPrepared = true
                                    start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    playbackPositionMs = durationMs
                                    isPlaying = false
                                }
                                setOnErrorListener { _, what, extra ->
                                    Log.e("EvaluationResult", "AI analysis playback failed: what=$what extra=$extra")
                                    hasPlaybackError = true
                                    isPlaying = false
                                    true
                                }
                            }
                        },
                        update = { view ->
                            videoView = view
                            val source = resolvedUri.toString()
                            if (view.tag != source) {
                                view.tag = source
                                isPrepared = false
                                playbackPositionMs = 0L
                                durationMs = 1L
                                view.stopPlayback()
                                view.setVideoURI(resolvedUri)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isRepBased -> "Reps: $liveValueText"
                                        isShuttleBased -> "Shuttles: $liveValueText"
                                        else -> liveValueText
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = liveCaption,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { (playbackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatDuration(playbackPositionMs)} / ${formatDuration(durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            val currentView = videoView ?: return@TextButton
                            if (currentView.isPlaying) {
                                currentView.pause()
                                isPlaying = false
                            } else {
                                if (isPrepared && playbackPositionMs >= durationMs - 250L) {
                                    currentView.seekTo(0)
                                    playbackPositionMs = 0L
                                }
                                currentView.start()
                                isPlaying = true
                            }
                        },
                        enabled = !hasPlaybackError && isPrepared
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                }
            } else {
                Text(
                    text = "AI analysis video is not available for playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (hasPlaybackError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The annotated analysis video could not be played on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun resolveAnalysisVideoUri(videoUri: String?): Uri? {
    if (videoUri.isNullOrBlank()) return null
    return when {
        videoUri.startsWith("http://") || videoUri.startsWith("https://") -> Uri.parse(videoUri)
        videoUri.startsWith("/uploads/") -> Uri.parse("http://${ApiClient.currentIp}:8000$videoUri")
        videoUri.startsWith("/") -> Uri.fromFile(File(videoUri))
        else -> Uri.parse(videoUri)
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
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
