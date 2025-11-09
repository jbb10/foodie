package com.foodie.app.util

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.google.common.truth.Truth.assertThat

/**
 * Test utilities for deep link testing in navigation flows.
 *
 * Provides helper functions for creating deep link intents, launching apps with deep links,
 * and verifying back stack state after deep link navigation.
 *
 * Usage:
 * ```
 * // Create deep link intent
 * val intent = DeepLinkTestHelper.createDeepLinkIntent("foodie://meals")
 *
 * // Verify back stack contains expected destinations
 * DeepLinkTestHelper.assertBackStackContains(
 *     navController,
 *     listOf("meal_list", "meal_detail")
 * )
 * ```
 *
 * @see [Android Deep Link Testing](https://developer.android.com/guide/navigation/navigation-testing)
 */
object DeepLinkTestHelper {
    
    /**
     * Creates an Android Intent configured for deep link navigation.
     *
     * This intent can be used in instrumentation tests to simulate external deep link triggers
     * (e.g., widget taps, notifications, external apps).
     *
     * @param uri The deep link URI (e.g., "foodie://meals" or "foodie://meals/meal-123")
     * @return Intent configured with ACTION_VIEW and the specified deep link URI
     *
     * Example:
     * ```
     * val intent = createDeepLinkIntent("foodie://meals/meal-123")
     * // Use with ActivityScenario.launch(intent) or similar
     * ```
     */
    fun createDeepLinkIntent(uri: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
            // Add FLAG_ACTIVITY_NEW_TASK for proper deep link behavior
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
    
    /**
     * Verifies that the navigation back stack contains the expected destination routes.
     *
     * This assertion helper is useful for validating proper back stack behavior after
     * deep link navigation, ensuring the correct navigation hierarchy is established.
     *
     * @param navController The navigation controller to inspect
     * @param expectedRoutes List of expected route strings in back stack order (bottom to top)
     * @throws AssertionError if back stack doesn't match expected routes
     *
     * Example:
     * ```
     * // After navigating to meal detail via deep link, verify back stack
     * assertBackStackContains(navController, listOf("meal_list", "meal_detail/{mealId}"))
     * ```
     */
    fun assertBackStackContains(
        navController: NavController,
        expectedRoutes: List<String>
    ) {
        val actualRoutes = navController.currentBackStack.value
            .filter { it.destination.route != null } // Filter out graph destinations
            .map { it.destination.route!! }
        
        assertThat(actualRoutes)
            .containsExactlyElementsIn(expectedRoutes)
            .inOrder()
    }
    
    /**
     * Verifies that the current navigation destination matches the expected route.
     *
     * Convenience method for asserting the top of the back stack (current screen).
     *
     * @param navController The navigation controller to inspect
     * @param expectedRoute The expected current route string
     * @throws AssertionError if current destination doesn't match expected route
     *
     * Example:
     * ```
     * assertCurrentDestination(navController, "meal_detail/{mealId}")
     * ```
     */
    fun assertCurrentDestination(
        navController: NavController,
        expectedRoute: String
    ) {
        val currentRoute = navController.currentDestination?.route
        assertThat(currentRoute).isEqualTo(expectedRoute)
    }
    
    /**
     * Extracts navigation arguments from a back stack entry.
     *
     * Helper for verifying that deep link parameters (e.g., mealId) are correctly
     * parsed and passed to the destination.
     *
     * @param backStackEntry The back stack entry to extract arguments from
     * @param argumentName The name of the argument to retrieve (e.g., "mealId")
     * @return The argument value as String, or null if not present
     *
     * Example:
     * ```
     * val mealId = extractArgument(navController.currentBackStackEntry, "mealId")
     * assertThat(mealId).isEqualTo("meal-123")
     * ```
     */
    fun extractArgument(
        backStackEntry: NavBackStackEntry?,
        argumentName: String
    ): String? {
        return backStackEntry?.arguments?.getString(argumentName)
    }
    
    /**
     * Creates a list of common deep link URIs for testing.
     *
     * Provides a standardized set of test URIs to ensure consistency across tests.
     *
     * @return Map of URI descriptions to URI strings
     *
     * Example:
     * ```
     * val uris = getCommonTestUris()
     * val mealListUri = uris["meal_list"] // "foodie://meals"
     * ```
     */
    fun getCommonTestUris(): Map<String, String> = mapOf(
        "meal_list" to "foodie://meals",
        "meal_detail_valid" to "foodie://meals/test-meal-123",
        "home_legacy" to "foodie://home", // Legacy deep link from Story 1-3
        "malformed_no_host" to "foodie://",
        "malformed_invalid_host" to "foodie://invalid",
        "malformed_invalid_meal_id" to "foodie://meals/"
    )
}
