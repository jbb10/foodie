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
    fun `MealDetail route should contain mealId placeholder`() {
        assertThat(Screen.MealDetail.route).isEqualTo("meal_detail/{mealId}")
    }

    @Test
    fun `MealDetail createRoute should produce correct route with mealId`() {
        val mealId = "test-meal-123"
        val route = Screen.MealDetail.createRoute(mealId)
        
        assertThat(route).isEqualTo("meal_detail/test-meal-123")
    }

    @Test
    fun `MealDetail createRoute should handle special characters in mealId`() {
        val mealId = "abc-123-xyz"
        val route = Screen.MealDetail.createRoute(mealId)
        
        assertThat(route).isEqualTo("meal_detail/abc-123-xyz")
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
            Screen.Settings.route
        )
        
        assertThat(routes).containsNoDuplicates()
    }
}
