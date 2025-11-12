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
 * @param id Unique identifier for the meal entry (typically from Health Connect)
 * @param timestamp When the meal was consumed
 * @param description Text description of the meal (e.g., "Chicken salad with avocado")
 * @param calories Energy content in kilocalories (kcal)
 *
 * @throws IllegalArgumentException if calories is outside valid range (1-5000) or description is blank
 */
data class MealEntry(
    val id: String,
    val timestamp: Instant,
    val description: String,
    val calories: Int,
    val zoneOffset: ZoneOffset? = null
) {
    init {
        require(calories in 1..5000) {
            "Calories must be between 1 and 5000, got $calories"
        }
        require(description.isNotBlank()) {
            "Description cannot be blank"
        }
    }
}
