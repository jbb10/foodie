package com.foodie.app.util

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.common.truth.Truth.assertThat

/**
 * Test utilities for deep link testing in navigation flows (Android Test version).
 *
 * Provides helper functions for creating deep link intents, launching apps with deep links,
 * and verifying back stack state after deep link navigation.
 *
 * Note: This is the androidTest version. A simpler version exists in test/ for unit tests.
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
 */
object DeepLinkTestHelper {
    
    /**
     * Creates an Android Intent configured for deep link navigation.
     *
     * @param uri The deep link URI (e.g., "foodie://meals" or "foodie://meals/meal-123")
     * @return Intent configured with ACTION_VIEW and the specified deep link URI
     */
    fun createDeepLinkIntent(uri: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
    
    /**
     * Verifies that the navigation back stack contains the expected destination routes.
     *
     * @param navController The navigation controller to inspect
     * @param expectedRoutes List of expected route strings in back stack order (bottom to top)
     */
    fun assertBackStackContains(
        navController: NavController,
        expectedRoutes: List<String>
    ) {
        val actualRoutes = navController.currentBackStack.value
            .filter { it.destination.route != null }
            .map { it.destination.route!! }
        
        assertThat(actualRoutes)
            .containsExactlyElementsIn(expectedRoutes)
            .inOrder()
    }
    
    /**
     * Verifies that the current navigation destination matches the expected route.
     *
     * @param navController The navigation controller to inspect
     * @param expectedRoute The expected current route string
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
     * @param backStackEntry The back stack entry to extract arguments from
     * @param argumentName The name of the argument to retrieve (e.g., "mealId")
     * @return The argument value as String, or null if not present
     */
    fun extractArgument(
        backStackEntry: NavBackStackEntry?,
        argumentName: String
    ): String? {
        return backStackEntry?.arguments?.getString(argumentName)
    }
}
