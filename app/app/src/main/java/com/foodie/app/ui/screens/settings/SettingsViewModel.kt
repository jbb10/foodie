package com.foodie.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.data.repository.PreferencesRepository
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
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
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
        // Load initial preferences and observe changes
        observePreferences()
    }

    /**
     * Observes preferences reactively and updates state when they change.
     *
     * Uses repository Flow to receive updates when preferences are modified
     * (either by this ViewModel or externally, e.g., from background sync).
     */
    private fun observePreferences() {
        viewModelScope.launch {
            try {
                // Observe preferences map and update state fields
                preferencesRepository.observePreferences()
                    .onEach { preferences ->
                        _state.update { currentState ->
                            currentState.copy(
                                apiEndpoint = preferences["pref_azure_endpoint"] as? String ?: "",
                                modelName = preferences["pref_azure_model"] as? String ?: "gpt-4.1",
                                themeMode = preferences["pref_theme_mode"] as? String ?: "system",
                                isLoading = false
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            } catch (e: Exception) {
                Timber.e(e, "Failed to observe preferences")
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
}
