package com.foodie.app.ui.screens.meallist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Unit tests for MealListViewModel.
 *
 * Tests verify Health Connect test operation and result handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealListViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var healthConnectRepository: HealthConnectRepository
    private lateinit var viewModel: MealListViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        healthConnectRepository = mock()
        viewModel = MealListViewModel(healthConnectRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `testHealthConnect should show success message when round-trip succeeds`() = runTest {
        // Given
        val recordId = "test-record-123"
        val calories = 500
        val description = "Test meal"
        
        whenever(healthConnectRepository.insertNutritionRecord(any(), any(), any()))
            .thenReturn(Result.Success(recordId))
        
        val mealEntry = MealEntry(
            id = recordId,
            timestamp = Instant.now(),
            description = description,
            calories = calories
        )
        
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Success(listOf(mealEntry)))
        
        // When
        viewModel.testHealthConnect()
        
        // Then
        val result = viewModel.testResult.value
        assertThat(result).isNotNull()
        assertThat(result).contains("Test successful")
        assertThat(result).contains("500 cal")
    }
    
    @Test
    fun `testHealthConnect should show error message when insert fails`() = runTest {
        // Given
        val errorMessage = "Health Connect permissions required"
        
        whenever(healthConnectRepository.insertNutritionRecord(any(), any(), any()))
            .thenReturn(Result.Error(SecurityException(), errorMessage))
        
        // When
        viewModel.testHealthConnect()
        
        // Then
        val result = viewModel.testResult.value
        assertThat(result).isNotNull()
        assertThat(result).contains("Test failed")
        assertThat(result).contains(errorMessage)
    }
    
    @Test
    fun `testHealthConnect should show error message when query fails`() = runTest {
        // Given
        val recordId = "test-record-456"
        
        whenever(healthConnectRepository.insertNutritionRecord(any(), any(), any()))
            .thenReturn(Result.Success(recordId))
        
        val errorMessage = "Failed to load meal entries"
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Error(Exception(), errorMessage))
        
        // When
        viewModel.testHealthConnect()
        
        // Then
        val result = viewModel.testResult.value
        assertThat(result).isNotNull()
        assertThat(result).contains("Test failed")
        assertThat(result).contains(errorMessage)
    }
    
    @Test
    fun `testHealthConnect should show error when record not found after insert`() = runTest {
        // Given
        val recordId = "test-record-789"
        
        whenever(healthConnectRepository.insertNutritionRecord(any(), any(), any()))
            .thenReturn(Result.Success(recordId))
        
        // Return empty list (record not found)
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Success(emptyList()))
        
        // When
        viewModel.testHealthConnect()
        
        // Then
        val result = viewModel.testResult.value
        assertThat(result).isNotNull()
        assertThat(result).contains("Test failed")
        assertThat(result).contains("record not found")
    }
    
    @Test
    fun `clearTestResult should set testResult to null`() = runTest {
        // Given - Set a test result
        val recordId = "test-record"
        whenever(healthConnectRepository.insertNutritionRecord(any(), any(), any()))
            .thenReturn(Result.Success(recordId))
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Success(listOf(
                MealEntry(recordId, Instant.now(), "Test", 500)
            )))
        viewModel.testHealthConnect()
        assertThat(viewModel.testResult.value).isNotNull()
        
        // When
        viewModel.clearTestResult()
        
        // Then
        assertThat(viewModel.testResult.value).isNull()
    }

    // Error handling tests (Story 1.5)
    
    @Test
    fun `loadMeals sets loading state when called`() = runTest {
        // Given
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Success(emptyList()))

        // When
        viewModel.loadMeals()

        // Then - Check loading state is set
        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse() // UnconfinedTestDispatcher executes immediately
        assertThat(state.error).isNull()
    }

    @Test
    fun `loadMeals updates state with meals on success`() = runTest {
        // Given
        val testMeals = listOf(
            MealEntry("1", Instant.now(), "Test meal", 500)
        )
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Success(testMeals))

        // When
        viewModel.loadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.meals).isEqualTo(testMeals)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `loadMeals updates error state when repository returns error`() = runTest {
        // Given
        val errorMessage = "Network error. Please check your connection and try again."
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Error(java.io.IOException(), errorMessage))

        // When
        viewModel.loadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo(errorMessage)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `loadMeals updates error state with user-friendly message on SecurityException`() = runTest {
        // Given
        val errorMessage = "Permission denied. Please grant Health Connect access in settings."
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Error(SecurityException(), errorMessage))

        // When
        viewModel.loadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo(errorMessage)
        assertThat(state.error).doesNotContain("SecurityException")
    }

    @Test
    fun `retryLoadMeals clears error and loads meals again`() = runTest {
        // Given - Initial error state
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Error(java.io.IOException(), "Network error"))
        viewModel.loadMeals()
        assertThat(viewModel.state.value.error).isNotNull()

        // When - Retry with successful result
        val testMeals = listOf(MealEntry("1", Instant.now(), "Test", 500))
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Success(testMeals))
        viewModel.retryLoadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isNull()
        assertThat(state.meals).isEqualTo(testMeals)
    }

    @Test
    fun `clearError removes error message without reloading`() = runTest {
        // Given - Error state
        whenever(healthConnectRepository.queryNutritionRecords(any(), any()))
            .thenReturn(Result.Error(java.io.IOException(), "Network error"))
        viewModel.loadMeals()
        assertThat(viewModel.state.value.error).isNotNull()

        // When
        viewModel.clearError()

        // Then
        assertThat(viewModel.state.value.error).isNull()
    }
}
