package com.foodie.app.ui.screens.meallist

import com.foodie.app.domain.model.MealEntry

/**
 * UI state for the Meal List screen.
 *
 * Represents all possible states of the meal list including loading, success, and error states.
 * Meals are grouped by date for display with headers ("Today", "Yesterday", etc.)
 *
 * @param mealsByDate Map of date headers to meal entries (e.g., "Today" -> [meal1, meal2])
 * @param isLoading True if initial data load is in progress
 * @param isRefreshing True if pull-to-refresh is in progress
 * @param error User-friendly error message, null if no error
 * @param emptyStateVisible True when no meals exist (shows empty state message)
 * @param showDeleteDialog True when delete confirmation dialog should be shown
 * @param deleteTargetId ID of the meal entry selected for deletion, null if no entry selected
 * @param successMessage User-friendly success message (e.g., "Entry deleted"), null if no message
 */
data class MealListState(
    val mealsByDate: Map<String, List<MealEntry>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val emptyStateVisible: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleteTargetId: String? = null,
    val successMessage: String? = null
)
