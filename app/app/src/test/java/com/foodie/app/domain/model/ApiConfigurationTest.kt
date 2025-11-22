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

    @Test
    fun `validate emptyApiKey returnsError`() {
        val config = ApiConfiguration(
            apiKey = "",
            endpoint = "https://test.openai.azure.com",
            modelName = "gpt-4.1"
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("API key required")
    }

    @Test
    fun `validate invalidEndpointFormat returnsError`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "invalid-url",
            modelName = "gpt-4.1"
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
    }

    @Test
    fun `validate nonHttpsEndpoint returnsError`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "http://test.openai.azure.com",
            modelName = "gpt-4.1"
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("Endpoint must use HTTPS")
    }

    @Test
    fun `validate wrongDomain returnsError`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://wrong-domain.com",
            modelName = "gpt-4.1"
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("Invalid Azure OpenAI endpoint format")
    }

    @Test
    fun `validate emptyModelName returnsError`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://test.openai.azure.com",
            modelName = ""
        )

        val result = config.validate()

        assertThat(result).isInstanceOf(ValidationResult.Error::class.java)
        assertThat((result as ValidationResult.Error).message).isEqualTo("Model name required")
    }

    @Test
    fun `validate allFieldsValid returnsSuccess`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://test.openai.azure.com",
            modelName = "gpt-4.1"
        )

        val result = config.validate()

        assertThat(result).isEqualTo(ValidationResult.Success)
    }

    @Test
    fun `isConfigured returnsFalse whenApiKeyBlank`() {
        val config = ApiConfiguration(
            apiKey = "",
            endpoint = "https://test.openai.azure.com",
            modelName = "gpt-4.1"
        )

        assertThat(config.isConfigured).isFalse()
    }

    @Test
    fun `isConfigured returnsTrue whenAllFieldsSet`() {
        val config = ApiConfiguration(
            apiKey = "sk-test123",
            endpoint = "https://test.openai.azure.com",
            modelName = "gpt-4.1"
        )

        assertThat(config.isConfigured).isTrue()
    }
}
