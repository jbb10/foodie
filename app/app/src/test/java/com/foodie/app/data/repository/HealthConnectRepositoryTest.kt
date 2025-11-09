package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

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
        
        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any()))
            .thenThrow(SecurityException("Permissions not granted"))
        
        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(SecurityException::class.java)
        assertThat(error.message).contains("permissions")
    }
    
    @Test
    fun `insertNutritionRecord returns Error when IllegalStateException thrown`() = runTest {
        // Given
        val calories = 500
        val description = "Test meal"
        val timestamp = Instant.now()
        
        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any()))
            .thenThrow(IllegalStateException("Health Connect not available"))
        
        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).contains("not available")
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
        assertThat((result as Result.Error).message).contains("permissions")
    }
    
    @Test
    fun `insertNutritionRecord returns Error when generic Exception thrown`() = runTest {
        // Given
        val calories = 400
        val description = "Test"
        val timestamp = Instant.now()
        
        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any()))
            .thenThrow(RuntimeException("Network timeout"))
        
        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.exception).isInstanceOf(RuntimeException::class.java)
        assertThat(error.message).contains("timeout")
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
        assertThat(error.message).contains("Invalid")
    }
    
    @Test
    fun `insertNutritionRecord with invalid data returns Error`() = runTest {
        // Given - negative calories
        val calories = -100
        val description = "Invalid meal"
        val timestamp = Instant.now()
        
        whenever(healthConnectManager.insertNutritionRecord(any(), any(), any()))
            .thenThrow(IllegalArgumentException("Calories must be positive"))
        
        // When
        val result = repository.insertNutritionRecord(calories, description, timestamp)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("positive")
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
}
