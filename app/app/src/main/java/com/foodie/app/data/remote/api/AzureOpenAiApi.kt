package com.foodie.app.data.remote.api

/**
 * Retrofit API interface for Azure OpenAI nutrition analysis.
 *
 * This is a placeholder for Epic 2 implementation. The actual Azure OpenAI
 * integration will be developed when AI-powered meal capture is implemented.
 *
 * Expected endpoints:
 * - POST /chat/completions - Send image + prompt for nutrition analysis
 *
 * Expected request format:
 * - Multipart image upload or base64-encoded image
 * - System prompt for nutrition extraction
 * - Model specification (GPT-4 Vision)
 *
 * Expected response format:
 * - JSON with calories and description
 * - Confidence scores
 * - Error messages
 *
 * TODO (Epic 2):
 * - Define request/response DTOs
 * - Add Retrofit annotations
 * - Configure authentication headers
 * - Implement retry logic
 */
interface AzureOpenAiApi {
    // API methods will be defined in Epic 2
}
