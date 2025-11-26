package com.foodie.app.domain.model

/**
 * Domain model representing Azure OpenAI API configuration.
 *
 * Encapsulates all required fields for Azure OpenAI Responses API authentication.
 * Provides validation logic to ensure configuration is complete and correctly formatted.
 *
 * **Configuration Fields:**
 * - apiKey: Azure OpenAI API key (stored encrypted)
 * - endpoint: Azure OpenAI endpoint URL (e.g., https://your-resource.openai.azure.com)
 * - modelName: Model deployment name (e.g., gpt-4.1, gpt-4o-mini)
 * - isConfigured: Derived property indicating all fields are valid
 *
 * **Validation Rules:**
 * - API key: Must not be blank
 * - Endpoint: Must use HTTPS, contain ".openai.azure.com"
 * - Model name: Must not be blank
 *
 * Usage:
 * ```kotlin
 * val config = ApiConfiguration(
 *     apiKey = "...",
 *     endpoint = "https://my-resource.openai.azure.com",
 *     modelName = "gpt-4.1"
 * )
 *
 * config.validate() // Returns ValidationResult.Success or ValidationResult.Error
 * ```
 *
 * @property apiKey Azure OpenAI API key
 * @property endpoint Azure OpenAI endpoint URL
 * @property modelName Model deployment name
 */
data class ApiConfiguration(
    val apiKey: String = "",
    val endpoint: String = "",
    val modelName: String = "gpt-4.1",
) {
    /**
     * Indicates whether configuration is complete and valid.
     */
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && endpoint.isNotBlank() && modelName.isNotBlank()

    /**
     * Validates the API configuration.
     *
     * @return ValidationResult.Success if valid, ValidationResult.Error with message if invalid
     */
    fun validate(): ValidationResult {
        return when {
            apiKey.isBlank() -> ValidationResult.Error("API key required")
            endpoint.isBlank() -> ValidationResult.Error("Endpoint URL required")
            !endpoint.startsWith("https://") -> ValidationResult.Error("Endpoint must use HTTPS")
            !endpoint.contains(".openai.azure.com") -> ValidationResult.Error("Invalid Azure OpenAI endpoint format")
            modelName.isBlank() -> ValidationResult.Error("Model name required")
            else -> ValidationResult.Success
        }
    }
}

/**
 * Result of validation operations.
 */
sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
