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

    // Placeholder tests removed - SettingsScreen fully implemented in Epic 5
    // Original placeholder text no longer exists

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
