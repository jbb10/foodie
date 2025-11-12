package com.foodie.app.ui.screens.meallist

import com.foodie.app.domain.model.MealEntry
import com.foodie.app.domain.usecase.GetMealHistoryUseCase
import com.foodie.app.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MealListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getMealHistoryUseCase: GetMealHistoryUseCase = mockk()
    private lateinit var viewModel: MealListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMeals success updates state with grouped meals`() = runTest {
        // Given
        val mealList = listOf(
            MealEntry("1", "Breakfast", 300.0, Instant.now()),
            MealEntry("2", "Lunch", 500.0, Instant.now().minusSeconds(86400))
        )
        coEvery { getMealHistoryUseCase() } returns flowOf(Result.Success(mealList))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.emptyStateVisible)
        assertEquals(2, state.mealsByDate.size)
        assertEquals(1, state.mealsByDate[LocalDate.now()]?.size)
    }

    @Test
    fun `loadMeals with empty list shows empty state`() = runTest {
        // Given
        coEvery { getMealHistoryUseCase() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.emptyStateVisible)
        assertTrue(state.mealsByDate.isEmpty())
    }

    @Test
    fun `loadMeals error updates state with error message`() = runTest {
        // Given
        val errorMessage = "Failed to load meals"
        coEvery { getMealHistoryUseCase() } returns flowOf(Result.Error(RuntimeException(errorMessage)))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }
}
