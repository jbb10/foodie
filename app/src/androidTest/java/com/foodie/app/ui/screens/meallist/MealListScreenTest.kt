package com.foodie.app.ui.screens.meallist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodie.app.HiltTestActivity
import com.foodie.app.data.repository.HealthConnectRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MealListScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var repository: HealthConnectRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun whenScreenLaunches_andHasData_displaysMealList() = runBlocking {
        // Given
        repository.insertNutritionRecord(123, "Test Meal 1", Instant.now())
        repository.insertNutritionRecord(456, "Test Meal 2", Instant.now().minusSeconds(3600))

        // When
        composeTestRule.setContent {
            MealListScreen(onNavigateToDetail = {})
        }

        // Then
        composeTestRule.onNodeWithText("Test Meal 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("123 kcal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Meal 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("456 kcal").assertIsDisplayed()
    }

    @Test
    fun whenScreenLaunches_andHasNoData_displaysEmptyState() {
        // When
        composeTestRule.setContent {
            MealListScreen(onNavigateToDetail = {})
        }

        // Then
        composeTestRule.onNodeWithText("No meals logged yet. Use the widget to capture your first meal!").assertIsDisplayed()
    }
}
