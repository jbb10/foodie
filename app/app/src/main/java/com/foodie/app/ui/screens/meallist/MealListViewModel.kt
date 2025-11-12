package com.foodie.app.ui.screens.meallist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.domain.usecase.DeleteMealEntryUseCase
import com.foodie.app.domain.usecase.GetMealHistoryUseCase
import com.foodie.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Meal List screen.
 *
 * Manages meal list state with proper error handling using MealListState.
 * Loads meal history from the last 7 days and groups entries by date.
 */
@HiltViewModel
class MealListViewModel @Inject constructor(
    private val getMealHistoryUseCase: GetMealHistoryUseCase,
    private val deleteMealEntryUseCase: DeleteMealEntryUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(MealListState())
    val state: StateFlow<MealListState> = _state.asStateFlow()
    
    /**
     * Loads meal entries from Health Connect for the last 7 days.
     * Groups meals by date with headers like "Today", "Yesterday", "Nov 7".
     */
    fun loadMeals() {
        fetchMeals(isRefresh = false)
    }
    
    /**
     * Refreshes the meal list (called by pull-to-refresh).
     */
    fun refresh() {
        fetchMeals(isRefresh = true)
    }
    
    private fun fetchMeals(isRefresh: Boolean) {
        viewModelScope.launch {
            val startTimeMs = System.currentTimeMillis()
            if (isRefresh) {
                _state.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            getMealHistoryUseCase().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val groupedMeals = groupMealsByDate(result.data)
                        _state.update {
                            it.copy(
                                mealsByDate = groupedMeals,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                emptyStateVisible = result.data.isEmpty()
                            )
                        }
                        logLoadDuration(isRefresh, startTimeMs, resultCount = result.data.size, isError = false)
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message
                            )
                        }
                        logLoadDuration(isRefresh, startTimeMs, resultCount = 0, isError = true)
                        Timber.e(result.exception, "Failed to ${if (isRefresh) "refresh" else "load"} meals: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Already handled by initial state update
                    }
                }
            }
        }
    }

    private fun logLoadDuration(isRefresh: Boolean, startTimeMs: Long, resultCount: Int, isError: Boolean) {
        val elapsed = System.currentTimeMillis() - startTimeMs
        val action = if (isRefresh) "refresh" else "load"
        val message = "Meal list $action completed in ${elapsed}ms (${if (isError) "error" else "$resultCount entries"})"
        if (elapsed > 500) {
            Timber.w(message)
        } else {
            Timber.i(message)
        }
    }

    /**
     * Groups meal entries by date with headers like "Today", "Yesterday", or formatted date.
     * Sorts groups chronologically with newest dates first.
     */
    private fun groupMealsByDate(meals: List<MealEntry>): Map<String, List<MealEntry>> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
        
        // First, create a map of header to (date, meals) for proper sorting
        val groupedWithDates = meals
            .groupBy { meal ->
                val zoneId = meal.zoneOffset?.let { ZoneId.ofOffset("UTC", it) } ?: ZoneId.systemDefault()
                val mealDate = meal.timestamp.atZone(zoneId).toLocalDate()
                Timber.d(
                    "Meal grouping: id=%s, timestamp=%s, localDate=%s, zoneOffset=%s, today=%s, yesterday=%s",
                    meal.id,
                    meal.timestamp,
                    mealDate,
                    meal.zoneOffset,
                    today,
                    yesterday
                )
                mealDate
            }
            .map { (mealDate, mealsForDate) ->
                val header = when (mealDate) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> mealDate.format(dateFormatter)
                }
                Triple(header, mealDate, mealsForDate)
            }
            .sortedByDescending { it.second } // Sort by actual date, newest first
        
        // Convert to map preserving sorted order using LinkedHashMap
        return groupedWithDates.associate { (header, _, mealsForDate) ->
            header to mealsForDate
        }
    }
    
    /**
     * Clears the error state and retries loading meals.
     */
    fun retryLoadMeals() {
        _state.update { it.copy(error = null) }
        loadMeals()
    }
    
    /**
     * Handles long-press gesture on a meal entry.
     * Shows delete confirmation dialog for the selected entry.
     *
     * @param mealId The ID of the meal entry to potentially delete
     */
    fun onMealLongPress(mealId: String) {
        Timber.d("Long-press detected on meal $mealId, showing delete dialog")
        _state.update { it.copy(showDeleteDialog = true, deleteTargetId = mealId) }
    }
    
    /**
     * Dismisses the delete confirmation dialog without deleting.
     * Clears the selected meal ID.
     */
    fun onDismissDeleteDialog() {
        Timber.d("Delete dialog dismissed by user")
        _state.update { it.copy(showDeleteDialog = false, deleteTargetId = null) }
    }
    
    /**
     * Confirms deletion of the selected meal entry.
     * Removes the entry from Health Connect and updates the UI state.
     */
    fun onDeleteConfirmed() {
        val targetId = _state.value.deleteTargetId
        if (targetId == null) {
            Timber.w("Delete confirmed but no target ID set")
            return
        }
        
        Timber.d("Delete confirmed for meal $targetId")
        viewModelScope.launch {
            val startTimeMs = System.currentTimeMillis()
            
            when (val result = deleteMealEntryUseCase(targetId)) {
                is Result.Success -> {
                    val elapsed = System.currentTimeMillis() - startTimeMs
                    Timber.i("Meal $targetId deleted successfully in ${elapsed}ms")
                    
                    // Remove entry from state
                    _state.update { state ->
                        val updatedMeals = state.mealsByDate.mapValues { (_, meals) ->
                            meals.filterNot { it.id == targetId }
                        }.filterValues { it.isNotEmpty() }
                        
                        state.copy(
                            mealsByDate = updatedMeals,
                            showDeleteDialog = false,
                            deleteTargetId = null,
                            successMessage = "Entry deleted",
                            emptyStateVisible = updatedMeals.isEmpty()
                        )
                    }
                }
                is Result.Error -> {
                    val elapsed = System.currentTimeMillis() - startTimeMs
                    Timber.e(result.exception, "Failed to delete meal $targetId in ${elapsed}ms: ${result.message}")
                    
                    _state.update { it.copy(
                        showDeleteDialog = false,
                        deleteTargetId = null,
                        error = result.message
                    )}
                }
                is Result.Loading -> {
                    // Deletion is a single operation, should never be Loading
                }
            }
        }
    }
    
    /**
     * Clears the success message after it has been displayed.
     */
    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Handles delete confirmation for future delete workflow (Story 3.4).
     * Currently logs intent to delete so UX matches acceptance criteria.
     */
    fun onDeleteMealConfirmed(meal: MealEntry) {
        Timber.i(
            "Delete requested for meal id=%s (deferred to Story 3.4)",
            meal.id
        )
    }
    /**
     * Clears the current error message without reloading.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
