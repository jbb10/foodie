package com.foodie.app.ui.screens.settings

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.foodie.app.HiltTestActivity
import com.foodie.app.ui.navigation.NavGraph
import com.foodie.app.ui.navigation.Screen
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Instrumentation tests for Settings navigation flow.
 *
 * Tests verify complete navigation flow:
 * - MealListScreen → Settings (via toolbar menu)
 * - Settings → MealListScreen (via back button)
 *
 * Uses HiltTestActivity with TestNavHostController for reliable navigation testing.
 * Pattern based on NavGraphTest from the project.
 *
 * Story: 5.1 - Settings Screen Foundation
 */
@HiltAndroidTest
class SettingsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: TestNavHostController

    @Inject
    lateinit var healthConnectManager: com.foodie.app.data.local.healthconnect.HealthConnectManager

    @Before
    fun setup() {
        hiltRule.inject()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            FoodieTheme {
                NavGraph(healthConnectManager = healthConnectManager, navController = navController)
            }
        }
    }

    @Test
    fun clickSettingsMenu_navigatesToSettings() {
        // Given: App starts at MealListScreen
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()

        // When: User clicks Settings menu item
        composeTestRule.onNodeWithContentDescription("Open Settings").performClick()

        // Then: Settings screen is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.Settings.route)
    }

    @Test
    fun clickBackButton_returnsToMealList() {
        // Given: User navigates to Settings
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Settings.route)
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // When: User clicks back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Then: MealListScreen is displayed
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)
    }

    @Test
    fun settingsScreen_displaysAllCategories() {
        // Given: User navigates to Settings
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Settings.route)
        }

        // Then: All preference categories are visible
        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun navigationBackAndForth_maintainsState() {
        // Given: App starts at MealListScreen
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()

        // When: User navigates to Settings
        composeTestRule.onNodeWithContentDescription("Open Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // And: User navigates back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()

        // And: User navigates to Settings again
        composeTestRule.onNodeWithContentDescription("Open Settings").performClick()

        // Then: Settings screen still renders correctly
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("API Configuration").assertIsDisplayed()
    }
}
