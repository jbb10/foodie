package com.foodie.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for HealthConnectUnavailableDialog.
 *
 * Tests verify the dialog displays correctly and handles user interactions.
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectUnavailableDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_displaysCorrectMessage() {
        // Given
        composeTestRule.setContent {
            HealthConnectUnavailableDialog(onDismiss = {})
        }

        // Then
        composeTestRule.onNodeWithText("Health Connect Required").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Health Connect is required but not installed. Install from Play Store?",
        ).assertIsDisplayed()
    }

    @Test
    fun dialog_hasCancelAndInstallButtons() {
        // Given
        composeTestRule.setContent {
            HealthConnectUnavailableDialog(onDismiss = {})
        }

        // Then
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Install").assertIsDisplayed()
    }

    @Test
    fun cancelButton_invokesOnDismiss() {
        // Given
        var dismissed = false
        composeTestRule.setContent {
            HealthConnectUnavailableDialog(onDismiss = { dismissed = true })
        }

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        assertThat(dismissed).isTrue()
    }

    @Test
    fun installButton_invokesOnDismissAfterLaunchingIntent() {
        // Given
        var dismissed = false
        composeTestRule.setContent {
            HealthConnectUnavailableDialog(onDismiss = { dismissed = true })
        }

        // When
        composeTestRule.onNodeWithText("Install").performClick()

        // Then
        assertThat(dismissed).isTrue()
    }
}
