package com.saifit.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.saifit.app.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    currentUserId: String,
    entries: List<LeaderboardEntry>,
    availableTests: List<String>,
    availableRegions: List<String>,
    selectedTest: String,
    selectedRegion: String,
    onTestFilter: (String) -> Unit,
    onRegionFilter: (String) -> Unit
) {
    var expandedTest by remember { mutableStateOf(false) }
    var expandedRegion by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Leaderboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "See how you rank against athletes across India",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = { Text("Test", style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTest) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(expanded = expandedTest, onDismissRequest = { expandedTest = false }) {
                    DropdownMenuItem(
                        text = { Text("All Tests") },
                        onClick = { onTestFilter("All Tests"); expandedTest = false }
                    )
                    availableTests.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = { onTestFilter(t); expandedTest = false }
                        )
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
                    label = { Text("Region", style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRegion) },
                    modifier = Modifier.menuAnchor(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(expanded = expandedRegion, onDismissRequest = { expandedRegion = false }) {
                    DropdownMenuItem(
                        text = { Text("All Regions") },
                        onClick = { onRegionFilter("All Regions"); expandedRegion = false }
                    )
                    availableRegions.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = { onRegionFilter(r); expandedRegion = false }
                        )
                    }
                }
            }
        }

        if (entries.size >= 3) {
            PodiumSection(entries.take(3), currentUserId)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text("#", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Athlete", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Score", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tier", modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No rankings yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Complete tests to appear on the leaderboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                itemsIndexed(entries) { index, entry ->

                    val enterAnim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 50L) 
                        enterAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(0.9f + (0.1f * enterAnim.value))
                            .graphicsLayer { alpha = enterAnim.value }
                    ) {
                        LeaderboardRow(entry, currentUserId)
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(top3: List<LeaderboardEntry>, currentUserId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        if (top3.size >= 2) PodiumItem(top3[1], 2, currentUserId, height = 80)
        if (top3.isNotEmpty()) PodiumItem(top3[0], 1, currentUserId, height = 100)
        if (top3.size >= 3) PodiumItem(top3[2], 3, currentUserId, height = 64)
    }
}

@Composable
private fun PodiumItem(entry: LeaderboardEntry, position: Int, currentUserId: String, height: Int) {
    val isMe = entry.athleteId == currentUserId
    val medalColor = when (position) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.secondary
        3 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val medalEmoji = when (position) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {

        Text(medalEmoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary
                    else medalColor.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.athleteName.take(1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary else medalColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            entry.athleteName.split(" ").first(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1
        )
        Text(
            "${entry.value} ${entry.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(medalColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$position",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = medalColor
            )
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, currentUserId: String) {
    val isMe = entry.athleteId == currentUserId 
    val bgColor by animateColorAsState(
        targetValue = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                      else MaterialTheme.colorScheme.surface,
        label = "row_bg"
    )
    val tierColor = when (entry.tier) {
        PerformanceTier.ELITE -> MaterialTheme.colorScheme.primary
        PerformanceTier.EXCELLENT -> MaterialTheme.colorScheme.secondary
        PerformanceTier.GOOD -> MaterialTheme.colorScheme.tertiary
        PerformanceTier.AVERAGE -> MaterialTheme.colorScheme.onSurfaceVariant
        PerformanceTier.BELOW_AVERAGE -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            "${entry.rank}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (entry.rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.athleteName + if (isMe) " (You)" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.Medium
            )
            Text(
                "${entry.region} • Age ${entry.age}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "${entry.value} ${entry.unit}",
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Surface(
            shape = MaterialTheme.shapes.small,
            color = tierColor.copy(alpha = 0.15f),
            modifier = Modifier.width(52.dp)
        ) {
            Text(
                text = entry.tier.emoji,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
