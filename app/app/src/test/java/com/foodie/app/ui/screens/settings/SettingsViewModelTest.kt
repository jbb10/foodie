package com.foodie.app.ui.screens.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.domain.model.ApiConfiguration
import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.domain.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for SettingsViewModel API configuration functionality.
 *
 * Tests:
 * - API configuration save with validation
 * - Test connection flow
 * - State updates
 * - Error handling
 *
 * Story 5.2: Azure OpenAI API Key and Endpoint Configuration
 */
@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    companion object {
        private const val TEST_ENDPOINT = "https://test.openai.azure.com"
        private const val TEST_API_KEY = "sk-test123"
        private const val TEST_MODEL = "gpt-4.1"
    }

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: PreferencesRepository
    private lateinit var securePreferences: SecurePreferences
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)

        // Setup default mock behavior
        every { repository.observePreferences() } returns flowOf(emptyMap())
        every { securePreferences.azureOpenAiApiKey } returns null
        every { securePreferences.azureOpenAiEndpoint } returns null
        every { securePreferences.azureOpenAiModel } returns null
        every { userProfileRepository.getUserProfile() } returns flowOf(null)

        viewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveApiConfiguration validatesInputs`() = runTest {
        // Given invalid endpoint (non-HTTPS)
        val invalidApiKey = TEST_API_KEY
        val invalidEndpoint = "http://test.openai.azure.com"
        val model = TEST_MODEL

        // When saving configuration
        viewModel.saveApiConfiguration(invalidApiKey, invalidEndpoint, model)

        // Then error is set in state
        assertThat(viewModel.state.value.error).isEqualTo("Endpoint must use HTTPS")

        // And repository save not called
        coVerify(exactly = 0) { repository.saveApiConfiguration(any()) }
    }

    @Test
    fun `saveApiConfiguration callsRepository whenValid`() = runTest {
        // Given valid configuration
        val apiKey = TEST_API_KEY
        val endpoint = TEST_ENDPOINT
        val model = TEST_MODEL

        coEvery { repository.saveApiConfiguration(any()) } returns Result.success(Unit)

        // When saving configuration
        viewModel.saveApiConfiguration(apiKey, endpoint, model)

        // Then repository is called with correct config
        coVerify {
            repository.saveApiConfiguration(
                ApiConfiguration(apiKey, endpoint, model),
            )
        }

        // And state is updated
        assertThat(viewModel.state.value.apiKey).isEqualTo(apiKey)
        assertThat(viewModel.state.value.apiEndpoint).isEqualTo(endpoint)
        assertThat(viewModel.state.value.modelName).isEqualTo(model)
    }

    @Test
    fun `testConnection success updatesState`() = runTest {
        // Given successful test connection
        coEvery { repository.testConnection(any(), any(), any()) } returns Result.success(TestConnectionResult.Success)

        // When testing connection
        viewModel.testConnection(TEST_API_KEY, TEST_ENDPOINT, TEST_MODEL)
        advanceUntilIdle()

        // Then state shows success message
        assertThat(viewModel.state.value.saveSuccessMessage).isEqualTo("API configuration valid")
        assertThat(viewModel.state.value.isTestingConnection).isFalse()
    }

    @Test
    fun `testConnection failure displaysError`() = runTest {
        // Given failed test connection
        val errorMessage = "Invalid API key"
        coEvery { repository.testConnection(any(), any(), any()) } returns Result.success(
            TestConnectionResult.Failure(errorMessage),
        )

        // When testing connection
        viewModel.testConnection(TEST_API_KEY, TEST_ENDPOINT, TEST_MODEL)
        advanceUntilIdle()

        // Then state shows error message
        assertThat(viewModel.state.value.error).isEqualTo(errorMessage)
        assertThat(viewModel.state.value.isTestingConnection).isFalse()
    }

    @Test
    fun `apiConfiguration loadsFromRepository`() = runTest {
        // Given API configuration in secure preferences
        every { securePreferences.azureOpenAiApiKey } returns "sk-test123"
        every { securePreferences.azureOpenAiEndpoint } returns "https://test.openai.azure.com"
        every { securePreferences.azureOpenAiModel } returns "gpt-4.1"

        // When ViewModel is initialized
        val newViewModel = SettingsViewModel(repository, securePreferences, userProfileRepository)

        // Then state is populated from preferences
        assertThat(newViewModel.state.value.apiKey).isEqualTo("sk-test123")
        assertThat(newViewModel.state.value.apiEndpoint).isEqualTo("https://test.openai.azure.com")
        assertThat(newViewModel.state.value.modelName).isEqualTo("gpt-4.1")
    }

    @Test
    fun `clearSaveSuccess clearsSuccessMessage`() = runTest {
        // Given success message exists
        coEvery { repository.testConnection(any(), any(), any()) } returns Result.success(TestConnectionResult.Success)
        viewModel.testConnection("test-key", "https://test.openai.azure.com", "gpt-4.1")
        advanceUntilIdle()
        assertThat(viewModel.state.value.saveSuccessMessage).isNotNull()

        // When clearing success message
        viewModel.clearSaveSuccess()
        advanceUntilIdle()

        // Then message is null
        assertThat(viewModel.state.value.saveSuccessMessage).isNull()
    }
}
