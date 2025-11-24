package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for ApiConfiguration validation logic.
 *
 * Tests all validation rules for Azure OpenAI API configuration.
 * Story 5.2: Azure OpenAI API Key and Endpoint Configuration
 */
class ApiConfigurationTest {

    companion object {
        private const val TEST_ENDPOINT = "https://test.openai.azure.com"
        private const val TEST_API_KEY = "sk-test123"
        private const val TEST_MODEL = "gpt-4.1"
    }

    @Test
    fun `validate emptyApiKey returnsError`() {
        val config = ApiConfiguration(
            apiKey = "",
            endpoint = TEST_ENDPOINT,
            modelName = TEST_MODEL
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("API key required")
    }

    @Test
    fun `validate invalidEndpointFormat returnsError`() {
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = "invalid-url",
            modelName = TEST_MODEL
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
    }

    @Test
    fun `validate nonHttpsEndpoint returnsError`() {
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = "http://test.openai.azure.com",
            modelName = TEST_MODEL
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("Endpoint must use HTTPS")
    }

    @Test
    fun `validate wrongDomain returnsError`() {
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = "https://wrong-domain.com",
            modelName = TEST_MODEL
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("Invalid Azure OpenAI endpoint format")
    }

    @Test
    fun `validate emptyModelName returnsError`() {
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = TEST_ENDPOINT,
            modelName = ""
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("Model name required")
    }

    @Test
    fun `validate allFieldsValid returnsSuccess`() {
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = TEST_ENDPOINT,
            modelName = TEST_MODEL
        )

        val result = config.validate()

        assertThat(result).isEqualTo(ValidationResult.Success)
    }

    @Test
    fun `isConfigured returnsFalse whenApiKeyBlank`() {
        val config = ApiConfiguration(
            apiKey = "",
            endpoint = TEST_ENDPOINT,
            modelName = TEST_MODEL
        )

        assertThat(config.isConfigured).isFalse()
    }

    @Test
    fun `isConfigured returnsTrue whenAllFieldsSet`() {
        val config = ApiConfiguration(
            apiKey = TEST_API_KEY,
            endpoint = TEST_ENDPOINT,
            modelName = TEST_MODEL
        )

        assertThat(config.isConfigured).isTrue()
    }

    // Story 5.3: Model Selection and Configuration tests

    @Test
    fun `default modelName is gpt-4-1`() {
        val config = ApiConfiguration()

        assertThat(config.modelName).isEqualTo("gpt-4.1")
    }

    @Test
    fun `validate acceptsCustomModelName`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://test.openai.azure.com",
            modelName = "gpt-4o-mini"
        )

        val result = config.validate()

        assertThat(result).isEqualTo(ValidationResult.Success)
    }

    @Test
    fun `validate acceptsCustomDeploymentName`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://test.openai.azure.com",
            modelName = "my-custom-deployment-123"
        )

        val result = config.validate()

        assertThat(result).isEqualTo(ValidationResult.Success)
    }

    @Test
    fun `isConfigured returnsFalse whenModelNameBlank`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://test.openai.azure.com",
            modelName = ""
        )

        assertThat(config.isConfigured).isFalse()
    }
}
