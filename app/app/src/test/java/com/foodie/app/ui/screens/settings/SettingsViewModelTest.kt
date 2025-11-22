package com.foodie.app.ui.screens.settings

import com.foodie.app.data.repository.PreferencesRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsViewModel.
 *
 * Tests state management, preference loading, saving, and reactive updates.
 * Uses MockK for mocking PreferencesRepository and standard Flow testing.
 *
 * Test coverage:
 * - Preference loading on initialization
 * - Preference saving (string and boolean)
 * - Reactive state updates when preferences change
 * - Error handling for save failures
 * - Loading states during async operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var preferencesRepository: PreferencesRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPreferences loads from repository on init`() = runTest {
        // Given: Repository returns preferences
        val preferences = mapOf(
            "pref_azure_endpoint" to "https://test.openai.azure.com",
            "pref_azure_model" to "gpt-4",
            "pref_theme_mode" to "dark"
        )
        every { preferencesRepository.observePreferences() } returns flowOf(preferences)

        // When: ViewModel initialized
        viewModel = SettingsViewModel(preferencesRepository)

        // Then: State updated with preference values
        val state = viewModel.state.first()
        assertThat(state.apiEndpoint).isEqualTo("https://test.openai.azure.com")
        assertThat(state.modelName).isEqualTo("gpt-4")
        assertThat(state.themeMode).isEqualTo("dark")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `loadPreferences uses default values when preferences empty`() = runTest {
        // Given: Repository returns empty preferences
        every { preferencesRepository.observePreferences() } returns flowOf(emptyMap())

        // When: ViewModel initialized
        viewModel = SettingsViewModel(preferencesRepository)

        // Then: State has default values
        val state = viewModel.state.first()
        assertThat(state.apiEndpoint).isEmpty()
        assertThat(state.modelName).isEqualTo("gpt-4.1")
        assertThat(state.themeMode).isEqualTo("system")
    }

    @Test
    fun `saveString persists to repository successfully`() = runTest {
        // Given: ViewModel initialized
        every { preferencesRepository.observePreferences() } returns flowOf(emptyMap())
        coEvery { preferencesRepository.setString(any(), any()) } returns Result.success(Unit)
        viewModel = SettingsViewModel(preferencesRepository)

        // When: Saving a string preference
        viewModel.saveString("pref_test", "test_value")

        // Then: Repository called with correct parameters
        coVerify { preferencesRepository.setString("pref_test", "test_value") }
    }

    @Test
    fun `saveString sets error state on failure`() = runTest {
        // Given: Repository fails to save
        every { preferencesRepository.observePreferences() } returns flowOf(emptyMap())
        val exception = Exception("Save failed")
        coEvery { preferencesRepository.setString(any(), any()) } returns Result.failure(exception)
        viewModel = SettingsViewModel(preferencesRepository)

        // When: Attempting to save
        viewModel.saveString("pref_test", "test_value")
        testScheduler.advanceUntilIdle()

        // Then: Error state set
        val errorState = viewModel.state.value
        assertThat(errorState.isLoading).isFalse()
        assertThat(errorState.error).isEqualTo("Failed to save setting")
    }

    @Test
    fun `saveBoolean persists to repository successfully`() = runTest {
        // Given: ViewModel initialized
        every { preferencesRepository.observePreferences() } returns flowOf(emptyMap())
        coEvery { preferencesRepository.setBoolean(any(), any()) } returns Result.success(Unit)
        viewModel = SettingsViewModel(preferencesRepository)

        // When: Saving a boolean preference
        viewModel.saveBoolean("pref_test", true)

        // Then: Repository called with correct parameters
        coVerify { preferencesRepository.setBoolean("pref_test", true) }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        // Given: ViewModel with error state
        every { preferencesRepository.observePreferences() } returns flowOf(emptyMap())
        val exception = Exception("Save failed")
        coEvery { preferencesRepository.setString(any(), any()) } returns Result.failure(exception)
        viewModel = SettingsViewModel(preferencesRepository)

        viewModel.saveString("pref_test", "test_value")
        testScheduler.advanceUntilIdle()

        // Verify error is set
        assertThat(viewModel.state.value.error).isNotNull()

        // When: Clearing error
        viewModel.clearError()

        // Then: Error removed from state
        val clearedState = viewModel.state.value
        assertThat(clearedState.error).isNull()
    }
}

