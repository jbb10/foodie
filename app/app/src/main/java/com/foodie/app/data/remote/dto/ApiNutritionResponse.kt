package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Parsed nutrition data from Azure OpenAI's output_text field.
 *
 * The AI is instructed to return JSON with calories and description.
 * This DTO represents that parsed JSON structure.
 *
 * Example output_text content:
 * ```json
 * {
 *   "calories": 650,
 *   "description": "Grilled chicken breast with steamed rice and vegetables"
 * }
 * ```
 *
 * Usage:
 * ```kotlin
 * val response: AzureResponseResponse = api.analyzeNutrition(request)
 * val nutritionData = gson.fromJson(response.outputText, ApiNutritionResponse::class.java)
 * ```
 *
 * @property calories Estimated total calories for the meal (integer value)
 * @property description Natural language description of the food items identified
 *
 * Note: This is a data transfer object (DTO) used for API deserialization.
 * The domain model `NutritionData` adds validation (calories range, description length).
 */
data class ApiNutritionResponse(
    @SerializedName("calories")
    val calories: Int,
    @SerializedName("description")
    val description: String
)
