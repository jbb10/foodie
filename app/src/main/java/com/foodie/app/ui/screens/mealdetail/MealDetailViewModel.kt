package com.foodie.app.ui.screens.mealdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.domain.usecase.UpdateMealEntryUseCase
import com.foodie.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the meal detail/edit screen.
 *
 * Manages form state, real-time validation, and update operations for meal entries.
 * Follows MVVM + MVI pattern with event-based user interactions.
 *
 * @param savedStateHandle Navigation arguments containing meal entry data
 * @param updateMealEntryUseCase Use case for updating meal entries
 */
@HiltViewModel
class MealDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val updateMealEntryUseCase: UpdateMealEntryUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "MealDetailViewModel"
    }

    // Navigation args
    private val recordId: String = checkNotNull(savedStateHandle["recordId"]) {
        "recordId is required for MealDetailScreen"
    }
    private val initialCalories: String = checkNotNull(savedStateHandle["calories"]) {
        "calories is required for MealDetailScreen"
    }
    private val initialDescription: String = checkNotNull(savedStateHandle["description"]) {
        "description is required for MealDetailScreen"
    }
    private val timestampMillis: Long = checkNotNull(savedStateHandle["timestamp"]) {
        "timestamp is required for MealDetailScreen"
    }

    private val _uiState = MutableStateFlow(
        MealDetailState(
            recordId = recordId,
            calories = initialCalories,
            description = initialDescription,
            timestamp = Instant.ofEpochMilli(timestampMillis)
        )
    )
    val uiState: StateFlow<MealDetailState> = _uiState.asStateFlow()

    /**
     * Handles all user interaction events from the UI.
     */
    fun onEvent(event: MealDetailEvent) {
        when (event) {
            is MealDetailEvent.CaloriesChanged -> handleCaloriesChanged(event.value)
            is MealDetailEvent.DescriptionChanged -> handleDescriptionChanged(event.value)
            is MealDetailEvent.SaveClicked -> handleSaveClicked()
            MealDetailEvent.CancelClicked -> handleCancelClicked()
            MealDetailEvent.ErrorDismissed -> handleErrorDismissed()
        }
    }

    private fun handleCaloriesChanged(value: String) {
        // Filter to digits only
        val filtered = value.filter { it.isDigit() }

        // Validate calories range
        val error = when {
            filtered.isEmpty() -> "Calories are required"
            filtered.toIntOrNull() == null -> "Invalid number"
            filtered.toInt() < 1 -> "Calories must be at least 1"
            filtered.toInt() > 5000 -> "Calories cannot exceed 5000"
            else -> null
        }

        _uiState.update { it.copy(calories = filtered, caloriesError = error) }
    }

    private fun handleDescriptionChanged(value: String) {
        // Enforce max length
        val trimmed = if (value.length > 200) value.take(200) else value

        // Validate description
        val error = when {
            trimmed.isBlank() -> "Description is required"
            else -> null
        }

        _uiState.update { it.copy(description = trimmed, descriptionError = error) }
    }

    private fun handleSaveClicked() {
        val currentState = _uiState.value

        // Final validation check
        if (currentState.hasErrors()) {
            Timber.tag(TAG).w("Save clicked with validation errors present")
            return
        }

        // Parse calories
        val calories = currentState.calories.toIntOrNull()
        if (calories == null) {
            _uiState.update { it.copy(error = "Invalid calories value") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }

            val result = updateMealEntryUseCase(
                recordId = currentState.recordId,
                calories = calories,
                description = currentState.description,
                timestamp = currentState.timestamp
            )

            when (result) {
                is Result.Success -> {
                    Timber.tag(TAG).i("Meal entry updated successfully: ${currentState.recordId}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            shouldNavigateBack = true,
                            successMessage = "Entry updated"
                        )
                    }
                }
                is Result.Error -> {
                    Timber.tag(TAG).e(result.exception, "Failed to update meal entry")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.message ?: "Failed to update entry. Please try again.",
                            successMessage = null
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen with suspend function
                }
            }
        }
    }

    private fun handleCancelClicked() {
        Timber.tag(TAG).d("User cancelled edit")
    _uiState.update { it.copy(shouldNavigateBack = true, successMessage = null) }
    }

    private fun handleErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset navigation state after navigation completes.
     */
    fun onNavigationHandled() {
    _uiState.update { it.copy(shouldNavigateBack = false, successMessage = null) }
    }
}
