package com.foodie.app.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.foodie.app.HiltTestActivity
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for [NavGraph] navigation configuration.
 *
 * These tests verify the complete navigation flow between screens, back stack handling,
 * and proper configuration of all navigation destinations.
 *
 * Pattern: Uses createAndroidComposeRule<HiltTestActivity>() with @HiltAndroidTest.
 * This provides a Hilt-enabled activity so that hiltViewModel() in composables works.
 *
 * Based on android/architecture-samples AppNavigationTest pattern.
 */
@HiltAndroidTest
class NavGraphTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupNavHost() {
        hiltRule.inject()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            FoodieTheme {
                NavGraph(navController = navController)
            }
        }
    }

    @Test
    fun navGraph_startsAtMealListScreen() {
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)
    }

    @Test
    fun navGraph_navigateToMealDetail_displaysCorrectScreen() {
        val testMealId = "test-meal-123"

        // Navigate to meal detail
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.MealDetail.createRoute(testMealId))
        }

        // Verify screen displays
        composeTestRule.onNodeWithText("Edit Meal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Editing meal: $testMealId").assertIsDisplayed()
        
        // Verify navigation state
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealDetail.route)
    }

    @Test
    fun navGraph_navigateToSettings_displaysCorrectScreen() {
        // Navigate to settings
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Settings.route)
        }

        // Verify screen displays
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings will appear here").assertIsDisplayed()
        
        // Verify navigation state
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Settings.route)
    }

    @Test
    fun navGraph_clickMealInList_navigatesToMealDetail() {
        // Click on a meal in the list
        composeTestRule.onNodeWithText("Grilled chicken with quinoa and vegetables")
            .performClick()

        // Verify navigated to detail screen
        composeTestRule.onNodeWithText("Edit Meal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Editing meal: meal-001").assertIsDisplayed()
    }

    @Test
    fun navGraph_clickSettingsButton_navigatesToSettings() {
        // Click settings button
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Verify navigated to settings screen
        composeTestRule.onNodeWithText("Settings will appear here").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Settings.route)
    }

    @Test
    fun navGraph_fromMealDetail_clickBack_returnsToMealList() {
        // Navigate to meal detail
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.MealDetail.createRoute("test-meal"))
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify returned to meal list
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)
    }

    @Test
    fun navGraph_fromSettings_clickBack_returnsToMealList() {
        // Navigate to settings
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Settings.route)
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify returned to meal list
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)
    }

    @Test
    fun navGraph_backStackHandling_multipleScreensNavigation() {
        // Start at MealList
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)

        // Navigate to MealDetail
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.MealDetail.createRoute("meal-1"))
        }
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealDetail.route)

        // Back to MealList
        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)

        // Navigate to Settings
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Settings.route)
        }
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Settings.route)

        // Back to MealList
        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)
    }

    @Test
    fun navGraph_mealDetailWithDifferentIds_displaysDifferentContent() {
        // Navigate to meal detail with ID 1
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.MealDetail.createRoute("meal-abc"))
        }
        composeTestRule.onNodeWithText("Editing meal: meal-abc").assertIsDisplayed()

        // Go back
        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        // Navigate to meal detail with ID 2
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.MealDetail.createRoute("meal-xyz"))
        }
        composeTestRule.onNodeWithText("Editing meal: meal-xyz").assertIsDisplayed()
    }
}
