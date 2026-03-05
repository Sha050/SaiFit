package com.saifit.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProfileAvatar(
    imageUri: String?,
    firstName: String,
    lastName: String,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}"
        .uppercase()
        .ifBlank { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = Uri.parse(imageUri),
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = when {
                    size >= 96.dp -> MaterialTheme.typography.headlineLarge
                    size >= 64.dp -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
