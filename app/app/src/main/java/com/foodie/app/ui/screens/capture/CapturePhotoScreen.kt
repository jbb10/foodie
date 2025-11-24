package com.foodie.app.ui.screens.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
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

    // Set up permission and camera launchers
    val launchers = setupLaunchers(viewModel, onNavigateBack, hapticFeedback, context)

    // Handle state-driven side effects
    HandleStateEffects(state, launchers, viewModel, context)

    // Check permission on initial load
    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndPrepare(context)
    }

    // Render UI based on state
    CaptureScreenContent(state, viewModel, onPhotoConfirmed, onNavigateBack, context)
}

@Composable
private fun setupLaunchers(
    viewModel: CapturePhotoViewModel,
    onNavigateBack: () -> Unit,
    hapticFeedback: HapticFeedback,
    context: Context
): CaptureLaunchers {
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.createHealthConnectPermissionContract()
    ) { granted ->
        Timber.i("Health Connect permission result: granted=${granted.size}, required=${com.foodie.app.data.local.healthconnect.HealthConnectManager.REQUIRED_PERMISSIONS.size}")
        if (granted.containsAll(com.foodie.app.data.local.healthconnect.HealthConnectManager.REQUIRED_PERMISSIONS)) {
            Timber.i("✅ Health Connect permissions granted from capture screen")
            viewModel.onHealthConnectPermissionGranted(context)
        } else {
            Timber.w("❌ Health Connect permissions denied (granted: ${granted.size})")
            onNavigateBack()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
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

    return CaptureLaunchers(
        healthConnect = healthConnectPermissionLauncher,
        camera = cameraLauncher,
        permission = permissionLauncher,
        notification = notificationPermissionLauncher
    )
}

@Composable
private fun HandleStateEffects(
    state: CaptureState,
    launchers: CaptureLaunchers,
    viewModel: CapturePhotoViewModel,
    context: Context
) {
    LaunchedEffect(state) {
        when (state) {
            is CaptureState.RequestingHealthConnectPermission -> {
                launchers.healthConnect.launch(
                    com.foodie.app.data.local.healthconnect.HealthConnectManager.REQUIRED_PERMISSIONS
                )
            }
            is CaptureState.RequestingPermission -> {
                launchers.permission.launch(Manifest.permission.CAMERA)
            }
            is CaptureState.ReadyToCapture -> {
                val photoUri = state.photoUri
                launchers.camera.launch(photoUri)
            }
            is CaptureState.NotificationPermissionRequired -> {
                if (viewModel.isNotificationPermissionRequired()) {
                    launchers.notification.launch(NotificationPermissionManager.PERMISSION)
                } else {
                    viewModel.onNotificationPermissionResult(granted = true, context = context)
                }
            }
            else -> {
                // All other states (Ready, Preview, ProcessingPhoto, etc.) are handled in UI rendering
            }
        }
    }
}

@Composable
private fun CaptureScreenContent(
    state: CaptureState,
    viewModel: CapturePhotoViewModel,
    onPhotoConfirmed: (Uri) -> Unit,
    onNavigateBack: () -> Unit,
    context: Context
) {
    when (state) {
        is CaptureState.Idle,
        is CaptureState.RequestingHealthConnectPermission,
        is CaptureState.RequestingPermission,
        is CaptureState.ReadyToCapture,
        is CaptureState.Processing -> {
            LoadingState(state)
        }
        is CaptureState.BackgroundProcessingStarted -> {
            LaunchedEffect(Unit) {
                onPhotoConfirmed(android.net.Uri.EMPTY)
            }
        }
        is CaptureState.NotificationPermissionRequired -> {
            NotificationPermissionRequiredState(viewModel)
        }
        is CaptureState.NotificationPermissionDenied -> {
            NotificationPermissionDeniedState(viewModel, onNavigateBack, context)
        }
        is CaptureState.ProcessingComplete -> {
            ProcessingCompleteState(state, viewModel)
        }
        is CaptureState.Error -> {
            ErrorState(state, viewModel, onNavigateBack, context)
        }
        is CaptureState.PermissionDenied -> {
            PermissionDeniedState(onNavigateBack, context)
        }
        is CaptureState.HealthConnectPermissionDenied -> {
            LaunchedEffect(Unit) {
                Timber.w("Health Connect permissions denied - closing capture screen")
                onNavigateBack()
            }
        }
        is CaptureState.StorageFull -> {
            StorageFullScreen(onCancel = onNavigateBack)
        }
    }
}

@Composable
private fun LoadingState(state: CaptureState) {
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

@Composable
private fun NotificationPermissionRequiredState(viewModel: CapturePhotoViewModel) {
    viewModel.getProcessedPhotoUri()?.let { processedUri ->
        PreviewScreen(
            photoUri = processedUri,
            onRetake = { viewModel.onRetake() },
            onUsePhoto = { },
            confirmationEnabled = false
        )
    }
}

@Composable
private fun NotificationPermissionDeniedState(
    viewModel: CapturePhotoViewModel,
    onNavigateBack: () -> Unit,
    context: Context
) {
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

@Composable
private fun ProcessingCompleteState(
    state: CaptureState.ProcessingComplete,
    viewModel: CapturePhotoViewModel
) {
    PreviewScreen(
        photoUri = state.processedPhotoUri,
        onRetake = { viewModel.onRetake() },
        onUsePhoto = { ctx -> viewModel.onUsePhoto(ctx) }
    )
}

@Composable
private fun ErrorState(
    state: CaptureState.Error,
    viewModel: CapturePhotoViewModel,
    onNavigateBack: () -> Unit,
    context: Context
) {
    CaptureErrorScreen(
        message = state.message,
        onRetry = { viewModel.checkPermissionAndPrepare(context) },
        onCancel = onNavigateBack
    )
}

@Composable
private fun PermissionDeniedState(onNavigateBack: () -> Unit, context: Context) {
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

private data class CaptureLaunchers(
    val healthConnect: androidx.activity.result.ActivityResultLauncher<Set<String>>,
    val camera: androidx.activity.result.ActivityResultLauncher<Uri>,
    val permission: androidx.activity.result.ActivityResultLauncher<String>,
    val notification: androidx.activity.result.ActivityResultLauncher<String>
)

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
