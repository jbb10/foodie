package com.foodie.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import timber.log.Timber

/**
 * Reusable Health Connect permission gate component.
 *
 * Handles the complete Health Connect flow before showing content:
 * 1. Checks if HC is available (installed)
 * 2. If not available: shows unavailable dialog with Play Store link
 * 3. If available but permissions not granted: shows OS permission dialog
 * 4. If permissions denied: shows education screen with retry/cancel options
 * 5. If all checks pass: shows content
 *
 * @param healthConnectManager HealthConnectManager instance for availability/permission checking
 * @param onPermissionsDenied Callback invoked when user cancels/exits after denial
 * @param content Content to show once HC is available and permissions are granted
 */
@Composable
fun HealthConnectPermissionGate(
    healthConnectManager: HealthConnectManager,
    onPermissionsDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    var healthConnectAvailable by remember { mutableStateOf<Boolean?>(null) }
    var permissionsGranted by remember { mutableStateOf<Boolean?>(null) }
    var showDenialScreen by remember { mutableStateOf(false) }
    var showUnavailableDialog by remember { mutableStateOf(false) }
    var permissionRequestTrigger by remember { mutableIntStateOf(0) }
    var availabilityCheckTrigger by remember { mutableIntStateOf(0) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.createPermissionRequestContract(),
    ) { granted ->
        Timber.i("Health Connect permission result: granted=${granted.size}/${HealthConnectManager.REQUIRED_PERMISSIONS.size}")

        if (granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)) {
            Timber.i("✅ Health Connect permissions granted")
            showDenialScreen = false
            permissionsGranted = true
        } else {
            Timber.w("❌ Health Connect permissions denied (granted: ${granted.size})")
            showDenialScreen = true
            permissionsGranted = false
        }
    }

    // Check availability on initial load and when triggered
    LaunchedEffect(availabilityCheckTrigger) {
        Timber.i("Checking Health Connect availability (trigger=$availabilityCheckTrigger)")
        val available = healthConnectManager.isAvailable()
        healthConnectAvailable = available

        if (!available) {
            Timber.w("Health Connect not available - showing install dialog")
            showUnavailableDialog = true
            permissionsGranted = false
        } else {
            Timber.i("Health Connect available - proceeding to permission check")
            showUnavailableDialog = false

            // Check permissions if HC is available
            val hasPermissions = healthConnectManager.checkPermissions()
            if (hasPermissions) {
                Timber.i("Health Connect permissions already granted")
                permissionsGranted = true
            } else {
                Timber.i("Health Connect permissions missing - requesting")
                permissionRequestTrigger++
            }
        }
    }

    // Launch permission request when triggered (only if HC available)
    LaunchedEffect(permissionRequestTrigger) {
        if (permissionRequestTrigger > 0 && healthConnectAvailable == true) {
            Timber.i("Launching Health Connect permission request (trigger=$permissionRequestTrigger)")
            permissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
        }
    }

    // Render based on state
    when {
        permissionsGranted == true && healthConnectAvailable == true -> {
            // All checks passed - show content
            content()
        }
        showDenialScreen -> {
            // Permissions denied - show education screen
            HealthConnectDenialScreen(
                onRetry = {
                    Timber.i("User tapped retry - re-requesting Health Connect permissions")
                    permissionRequestTrigger++
                },
                onCancel = {
                    Timber.i("User cancelled Health Connect permission flow")
                    onPermissionsDenied()
                },
            )
        }
        showUnavailableDialog -> {
            // HC not installed - show unavailable dialog
            HealthConnectUnavailableDialog(
                onDismiss = {
                    Timber.i("User dismissed HC unavailable dialog - re-checking availability")
                    showUnavailableDialog = false
                    // Re-check availability in case user installed HC
                    availabilityCheckTrigger++
                },
            )
        }
        else -> {
            // Loading / checking state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Screen shown when Health Connect permissions are denied.
 *
 * Provides education about why HC is needed and options to retry or exit.
 */
@Composable
private fun HealthConnectDenialScreen(
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Health Connect Required",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Foodie saves your meal nutrition data to Health Connect so you can:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                BulletPoint("Track nutrition across all your health apps")
                BulletPoint("Keep your data private and on-device")
                BulletPoint("View your meal history anytime")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Grant Access")
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
