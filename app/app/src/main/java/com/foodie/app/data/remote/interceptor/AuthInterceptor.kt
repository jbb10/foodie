package com.foodie.app.data.remote.interceptor

import com.foodie.app.data.local.preferences.SecurePreferences
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * OkHttp interceptor for adding Azure OpenAI authentication headers to API requests.
 *
 * This interceptor automatically injects the `api-key` header required by Azure OpenAI
 * Responses API, using the key stored in SecurePreferences (Android's EncryptedSharedPreferences).
 *
 * **Authentication Method:**
 * - Azure OpenAI uses `api-key` header (NOT `Authorization: Bearer`)
 * - Key is stored securely in EncryptedSharedPreferences
 * - Throws IllegalStateException if API key not configured
 *
 * **Headers Added:**
 * - `api-key: {your_azure_openai_key}`
 * - `Content-Type: application/json`
 *
 * **Error Handling:**
 * - Missing API key → IllegalStateException (halts request)
 * - Malformed requests → Logged with Timber
 * - Network errors → Propagated to caller (WorkManager retry logic handles)
 *
 * **Configuration:**
 * User must set API key via Settings screen before making any nutrition analysis requests.
 * The key is validated when stored and checked again here for defense-in-depth.
 *
 * Example usage in NetworkModule:
 * ```kotlin
 * OkHttpClient.Builder()
 *     .addInterceptor(authInterceptor)
 *     .build()
 * ```
 *
 * Reference: Azure OpenAI Authentication
 * https://learn.microsoft.com/en-us/azure/ai-services/openai/reference#authentication
 */
class AuthInterceptor @Inject constructor(
    private val securePreferences: SecurePreferences
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        const val HEADER_API_KEY = "api-key"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val CONTENT_TYPE_JSON = "application/json"
    }

    /**
     * Intercepts outgoing HTTP requests to add authentication headers.
     *
     * @param chain Interceptor chain from OkHttp
     * @return Response from the server (or error if authentication fails)
     * @throws IllegalStateException If Azure OpenAI API key is not configured
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Retrieve API key from secure storage
        val apiKey = securePreferences.azureOpenAiApiKey
        if (apiKey.isNullOrBlank()) {
            val errorMsg = "Azure OpenAI API key not configured. Please set it in Settings."
            Timber.tag(TAG).e(errorMsg)
            throw IllegalStateException(errorMsg)
        }

        // Build request with authentication headers
        val request = original.newBuilder()
            .header(HEADER_API_KEY, apiKey)
            .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
            .method(original.method, original.body)
            .build()

        Timber.tag(TAG).d("Adding Azure OpenAI auth headers to: ${original.url}")

        return chain.proceed(request)
    }
}
