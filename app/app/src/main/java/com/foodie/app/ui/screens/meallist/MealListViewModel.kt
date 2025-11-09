package com.foodie.app.ui.screens.meallist

import androidx.lifecycle.viewModelScope
import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.ui.base.BaseViewModel
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
 * ViewModel for the Meal List screen.
 *
 * Manages meal list state with proper error handling using MealListState.
 * Demonstrates the error handling pattern: Result<T> → UI state with error field.
 */
@HiltViewModel
class MealListViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : BaseViewModel() {
    
    private val _state = MutableStateFlow(MealListState())
    val state: StateFlow<MealListState> = _state.asStateFlow()
    
    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()
    
    /**
     * Loads meal entries from Health Connect for the last 30 days.
     *
     * Demonstrates error state handling pattern:
     * - Loading: isLoading = true, error = null
     * - Success: meals populated, isLoading = false, error = null
     * - Error: isLoading = false, error = user-friendly message
     */
    fun loadMeals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(30L * 24 * 60 * 60) // 30 days ago
            
            when (val result = healthConnectRepository.queryNutritionRecords(startTime, endTime)) {
                is Result.Success -> {
                    _state.update { 
                        it.copy(
                            meals = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                    Timber.i("Loaded ${result.data.size} meal entries")
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    Timber.e(result.exception, "Failed to load meals: ${result.message}")
                }
                is Result.Loading -> {
                    // Already handling loading state
                }
            }
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
     * Clears the current error message without reloading.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Tests Health Connect integration with a round-trip write + read operation.
     *
     * Inserts a test meal record, queries it back, and reports success/failure.
     * This validates the complete Health Connect integration stack.
     */
    fun testHealthConnect() {
        viewModelScope.launch {
            Timber.d("Starting Health Connect test")
            
            val calories = 500
            val description = "Test meal"
            val timestamp = Instant.now()
            
            // Insert test record
            when (val insertResult = healthConnectRepository.insertNutritionRecord(calories, description, timestamp)) {
                is Result.Success -> {
                    val recordId = insertResult.data
                    Timber.d("Test insert successful: $recordId")
                    
                    // Query records to validate round-trip
                    val startTime = timestamp.minusSeconds(3600)
                    val endTime = timestamp.plusSeconds(3600)
                    
                    when (val queryResult = healthConnectRepository.queryNutritionRecords(startTime, endTime)) {
                        is Result.Success -> {
                            val entries = queryResult.data
                            val testEntry = entries.find { it.id == recordId }
                            
                            if (testEntry != null) {
                                _testResult.value = "Test successful - $calories cal saved and retrieved"
                                Timber.i("Health Connect test PASSED: round-trip successful")
                            } else {
                                _testResult.value = "Test failed: record not found after insert"
                                Timber.e("Health Connect test FAILED: record not found")
                            }
                        }
                        is Result.Error -> {
                            _testResult.value = "Test failed: ${queryResult.message}"
                            Timber.e(queryResult.exception, "Health Connect test FAILED: query error")
                        }
                        is Result.Loading -> { /* Not expected */ }
                    }
                }
                is Result.Error -> {
                    _testResult.value = "Test failed: ${insertResult.message}"
                    Timber.e(insertResult.exception, "Health Connect test FAILED: insert error")
                }
                is Result.Loading -> { /* Not expected */ }
            }
        }
    }
    
    /**
     * Clears the test result message.
     */
    fun clearTestResult() {
        _testResult.value = null
    }
}
