package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for Azure OpenAI Responses API.
 *
 * The Responses API returns a response object with metadata and output.
 * The `output_text` field provides direct text output for simple access.
 * The `output` array contains structured output items.
 *
 * Example response from official docs:
 * ```json
 * {
 *   "id": "resp_67cb61fa3a448190bcf2c42d96f0d1a8",
 *   "created_at": 1741408624.0,
 *   "model": "gpt-4o-2024-08-06",
 *   "object": "response",
 *   "status": "completed",
 *   "output_text": "{\"calories\": 650, \"description\": \"Grilled chicken\"}",
 *   "usage": {
 *     "input_tokens": 1245,
 *     "output_tokens": 25,
 *     "total_tokens": 1270
 *   }
 * }
 * ```
 *
 * @property id Unique response identifier (e.g., "resp_abc123")
 *property createdAt Unix timestamp when the response was created
 * @property model The model that generated the response
 * @property objectType Always "response" for Responses API
 * @property status Response status (e.g., "completed", "in_progress", "failed")
 * @property outputText Direct text output from the AI (our primary field for nutrition JSON)
 * @property output Array of structured output items (alternative to output_text)
 * @property usage Token usage statistics
 *
 * Reference: https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses
 */
data class AzureResponseResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("created_at")
    val createdAt: Double,
    @SerializedName("model")
    val model: String,
    @SerializedName("object")
    val objectType: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("output_text")
    val outputText: String? = null,
    @SerializedName("output")
    val output: List<Map<String, Any>>? = null,
    @SerializedName("usage")
    val usage: TokenUsage? = null
)

/**
 * Token usage statistics from Azure OpenAI API.
 *
 * @property inputTokens Number of tokens in the request (prompt + image)
 * @property outputTokens Number of tokens in the response
 * @property totalTokens Total tokens consumed (input + output)
 *
 * Note: Vision models consume significantly more input tokens (~1200-1500 for a 2MP image).
 */
data class TokenUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
