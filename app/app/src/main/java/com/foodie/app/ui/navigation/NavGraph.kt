package com.foodie.app.ui.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.local.preferences.OnboardingPreferences
import com.foodie.app.ui.components.HealthConnectPermissionGate
import com.foodie.app.ui.screens.capture.CapturePhotoScreen
import com.foodie.app.ui.screens.energybalance.EnergyBalanceDashboardScreen
import com.foodie.app.ui.screens.mealdetail.MealDetailScreen
import com.foodie.app.ui.screens.meallist.MealListScreen
import com.foodie.app.ui.screens.onboarding.OnboardingScreen
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
 * - Onboarding: Conditional start destination based on first-launch flag (Story 5.7)
 *
 * Navigation flow:
 * ```
 * Onboarding (first launch) → MealList (start) ←→ MealDetail
 *    ↓                             ↓
 * Settings                   EnergyBalance
 * ```
 *
 * Bottom Navigation (Story 6.6):
 * - Meals: MealList screen with meal list icon
 * - Energy: EnergyBalance screen with scale icon
 * - Visible on: MealList, EnergyBalance screens only
 * - Hidden on: Onboarding, Settings, MealDetail, CameraCapture
 *
 * @param healthConnectManager HealthConnectManager for permission checking
 * @param onboardingPreferences OnboardingPreferences for first-launch detection (Story 5.7)
 * @param navController The navigation controller (typically from [rememberNavController])
 * @param initialRoute Optional route to navigate to on first launch (e.g., from notification)
 * @param modifier Optional modifier for the NavHost
 */
@Composable
fun NavGraph(
    healthConnectManager: HealthConnectManager,
    onboardingPreferences: OnboardingPreferences,
    navController: NavHostController = rememberNavController(),
    initialRoute: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Determine start destination based on onboarding status (Story 5.7)
    val startDestination = if (onboardingPreferences.isOnboardingCompleted()) {
        Screen.MealList.route
    } else {
        Screen.Onboarding.route
    }

    // Handle initial route navigation (e.g., from notification deep link)
    androidx.compose.runtime.LaunchedEffect(initialRoute) {
        if (initialRoute != null && navController.currentDestination?.route != initialRoute) {
            navController.navigate(initialRoute) {
                // Avoid multiple copies of the same destination
                launchSingleTop = true
            }
        }
    }

    // Determine which screens should show bottom navigation (Story 6.6)
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentDestination = currentRoute?.destination?.route
    val showBottomNav = currentDestination in listOf(Screen.MealList.route, Screen.EnergyBalance.route)

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    currentRoute = currentDestination ?: Screen.MealList.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to start destination to avoid large back stack
                            popUpTo(Screen.MealList.route) { saveState = true }
                            // Avoid multiple copies of same screen
                            launchSingleTop = true
                            // Restore state when navigating back
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(paddingValues),
        ) {
        // Onboarding screen (Story 5.7 - first launch only)
        composable(
            route = Screen.Onboarding.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
        ) {
            OnboardingScreen(
                onOnboardingComplete = {
                    // Navigate to MealList and remove onboarding from backstack
                    navController.navigate(Screen.MealList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSkipOnboarding = {
                    // Same as complete - navigate to MealList and remove onboarding
                    navController.navigate(Screen.MealList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    // Navigate to Settings, keep onboarding in backstack
                    navController.navigate(Screen.Settings.route)
                },
                healthConnectManager = healthConnectManager,
            )
        }

        // Meal List screen (home/start destination)
        composable(
            route = Screen.MealList.route,
            deepLinks = listOf(
                // Legacy - Story 1-3
                navDeepLink { uriPattern = "foodie://home" },
                // Primary - Story 2-0
                navDeepLink { uriPattern = "foodie://meals" },
            ),
            enterTransition = {
                // Slide from right when returning from detail/settings
                when (initialState.destination.route) {
                    Screen.MealDetail.route, Screen.Settings.route ->
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
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
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        )
                    else -> fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                }
            },
        ) {
            MealListScreen(
                onMealClick = { meal ->
                    navController.navigate(
                        Screen.MealDetail.createRoute(
                            recordId = meal.id,
                            calories = meal.calories,
                            description = meal.description,
                            timestamp = meal.timestamp.toEpochMilli(),
                        ),
                    )
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
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
                },
            ),
            deepLinks = listOf(
                // Updated for Story 3-2
                navDeepLink {
                    uriPattern = "foodie://meals/{recordId}?" +
                        "calories={calories}&description={description}&timestamp={timestamp}"
                },
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                )
            },
        ) {
            MealDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // Camera Capture screen (Story 2-3)
        composable(
            route = Screen.CameraCapture.route,
            deepLinks = listOf(
                // Widget - Story 2-2
                navDeepLink { uriPattern = "foodie://capture" },
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
            },
        ) {
            HealthConnectPermissionGate(
                healthConnectManager = healthConnectManager,
                onPermissionsDenied = {
                    // User cancelled permission flow - finish activity
                    activity?.finish()
                },
            ) {
                CapturePhotoScreen(
                    onPhotoConfirmed = { _ ->
                        // Story 2-5: Background processing started via WorkManager
                        // Finish activity to return to home screen / previous app
                        activity?.finish()
                    },
                    onNavigateBack = {
                        // User cancelled - finish activity
                        activity?.finish()
                    },
                )
            }
        }

        // Settings screen
        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                )
            },
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // Energy Balance Dashboard screen (Story 6.6)
        composable(
            route = Screen.EnergyBalance.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "foodie://energy-balance" },
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                )
            },
        ) {
            EnergyBalanceDashboardScreen(
                healthConnectManager = healthConnectManager,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }
        }
    }
}

/**
 * Bottom navigation bar for primary app navigation (Story 6.6).
 *
 * Displays two primary destinations:
 * - Meals: Meal list screen with meal entry list
 * - Energy: Energy balance dashboard with caloric tracking
 *
 * Only shown on MealList and EnergyBalance screens, hidden on detail/settings/capture screens.
 *
 * @param currentRoute Current active route from NavController
 * @param onNavigate Callback when navigation item tapped, receives route string
 */
@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.MealList.route,
            onClick = { onNavigate(Screen.MealList.route) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                )
            },
            label = { Text("Meals") },
        )
        NavigationBarItem(
            selected = currentRoute == Screen.EnergyBalance.route,
            onClick = { onNavigate(Screen.EnergyBalance.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            },
            label = { Text("Energy") },
        )
    }
}
