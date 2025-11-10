package com.foodie.app.data.local.preferences

import com.foodie.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage interface for sensitive configuration data using EncryptedSharedPreferences.
 *
 * This class provides read-only access to Azure OpenAI API configuration for Story 2.4.
 * Write methods (for user configuration via Settings screen) will be implemented in Story 5.2.
 *
 * **Current Implementation (Story 2.4):**
 * - Reads from BuildConfig fields (populated from local.properties)
 * - Returns configured values for API key, endpoint, and model
 * - Temporary approach until Story 5.2 implements EncryptedSharedPreferences
 *
 * **Future Implementation (Story 5.2):**
 * - Will use Android's EncryptedSharedPreferences (androidx.security.crypto)
 * - AES256-GCM encryption for values
 * - AES256-SIV encryption for keys
 * - MasterKey stored in Android KeyStore
 * - User-configurable via Settings screen
 *
 * **Configuration Fields:**
 * - Azure OpenAI API key (required for authentication)
 * - Azure OpenAI endpoint URL (e.g., https://your-resource.openai.azure.com)
 * - Model name (e.g., gpt-4.1, gpt-4-vision-preview)
 *
 * **Configuration via local.properties:**
 * ```
 * azure.openai.api.key="your-api-key-here"
 * azure.openai.endpoint="https://your-resource.openai.azure.com"
 * azure.openai.model="gpt-4.1"
 * ```
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
     * Note: Story 2.4 - Reads from BuildConfig (local.properties).
     *       Story 5.2 will implement actual EncryptedSharedPreferences.
     */
    val azureOpenAiApiKey: String?
        get() = BuildConfig.AZURE_OPENAI_API_KEY.takeIf { it.isNotBlank() }

    /**
     * Retrieves the Azure OpenAI endpoint URL.
     *
     * @return Endpoint URL string (e.g., https://your-resource.openai.azure.com), or null if not configured
     *
     * Note: Story 2.4 - Reads from BuildConfig (local.properties).
     *       Story 5.2 will implement actual EncryptedSharedPreferences.
     */
    val azureOpenAiEndpoint: String?
        get() = BuildConfig.AZURE_OPENAI_ENDPOINT.takeIf { it.isNotBlank() && it != "https://your-resource.openai.azure.com" }

    /**
     * Retrieves the Azure OpenAI model name.
     *
     * @return Model name (e.g., gpt-4.1), or null if not configured
     *
     * Note: Story 2.4 - Reads from BuildConfig (local.properties).
     *       Story 5.2 will implement actual EncryptedSharedPreferences.
     */
    val azureOpenAiModel: String?
        get() = BuildConfig.AZURE_OPENAI_MODEL.takeIf { it.isNotBlank() }
}
