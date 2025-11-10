package com.foodie.app.data.local.preferences

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage interface for sensitive configuration data using EncryptedSharedPreferences.
 *
 * This class provides read-only access to Azure OpenAI API configuration for Story 2.4.
 * Write methods (for user configuration via Settings screen) will be implemented in Story 5.2.
 *
 * **Current Implementation:**
 * - Placeholder implementation for testing
 * - Returns null for all fields (API key, endpoint, model)
 * - Full EncryptedSharedPreferences implementation deferred to Story 5.2
 *
 * **Security:**
 * - Will use Android's EncryptedSharedPreferences (androidx.security.crypto)
 * - AES256-GCM encryption for values
 * - AES256-SIV encryption for keys
 * - MasterKey stored in Android KeyStore
 *
 * **Configuration Fields:**
 * - Azure OpenAI API key (required for authentication)
 * - Azure OpenAI endpoint URL (e.g., https://your-resource.openai.azure.com)
 * - Model name (e.g., gpt-4.1, gpt-4-vision-preview)
 *
 * Story 5.2 will add:
 * - setAzureOpenAiApiKey(key: String)
 * - setAzureOpenAiEndpoint(endpoint: String)
 * - setAzureOpenAiModel(model: String)
 * - clearAll()
 */
@Singleton
class SecurePreferences @Inject constructor() {

    /**
     * Retrieves the Azure OpenAI API key.
     *
     * @return API key string, or null if not configured
     *
     * Note: Placeholder implementation. Story 5.2 will implement actual EncryptedSharedPreferences.
     */
    val azureOpenAiApiKey: String?
        get() = null  // TODO: Story 5.2 - Read from EncryptedSharedPreferences

    /**
     * Retrieves the Azure OpenAI endpoint URL.
     *
     * @return Endpoint URL string (e.g., https://your-resource.openai.azure.com), or null if not configured
     *
     * Note: Placeholder implementation. Story 5.2 will implement actual EncryptedSharedPreferences.
     */
    val azureOpenAiEndpoint: String?
        get() = null  // TODO: Story 5.2 - Read from EncryptedSharedPreferences

    /**
     * Retrieves the Azure OpenAI model name.
     *
     * @return Model name (e.g., gpt-4.1), or null if not configured
     *
     * Note: Placeholder implementation. Story 5.2 will implement actual EncryptedSharedPreferences.
     */
    val azureOpenAiModel: String?
        get() = null  // TODO: Story 5.2 - Read from EncryptedSharedPreferences
}
