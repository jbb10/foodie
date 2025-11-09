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
}
