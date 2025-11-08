package com.foodie.app.ui.screens.mealdetail

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
 * Instrumentation tests for [MealDetailScreen].
 *
 * These tests verify that the meal detail screen renders correctly with the meal ID
 * parameter and handles back navigation properly.
 */
class MealDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mealDetailScreen_displaysTopAppBarWithTitle() {
        composeTestRule.setContent {
            FoodieTheme {
                MealDetailScreen(
                    mealId = "test-meal-123",
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Meal").assertIsDisplayed()
    }

    @Test
    fun mealDetailScreen_displaysBackButton() {
        composeTestRule.setContent {
            FoodieTheme {
                MealDetailScreen(
                    mealId = "test-meal-123",
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun mealDetailScreen_displaysMealIdInContent() {
        val testMealId = "test-meal-456"

        composeTestRule.setContent {
            FoodieTheme {
                MealDetailScreen(
                    mealId = testMealId,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Editing meal: $testMealId").assertIsDisplayed()
    }

    @Test
    fun mealDetailScreen_clickBackButton_invokesOnNavigateBack() {
        var backNavigationInvoked = false

        composeTestRule.setContent {
            FoodieTheme {
                MealDetailScreen(
                    mealId = "test-meal-123",
                    onNavigateBack = { backNavigationInvoked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backNavigationInvoked).isTrue()
    }

    @Test
    fun mealDetailScreen_displaysPlaceholderMessage() {
        composeTestRule.setContent {
            FoodieTheme {
                MealDetailScreen(
                    mealId = "test-meal-123",
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            "Meal editing functionality will be implemented in Epic 3"
        ).assertIsDisplayed()
    }
}
