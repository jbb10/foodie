package com.foodie.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hilt-enabled test activity for instrumentation tests.
 *
 * Used with createAndroidComposeRule<HiltTestActivity>() to enable Hilt dependency
 * injection in tests that need to test full navigation graphs or integration flows.
 *
 * Pattern: Based on android/architecture-samples HiltTestActivity
 *
 * Location: app/src/debug because it's only needed for testing, not production.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
