package com.foodie.app.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OnboardingPreferences].
 *
 * Validates first-launch detection logic:
 * - Default value (false on fresh install)
 * - Persistence (flag remains true after marking completed)
 * - SharedPreferences integration (correct key/file name)
 *
 * Story: 5.7 - User Onboarding (First Launch)
 * AC: #8 - Onboarding shows only once (first-launch detection via SharedPreferences)
 */
class OnboardingPreferencesTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var onboardingPreferences: OnboardingPreferences

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor

        onboardingPreferences = OnboardingPreferences(context)
    }

    @Test
    fun `isOnboardingCompleted should return false by default`() {
        // Given: Fresh install (no onboarding_completed flag set)
        every { sharedPreferences.getBoolean("onboarding_completed", false) } returns false

        // When: Check onboarding status
        val completed = onboardingPreferences.isOnboardingCompleted()

        // Then: Should return false (onboarding required)
        assertThat(completed).isFalse()
    }

    @Test
    fun `isOnboardingCompleted should return true when flag is set`() {
        // Given: Onboarding previously completed
        every { sharedPreferences.getBoolean("onboarding_completed", false) } returns true

        // When: Check onboarding status
        val completed = onboardingPreferences.isOnboardingCompleted()

        // Then: Should return true (onboarding not required)
        assertThat(completed).isTrue()
    }

    @Test
    fun `markOnboardingCompleted should persist flag`() {
        // When: Mark onboarding completed
        onboardingPreferences.markOnboardingCompleted()

        // Then: Should save flag to SharedPreferences
        verify { editor.putBoolean("onboarding_completed", true) }
        verify { editor.apply() }
    }

    @Test
    fun `uses correct SharedPreferences file name`() {
        // When: OnboardingPreferences is accessed (trigger lazy initialization)
        onboardingPreferences.isOnboardingCompleted()

        // Then: Should use "onboarding_prefs" file
        verify { context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE) }
    }
}
