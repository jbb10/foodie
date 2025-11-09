package com.foodie.app.data.local.healthconnect

import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import com.foodie.app.domain.model.MealEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for NutritionRecord extension functions.
 *
 * Tests verify correct conversion from Health Connect NutritionRecord to MealEntry domain model.
 */
class NutritionRecordExtensionsTest {
    
    @Test
    fun `toDomainModel should convert NutritionRecord to MealEntry correctly`() {
        // Given
        val timestamp = Instant.now()
        val description = "Grilled chicken with vegetables"
        val calories = 450
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
        
        val nutritionRecord = NutritionRecord(
            energy = Energy.kilocalories(calories.toDouble()),
            name = description,
            startTime = timestamp,
            endTime = timestamp.plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            metadata = Metadata.autoRecorded(device = Device(type = Device.TYPE_PHONE))
        )
        
        // When
        val mealEntry = nutritionRecord.toDomainModel()
        
        // Then
        // Note: ID will be empty until record is inserted into Health Connect
        assertThat(mealEntry.timestamp).isEqualTo(timestamp)
        assertThat(mealEntry.description).isEqualTo(description)
        assertThat(mealEntry.calories).isEqualTo(calories)
    }
    
    @Test
    fun `toDomainModel should handle null name by using empty string`() {
        // Given
        val timestamp = Instant.now()
        val calories = 300
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
        
        val nutritionRecord = NutritionRecord(
            energy = Energy.kilocalories(calories.toDouble()),
            name = null,
            startTime = timestamp,
            endTime = timestamp.plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            metadata = Metadata.autoRecorded(device = Device(type = Device.TYPE_PHONE))
        )
        
        // When/Then - Should throw because MealEntry validates description is not blank
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            nutritionRecord.toDomainModel()
        }
        
        assertThat(exception.message).contains("Description cannot be blank")
    }
    
    @Test
    fun `toDomainModel should handle null energy by using 0 calories`() {
        // Given
        val timestamp = Instant.now()
        val description = "Unknown calories"
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
        
        val nutritionRecord = NutritionRecord(
            energy = null,
            name = description,
            startTime = timestamp,
            endTime = timestamp.plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            metadata = Metadata.autoRecorded(device = Device(type = Device.TYPE_PHONE))
        )
        
        // When/Then - Should throw because MealEntry validates calories >= 1
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            nutritionRecord.toDomainModel()
        }
        
        assertThat(exception.message).contains("Calories must be between 1 and 5000")
    }
}
