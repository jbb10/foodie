package com.foodie.app.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using Android SharedPreferences.
 *
 * Provides reactive preference access with Flow-based observation for Compose integration.
 * Uses standard SharedPreferences for non-sensitive data. Story 5.2 will introduce
 * EncryptedSharedPreferences for API key storage.
 *
 * Architecture:
 * - Singleton lifecycle (Hilt @Singleton)
 * - SharedPreferences injected via Hilt (provided in AppModule)
 * - Flow integration via callbackFlow and OnSharedPreferenceChangeListener
 * - Timber logging for preference changes (no sensitive data logged)
 *
 * Thread safety:
 * - SharedPreferences.Editor.apply() is async and thread-safe
 * - Flow emissions happen on SharedPreferences listener thread
 * - Suspend functions for API consistency (future Room/network integration)
 *
 * @property sharedPreferences SharedPreferences instance from Hilt
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : PreferencesRepository {

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
}
