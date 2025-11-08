package com.foodie.app.domain.repository

import android.net.Uri
import com.foodie.app.domain.model.NutritionData
import com.foodie.app.util.Result

/**
 * Repository interface for AI-powered nutrition analysis.
 *
 * This is a placeholder interface for Epic 2 implementation. The actual Azure OpenAI
 * integration will be implemented when the AI-powered meal capture feature is developed.
 *
 * Expected behavior:
 * - Accept a photo URI (from camera or gallery)
 * - Send to Azure OpenAI for analysis
 * - Parse the response into structured NutritionData
 * - Handle API errors, rate limits, and network issues gracefully
 */
interface NutritionAnalysisRepository {
    /**
     * Analyzes a meal photo using Azure OpenAI vision capabilities.
     *
     * This method will be implemented in Epic 2. It should:
     * 1. Read the image from the URI
     * 2. Send to Azure OpenAI API with appropriate prompt
     * 3. Parse the response to extract calories and description
     * 4. Return structured NutritionData
     *
     * @param photoUri URI pointing to the meal photo (content:// or file://)
     * @return Result.Success with NutritionData on successful analysis, Result.Error on failure
     *
     * Possible errors:
     * - Invalid or inaccessible photo URI
     * - Network connectivity issues
     * - Azure OpenAI API errors (rate limit, authentication, etc.)
     * - Unable to parse nutrition information from response
     */
    suspend fun analyzePhoto(photoUri: Uri): Result<NutritionData>
}
