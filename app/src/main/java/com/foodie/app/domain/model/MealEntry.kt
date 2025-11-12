package com.foodie.app.domain.model

import java.time.Instant

/**
 * Represents a single meal entry in the user's history.
 *
 * This domain model simplifies the `NutritionRecord` from Health Connect
 * into the essential data needed by the UI.
 *
 * @param id The unique identifier from Health Connect.
 * @param name The AI-generated description of the meal.
 * @param calories The total calories for the meal.
 * @param timestamp The time the meal was recorded.
 */
data class MealEntry(
    val id: String,
    val name: String,
    val calories: Double,
    val timestamp: Instant
)
