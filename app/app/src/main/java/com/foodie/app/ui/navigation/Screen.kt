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
     * Meal detail/edit screen with meal data parameters.
     * Displays and allows editing a specific meal entry.
     *
     * Route pattern: "meal_detail/{recordId}?calories={calories}&description={description}&protein={protein}&carbs={carbs}&fat={fat}&timestamp={timestamp}"
     */
    data object MealDetail : Screen("meal_detail/{recordId}?calories={calories}&description={description}&protein={protein}&carbs={carbs}&fat={fat}&timestamp={timestamp}") {
        /**
         * Creates a navigation route with the specified meal data.
         *
         * @param recordId The unique identifier of the meal record
         * @param calories Energy content in kilocalories
         * @param description Meal description/name
         * @param protein Protein in grams (supports up to 2 decimal places)
         * @param carbs Carbohydrates in grams (supports up to 2 decimal places)
         * @param fat Fat in grams (supports up to 2 decimal places)
         * @param timestamp When the meal was consumed (epoch millis)
         * @return Complete route string with all parameters
         */
        fun createRoute(
            recordId: String,
            calories: Int,
            description: String,
            protein: Double,
            carbs: Double,
            fat: Double,
            timestamp: Long,
        ): String =
            "meal_detail/$recordId?calories=$calories&description=${java.net.URLEncoder.encode(description, "UTF-8")}&protein=$protein&carbs=$carbs&fat=$fat&timestamp=$timestamp"
    }

    /**
     * Settings screen.
     * Displays app configuration and preferences.
     */
    data object Settings : Screen("settings")

    /**
     * Camera capture screen (Story 2-3).
     * Handles photo capture, processing, and preview confirmation.
     * Entry point from widget deep link (foodie://capture).
     */
    data object CameraCapture : Screen("camera_capture")

    /**
     * Onboarding screen (Story 5.7).
     * First-launch flow with 4 screens: Welcome, Widget setup, Permissions, Settings prompt.
     * Shows only once per install via OnboardingPreferences flag.
     */
    data object Onboarding : Screen("onboarding")

    /**
     * Energy Balance Dashboard screen (Story 6.6).
     * Displays daily calories in vs calories out with TDEE breakdown.
     * Shows deficit/surplus with colour coding and real-time updates.
     */
    object EnergyBalance : Screen("energy_balance")
}
