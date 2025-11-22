package com.foodie.app.ui.screens.settings

import com.foodie.app.domain.model.TestConnectionResult

/**
 * State representation for the Settings screen.
 *
 * Encapsulates all preference values and UI state for reactive composition.
 * SettingsViewModel exposes this as StateFlow for Compose to observe and recompose on changes.
 *
 * Architecture:
 * - Immutable data class for predictable state updates
 * - Loading state for async preference operations
 * - Error state for persistence failures
 * - Preference fields added as stories implement them (5.2, 5.3, 5.4)
 *
 * @property isLoading True when loading or saving preferences
 * @property error Error message if preference operation failed, null otherwise
 * @property apiKey Azure OpenAI API key (masked display, Story 5.2)
 * @property apiEndpoint Azure OpenAI endpoint URL (Story 5.2)
 * @property modelName Model/deployment name for API requests (Story 5.2)
 * @property isTestingConnection True when testing API connection (Story 5.2)
 * @property testConnectionResult Result of connection test (Story 5.2)
 * @property themeMode Current theme setting: "system", "light", or "dark" (Story 5.4)
 */
data class SettingsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // API Configuration (Story 5.2)
    val apiKey: String = "",
    val apiEndpoint: String = "",
    val modelName: String = "gpt-4.1",
    val isTestingConnection: Boolean = false,
    val testConnectionResult: TestConnectionResult? = null,
    val saveSuccessMessage: String? = null,
    // Theme (Story 5.4)
    val themeMode: String = "system" // system|light|dark
)
