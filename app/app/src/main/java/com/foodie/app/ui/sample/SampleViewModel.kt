package com.foodie.app.ui.sample

import androidx.lifecycle.viewModelScope
import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.ui.base.BaseViewModel
import com.foodie.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel demonstrating the MVVM pattern for the Foodie app.
 *
 * This ViewModel shows:
 * - Hilt dependency injection with @HiltViewModel
 * - Repository pattern for data access
 * - StateFlow for reactive UI state management
 * - Result wrapper for error handling
 * - viewModelScope for coroutine lifecycle management
 *
 * This is a sample implementation to establish the pattern. Future feature
 * ViewModels should follow this structure.
 *
 * Architecture flow:
 * 1. UI calls loadSampleData()
 * 2. ViewModel calls repository.getMealHistory()
 * 3. Repository returns Flow<Result<List<MealEntry>>>
 * 4. ViewModel maps Result to UI state
 * 5. UI observes state and renders accordingly
 */
@HiltViewModel
class SampleViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : BaseViewModel() {

    // Private mutable state - only ViewModel can modify
    private val _state = MutableStateFlow(SampleState())

    // Public immutable state - UI can observe but not modify
    val state: StateFlow<SampleState> = _state.asStateFlow()

    /**
     * Loads sample meal data from the repository.
     *
     * This demonstrates the full MVVM stack:
     * - Calls repository method
     * - Handles loading, success, and error states
     * - Updates UI state reactively
     *
     * The repository returns a Flow, so we collect it in viewModelScope
     * which automatically cancels when the ViewModel is cleared.
     */
    fun loadSampleData() {
        logDebug("LoadData", "Starting to load meal history")

        mealRepository.getMealHistory()
            .onEach { result ->
                when (result) {
                    is Result.Loading -> {
                        logDebug("LoadData", "Loading state")
                        _state.value = SampleState(isLoading = true)
                    }
                    is Result.Success -> {
                        logDebug("LoadData", "Success: ${result.data.size} meals loaded")
                        _state.value = SampleState(
                            isLoading = false,
                            meals = result.data,
                            error = null,
                        )
                    }
                    is Result.Error -> {
                        logError("LoadData", result.exception, result.message)
                        _state.value = SampleState(
                            isLoading = false,
                            meals = emptyList(),
                            error = result.message,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Clears any error state.
     *
     * Called when user dismisses error message.
     */
    fun clearError() {
        if (_state.value.error != null) {
            _state.value = _state.value.copy(error = null)
        }
    }
}
