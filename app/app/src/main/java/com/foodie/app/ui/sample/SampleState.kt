package com.foodie.app.ui.sample

import com.foodie.app.domain.model.MealEntry

/**
 * UI state for the SampleScreen.
 *
 * Represents all possible states the screen can be in, following the
 * unidirectional data flow pattern in Jetpack Compose.
 *
 * @param isLoading Whether data is currently being loaded
 * @param meals List of meal entries to display (empty if not loaded or on error)
 * @param error User-friendly error message, null if no error
 */
data class SampleState(
    val isLoading: Boolean = false,
    val meals: List<MealEntry> = emptyList(),
    val error: String? = null,
)
