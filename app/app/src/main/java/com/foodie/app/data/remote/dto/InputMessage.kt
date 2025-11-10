package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Represents an input message for Azure OpenAI Responses API.
 *
 * Input messages can contain multimodal content (text + images).
 * The `role` field specifies the message sender (typically "user").
 *
 * Example from official docs:
 * ```json
 * {
 *   "role": "user",
 *   "content": [
 *     { "type": "input_text", "text": "Analyze this meal" },
 *     { "type": "input_image", "image_url": "data:image/jpeg;base64,..." }
 *   ]
 * }
 * ```
 *
 * @property role The role of the message sender (e.g., "user", "assistant")
 * @property content Array of multimodal content items (text and/or images)
 *
 * Reference: https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses#image-input
 */
data class InputMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: List<ContentItem>
)
