package com.foodie.app.ui.screens.settings

import android.content.SharedPreferences
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.foodie.app.data.repository.PreferencesRepositoryImpl
import com.foodie.app.domain.model.ApiConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for model selection and configuration functionality.
 * Story 5.3: Model Selection and Configuration
 *
 * Tests:
 * - Model selection default value
 * - Model persistence to SharedPreferences
 * - Custom deployment name acceptance
 *
 * Note: Test connection validation requires integration testing as it creates
 * a new Retrofit instance internally. See manual test guide for connection testing.
 */
class ModelSelectionTest {

    companion object {
        private const val TEST_ENDPOINT = "https://test.openai.azure.com"
        private const val TEST_API_KEY = "test-key"
        private const val TEST_MODEL = "gpt-4.1"
    }

    private lateinit var repository: PreferencesRepositoryImpl
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var securePreferences: SecurePreferences
    private lateinit var azureOpenAiApi: AzureOpenAiApi
    private lateinit var gson: Gson

    @Before
    fun setup() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        azureOpenAiApi = mockk(relaxed = true)
        gson = Gson()

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        repository = PreferencesRepositoryImpl(sharedPreferences, securePreferences, azureOpenAiApi, gson)
    }

    @Test
    fun `getApiConfiguration returns default model gpt-4-1`() = runTest {
        // Given: No model configured
        every { securePreferences.azureOpenAiApiKey } returns TEST_API_KEY
        every { sharedPreferences.getString("pref_azure_endpoint", "") } returns TEST_ENDPOINT
        every { sharedPreferences.getString("pref_azure_model", "gpt-4.1") } returns TEST_MODEL

        // When: Getting API configuration
        val config = repository.getApiConfiguration().first()

        // Then: Default model is gpt-4.1
        assertThat(config.modelName).isEqualTo(TEST_MODEL)
    }

    @Test
    fun `saveApiConfiguration persistsCustomModelName`() = runTest {
        // Given: Custom model name
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = TEST_ENDPOINT,
            modelName = "gpt-4o-mini",
        )

        every { securePreferences.setAzureOpenAiApiKey(any()) } returns Unit
        every { securePreferences.setAzureOpenAiEndpoint(any()) } returns Unit
        every { securePreferences.setAzureOpenAiModelName(any()) } returns Unit

        // When: Saving configuration with custom model
        val result = repository.saveApiConfiguration(config)

        // Then: Custom model name saved
        verify { securePreferences.setAzureOpenAiModelName("gpt-4o-mini") }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `saveApiConfiguration acceptsArbitraryDeploymentName`() = runTest {
        // Given: Arbitrary deployment name
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = TEST_ENDPOINT,
            modelName = "my-custom-deployment-123",
        )

        every { securePreferences.setAzureOpenAiApiKey(any()) } returns Unit
        every { securePreferences.setAzureOpenAiEndpoint(any()) } returns Unit
        every { securePreferences.setAzureOpenAiModelName(any()) } returns Unit

        // When: Saving configuration
        val result = repository.saveApiConfiguration(config)

        // Then: Arbitrary name accepted
        verify { securePreferences.setAzureOpenAiModelName("my-custom-deployment-123") }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `testConnection withBlankModel returnsError`() = runTest {
        // When: Testing connection with blank model
        val result = repository.testConnection(
            apiKey = "test-key",
            endpoint = "https://test.openai.azure.com",
            modelName = "",
        )

        // Then: Configuration incomplete error
        assertThat(result.isSuccess).isTrue()
        val failure = result.getOrNull() as? com.foodie.app.domain.model.TestConnectionResult.Failure
        assertThat(failure?.errorMessage).isEqualTo("API configuration incomplete")
    }
}
