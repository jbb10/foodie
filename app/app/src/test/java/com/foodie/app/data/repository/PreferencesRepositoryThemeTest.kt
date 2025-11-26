package com.foodie.app.data.repository

import android.content.SharedPreferences
import com.foodie.app.domain.model.ThemeMode
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PreferencesRepository theme mode methods.
 *
 * Story 5.4: Dark Mode Support (AC-6, AC-7, AC-8)
 */
class PreferencesRepositoryThemeTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        // Create repository with mocked dependencies
        // Note: AzureOpenAiApi and SecurePreferences not needed for theme tests
        repository = PreferencesRepositoryImpl(
            sharedPreferences = sharedPreferences,
            securePreferences = mockk(relaxed = true),
            azureOpenAiApi = mockk(relaxed = true),
            gson = mockk(relaxed = true),
        )
    }

    @Test
    fun `saveThemeMode persists SYSTEM_DEFAULT to SharedPreferences`() = runTest {
        val result = repository.saveThemeMode(ThemeMode.SYSTEM_DEFAULT)

        assertThat(result.isSuccess).isTrue()
        verify { editor.putString("pref_theme_mode", "system") }
        verify { editor.apply() }
    }

    @Test
    fun `saveThemeMode persists LIGHT to SharedPreferences`() = runTest {
        val result = repository.saveThemeMode(ThemeMode.LIGHT)

        assertThat(result.isSuccess).isTrue()
        verify { editor.putString("pref_theme_mode", "light") }
        verify { editor.apply() }
    }

    @Test
    fun `saveThemeMode persists DARK to SharedPreferences`() = runTest {
        val result = repository.saveThemeMode(ThemeMode.DARK)

        assertThat(result.isSuccess).isTrue()
        verify { editor.putString("pref_theme_mode", "dark") }
        verify { editor.apply() }
    }

    @Test
    fun `getThemeMode returns SYSTEM_DEFAULT when no preference set`() = runTest {
        every { sharedPreferences.getString("pref_theme_mode", "system") } returns "system"

        val themeMode = repository.getThemeMode().first()

        assertThat(themeMode).isEqualTo(ThemeMode.SYSTEM_DEFAULT)
    }

    @Test
    fun `getThemeMode returns LIGHT when preference is light`() = runTest {
        every { sharedPreferences.getString("pref_theme_mode", "system") } returns "light"

        val themeMode = repository.getThemeMode().first()

        assertThat(themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `getThemeMode returns DARK when preference is dark`() = runTest {
        every { sharedPreferences.getString("pref_theme_mode", "system") } returns "dark"

        val themeMode = repository.getThemeMode().first()

        assertThat(themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `getThemeMode defaults to SYSTEM_DEFAULT for invalid value`() = runTest {
        every { sharedPreferences.getString("pref_theme_mode", "system") } returns "invalid"

        val themeMode = repository.getThemeMode().first()

        assertThat(themeMode).isEqualTo(ThemeMode.SYSTEM_DEFAULT)
    }
}
