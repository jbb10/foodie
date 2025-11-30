package com.foodie.app.ui.screens.mealdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.domain.usecase.DeleteMealEntryUseCase
import com.foodie.app.domain.usecase.UpdateMealEntryUseCase
import com.foodie.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val updateMealEntryUseCase: UpdateMealEntryUseCase,
    private val deleteMealEntryUseCase: DeleteMealEntryUseCase,
) : ViewModel() {

    companion object {
        private const val TAG = "MealDetailViewModel"
    }

    // Navigation args
    private val recordId: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["recordId"]) {
            "recordId is required for MealDetailScreen"
        },
        Charsets.UTF_8.name(),
    )
    private val initialCalories: String = checkNotNull(savedStateHandle["calories"]) {
        "calories is required for MealDetailScreen"
    }
    private val initialDescription: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["description"]) {
            "description is required for MealDetailScreen"
        },
        Charsets.UTF_8.name(),
    )
    private val initialProtein: String = checkNotNull(savedStateHandle["protein"]) {
        "protein is required for MealDetailScreen"
    }
    private val initialCarbs: String = checkNotNull(savedStateHandle["carbs"]) {
        "carbs is required for MealDetailScreen"
    }
    private val initialFat: String = checkNotNull(savedStateHandle["fat"]) {
        "fat is required for MealDetailScreen"
    }
    private val timestampMillis: Long = checkNotNull(savedStateHandle["timestamp"]) {
        "timestamp is required for MealDetailScreen"
    }

    private val _uiState = MutableStateFlow(
        MealDetailState(
            recordId = recordId,
            calories = initialCalories,
            description = initialDescription,
            protein = initialProtein,
            carbs = initialCarbs,
            fat = initialFat,
            timestamp = Instant.ofEpochMilli(timestampMillis),
        ),
    )
    val uiState: StateFlow<MealDetailState> = _uiState.asStateFlow()

    /**
     * Handles all user interaction events from the UI.
     */
    fun onEvent(event: MealDetailEvent) {
        when (event) {
            is MealDetailEvent.CaloriesChanged -> handleCaloriesChanged(event.value)
            is MealDetailEvent.ProteinChanged -> handleProteinChanged(event.value)
            is MealDetailEvent.CarbsChanged -> handleCarbsChanged(event.value)
            is MealDetailEvent.FatChanged -> handleFatChanged(event.value)
            is MealDetailEvent.DescriptionChanged -> handleDescriptionChanged(event.value)
            is MealDetailEvent.SaveClicked -> handleSaveClicked()
            MealDetailEvent.CancelClicked -> handleCancelClicked()
            MealDetailEvent.DeleteClicked -> handleDeleteClicked()
            MealDetailEvent.DeleteConfirmed -> handleDeleteConfirmed()
            MealDetailEvent.DeleteCancelled -> handleDeleteCancelled()
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

    private fun handleProteinChanged(value: String) {
        // Normalize comma to dot and filter to valid decimal characters
        val normalized = value.replace(',', '.')
        val filtered = normalized.filter { it.isDigit() || it == '.' }

        // Ensure only one decimal point and limit to 2 decimal places
        val validDecimal = if (filtered.count { it == '.' } <= 1) {
            val parts = filtered.split('.')
            if (parts.size == 2 && parts[1].length > 2) {
                "${parts[0]}.${parts[1].take(2)}"
            } else {
                filtered
            }
        } else {
            // Multiple decimal points - keep only the first one
            val firstDotIndex = filtered.indexOf('.')
            filtered.take(firstDotIndex + 1) + filtered.drop(firstDotIndex + 1).filter { it != '.' }
        }

        // Validate protein range (0-500g)
        val error = when {
            validDecimal.isNotEmpty() && validDecimal != "." && validDecimal.toDoubleOrNull() == null -> "Invalid number"
            validDecimal.isNotEmpty() && validDecimal != "." && (validDecimal.toDoubleOrNull() ?: 0.0) !in 0.0..500.0 -> "Protein must be 0-500g"
            else -> null
        }

        _uiState.update { it.copy(protein = validDecimal, proteinError = error) }
    }

    private fun handleCarbsChanged(value: String) {
        // Normalize comma to dot and filter to valid decimal characters
        val normalized = value.replace(',', '.')
        val filtered = normalized.filter { it.isDigit() || it == '.' }

        // Ensure only one decimal point and limit to 2 decimal places
        val validDecimal = if (filtered.count { it == '.' } <= 1) {
            val parts = filtered.split('.')
            if (parts.size == 2 && parts[1].length > 2) {
                "${parts[0]}.${parts[1].take(2)}"
            } else {
                filtered
            }
        } else {
            // Multiple decimal points - keep only the first one
            val firstDotIndex = filtered.indexOf('.')
            filtered.take(firstDotIndex + 1) + filtered.drop(firstDotIndex + 1).filter { it != '.' }
        }

        // Validate carbs range (0-1000g)
        val error = when {
            validDecimal.isNotEmpty() && validDecimal != "." && validDecimal.toDoubleOrNull() == null -> "Invalid number"
            validDecimal.isNotEmpty() && validDecimal != "." && (validDecimal.toDoubleOrNull() ?: 0.0) !in 0.0..1000.0 -> "Carbs must be 0-1000g"
            else -> null
        }

        _uiState.update { it.copy(carbs = validDecimal, carbsError = error) }
    }

    private fun handleFatChanged(value: String) {
        // Normalize comma to dot and filter to valid decimal characters
        val normalized = value.replace(',', '.')
        val filtered = normalized.filter { it.isDigit() || it == '.' }

        // Ensure only one decimal point and limit to 2 decimal places
        val validDecimal = if (filtered.count { it == '.' } <= 1) {
            val parts = filtered.split('.')
            if (parts.size == 2 && parts[1].length > 2) {
                "${parts[0]}.${parts[1].take(2)}"
            } else {
                filtered
            }
        } else {
            // Multiple decimal points - keep only the first one
            val firstDotIndex = filtered.indexOf('.')
            filtered.take(firstDotIndex + 1) + filtered.drop(firstDotIndex + 1).filter { it != '.' }
        }

        // Validate fat range (0-500g)
        val error = when {
            validDecimal.isNotEmpty() && validDecimal != "." && validDecimal.toDoubleOrNull() == null -> "Invalid number"
            validDecimal.isNotEmpty() && validDecimal != "." && (validDecimal.toDoubleOrNull() ?: 0.0) !in 0.0..500.0 -> "Fat must be 0-500g"
            else -> null
        }

        _uiState.update { it.copy(fat = validDecimal, fatError = error) }
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

        // Parse macros (default to 0.0 if empty)
        val protein = currentState.protein.toDoubleOrNull() ?: 0.0
        val carbs = currentState.carbs.toDoubleOrNull() ?: 0.0
        val fat = currentState.fat.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }

            val result = updateMealEntryUseCase(
                recordId = currentState.recordId,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                description = currentState.description,
                timestamp = currentState.timestamp,
            )

            when (result) {
                is Result.Success -> {
                    Timber.tag(TAG).i("Meal entry updated successfully: ${currentState.recordId}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            shouldNavigateBack = true,
                            successMessage = "Entry updated",
                        )
                    }
                }
                is Result.Error -> {
                    Timber.tag(TAG).e(result.exception, "Failed to update meal entry")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.message,
                            successMessage = null,
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

    private fun handleDeleteClicked() {
        Timber.tag(TAG).d("User requested delete")
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    private fun handleDeleteConfirmed() {
        Timber.tag(TAG).i("User confirmed delete for entry: ${_uiState.value.recordId}")
        _uiState.update { it.copy(showDeleteConfirmation = false, isDeleting = true, error = null) }

        viewModelScope.launch {
            val result = deleteMealEntryUseCase(_uiState.value.recordId)

            when (result) {
                is Result.Success -> {
                    Timber.tag(TAG).i("Meal entry deleted successfully: ${_uiState.value.recordId}")
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            shouldNavigateBack = true,
                            successMessage = "Entry deleted",
                        )
                    }
                }
                is Result.Error -> {
                    Timber.tag(TAG).e(result.exception, "Failed to delete meal entry")
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = result.message,
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen with suspend function
                }
            }
        }
    }

    private fun handleDeleteCancelled() {
        Timber.tag(TAG).d("User cancelled delete")
        _uiState.update { it.copy(showDeleteConfirmation = false) }
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
