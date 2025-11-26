package com.foodie.app.ui.screens.settings

import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.domain.model.UserProfile
import java.time.LocalDate

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
 * - Preference fields added as stories implement them (5.2, 5.3, 5.4, 6.1)
 *
 * @property isLoading True when loading or saving preferences
 * @property error Error message if preference operation failed, null otherwise
 * @property apiKey Azure OpenAI API key (masked display, Story 5.2)
 * @property apiEndpoint Azure OpenAI endpoint URL (Story 5.2)
 * @property modelName Model/deployment name for API requests (Story 5.2)
 * @property isTestingConnection True when testing API connection (Story 5.2)
 * @property testConnectionResult Result of connection test (Story 5.2)
 * @property themeMode Current theme setting: "system", "light", or "dark" (Story 5.4)
 * @property isEditingProfile True when user has modified profile fields (Story 6.1)
 * @property editableSex Selected sex for BMR calculation (Story 6.1)
 * @property editableBirthDate Birth date for age calculation (Story 6.1)
 * @property editableWeight Weight input as string in kg (Story 6.1)
 * @property editableHeight Height input as string in cm (Story 6.1)
 * @property weightSourcedFromHC True if weight was pre-populated from Health Connect (Story 6.1)
 * @property heightSourcedFromHC True if height was pre-populated from Health Connect (Story 6.1)
 * @property profileValidationError Validation error message for profile fields (Story 6.1)
 * @property profileSaveSuccess True when profile saved successfully (Story 6.1)
 * @property showProfilePermissionError True when HC permission denied during profile save (Story 6.1)
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
    val themeMode: String = "system", // system|light|dark
    // User Profile (Story 6.1)
    val isEditingProfile: Boolean = false,
    val editableSex: UserProfile.Sex? = null,
    val editableBirthDate: LocalDate? = null,
    val editableWeight: String = "",
    val editableHeight: String = "",
    val weightSourcedFromHC: Boolean = false,
    val heightSourcedFromHC: Boolean = false,
    val profileValidationError: String? = null,
    val profileSaveSuccess: Boolean = false,
    val showProfilePermissionError: Boolean = false,
)
