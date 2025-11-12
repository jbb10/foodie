package com.foodie.app.domain.usecase

import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for DeleteMealEntryUseCase.
 *
 * Validates that the use case correctly delegates to the repository
 * and handles success and error scenarios appropriately.
 */
class DeleteMealEntryUseCaseTest {

    private lateinit var mealRepository: MealRepository
    private lateinit var deleteMealEntryUseCase: DeleteMealEntryUseCase

    @Before
    fun setUp() {
        mealRepository = mockk()
        deleteMealEntryUseCase = DeleteMealEntryUseCase(mealRepository)
    }

    @Test
    fun `invoke should delegate to repository deleteMeal`() = runTest {
        // Given
        val mealId = "meal-123"
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.Success(Unit)

        // When
        deleteMealEntryUseCase(mealId)

        // Then
        coVerify(exactly = 1) { mealRepository.deleteMeal(mealId) }
    }

    @Test
    fun `invoke should return Success when repository returns Success`() = runTest {
        // Given
        val mealId = "meal-123"
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.Success(Unit)

        // When
        val result = deleteMealEntryUseCase(mealId)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke should return Error when repository returns Error`() = runTest {
        // Given
        val mealId = "meal-123"
        val errorMessage = "Failed to delete meal"
        val exception = IOException("Network error")
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.Error(exception, errorMessage)

        // When
        val result = deleteMealEntryUseCase(mealId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).isEqualTo(errorMessage)
        assertThat(error.exception).isEqualTo(exception)
    }

    @Test
    fun `invoke should handle SecurityException from repository`() = runTest {
        // Given
        val mealId = "meal-123"
        val exception = SecurityException("Permission denied")
        val message = "Health Connect permissions not granted. Please grant permissions in Settings."
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.Error(exception, message)

        // When
        val result = deleteMealEntryUseCase(mealId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(SecurityException::class.java)
        assertThat(error.message).contains("permissions")
    }

    @Test
    fun `invoke should handle IllegalStateException from repository`() = runTest {
        // Given
        val mealId = "meal-123"
        val exception = IllegalStateException("Health Connect unavailable")
        val message = "Health Connect is not available on this device."
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.Error(exception, message)

        // When
        val result = deleteMealEntryUseCase(mealId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).contains("Health Connect")
    }
}
