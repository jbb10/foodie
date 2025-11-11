package com.foodie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.ui.components.HealthConnectUnavailableDialog
import com.foodie.app.ui.navigation.NavGraph
import com.foodie.app.ui.theme.FoodieTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var healthConnectManager: HealthConnectManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            var showHealthConnectDialog by remember { mutableStateOf(false) }
            
            // Track first launch for one-time permission requests
            val prefs = getSharedPreferences("foodie_prefs", MODE_PRIVATE)
            val isFirstLaunch = remember { prefs.getBoolean("first_launch", true) }
            
            // Track whether to request notification permission after HC permissions
            var shouldRequestNotificationPermission by remember { mutableStateOf(false) }
            
            // Register notification permission launcher (Android 13+)
            val requestNotificationPermission = rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted ->
                Timber.i("Notification permission result: granted=$granted")
                shouldRequestNotificationPermission = false
            }
            
            // Register Health Connect permission launcher
            val requestHealthConnectPermissions = rememberLauncherForActivityResult(
                healthConnectManager.createPermissionRequestContract()
            ) { granted ->
                Timber.i("Health Connect permission result: granted=${granted.size}, required=${HealthConnectManager.REQUIRED_PERMISSIONS.size}")
                if (granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)) {
                    Timber.i("Health Connect permissions granted")
                } else {
                    Timber.w("Health Connect permissions denied or incomplete")
                }
                
                // After Health Connect permissions handled, request notification permission
                if (shouldRequestNotificationPermission) {
                    shouldRequestNotificationPermission = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = checkSelfPermission(
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (!hasNotificationPermission) {
                            Timber.i("Requesting notification permission after HC...")
                            requestNotificationPermission.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                }
            }
            
            FoodieTheme {
                // First-launch permission flow
                LaunchedEffect(isFirstLaunch) {
                    if (isFirstLaunch) {
                        Timber.i("🎯 First launch detected - requesting all permissions")
                        
                        // 1. Check Health Connect availability
                        val available = healthConnectManager.isAvailable()
                        Timber.d("Health Connect available: $available")
                        
                        if (!available) {
                            showHealthConnectDialog = true
                            return@LaunchedEffect
                        }
                        
                        // 2. Request Health Connect permissions
                        val hasHealthPermissions = healthConnectManager.checkPermissions()
                        if (!hasHealthPermissions) {
                            Timber.i("Requesting Health Connect permissions...")
                            // Set flag to request notification permission after HC callback
                            shouldRequestNotificationPermission = true
                            requestHealthConnectPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                        } else {
                            // Already have HC permissions, request notification directly
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val hasNotificationPermission = checkSelfPermission(
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (!hasNotificationPermission) {
                                    Timber.i("Requesting notification permission...")
                                    requestNotificationPermission.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            }
                        }
                        
                        // Mark first launch complete
                        prefs.edit().putBoolean("first_launch", false).apply()
                        Timber.i("✅ First launch permissions requested")
                    }
                }
                
                NavGraph()
                
                if (showHealthConnectDialog) {
                    HealthConnectUnavailableDialog(
                        onDismiss = { showHealthConnectDialog = false }
                    )
                }
            }
        }
    }
}