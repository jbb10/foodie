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
            
            // Register permission request launcher inside setContent
            val requestPermissions = rememberLauncherForActivityResult(
                healthConnectManager.createPermissionRequestContract()
            ) { granted ->
                Timber.i("Health Connect permission result: granted=${granted.size}, required=${HealthConnectManager.REQUIRED_PERMISSIONS.size}")
                Timber.i("Granted permissions: $granted")
                if (granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)) {
                    Timber.i("Health Connect permissions granted")
                } else {
                    Timber.w("Health Connect permissions denied or incomplete")
                }
            }
            
            FoodieTheme {
                // Check Health Connect availability and permissions on launch
                LaunchedEffect(Unit) {
                    lifecycleScope.launch {
                        Timber.d("Checking Health Connect availability...")
                        val available = healthConnectManager.isAvailable()
                        Timber.d("Health Connect available: $available")
                        
                        if (!available) {
                            showHealthConnectDialog = true
                        } else {
                            // Check permissions
                            val hasPermissions = healthConnectManager.checkPermissions()
                            Timber.d("Has Health Connect permissions: $hasPermissions")
                            
                            if (!hasPermissions) {
                                Timber.i("Launching Health Connect permission request...")
                                requestPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                            } else {
                                Timber.i("Health Connect permissions already granted")
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