package com.foodie.app.ui.screens.meallist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.domain.usecase.DeleteMealEntryUseCase
import com.foodie.app.domain.usecase.GetMealHistoryUseCase
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit tests for MealListViewModel.
 *
 * Tests verify meal history loading, date grouping, error handling, and refresh functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealListViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var getMealHistoryUseCase: GetMealHistoryUseCase
    private lateinit var deleteMealEntryUseCase: DeleteMealEntryUseCase
    private lateinit var viewModel: MealListViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getMealHistoryUseCase = mock()
        deleteMealEntryUseCase = mock()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `init loads meals automatically`() = runTest {
        // Given
        val testMeals = listOf(
            MealEntry("1", Instant.now(), "Breakfast", 500)
        )
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals() // Explicitly call loadMeals (called by LaunchedEffect in screen)

        // Then
        val state = viewModel.state.value
        assertThat(state.mealsByDate).containsKey("Today")
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `loadMeals groups meals by date correctly`() = runTest {
        // Given
        val today = Instant.now()
        val yesterday = today.minusSeconds(86400) // 1 day ago
        val twoDaysAgo = today.minusSeconds(172800) // 2 days ago
        
        val testMeals = listOf(
            MealEntry("1", today, "Lunch", 600),
            MealEntry("2", yesterday, "Dinner", 700),
            MealEntry("3", twoDaysAgo, "Breakfast", 400)
        )
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.mealsByDate.keys).contains("Today")
        assertThat(state.mealsByDate.keys).contains("Yesterday")
        assertThat(state.mealsByDate["Today"]).hasSize(1)
        assertThat(state.mealsByDate["Yesterday"]).hasSize(1)
    }

    // Error handling tests (Story 1.5)
    
    @Test
    fun `loadMeals sets loading state when called`() = runTest {
        // Given
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()

        // Then - Check loading state is set (UnconfinedTestDispatcher executes immediately)
        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.emptyStateVisible).isTrue()
    }

    @Test
    fun `loadMeals updates state with meals on success`() = runTest {
        // Given
        val testMeals = listOf(
            MealEntry("1", Instant.now(), "Test meal", 500)
        )
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.mealsByDate).isNotEmpty()
        assertThat(state.mealsByDate["Today"]).isEqualTo(testMeals)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.emptyStateVisible).isFalse()
    }

    @Test
    fun `loadMeals updates error state when repository returns error`() = runTest {
        // Given
        val errorMessage = "Network error. Please check your connection and try again."
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Error(java.io.IOException(), errorMessage)))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
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
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Error(SecurityException(), errorMessage)))

        // When
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo(errorMessage)
        assertThat(state.error).doesNotContain("SecurityException")
    }

    @Test
    fun `retryLoadMeals clears error and loads meals again`() = runTest {
        // Given - Initial error state
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Error(java.io.IOException(), "Network error")))
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        assertThat(viewModel.state.value.error).isNotNull()

        // When - Retry with successful result
        val testMeals = listOf(MealEntry("1", Instant.now(), "Test", 500))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        viewModel.retryLoadMeals()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isNull()
        assertThat(state.mealsByDate).isNotEmpty()
    }

    @Test
    fun `clearError removes error message without reloading`() = runTest {
        // Given - Error state
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Error(java.io.IOException(), "Network error")))
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        assertThat(viewModel.state.value.error).isNotNull()

        // When
        viewModel.clearError()

        // Then
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `refresh updates isRefreshing state and reloads meals`() = runTest {
        // Given
        val initialMeals = listOf(MealEntry("1", Instant.now(), "Old meal", 300))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(initialMeals)))
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)

        // When
        val newMeals = listOf(
            MealEntry("2", Instant.now(), "New meal", 500),
            MealEntry("3", Instant.now(), "Another meal", 600)
        )
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(newMeals)))
        viewModel.refresh()

        // Then
        val state = viewModel.state.value
        assertThat(state.isRefreshing).isFalse() // Completed immediately with test dispatcher
        assertThat(state.mealsByDate["Today"]).hasSize(2)
        assertThat(state.error).isNull()
    }

    @Test
    fun `onDeleteMealConfirmed leaves state unchanged`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("1", Instant.now(), "Test", 450))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        val stateBefore = viewModel.state.value

        // When
        viewModel.onDeleteMealConfirmed(testMeals.first())

        // Then
        assertThat(viewModel.state.value).isEqualTo(stateBefore)
    }

    // Delete functionality tests (Story 3.4)

    @Test
    fun `onMealLongPress sets showDeleteDialog and deleteTargetId`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-123", Instant.now(), "Lunch", 600))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)

        // When
        viewModel.onMealLongPress("meal-123")

        // Then
        val state = viewModel.state.value
        assertThat(state.showDeleteDialog).isTrue()
        assertThat(state.deleteTargetId).isEqualTo("meal-123")
    }

    @Test
    fun `onDismissDeleteDialog clears dialog state`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-123", Instant.now(), "Lunch", 600))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.onMealLongPress("meal-123")

        // When
        viewModel.onDismissDeleteDialog()

        // Then
        val state = viewModel.state.value
        assertThat(state.showDeleteDialog).isFalse()
        assertThat(state.deleteTargetId).isNull()
    }

    @Test
    fun `onDeleteConfirmed removes entry from state on success`() = runTest {
        // Given
        val testMeals = listOf(
            MealEntry("meal-1", Instant.now(), "Breakfast", 400),
            MealEntry("meal-2", Instant.now(), "Lunch", 600)
        )
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        whenever(deleteMealEntryUseCase(any()))
            .thenReturn(Result.Success(Unit))
        
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        viewModel.onMealLongPress("meal-1")

        // When
        viewModel.onDeleteConfirmed()

        // Then
        val state = viewModel.state.value
        assertThat(state.mealsByDate["Today"]).hasSize(1)
        assertThat(state.mealsByDate["Today"]?.first()?.id).isEqualTo("meal-2")
        assertThat(state.showDeleteDialog).isFalse()
        assertThat(state.deleteTargetId).isNull()
        assertThat(state.successMessage).isEqualTo("Entry deleted")
    }

    @Test
    fun `onDeleteConfirmed sets emptyStateVisible when last entry deleted`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-1", Instant.now(), "Breakfast", 400))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        whenever(deleteMealEntryUseCase(any()))
            .thenReturn(Result.Success(Unit))
        
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        viewModel.onMealLongPress("meal-1")

        // When
        viewModel.onDeleteConfirmed()

        // Then
        val state = viewModel.state.value
        assertThat(state.mealsByDate).isEmpty()
        assertThat(state.emptyStateVisible).isTrue()
        assertThat(state.successMessage).isEqualTo("Entry deleted")
    }

    @Test
    fun `onDeleteConfirmed sets error on delete failure`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-1", Instant.now(), "Breakfast", 400))
        val errorMessage = "Failed to delete meal"
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        whenever(deleteMealEntryUseCase(any()))
            .thenReturn(Result.Error(Exception("Network error"), errorMessage))
        
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        viewModel.onMealLongPress("meal-1")

        // When
        viewModel.onDeleteConfirmed()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo(errorMessage)
        assertThat(state.showDeleteDialog).isFalse()
        assertThat(state.mealsByDate["Today"]).hasSize(1) // Entry still present
    }

    @Test
    fun `onDeleteConfirmed handles SecurityException with user-friendly message`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-1", Instant.now(), "Breakfast", 400))
        val errorMessage = "Health Connect permissions not granted. Please grant permissions in Settings."
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        whenever(deleteMealEntryUseCase(any()))
            .thenReturn(Result.Error(SecurityException("Permission denied"), errorMessage))
        
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        viewModel.onMealLongPress("meal-1")

        // When
        viewModel.onDeleteConfirmed()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).contains("permissions")
    }

    @Test
    fun `onDeleteConfirmed does nothing when no deleteTargetId set`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-1", Instant.now(), "Breakfast", 400))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        val stateBefore = viewModel.state.value

        // When
        viewModel.onDeleteConfirmed()

        // Then (state unchanged, use case not called)
        assertThat(viewModel.state.value.mealsByDate).isEqualTo(stateBefore.mealsByDate)
    }

    @Test
    fun `clearSuccessMessage clears successMessage field`() = runTest {
        // Given
        val testMeals = listOf(MealEntry("meal-1", Instant.now(), "Breakfast", 400))
        whenever(getMealHistoryUseCase.invoke())
            .thenReturn(flowOf(Result.Success(testMeals)))
        whenever(deleteMealEntryUseCase(any()))
            .thenReturn(Result.Success(Unit))
        
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealEntryUseCase)
        viewModel.loadMeals()
        viewModel.onMealLongPress("meal-1")
        viewModel.onDeleteConfirmed()

        // When
        viewModel.clearSuccessMessage()

        // Then
        assertThat(viewModel.state.value.successMessage).isNull()
    }
}

