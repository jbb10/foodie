package com.foodie.app.ui.screens.settings

import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.domain.model.ThemeMode
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsViewModel theme mode methods.
 *
 * Story 5.4: Dark Mode Support (AC-7, AC-8)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelThemeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PreferencesRepository
    private lateinit var securePreferences: SecurePreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)

        // Mock initial preferences
        every { securePreferences.azureOpenAiApiKey } returns ""
        every { securePreferences.azureOpenAiEndpoint } returns ""
        every { securePreferences.azureOpenAiModel } returns "gpt-4.1"
        every { repository.observePreferences() } returns flowOf(emptyMap())

        viewModel = SettingsViewModel(repository, securePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateThemeMode with LIGHT persists to repository`() = runTest {
        coEvery { repository.saveThemeMode(ThemeMode.LIGHT) } returns Result.success(Unit)

        viewModel.updateThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { repository.saveThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `updateThemeMode with DARK persists to repository`() = runTest {
        coEvery { repository.saveThemeMode(ThemeMode.DARK) } returns Result.success(Unit)

        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        coVerify { repository.saveThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `updateThemeMode with SYSTEM_DEFAULT persists to repository`() = runTest {
        coEvery { repository.saveThemeMode(ThemeMode.SYSTEM_DEFAULT) } returns Result.success(Unit)

        viewModel.updateThemeMode(ThemeMode.SYSTEM_DEFAULT)
        advanceUntilIdle()

        coVerify { repository.saveThemeMode(ThemeMode.SYSTEM_DEFAULT) }
    }

    @Test
    fun `updateThemeMode updates state themeMode on success`() = runTest {
        coEvery { repository.saveThemeMode(ThemeMode.DARK) } returns Result.success(Unit)

        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertThat(viewModel.state.value.themeMode).isEqualTo("dark")
    }

    @Test
    fun `updateThemeMode sets isLoading true during save`() = runTest {
        coEvery { repository.saveThemeMode(any()) } coAnswers {
            kotlinx.coroutines.delay(100) // Simulate async delay
            Result.success(Unit)
        }

        viewModel.updateThemeMode(ThemeMode.LIGHT)
        // Before advanceUntilIdle, isLoading should be true
        advanceUntilIdle()

        // After completion, isLoading should be false
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `updateThemeMode sets error state on failure`() = runTest {
        val error = Exception("Failed to save theme")
        coEvery { repository.saveThemeMode(any()) } returns Result.failure(error)

        viewModel.updateThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo("Failed to save theme")
        assertThat(viewModel.state.value.isLoading).isFalse()
    }
}
