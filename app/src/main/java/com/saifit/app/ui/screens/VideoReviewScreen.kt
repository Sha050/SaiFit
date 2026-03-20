package com.saifit.app.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.saifit.app.data.model.FitnessTest
import com.saifit.app.data.model.VideoSegment
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoReviewScreen(
    test: FitnessTest?,
    videoUri: String?,
    recordingDurationMs: Long,
    segments: List<VideoSegment>,
    isEvaluating: Boolean,
    errorMessage: String?,
    onConfirmAndEvaluate: () -> Unit,
    onDismissError: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    val durationSeconds = (recordingDurationMs / 1000).toInt()
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)

    val resolvedUri: Uri? = remember(videoUri) {
        val result = resolveVideoUri(videoUri)
        Log.d("VideoReview", "Input URI string: $videoUri")
        Log.d("VideoReview", "Resolved URI: $result")
        result
    }

    val hasVideo = resolvedUri != null
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, hasVideo) {
        if (!hasVideo && isPlaying && recordingDurationMs > 0) {
            val stepMs = 50L
            while (isPlaying && playbackProgress < 1f) {
                kotlinx.coroutines.delay(stepMs)
                playbackProgress += (stepMs.toFloat() / recordingDurationMs.toFloat())
                if (playbackProgress >= 1f) {
                    playbackProgress = 1f
                    isPlaying = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Recording") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isEvaluating) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasVideo) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(resolvedUri)
                                    val mediaController = MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)
                                    setOnPreparedListener { mediaPlayer ->
                                        mediaPlayer.isLooping = false
                                        start()
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        Log.e("VideoReview", "Playback error: what=$what extra=$extra")
                                        false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${test?.name ?: "Test"} Recording",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Duration: $durationText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Video preview unavailable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (videoUri != null) {
                    val fileInfo = remember(videoUri) { getVideoFileInfo(videoUri) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VideoFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fileInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                if (!hasVideo) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { playbackProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val currentMs = (playbackProgress * recordingDurationMs).toLong()
                            val curMin = (currentMs / 60000).toInt()
                            val curSec = ((currentMs % 60000) / 1000).toInt()
                            Text(
                                text = String.format("%02d:%02d", curMin, curSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (segments.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Detected Segments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "${segments.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI has automatically identified key moments in your recording",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(segments) { segment ->
                            SegmentChip(
                                segment = segment,
                                isActive = false,
                                onClick = { }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        segments.forEach { segment ->
                            if (recordingDurationMs > 0) {
                                val startFraction = segment.startTimeMs.toFloat() / recordingDurationMs
                                val widthFraction = (segment.endTimeMs - segment.startTimeMs).toFloat() / recordingDurationMs
                                val color = when {
                                    segment.confidence >= 90 -> MaterialTheme.colorScheme.tertiary
                                    segment.confidence >= 80 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(widthFraction)
                                        .offset(x = (startFraction * 300).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(color.copy(alpha = 0.35f))
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = segment.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = color,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Recording Quality",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QualityIndicator(
                                icon = Icons.Default.Videocam,
                                label = "Resolution",
                                value = "1080p",
                                isGood = true
                            )
                            QualityIndicator(
                                icon = Icons.Default.WbSunny,
                                label = "Lighting",
                                value = "Good",
                                isGood = true
                            )
                            QualityIndicator(
                                icon = Icons.Default.Vibration,
                                label = "Stability",
                                value = if (recordingDurationMs > 3000) "Stable" else "Shaky",
                                isGood = recordingDurationMs > 3000
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = onConfirmAndEvaluate,
                        enabled = !isEvaluating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Confirm & Evaluate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onRetake,
                        enabled = !isEvaluating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retake Recording")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (isEvaluating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (test?.id == "situps") "Running Sit-up AI" else "Evaluating Video",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (test?.id == "situps") {
                                "Uploading the video and running YOLO pose detection. This can take a little time."
                            } else {
                                "Uploading the video and analyzing your performance."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun resolveVideoUri(uriString: String?): Uri? {
    if (uriString.isNullOrBlank()) return null
    return try {
        when {
            uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                Uri.parse(uriString)
            }

            uriString.startsWith("/") -> {
                val file = File(uriString)
                if (file.exists() && file.length() > 0) Uri.fromFile(file) else null
            }

            else -> Uri.parse(uriString)
        }
    } catch (e: Exception) {
        Log.e("VideoReview", "Failed to resolve URI: $uriString", e)
        null
    }
}

private fun getVideoFileInfo(uriString: String): String {
    return try {
        when {
            uriString.startsWith("/") -> {
                val file = File(uriString)
                if (file.exists()) {
                    val sizeKb = file.length() / 1024
                    val sizeMb = sizeKb / 1024.0
                    if (sizeMb >= 1.0) {
                        "Video ${String.format("%.1f", sizeMb)} MB - ${file.name}"
                    } else {
                        "Video ${sizeKb} KB - ${file.name}"
                    }
                } else {
                    "File not found: ${file.name}"
                }
            }

            uriString.startsWith("file://") -> {
                val path = Uri.parse(uriString).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        val sizeMb = file.length() / (1024.0 * 1024.0)
                        "Video ${String.format("%.1f", sizeMb)} MB - ${file.name}"
                    } else {
                        "File not found"
                    }
                } else {
                    "Invalid file path"
                }
            }

            uriString.startsWith("content://") -> "Video selected from device"
            else -> "Video: ${uriString.takeLast(40)}"
        }
    } catch (_: Exception) {
        "Unable to read video info"
    }
}

@Composable
private fun SegmentChip(
    segment: VideoSegment,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val startSec = (segment.startTimeMs / 1000).toInt()
    val endSec = (segment.endTimeMs / 1000).toInt()
    val confidenceColor = when {
        segment.confidence >= 90 -> MaterialTheme.colorScheme.tertiary
        segment.confidence >= 80 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) confidenceColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, confidenceColor) else null,
        modifier = Modifier
            .clickable(onClick = onClick)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = segment.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) confidenceColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format("%02d:%02d", startSec / 60, startSec % 60)} - ${String.format("%02d:%02d", endSec / 60, endSec % 60)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(confidenceColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${segment.confidence}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = confidenceColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QualityIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isGood: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGood) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isGood) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
