package com.foodie.app.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [Screen] sealed class route generation.
 *
 * These tests verify that navigation routes are generated correctly for all screen
 * destinations, ensuring type-safe navigation works as expected.
 */
class ScreenTest {

    @Test
    fun `MealList route should be meal_list`() {
        assertThat(Screen.MealList.route).isEqualTo("meal_list")
    }

    @Test
    fun `MealDetail route should contain all required query parameters`() {
        assertThat(Screen.MealDetail.route)
            .isEqualTo("meal_detail/{recordId}?calories={calories}&description={description}&protein={protein}&carbs={carbs}&fat={fat}&timestamp={timestamp}")
    }

    @Test
    fun `MealDetail createRoute should produce correct route with parameters`() {
        val route = Screen.MealDetail.createRoute(
            recordId = "test-meal-123",
            calories = 450,
            description = "Grilled chicken",
            protein = 0.0,
            carbs = 0.0,
            fat = 0.0,
            timestamp = 1731421800000L,
        )

        assertThat(route)
            .isEqualTo("meal_detail/test-meal-123?calories=450&description=Grilled+chicken&protein=0.0&carbs=0.0&fat=0.0&timestamp=1731421800000")
    }

    @Test
    fun `MealDetail createRoute should URL encode description`() {
        val route = Screen.MealDetail.createRoute(
            recordId = "abc-123-xyz",
            calories = 620,
            description = "Yogurt & berries",
            protein = 0.0,
            carbs = 0.0,
            fat = 0.0,
            timestamp = 1731422400000L,
        )

        assertThat(route)
            .isEqualTo("meal_detail/abc-123-xyz?calories=620&description=Yogurt+%26+berries&protein=0.0&carbs=0.0&fat=0.0&timestamp=1731422400000")
    }

    @Test
    fun `Settings route should be settings`() {
        assertThat(Screen.Settings.route).isEqualTo("settings")
    }

    @Test
    fun `all screen routes should be unique`() {
        val routes = listOf(
            Screen.MealList.route,
            Screen.MealDetail.route,
            Screen.Settings.route,
        )

        assertThat(routes).containsNoDuplicates()
    }
}
