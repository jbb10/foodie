package com.foodie.app.ui.screens.meallist

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foodie.app.R
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.ui.theme.FoodieTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import timber.log.Timber

/**
 * Meal list screen displaying nutrition entries from Health Connect.
 *
 * Shows meals grouped by date (Today, Yesterday, or specific date) with swipe-to-refresh,
 * long-press delete, and error handling with retry capability. Navigation to detail screen
 * passes full meal data for editing.
 *
 * @param onMealClick Callback invoked when meal entry is tapped, receives MealEntry object
 * @param onSettingsClick Callback invoked when settings button is tapped
 * @param viewModel ViewModel managing meal list state and Health Connect operations
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealListScreen(
    onMealClick: (MealEntry) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Health Connect permission launcher
    val requestHealthConnectPermissions = setupHealthConnectPermissionLauncher(viewModel)

    // Initial load on first composition
    HandleInitialLoad(viewModel)

    // Refresh when app resumes from background (AC #1, #2, #3, #4)
    HandleLifecycleRefresh(lifecycleOwner, state, viewModel)

    // Show error snackbar with retry action, or request permissions if SecurityException
    HandleErrorSnackbar(
        error = state.error,
        snackbarHostState = snackbarHostState,
        viewModel = viewModel,
        requestHealthConnectPermissions = requestHealthConnectPermissions,
    )

    // Show success toast message
    HandleSuccessMessage(state.successMessage, snackbarHostState, viewModel)

    MealListScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        callbacks = MealListCallbacks(
            onRefresh = viewModel::refresh,
            onMealClick = onMealClick,
            onSettingsClick = onSettingsClick,
            onMealLongPress = viewModel::onMealLongPress,
            onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
        ),
        modifier = modifier,
    )
}

/**
 * Data class to group callback functions and reduce parameter count
 */
internal data class MealListCallbacks(
    val onRefresh: () -> Unit,
    val onMealClick: (MealEntry) -> Unit,
    val onSettingsClick: () -> Unit,
    val onMealLongPress: (String) -> Unit,
    val onDismissDeleteDialog: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
)

@VisibleForTesting
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MealListScreenContent(
    state: MealListState,
    snackbarHostState: SnackbarHostState,
    callbacks: MealListCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = callbacks.onSettingsClick,
                        modifier = Modifier
                            .testTag("settings_button")
                            .semantics { contentDescription = "Open Settings" },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null, // Description on IconButton, not Icon
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = callbacks.onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading -> {
                    // Show loading indicator on first load
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.emptyStateVisible -> {
                    // Show empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.meal_list_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }
                else -> {
                    // Show meal list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.mealsByDate.forEach { (dateHeader, meals) ->
                            // Date header
                            item(key = "header_$dateHeader") {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }

                            // Meals for this date
                            items(
                                items = meals,
                                key = { meal -> meal.id },
                            ) { meal ->
                                MealEntryCard(
                                    meal = meal,
                                    onClick = { callbacks.onMealClick(meal) },
                                    onLongClick = { callbacks.onMealLongPress(meal.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show delete confirmation dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = callbacks.onDismissDeleteDialog,
            title = { Text(text = stringResource(id = R.string.meal_list_delete_title)) },
            text = { Text(text = stringResource(id = R.string.meal_list_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = callbacks.onDeleteConfirmed,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = stringResource(id = R.string.meal_list_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = callbacks.onDismissDeleteDialog) {
                    Text(text = stringResource(id = R.string.meal_list_delete_cancel))
                }
            },
        )
    }
}

/**
 * Individual meal item card in the list.
 *
 * Displays meal description, timestamp, calories, and macros in a tappable card.
 * Supports long-press for future delete functionality.
 *
 * **Epic 7 Extension:** Added macros display line showing protein, carbs, and fat.
 * Legacy records (pre-Epic-7) display "P: 0g | C: 0g | F: 0g".
 *
 * @param meal The meal entry to display
 * @param onClick Callback invoked when the card is tapped
 * @param onLongClick Callback invoked when the card is long-pressed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealEntryCard(
    meal: MealEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Cache formatted timestamp to prevent recalculation on every recomposition (Story 5.6)
    val formattedTimestamp = remember(meal.timestamp) {
        formatTimestamp(meal.timestamp)
    }

    // Cache formatted macros to prevent recalculation (Epic 7)
    val formattedMacros = remember(meal.protein, meal.carbs, meal.fat) {
        "P: ${meal.protein}g | C: ${meal.carbs}g | F: ${meal.fat}g"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Meal entry, ${meal.calories} calories, ${meal.description}, $formattedTimestamp, macros: $formattedMacros"
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = meal.description,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = formattedTimestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${meal.calories} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            // Epic 7: Macros line (protein, carbs, fat)
            Text(
                text = formattedMacros,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Formats a timestamp for display (e.g., "2:30 PM").
 *
 * @param timestamp The instant to format
 * @return Formatted timestamp string
 */
private fun formatTimestamp(timestamp: Instant): String {
    return TIME_FORMATTER.format(timestamp)
}

private val TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())

/**
 * Sets up Health Connect permission launcher with result handling.
 */
@Composable
private fun setupHealthConnectPermissionLauncher(
    viewModel: MealListViewModel,
): ManagedActivityResultLauncher<Set<String>, Set<String>> {
    return rememberLauncherForActivityResult(
        viewModel.createPermissionRequestContract(),
    ) { granted ->
        Timber.i("Health Connect permission result from MealListScreen: granted=${granted.size}")
        if (granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)) {
            Timber.i("Health Connect permissions granted, reloading meals")
            viewModel.loadMeals()
        } else {
            Timber.w("Health Connect permissions denied or incomplete")
        }
    }
}

/**
 * Handles initial meal loading on first composition.
 */
@Composable
private fun HandleInitialLoad(viewModel: MealListViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadMeals()
    }
}

/**
 * Handles automatic refresh when app resumes from background.
 */
@Composable
private fun HandleLifecycleRefresh(
    lifecycleOwner: LifecycleOwner,
    state: MealListState,
    viewModel: MealListViewModel,
) {
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // This block runs when lifecycle enters STARTED state and cancels when it drops below
            // We use STARTED instead of RESUMED to handle cases like multi-window mode
            // Skip refresh if this is the first composition (already handled by loadMeals above)
            if (!state.isLoading && state.mealsByDate.isNotEmpty()) {
                viewModel.refresh()
            }
        }
    }
}

/**
 * Handles error snackbar display with permission request logic.
 */
@Composable
private fun HandleErrorSnackbar(
    error: String?,
    snackbarHostState: SnackbarHostState,
    viewModel: MealListViewModel,
    requestHealthConnectPermissions: ManagedActivityResultLauncher<Set<String>, Set<String>>,
) {
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            val isPermissionError = isPermissionError(errorMessage)

            if (isPermissionError) {
                handlePermissionError(viewModel, snackbarHostState, errorMessage, requestHealthConnectPermissions)
            } else {
                handleRegularError(viewModel, snackbarHostState, errorMessage)
            }
        }
    }
}

/**
 * Checks if error message indicates a permission issue.
 */
private fun isPermissionError(errorMessage: String): Boolean {
    return errorMessage.contains("Permission denied", ignoreCase = true) ||
        errorMessage.contains("grant Health Connect access", ignoreCase = true)
}

/**
 * Handles permission-related errors by requesting HC permissions or showing retry.
 */
private suspend fun handlePermissionError(
    viewModel: MealListViewModel,
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
    requestHealthConnectPermissions: ManagedActivityResultLauncher<Set<String>, Set<String>>,
) {
    Timber.i("Permission error detected, requesting Health Connect permissions")
    viewModel.clearError() // Clear error immediately

    val hasPermissions = viewModel.hasHealthConnectPermissions()
    if (!hasPermissions) {
        Timber.i("Requesting Health Connect permissions from MealListScreen")
        requestHealthConnectPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
    } else {
        // Permissions exist but still got error - show regular retry snackbar
        Timber.w("Permission error but permissions exist - showing retry")
        showRetrySnackbar(viewModel, snackbarHostState, errorMessage)
    }
}

/**
 * Handles regular (non-permission) errors by showing retry snackbar.
 */
private suspend fun handleRegularError(
    viewModel: MealListViewModel,
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
) {
    showRetrySnackbar(viewModel, snackbarHostState, errorMessage)
}

/**
 * Shows snackbar with retry action.
 */
private suspend fun showRetrySnackbar(
    viewModel: MealListViewModel,
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
) {
    val result = snackbarHostState.showSnackbar(
        message = errorMessage,
        actionLabel = "Retry",
        withDismissAction = true,
    )
    if (result == SnackbarResult.ActionPerformed) {
        viewModel.retryLoadMeals()
    } else {
        viewModel.clearError()
    }
}

/**
 * Handles success message display.
 */
@Composable
private fun HandleSuccessMessage(
    successMessage: String?,
    snackbarHostState: SnackbarHostState,
    viewModel: MealListViewModel,
) {
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
            )
            viewModel.clearSuccessMessage()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListScreenPreview() {
    FoodieTheme {
        val sampleMeals = listOf(
            MealEntry(
                id = "1",
                timestamp = Instant.now(),
                description = "Avocado toast",
                calories = 320,
            ),
        )
        val state = MealListState(
            mealsByDate = mapOf("Today" to sampleMeals),
        )
        val snackbarHostState = remember { SnackbarHostState() }
        MealListScreenContent(
            state = state,
            snackbarHostState = snackbarHostState,
            callbacks = MealListCallbacks(
                onRefresh = {},
                onMealClick = {},
                onSettingsClick = {},
                onMealLongPress = {},
                onDismissDeleteDialog = {},
                onDeleteConfirmed = {},
            ),
        )
    }
}
