package com.foodie.app.domain.model

/**
 * Result of Azure OpenAI API connection test.
 *
 * Returned by PreferencesRepository.testConnection() to indicate whether
 * the configured API credentials are valid and can successfully connect to Azure OpenAI.
 *
 * **Success:** API configuration is valid, endpoint reachable, authentication successful
 * **Failure:** Contains specific error message (network error, invalid key, model not found, etc.)
 *
 * Usage:
 * ```kotlin
 * when (val result = repository.testConnection()) {
 *     is TestConnectionResult.Success -> showToast("Connection valid ✓")
 *     is TestConnectionResult.Failure -> showToast("Error: ${result.errorMessage}")
 * }
 * ```
 */
sealed class TestConnectionResult {
    /**
     * Connection test succeeded - API configuration is valid.
     */
    data object Success : TestConnectionResult()

    /**
     * Connection test failed - API configuration invalid or unreachable.
     *
     * @property errorMessage User-friendly error message explaining the failure
     */
    data class Failure(val errorMessage: String) : TestConnectionResult()
}
