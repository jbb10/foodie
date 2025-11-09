package com.foodie.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.foodie.app.ui.screens.capture.CapturePhotoScreen
import com.foodie.app.ui.screens.mealdetail.MealDetailScreen
import com.foodie.app.ui.screens.meallist.MealListScreen
import com.foodie.app.ui.screens.settings.SettingsScreen

/**
 * Main navigation graph for the Foodie application.
 *
 * This configures the app's single-activity navigation using Jetpack Navigation Compose.
 * All screens are Composable functions, and navigation actions are wired through callbacks
 * to maintain unidirectional data flow and screen testability.
 *
 * Architecture:
 * - Single Activity pattern: MainActivity hosts this NavHost
 * - Type-safe routes: All routes defined in [Screen] sealed class
 * - Unidirectional flow: Screens receive callbacks, not NavController
 * - Deep linking: Configured for widget integration (foodie://home)
 *
 * Navigation flow:
 * ```
 * MealList (start) ←→ MealDetail
 *    ↓
 * Settings
 * ```
 *
 * @param navController The navigation controller (typically from [rememberNavController])
 * @param modifier Optional modifier for the NavHost
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MealList.route,
        modifier = modifier
    ) {
        // Meal List screen (home/start destination)
        composable(
            route = Screen.MealList.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "foodie://home" }, // Legacy - Story 1-3
                navDeepLink { uriPattern = "foodie://meals" } // Primary - Story 2-0
            )
        ) {
            MealListScreen(
                onMealClick = { mealId ->
                    navController.navigate(Screen.MealDetail.createRoute(mealId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Meal Detail screen (edit meal with mealId parameter)
        composable(
            route = Screen.MealDetail.route,
            arguments = listOf(
                navArgument("mealId") {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "foodie://meals/{mealId}" } // Story 2-0
            )
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId") ?: ""
            MealDetailScreen(
                mealId = mealId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Camera Capture screen (Story 2-3)
        composable(
            route = Screen.CameraCapture.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "foodie://capture" } // Widget - Story 2-2
            )
        ) {
            CapturePhotoScreen(
                onPhotoConfirmed = { photoUri ->
                    // TODO Story 2-5: Navigate to background processing
                    // For now, return to meal list
                    navController.navigate(Screen.MealList.route) {
                        popUpTo(Screen.MealList.route) { inclusive = false }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
