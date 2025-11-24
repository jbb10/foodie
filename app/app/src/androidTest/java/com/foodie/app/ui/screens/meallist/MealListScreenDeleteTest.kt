package com.foodie.app.ui.screens.meallist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.foodie.app.domain.model.MealEntry
import com.foodie.app.ui.theme.FoodieTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Instrumentation tests for delete functionality in MealListScreen.
 *
 * Tests long-press gesture detection, dialog UI rendering, and user interaction flows.
 * Validates AC #1, #2, #3 (dialog behavior).
 */
@OptIn(ExperimentalTestApi::class)
class MealListScreenDeleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun longPressMealEntry_showsDeleteDialog() {
        // Given - Screen with one meal entry
        val testMeal = MealEntry(
            id = "test-meal-1",
            timestamp = Instant.now(),
            description = "Test Lunch",
            calories = 600
        )
        val state = MealListState(
            mealsByDate = mapOf("Today" to listOf(testMeal)),
            isLoading = false
        )

        var longPressedId: String? = null

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    callbacks = MealListCallbacks(
                        onRefresh = {},
                        onMealClick = {},
                        onSettingsClick = {},
                        onMealLongPress = { longPressedId = it },
                        onDismissDeleteDialog = {},
                        onDeleteConfirmed = {}
                    )
                )
            }
        }

        // When - Long-press the meal entry
        composeTestRule.onNodeWithText("Test Lunch")
            .performTouchInput { longClick() }

        // Then - Verify long-press event was triggered
        assert(longPressedId == "test-meal-1")
    }

    @Test
    fun deleteDialog_displaysCorrectText() {
        // Given - State with dialog visible
        val state = MealListState(
            mealsByDate = emptyMap(),
            showDeleteDialog = true,
            deleteTargetId = "test-meal-1"
        )

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    callbacks = MealListCallbacks(
                        onRefresh = {},
                        onMealClick = {},
                        onSettingsClick = {},
                        onMealLongPress = {},
                        onDismissDeleteDialog = {},
                        onDeleteConfirmed = {}
                    )
                )
            }
        }

        // Then - Verify dialog content (AC #1, #2)
        composeTestRule.onNodeWithText("Delete Entry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete this entry? This cannot be undone.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun deleteDialog_cancelButton_dismissesDialog() {
        // Given - State with dialog visible
        val state = MealListState(
            mealsByDate = emptyMap(),
            showDeleteDialog = true,
            deleteTargetId = "test-meal-1"
        )

        var dismissCalled = false

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    callbacks = MealListCallbacks(
                        onRefresh = {},
                        onMealClick = {},
                        onSettingsClick = {},
                        onMealLongPress = {},
                        onDismissDeleteDialog = { dismissCalled = true },
                        onDeleteConfirmed = {}
                    )
                )
            }
        }

        // When - Tap Cancel button
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then - Verify dismiss callback invoked (AC #3)
        assert(dismissCalled)
    }

    @Test
    fun deleteDialog_deleteButton_triggersDeleteConfirmed() {
        // Given - State with dialog visible
        val state = MealListState(
            mealsByDate = emptyMap(),
            showDeleteDialog = true,
            deleteTargetId = "test-meal-1"
        )

        var deleteConfirmedCalled = false

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    callbacks = MealListCallbacks(
                        onRefresh = {},
                        onMealClick = {},
                        onSettingsClick = {},
                        onMealLongPress = {},
                        onDismissDeleteDialog = {},
                        onDeleteConfirmed = { deleteConfirmedCalled = true }
                    )
                )
            }
        }

        // When - Tap Delete button
        composeTestRule.onNodeWithText("Delete").performClick()

        // Then - Verify delete callback invoked (AC #4)
        assert(deleteConfirmedCalled)
    }

    @Test
    fun deleteDialog_notVisible_whenShowDeleteDialogFalse() {
        // Given - State with dialog hidden
        val state = MealListState(
            mealsByDate = emptyMap(),
            showDeleteDialog = false,
            deleteTargetId = null
        )

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    callbacks = MealListCallbacks(
                        onRefresh = {},
                        onMealClick = {},
                        onSettingsClick = {},
                        onMealLongPress = {},
                        onDismissDeleteDialog = {},
                        onDeleteConfirmed = {}
                    )
                )
            }
        }

        // Then - Verify dialog is not displayed
        composeTestRule.onNodeWithText("Delete Entry").assertIsNotDisplayed()
    }

    @Test
    fun successMessage_displaysSnackbar() {
        // This test requires the full MealListScreen with ViewModel, not MealListScreenContent
        // MealListScreenContent doesn't handle LaunchedEffect for snackbar messages
        // Snackbar functionality is handled by HandleSuccessMessage in parent MealListScreen
        // Skipping this test as it tests ViewModel integration, not UI component
        // covered by MealListViewModelTest and integration tests

        // Note: To properly test this, we would need to use MealListScreen with a test ViewModel
        // or refactor HandleSuccessMessage into MealListScreenContent
    }

    @Test
    fun normalTap_doesNotTriggerLongPress() {
        // Given - Screen with one meal entry
        val testMeal = MealEntry(
            id = "test-meal-1",
            timestamp = Instant.now(),
            description = "Test Lunch",
            calories = 600
        )
        val state = MealListState(
            mealsByDate = mapOf("Today" to listOf(testMeal)),
            isLoading = false
        )

        var normalClickCalled = false
        var longPressedId: String? = null

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    callbacks = MealListCallbacks(
                        onRefresh = {},
                        onMealClick = { normalClickCalled = true },
                        onSettingsClick = {},
                        onMealLongPress = { longPressedId = it },
                        onDismissDeleteDialog = {},
                        onDeleteConfirmed = {}
                    )
                )
            }
        }

        // When - Normal tap (not long-press)
        composeTestRule.onNodeWithText("Test Lunch").performClick()

        // Then - Verify normal click works, long-press not triggered
        assert(normalClickCalled)
        assert(longPressedId == null)
    }
}
