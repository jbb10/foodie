package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for Azure OpenAI Responses API.
 *
 * The Responses API uses a simpler structure compared to chat completions.
 * The `input` field can be either:
 * - A simple string for text-only requests
 * - An array of InputMessage objects for multimodal requests
 *
 * Example simple request:
 * ```json
 * {
 *   "model": "gpt-4.1",
 *   "input": "This is a test"
 * }
 * ```
 *
 * Example multimodal request with structured outputs (our use case):
 * ```json
 * {
 *   "model": "gpt-4.1",
 *   "instructions": "You are a nutrition assistant...",
 *   "input": [
 *     {
 *       "role": "user",
 *       "content": [
 *         { "type": "input_text", "text": "Analyze this meal" },
 *         { "type": "input_image", "image_url": "data:image/jpeg;base64,..." }
 *       ]
 *     }
 *   ],
 *   "text": {
 *     "format": {
 *       "type": "json_schema",
 *       "name": "nutrition_analysis_with_macros",
 *       "strict": true,
 *       "schema": { ... }
 *     }
 *   }
 * }
 * ```
 *
 * @property model The Azure OpenAI deployment name (e.g., "gpt-4.1")
 * @property instructions Optional system-level instructions for the AI
 * @property input Array of input messages with multimodal content
 * @property text Optional text modality configuration including format for structured outputs
 *
 * Reference: https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses
 */
data class AzureResponseRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("instructions")
    val instructions: String? = null,
    @SerializedName("input")
    val input: List<InputMessage>,
    @SerializedName("text")
    val text: TextModality? = null,
)

/**
 * Text modality configuration for Azure OpenAI Responses API.
 *
 * In the Responses API, the `response_format` parameter is nested inside the `text` modality object.
 * This allows configuration of text output format including JSON Schema for structured outputs.
 *
 * @property format The response format configuration (e.g., JSON Schema for structured outputs)
 */
data class TextModality(
    @SerializedName("format")
    val format: ResponseFormat,
)
