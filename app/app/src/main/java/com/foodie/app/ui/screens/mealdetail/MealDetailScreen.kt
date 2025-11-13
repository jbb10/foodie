package com.foodie.app.ui.screens.mealdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Meal detail/edit screen for modifying meal entries.
 *
 * Displays editable form with calories, description, and read-only timestamp.
 * Provides real-time validation feedback and Save/Cancel actions.
 *
 * @param viewModel ViewModel managing form state and update operations
 * @param onNavigateBack Callback to navigate back to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    viewModel: MealDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Show success toast when update completes
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Handle navigation
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.onNavigationHandled()
            onNavigateBack()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onEvent(MealDetailEvent.ErrorDismissed)
        }
    }

    MealDetailScreenContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState
    )
}

/**
 * Formats timestamp for display.
 *
 * Example: "Nov 12, 2025 at 2:30 PM"
 */
@Composable
private fun formatTimestamp(timestamp: java.time.Instant): String {
    val zonedDateTime = timestamp.atZone(ZoneId.systemDefault())
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return "${zonedDateTime.format(dateFormatter)} at ${zonedDateTime.format(timeFormatter)}"
}

/**
 * UI content for the meal detail/edit screen.
 *
 * Extracted for easier previewing and testing without Hilt dependencies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@VisibleForTesting
internal fun MealDetailScreenContent(
    state: MealDetailState,
    onEvent: (MealDetailEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Meal") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(MealDetailEvent.CancelClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Description first (user's request)
            OutlinedTextField(
                value = state.description,
                onValueChange = { onEvent(MealDetailEvent.DescriptionChanged(it)) },
                label = { Text("Description") },
                isError = state.descriptionError != null,
                supportingText = {
                    if (state.descriptionError != null) {
                        Text(state.descriptionError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${state.description.length}/200 characters")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("descriptionField"),
                maxLines = 3,
                enabled = !state.isSaving && !state.isDeleting
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calories second
            OutlinedTextField(
                value = state.calories,
                onValueChange = { onEvent(MealDetailEvent.CaloriesChanged(it)) },
                label = { Text("Calories") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.caloriesError != null,
                supportingText = state.caloriesError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("caloriesField"),
                enabled = !state.isSaving && !state.isDeleting
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Captured: ${formatTimestamp(state.timestamp)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onEvent(MealDetailEvent.CancelClicked) },
                    enabled = !state.isSaving && !state.isDeleting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancelButton")
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { onEvent(MealDetailEvent.SaveClicked) },
                    enabled = state.canSave(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("saveButton")
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete button
            OutlinedButton(
                onClick = { onEvent(MealDetailEvent.DeleteClicked) },
                enabled = !state.isSaving && !state.isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deleteButton")
            ) {
                Text(if (state.isDeleting) "Deleting..." else "Delete Entry")
            }
        }

        // Delete confirmation dialog
        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { onEvent(MealDetailEvent.DeleteCancelled) },
                title = { Text("Delete Entry?") },
                text = { Text("This will permanently delete this meal entry from your health data. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = { onEvent(MealDetailEvent.DeleteConfirmed) },
                        modifier = Modifier.testTag("confirmDeleteButton")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(MealDetailEvent.DeleteCancelled) },
                        modifier = Modifier.testTag("cancelDeleteButton")
                    ) {
                        Text("Cancel")
                    }
                },
                modifier = Modifier.testTag("deleteConfirmationDialog")
            )
        }
    }
}
