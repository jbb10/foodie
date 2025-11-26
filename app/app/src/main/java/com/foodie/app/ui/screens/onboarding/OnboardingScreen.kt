package com.foodie.app.ui.screens.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodie.app.R
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main onboarding screen with 4-page HorizontalPager.
 *
 * Displays a swipeable flow of onboarding screens:
 * 1. Welcome screen (core concept)
 * 2. Widget setup instructions
 * 3. Health Connect permissions
 * 4. Settings/API configuration prompt
 *
 * **Navigation:**
 * - Skip button on all screens → onSkipOnboarding callback
 * - Done button on final screen → onOnboardingComplete callback
 * - Callbacks trigger navigation to MealListScreen and mark onboarding completed
 *
 * **Architecture:**
 * - MVVM: ViewModel manages state, screens are stateless Composables
 * - HorizontalPager: Compose Foundation API (native, no Accompanist dependency)
 * - Material 3: All components use Material Design 3 theming
 *
 * Story: 5.7 - User Onboarding (First Launch)
 * AC: #1-10 (all acceptance criteria)
 *
 * @param onOnboardingComplete Callback invoked when user completes onboarding (taps "Done" on final screen)
 * @param onSkipOnboarding Callback invoked when user taps "Skip" on any screen
 * @param onNavigateToSettings Callback invoked when user taps "Open Settings" on Screen 4
 * @param healthConnectManager HealthConnectManager for creating permission contract
 * @param viewModel OnboardingViewModel for state management
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onNavigateToSettings: () -> Unit,
    healthConnectManager: HealthConnectManager,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.totalPages })
    val coroutineScope = rememberCoroutineScope()

    // Register Health Connect permission launcher
    val requestHealthConnectPermissions = rememberLauncherForActivityResult(
        healthConnectManager.createPermissionRequestContract(),
    ) { grantedPermissions ->
        Timber.i("Health Connect permission result: granted=${grantedPermissions.size}, required=${HealthConnectManager.REQUIRED_PERMISSIONS.size}")
        viewModel.onHealthConnectPermissionResult(grantedPermissions)
    }

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> WelcomeScreen(
                onNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onSkip = {
                    viewModel.markOnboardingCompleted()
                    onSkipOnboarding()
                },
            )
            1 -> WidgetSetupScreen(
                onNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                },
                onSkip = {
                    viewModel.markOnboardingCompleted()
                    onSkipOnboarding()
                },
            )
            2 -> PermissionsScreen(
                permissionsGranted = state.healthConnectPermissionsGranted,
                onRequestPermissions = {
                    requestHealthConnectPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                },
                onNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(3)
                    }
                },
                onSkip = {
                    viewModel.markOnboardingCompleted()
                    onSkipOnboarding()
                },
            )
            3 -> SettingsPromptScreen(
                apiConfigured = state.apiConfigured,
                onOpenSettings = onNavigateToSettings,
                onComplete = {
                    viewModel.markOnboardingCompleted()
                    onOnboardingComplete()
                },
                onSkip = {
                    viewModel.markOnboardingCompleted()
                    onSkipOnboarding()
                },
            )
        }
    }
}

/**
 * Welcome screen (Screen 1 of 4).
 *
 * Displays app icon, welcome message, and core concept explanation.
 *
 * AC: #1 - Welcome message displays explaining core concept
 * AC: #7 - Skip button available
 */
@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // App icon
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Foodie app icon",
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome title
        Text(
            text = "Welcome to Foodie",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Core concept explanation
        Text(
            text = "Capture meals in 2 seconds.\nAI analyzes.\nHealth Connect saves.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }

            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

/**
 * Widget setup instructions screen (Screen 2 of 4).
 *
 * Provides clear instructions for adding the home screen widget.
 *
 * AC: #2 - User is prompted to add home screen widget
 * AC: #3 - Widget addition instructions state: "Long-press home screen → Widgets → Foodie"
 * AC: #7 - Skip button available
 */
@Composable
fun WidgetSetupScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = "Add Home Screen Widget",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions
        Text(
            text = "Long-press home screen → Widgets → Foodie",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "The widget launches the camera for fastest meal capture.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }

            Button(onClick = onNext) {
                Text("Next")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Health Connect permissions screen (Screen 3 of 4).
 *
 * Requests Health Connect permissions with clear rationale.
 *
 * AC: #4 - Health Connect permissions requested with clear rationale
 * AC: #7 - Skip button available
 *
 * @param permissionsGranted Whether Health Connect permissions are granted
 * @param onRequestPermissions Callback to trigger permission request
 */
@Composable
fun PermissionsScreen(
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = "Grant Health Connect Access",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rationale
        Text(
            text = "Foodie saves nutrition data to Health Connect for interoperability with other health apps like Google Fit.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permission status / request button
        if (permissionsGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Permissions granted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Permissions Granted ✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Button(onClick = onRequestPermissions) {
                Text("Grant Permissions")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }

            Button(onClick = onNext) {
                Text("Next")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Settings/API configuration prompt screen (Screen 4 of 4).
 *
 * Prompts user to configure Azure OpenAI API in Settings.
 *
 * AC: #5 - Azure OpenAI configuration prompt directs user to Settings
 * AC: #7 - Skip button available
 * AC: #9 - After onboarding, user is on MealListScreen
 *
 * @param apiConfigured Whether Azure OpenAI API is fully configured
 * @param onOpenSettings Callback to navigate to Settings screen
 * @param onComplete Callback when user taps "Done" (completes onboarding)
 */
@Composable
fun SettingsPromptScreen(
    apiConfigured: Boolean,
    onOpenSettings: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = "Configure Azure OpenAI",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Body
        Text(
            text = "Enter your Azure OpenAI API key to enable AI meal analysis.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Get your API key at portal.azure.com",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Configuration status / button
        if (apiConfigured) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "API configured",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "API Configured ✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Buttons (Skip and Done both complete onboarding)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }

            Button(onClick = onComplete) {
                Text("Done")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
