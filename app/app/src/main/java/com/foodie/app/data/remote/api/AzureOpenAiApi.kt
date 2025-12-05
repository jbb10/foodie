package com.foodie.app.data.remote.api

import com.foodie.app.data.remote.dto.AzureResponseRequest
import com.foodie.app.data.remote.dto.AzureResponseResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for Azure OpenAI Responses API with multimodal vision support.
 *
 * This interface defines the contract for communicating with Azure OpenAI Service
 * to analyse food photos and extract nutrition data (calories + description).
 *
 * **Endpoint Structure:**
 * - Base URL: `https://{your-resource-name}.openai.azure.com/`
 * - Full endpoint: `https://{your-resource-name}.openai.azure.com/openai/v1/responses`
 *
 * **Authentication:**
 * - Uses `api-key` header (injected by AuthInterceptor)
 * - NOT `Authorization: Bearer` token (Azure-specific authentication)
 *
 * **Request/Response Flow:**
 * 1. Encode food photo to base64 (via ImageUtils)
 * 2. Build AzureResponseRequest with model, optional instructions, and input array
 * 3. Call analyzeNutrition() with request body
 * 4. Parse AzureResponseResponse.outputText as JSON
 * 5. Map to NutritionData domain model
 *
 * **Error Handling:**
 * - Network errors (IOException) → Retryable via WorkManager
 * - HTTP 4xx (client errors) → Non-retryable, log and fail
 * - HTTP 5xx (server errors) → Retryable via WorkManager
 * - Parse errors (JsonSyntaxException) → Non-retryable, log and fail
 *
 * Reference: Azure OpenAI Responses API
 * https://learn.microsoft.com/en-us/azure/ai-foundry/openai/how-to/responses
 */
interface AzureOpenAiApi {
    /**
     * Analyses a meal photo using Azure OpenAI's multimodal vision capabilities.
     *
     * Sends a food photo (base64-encoded) with a prompt to the AI model and
     * receives structured nutrition data (calories + description) in the response.
     *
     * **Request Format:**
     * ```json
     * {
     *   "model": "gpt-4.1",
     *   "instructions": "You are a nutrition analysis assistant. Analyse the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food).",
     *   "input": [
     *     {
     *       "role": "user",
     *       "content": [
     *         { "type": "input_text", "text": "Analyse this meal and estimate total calories." },
     *         { "type": "input_image", "image_url": "data:image/jpeg;base64,..." }
     *       ]
     *     }
     *   ]
     * }
     * ```
     *
     * **Response Format:**
     * ```json
     * {
     *   "id": "resp_abc123",
     *   "status": "completed",
     *   "output_text": "{\"calories\": 650, \"description\": \"Grilled chicken with rice\"}",
     *   "usage": { "input_tokens": 1245, "output_tokens": 25, "total_tokens": 1270 }
     * }
     * ```
     *
     * @param request Request body with model, optional instructions, and multimodal input
     * @return AzureResponseResponse with output_text containing JSON nutrition data
     * @throws IOException If network error occurs (retryable)
     * @throws retrofit2.HttpException If HTTP error occurs (4xx/5xx status codes)
     *
     * Typical execution time: 10-30 seconds (AI vision analysis latency)
     * Read timeout configured: 30 seconds (see NetworkModule)
     */
    @POST("openai/v1/responses")
    suspend fun analyzeNutrition(
        @Body request: AzureResponseRequest,
    ): AzureResponseResponse
}
