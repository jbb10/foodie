package com.foodie.app.domain.usecase

import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for GetMealHistoryUseCase.
 *
 * Tests verify that the use case correctly delegates to the repository
 * and passes through the Flow of Results without transformation.
 */
class GetMealHistoryUseCaseTest {

    private lateinit var healthConnectRepository: HealthConnectRepository
    private lateinit var getMealHistoryUseCase: GetMealHistoryUseCase

    @Before
    fun setup() {
        healthConnectRepository = mock()
        getMealHistoryUseCase = GetMealHistoryUseCase(healthConnectRepository)
    }

    @Test
    fun `invoke delegates to repository getMealHistory`() = runTest {
        // Given
        val testMeals = listOf(
            MealEntry("1", Instant.now(), "Breakfast", 500),
        )
        whenever(healthConnectRepository.getMealHistory())
            .thenReturn(flowOf(Result.Success(testMeals)))

        // When
        val result = getMealHistoryUseCase().toList()

        // Then
        verify(healthConnectRepository).getMealHistory()
        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(Result.Success::class.java)
        assertThat((result[0] as Result.Success).data).isEqualTo(testMeals)
    }

    @Test
    fun `invoke passes through repository errors`() = runTest {
        // Given
        val errorMessage = "Health Connect error"
        val error = Result.Error(Exception(), errorMessage)
        whenever(healthConnectRepository.getMealHistory())
            .thenReturn(flowOf(error))

        // When
        val result = getMealHistoryUseCase().toList()

        // Then
        verify(healthConnectRepository).getMealHistory()
        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(Result.Error::class.java)
        assertThat((result[0] as Result.Error).message).isEqualTo(errorMessage)
    }

    @Test
    fun `invoke passes through loading state`() = runTest {
        // Given
        val testMeals = listOf(
            MealEntry("1", Instant.now(), "Lunch", 600),
        )
        whenever(healthConnectRepository.getMealHistory())
            .thenReturn(flowOf(Result.Loading, Result.Success(testMeals)))

        // When
        val result = getMealHistoryUseCase().toList()

        // Then
        verify(healthConnectRepository).getMealHistory()
        assertThat(result).hasSize(2)
        assertThat(result[0]).isInstanceOf(Result.Loading::class.java)
        assertThat(result[1]).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke returns empty list when repository returns empty`() = runTest {
        // Given
        whenever(healthConnectRepository.getMealHistory())
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        val result = getMealHistoryUseCase().toList()

        // Then
        verify(healthConnectRepository).getMealHistory()
        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(Result.Success::class.java)
        assertThat((result[0] as Result.Success).data).isEmpty()
    }
}
