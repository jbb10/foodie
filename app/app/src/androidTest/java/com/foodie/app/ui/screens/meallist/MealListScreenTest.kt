package com.foodie.app.ui.screens.meallist

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
 * Instrumentation tests for [MealListScreen].
 *
 * These tests verify that the meal list screen renders correctly and handles user
 * interactions properly, ensuring navigation callbacks are invoked as expected.
 *
 * Note: These are UI component tests that do NOT require Hilt injection.
 * We test the MealListScreen composable in isolation by providing test data directly.
 */
class MealListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mealListScreen_displaysTopAppBarWithTitle() {
        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_displaysSettingsButton() {
        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_displaysFloatingActionButton() {
        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add meal").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_displaysTestMeals() {
        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = {}
                )
            }
        }

        // Verify test meals are displayed
        composeTestRule.onNodeWithText("Grilled chicken with quinoa and vegetables")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("450 kcal").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_clickMeal_invokesOnMealClickWithCorrectId() {
        var clickedMealId: String? = null

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = { mealId -> clickedMealId = mealId },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Grilled chicken with quinoa and vegetables")
            .performClick()

        assertThat(clickedMealId).isEqualTo("meal-001")
    }

    @Test
    fun mealListScreen_clickSettings_invokesOnSettingsClick() {
        var settingsClicked = false

        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = { settingsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        assertThat(settingsClicked).isTrue()
    }

    @Test
    fun mealListScreen_displaysMultipleMeals() {
        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = {}
                )
            }
        }

        // Verify multiple meals are displayed
        composeTestRule.onNodeWithText("Grilled chicken with quinoa and vegetables")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Greek yogurt with berries and granola")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Salmon salad with avocado")
            .assertIsDisplayed()
    }
}
