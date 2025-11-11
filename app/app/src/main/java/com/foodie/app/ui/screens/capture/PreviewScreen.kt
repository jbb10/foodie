package com.foodie.app.ui.screens.capture

import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.foodie.app.R
import kotlinx.coroutines.delay

/**
 * Preview screen for captured photo with confirmation options.
 *
 * Displays the processed photo (2MP, 80% JPEG) and provides Retake/Use Photo
 * action buttons for one-handed operation.
 *
 * Architecture:
 * - Stateless composable receiving callbacks
 * - Uses Coil for efficient image loading from FileProvider URI
 * - Material3 design with large touch targets (48dp minimum)
 * - Visual checkmark animation on photo confirmation (AC#9)
 *
 * Layout:
 * - Full-screen photo preview (fills available space)
 * - Bottom action bar with Retake (left) and Use Photo (right) buttons
 * - Buttons sized for one-handed thumb access
 * - Checkmark overlay shown briefly (700ms) before proceeding
 *
 * @param photoUri URI of processed photo to preview
 * @param onRetake Callback when user taps Retake button (returns to camera)
 * @param onUsePhoto Callback when user taps Use Photo button (proceeds to processing)
 */
@Composable
fun PreviewScreen(
    photoUri: Uri,
    onRetake: () -> Unit,
    onUsePhoto: (Context) -> Unit,
    confirmationEnabled: Boolean = true
) {
    val context = LocalContext.current
    var showCheckmark by remember { mutableStateOf(false) }

    LaunchedEffect(confirmationEnabled) {
        if (!confirmationEnabled) {
            showCheckmark = false
        }
    }

    // Animate checkmark fade-in and trigger callback after delay
    LaunchedEffect(showCheckmark) {
        if (showCheckmark && confirmationEnabled) {
            delay(700) // Display checkmark for 700ms (AC#9)
            onUsePhoto(context)
        }
    }

    val checkmarkAlpha by animateFloatAsState(
        targetValue = if (showCheckmark) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkmark_fade"
    )

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Photo preview (full screen)
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(photoUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = stringResource(R.string.preview_photo_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Visual checkmark overlay (AC#9)
            if (showCheckmark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .alpha(checkmarkAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Photo confirmed",
                        tint = Color(0xFF4CAF50), // Material Green 500
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            // Action buttons (bottom overlay) - hide when checkmark visible
            if (!showCheckmark) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Retake button (left side, secondary style)
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp), // > 48dp for easy thumb access
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.preview_retake),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Use Photo button (right side, primary style)
                    Button(
                        onClick = { showCheckmark = true }, // Trigger checkmark animation
                        enabled = confirmationEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp), // > 48dp for easy thumb access
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.preview_use_photo),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
