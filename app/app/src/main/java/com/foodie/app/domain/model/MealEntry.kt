package com.foodie.app.domain.model

import java.time.Instant
import java.time.ZoneOffset

/**
 * Domain model representing a meal entry in the nutrition tracking system.
 *
 * This model represents the business concept of a meal, independent of any data source
 * implementation (Health Connect, API, etc.). All meal data flows through this model
 * in the domain layer.
 *
 * **Epic 7 Extension:** Added macros fields (protein, carbs, fat) to support
 * macronutrient tracking for body recomposition goals.
 *
 * @param id Unique identifier for the meal entry (typically from Health Connect)
 * @param timestamp When the meal was consumed
 * @param description Text description of the meal (e.g., "Chicken salad with avocado")
 * @param calories Energy content in kilocalories (kcal)
 * @param protein Protein content in grams (supports up to 2 decimal places)
 * @param carbs Carbohydrate content in grams (supports up to 2 decimal places)
 * @param fat Fat content in grams (supports up to 2 decimal places)
 * @param zoneOffset Time zone offset for the meal timestamp
 *
 * @throws IllegalArgumentException if calories is outside valid range (1-5000), macros outside valid ranges, or description is blank
 */
data class MealEntry(
    val id: String,
    val timestamp: Instant,
    val description: String,
    val calories: Int,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val zoneOffset: ZoneOffset? = null,
) {
    init {
        require(calories in 1..5000) {
            "Calories must be between 1 and 5000, got $calories"
        }
        require(protein >= 0.0 && protein <= 500.0) {
            "Protein must be between 0 and 500 grams, got $protein"
        }
        require(carbs >= 0.0 && carbs <= 1000.0) {
            "Carbs must be between 0 and 1000 grams, got $carbs"
        }
        require(fat >= 0.0 && fat <= 500.0) {
            "Fat must be between 0 and 500 grams, got $fat"
        }
        require(description.isNotBlank()) {
            "Description cannot be blank"
        }
    }
}
