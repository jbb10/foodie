package com.foodie.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Sealed class representing content items for Azure OpenAI Responses API multimodal input.
 *
 * The Responses API supports two types of content:
 * - Text prompts ("input_text" type)
 * - Images as base64 data URLs or URLs ("input_image" type)
 *
 * Example from official docs:
 * ```json
 * {
 *   "role": "user",
 *   "content": [
 *     {
 *       "type": "input_text",
 *       "text": "what is in this image?"
 *     },
 *     {
 *       "type": "input_image",
 *       "image_url": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
 *     }
 *   ]
 * }
 * ```
 *
 * Reference: https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses
 */
sealed class ContentItem {
    abstract val type: String

    /**
     * Text content item for prompts.
     *
     * @property type Always "input_text" for text content
     * @property text The text prompt or message
     */
    data class TextContent(
        @SerializedName("type")
        override val type: String = "input_text",
        @SerializedName("text")
        val text: String
    ) : ContentItem()

    /**
     * Image content item for base64-encoded images or image URLs.
     *
     * @property type Always "input_image" for image content
     * @property imageUrl Base64 data URL format: "data:image/jpeg;base64,{encoded_data}"
     *                    or a publicly accessible HTTPS URL
     */
    data class ImageContent(
        @SerializedName("type")
        override val type: String = "input_image",
        @SerializedName("image_url")
        val imageUrl: String
    ) : ContentItem()
}
