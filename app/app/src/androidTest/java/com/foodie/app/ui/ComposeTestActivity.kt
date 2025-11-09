package com.foodie.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Test activity for Compose instrumentation tests with Hilt support.
 *
 * This activity solves the problem of testing Compose screens that use `hiltViewModel()`
 * by providing an Activity annotated with @AndroidEntryPoint, which enables Hilt
 * dependency injection in tests.
 *
 * Problem Solved:
 * - `createComposeRule()` creates a plain ComponentActivity without Hilt support
 * - Screens using `hiltViewModel()` crash with "does not implement GeneratedComponent"
 * - `createAndroidComposeRule<MainActivity>()` doesn't work because MainActivity
 *   already calls setContent() in onCreate()
 *
 * Solution:
 * - Use `createAndroidComposeRule<ComposeTestActivity>()`
 * - Manually call `activity.setContent { }` in your test
 * - This gives you Hilt support while allowing custom content
 *
 * Usage Example:
 * ```kotlin
 * @HiltAndroidTest
 * class MyScreenTest {
 *     @get:Rule(order = 0)
 *     val hiltRule = HiltAndroidRule(this)
 *
 *     @get:Rule(order = 1)
 *     val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()
 *
 *     @Test
 *     fun myTest() {
 *         composeTestRule.activity.setContent {
 *             MyScreen() // Can use hiltViewModel() inside
 *         }
 *         // ... test assertions
 *     }
 * }
 * ```
 *
 * Important:
 * - DO NOT override onCreate() or call setContent() in this class
 * - Tests must call setContent() manually
 * - This activity should remain minimal and empty
 *
 * @see docs/testing/compose-hilt-testing-guide.md for complete usage guide
 */
@AndroidEntryPoint
class ComposeTestActivity : ComponentActivity() {
    // Intentionally empty - tests call setContent() manually
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // DO NOT call setContent() here - let tests do it
    }
}
