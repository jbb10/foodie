package com.foodie.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.domain.model.ApiConfiguration
import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.domain.model.ThemeMode
import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.model.ValidationError
import com.foodie.app.domain.model.ValidationResult
import com.foodie.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Settings screen.
 *
 * Manages settings state and coordinates preference persistence through PreferencesRepository.
 * Exposes reactive StateFlow for Compose UI to observe and automatically recompose on changes.
 *
 * Architecture:
 * - MVVM pattern: ViewModel mediates between UI and repository
 * - Unidirectional data flow: UI observes StateFlow, emits user actions via methods
 * - Repository abstraction for testability (mock repository in unit tests)
 * - Lifecycle-aware: viewModelScope for coroutines (cancels when ViewModel cleared)
 *
 * State management:
 * - StateFlow<SettingsState> for reactive UI updates
 * - Loading state during async operations
 * - Error state for persistence failures
 * - Observes preferences via repository Flow (auto-updates when preferences change)
 *
 * Usage in Compose:
 * ```
 * @Composable
 * fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
 *     val state by viewModel.state.collectAsState()
 *     // UI renders based on state
 * }
 * ```
 *
 * @property preferencesRepository Repository for preference CRUD and observation
 * @property securePreferences SecurePreferences for API key retrieval
 * @property userProfileRepository Repository for user profile management (Story 6.1)
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val securePreferences: SecurePreferences,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())

    /**
     * Current settings state.
     *
     * Compose UI observes this with collectAsState() for automatic recomposition.
     * Emits new state when preferences load, save, or change externally.
     */
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        // Load initial configuration, then observe changes
        // This ensures deterministic state initialization
        loadApiConfigurationAndObserve()
        loadUserProfile()
        prePopulateHealthConnectData()
    }

    /**
     * Loads initial API configuration and then observes preference changes.
     *
     * This combined approach fixes the race condition between loadApiConfiguration()
     * and observePreferences() by ensuring initial load completes before observation starts.
     */
    private fun loadApiConfigurationAndObserve() {
        viewModelScope.launch {
            try {
                // Load initial API configuration from SecurePreferences
                val apiKey = securePreferences.azureOpenAiApiKey ?: ""
                val endpoint = securePreferences.azureOpenAiEndpoint ?: ""
                val model = securePreferences.azureOpenAiModel ?: "gpt-4.1"

                _state.update {
                    it.copy(
                        apiKey = apiKey,
                        apiEndpoint = endpoint,
                        modelName = model,
                    )
                }

                // Now observe preferences for updates
                preferencesRepository.observePreferences()
                    .onEach { preferences ->
                        _state.update { currentState ->
                            currentState.copy(
                                apiEndpoint = preferences["pref_azure_endpoint"] as? String ?: currentState.apiEndpoint,
                                modelName = preferences["pref_azure_model"] as? String ?: currentState.modelName,
                                themeMode = preferences["pref_theme_mode"] as? String ?: "system",
                                isLoading = false,
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load API configuration")
                _state.update { it.copy(error = "Failed to load preferences", isLoading = false) }
            }
        }
    }

    /**
     * Saves a string preference value.
     *
     * @param key Preference key (use "pref_" prefix)
     * @param value Value to save
     */
    fun saveString(key: String, value: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            preferencesRepository.setString(key, value)
                .onSuccess {
                    Timber.d("Preference saved successfully: $key")
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to save preference: $key")
                    _state.update { it.copy(error = "Failed to save setting", isLoading = false) }
                }
        }
    }

    /**
     * Saves a boolean preference value.
     *
     * @param key Preference key (use "pref_" prefix)
     * @param value Value to save
     */
    fun saveBoolean(key: String, value: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            preferencesRepository.setBoolean(key, value)
                .onSuccess {
                    Timber.d("Preference saved successfully: $key")
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to save preference: $key")
                    _state.update { it.copy(error = "Failed to save setting", isLoading = false) }
                }
        }
    }

    /**
     * Clears any error state.
     *
     * Call from UI after displaying error to user (e.g., after Toast shown).
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Saves complete Azure OpenAI API configuration.
     *
     * Validates inputs before saving to SecurePreferences and SharedPreferences.
     *
     * @param apiKey Azure OpenAI API key
     * @param endpoint Azure OpenAI endpoint URL
     * @param modelName Model deployment name
     */
    fun saveApiConfiguration(apiKey: String, endpoint: String, modelName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, saveSuccessMessage = null) }

            val config = ApiConfiguration(apiKey, endpoint, modelName)
            val validationResult = config.validate()

            if (validationResult is ValidationResult.Error) {
                _state.update { it.copy(error = validationResult.message, isLoading = false) }
                return@launch
            }

            preferencesRepository.saveApiConfiguration(config)
                .onSuccess {
                    Timber.d("API configuration saved successfully")
                    _state.update {
                        it.copy(
                            apiKey = apiKey,
                            apiEndpoint = endpoint,
                            modelName = modelName,
                            isLoading = false,
                            saveSuccessMessage = "Configuration saved",
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to save API configuration")
                    _state.update { it.copy(error = "Failed to save: ${error.message}", isLoading = false) }
                }
        }
    }

    /**
     * Tests Azure OpenAI API connection with provided configuration.
     *
     * Makes a minimal Responses API request to validate credentials.
     *
     * @param apiKey Azure OpenAI API key to test
     * @param endpoint Azure OpenAI endpoint URL to test
     * @param modelName Model deployment name to test
     */
    fun testConnection(apiKey: String, endpoint: String, modelName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isTestingConnection = true, testConnectionResult = null, error = null, saveSuccessMessage = null) }

            // Validate before testing
            val config = ApiConfiguration(apiKey, endpoint, modelName)
            val validationResult = config.validate()

            if (validationResult is ValidationResult.Error) {
                _state.update {
                    it.copy(
                        error = validationResult.message,
                        isTestingConnection = false,
                    )
                }
                return@launch
            }

            preferencesRepository.testConnection(apiKey, endpoint, modelName)
                .onSuccess { result ->
                    Timber.d("Connection test completed: $result")
                    when (result) {
                        is TestConnectionResult.Success -> {
                            _state.update {
                                it.copy(
                                    isTestingConnection = false,
                                    saveSuccessMessage = "API configuration valid",
                                )
                            }
                        }
                        is TestConnectionResult.Failure -> {
                            _state.update {
                                it.copy(
                                    isTestingConnection = false,
                                    error = result.errorMessage,
                                )
                            }
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Connection test failed")
                    _state.update {
                        it.copy(
                            isTestingConnection = false,
                            error = "Test failed: ${error.message}",
                        )
                    }
                }
        }
    }

    /**
     * Clears test connection result.
     *
     * Call after displaying result to user to reset UI state.
     */
    fun clearTestResult() {
        _state.update { it.copy(testConnectionResult = null) }
    }

    /**
     * Clears save success message.
     *
     * Called after Snackbar is shown to prevent re-triggering.
     */
    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccessMessage = null) }
    }

    /**
     * Updates theme mode preference.
     *
     * Saves theme preference to SharedPreferences and updates state.
     * MainActivity observes theme changes via PreferencesRepository.getThemeMode() Flow.
     *
     * @param mode ThemeMode to apply (SYSTEM_DEFAULT, LIGHT, or DARK)
     *
     * Story 5.4: Dark Mode Support (AC-7, AC-8)
     */
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            preferencesRepository.saveThemeMode(mode)
                .onSuccess {
                    Timber.d("Theme mode updated: ${mode.value}")
                    _state.update {
                        it.copy(
                            themeMode = mode.value,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to save theme mode")
                    _state.update { it.copy(error = "Failed to save theme", isLoading = false) }
                }
        }
    }

    /**
     * Loads user profile from repository and pre-populates state.
     *
     * Queries Health Connect for latest weight/height and SharedPreferences for sex/age.
     * Tracks data source (HC vs manual entry) with weightSourcedFromHC/heightSourcedFromHC flags.
     *
     * Story 6.1: User Profile Settings (AC-2)
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            userProfileRepository.getUserProfile()
                .onEach { profile ->
                    if (profile != null) {
                        Timber.d("User profile loaded: sex=${profile.sex}, birthDate=${profile.birthDate}, weight=${profile.weightKg}, height=${profile.heightCm}")
                        _state.update {
                            it.copy(
                                editableSex = profile.sex,
                                editableBirthDate = profile.birthDate,
                                editableWeight = "%.1f".format(profile.weightKg),
                                editableHeight = "%.0f".format(profile.heightCm),
                                weightSourcedFromHC = true, // Assume HC-sourced initially
                                heightSourcedFromHC = true,
                                isEditingProfile = false,
                            )
                        }
                    } else {
                        Timber.d("User profile not configured")
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Pre-populates weight and height from Health Connect if available.
     *
     * This runs independently of loadUserProfile() to show HC data even when
     * sex/birthDate are not yet configured. Only updates fields if they're empty.
     *
     * Story 6.1: User Profile Settings (AC-7, AC-8)
     */
    private fun prePopulateHealthConnectData() {
        viewModelScope.launch {
            try {
                val weightRecord = userProfileRepository.queryLatestWeight()
                val heightRecord = userProfileRepository.queryLatestHeight()

                _state.update { currentState ->
                    var newState = currentState

                    // Pre-populate weight if field is empty and HC has data
                    if (currentState.editableWeight.isEmpty() && weightRecord != null) {
                        val weightKg = weightRecord.weight.inKilograms
                        Timber.d("Pre-populating weight from HC: $weightKg kg")
                        newState = newState.copy(
                            editableWeight = "%.1f".format(weightKg),
                            weightSourcedFromHC = true,
                        )
                    }

                    // Pre-populate height if field is empty and HC has data
                    if (currentState.editableHeight.isEmpty() && heightRecord != null) {
                        val heightCm = heightRecord.height.inMeters * 100.0
                        Timber.d("Pre-populating height from HC: $heightCm cm")
                        newState = newState.copy(
                            editableHeight = "%.0f".format(heightCm),
                            heightSourcedFromHC = true,
                        )
                    }

                    newState
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to pre-populate HC data")
            }
        }
    }

    /**
     * Updates selected sex for BMR calculation.
     *
     * Sets isEditingProfile=true to enable Save button.
     *
     * Story 6.1: User Profile Settings (AC-4)
     */
    fun onSexChanged(sex: UserProfile.Sex) {
        _state.update {
            it.copy(
                editableSex = sex,
                isEditingProfile = true,
            )
        }
    }

    /**
     * Updates birth date.
     *
     * Accepts LocalDate input, sets isEditingProfile=true to enable Save button.
     *
     * Story 6.1: User Profile Settings (AC-5)
     */
    fun onBirthDateChanged(birthDate: LocalDate) {
        _state.update {
            it.copy(
                editableBirthDate = birthDate,
                isEditingProfile = true,
            )
        }
    }

    /**
     * Updates weight input field.
     *
     * Marks weightSourcedFromHC=false (user explicitly edited, will write to HC on save).
     * Sets isEditingProfile=true to enable Save button.
     *
     * Story 6.1: User Profile Settings (AC-6, AC-10)
     */
    fun onWeightChanged(weight: String) {
        _state.update {
            it.copy(
                editableWeight = weight.replace(',', '.'),
                weightSourcedFromHC = false, // User edited, no longer HC-sourced
                isEditingProfile = true,
            )
        }
    }

    /**
     * Updates height input field.
     *
     * Marks heightSourcedFromHC=false (user explicitly edited, will write to HC on save).
     * Sets isEditingProfile=true to enable Save button.
     *
     * Story 6.1: User Profile Settings (AC-7, AC-10)
     */
    fun onHeightChanged(height: String) {
        _state.update {
            it.copy(
                editableHeight = height.replace(',', '.'),
                heightSourcedFromHC = false, // User edited, no longer HC-sourced
                isEditingProfile = true,
            )
        }
    }

    /**
     * Saves user profile with validation.
     *
     * **Validation:**
     * - Converts string inputs to numeric types
     * - Calls UserProfile.validate() for range checks (age 13-120, weight 30-300, height 100-250)
     *
     * **Storage:**
     * - Sex & age: Always saved to SharedPreferences
     * - Weight: Written to HC only if weightSourcedFromHC=false (user edited)
     * - Height: Written to HC only if heightSourcedFromHC=false (user edited)
     *
     * **Error Handling:**
     * - ValidationError: Updates profileValidationError state for inline display
     * - SecurityException: Sets showProfilePermissionError=true for permission prompt
     *
     * Story 6.1: User Profile Settings (AC-9, AC-10, AC-11, AC-12)
     */
    fun saveUserProfile() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    profileValidationError = null,
                    profileSaveSuccess = false,
                    showProfilePermissionError = false,
                )
            }

            // Parse inputs
            val sex = _state.value.editableSex
            if (sex == null) {
                _state.update { it.copy(profileValidationError = "Please select sex") }
                return@launch
            }

            val birthDate = _state.value.editableBirthDate
            if (birthDate == null) {
                _state.update { it.copy(profileValidationError = "Please select birth date") }
                return@launch
            }

            val weight = _state.value.editableWeight.replace(',', '.').toDoubleOrNull()
            if (weight == null) {
                _state.update { it.copy(profileValidationError = "Please enter a valid weight") }
                return@launch
            }

            val height = _state.value.editableHeight.replace(',', '.').toDoubleOrNull()
            if (height == null) {
                _state.update { it.copy(profileValidationError = "Please enter a valid height") }
                return@launch
            }

            // Create and validate profile
            val profile = UserProfile(sex, birthDate, weight, height)

            val validationResult = profile.validate()
            if (validationResult.isFailure) {
                val error = validationResult.exceptionOrNull()
                Timber.w("Profile validation failed: ${error?.message}")
                _state.update { it.copy(profileValidationError = error?.message) }
                return@launch
            }

            // Save with selective HC writes
            val writeWeightToHC = !_state.value.weightSourcedFromHC
            val writeHeightToHC = !_state.value.heightSourcedFromHC

            Timber.d("Saving profile: writeWeight=$writeWeightToHC, writeHeight=$writeHeightToHC")

            val result = userProfileRepository.updateProfile(profile, writeWeightToHC, writeHeightToHC)

            result.onSuccess {
                Timber.i("Profile saved successfully")
                _state.update {
                    it.copy(
                        profileSaveSuccess = true,
                        isEditingProfile = false,
                        weightSourcedFromHC = !writeWeightToHC, // If we wrote to HC, it's now HC-sourced
                        heightSourcedFromHC = !writeHeightToHC,
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to save profile")
                when (error) {
                    is ValidationError -> {
                        _state.update { it.copy(profileValidationError = error.message) }
                    }
                    is SecurityException -> {
                        _state.update { it.copy(showProfilePermissionError = true) }
                    }
                    else -> {
                        _state.update { it.copy(profileValidationError = "Failed to save: ${error.message}") }
                    }
                }
            }
        }
    }

    /**
     * Clears profile save success state.
     *
     * Called after toast is shown to prevent re-triggering.
     *
     * Story 6.1: User Profile Settings (AC-11)
     */
    fun clearProfileSaveSuccess() {
        _state.update { it.copy(profileSaveSuccess = false) }
    }

    /**
     * Clears profile permission error state.
     *
     * Called after permission prompt is shown.
     *
     * Story 6.1: User Profile Settings (AC-10, AC-12)
     */
    fun clearProfilePermissionError() {
        _state.update { it.copy(showProfilePermissionError = false) }
    }
}
