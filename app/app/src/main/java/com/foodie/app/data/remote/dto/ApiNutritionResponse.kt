package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Parsed nutrition data from Azure OpenAI's output_text field.
 *
 * The AI is instructed to return JSON with food detection and nutrition data.
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
 * 2. **Food detected:**
 * ```json
 * {
 *   "hasFood": true,
 *   "calories": 650,
 *   "description": "Grilled chicken breast with steamed rice and vegetables"
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
 *     description = nutritionData.description ?: ""
 * )
 * ```
 *
 * @property hasFood True if food was detected in the image, false otherwise
 * @property calories Estimated total calories (required when hasFood=true, null when hasFood=false)
 * @property description Natural language description of food items (required when hasFood=true, null when hasFood=false)
 * @property reason Explanation of what was detected instead of food (required when hasFood=false, null when hasFood=true)
 *
 * Note: This is a data transfer object (DTO) used for API deserialization.
 * The domain model `NutritionData` adds validation (calories range, description length).
 */
data class ApiNutritionResponse(
    @SerializedName("hasFood")
    val hasFood: Boolean? = null,
    
    @SerializedName("calories")
    val calories: Int? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("reason")
    val reason: String? = null
)
