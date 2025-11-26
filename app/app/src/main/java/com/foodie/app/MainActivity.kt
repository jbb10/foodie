package com.foodie.app

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.local.preferences.OnboardingPreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.domain.model.ThemeMode
import com.foodie.app.ui.components.HealthConnectUnavailableDialog
import com.foodie.app.ui.navigation.NavGraph
import com.foodie.app.ui.theme.FoodieTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main activity for the Foodie application.
 *
 * This activity follows the single-activity architecture pattern, hosting the main
 * navigation graph with all app screens implemented as Composables.
 *
 * Architecture:
 * - Single Activity: All screens are Composables navigated via NavHost
 * - Hilt Integration: @AndroidEntryPoint enables dependency injection
 * - Edge-to-Edge: System bars handled by individual screens via Scaffold
 * - Health Connect: Checks availability and requests permissions on launch
 * - Theme Management: Observes theme preference and applies via FoodieTheme (Story 5.4)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var healthConnectManager: HealthConnectManager

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MainContent()
        }
    }

    @Composable
    private fun MainContent() {
        var showHealthConnectDialog by remember { mutableStateOf(false) }
        val prefs = getSharedPreferences("foodie_prefs", MODE_PRIVATE)
        val isFirstLaunch = remember { prefs.getBoolean("first_launch", true) }

        // Register permission launchers
        val requestHealthConnectPermissions = createHealthConnectPermissionLauncher()

        // Observe theme preference
        val darkTheme = observeThemePreference()

        FoodieTheme(darkTheme = darkTheme) {
            val navigateToRoute = intent?.getStringExtra("navigate_to")

            // Handle deep link actions
            handleDeepLinkActions(requestHealthConnectPermissions)

            // Permission check flow on launch
            handlePermissionCheckFlow(
                isFirstLaunch = isFirstLaunch,
                prefs = prefs,
                onShowDialog = { showHealthConnectDialog = it },
                requestHealthConnect = requestHealthConnectPermissions,
            )

            NavGraph(
                healthConnectManager = healthConnectManager,
                onboardingPreferences = onboardingPreferences,
                initialRoute = navigateToRoute,
            )

            if (showHealthConnectDialog) {
                HealthConnectUnavailableDialog(
                    onDismiss = { showHealthConnectDialog = false },
                )
            }
        }
    }

    @Composable
    private fun createHealthConnectPermissionLauncher() = rememberLauncherForActivityResult(
        healthConnectManager.createPermissionRequestContract(),
    ) { granted ->
        Timber.i("Health Connect permission result: granted=${granted.size}, required=${HealthConnectManager.REQUIRED_PERMISSIONS.size}")

        val allGranted = granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)
        Timber.i(if (allGranted) "Health Connect permissions granted" else "Health Connect permissions denied or incomplete")
    }

    @Composable
    private fun observeThemePreference(): Boolean {
        val themeMode by preferencesRepository.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM_DEFAULT)
        val systemInDarkTheme = isSystemInDarkTheme()
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM_DEFAULT -> systemInDarkTheme
        }
    }

    @Composable
    private fun handleDeepLinkActions(requestHealthConnectPermissions: ActivityResultLauncher<Set<String>>) {
        LaunchedEffect(intent) {
            when (intent?.action) {
                "com.foodie.app.OPEN_SETTINGS" -> {
                    Timber.i("Deep link action: Open Settings")
                    // TODO: Navigate to settings screen when implemented (Story 5.1)
                }
                "com.foodie.app.GRANT_PERMISSIONS" -> {
                    Timber.i("Deep link action: Grant Permissions")
                    if (!healthConnectManager.checkPermissions()) {
                        requestHealthConnectPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                    }
                }
            }
        }
    }

    @Composable
    private fun handlePermissionCheckFlow(
        isFirstLaunch: Boolean,
        prefs: SharedPreferences,
        onShowDialog: (Boolean) -> Unit,
        requestHealthConnect: ActivityResultLauncher<Set<String>>,
    ) {
        LaunchedEffect(Unit) {
            Timber.i("🎯 Checking permissions on launch (first_launch=$isFirstLaunch)")

            if (!healthConnectManager.isAvailable()) {
                Timber.d("Health Connect not available")
                onShowDialog(true)
                return@LaunchedEffect
            }

            val hasHealthPermissions = healthConnectManager.checkPermissions()
            when {
                !hasHealthPermissions -> {
                    Timber.i("Health Connect permissions missing - requesting...")
                    requestHealthConnect.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                }
                isFirstLaunch -> {
                    prefs.edit().putBoolean("first_launch", false).apply()
                    Timber.i("✅ First launch completed")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Re-check Health Connect availability and permissions when app resumes
        // This handles cases where:
        // - User installed HC from Play Store while app was in background
        // - User revoked HC permissions in Settings while app was in background
        lifecycleScope.launch {
            Timber.i("🔄 Checking HC availability/permissions on resume")

            val available = healthConnectManager.isAvailable()
            val hasPermissions = if (available) {
                healthConnectManager.checkPermissions()
            } else {
                false
            }

            Timber.i("HC state on resume: available=$available, hasPermissions=$hasPermissions")

            // Note: UI state updates are handled by LaunchedEffect(Unit) in setContent
            // This just logs the state change for debugging
        }
    }
}
