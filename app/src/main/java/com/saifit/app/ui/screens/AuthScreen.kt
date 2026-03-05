package com.saifit.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.saifit.app.data.model.Gender
import com.saifit.app.data.model.UserRole
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AuthScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (firstName: String, lastName: String, age: Int, gender: Gender, role: UserRole,
                 email: String, phone: String, aadhaar: String, region: String,
                 profileImageUri: String?) -> Unit,
    onClearError: () -> Unit = {}
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.ATHLETE) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileImageUri = it }
    }

    LaunchedEffect(isSignUp) { onClearError() }

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted && region.isBlank()) {
            isDetectingLocation = true
            detectLocation(context) { detectedRegion ->
                region = detectedRegion
                isDetectingLocation = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SAI FIT",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isSignUp) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isSignUp) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Demo Accounts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Athlete: athlete@saifit.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Admin: admin@saifit.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Password: any text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )

            if (isSignUp) {
                Spacer(modifier = Modifier.height(20.dp))

                Text("Profile Photo", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        coil.compose.AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Add photo",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Add Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (profileImageUri != null) {
                    TextButton(onClick = { profileImageUri = null }) {
                        Text("Remove Photo")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("I am a:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = selectedRole == UserRole.ATHLETE,
                        onClick = { selectedRole = UserRole.ATHLETE },
                        label = { Text("Athlete") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedRole == UserRole.ADMIN,
                        onClick = { selectedRole = UserRole.ADMIN },
                        label = { Text("Admin") },
                        leadingIcon = {
                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = aadhaar,
                    onValueChange = { aadhaar = it },
                    label = { Text("Aadhaar / Govt ID No.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedRole == UserRole.ATHLETE) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = region,
                        onValueChange = { region = it },
                        label = { Text("Region/State") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = {
                                    if (locationPermission.status.isGranted) {
                                        isDetectingLocation = true
                                        detectLocation(context) { detectedRegion ->
                                            region = detectedRegion
                                            isDetectingLocation = false
                                        }
                                    } else {
                                        locationPermission.launchPermissionRequest()
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "Auto-detect location",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        supportingText = {
                            Text("Tap 📍 to auto-detect from GPS")
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it.filter { c -> c.isDigit() } },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Gender", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Gender.values().forEach { g ->
                            FilterChip(
                                selected = selectedGender == g,
                                onClick = { selectedGender = g },
                                label = { Text(g.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!isSignUp) {
                        onLogin(email, password)
                    } else {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) return@Button

                        val dummyRegion = if (region.isBlank()) "Unknown" else region
                        val ageInt = age.toIntOrNull() ?: 18

                        onRegister(
                            firstName, lastName, ageInt, selectedGender, selectedRole,
                            email, phone, aadhaar, dummyRegion,
                            profileImageUri?.toString()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isSignUp) "Sign Up" else "Sign In",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    text = if (isSignUp) "Already have an account? Sign In"
                           else "Don't have an account? Sign Up"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            var serverIp by remember { mutableStateOf(com.saifit.app.data.api.ApiClient.currentIp) }
            OutlinedTextField(
                value = serverIp,
                onValueChange = { 
                    serverIp = it
                    com.saifit.app.data.api.ApiClient.currentIp = it 
                },
                label = { Text("Server IP Address (Local Backend)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                textStyle = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Developer Menu: Point this to your computer's local Wi-Fi IP address so the app can reach the Python Backend.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.8f).padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Please wait...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun detectLocation(context: Context, onResult: (String) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                as android.location.LocationManager

        val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            try {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val state = addresses[0].adminArea ?: ""
                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    val result = if (city.isNotBlank() && state.isNotBlank()) "$city, $state"
                                 else state.ifBlank { city }
                    onResult(result.ifBlank { "Location detected" })
                } else {
                    onResult("Could not determine region")
                }
            } catch (e: Exception) {
                onResult("Geocoder error")
            }
        } else {
            onResult("GPS unavailable - enter manually")
        }
    } catch (e: Exception) {
        onResult("Location error")
    }
}
