package com.foodie.app.ui.screens.meallist

import com.foodie.app.domain.model.MealEntry
import java.time.LocalDate

data class MealListState(
    val mealsByDate: Map<LocalDate, List<MealEntry>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val emptyStateVisible: Boolean = false
)
