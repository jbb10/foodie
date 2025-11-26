package com.foodie.app.ui.screens.mealdetail

import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Show success toast when update completes
    HandleSuccessToast(uiState.successMessage, context)

    // Handle navigation
    HandleNavigation(uiState.shouldNavigateBack, viewModel, onNavigateBack)

    // Show error snackbar
    HandleErrorSnackbar(uiState.error, snackbarHostState, viewModel)

    MealDetailScreenContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Displays success toast message.
 */
@Composable
private fun HandleSuccessToast(successMessage: String?, context: android.content.Context) {
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Handles navigation back when update completes.
 */
@Composable
private fun HandleNavigation(
    shouldNavigateBack: Boolean,
    viewModel: MealDetailViewModel,
    onNavigateBack: () -> Unit,
) {
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            viewModel.onNavigationHandled()
            onNavigateBack()
        }
    }
}

/**
 * Displays error snackbar.
 */
@Composable
private fun HandleErrorSnackbar(
    error: String?,
    snackbarHostState: SnackbarHostState,
    viewModel: MealDetailViewModel,
) {
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onEvent(MealDetailEvent.ErrorDismissed)
        }
    }
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Meal") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(MealDetailEvent.CancelClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        MealDetailFormContent(
            state = state,
            onEvent = onEvent,
            paddingValues = paddingValues,
        )

        // Delete confirmation dialog
        if (state.showDeleteConfirmation) {
            DeleteConfirmationDialog(onEvent = onEvent)
        }
    }
}

@Composable
private fun MealDetailFormContent(
    state: MealDetailState,
    onEvent: (MealDetailEvent) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
    ) {
        DescriptionField(
            description = state.description,
            descriptionError = state.descriptionError,
            enabled = !state.isSaving && !state.isDeleting,
            onDescriptionChange = { onEvent(MealDetailEvent.DescriptionChanged(it)) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        CaloriesField(
            calories = state.calories,
            caloriesError = state.caloriesError,
            enabled = !state.isSaving && !state.isDeleting,
            onCaloriesChange = { onEvent(MealDetailEvent.CaloriesChanged(it)) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Captured: ${formatTimestamp(state.timestamp)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        ActionButtons(
            state = state,
            onEvent = onEvent,
        )

        Spacer(modifier = Modifier.height(16.dp))

        DeleteButton(
            enabled = !state.isSaving && !state.isDeleting,
            isDeleting = state.isDeleting,
            onClick = { onEvent(MealDetailEvent.DeleteClicked) },
        )
    }
}

@Composable
private fun DescriptionField(
    description: String,
    descriptionError: String?,
    enabled: Boolean,
    onDescriptionChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Description") },
        isError = descriptionError != null,
        supportingText = {
            if (descriptionError != null) {
                Text(descriptionError, color = MaterialTheme.colorScheme.error)
            } else {
                Text("${description.length}/200 characters")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("descriptionField"),
        maxLines = 3,
        enabled = enabled,
    )
}

@Composable
private fun CaloriesField(
    calories: String,
    caloriesError: String?,
    enabled: Boolean,
    onCaloriesChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = calories,
        onValueChange = onCaloriesChange,
        label = { Text("Calories") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = caloriesError != null,
        supportingText = caloriesError?.let {
            { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("caloriesField"),
        enabled = enabled,
    )
}

@Composable
private fun ActionButtons(
    state: MealDetailState,
    onEvent: (MealDetailEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(
            onClick = { onEvent(MealDetailEvent.CancelClicked) },
            enabled = !state.isSaving && !state.isDeleting,
            modifier = Modifier
                .weight(1f)
                .testTag("cancelButton"),
        ) {
            Text("Cancel")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = { onEvent(MealDetailEvent.SaveClicked) },
            enabled = state.canSave(),
            modifier = Modifier
                .weight(1f)
                .testTag("saveButton"),
        ) {
            Text(if (state.isSaving) "Saving..." else "Save")
        }
    }
}

@Composable
private fun DeleteButton(
    enabled: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deleteButton"),
    ) {
        Text(if (isDeleting) "Deleting..." else "Delete Entry")
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onEvent: (MealDetailEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(MealDetailEvent.DeleteCancelled) },
        title = { Text("Delete Entry?") },
        text = { Text("This will permanently delete this meal entry from your health data. This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = { onEvent(MealDetailEvent.DeleteConfirmed) },
                modifier = Modifier.testTag("confirmDeleteButton"),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(MealDetailEvent.DeleteCancelled) },
                modifier = Modifier.testTag("cancelDeleteButton"),
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("deleteConfirmationDialog"),
    )
}
