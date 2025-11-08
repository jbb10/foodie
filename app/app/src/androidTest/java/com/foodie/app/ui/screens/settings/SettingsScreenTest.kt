package com.foodie.app.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for [SettingsScreen].
 *
 * These tests verify that the settings screen renders correctly and handles
 * back navigation properly.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
}
