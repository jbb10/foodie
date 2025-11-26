package com.foodie.app.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences manager for onboarding state.
 *
 * Tracks first-launch detection to conditionally show onboarding flow.
 * Uses standard SharedPreferences (not EncryptedSharedPreferences) since
 * onboarding completion flag is non-sensitive data.
 *
 * **Storage:**
 * - File: "onboarding_prefs"
 * - Key: "onboarding_completed"
 * - Default: false (onboarding required)
 *
 * **Usage:**
 * ```kotlin
 * // Check if onboarding completed (in NavGraph or MainActivity)
 * if (!onboardingPreferences.isOnboardingCompleted()) {
 *     // Show onboarding flow
 * }
 *
 * // Mark onboarding completed (on skip or final screen "Done")
 * onboardingPreferences.markOnboardingCompleted()
 * ```
 *
 * **Architecture:**
 * - Singleton scope (Hilt @Singleton)
 * - Application context injection (@ApplicationContext)
 * - Synchronous operations (SharedPreferences reads/writes are fast)
 *
 * **Testing:**
 * - Unit tests verify default value (false on fresh install)
 * - Unit tests verify persistence across app restarts
 * - Manual tests verify onboarding shows only once
 *
 * Story: 5.7 - User Onboarding (First Launch)
 * AC: #8 - Onboarding shows only once (first-launch detection via SharedPreferences)
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    /**
     * SharedPreferences instance for onboarding state.
     * Lazily initialized to avoid blocking injection.
     */
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the user has completed onboarding.
     *
     * @return true if onboarding completed, false if first launch or incomplete
     */
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Marks onboarding as completed.
     *
     * This should be called when:
     * - User completes all onboarding screens and taps "Done"
     * - User taps "Skip" button on any onboarding screen
     *
     * After calling this method, [isOnboardingCompleted] will return true
     * for all subsequent app launches.
     */
    fun markOnboardingCompleted() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }
}
