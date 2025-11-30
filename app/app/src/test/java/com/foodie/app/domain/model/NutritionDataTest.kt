package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for NutritionData domain model validation logic.
 * Includes Epic 7 macros validation tests.
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
            description = description,
        )

        // Then
        assertThat(nutritionData.calories).isEqualTo(calories)
        assertThat(nutritionData.description).isEqualTo(description)
    }

    @Test
    fun `constructor should create valid NutritionData with macros when all parameters are valid`() {
        // Given
        val calories = 450
        val protein = 35
        val carbs = 45
        val fat = 12
        val description = "Chicken salad with avocado"

        // When
        val nutritionData = NutritionData(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            description = description,
        )

        // Then
        assertThat(nutritionData.calories).isEqualTo(calories)
        assertThat(nutritionData.protein).isEqualTo(protein)
        assertThat(nutritionData.carbs).isEqualTo(carbs)
        assertThat(nutritionData.fat).isEqualTo(fat)
        assertThat(nutritionData.description).isEqualTo(description)
    }

    @Test
    fun `constructor should accept calories at minimum valid value 1`() {
        // When
        val nutritionData = NutritionData(
            calories = 1,
            description = "Small snack",
        )

        // Then
        assertThat(nutritionData.calories).isEqualTo(1)
    }

    @Test
    fun `constructor should accept calories at maximum valid value 5000`() {
        // When
        val nutritionData = NutritionData(
            calories = 5000,
            description = "Large meal",
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
                description = "Invalid data",
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
                description = "Invalid data",
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
                description = "",
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
                description = "   ",
            )
        }

        assertThat(exception.message).contains("Description cannot be blank")
    }

    // Epic 7: Macros Validation Tests

    @Test
    fun `constructor should accept protein at minimum valid value 0`() {
        // When
        val nutritionData = NutritionData(
            calories = 100,
            protein = 0,
            carbs = 20,
            fat = 5,
            description = "Fruit snack",
        )

        // Then
        assertThat(nutritionData.protein).isEqualTo(0)
    }

    @Test
    fun `constructor should accept protein at maximum valid value 500`() {
        // When
        val nutritionData = NutritionData(
            calories = 2000,
            protein = 500,
            carbs = 0,
            fat = 0,
            description = "Pure protein powder",
        )

        // Then
        assertThat(nutritionData.protein).isEqualTo(500)
    }

    @Test
    fun `constructor should throw exception when protein is negative`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 500,
                protein = -1,
                carbs = 50,
                fat = 10,
                description = "Invalid protein",
            )
        }

        assertThat(exception.message).contains("Protein must be between 0 and 500")
        assertThat(exception.message).contains("-1")
    }

    @Test
    fun `constructor should throw exception when protein exceeds 500`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 2000,
                protein = 501,
                carbs = 0,
                fat = 0,
                description = "Too much protein",
            )
        }

        assertThat(exception.message).contains("Protein must be between 0 and 500")
        assertThat(exception.message).contains("501")
    }

    @Test
    fun `constructor should accept carbs at minimum valid value 0`() {
        // When
        val nutritionData = NutritionData(
            calories = 200,
            protein = 40,
            carbs = 0,
            fat = 10,
            description = "Zero carb meal",
        )

        // Then
        assertThat(nutritionData.carbs).isEqualTo(0)
    }

    @Test
    fun `constructor should accept carbs at maximum valid value 1000`() {
        // When
        val nutritionData = NutritionData(
            calories = 4000,
            protein = 0,
            carbs = 1000,
            fat = 0,
            description = "High carb load",
        )

        // Then
        assertThat(nutritionData.carbs).isEqualTo(1000)
    }

    @Test
    fun `constructor should throw exception when carbs is negative`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 500,
                protein = 30,
                carbs = -1,
                fat = 10,
                description = "Invalid carbs",
            )
        }

        assertThat(exception.message).contains("Carbs must be between 0 and 1000")
        assertThat(exception.message).contains("-1")
    }

    @Test
    fun `constructor should throw exception when carbs exceeds 1000`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 4000,
                protein = 0,
                carbs = 1001,
                fat = 0,
                description = "Too many carbs",
            )
        }

        assertThat(exception.message).contains("Carbs must be between 0 and 1000")
        assertThat(exception.message).contains("1001")
    }

    @Test
    fun `constructor should accept fat at minimum valid value 0`() {
        // When
        val nutritionData = NutritionData(
            calories = 300,
            protein = 50,
            carbs = 50,
            fat = 0,
            description = "Fat-free meal",
        )

        // Then
        assertThat(nutritionData.fat).isEqualTo(0)
    }

    @Test
    fun `constructor should accept fat at maximum valid value 500`() {
        // When
        val nutritionData = NutritionData(
            calories = 4500,
            protein = 0,
            carbs = 0,
            fat = 500,
            description = "Pure fat",
        )

        // Then
        assertThat(nutritionData.fat).isEqualTo(500)
    }

    @Test
    fun `constructor should throw exception when fat is negative`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 500,
                protein = 30,
                carbs = 50,
                fat = -1,
                description = "Invalid fat",
            )
        }

        assertThat(exception.message).contains("Fat must be between 0 and 500")
        assertThat(exception.message).contains("-1")
    }

    @Test
    fun `constructor should throw exception when fat exceeds 500`() {
        // When/Then
        val exception = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NutritionData(
                calories = 4500,
                protein = 0,
                carbs = 0,
                fat = 501,
                description = "Too much fat",
            )
        }

        assertThat(exception.message).contains("Fat must be between 0 and 500")
        assertThat(exception.message).contains("501")
    }

    @Test
    fun `constructor should accept typical balanced meal macros`() {
        // When
        val nutritionData = NutritionData(
            calories = 650,
            protein = 45,
            carbs = 60,
            fat = 20,
            description = "Balanced chicken and rice meal",
        )

        // Then
        assertThat(nutritionData.protein).isEqualTo(45)
        assertThat(nutritionData.carbs).isEqualTo(60)
        assertThat(nutritionData.fat).isEqualTo(20)
        // Verify approx calorie calculation: (45*4) + (60*4) + (20*9) = 180 + 240 + 180 = 600
        // Actual calories 650 is reasonable (includes fiber, alcohol, etc.)
    }
}

