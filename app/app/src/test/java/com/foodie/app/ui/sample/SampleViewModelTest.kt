package com.foodie.app.ui.sample

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Unit tests for SampleViewModel.
 *
 * Uses Mockito to mock MealRepository and verify ViewModel behavior.
 * Uses Truth assertions for readable test code.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SampleViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mealRepository: MealRepository
    private lateinit var viewModel: SampleViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mealRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() {
        // Given/When
        viewModel = SampleViewModel(mealRepository)

        // Then
        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.meals).isEmpty()
        assertThat(state.error).isNull()
    }

    @Test
    fun `loadSampleData should update state to loading then success when repository returns data`() = runTest {
        // Given
        val mockMeals = listOf(
            MealEntry("id1", Instant.now(), "Meal 1", 450),
            MealEntry("id2", Instant.now(), "Meal 2", 250)
        )
        val mockFlow = flow {
            emit(Result.Loading)
            emit(Result.Success(mockMeals))
        }
        whenever(mealRepository.getMealHistory()).thenReturn(mockFlow)
        viewModel = SampleViewModel(mealRepository)

        // When
        viewModel.loadSampleData()

        // Then
        val finalState = viewModel.state.value
        assertThat(finalState.isLoading).isFalse()
        assertThat(finalState.meals).hasSize(2)
        assertThat(finalState.meals[0].id).isEqualTo("id1")
        assertThat(finalState.meals[1].id).isEqualTo("id2")
        assertThat(finalState.error).isNull()
    }

    @Test
    fun `loadSampleData should update state to loading then error when repository returns error`() = runTest {
        // Given
        val errorMessage = "Failed to load meals"
        val mockFlow = flow {
            emit(Result.Loading)
            emit(Result.Error(RuntimeException("Test error"), errorMessage))
        }
        whenever(mealRepository.getMealHistory()).thenReturn(mockFlow)
        viewModel = SampleViewModel(mealRepository)

        // When
        viewModel.loadSampleData()

        // Then
        val finalState = viewModel.state.value
        assertThat(finalState.isLoading).isFalse()
        assertThat(finalState.meals).isEmpty()
        assertThat(finalState.error).isEqualTo(errorMessage)
    }

    @Test
    fun `loadSampleData should set loading state while repository is loading`() = runTest {
        // Given
        val mockFlow = flow<Result<List<MealEntry>>> {
            emit(Result.Loading)
            // Never emit success or error - stay in loading
        }
        whenever(mealRepository.getMealHistory()).thenReturn(mockFlow)
        viewModel = SampleViewModel(mealRepository)

        // When
        viewModel.loadSampleData()

        // Then
        val state = viewModel.state.value
        assertThat(state.isLoading).isTrue()
        assertThat(state.meals).isEmpty()
        assertThat(state.error).isNull()
    }

    @Test
    fun `clearError should set error to null`() = runTest {
        // Given
        val mockFlow = flow {
            emit(Result.Loading)
            emit(Result.Error(RuntimeException("Test error"), "Error message"))
        }
        whenever(mealRepository.getMealHistory()).thenReturn(mockFlow)
        viewModel = SampleViewModel(mealRepository)
        viewModel.loadSampleData()

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.state.value
        assertThat(state.error).isNull()
    }

    @Test
    fun `clearError should not modify other state values`() = runTest {
        // Given
        val mockMeals = listOf(MealEntry("id1", Instant.now(), "Meal 1", 450))
        val mockFlow = flow {
            emit(Result.Loading)
            emit(Result.Success(mockMeals))
        }
        whenever(mealRepository.getMealHistory()).thenReturn(mockFlow)
        viewModel = SampleViewModel(mealRepository)
        viewModel.loadSampleData()

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.state.value
        assertThat(state.meals).hasSize(1)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `multiple loadSampleData calls should update state correctly`() = runTest {
        // Given
        val firstMeals = listOf(MealEntry("id1", Instant.now(), "Meal 1", 450))
        val secondMeals = listOf(
            MealEntry("id2", Instant.now(), "Meal 2", 250),
            MealEntry("id3", Instant.now(), "Meal 3", 350)
        )
        whenever(mealRepository.getMealHistory())
            .thenReturn(flow {
                emit(Result.Loading)
                emit(Result.Success(firstMeals))
            })
            .thenReturn(flow {
                emit(Result.Loading)
                emit(Result.Success(secondMeals))
            })
        viewModel = SampleViewModel(mealRepository)

        // When
        viewModel.loadSampleData()
        val stateAfterFirst = viewModel.state.value

        viewModel.loadSampleData()
        val stateAfterSecond = viewModel.state.value

        // Then
        assertThat(stateAfterFirst.meals).hasSize(1)
        assertThat(stateAfterSecond.meals).hasSize(2)
    }
}
