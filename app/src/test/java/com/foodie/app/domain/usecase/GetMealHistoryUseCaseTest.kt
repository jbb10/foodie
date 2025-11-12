package com.foodie.app.domain.usecase

import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GetMealHistoryUseCaseTest {

    private val repository: HealthConnectRepository = mockk()
    private val useCase = GetMealHistoryUseCase(repository)

    @Test
    fun `invoke returns success with data from repository`() = runBlocking {
        // Given
        val mealList = listOf(MealEntry("1", "Test Meal", 100.0, Instant.now()))
        coEvery { repository.getMealHistory() } returns flowOf(Result.Success(mealList))

        // When
        val result = useCase.invoke().first()

        // Then
        assertEquals(Result.Success(mealList), result)
    }

    @Test
    fun `invoke returns error from repository`() = runBlocking {
        // Given
        val exception = RuntimeException("Test Error")
        coEvery { repository.getMealHistory() } returns flowOf(Result.Error(exception))

        // When
        val result = useCase.invoke().first()

        // Then
        assertEquals(Result.Error(exception), result)
    }
}
