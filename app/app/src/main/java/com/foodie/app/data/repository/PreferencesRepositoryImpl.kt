package com.foodie.app.data.repository

import android.content.SharedPreferences
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.foodie.app.data.remote.dto.AzureResponseRequest
import com.foodie.app.data.remote.dto.ContentItem
import com.foodie.app.data.remote.dto.InputMessage
import com.foodie.app.domain.model.ApiConfiguration
import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.domain.model.ThemeMode
import com.foodie.app.domain.model.ValidationResult
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using Android SharedPreferences.
 *
 * Provides reactive preference access with Flow-based observation for Compose integration.
 * Uses standard SharedPreferences for non-sensitive data and EncryptedSharedPreferences
 * (via SecurePreferences) for API key storage (Story 5.2).
 *
 * Architecture:
 * - Singleton lifecycle (Hilt @Singleton)
 * - SharedPreferences injected via Hilt (provided in AppModule)
 * - SecurePreferences injected for API key encryption
 * - AzureOpenAiApi injected for connection testing
 * - Flow integration via callbackFlow and OnSharedPreferenceChangeListener
 * - Timber logging for preference changes (no sensitive data logged)
 *
 * Thread safety:
 * - SharedPreferences.Editor.apply() is async and thread-safe
 * - Flow emissions happen on SharedPreferences listener thread
 * - Suspend functions for API consistency (future Room/network integration)
 *
 * @property sharedPreferences SharedPreferences instance from Hilt
 * @property securePreferences SecurePreferences for API key storage (EncryptedSharedPreferences)
 * @property azureOpenAiApi Retrofit API client for testing connection
 * @property gson Gson instance for JSON serialization in dynamic Retrofit
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val securePreferences: SecurePreferences,
    private val azureOpenAiApi: AzureOpenAiApi,
    private val gson: Gson
) : PreferencesRepository {

    /**
     * Creates a temporary Retrofit instance with custom endpoint and API key for testing.
     *
     * This allows testing connection with user-provided credentials before saving them.
     * The main Retrofit instance (from NetworkModule) is configured at app startup and
     * doesn't update when endpoint changes, so we create a fresh one for testing.
     */
    private fun createRetrofitForTest(endpoint: String, apiKey: String): Retrofit {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val baseUrl = if (endpoint.endsWith("/")) endpoint else "$endpoint/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    override suspend fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    override suspend fun setString(key: String, value: String): Result<Unit> {
        return try {
            sharedPreferences.edit().putString(key, value).apply()
            Timber.d("Preference saved: $key = $value")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save preference: $key")
            Result.failure(e)
        }
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override suspend fun setBoolean(key: String, value: Boolean): Result<Unit> {
        return try {
            sharedPreferences.edit().putBoolean(key, value).apply()
            Timber.d("Preference saved: $key = $value")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save preference: $key")
            Result.failure(e)
        }
    }

    override fun observePreferences(): Flow<Map<String, Any?>> = callbackFlow {
        // Emit current values immediately
        trySend(sharedPreferences.all)

        // Listen for changes
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ ->
            trySend(prefs.all)
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Cleanup when Flow is cancelled
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun observeString(key: String): Flow<String?> = callbackFlow {
        // Emit current value immediately
        trySend(sharedPreferences.getString(key, null))

        // Listen for changes to this specific key
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getString(key, null))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Cleanup when Flow is cancelled
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun observeBoolean(key: String): Flow<Boolean?> = callbackFlow {
        // Emit current value immediately (null if not set)
        val currentValue = if (sharedPreferences.contains(key)) {
            sharedPreferences.getBoolean(key, false)
        } else {
            null
        }
        trySend(currentValue)

        // Listen for changes to this specific key
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                val value = if (prefs.contains(key)) {
                    prefs.getBoolean(key, false)
                } else {
                    null
                }
                trySend(value)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Cleanup when Flow is cancelled
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return try {
            sharedPreferences.edit().clear().apply()
            Timber.d("All preferences cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear preferences")
            Result.failure(e)
        }
    }

    override suspend fun saveApiConfiguration(config: ApiConfiguration): Result<Unit> {
        return try {
            // Validate configuration first
            val validationResult = config.validate()
            if (validationResult is ValidationResult.Error) {
                return Result.failure(IllegalArgumentException(validationResult.message))
            }

            // Save API key to encrypted storage
            securePreferences.setAzureOpenAiApiKey(config.apiKey)

            // Save endpoint and model using SecurePreferences for consistency
            securePreferences.setAzureOpenAiEndpoint(config.endpoint)
            securePreferences.setAzureOpenAiModelName(config.modelName)

            Timber.d("API configuration saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save API configuration")
            Result.failure(e)
        }
    }

    override fun getApiConfiguration(): Flow<ApiConfiguration> = flow {
        // Emit current configuration
        val apiKey = securePreferences.azureOpenAiApiKey ?: ""
        val endpoint = sharedPreferences.getString("pref_azure_endpoint", "") ?: ""
        val modelName = sharedPreferences.getString("pref_azure_model", "gpt-4.1") ?: "gpt-4.1"

        emit(ApiConfiguration(apiKey, endpoint, modelName))

        // Note: For reactive updates, UI should observe individual preference fields
        // or trigger manual refresh after save
    }

    override suspend fun testConnection(
        apiKey: String,
        endpoint: String,
        modelName: String
    ): Result<TestConnectionResult> {
        return try {
            // Validate configuration
            if (apiKey.isBlank() || endpoint.isBlank() || modelName.isBlank()) {
                return Result.success(TestConnectionResult.Failure("API configuration incomplete"))
            }

            // Create temporary Retrofit with test credentials
            val testRetrofit = createRetrofitForTest(endpoint, apiKey)
            val testApi = testRetrofit.create(AzureOpenAiApi::class.java)

            // Make minimal test request
            val testRequest = AzureResponseRequest(
                model = modelName,
                instructions = "Return a simple greeting.",
                input = listOf(
                    InputMessage(
                        role = "user",
                        content = listOf(
                            ContentItem.TextContent(text = "Hello")
                        )
                    )
                )
            )

            val response = testApi.analyzeNutrition(testRequest)

            // Check if response indicates success
            if (response.status == "completed") {
                Timber.d("API connection test successful")
                Result.success(TestConnectionResult.Success)
            } else {
                Timber.w("API connection test returned unexpected status: ${response.status}")
                Result.success(TestConnectionResult.Failure("Unexpected response status: ${response.status}"))
            }
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401, 403 -> "Invalid API key"
                404 -> {
                    // Provide more specific error message for model vs endpoint issues
                    if (endpoint.startsWith("https://") && endpoint.contains(".openai.azure.com")) {
                        "Deployment '$modelName' not available in your Azure resource"
                    } else {
                        "Endpoint not found. Check your Azure OpenAI endpoint URL."
                    }
                }
                in 500..599 -> "Azure service error"
                else -> "Connection failed: ${e.message()}"
            }
            Timber.e(e, "API connection test failed: HTTP ${e.code()}")
            Result.success(TestConnectionResult.Failure(errorMessage))
        } catch (e: IOException) {
            Timber.e(e, "API connection test failed: Network error")
            Result.success(TestConnectionResult.Failure("Connection failed. Check internet."))
        } catch (e: Exception) {
            Timber.e(e, "API connection test failed: Unexpected error")
            Result.success(TestConnectionResult.Failure("Connection test failed: ${e.message}"))
        }
    }

    /**
     * Saves theme mode preference to SharedPreferences.
     *
     * Stores theme preference as string value ("system", "light", or "dark").
     * Triggers Flow emission to observers via OnSharedPreferenceChangeListener.
     *
     * @param mode ThemeMode to save
     * @return Result.success(Unit) always (SharedPreferences.apply() cannot fail)
     *
     * Story 5.4: Dark Mode Support (AC-8)
     */
    override suspend fun saveThemeMode(mode: ThemeMode): Result<Unit> {
        return try {
            sharedPreferences.edit()
                .putString(KEY_THEME_MODE, mode.value)
                .apply()
            Timber.d("Theme mode saved: ${mode.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save theme mode")
            Result.failure(e)
        }
    }

    /**
     * Retrieves current theme mode preference.
     *
     * Returns Flow for reactive observation by MainActivity and SettingsViewModel.
     * Defaults to SYSTEM_DEFAULT for new installations.
     *
     * @return Flow emitting ThemeMode (defaults to SYSTEM_DEFAULT if not set)
     *
     * Story 5.4: Dark Mode Support (AC-7, AC-8)
     */
    override fun getThemeMode(): Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) {
                val value = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.DEFAULT.value)
                    ?: ThemeMode.DEFAULT.value
                trySend(ThemeMode.fromValue(value))
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Emit initial value
        val initialValue = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.DEFAULT.value)
            ?: ThemeMode.DEFAULT.value
        trySend(ThemeMode.fromValue(initialValue))

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
    }
}
