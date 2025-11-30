package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

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
            calories = calories,
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
            calories = calories,
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
            calories = calories,
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
                calories = 0,
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
                calories = 5001,
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
                calories = 500,
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
                calories = 500,
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
            calories = 500,
        )

        // Then
        assertThat(mealEntry.description).isEqualTo(description)
    }

    // Epic 7: Macros Tests

    @Test
    fun `constructor should create MealEntry with macros when all parameters valid`() {
        // Given
        val timestamp = Instant.now()

        // When
        val mealEntry = MealEntry(
            id = "test-id",
            timestamp = timestamp,
            description = "Chicken and rice",
            calories = 650,
            protein = 45,
            carbs = 60,
            fat = 20,
        )

        // Then
        assertThat(mealEntry.protein).isEqualTo(45)
        assertThat(mealEntry.carbs).isEqualTo(60)
        assertThat(mealEntry.fat).isEqualTo(20)
    }

    @Test
    fun `constructor should use default macros values of 0 when not specified`() {
        // When
        val mealEntry = MealEntry(
            id = "test-id",
            timestamp = Instant.now(),
            description = "Legacy meal without macros",
            calories = 500,
        )

        // Then
        assertThat(mealEntry.protein).isEqualTo(0)
        assertThat(mealEntry.carbs).isEqualTo(0)
        assertThat(mealEntry.fat).isEqualTo(0)
    }

    @Test
    fun `constructor should accept macros at boundary values`() {
        // When
        val mealEntry = MealEntry(
            id = "test-id",
            timestamp = Instant.now(),
            description = "Extreme macros",
            calories = 5000,
            protein = 500,
            carbs = 1000,
            fat = 500,
        )

        // Then
        assertThat(mealEntry.protein).isEqualTo(500)
        assertThat(mealEntry.carbs).isEqualTo(1000)
        assertThat(mealEntry.fat).isEqualTo(500)
    }

    @Test
    fun `constructor should throw exception when protein is negative`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "test-id",
                timestamp = Instant.now(),
                description = "Invalid protein",
                calories = 500,
                protein = -1,
                carbs = 50,
                fat = 10,
            )
        }

        assertThat(exception.message).contains("Protein must be between 0 and 500")
    }

    @Test
    fun `constructor should throw exception when carbs is negative`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "test-id",
                timestamp = Instant.now(),
                description = "Invalid carbs",
                calories = 500,
                protein = 30,
                carbs = -1,
                fat = 10,
            )
        }

        assertThat(exception.message).contains("Carbs must be between 0 and 1000")
    }

    @Test
    fun `constructor should throw exception when fat is negative`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "test-id",
                timestamp = Instant.now(),
                description = "Invalid fat",
                calories = 500,
                protein = 30,
                carbs = 50,
                fat = -1,
            )
        }

        assertThat(exception.message).contains("Fat must be between 0 and 500")
    }

    @Test
    fun `constructor should throw exception when protein exceeds maximum`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "test-id",
                timestamp = Instant.now(),
                description = "Too much protein",
                calories = 2000,
                protein = 501,
                carbs = 0,
                fat = 0,
            )
        }

        assertThat(exception.message).contains("Protein must be between 0 and 500")
    }

    @Test
    fun `constructor should throw exception when carbs exceeds maximum`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "test-id",
                timestamp = Instant.now(),
                description = "Too many carbs",
                calories = 4000,
                protein = 0,
                carbs = 1001,
                fat = 0,
            )
        }

        assertThat(exception.message).contains("Carbs must be between 0 and 1000")
    }

    @Test
    fun `constructor should throw exception when fat exceeds maximum`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            MealEntry(
                id = "test-id",
                timestamp = Instant.now(),
                description = "Too much fat",
                calories = 4500,
                protein = 0,
                carbs = 0,
                fat = 501,
            )
        }

        assertThat(exception.message).contains("Fat must be between 0 and 500")
    }
}

