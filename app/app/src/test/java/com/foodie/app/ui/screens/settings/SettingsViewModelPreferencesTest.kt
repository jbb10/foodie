package com.foodie.app.ui.screens.settings

import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.domain.repository.UserProfileRepository
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
 * Unit tests for SettingsViewModel preference handling (Story 5.1).
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
 *
 * NOTE: API configuration tests are in SettingsViewModelApiConfigTest (Story 5.2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelPreferencesTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var repository: PreferencesRepository
    private lateinit var securePreferences: SecurePreferences
    private lateinit var userProfileRepository: UserProfileRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)

        // Default mock behavior
        every { repository.observePreferences() } returns flowOf(emptyMap())
        every { securePreferences.azureOpenAiApiKey } returns null
        every { securePreferences.azureOpenAiEndpoint } returns null
        every { securePreferences.azureOpenAiModel } returns null
        every { userProfileRepository.getUserProfile() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPreferences loads from repository on init`() = runTest {
        // Given: SecurePreferences and Repository return values
        every { securePreferences.azureOpenAiApiKey } returns "sk-test-key"
        every { securePreferences.azureOpenAiEndpoint } returns "https://test.openai.azure.com"
        every { securePreferences.azureOpenAiModel } returns "gpt-4"
        every { repository.observePreferences() } returns flowOf(emptyMap())

        // When: ViewModel initialized
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // Then: State updated with preference values from SecurePreferences
        val state = viewModel.state.first()
        assertThat(state.apiKey).isEqualTo("sk-test-key")
        assertThat(state.apiEndpoint).isEqualTo("https://test.openai.azure.com")
        assertThat(state.modelName).isEqualTo("gpt-4")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `loadPreferences uses default values when preferences empty`() = runTest {
        // Given: Repository returns empty preferences
        every { repository.observePreferences() } returns flowOf(emptyMap())

        // When: ViewModel initialized
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // Then: State has default values
        val state = viewModel.state.first()
        assertThat(state.apiEndpoint).isEmpty()
        assertThat(state.modelName).isEqualTo("gpt-4.1")
    }

    @Test
    fun `saveString persists to repository successfully`() = runTest {
        // Given: ViewModel initialized
        coEvery { repository.setString(any(), any()) } returns Result.success(Unit)
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // When: Saving a string preference
        viewModel.saveString("pref_test", "test_value")

        // Then: Repository called with correct parameters
        coVerify { repository.setString("pref_test", "test_value") }
    }

    @Test
    fun `saveString sets error state on failure`() = runTest {
        // Given: Repository fails to save
        val exception = Exception("Save failed")
        coEvery { repository.setString(any(), any()) } returns Result.failure(exception)
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // When: Attempting to save
        viewModel.saveString("pref_test", "test_value")

        // Then: Error state set
        val errorState = viewModel.state.value
        assertThat(errorState.isLoading).isFalse()
        assertThat(errorState.error).isEqualTo("Failed to save setting")
    }

    @Test
    fun `saveBoolean persists to repository successfully`() = runTest {
        // Given: ViewModel initialized
        coEvery { repository.setBoolean(any(), any()) } returns Result.success(Unit)
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // When: Saving a boolean preference
        viewModel.saveBoolean("pref_test", true)

        // Then: Repository called with correct parameters
        coVerify { repository.setBoolean("pref_test", true) }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        // Given: ViewModel with error state
        val exception = Exception("Save failed")
        coEvery { repository.setString(any(), any()) } returns Result.failure(exception)
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        viewModel.saveString("pref_test", "test_value")

        // Verify error is set
        assertThat(viewModel.state.value.error).isNotNull()

        // When: Clearing error
        viewModel.clearError()

        // Then: Error removed from state
        val clearedState = viewModel.state.value
        assertThat(clearedState.error).isNull()
    }

    @Test
    fun `saveBoolean sets error state on failure`() = runTest {
        // Given: Repository fails to save
        val exception = Exception("Boolean save failed")
        coEvery { repository.setBoolean(any(), any()) } returns Result.failure(exception)
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // When: Attempting to save boolean
        viewModel.saveBoolean("pref_test_bool", false)

        // Then: Error state set
        val errorState = viewModel.state.value
        assertThat(errorState.isLoading).isFalse()
        assertThat(errorState.error).isEqualTo("Failed to save setting")
    }

    @Test
    fun `observePreferences handles exception gracefully`() = runTest {
        // Given: Repository throws exception during observation
        every { repository.observePreferences() } throws RuntimeException("Observation failed")

        // When: ViewModel initializes (triggers observation)
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // Then: Error is set in state
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo("Failed to load preferences")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `loadApiConfiguration preserves values during observe`() = runTest {
        // Given: SecurePreferences has values
        every { securePreferences.azureOpenAiApiKey } returns "sk-initial-key"
        every { securePreferences.azureOpenAiEndpoint } returns "https://initial.openai.azure.com"
        every { securePreferences.azureOpenAiModel } returns "gpt-4-initial"

        // And repository observePreferences emits update without endpoint/model
        every { repository.observePreferences() } returns flowOf(
            mapOf("pref_theme_mode" to "dark"),
        )

        // When: ViewModel initializes
        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // Then: Initial values from SecurePreferences are preserved
        val state = viewModel.state.value
        assertThat(state.apiKey).isEqualTo("sk-initial-key")
        assertThat(state.apiEndpoint).isEqualTo("https://initial.openai.azure.com")
        assertThat(state.modelName).isEqualTo("gpt-4-initial")
        assertThat(state.themeMode).isEqualTo("dark")
    }
}
