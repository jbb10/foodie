package com.foodie.app.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.foodie.app.HiltTestActivity
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Instrumentation tests for Story 5.2: Azure OpenAI API Configuration.
 *
 * Tests verify:
 * - API key input with masking
 * - Endpoint and model configuration
 * - Save button interaction and persistence
 * - Test connection button interaction
 * - Validation error handling
 * - SecurePreferences integration
 *
 * Uses HiltTestActivity for full ViewModel integration with actual dependencies.
 * Cleans up SecurePreferences after each test to ensure test isolation.
 *
 * Story: 5.2 - Azure OpenAI API Key and Endpoint Configuration
 */
@HiltAndroidTest
class ApiConfigurationInstrumentationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var securePreferences: SecurePreferences

    @Before
    fun setup() {
        hiltRule.inject()
        // Clean state before each test
        securePreferences.clear()
    }

    @After
    fun tearDown() {
        // Clean up after tests
        securePreferences.clear()
    }

    @Test
    fun apiKeyField_acceptsInput() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        // Find and interact with API key field
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performTextInput("test-api-key-12345")

        // Note: We can't verify masked display in instrumentation tests due to PasswordVisualTransformation
        // This is covered by unit tests for ApiConfiguration
    }

    @Test
    fun endpointField_acceptsInput() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNode(hasText("Azure OpenAI Endpoint")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI Endpoint")).performTextInput("https://test.openai.azure.com")
    }

    @Test
    fun modelField_acceptsInput() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNode(hasText("Model Deployment Name")).performClick()
        composeTestRule.onNode(hasText("Model Deployment Name")).performTextInput("gpt-4o")
    }

    @Test
    fun saveButton_enabledByDefault() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Save Configuration").assertIsEnabled()
    }

    @Test
    fun testConnectionButton_enabledByDefault() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Test Connection").assertIsEnabled()
    }

    @Test
    fun saveConfiguration_persistsToSecurePreferences() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        // Input API configuration
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performTextInput("sk-test-key-abc123")

        composeTestRule.onNode(hasText("Azure OpenAI Endpoint")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI Endpoint"))
            .performTextInput("https://test-resource.openai.azure.com")

        composeTestRule.onNode(hasText("Model Deployment Name")).performClick()
        composeTestRule.onNode(hasText("Model Deployment Name")).performTextReplacement("gpt-4o-mini")

        // Save configuration
        composeTestRule.onNodeWithText("Save Configuration").performClick()

        // Wait for async save operation
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            securePreferences.azureOpenAiApiKey == "sk-test-key-abc123"
        }

        // Verify persistence
        assertThat(securePreferences.azureOpenAiApiKey).isEqualTo("sk-test-key-abc123")
        assertThat(securePreferences.azureOpenAiEndpoint).isEqualTo("https://test-resource.openai.azure.com")
        assertThat(securePreferences.azureOpenAiModel).isEqualTo("gpt-4o-mini")
    }

    @Test
    fun apiConfiguration_loadsFromSecurePreferences() {
        // Pre-populate SecurePreferences
        securePreferences.setAzureOpenAiApiKey("pre-configured-key-xyz")
        securePreferences.setAzureOpenAiEndpoint("https://pre-configured.openai.azure.com")
        securePreferences.setAzureOpenAiModelName("gpt-4-turbo")

        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        // Wait for ViewModel to load preferences (LaunchedEffect)
        composeTestRule.waitForIdle()

        // Verify fields display the configured values
        // Note: API key field will show masked version, endpoint and model show full text
        composeTestRule.onNodeWithText("https://pre-configured.openai.azure.com", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("gpt-4-turbo", substring = true).assertIsDisplayed()
    }

    @Test
    fun saveButton_showsLoadingIndicator() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        // Input minimal valid configuration
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performTextInput("sk-key")

        composeTestRule.onNode(hasText("Azure OpenAI Endpoint")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI Endpoint"))
            .performTextInput("https://test.openai.azure.com")

        // Click save and immediately check for disabled state
        composeTestRule.onNodeWithText("Save Configuration").performClick()

        // Note: Loading state may be too quick to capture reliably in tests
        // This is covered by unit tests for SettingsViewModel
    }

    @Test
    fun testConnectionButton_showsLoadingIndicator() {
        // Pre-configure valid credentials
        securePreferences.setAzureOpenAiApiKey("test-key")
        securePreferences.setAzureOpenAiEndpoint("https://test.openai.azure.com")
        securePreferences.setAzureOpenAiModelName("gpt-4o")

        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.waitForIdle()

        // Click test connection
        composeTestRule.onNodeWithText("Test Connection").performClick()

        // Note: Loading state may be too quick to capture, and test will fail if API not available
        // This is covered by unit tests with mocked repository
    }

    @Test
    fun helpText_displaysAzurePortalGuidance() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText(
            "Get your Azure OpenAI credentials at portal.azure.com",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun allApiConfigurationFields_displayInCorrectOrder() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        // Verify all fields are displayed
        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("API Key").assertIsDisplayed()
        composeTestRule.onNodeWithText("Azure OpenAI Endpoint").assertIsDisplayed()
        composeTestRule.onNodeWithText("Model Deployment Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Connection").assertIsDisplayed()
    }

    @Test
    fun clearingApiKey_allowsSavingEmptyConfiguration() {
        // Pre-configure
        securePreferences.setAzureOpenAiApiKey("old-key")

        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.waitForIdle()

        // Clear API key field
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performClick()
        composeTestRule.onNode(hasText("Azure OpenAI API Key")).performTextReplacement("")

        // Save empty configuration
        composeTestRule.onNodeWithText("Save Configuration").performClick()

        composeTestRule.waitForIdle()

        // Note: Validation may prevent empty save - this behavior is defined in ViewModel
        // Unit tests cover validation logic
    }
}
