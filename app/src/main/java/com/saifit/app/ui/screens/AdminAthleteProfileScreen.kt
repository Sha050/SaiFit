package com.saifit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saifit.app.data.model.ResultStatus
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.model.User
import com.saifit.app.ui.components.ProfileAvatar

import androidx.compose.runtime.*
import com.saifit.app.ui.components.VideoPlayerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAthleteProfileScreen(
    athlete: User?,
    results: List<TestResult>,
    onBack: () -> Unit,
    onSendToSaiClick: () -> Unit
) {
    val athleteName = athlete?.name ?: results.firstOrNull()?.athleteName ?: "Unknown"
    var videoUrlToPlay by remember { mutableStateOf<String?>(null) }

    if (videoUrlToPlay != null) {
        VideoPlayerDialog(
            videoUrl = videoUrlToPlay!!,
            onDismiss = { videoUrlToPlay = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Athlete Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    ProfileAvatar(
                        imageUri = athlete?.profileImageUri,
                        firstName = athlete?.firstName ?: athleteName.take(1),
                        lastName = athlete?.lastName ?: "",
                        size = 64.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = athleteName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (athlete != null) {
                            Text(
                                text = "Age ${athlete.age} • ${athlete.region}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = athlete.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${results.size} Test Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(results) { result ->
                    val statusColor = when (result.status) {
                        ResultStatus.VALID -> MaterialTheme.colorScheme.primary
                        ResultStatus.RETRY -> MaterialTheme.colorScheme.error
                        ResultStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                    }
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.testName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Conf: ${result.confidencePercent}%",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = statusColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = result.status.name,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${result.value} ${result.unit}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (result.videoUri != null) {
                            val context = LocalContext.current
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            OutlinedButton(
                                onClick = {
                                    val baseUrl = "http://${com.saifit.app.data.api.ApiClient.currentIp}:8000"
                                    val fullUrl = if (result.videoUri.startsWith("http")) result.videoUri else baseUrl + result.videoUri
                                    videoUrlToPlay = fullUrl
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Play Video")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch Assessment Video")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onSendToSaiClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Suggest Sport & Send to SAI")
            }
        }
    }
}
