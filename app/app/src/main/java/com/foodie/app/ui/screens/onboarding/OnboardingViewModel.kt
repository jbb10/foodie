package com.foodie.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.local.preferences.OnboardingPreferences
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Data class representing the current onboarding state.
 *
 * @property currentPage Current page index (0-3)
 * @property totalPages Total number of onboarding screens (4)
 * @property healthConnectPermissionsGranted Whether Health Connect permissions are granted
 * @property apiConfigured Whether Azure OpenAI API is fully configured (key + endpoint)
 */
data class OnboardingState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val healthConnectPermissionsGranted: Boolean = false,
    val apiConfigured: Boolean = false
)

/**
 * ViewModel for the onboarding flow.
 *
 * Manages state for 4 onboarding screens:
 * 1. Welcome screen (core concept explanation)
 * 2. Widget setup instructions
 * 3. Health Connect permissions
 * 4. Settings/API configuration prompt
 *
 * **Architecture:**
 * - MVVM pattern: ViewModel + StateFlow + Composable screens
 * - Hilt injection: HealthConnectManager, OnboardingPreferences, PreferencesRepository
 * - Unidirectional data flow: UI actions → ViewModel methods → State updates → UI recomposition
 *
 * **State Management:**
 * - StateFlow<OnboardingState> for reactive UI updates
 * - Permission status checked on Screen 3 (Health Connect)
 * - API config status checked on Screen 4 (Settings prompt)
 *
 * **Navigation:**
 * - ViewModel does NOT handle navigation (screens pass callbacks)
 * - Screens call markOnboardingCompleted() then navigate via callback
 *
 * Story: 5.7 - User Onboarding (First Launch)
 * AC: #4 (Health Connect permissions), #5 (API configuration prompt), #8 (first-launch detection)
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val onboardingPreferences: OnboardingPreferences,
    private val securePreferences: SecurePreferences,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        // Check initial permission and configuration status
        checkHealthConnectPermissions()
        checkApiConfigurationStatus()
    }

    /**
     * Checks Health Connect permission status.
     *
     * Called on:
     * - ViewModel initialization
     * - After permission request completes (from Activity)
     * - When returning to Screen 3 from Settings
     */
    fun checkHealthConnectPermissions() {
        viewModelScope.launch {
            try {
                val granted = healthConnectManager.checkPermissions()
                _state.value = _state.value.copy(healthConnectPermissionsGranted = granted)
                Timber.tag(TAG).d("Health Connect permissions: granted=$granted")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to check Health Connect permissions")
                _state.value = _state.value.copy(healthConnectPermissionsGranted = false)
            }
        }
    }

    /**
     * Checks if Azure OpenAI API is fully configured.
     *
     * API is considered configured if BOTH:
     * - API key is set (in EncryptedSharedPreferences)
     * - Endpoint is set (in standard SharedPreferences)
     *
     * Model selection is optional (defaults to gpt-4o-mini).
     *
     * Called on:
     * - ViewModel initialization
     * - When returning to Screen 4 from Settings
     */
    fun checkApiConfigurationStatus() {
        viewModelScope.launch {
            try {
                val hasApiKey = securePreferences.hasApiKey()
                val endpoint = preferencesRepository.getString("pref_azure_endpoint", "")
                val configured = hasApiKey && endpoint.isNotBlank()
                _state.value = _state.value.copy(apiConfigured = configured)
                Timber.tag(TAG).d("API configuration: configured=$configured (hasKey=$hasApiKey, hasEndpoint=${endpoint.isNotBlank()})")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to check API configuration status")
                _state.value = _state.value.copy(apiConfigured = false)
            }
        }
    }

    /**
     * Updates the current page index.
     *
     * Called by HorizontalPager when user swipes between screens.
     *
     * @param page New page index (0-3)
     */
    fun setCurrentPage(page: Int) {
        _state.value = _state.value.copy(currentPage = page)
    }

    /**
     * Marks onboarding as completed.
     *
     * This should be called when:
     * - User completes all screens and taps "Done"
     * - User taps "Skip" button on any screen
     *
     * After calling this, the onboarding flow will not show again on subsequent app launches.
     * Navigation is handled by the screen (via callback to MainActivity/NavGraph).
     */
    fun markOnboardingCompleted() {
        onboardingPreferences.markOnboardingCompleted()
        Timber.tag(TAG).i("Onboarding marked completed")
    }

    /**
     * Handles the result of Health Connect permission request.
     *
     * Called from Activity's permission launcher callback.
     *
     * @param grantedPermissions Set of permissions that were granted
     */
    fun onHealthConnectPermissionResult(grantedPermissions: Set<String>) {
        val allGranted = grantedPermissions.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)
        _state.value = _state.value.copy(healthConnectPermissionsGranted = allGranted)
        Timber.tag(TAG).i("Permission result: granted=${grantedPermissions.size}/${HealthConnectManager.REQUIRED_PERMISSIONS.size}, allGranted=$allGranted")
    }
}
