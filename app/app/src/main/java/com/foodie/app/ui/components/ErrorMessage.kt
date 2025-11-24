package com.foodie.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Reusable error message display component using Snackbar.
 *
 * Shows a user-friendly error message with optional retry action.
 * Automatically dismisses after 4 seconds.
 *
 * Usage:
 * ```
 * val snackbarHostState = remember { SnackbarHostState() }
 * val errorMessage by viewModel.errorState.collectAsState()
 *
 * ErrorMessage(
 *     message = errorMessage,
 *     snackbarHostState = snackbarHostState,
 *     onRetry = { viewModel.retry() },
 *     onDismiss = { viewModel.clearError() }
 * )
 * ```
 *
 * @param message The error message to display, null if no error
 * @param snackbarHostState The SnackbarHostState to show the message in
 * @param onRetry Optional callback when user taps "Retry" action
 * @param onDismiss Callback when error is dismissed (auto-dismiss or manual)
 */
@Composable
fun ErrorMessage(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        message?.let { errorText ->
            val result = snackbarHostState.showSnackbar(
                message = errorText,
                actionLabel = if (onRetry != null) "Retry" else null,
                withDismissAction = true,
                duration = SnackbarDuration.Long // 4 seconds
            )
            
            when (result) {
                SnackbarResult.ActionPerformed -> onRetry?.invoke()
                SnackbarResult.Dismissed -> onDismiss()
            }
        }
    }
}

/**
 * Simple error message display without retry action.
 *
 * Usage:
 * ```
 * ErrorMessage(
 *     message = errorMessage,
 *     snackbarHostState = snackbarHostState,
 *     onDismiss = { viewModel.clearError() }
 * )
 * ```
 */
@Composable
fun ErrorMessage(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    ErrorMessage(
        message = message,
        snackbarHostState = snackbarHostState,
        onRetry = null,
        onDismiss = onDismiss
    )
}
