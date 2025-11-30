package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
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

/**
 * Unit tests for HealthConnectRepository.
 *
 * Tests verify CRUD operations and error handling with mocked HealthConnectManager.
 */
class HealthConnectRepositoryTest {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var repository: HealthConnectRepository

    @Before
    fun setup() {
        healthConnectManager = mock()
        repository = HealthConnectRepository(healthConnectManager)
    }

    @Test
    fun `insertNutritionRecord returns Success with record ID when operation succeeds`() = runTest {
        // Given
        val calories = 650
        val description = "Grilled chicken"
        val timestamp = Instant.now()
        val expectedRecordId = "record-123"

        whenever(healthConnectManager.insertNutritionRecord(calories, description, timestamp))
            .thenReturn(expectedRecordId)

        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(expectedRecordId)
        verify(healthConnectManager).insertNutritionRecord(calories, description, timestamp)
    }

    @Test
    fun `insertNutritionRecord returns Error when SecurityException thrown`() = runTest {
        // Given
        val calories = 500
        val description = "Test meal"
        val timestamp = Instant.now()

        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any(), any(), any(), any()))
            .thenThrow(SecurityException("Permissions not granted"))

        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(SecurityException::class.java)
        assertThat(error.message).isEqualTo("Permission denied. Please grant Health Connect access in settings.")
    }

    @Test
    fun `insertNutritionRecord returns Error when IllegalStateException thrown`() = runTest {
        // Given
        val calories = 500
        val description = "Test meal"
        val timestamp = Instant.now()

        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any(), any(), any(), any()))
            .thenThrow(IllegalStateException("Health Connect not available"))

        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).isEqualTo("Health Connect is not available. Please install it from the Play Store.")
    }

    @Test
    fun `deleteNutritionRecord returns Success when operation succeeds`() = runTest {
        // Given
        val recordId = "record-456"

        // When
        val result = repository.deleteNutritionRecord(recordId)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(healthConnectManager).deleteNutritionRecord(recordId)
    }

    @Test
    fun `deleteNutritionRecord returns Error when SecurityException thrown`() = runTest {
        // Given
        val recordId = "record-789"

        whenever(healthConnectManager.deleteNutritionRecord(any()))
            .thenThrow(SecurityException("Permissions not granted"))

        // When
        val result = repository.deleteNutritionRecord(recordId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).isEqualTo("Permission denied. Please grant Health Connect access in settings.")
    }

    @Test
    fun `insertNutritionRecord returns Error when generic Exception thrown`() = runTest {
        // Given
        val calories = 400
        val description = "Test"
        val timestamp = Instant.now()

        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network timeout"))

        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(RuntimeException::class.java)
        assertThat(error.message).isEqualTo("An unexpected error occurred. Please try again.")
    }

    @Test
    fun `deleteNutritionRecord returns Error when IllegalArgumentException thrown`() = runTest {
        // Given
        val recordId = "invalid-id"

        whenever(healthConnectManager.deleteNutritionRecord(any()))
            .thenThrow(IllegalArgumentException("Invalid record ID"))

        // When
        val result = repository.deleteNutritionRecord(recordId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).isEqualTo("Invalid input. Please check your data and try again.")
    }

    @Test
    fun `insertNutritionRecord with invalid data returns Error`() = runTest {
        // Given - negative calories
        val calories = -100
        val description = "Invalid meal"
        val timestamp = Instant.now()

        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any(), any(), any(), any()))
            .thenThrow(IllegalArgumentException("Calories must be positive"))

        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).isEqualTo("Invalid input. Please check your data and try again.")
    }

    @Test
    fun `deleteNutritionRecord returns Error when Health Connect unavailable`() = runTest {
        // Given
        val recordId = "record-999"

        whenever(healthConnectManager.deleteNutritionRecord(any()))
            .thenThrow(IllegalStateException("Health Connect SDK not initialized"))

        // When
        val result = repository.deleteNutritionRecord(recordId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `updateNutritionRecord returns Success when operation succeeds`() = runTest {
        // Given
        val recordId = "record-123"
        val calories = 750
        val description = "Updated grilled chicken"
        val timestamp = Instant.parse("2025-11-12T14:30:00Z")

        // When
        val result = repository.updateNutritionRecord(recordId, calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(healthConnectManager).updateNutritionRecord(recordId, calories, description, timestamp)
    }

    @Test
    fun `updateNutritionRecord returns Error when SecurityException thrown`() = runTest {
        // Given
        val recordId = "record-456"
        val calories = 600
        val description = "Test update"
        val timestamp = Instant.now()

        whenever(healthConnectManager.updateNutritionRecord(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(SecurityException("Permissions not granted"))

        // When
        val result = repository.updateNutritionRecord(recordId, calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(SecurityException::class.java)
        assertThat(error.message).isEqualTo("Permission denied. Please grant Health Connect access in settings.")
    }

    @Test
    fun `updateNutritionRecord returns Error when IllegalStateException thrown`() = runTest {
        // Given
        val recordId = "record-789"
        val calories = 500
        val description = "Test meal"
        val timestamp = Instant.now()

        whenever(healthConnectManager.updateNutritionRecord(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(IllegalStateException("Health Connect not available"))

        // When
        val result = repository.updateNutritionRecord(recordId, calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).isEqualTo("Health Connect is not available. Please install it from the Play Store.")
    }

    @Test
    fun `updateNutritionRecord preserves original timestamp`() = runTest {
        // Given - timestamp from 2 days ago
        val recordId = "record-old"
        val calories = 800
        val description = "Updated description"
        val originalTimestamp = Instant.now().minusSeconds(172800) // 2 days ago

        // When
        val result = repository.updateNutritionRecord(recordId, calories, description, originalTimestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(healthConnectManager).updateNutritionRecord(
            recordId = recordId,
            calories = calories,
            description = description,
            timestamp = originalTimestamp,
        )
    }

    @Test
    fun `updateNutritionRecord returns Error when generic Exception thrown`() = runTest {
        // Given
        val recordId = "record-error"
        val calories = 450
        val description = "Test"
        val timestamp = Instant.now()

        whenever(healthConnectManager.updateNutritionRecord(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))

        // When
        val result = repository.updateNutritionRecord(recordId, calories, description, timestamp)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(RuntimeException::class.java)
        assertThat(error.message).isEqualTo("An unexpected error occurred. Please try again.")
    }
}
