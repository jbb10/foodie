package com.foodie.app.data.remote.dto

/**
 * JSON Schema for Azure OpenAI Structured Outputs - Nutrition Analysis Response.
 *
 * This schema enforces strict typing and required fields for the nutrition analysis response,
 * eliminating JSON parsing errors. Azure OpenAI will guarantee that responses match this schema
 * when `response_format` is set with `strict: true`.
 *
 * **Schema Structure:**
 * - `hasFood`: Boolean - true if food detected, false otherwise (REQUIRED)
 * - `calories`: Integer 1-5000 - total estimated calories (REQUIRED when hasFood=true)
 * - `protein`: Integer 0-500 - total estimated protein in grams (REQUIRED when hasFood=true)
 * - `carbs`: Integer 0-1000 - total estimated carbs in grams (REQUIRED when hasFood=true)
 * - `fat`: Integer 0-500 - total estimated fat in grams (REQUIRED when hasFood=true)
 * - `description`: String - meal description (REQUIRED when hasFood=true)
 * - `confidence`: Number 0.0-1.0 - estimation confidence (REQUIRED when hasFood=true)
 * - `reason`: String - explanation when hasFood=false (REQUIRED when hasFood=false)
 * - `caloriesRange`: Object with low/high integers - calorie range estimate (OPTIONAL)
 * - `items`: Array of meal item objects - breakdown by food item (OPTIONAL)
 * - `assumptions`: Array of strings - key estimation assumptions (OPTIONAL)
 *
 * **Validation:**
 * - All required fields are enforced by the JSON schema (`strict: true`)
 * - Range validation (calories 1-5000, protein 0-500g, etc.) is NOT enforced by schema
 *   (JSON Schema `minimum`/`maximum` keywords not supported in strict mode)
 * - Range validation happens in domain model (`NutritionData` init block)
 *
 * **Usage:**
 * ```kotlin
 * val request = AzureResponseRequest(
 *     model = "gpt-4.1",
 *     instructions = augmentedPrompt,
 *     input = inputMessages,
 *     text = TextModality(
 *         format = ResponseFormat(
 *             type = "json_schema",
 *             name = "nutrition_analysis_with_macros",
 *             strict = true,
 *             schema = NutritionAnalysisSchema.schema
 *         )
 *     )
 * )
 * ```
 *
 * Reference: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/structured-outputs
 */
object NutritionAnalysisSchema {
    /**
     * JSON Schema object for structured nutrition analysis response.
     *
     * This schema defines all fields as required since Azure OpenAI strict mode requires
     * all properties to be in the required array. The prompt instructions guide the model
     * to populate fields conditionally:
     * - If hasFood=false: Set calories/protein/carbs/fat to 0, description to empty string,
     *   confidence to 0.0, and populate reason with explanation
     * - If hasFood=true: Populate all nutrition fields with estimates, set reason to empty string
     *
     * Note: Azure OpenAI strict mode requires all properties to be required (no optional fields).
     */
    val schema = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "hasFood" to mapOf(
                "type" to "boolean",
                "description" to "True if food was detected in the image, false otherwise"
            ),
            "calories" to mapOf(
                "type" to "integer",
                "description" to "Estimated total calories for all visible food (1-5000 kcal, rounded to nearest 10). Set to 0 if hasFood=false."
            ),
            "protein" to mapOf(
                "type" to "integer",
                "description" to "Estimated total protein in grams (0-500g, rounded to nearest gram). Set to 0 if hasFood=false."
            ),
            "carbs" to mapOf(
                "type" to "integer",
                "description" to "Estimated total carbohydrates in grams (0-1000g, rounded to nearest gram). Set to 0 if hasFood=false."
            ),
            "fat" to mapOf(
                "type" to "integer",
                "description" to "Estimated total fat in grams (0-500g, rounded to nearest gram). Set to 0 if hasFood=false."
            ),
            "description" to mapOf(
                "type" to "string",
                "description" to "Brief natural language description of the meal or packaged food product. Set to empty string if hasFood=false."
            ),
            "confidence" to mapOf(
                "type" to "number",
                "description" to "Estimation confidence level (0.0 to 1.0). Set to 0.0 if hasFood=false."
            ),
            "reason" to mapOf(
                "type" to "string",
                "description" to "Explanation of what was detected instead of food when hasFood=false. Set to empty string if hasFood=true."
            )
        ),
        "required" to listOf("hasFood", "calories", "protein", "carbs", "fat", "description", "confidence", "reason"),
        "additionalProperties" to false
    )
}
