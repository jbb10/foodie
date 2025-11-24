package com.foodie.app.ui.screens.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for edge case handling in CapturePhotoScreen.
 *
 * Tests storage full error dialog, permission denial screens, and visual feedback.
 *
 * Story: 4.6 - Graceful Degradation (AC: All)
 */
class CapturePhotoEdgeCasesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Story 4.6, AC #4: Storage full error shows dialog
     * Test: StorageFull state displays error dialog with correct message
     */
    @Test
    fun storageFull_displaysErrorDialog() {
        // Given: CapturePhotoScreen with StorageFull state
        composeTestRule.setContent {
            FoodieTheme {
                StorageFullScreen(onCancel = {})
            }
        }

        // Then: Error dialog is displayed
        composeTestRule.onNodeWithText(
            "Storage Full"
        ).assertIsDisplayed()
        
        composeTestRule.onNodeWithText(
            "Storage full. Free up space to continue."
        ).assertIsDisplayed()
        
        // And: OK button is present
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    /**
     * Story 4.6, AC #4: Storage full error can be dismissed
     * Test: OK button dismisses the error dialog
     */
    @Test
    fun storageFull_canBeDismissed() {
        // Given: CapturePhotoScreen with StorageFull state
        var cancelled = false
        composeTestRule.setContent {
            FoodieTheme {
                StorageFullScreen(onCancel = { cancelled = true })
            }
        }

        // When: OK button is clicked
        composeTestRule.onNodeWithText("OK").performClick()

        // Then: onCancel callback is invoked
        assertThat(cancelled).isTrue()
    }

    /**
     * Story 4.6, AC #1: Permission denied screen displays with settings button
     * Test: PermissionDenied state shows dialog with "Open Settings" action
     */
    @Test
    fun permissionDenied_displaysErrorDialog() {
        // Given: CapturePhotoScreen with PermissionDenied state
        composeTestRule.setContent {
            FoodieTheme {
                PermissionDeniedScreen(
                    onOpenSettings = {},
                    onCancel = {}
                )
            }
        }

        // Then: Error dialog is displayed
        composeTestRule.onNodeWithText(
            "Camera Permission Required"
        ).assertIsDisplayed()
        
        composeTestRule.onNodeWithText(
            "Camera access is needed to photograph meals. Please grant permission in app settings."
        ).assertIsDisplayed()
        
        // And: "Open Settings" button is present
        composeTestRule.onNodeWithText("Open Settings").assertIsDisplayed()
    }

    /**
     * Story 4.6, AC #1: Settings button triggers navigation
     * Test: "Open Settings" button invokes callback
     */
    @Test
    fun permissionDenied_openSettingsButton_invokesCallback() {
        // Given: CapturePhotoScreen with PermissionDenied state
        var settingsOpened = false
        composeTestRule.setContent {
            FoodieTheme {
                PermissionDeniedScreen(
                    onOpenSettings = { settingsOpened = true },
                    onCancel = {}
                )
            }
        }

        // When: "Open Settings" button is clicked
        composeTestRule.onNodeWithText("Open Settings").performClick()

        // Then: onOpenSettings callback is invoked
        assertThat(settingsOpened).isTrue()
    }

    /**
     * Story 4.6, AC #7: Visual checkmark animation exists
     * Test: PreviewScreen includes checkmark animation logic
     * 
     * Note: This is a smoke test verifying the component renders.
     * Visual animation timing verified via manual testing.
     */
    @Test
    fun previewScreen_rendersSuccessfully() {
        // Given: PreviewScreen with mock photo URI
        val mockUri = android.net.Uri.parse("content://test/photo.jpg")
        
        composeTestRule.setContent {
            FoodieTheme {
                PreviewScreen(
                    photoUri = mockUri,
                    onRetake = {},
                    onUsePhoto = {},
                    confirmationEnabled = true
                )
            }
        }

        // Then: Retake and Use Photo buttons are displayed
        composeTestRule.onNodeWithText("Retake").assertIsDisplayed()
        composeTestRule.onNodeWithText("Use Photo").assertIsDisplayed()
    }
}
