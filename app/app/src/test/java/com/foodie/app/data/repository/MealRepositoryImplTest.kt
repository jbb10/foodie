package com.foodie.app.data.repository

import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.units.Energy
import com.foodie.app.data.local.healthconnect.HealthConnectDataSource
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for MealRepositoryImpl.
 *
 * NOTE: These tests are currently @Ignore'd because HealthConnectDataSourceImpl
 * is a placeholder (TODO) for Story 1.4. These tests will be enabled when
 * Health Connect integration is fully implemented in Story 1.4.
 *
 * The tests demonstrate the expected behavior and will validate the implementation
 * once the data source is complete.
 */
@Ignore("Health Connect data source implementation pending Story 1.4")
class MealRepositoryImplTest {

    private lateinit var healthConnectDataSource: HealthConnectDataSource
    private lateinit var repository: MealRepositoryImpl

    @Before
    fun setup() {
        healthConnectDataSource = mock()
        repository = MealRepositoryImpl(healthConnectDataSource)
    }

    @Test
    fun `getMealHistory should return Success with meal entries when Health Connect query succeeds`() = runTest {
        // Given
        val now = Instant.now()
        val mockRecords = listOf(
            createNutritionRecord("id1", now.minus(1, ChronoUnit.HOURS), 450, "Chicken salad"),
            createNutritionRecord("id2", now.minus(2, ChronoUnit.HOURS), 250, "Protein shake"),
            createNutritionRecord("id3", now.minus(3, ChronoUnit.HOURS), 350, "Oatmeal"),
        )
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenReturn(mockRecords)

        // When
        val result = repository.getMealHistory().first()

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val meals = (result as Result.Success).data
        assertThat(meals).hasSize(3)
        assertThat(meals[0].id).isEqualTo("id1")
        assertThat(meals[0].calories).isEqualTo(450)
        assertThat(meals[0].description).isEqualTo("Chicken salad")
        verify(healthConnectDataSource).queryNutritionRecords(any(), any())
    }

    @Test
    fun `getMealHistory should return Error when Health Connect throws SecurityException`() = runTest {
        // Given
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenThrow(SecurityException("Permission denied"))

        // When
        val result = repository.getMealHistory().first()

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("permissions not granted")
        assertThat(error.exception).isInstanceOf(SecurityException::class.java)
    }

    @Test
    fun `getMealHistory should return Error when Health Connect throws IllegalStateException`() = runTest {
        // Given
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenThrow(IllegalStateException("Health Connect not available"))

        // When
        val result = repository.getMealHistory().first()

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).contains("Health Connect is not available")
        assertThat(error.exception).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `getMealHistory should skip records with invalid calories`() = runTest {
        // Given
        val now = Instant.now()
        val mockRecords = listOf(
            createNutritionRecord("id1", now, 450, "Valid meal"),
            createNutritionRecord("id2", now, 0, "Invalid - zero calories"),
            createNutritionRecord("id3", now, 6000, "Invalid - too many calories"),
            createNutritionRecord("id4", now, 250, "Valid meal 2"),
        )
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenReturn(mockRecords)

        // When
        val result = repository.getMealHistory().first()

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val meals = (result as Result.Success).data
        assertThat(meals).hasSize(2) // Only valid meals
        assertThat(meals[0].id).isEqualTo("id1")
        assertThat(meals[1].id).isEqualTo("id4")
    }

    @Test
    fun `getMealHistory should sort meals by timestamp descending`() = runTest {
        // Given
        val now = Instant.now()
        val mockRecords = listOf(
            createNutritionRecord("id1", now.minus(5, ChronoUnit.HOURS), 100, "Oldest"),
            createNutritionRecord("id2", now.minus(1, ChronoUnit.HOURS), 200, "Newest"),
            createNutritionRecord("id3", now.minus(3, ChronoUnit.HOURS), 150, "Middle"),
        )
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenReturn(mockRecords)

        // When
        val result = repository.getMealHistory().first()

        // Then
        val meals = (result as Result.Success).data
        assertThat(meals[0].id).isEqualTo("id2") // Newest first
        assertThat(meals[1].id).isEqualTo("id3")
        assertThat(meals[2].id).isEqualTo("id1") // Oldest last
    }

    @Test
    fun `updateMeal should return Success when update completes successfully`() = runTest {
        // Given
        val recordId = "test-id"
        val now = Instant.now()
        val originalRecord = createNutritionRecord(recordId, now, 400, "Old description")
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenReturn(listOf(originalRecord))

        // When
        val result = repository.updateMeal(recordId, 500, "New description")

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(healthConnectDataSource).deleteNutritionRecord(recordId)
        verify(healthConnectDataSource).insertNutritionRecord(500, "New description", now)
    }

    @Test
    fun `updateMeal should return Error when meal not found`() = runTest {
        // Given
        whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
            .thenReturn(emptyList())

        // When
        val result = repository.updateMeal("nonexistent-id", 500, "Description")

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("not found")
    }

    @Test
    fun `updateMeal should return Error when calories is invalid`() = runTest {
        // When
        val result = repository.updateMeal("id", 6000, "Description")

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("Invalid input")
    }

    @Test
    fun `updateMeal should return Error when description is blank`() = runTest {
        // When
        val result = repository.updateMeal("id", 500, "   ")

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("Invalid input")
    }

    @Test
    fun `deleteMeal should return Success when delete completes successfully`() = runTest {
        // Given
        val recordId = "test-id"

        // When
        val result = repository.deleteMeal(recordId)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(healthConnectDataSource).deleteNutritionRecord(recordId)
    }

    @Test
    fun `deleteMeal should return Error when Health Connect throws exception`() = runTest {
        // Given
        val recordId = "test-id"
        whenever(healthConnectDataSource.deleteNutritionRecord(recordId))
            .thenThrow(RuntimeException("Delete failed"))

        // When
        val result = repository.deleteMeal(recordId)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("Failed to delete meal")
    }

    /**
     * Helper to create mock NutritionRecords for testing.
     *
     * Note: In actual Health Connect usage, metadata is auto-generated.
     * For testing, we use Mockito to create mock records with the necessary data.
     */
    private fun createNutritionRecord(
        id: String,
        timestamp: Instant,
        calories: Int,
        description: String,
    ): NutritionRecord {
        return mock<NutritionRecord>().apply {
            whenever(this.metadata).thenReturn(mock())
            whenever(this.metadata.id).thenReturn(id)
            whenever(this.startTime).thenReturn(timestamp)
            whenever(this.energy).thenReturn(Energy.kilocalories(calories.toDouble()))
            whenever(this.name).thenReturn(description)
        }
    }
}
