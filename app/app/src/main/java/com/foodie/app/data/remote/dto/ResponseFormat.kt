package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response format configuration for Azure OpenAI Responses API Structured Outputs.
 *
 * In the Responses API, the format structure is flattened with name, strict, and schema
 * at the top level (not nested in a json_schema wrapper).
 *
 * When set to `json_schema` with `strict: true`, Azure OpenAI guarantees the response
 * will match the provided JSON Schema, eliminating parsing errors.
 *
 * Example usage:
 * ```kotlin
 * val responseFormat = ResponseFormat(
 *     type = "json_schema",
 *     name = "nutrition_analysis_with_macros",
 *     strict = true,
 *     schema = NutritionAnalysisSchema.schema
 * )
 * ```
 *
 * Serializes to:
 * ```json
 * {
 *   "type": "json_schema",
 *   "name": "nutrition_analysis_with_macros",
 *   "strict": true,
 *   "schema": { ... }
 * }
 * ```
 *
 * @property type Format type (must be "json_schema" for structured outputs)
 * @property name Schema name (must be descriptive, a-z, A-Z, 0-9, underscores, dashes, max 64 chars)
 * @property strict Whether to enforce strict schema compliance (must be true for structured outputs)
 * @property schema The actual JSON Schema object (as a Map<String, Any>)
 *
 * Reference: https://platform.openai.com/docs/guides/structured-outputs
 */
data class ResponseFormat(
    @SerializedName("type")
    val type: String, // "json_schema"

    @SerializedName("name")
    val name: String,

    @SerializedName("strict")
    val strict: Boolean,

    @SerializedName("schema")
    val schema: Map<String, Any>,
)
