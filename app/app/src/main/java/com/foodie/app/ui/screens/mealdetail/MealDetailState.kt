package com.foodie.app.ui.screens.mealdetail

import java.time.Instant

/**
 * UI state for the meal detail/edit screen.
 *
 * Contains all data needed to render the form, including validation errors
 * and loading/navigation states.
 *
 * **Epic 7 Extension:** Added macros fields (protein, carbs, fat) with validation errors.
 */
data class MealDetailState(
    val recordId: String,
    val calories: String,
    val description: String,
    val protein: String = "0",
    val carbs: String = "0",
    val fat: String = "0",
    val timestamp: Instant,
    val caloriesError: String? = null,
    val descriptionError: String? = null,
    val proteinError: String? = null,
    val carbsError: String? = null,
    val fatError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val error: String? = null,
    val shouldNavigateBack: Boolean = false,
    val successMessage: String? = null,
) {
    /**
     * Checks if there are any validation errors that would prevent saving.
     */
    fun hasErrors(): Boolean = caloriesError != null ||
        descriptionError != null ||
        proteinError != null ||
        carbsError != null ||
        fatError != null

    /**
     * Checks if the Save button should be enabled.
     */
    fun canSave(): Boolean = !hasErrors() &&
        !isSaving &&
        !isDeleting &&
        calories.isNotBlank() &&
        description.isNotBlank() &&
        protein.isNotBlank() &&
        carbs.isNotBlank() &&
        fat.isNotBlank()
}
