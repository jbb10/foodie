package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Parsed nutrition data from Azure OpenAI's output_text field.
 *
 * The AI is instructed to return JSON with food detection and nutrition data including macros.
 * This DTO supports two response formats:
 *
 * 1. **No food detected:**
 * ```json
 * {
 *   "hasFood": false,
 *   "reason": "Image shows a document, not food"
 * }
 * ```
 *
 * 2. **Food detected (with macros):**
 * ```json
 * {
 *   "hasFood": true,
 *   "calories": 650,
 *   "protein": 45,
 *   "carbs": 60,
 *   "fat": 20,
 *   "description": "Grilled chicken breast with steamed rice and vegetables",
 *   "confidence": 0.85
 * }
 * ```
 *
 * Usage:
 * ```kotlin
 * val response: AzureResponseResponse = api.analyzeNutrition(request)
 * val nutritionData = gson.fromJson(response.outputText, ApiNutritionResponse::class.java)
 *
 * if (nutritionData.hasFood == false) {
 *     throw NoFoodDetectedException(nutritionData.reason ?: "No food detected")
 * }
 *
 * return NutritionData(
 *     calories = nutritionData.calories ?: 0,
 *     protein = nutritionData.protein ?: 0,
 *     carbs = nutritionData.carbs ?: 0,
 *     fat = nutritionData.fat ?: 0,
 *     description = nutritionData.description ?: ""
 * )
 * ```
 *
 * @property hasFood True if food was detected in the image, false otherwise
 * @property calories Estimated total calories (required when hasFood=true, null when hasFood=false)
 * @property protein Estimated total protein in grams (required when hasFood=true, null when hasFood=false)
 * @property carbs Estimated total carbohydrates in grams (required when hasFood=true, null when hasFood=false)
 * @property fat Estimated total fat in grams (required when hasFood=true, null when hasFood=false)
 * @property description Natural language description of food items (required when hasFood=true, null when hasFood=false)
 * @property confidence Estimation confidence level 0.0-1.0 (required when hasFood=true, null when hasFood=false)
 * @property reason Explanation of what was detected instead of food (required when hasFood=false, null when hasFood=true)
 *
 * Note: This is a data transfer object (DTO) used for API deserialization.
 * The domain model `NutritionData` adds validation (calories range, macros ranges, description length).
 */
data class ApiNutritionResponse(
    @SerializedName("hasFood")
    val hasFood: Boolean? = null,

    @SerializedName("calories")
    val calories: Int? = null,

    @SerializedName("protein")
    val protein: Int? = null,

    @SerializedName("carbs")
    val carbs: Int? = null,

    @SerializedName("fat")
    val fat: Int? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("confidence")
    val confidence: Double? = null,

    @SerializedName("reason")
    val reason: String? = null,
)
