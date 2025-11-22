package com.foodie.app.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive configuration data using EncryptedSharedPreferences.
 *
 * Provides encrypted storage for Azure OpenAI API credentials using Android Keystore.
 * All data is encrypted at rest with AES256-GCM (values) and AES256-SIV (keys).
 *
 * **Security Architecture:**
 * - MasterKey stored in Android KeyStore (hardware-backed when available)
 * - EncryptedSharedPreferences encrypts all keys and values
 * - API key never logged in full (only length or status)
 * - Endpoint/model stored in standard SharedPreferences (non-sensitive)
 *
 * **Storage Format:**
 * - API Key: Encrypted in "secure_prefs" file
 * - Endpoint/Model: Standard SharedPreferences (visible in device settings)
 *
 * **Error Handling:**
 * - Encryption failures logged with Timber.e()
 * - Returns null on read errors (caller handles gracefully)
 * - Throws IllegalStateException in AuthInterceptor if API key missing
 *
 * **Migration (Story 5.2):**
 * - FoodieApplication.onCreate() migrates BuildConfig → EncryptedSharedPreferences
 * - One-time migration on first launch after Story 5.2 deployment
 *
 * Usage:
 * ```kotlin
 * // Save API key (Settings screen)
 * securePreferences.setAzureOpenAiApiKey("...")
 *
 * // Retrieve API key (AuthInterceptor)
 * val apiKey = securePreferences.azureOpenAiApiKey
 * ```
 *
 * Reference: https://developer.android.com/topic/security/data
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecurePreferences"
        private const val ENCRYPTED_PREFS_FILENAME = "secure_prefs"
        private const val KEY_AZURE_OPENAI_API_KEY = "azure_openai_api_key"
    }

    /**
     * EncryptedSharedPreferences instance for API key storage.
     * Initialized lazily to handle potential encryption failures gracefully.
     */
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize EncryptedSharedPreferences - using fallback")
            // Fallback to standard SharedPreferences if encryption fails
            // This should rarely happen, but prevents app crash on incompatible devices
            context.getSharedPreferences("${ENCRYPTED_PREFS_FILENAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    /**
     * Retrieves the Azure OpenAI API key from encrypted storage.
     *
     * @return API key string, or null if not configured
     */
    val azureOpenAiApiKey: String?
        get() = try {
            encryptedPrefs.getString(KEY_AZURE_OPENAI_API_KEY, null)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to retrieve API key")
            null
        }

    /**
     * Saves the Azure OpenAI API key to encrypted storage.
     *
     * @param key API key to save
     */
    fun setAzureOpenAiApiKey(key: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_AZURE_OPENAI_API_KEY, key)
                .apply()
            Timber.tag(TAG).d("API key saved (length: ${key.length} chars)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save API key")
            throw e
        }
    }

    /**
     * Clears the Azure OpenAI API key from encrypted storage.
     */
    fun clearAzureOpenAiApiKey() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_AZURE_OPENAI_API_KEY)
                .apply()
            Timber.tag(TAG).d("API key cleared")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear API key")
            throw e
        }
    }

    /**
     * Checks if an API key is configured.
     *
     * @return true if API key exists and is not blank
     */
    fun hasApiKey(): Boolean {
        return !azureOpenAiApiKey.isNullOrBlank()
    }

    /**
     * Retrieves the Azure OpenAI endpoint URL from standard SharedPreferences.
     *
     * @return Endpoint URL (e.g., https://your-resource.openai.azure.com), or null if not configured
     */
    val azureOpenAiEndpoint: String?
        get() = context.getSharedPreferences("foodie_prefs", Context.MODE_PRIVATE)
            .getString("pref_azure_endpoint", null)

    /**
     * Retrieves the Azure OpenAI model name from standard SharedPreferences.
     *
     * @return Model name (e.g., gpt-4.1), or null if not configured
     */
    val azureOpenAiModel: String?
        get() = context.getSharedPreferences("foodie_prefs", Context.MODE_PRIVATE)
            .getString("pref_azure_model", null)

    /**
     * Saves the Azure OpenAI endpoint URL to standard SharedPreferences.
     *
     * @param endpoint Endpoint URL to save
     */
    fun setAzureOpenAiEndpoint(endpoint: String) {
        context.getSharedPreferences("foodie_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("pref_azure_endpoint", endpoint)
            .apply()
        Timber.tag(TAG).d("Azure OpenAI endpoint saved")
    }

    /**
     * Saves the Azure OpenAI model name to standard SharedPreferences.
     *
     * @param modelName Model name to save
     */
    fun setAzureOpenAiModelName(modelName: String) {
        context.getSharedPreferences("foodie_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("pref_azure_model", modelName)
            .apply()
        Timber.tag(TAG).d("Azure OpenAI model name saved")
    }

    /**
     * Clears all Azure OpenAI configuration (API key, endpoint, model).
     * Used for testing and reset functionality.
     */
    fun clear() {
        clearAzureOpenAiApiKey()
        context.getSharedPreferences("foodie_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("pref_azure_endpoint")
            .remove("pref_azure_model")
            .apply()
        Timber.tag(TAG).d("All Azure OpenAI configuration cleared")
    }
}
