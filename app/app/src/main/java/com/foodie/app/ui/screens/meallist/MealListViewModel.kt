package com.foodie.app.ui.screens.meallist

import androidx.lifecycle.viewModelScope
import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.ui.base.BaseViewModel
import com.foodie.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the Meal List screen.
 *
 * Manages meal list state and Health Connect test operations for Story 1.4.
 * Future stories will add full meal list loading and management.
 */
@HiltViewModel
class MealListViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : BaseViewModel() {
    
    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()
    
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
