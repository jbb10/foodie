package com.foodie.app.ui.navigation

/**
 * Sealed class defining all navigation routes in the application.
 *
 * This provides type-safe navigation by encapsulating all route strings in compile-time
 * constants, preventing typos and making route changes easier to maintain.
 *
 * Architecture:
 * - Each screen destination is a sealed class object
 * - Routes with parameters provide createRoute() helper methods
 * - NavGraph uses these routes to configure composable destinations
 *
 * Usage example:
 * ```
 * // Simple route navigation
 * navController.navigate(Screen.Settings.route)
 *
 * // Parameterized route navigation
 * navController.navigate(Screen.MealDetail.createRoute(mealId))
 * ```
 */
sealed class Screen(val route: String) {
    /**
     * Meal list screen (home/start destination).
     * Displays all meal entries with navigation to detail and settings.
     */
    data object MealList : Screen("meal_list")

    /**
     * Meal detail/edit screen with meal ID parameter.
     * Displays and allows editing a specific meal entry.
     *
     * Route pattern: "meal_detail/{mealId}"
     * Actual route example: "meal_detail/abc123"
     */
    data object MealDetail : Screen("meal_detail/{mealId}") {
        /**
         * Creates a navigation route with the specified meal ID.
         *
         * @param mealId The unique identifier of the meal to display/edit
         * @return Complete route string with mealId parameter (e.g., "meal_detail/abc123")
         */
        fun createRoute(mealId: String): String = "meal_detail/$mealId"
    }

    /**
     * Settings screen.
     * Displays app configuration and preferences.
     */
    data object Settings : Screen("settings")
}
