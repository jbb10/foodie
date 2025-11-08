package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for MealEntry domain model validation logic.
 */
class MealEntryTest {

    @Test
    fun `constructor should create valid MealEntry when all parameters are valid`() {
        // Given
        val id = "test-id"
        val timestamp = Instant.now()
        val description = "Chicken salad"
        val calories = 450

        // When
        val mealEntry = MealEntry(
            id = id,
            timestamp = timestamp,
            description = description,
            calories = calories
        )

        // Then
        assertThat(mealEntry.id).isEqualTo(id)
        assertThat(mealEntry.timestamp).isEqualTo(timestamp)
        assertThat(mealEntry.description).isEqualTo(description)
        assertThat(mealEntry.calories).isEqualTo(calories)
    }

    @Test
    fun `constructor should accept calories at minimum valid value 1`() {
        // Given
        val calories = 1

        // When
        val mealEntry = MealEntry(
            id = "id",
            timestamp = Instant.now(),
            description = "Small snack",
            calories = calories
        )

        // Then
        assertThat(mealEntry.calories).isEqualTo(1)
    }

    @Test
    fun `constructor should accept calories at maximum valid value 5000`() {
        // Given
        val calories = 5000

        // When
        val mealEntry = MealEntry(
            id = "id",
            timestamp = Instant.now(),
            description = "Large meal",
            calories = calories
        )

        // Then
        assertThat(mealEntry.calories).isEqualTo(5000)
    }

    @Test
    fun `constructor should throw exception when calories is less than 1`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "id",
                timestamp = Instant.now(),
                description = "Invalid meal",
                calories = 0
            )
        }

        assertThat(exception.message).contains("Calories must be between 1 and 5000")
        assertThat(exception.message).contains("0")
    }

    @Test
    fun `constructor should throw exception when calories is greater than 5000`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "id",
                timestamp = Instant.now(),
                description = "Invalid meal",
                calories = 5001
            )
        }

        assertThat(exception.message).contains("Calories must be between 1 and 5000")
        assertThat(exception.message).contains("5001")
    }

    @Test
    fun `constructor should throw exception when description is blank`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "id",
                timestamp = Instant.now(),
                description = "",
                calories = 500
            )
        }

        assertThat(exception.message).contains("Description cannot be blank")
    }

    @Test
    fun `constructor should throw exception when description is only whitespace`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "id",
                timestamp = Instant.now(),
                description = "   ",
                calories = 500
            )
        }

        assertThat(exception.message).contains("Description cannot be blank")
    }

    @Test
    fun `constructor should accept description with leading and trailing whitespace`() {
        // Given
        val description = "  Chicken salad  "

        // When
        val mealEntry = MealEntry(
            id = "id",
            timestamp = Instant.now(),
            description = description,
            calories = 500
        )

        // Then
        assertThat(mealEntry.description).isEqualTo(description)
    }
}
