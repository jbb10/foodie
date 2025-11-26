package com.foodie.app.domain.model

/**
 * Domain model representing nutrition data parsed from AI analysis.
 *
 * Used to communicate nutrition information between the AI service layer and
 * the domain layer. This will be the primary data structure for Epic 2 when
 * Azure OpenAI analyzes meal photos.
 *
 * @param calories Estimated energy content in kilocalories (kcal)
 * @param description AI-generated description of the meal
 */
data class NutritionData(
    val calories: Int,
    val description: String,
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
