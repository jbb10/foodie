package com.foodie.app.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.foodie.app.HiltTestActivity
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for [SettingsScreen].
 *
 * Tests verify that the settings screen renders correctly with preference categories,
 * displays category headers, and handles back navigation properly.
 *
 * Uses HiltTestActivity pattern to support hiltViewModel() in SettingsScreen.
 *
 * Story: 5.1 - Settings Screen Foundation
 */
@HiltAndroidTest
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun settingsScreen_displaysTopAppBarWithTitle() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysBackButton() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysPlaceholderText() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Settings will appear here").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_clickBackButton_invokesOnNavigateBack() {
        var backNavigationInvoked = false

        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(
                    onNavigateBack = { backNavigationInvoked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backNavigationInvoked).isTrue()
    }

    @Test
    fun settingsScreen_displaysPlaceholderMessage() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText(
            "Settings functionality will be implemented in Epic 5"
        ).assertIsDisplayed()
    }

    // Story 5.2: API Configuration Tests

    @Test
    fun settingsScreen_displaysApiConfigurationCategory() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysApiKeyField() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("API Key").assertIsDisplayed()
        composeTestRule.onNodeWithText("Azure OpenAI API Key").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysEndpointField() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Azure OpenAI Endpoint").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysModelField() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Model Deployment Name").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSaveButton() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Save Configuration").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysTestConnectionButton() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Test Connection").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysApiHelpText() {
        composeTestRule.setContent {
            FoodieTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText(
            "Get your Azure OpenAI credentials at portal.azure.com → Azure OpenAI Service → Keys and Endpoint",
            substring = true
        ).assertIsDisplayed()
    }
}
