package com.foodie.app.ui.screens.mealdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests verifying the edit meal screen renders form state correctly and
 * wires user interactions to the ViewModel event channel.
 */
class MealDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTimestamp = Instant.parse("2025-11-12T14:30:00Z")

    @Test
    fun mealDetailScreen_displaysPrefilledValuesFromState() {
        val state = MealDetailState(
            recordId = "record-123",
            calories = "650",
            description = "Grilled salmon with rice",
            timestamp = sampleTimestamp,
        )

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealDetailScreenContent(
                    state = state,
                    onEvent = {},
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Meal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("caloriesField").assertIsDisplayed()
        composeTestRule.onNodeWithTag("descriptionField").assertIsDisplayed()
        composeTestRule.onNodeWithText("650").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grilled salmon with rice").assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedTimestampText(sampleTimestamp)).assertIsDisplayed()
    }

    @Test
    fun mealDetailScreen_showsValidationErrorAndDisablesSave() {
        val state = MealDetailState(
            recordId = "record-123",
            calories = "0",
            description = "Test meal",
            timestamp = sampleTimestamp,
            caloriesError = "Calories must be between 1 and 5000",
        )

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealDetailScreenContent(
                    state = state,
                    onEvent = {},
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        composeTestRule.onNodeWithText("Calories must be between 1 and 5000").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saveButton").assertIsNotEnabled()
    }

    @Test
    fun mealDetailScreen_saveButtonClick_emitsSaveEvent() {
        val events = mutableListOf<MealDetailEvent>()
        val state = MealDetailState(
            recordId = "record-456",
            calories = "450",
            description = "Avocado toast",
            timestamp = sampleTimestamp,
        )

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealDetailScreenContent(
                    state = state,
                    onEvent = { events.add(it) },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        composeTestRule.onNodeWithTag("saveButton").performClick()

        assertThat(events.last()).isInstanceOf(MealDetailEvent.SaveClicked::class.java)
    }

    @Test
    fun mealDetailScreen_cancelButtonClick_emitsCancelEvent() {
        val events = mutableListOf<MealDetailEvent>()
        val state = MealDetailState(
            recordId = "record-789",
            calories = "320",
            description = "Egg scramble",
            timestamp = sampleTimestamp,
        )

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealDetailScreenContent(
                    state = state,
                    onEvent = { events.add(it) },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Cancel").performClick()

        assertThat(events.last()).isInstanceOf(MealDetailEvent.CancelClicked::class.java)
    }

    @Test
    fun mealDetailScreen_textInput_emitsCaloriesChangedEvent() {
        val events = mutableListOf<MealDetailEvent>()
        val state = MealDetailState(
            recordId = "record-321",
            calories = "300",
            description = "Smoothie",
            timestamp = sampleTimestamp,
        )

        composeTestRule.setContent {
            FoodieTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                MealDetailScreenContent(
                    state = state,
                    onEvent = { events.add(it) },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        composeTestRule.onNodeWithTag("caloriesField").performTextInput("5")

        val lastEvent = events.lastOrNull()
        assertThat(lastEvent).isInstanceOf(MealDetailEvent.CaloriesChanged::class.java)
        assertThat((lastEvent as MealDetailEvent.CaloriesChanged).value).contains("5")
    }

    private fun expectedTimestampText(timestamp: Instant): String {
        val zoned = timestamp.atZone(ZoneId.systemDefault())
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        return "Captured: ${zoned.format(dateFormatter)} at ${zoned.format(timeFormatter)}"
    }
}
