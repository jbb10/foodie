package com.foodie.app.ui.screens.settings

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
 * @property apiEndpoint Azure OpenAI endpoint URL (Story 5.2)
 * @property modelName Model/deployment name for API requests (Story 5.3)
 * @property themeMode Current theme setting: "system", "light", or "dark" (Story 5.4)
 */
data class SettingsState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Placeholder fields for future preferences
    val apiEndpoint: String = "",
    val modelName: String = "gpt-4.1",
    val themeMode: String = "system" // system|light|dark
)
