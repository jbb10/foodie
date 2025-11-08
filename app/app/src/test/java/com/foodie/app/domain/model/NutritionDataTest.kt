package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for NutritionData domain model validation logic.
 */
class NutritionDataTest {

    @Test
    fun `constructor should create valid NutritionData when all parameters are valid`() {
        // Given
        val calories = 450
        val description = "Chicken salad with avocado"

        // When
        val nutritionData = NutritionData(
            calories = calories,
            description = description
        )

        // Then
        assertThat(nutritionData.calories).isEqualTo(calories)
        assertThat(nutritionData.description).isEqualTo(description)
    }

    @Test
    fun `constructor should accept calories at minimum valid value 1`() {
        // When
        val nutritionData = NutritionData(
            calories = 1,
            description = "Small snack"
        )

        // Then
        assertThat(nutritionData.calories).isEqualTo(1)
    }

    @Test
    fun `constructor should accept calories at maximum valid value 5000`() {
        // When
        val nutritionData = NutritionData(
            calories = 5000,
            description = "Large meal"
        )

        // Then
        assertThat(nutritionData.calories).isEqualTo(5000)
    }

    @Test
    fun `constructor should throw exception when calories is less than 1`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 0,
                description = "Invalid data"
            )
        }

        assertThat(exception.message).contains("Calories must be between 1 and 5000")
        assertThat(exception.message).contains("0")
    }

    @Test
    fun `constructor should throw exception when calories is greater than 5000`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 5001,
                description = "Invalid data"
            )
        }

        assertThat(exception.message).contains("Calories must be between 1 and 5000")
        assertThat(exception.message).contains("5001")
    }

    @Test
    fun `constructor should throw exception when description is blank`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 500,
                description = ""
            )
        }

        assertThat(exception.message).contains("Description cannot be blank")
    }

    @Test
    fun `constructor should throw exception when description is only whitespace`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 500,
                description = "   "
            )
        }

        assertThat(exception.message).contains("Description cannot be blank")
    }
}
