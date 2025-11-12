package com.foodie.app.ui.screens.meallist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.assertCountEquals
import com.foodie.app.HiltTestActivity
import com.foodie.app.R
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.foodie.app.domain.model.MealEntry
import java.time.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for [MealListScreen].
 *
 * These tests verify that the meal list screen renders correctly and handles user
 * interactions properly, ensuring navigation callbacks are invoked as expected.
 *
 * Note: These tests use the actual ViewModel with Health Connect repository.
 * The screen will display meals from Health Connect if any exist, or show an empty state.
 * Tests verify UI elements are present rather than specific content.
 */
@HiltAndroidTest
class MealListScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun mealListScreen_displaysTopAppBarWithTitle() {
        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealListScreenContent(
                    state = MealListState(),
                    snackbarHostState = snackbarHostState,
                    onRefresh = {},
                    onMealClick = {},
                    onSettingsClick = {},
                    onDeleteConfirmed = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_displaysSettingsButton() {
        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealListScreenContent(
                    state = MealListState(),
                    snackbarHostState = snackbarHostState,
                    onRefresh = {},
                    onMealClick = {},
                    onSettingsClick = {},
                    onDeleteConfirmed = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_clickSettings_invokesOnSettingsClick() {
        var settingsClicked = false

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealListScreenContent(
                    state = MealListState(),
                    snackbarHostState = snackbarHostState,
                    onRefresh = {},
                    onMealClick = {},
                    onSettingsClick = { settingsClicked = true },
                    onDeleteConfirmed = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        assertThat(settingsClicked).isTrue()
    }

    @Test
    fun mealListScreen_clickMeal_invokesOnMealClick() {
        val sampleMeal = MealEntry(
            id = "meal-001",
            timestamp = Instant.now(),
            description = "Grilled chicken with quinoa and vegetables",
            calories = 520
        )
    var clickedMeal: MealEntry? = null

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealListScreenContent(
                    state = MealListState(
                        mealsByDate = mapOf("Today" to listOf(sampleMeal))
                    ),
                    snackbarHostState = snackbarHostState,
                    onRefresh = {},
                    onMealClick = { clickedMeal = it },
                    onSettingsClick = {},
                    onDeleteConfirmed = {}
                )
            }
        }

        composeTestRule.onNodeWithText(sampleMeal.description).performClick()

    assertThat(clickedMeal?.id).isEqualTo(sampleMeal.id)
    assertThat(clickedMeal?.description).isEqualTo(sampleMeal.description)
    }

    @Test
    fun mealListScreen_displaysEmptyStateWhenNoMeals() {
        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealListScreenContent(
                    state = MealListState(emptyStateVisible = true),
                    snackbarHostState = snackbarHostState,
                    onRefresh = {},
                    onMealClick = {},
                    onSettingsClick = {},
                    onDeleteConfirmed = {}
                )
            }
        }

        val emptyStateText = composeTestRule.activity.getString(R.string.meal_list_empty_state)
        composeTestRule.onNodeWithText(emptyStateText).assertIsDisplayed()
    }

    @Test
    fun mealListScreen_longPressShowsDeleteDialog() {
        val sampleMeal = MealEntry(
            id = "1",
            timestamp = Instant.now(),
            description = "Sample meal",
            calories = 450
        )

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealListScreenContent(
                    state = MealListState(
                        mealsByDate = mapOf("Today" to listOf(sampleMeal))
                    ),
                    snackbarHostState = snackbarHostState,
                    onRefresh = {},
                    onMealClick = {},
                    onSettingsClick = {},
                    onDeleteConfirmed = {}
                )
            }
        }

        composeTestRule.onNodeWithText(sampleMeal.description)
            .performTouchInput { longClick() }

        val deleteTitle = composeTestRule.activity.getString(R.string.meal_list_delete_title)
        composeTestRule.onNodeWithText(deleteTitle).assertIsDisplayed()

        val cancelLabel = composeTestRule.activity.getString(R.string.meal_list_delete_cancel)
    composeTestRule.onNodeWithText(cancelLabel).performClick()

    composeTestRule.onAllNodesWithText(deleteTitle).assertCountEquals(0)
    }
}

