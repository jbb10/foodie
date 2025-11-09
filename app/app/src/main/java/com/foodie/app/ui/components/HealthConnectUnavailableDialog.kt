package com.foodie.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber

/**
 * Dialog shown when Health Connect is not available on the device.
 *
 * Provides options to install Health Connect from Play Store or cancel.
 * Handles fallback to browser if Play Store app is not available.
 *
 * @param onDismiss Callback invoked when user dismisses the dialog (Cancel button)
 */
@Composable
fun HealthConnectUnavailableDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Health Connect Required") },
        text = { 
            Text("Health Connect is required but not installed. Install from Play Store?") 
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        // Try Play Store app first
                        val playStoreIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.google.android.apps.healthdata")
                        )
                        context.startActivity(playStoreIntent)
                        Timber.d("Launched Play Store for Health Connect installation")
                    } catch (e: Exception) {
                        // Fallback to web browser if Play Store not installed
                        try {
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                            )
                            context.startActivity(browserIntent)
                            Timber.d("Launched browser for Health Connect installation")
                        } catch (e2: Exception) {
                            Timber.e(e2, "Failed to launch Play Store or browser")
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
