package com.foodie.app.domain.model

/**
 * Domain model representing nutrition data parsed from AI analysis.
 *
 * Used to communicate nutrition information between the AI service layer and
 * the domain layer. This will be the primary data structure for Epic 2 when
 * Azure OpenAI analyzes meal photos.
 *
 * **Epic 7 Extension:** Added macros tracking (protein, carbs, fat) to support
 * macronutrient balance for body recomposition goals.
 *
 * @param calories Estimated energy content in kilocalories (kcal)
 * @param protein Estimated protein content in grams (supports up to 2 decimal places, default 0.0 for backward compatibility)
 * @param carbs Estimated carbohydrate content in grams (supports up to 2 decimal places, default 0.0 for backward compatibility)
 * @param fat Estimated fat content in grams (supports up to 2 decimal places, default 0.0 for backward compatibility)
 * @param description AI-generated description of the meal
 */
data class NutritionData(
    val calories: Int,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val description: String,
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
