package com.saifit.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saifit.app.data.model.AthleteProgress
import com.saifit.app.data.model.Badge
import com.saifit.app.data.model.BadgeCategory

@Composable
fun BadgesScreen(
    progress: AthleteProgress,
    athleteName: String
) {
    val earnedCount = progress.badges.count { it.earned }
    val totalBadges = progress.badges.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item(span = { GridItemSpan(2) }) {
            ProgressHeader(progress, athleteName)
        }

        item(span = { GridItemSpan(2) }) {
            CompletionStrip(progress)
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "🎖️ Earned ($earnedCount / $totalBadges)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val earned = progress.badges.filter { it.earned }
        if (earned.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "Complete tests to earn your first badge!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(earned) { badge -> BadgeCard(badge, earned = true) }
        }

        val locked = progress.badges.filter { !it.earned }
        if (locked.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🔒 Locked (${locked.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(locked) { badge -> BadgeCard(badge, earned = false) }
        }

        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProgressHeader(progress: AthleteProgress, athleteName: String) {

    val animatedProgress by animateFloatAsState(
        targetValue = progress.completionPercent,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                athleteName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Tests Done", "${progress.completedTests}/${progress.totalTests}")
                StatItem("Valid", "${progress.validResults}")
                StatItem("Badges", "${progress.badges.count { it.earned }}")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompletionStrip(progress: AthleteProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Test Progress",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.completionPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${progress.completedTests} of ${progress.totalTests} tests completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge, earned: Boolean) {
    val scaleAnim = animateFloatAsState(
        targetValue = if (earned) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "badge_scale"
    )

    val icon = when (badge.iconName) {
        "star"               -> Icons.Default.Star
        "trending_up"        -> Icons.Default.TrendingUp
        "emoji_events"       -> Icons.Default.EmojiEvents
        "repeat"             -> Icons.Default.Repeat
        "military_tech"      -> Icons.Default.MilitaryTech
        "workspace_premium"  -> Icons.Default.WorkspacePremium
        "verified"           -> Icons.Default.Verified
        "speed"              -> Icons.Default.Speed
        "fitness_center"     -> Icons.Default.FitnessCenter
        "rocket_launch"      -> Icons.Default.RocketLaunch
        "timer"              -> Icons.Default.Timer
        else                 -> Icons.Default.Stars
    }

    val categoryColor = when (badge.category) {
        BadgeCategory.MILESTONE   -> MaterialTheme.colorScheme.primary
        BadgeCategory.PERFORMANCE -> MaterialTheme.colorScheme.secondary
        BadgeCategory.STREAK      -> MaterialTheme.colorScheme.tertiary
        BadgeCategory.SPECIAL     -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim.value)
            .alpha(if (earned) 1f else 0.5f),
        colors = CardDefaults.cardColors(
            containerColor = if (earned)
                categoryColor.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (earned) categoryColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = badge.name,
                    modifier = Modifier.size(28.dp),
                    tint = if (earned) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                badge.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (badge.progress != null && !earned) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { badge.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = categoryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "${(badge.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (earned) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "✅ Earned",
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
