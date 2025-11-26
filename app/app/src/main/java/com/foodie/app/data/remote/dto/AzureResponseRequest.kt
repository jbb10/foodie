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
 * Example multimodal request (our use case):
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
 *   ]
 * }
 * ```
 *
 * @property model The Azure OpenAI deployment name (e.g., "gpt-4.1")
 * @property instructions Optional system-level instructions for the AI
 * @property input Array of input messages with multimodal content
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
)
