package com.foodie.app.domain.model

/**
 * Represents the current state of AI-powered nutrition analysis.
 *
 * Used to track background processing of meal photos through the Azure OpenAI pipeline.
 * This sealed class provides type-safe state management for async operations.
 */
sealed class AnalysisStatus {
    /**
     * No analysis is currently in progress.
     */
    data object Idle : AnalysisStatus()

    /**
     * Analysis is currently being processed by Azure OpenAI.
     *
     * @param progress Optional progress indicator (0.0 to 1.0), null if indeterminate
     */
    data class Analyzing(val progress: Float? = null) : AnalysisStatus()

    /**
     * Analysis completed successfully.
     *
     * @param data The nutrition data extracted from the meal photo
     */
    data class Success(val data: NutritionData) : AnalysisStatus()

    /**
     * Analysis failed due to an error.
     *
     * @param message User-friendly error message explaining what went wrong
     * @param exception Optional exception for debugging purposes
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : AnalysisStatus()
}
