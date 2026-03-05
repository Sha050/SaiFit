package com.saifit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saifit.app.data.model.Gender
import com.saifit.app.data.model.User
import com.saifit.app.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No user data")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ProfileContent(
                user = user,
                onLogout = onLogout,
                onSettingsClick = null,
                onSubmissionQueueClick = null,
                isEditable = false
            )
        }
    }
}

@Composable
fun ProfileScreenContent(
    user: User,
    onLogout: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onSubmissionQueueClick: (() -> Unit)? = null
) {
    ProfileContent(
        user = user,
        onLogout = onLogout,
        onSettingsClick = onSettingsClick,
        onSubmissionQueueClick = onSubmissionQueueClick,
        isEditable = true
    )
}

@Composable
private fun ProfileContent(
    user: User,
    onLogout: () -> Unit,
    onSettingsClick: (() -> Unit)?,
    onSubmissionQueueClick: (() -> Unit)?,
    isEditable: Boolean
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedSport by remember(user.sport) { mutableStateOf(user.sport ?: "") }
    var editedPhone by remember(user.phoneNumber) { mutableStateOf(user.phoneNumber ?: "") }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        ProfileAvatar(
            imageUri = user.profileImageUri,
            firstName = user.firstName,
            lastName = user.lastName,
            size = 100.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = user.role.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (isEditable) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                if (isEditing) {

                    showSavedSnackbar = true
                    isEditing = false

                } else {
                    isEditing = true
                }
            }) {
                Icon(
                    if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isEditing) "Save Changes" else "Edit Profile")
            }
            if (showSavedSnackbar) {
                LaunchedEffect(showSavedSnackbar) {
                    kotlinx.coroutines.delay(2000)
                    showSavedSnackbar = false
                }
                Text(
                    "✓ Changes saved (mock — will sync with backend)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (onSettingsClick != null || onSubmissionQueueClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onSettingsClick != null) {
                    OutlinedButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Settings", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (onSubmissionQueueClick != null) {
                    OutlinedButton(
                        onClick = onSubmissionQueueClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Uploads", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ProfileInfoCard(
                icon = Icons.Default.Person,
                label = "First Name",
                value = user.firstName
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProfileInfoCard(
                icon = Icons.Default.Person,
                label = "Last Name",
                value = user.lastName
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProfileInfoCard(
                icon = Icons.Default.Cake,
                label = "Age",
                value = "${user.age} years"
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProfileInfoCard(
                icon = if (user.gender == Gender.MALE) Icons.Default.Male
                       else if (user.gender == Gender.FEMALE) Icons.Default.Female
                       else Icons.Default.Person,
                label = "Gender",
                value = user.gender.name.lowercase().replaceFirstChar { it.uppercase() }
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProfileInfoCard(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                EditableProfileInfoCard(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = editedPhone,
                    onValueChange = { editedPhone = it }
                )
            } else {
                ProfileInfoCard(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = user.phoneNumber?.ifBlank { "Not specified" } ?: "Not specified"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            ProfileInfoCard(
                icon = Icons.Default.Badge,
                label = "Aadhaar / Govt ID",
                value = user.aadhaarNumber?.ifBlank { "Not specified" } ?: "Not specified"
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProfileInfoCard(
                icon = Icons.Default.LocationOn,
                label = "Region",
                value = user.region?.ifBlank { "Not specified" } ?: "Not specified"
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                EditableProfileInfoCard(
                    icon = Icons.Default.SportsSoccer,
                    label = "Sport",
                    value = editedSport,
                    onValueChange = { editedSport = it }
                )
            } else {
                ProfileInfoCard(
                    icon = Icons.Default.SportsSoccer,
                    label = "Sport",
                    value = user.sport?.ifBlank { "Not specified" } ?: "Not specified"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SAI FIT v1.0.0-prototype",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
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
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EditableProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
