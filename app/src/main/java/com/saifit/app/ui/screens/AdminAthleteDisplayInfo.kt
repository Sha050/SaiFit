package com.saifit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saifit.app.data.model.TestResult
import com.saifit.app.data.model.User

internal data class AdminAthleteDisplayInfo(
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val ageText: String,
    val genderText: String,
    val emailText: String,
    val phoneText: String,
    val aadhaarText: String,
    val regionText: String,
    val sportText: String,
    val defaultSuggestedSport: String,
    val profileImageUri: String?,
    val hasProfilePhoto: Boolean,
    val isProfileSynced: Boolean,
    val syncNotice: String
)

internal fun buildAdminAthleteDisplayInfo(
    athlete: User?,
    results: List<TestResult>
): AdminAthleteDisplayInfo {
    val fullName = athlete?.name.ifMeaningful()
        ?: results.firstNotNullOfOrNull { it.athleteName.ifMeaningful() }
        ?: "Unknown Athlete"
    val (fallbackFirstName, fallbackLastName) = splitName(fullName)
    val recoveredSport = mostFrequentSuggestedSport(results)

    return AdminAthleteDisplayInfo(
        fullName = fullName,
        firstName = athlete?.firstName.ifMeaningful() ?: fallbackFirstName,
        lastName = athlete?.lastName.ifMeaningful() ?: fallbackLastName,
        ageText = athlete?.age?.takeIf { it > 0 }?.let { "$it years" } ?: "Not synced yet",
        genderText = athlete?.gender?.name
            ?.lowercase()
            ?.replaceFirstChar { it.titlecase() }
            ?: "Not synced yet",
        emailText = athlete?.email.ifMeaningful() ?: "Not synced yet",
        phoneText = athlete?.phoneNumber.ifMeaningful() ?: "Not synced yet",
        aadhaarText = athlete?.aadhaarNumber.ifMeaningful() ?: "Not synced yet",
        regionText = athlete?.region.ifMeaningful() ?: "Not synced yet",
        sportText = athlete?.sport.ifMeaningful() ?: recoveredSport.ifBlank { "Not assigned yet" },
        defaultSuggestedSport = athlete?.sport.ifMeaningful() ?: recoveredSport,
        profileImageUri = athlete?.profileImageUri.ifMeaningful(),
        hasProfilePhoto = athlete?.profileImageUri.ifMeaningful() != null,
        isProfileSynced = athlete != null,
        syncNotice = if (athlete != null) {
            ""
        } else {
            "This athlete's assessment history is available, but the full profile has not been synced to the user database yet."
        }
    )
}

@Composable
internal fun AdminProfileDataNotice(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun splitName(fullName: String): Pair<String, String> {
    val nameParts = fullName.trim().split(Regex("\\s+"), limit = 2)
    val firstName = nameParts.firstOrNull().orEmpty().ifBlank { "Unknown" }
    val lastName = nameParts.getOrElse(1) { "" }
    return firstName to lastName
}

private fun mostFrequentSuggestedSport(results: List<TestResult>): String =
    results
        .mapNotNull { it.suggestedSport.ifMeaningful() }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        .orEmpty()

private fun String?.ifMeaningful(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
