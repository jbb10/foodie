package com.foodie.app.domain.usecase

import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UpdateMealEntryUseCaseTest {

    private lateinit var repository: HealthConnectRepository
    private lateinit var useCase: UpdateMealEntryUseCase

    private val testRecordId = "test-record-123"
    private val testTimestamp = Instant.parse("2025-11-12T14:30:00Z")

    @Before
    fun setup() {
        repository = mock()
        useCase = UpdateMealEntryUseCase(repository)
    }

    @Test
    fun `invoke with valid input should delegate to repository`() = runTest {
        // Given
        val calories = 500
        val description = "Grilled chicken salad"
        whenever(repository.updateNutritionRecord(any(), any(), any(), any()))
            .thenReturn(Result.Success(Unit))

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        verify(repository).updateNutritionRecord(
            recordId = testRecordId,
            calories = calories,
            description = description,
            timestamp = testTimestamp,
        )
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke with calories below minimum should return error`() = runTest {
        // Given
        val calories = 0
        val description = "Valid description"

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).contains("Calories must be between 1 and 5000")
    }

    @Test
    fun `invoke with calories above maximum should return error`() = runTest {
        // Given
        val calories = 5001
        val description = "Valid description"

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).contains("Calories must be between 1 and 5000")
    }

    @Test
    fun `invoke with blank description should return error`() = runTest {
        // Given
        val calories = 500
        val description = "   "

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).contains("Description cannot be blank")
    }

    @Test
    fun `invoke with empty description should return error`() = runTest {
        // Given
        val calories = 500
        val description = ""

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).contains("Description cannot be blank")
    }

    @Test
    fun `invoke with description exceeding 200 characters should return error`() = runTest {
        // Given
        val calories = 500
        val description = "a".repeat(201)

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).contains("Description cannot exceed 200 characters")
    }

    @Test
    fun `invoke with exactly 200 character description should succeed`() = runTest {
        // Given
        val calories = 500
        val description = "a".repeat(200)
        whenever(repository.updateNutritionRecord(any(), any(), any(), any()))
            .thenReturn(Result.Success(Unit))

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        verify(repository).updateNutritionRecord(
            recordId = testRecordId,
            calories = calories,
            description = description,
            timestamp = testTimestamp,
        )
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke with calories at minimum boundary should succeed`() = runTest {
        // Given
        val calories = 1
        val description = "Valid description"
        whenever(repository.updateNutritionRecord(any(), any(), any(), any()))
            .thenReturn(Result.Success(Unit))

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        verify(repository).updateNutritionRecord(
            recordId = testRecordId,
            calories = calories,
            description = description,
            timestamp = testTimestamp,
        )
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke with calories at maximum boundary should succeed`() = runTest {
        // Given
        val calories = 5000
        val description = "Valid description"
        whenever(repository.updateNutritionRecord(any(), any(), any(), any()))
            .thenReturn(Result.Success(Unit))

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        verify(repository).updateNutritionRecord(
            recordId = testRecordId,
            calories = calories,
            description = description,
            timestamp = testTimestamp,
        )
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke should propagate repository error`() = runTest {
        // Given
        val calories = 500
        val description = "Valid description"
        val repositoryError = RuntimeException("Health Connect unavailable")
        whenever(repository.updateNutritionRecord(any(), any(), any(), any()))
            .thenReturn(Result.Error(repositoryError))

        // When
        val result = useCase(testRecordId, calories, description, testTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = (result as Result.Error).exception
        assertThat(error).isEqualTo(repositoryError)
        assertThat(error.message).isEqualTo("Health Connect unavailable")
    }
}
