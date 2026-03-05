package com.saifit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import com.saifit.app.data.model.ResultStatus
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: User,
    allResults: List<TestResult>,
    availableTests: List<String>,
    availableRegions: List<String>,
    allAthletes: List<User>,
    onLogout: () -> Unit,
    onCandidateClick: (athleteId: String) -> Unit
) {
    var selectedTest by remember { mutableStateOf("All Tests") }
    var expandedTest by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf("All Regions") }
    var expandedRegion by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showExportSnackbar by remember { mutableStateOf(false) }

    val filtered = allResults
        .filter { if (selectedTest != "All Tests") it.testName == selectedTest else true }
        .filter { r ->
            if (selectedRegion != "All Regions") {
                val athlete = allAthletes.find { it.id == r.athleteId }
                athlete?.region?.equals(selectedRegion, ignoreCase = true) == true
            } else true
        }

    data class AthleteScore(
        val athleteId: String,
        val athleteName: String,
        val results: List<TestResult>,
        val avgConfidence: Int,
        val totalTests: Int
    )

    val athleteScores = filtered
        .groupBy { it.athleteId }
        .map { (id, results) ->
            AthleteScore(
                athleteId = id,
                athleteName = results.first().athleteName,
                results = results,
                avgConfidence = results.map { it.confidencePercent }.average().toInt(),
                totalTests = results.size
            )
        }

        .filter { athlete ->
            if (searchQuery.isBlank()) true
            else athlete.athleteName.contains(searchQuery, ignoreCase = true)
        }
        .let { list ->
            if (sortAscending) list.sortedBy { it.avgConfidence }
            else list.sortedByDescending { it.avgConfidence }
        }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(showExportSnackbar) {
        if (showExportSnackbar) {
            snackbarHostState.showSnackbar("Export feature coming soon — data will be saved as CSV")
            showExportSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showExportSnackbar = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            val scaleAnim = remember { Animatable(0.95f) }
            LaunchedEffect(Unit) {
                scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .scale(scaleAnim.value),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SupervisorAccount, null, Modifier.size(32.dp), tint = Color.White)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Admin ${user.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("${athleteScores.size} candidates  •  ${filtered.size} test records",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search athletes by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                ExposedDropdownMenuBox(
                    expanded = expandedTest,
                    onExpandedChange = { expandedTest = !expandedTest },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedTest,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Test") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTest) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = expandedTest, onDismissRequest = { expandedTest = false }) {
                        DropdownMenuItem(text = { Text("All Tests") }, onClick = { selectedTest = "All Tests"; expandedTest = false })
                        availableTests.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { selectedTest = t; expandedTest = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedRegion,
                    onExpandedChange = { expandedRegion = !expandedRegion },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedRegion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRegion) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = expandedRegion, onDismissRequest = { expandedRegion = false }) {
                        DropdownMenuItem(text = { Text("All Regions") }, onClick = { selectedRegion = "All Regions"; expandedRegion = false })
                        availableRegions.forEach { r ->
                            DropdownMenuItem(text = { Text(r) }, onClick = { selectedRegion = r; expandedRegion = false })
                        }
                    }
                }

                IconButton(onClick = { sortAscending = !sortAscending }) {
                    Icon(
                        if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Sort"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text("#", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Athlete", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Tests", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Conf%", modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Status", modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (athleteScores.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No athletes found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                "Try a different search term",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    itemsIndexed(athleteScores) { index, athlete ->
                        val allValid = athlete.results.all { it.status == ResultStatus.VALID }
                        val statusColor = if (allValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                        val enterAnim = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 60L)
                            enterAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(0.9f + (0.1f * enterAnim.value))
                                .graphicsLayer { alpha = enterAnim.value }
                                .clickable { onCandidateClick(athlete.athleteId) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                "${index + 1}",
                                modifier = Modifier.width(32.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (index < 3) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(athlete.athleteName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                if (selectedTest != "All Tests") {
                                    val result = athlete.results.firstOrNull()
                                    if (result != null) {
                                        Text(
                                            "${result.value} ${result.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text("${athlete.totalTests}", modifier = Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium)

                            Text("${athlete.avgConfidence}%", modifier = Modifier.width(52.dp), style = MaterialTheme.typography.bodyMedium)

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = statusColor.copy(alpha = 0.15f),
                                modifier = Modifier.width(56.dp)
                            ) {
                                Text(
                                    text = if (allValid) "Valid" else "Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}
