package com.foodie.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.domain.model.ApiConfiguration
import com.foodie.app.domain.model.TestConnectionResult
import com.foodie.app.domain.model.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val securePreferences: SecurePreferences
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

                _state.update { it.copy(
                    apiKey = apiKey,
                    apiEndpoint = endpoint,
                    modelName = model
                )}

                // Now observe preferences for updates
                preferencesRepository.observePreferences()
                    .onEach { preferences ->
                        _state.update { currentState ->
                            currentState.copy(
                                apiEndpoint = preferences["pref_azure_endpoint"] as? String ?: currentState.apiEndpoint,
                                modelName = preferences["pref_azure_model"] as? String ?: currentState.modelName,
                                themeMode = preferences["pref_theme_mode"] as? String ?: "system",
                                isLoading = false
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
                    _state.update { it.copy(
                        apiKey = apiKey,
                        apiEndpoint = endpoint,
                        modelName = modelName,
                        isLoading = false,
                        saveSuccessMessage = "Configuration saved"
                    )}
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
                _state.update { it.copy(
                    error = validationResult.message,
                    isTestingConnection = false
                )}
                return@launch
            }

            preferencesRepository.testConnection(apiKey, endpoint, modelName)
                .onSuccess { result ->
                    Timber.d("Connection test completed: $result")
                    when (result) {
                        is TestConnectionResult.Success -> {
                            _state.update { it.copy(
                                isTestingConnection = false,
                                saveSuccessMessage = "API configuration valid"
                            )}
                        }
                        is TestConnectionResult.Failure -> {
                            _state.update { it.copy(
                                isTestingConnection = false,
                                error = result.errorMessage
                            )}
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Connection test failed")
                    _state.update { it.copy(
                        isTestingConnection = false,
                        error = "Test failed: ${error.message}"
                    )}
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
}
