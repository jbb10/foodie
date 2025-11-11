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

    // Handle state changes
    LaunchedEffect(state) {
        when (state) {
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

        is CaptureState.ProcessingComplete -> {
            val processedUri = (state as CaptureState.ProcessingComplete).processedPhotoUri
            PreviewScreen(
                photoUri = processedUri,
                onRetake = {
                    viewModel.onRetake()
                    // Retake will reset state to ReadyToCapture, relaunching camera
                },
                onUsePhoto = {
                    viewModel.onUsePhoto()
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
private fun PermissionDeniedScreen(
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
