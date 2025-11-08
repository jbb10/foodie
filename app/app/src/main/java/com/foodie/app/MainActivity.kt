package com.foodie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.foodie.app.ui.navigation.NavGraph
import com.foodie.app.ui.theme.FoodieTheme
import dagger.hilt.android.AndroidEntryPoint

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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodieTheme {
                NavGraph()
            }
        }
    }
}