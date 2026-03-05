package com.saifit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearData: () -> Unit
) {

    var videoQuality by remember { mutableStateOf("High (1080p)") }
    var expandedQuality by remember { mutableStateOf(false) }
    var uploadOnWifiOnly by remember { mutableStateOf(true) }
    var autoEvaluate by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showBenchmarks by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val videoQualities = listOf("Low (480p)", "Medium (720p)", "High (1080p)")

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onClearData()
                    showClearDataDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Data?") },
            text = { Text("This will remove all local test results, submission history, and cached data. This action cannot be undone.") }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            SettingsSectionHeader("Video & Recording")

            ListItem(
                headlineContent = { Text("Video Quality") },
                supportingContent = { Text("Higher quality = larger file size") },
                leadingContent = {
                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    ExposedDropdownMenuBox(
                        expanded = expandedQuality,
                        onExpandedChange = { expandedQuality = !expandedQuality }
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.menuAnchor()
                        ) {
                            Text(
                                videoQuality,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        ExposedDropdownMenu(
                            expanded = expandedQuality,
                            onDismissRequest = { expandedQuality = false }
                        ) {
                            videoQualities.forEach { quality ->
                                DropdownMenuItem(
                                    text = { Text(quality) },
                                    onClick = {
                                        videoQuality = quality
                                        expandedQuality = false
                                    },
                                    leadingIcon = {
                                        if (quality == videoQuality) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text("Auto-Evaluate After Recording") },
                supportingContent = { Text("Automatically submit for AI analysis when recording stops") },
                leadingContent = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                },
                trailingContent = {
                    Switch(checked = autoEvaluate, onCheckedChange = { autoEvaluate = it })
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("Network & Uploads")

            ListItem(
                headlineContent = { Text("Upload on Wi-Fi Only") },
                supportingContent = { Text("Prevents large video uploads over mobile data") },
                leadingContent = {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                },
                trailingContent = {
                    Switch(checked = uploadOnWifiOnly, onCheckedChange = { uploadOnWifiOnly = it })
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("Display")

            ListItem(
                headlineContent = { Text("Show Performance Benchmarks") },
                supportingContent = { Text("Display age/gender comparisons on results") },
                leadingContent = {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(checked = showBenchmarks, onCheckedChange = { showBenchmarks = it })
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = { Text("Use dark theme (app restart required)") },
                leadingContent = {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                },
                trailingContent = {
                    Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("Notifications")

            ListItem(
                headlineContent = { Text("Push Notifications") },
                supportingContent = { Text("Receive alerts for test results and leaderboard updates") },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                },
                trailingContent = {
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("Data Management")

            ListItem(
                headlineContent = {
                    Text(
                        "Clear All Local Data",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = { Text("Remove all cached results and videos") },
                leadingContent = {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier
                    .padding(horizontal = 0.dp)
                    .then(
                        Modifier.let {
                            it 
                        }
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showClearDataDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear All Data")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SAI FIT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "v1.0.0-prototype",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Sports Authority of India",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
