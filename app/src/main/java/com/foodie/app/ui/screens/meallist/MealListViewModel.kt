package com.foodie.app.ui.screens.meallist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.domain.usecase.GetMealHistoryUseCase
import com.foodie.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MealListViewModel @Inject constructor(
    private val getMealHistoryUseCase: GetMealHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealListState())
    val uiState: StateFlow<MealListState> = _uiState.asStateFlow()

    init {
        loadMeals()
    }

    fun loadMeals() {
        getMealHistoryUseCase().onEach { result ->
            _uiState.update { currentState ->
                when (result) {
                    is Result.Loading -> currentState.copy(isLoading = true, error = null)
                    is Result.Success -> {
                        val mealsByDate = result.data
                            .groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
                        currentState.copy(
                            isLoading = false,
                            mealsByDate = mealsByDate,
                            emptyStateVisible = mealsByDate.isEmpty()
                        )
                    }
                    is Result.Error -> currentState.copy(
                        isLoading = false,
                        error = result.exception.message ?: "An unknown error occurred"
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        getMealHistoryUseCase().onEach { result ->
            _uiState.update { currentState ->
                when (result) {
                    is Result.Loading -> currentState // Ignore loading state on refresh
                    is Result.Success -> {
                        val mealsByDate = result.data
                            .groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
                        currentState.copy(
                            isRefreshing = false,
                            mealsByDate = mealsByDate,
                            emptyStateVisible = mealsByDate.isEmpty(),
                            error = null
                        )
                    }
                    is Result.Error -> currentState.copy(
                        isRefreshing = false,
                        error = result.exception.message ?: "An unknown error occurred"
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}
