package com.foodie.app.ui.screens.meallist

import com.foodie.app.domain.model.MealEntry

/**
 * UI state for the Meal List screen.
 *
 * Represents all possible states of the meal list including loading, success, and error states.
 *
 * @param meals List of meal entries to display
 * @param isLoading True if data is being loaded
 * @param error User-friendly error message, null if no error
 */
data class MealListState(
    val meals: List<MealEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
