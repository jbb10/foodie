package com.foodie.app.ui.screens.capture

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodie.app.R
import com.foodie.app.notifications.NotificationPermissionManager
import timber.log.Timber

/**
 * Camera capture screen coordinating photo capture flow.
 *
 * This screen handles the complete camera capture workflow:
 * 1. Request camera permission if not granted
 * 2. Launch system camera intent for photo capture
 * 3. Process captured photo (resize + compress)
 * 4. Show preview screen with Retake/Use Photo options
 *
 * Architecture:
 * - Uses system camera intent (MediaStore.ACTION_IMAGE_CAPTURE)
 * - Photo processing via PhotoManager (2MP resize, 80% JPEG)
 * - State managed by CapturePhotoViewModel
 * - Unidirectional data flow via callbacks
 *
 * Navigation:
 * - Entry: Deep link from widget (foodie://capture)
 * - Exit: onPhotoConfirmed callback (proceeds to processing)
 * - Back: onNavigateBack callback (cancels capture)
 *
 * @param onPhotoConfirmed Callback when user confirms photo (passes processed photo URI)
 * @param onNavigateBack Callback when user cancels capture flow
 * @param viewModel ViewModel managing capture state (Hilt-injected)
 */
@Composable
fun CapturePhotoScreen(
    onPhotoConfirmed: (Uri) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CapturePhotoViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // Health Connect permission launcher (must come before camera)
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.createHealthConnectPermissionContract()
    ) { granted ->
        Timber.i("Health Connect permission result: granted=${granted.size}, required=${com.foodie.app.data.local.healthconnect.HealthConnectManager.REQUIRED_PERMISSIONS.size}")
        if (granted.containsAll(com.foodie.app.data.local.healthconnect.HealthConnectManager.REQUIRED_PERMISSIONS)) {
            Timber.i("✅ Health Connect permissions granted from capture screen")
            viewModel.onHealthConnectPermissionGranted(context)
        } else {
            Timber.w("❌ Health Connect permissions denied (granted: ${granted.size})")
            // User denied permissions - cannot proceed, navigate back
            onNavigateBack()
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // Camera intent launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Haptic feedback on successful photo capture (AC#8)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            Timber.d("Photo captured with haptic feedback")
            viewModel.onPhotoCaptured()
        } else {
            Timber.d("Camera cancelled or failed")
            viewModel.onCaptureCancelled()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted, context)
    }

    // Handle state changes
    LaunchedEffect(state) {
        when (state) {
            is CaptureState.RequestingHealthConnectPermission -> {
                healthConnectPermissionLauncher.launch(
                    com.foodie.app.data.local.healthconnect.HealthConnectManager.REQUIRED_PERMISSIONS
                )
            }
            is CaptureState.RequestingPermission -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            is CaptureState.ReadyToCapture -> {
                val photoUri = (state as CaptureState.ReadyToCapture).photoUri
                cameraLauncher.launch(photoUri)
            }
            is CaptureState.ProcessingComplete -> {
                val processedUri = (state as CaptureState.ProcessingComplete).processedPhotoUri
                // Don't auto-proceed - wait for preview screen interaction
            }
            is CaptureState.NotificationPermissionRequired -> {
                if (viewModel.isNotificationPermissionRequired()) {
                    notificationPermissionLauncher.launch(NotificationPermissionManager.PERMISSION)
                } else {
                    viewModel.onNotificationPermissionResult(granted = true, context = context)
                }
            }
            else -> {}
        }
    }

    // Check permission on initial load
    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndPrepare(context)
    }

    // Render UI based on state
    when (state) {
        is CaptureState.Idle,
        is CaptureState.RequestingHealthConnectPermission,
        is CaptureState.RequestingPermission,
        is CaptureState.ReadyToCapture,
        is CaptureState.Processing -> {
            // Loading state while preparing camera or processing photo
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = when (state) {
                            is CaptureState.RequestingPermission -> stringResource(R.string.capture_requesting_permission)
                            is CaptureState.ReadyToCapture -> stringResource(R.string.capture_launching_camera)
                            is CaptureState.Processing -> stringResource(R.string.capture_processing_photo)
                            else -> stringResource(R.string.capture_preparing_camera)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is CaptureState.BackgroundProcessingStarted -> {
            // Background processing started, navigate back
            LaunchedEffect(Unit) {
                onPhotoConfirmed(android.net.Uri.EMPTY) // Signal completion, URI not needed
            }
        }

        is CaptureState.NotificationPermissionRequired -> {
            // Keep showing preview while permission dialog is displayed
            // The LaunchedEffect above will trigger the permission request
            viewModel.getProcessedPhotoUri()?.let { processedUri ->
                PreviewScreen(
                    photoUri = processedUri,
                    onRetake = {
                        viewModel.onRetake()
                    },
                    onUsePhoto = { ctx ->
                        // Do nothing - already waiting for permission result
                    },
                    confirmationEnabled = false
                )
            }
        }

        is CaptureState.NotificationPermissionDenied -> {
            NotificationPermissionDeniedScreen(
                onRetry = { viewModel.retryNotificationPermission() },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                onCancel = onNavigateBack
            )
        }

        is CaptureState.ProcessingComplete -> {
            val processedUri = (state as CaptureState.ProcessingComplete).processedPhotoUri
            PreviewScreen(
                photoUri = processedUri,
                onRetake = {
                    viewModel.onRetake()
                    // Retake will reset state to ReadyToCapture, relaunching camera
                },
                onUsePhoto = { ctx ->
                    viewModel.onUsePhoto(ctx)
                }
            )
        }

        is CaptureState.Error -> {
            val errorMessage = (state as CaptureState.Error).message
            CaptureErrorScreen(
                message = errorMessage,
                onRetry = {
                    viewModel.checkPermissionAndPrepare(context)
                },
                onCancel = onNavigateBack
            )
        }

        is CaptureState.PermissionDenied -> {
            PermissionDeniedScreen(
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onCancel = onNavigateBack
            )
        }

        is CaptureState.HealthConnectPermissionDenied -> {
            // User denied Health Connect permissions - navigate back to previous screen
            LaunchedEffect(Unit) {
                Timber.w("Health Connect permissions denied - closing capture screen")
                onNavigateBack()
            }
        }

        is CaptureState.StorageFull -> {
            StorageFullScreen(
                onCancel = onNavigateBack
            )
        }
    }
}

/**
 * Error screen shown when capture or processing fails.
 *
 * @param message Error message to display
 * @param onRetry Callback to retry capture
 * @param onCancel Callback to cancel and navigate back
 */
@Composable
private fun CaptureErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_error_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.capture_cancel))
                }
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.capture_retry))
                }
            }
        }
    }
}

/**
 * Screen shown when camera permission is permanently denied.
 *
 * Provides option to open app settings or cancel.
 *
 * @param onOpenSettings Callback to open app settings
 * @param onCancel Callback to cancel and navigate back
 */
@Composable
internal fun PermissionDeniedScreen(
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_permission_denied_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = stringResource(R.string.capture_permission_denied_message),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.capture_cancel))
                }
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.capture_open_settings))
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionDeniedScreen(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_notification_permission_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = stringResource(R.string.capture_notification_permission_denied),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.capture_cancel))
                }
                OutlinedButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.capture_notification_permission_open_settings))
                }
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.capture_notification_permission_retry))
                }
            }
        }
    }
}

/**
 * Screen shown when Health Connect permissions are denied.
 * 
 * Explains why Health Connect is required and provides options to grant access or cancel.
 *
 * @param onOpenHealthConnect Callback to re-request Health Connect permissions
 * @param onCancel Callback to cancel and navigate back
 */
@Composable
private fun HealthConnectPermissionDeniedScreen(
    onOpenHealthConnect: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Health Connect Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Foodie saves your meal nutrition to Health Connect. " +
                      "Tap 'Grant Access' to open Health Connect settings.",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(onClick = onOpenHealthConnect) {
                    Text("Grant Access")
                }
            }
        }
    }
}

/**
 * Storage full error screen shown when device has insufficient storage.
 *
 * Explains storage issue and provides option to cancel. User must free up
 * storage space before retrying.
 *
 * Story: 4.6 - Graceful Degradation (AC#4)
 *
 * @param onCancel Callback to cancel and navigate back
 */
@Composable
internal fun StorageFullScreen(
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_storage_full_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = stringResource(R.string.capture_storage_full_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(onClick = onCancel) {
                Text(stringResource(R.string.capture_ok))
            }
        }
    }
}
