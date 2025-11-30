package com.foodie.app.ui.screens.energybalance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.foodie.app.ui.theme.FoodieTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * UI instrumentation tests for EnergyBalanceDashboard date navigation.
 *
 * Tests DateNavigationRow composable interactions:
 * - Previous/Next day buttons navigation
 * - Date label formatting ("Today", "Yesterday", formatted date)
 * - "Today" button visibility and functionality
 * - Next Day button disabled when viewing today
 *
 * **Test Coverage:**
 * - AC #1: Previous Day button navigates to yesterday
 * - AC #2: Date label updates correctly
 * - AC #4: Next Day button navigates forward
 * - AC #5: Next Day button disabled when viewing today
 * - AC #6: Today button navigates to today and hides when already on today
 */
class EnergyBalanceDashboardDateNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dateNavigationRow_whenViewingToday_thenShowsTodayLabel() {
        // Given: DateNavigationRow viewing today
        val today = LocalDate.now()

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = today,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: Date label shows "Today"
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    }

    @Test
    fun dateNavigationRow_whenViewingYesterday_thenShowsYesterdayLabel() {
        // Given: DateNavigationRow viewing yesterday
        val yesterday = LocalDate.now().minusDays(1)

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = yesterday,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: Date label shows "Yesterday"
        composeTestRule.onNodeWithText("Yesterday").assertIsDisplayed()
    }

    @Test
    fun dateNavigationRow_whenViewingHistoricalDate_thenShowsFormattedDate() {
        // Given: DateNavigationRow viewing 7 days ago
        val sevenDaysAgo = LocalDate.now().minusDays(7)
        val formattedDate = sevenDaysAgo.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = sevenDaysAgo,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: Date label shows formatted date (e.g., "Nov 22, 2025")
        composeTestRule.onNodeWithText(formattedDate).assertIsDisplayed()
    }

    @Test
    fun previousDayButton_whenClicked_thenInvokesCallback() {
        // Given: DateNavigationRow with callback
        var previousDayClicked = false

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = LocalDate.now(),
                    onPreviousDay = { previousDayClicked = true },
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // When: User taps Previous Day button
        composeTestRule.onNodeWithContentDescription("Previous Day").performClick()

        // Then: Callback is invoked
        assertThat(previousDayClicked).isTrue()
    }

    @Test
    fun nextDayButton_whenViewingYesterday_thenIsEnabled() {
        // Given: DateNavigationRow viewing yesterday
        val yesterday = LocalDate.now().minusDays(1)

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = yesterday,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: Next Day button is enabled
        composeTestRule.onNodeWithContentDescription("Next Day").assertIsEnabled()
    }

    @Test
    fun nextDayButton_whenViewingToday_thenIsDisabled() {
        // Given: DateNavigationRow viewing today
        val today = LocalDate.now()

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = today,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: Next Day button is disabled (cannot view future)
        composeTestRule.onNodeWithContentDescription("Next Day").assertIsNotEnabled()
    }

    @Test
    fun nextDayButton_whenEnabledAndClicked_thenInvokesCallback() {
        // Given: DateNavigationRow viewing yesterday
        var nextDayClicked = false

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = LocalDate.now().minusDays(1),
                    onPreviousDay = {},
                    onNextDay = { nextDayClicked = true },
                    onTodayClicked = {},
                )
            }
        }

        // When: User taps Next Day button
        composeTestRule.onNodeWithContentDescription("Next Day").performClick()

        // Then: Callback is invoked
        assertThat(nextDayClicked).isTrue()
    }

    @Test
    fun todayButton_whenViewingToday_thenIsNotDisplayed() {
        // Given: DateNavigationRow viewing today
        val today = LocalDate.now()

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = today,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: "Today" button is not displayed (already on today)
        composeTestRule.onNodeWithText("Today", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun todayButton_whenViewingHistoricalDate_thenIsDisplayed() {
        // Given: DateNavigationRow viewing yesterday
        val yesterday = LocalDate.now().minusDays(1)

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = yesterday,
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = {},
                )
            }
        }

        // Then: "Today" button is displayed
        composeTestRule.onNodeWithText("Today", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun todayButton_whenClicked_thenInvokesCallback() {
        // Given: DateNavigationRow viewing yesterday with callback
        var todayClicked = false

        composeTestRule.setContent {
            FoodieTheme {
                DateNavigationRow(
                    selectedDate = LocalDate.now().minusDays(1),
                    onPreviousDay = {},
                    onNextDay = {},
                    onTodayClicked = { todayClicked = true },
                )
            }
        }

        // When: User taps "Today" button
        composeTestRule.onNodeWithText("Today", useUnmergedTree = true).performClick()

        // Then: Callback is invoked
        assertThat(todayClicked).isTrue()
    }

    @Test
    fun emptyState_whenViewingHistoricalDate_thenShowsHistoricalMessage() {
        // Given: EmptyState for historical date
        composeTestRule.setContent {
            FoodieTheme {
                EmptyState(isHistoricalDate = true)
            }
        }

        // Then: Shows "No meals logged on this day"
        composeTestRule.onNodeWithText("No meals logged on this day").assertIsDisplayed()
    }

    @Test
    fun emptyState_whenViewingToday_thenShowsTodayMessage() {
        // Given: EmptyState for today
        composeTestRule.setContent {
            FoodieTheme {
                EmptyState(isHistoricalDate = false)
            }
        }

        // Then: Shows "Log your first meal to start tracking"
        composeTestRule.onNodeWithText("Log your first meal to start tracking").assertIsDisplayed()
    }
}
