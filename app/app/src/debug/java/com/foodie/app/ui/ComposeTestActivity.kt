package com.foodie.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Test activity for Compose instrumentation tests with Hilt support.
 *
 * This activity remains empty so tests can call setContent manually while
 * still benefiting from @AndroidEntryPoint for hiltViewModel() calls.
 */
@AndroidEntryPoint
class ComposeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Intentionally left blank; tests supply content via setContent.
    }
}
