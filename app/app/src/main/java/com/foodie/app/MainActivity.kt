package com.foodie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        
        // Register permission request launcher
        val requestPermissions = registerForActivityResult(
            healthConnectManager.createPermissionRequestContract()
        ) { granted ->
            if (granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)) {
                Timber.i("Health Connect permissions granted")
            } else {
                Timber.w("Health Connect permissions denied")
            }
        }
        
        enableEdgeToEdge()
        setContent {
            var showHealthConnectDialog by remember { mutableStateOf(false) }
            
            FoodieTheme {
                // Check Health Connect availability and permissions on launch
                LaunchedEffect(Unit) {
                    lifecycleScope.launch {
                        val available = healthConnectManager.isAvailable()
                        if (!available) {
                            showHealthConnectDialog = true
                        } else {
                            // Check permissions
                            val hasPermissions = healthConnectManager.checkPermissions()
                            if (!hasPermissions) {
                                Timber.d("Requesting Health Connect permissions")
                                requestPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                            }
                        }
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