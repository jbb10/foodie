package com.foodie.app.data.repository

import com.foodie.app.domain.model.ApiConfiguration
import com.foodie.app.domain.model.TestConnectionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstraction for application preferences.
 *
 * Provides reactive access to SharedPreferences with type-safe methods for common preference types.
 * This abstraction enables:
 * - Testability (mock repository in ViewModel tests)
 * - Migration flexibility (can swap SharedPreferences implementation)
 * - Reactive UI updates via Flow (Compose observes preference changes)
 *
 * Architecture:
 * - Domain layer abstraction (repository pattern)
 * - Implementation uses SharedPreferences (Story 5.1) or EncryptedSharedPreferences (Story 5.2)
 * - Hilt provides singleton instance bound in AppModule
 *
 * Usage example:
 * ```
 * // ViewModel observes preferences reactively
 * repository.observeString("pref_theme_mode").collectAsState(initial = "system")
 *
 * // Save preference (suspend function for Room/network future-proofing)
 * viewModelScope.launch {
 *     repository.setString("pref_theme_mode", "dark")
 * }
 * ```
 *
 * Preference key naming convention:
 * - Prefix all keys with "pref_"
 * - Use snake_case: pref_azure_endpoint, pref_theme_mode
 * - Group by category: API keys start with pref_azure_, appearance keys start with pref_theme_
 */
interface PreferencesRepository {
    /**
     * Retrieves a string preference value.
     *
     * @param key Preference key (use "pref_" prefix)
     * @param defaultValue Value returned if preference not set
     * @return Preference value or defaultValue
     */
    suspend fun getString(key: String, defaultValue: String = ""): String

    /**
     * Saves a string preference value.
     *
     * @param key Preference key (use "pref_" prefix)
     * @param value Value to save
     * @return Result.success(Unit) if saved, Result.failure if error
     */
    suspend fun setString(key: String, value: String): Result<Unit>

    /**
     * Retrieves a boolean preference value.
     *
     * @param key Preference key (use "pref_" prefix)
     * @param defaultValue Value returned if preference not set
     * @return Preference value or defaultValue
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /**
     * Saves a boolean preference value.
     *
     * @param key Preference key (use "pref_" prefix)
     * @param value Value to save
     * @return Result.success(Unit) if saved, Result.failure if error
     */
    suspend fun setBoolean(key: String, value: Boolean): Result<Unit>

    /**
     * Observes all preferences reactively.
     *
     * Emits complete preference map on subscription and whenever any preference changes.
     * Use for broad preference monitoring or debugging.
     *
     * @return Flow emitting preference map on changes
     */
    fun observePreferences(): Flow<Map<String, Any?>>

    /**
     * Observes a specific string preference reactively.
     *
     * Emits current value on subscription and whenever the preference changes.
     * Use in Compose with collectAsState() for automatic recomposition.
     *
     * @param key Preference key to observe
     * @return Flow emitting preference value (null if not set)
     */
    fun observeString(key: String): Flow<String?>

    /**
     * Observes a specific boolean preference reactively.
     *
     * Emits current value on subscription and whenever the preference changes.
     * Use in Compose with collectAsState() for automatic recomposition.
     *
     * @param key Preference key to observe
     * @return Flow emitting preference value (null if not set)
     */
    fun observeBoolean(key: String): Flow<Boolean?>

    /**
     * Clears all preferences.
     *
     * Use for testing, user logout, or reset functionality.
     *
     * @return Result.success(Unit) if cleared, Result.failure if error
     */
    suspend fun clearAll(): Result<Unit>

    /**
     * Saves complete Azure OpenAI API configuration.
     *
     * Stores API key in EncryptedSharedPreferences and endpoint/model in standard SharedPreferences.
     * Validates configuration before saving.
     *
     * @param config API configuration to save
     * @return Result.success(Unit) if saved, Result.failure if validation or save failed
     */
    suspend fun saveApiConfiguration(config: ApiConfiguration): Result<Unit>

    /**
     * Retrieves current Azure OpenAI API configuration.
     *
     * Reads from SecurePreferences (API key) and SharedPreferences (endpoint, model).
     *
     * @return Flow emitting current API configuration
     */
    fun getApiConfiguration(): Flow<ApiConfiguration>

    /**
     * Tests Azure OpenAI API connection with provided configuration.
     *
     * Makes a minimal Responses API request to validate credentials and connectivity.
     * Uses ErrorHandler from Epic 4 to classify errors (network, auth, server).
     *
     * @param apiKey Azure OpenAI API key to test
     * @param endpoint Azure OpenAI endpoint URL to test
     * @param modelName Model deployment name to test
     * @return Result containing TestConnectionResult (Success or Failure with error message)
     */
    suspend fun testConnection(apiKey: String, endpoint: String, modelName: String): Result<TestConnectionResult>
}
