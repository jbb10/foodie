package com.foodie.app.ui.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * Navigation uses Material 3 slide transitions for native Android feel:
 * - Forward navigation: slide left (300ms, emphasized easing)
 * - Back navigation: slide right (250ms, emphasized easing)
 * - Easing: FastOutSlowInEasing (Material 3 standard)
 *
 * Note: Compose Navigation doesn't provide Material Motion transitions out-of-the-box.
 * These manual transitions implement Material 3's Shared Axis pattern (X-axis) which
 * Google provides for View-based Android but not yet for Compose.
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
    val context = LocalContext.current
    val activity = context as? Activity
    
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
            ),
            enterTransition = {
                // Slide from right when returning from detail/settings
                when (initialState.destination.route) {
                    Screen.MealDetail.route, Screen.Settings.route ->
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    else -> fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            },
            exitTransition = {
                // Slide to left when navigating to detail/settings
                when (targetState.destination.route) {
                    Screen.MealDetail.route, Screen.Settings.route ->
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        )
                    else -> fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                }
            }
        ) {
            MealListScreen(
                onMealClick = { meal ->
                    navController.navigate(
                        Screen.MealDetail.createRoute(
                            recordId = meal.id,
                            calories = meal.calories,
                            description = meal.description,
                            timestamp = meal.timestamp.toEpochMilli()
                        )
                    )
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Meal Detail screen (edit meal with parameters)
        composable(
            route = Screen.MealDetail.route,
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.StringType
                },
                navArgument("calories") {
                    type = NavType.StringType
                },
                navArgument("description") {
                    type = NavType.StringType
                },
                navArgument("timestamp") {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "foodie://meals/{recordId}?calories={calories}&description={description}&timestamp={timestamp}" } // Updated for Story 3-2
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            }
        ) {
            MealDetailScreen(
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
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        ) {
            CapturePhotoScreen(
                onPhotoConfirmed = { photoUri ->
                    // Story 2-5: Background processing started via WorkManager
                    // Finish activity to return to home screen / previous app
                    activity?.finish()
                },
                onNavigateBack = {
                    // User cancelled - finish activity
                    activity?.finish()
                }
            )
        }

        // Settings screen
        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            }
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
