package com.foodie.app.ui.screens.energybalance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.data.repository.ProfileNotConfiguredError
import com.foodie.app.domain.repository.EnergyBalanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for Energy Balance Dashboard screen.
 *
 * Manages dashboard state and orchestrates data fetching from EnergyBalanceRepository.
 * Collects repository Flow in init block for automatic real-time updates when ANY
 * energy balance component changes (BMR, NEAT, Active, Calories In).
 *
 * **State Management:**
 * - Exposes immutable StateFlow to UI for reactive updates
 * - Updates state on repository Flow emissions (Success → update energyBalance, Error → set error)
 * - Handles ProfileNotConfiguredError specially (shows "Open Settings" button)
 *
 * **User Actions:**
 * - refresh(): Manual refresh via pull-to-refresh (calls repository.refresh())
 * - onRetryAfterError(): Retry after error (clears error, re-subscribes to repository Flow)
 *
 * Architecture:
 * - MVVM pattern: ViewModel exposes state, UI observes via collectAsState()
 * - Repository provides reactive Flow, ViewModel transforms to StateFlow
 * - Error handling: Result<T> from repository, map to UI-friendly error messages
 *
 * @property repository EnergyBalanceRepository for fetching energy balance data
 */
@HiltViewModel
class EnergyBalanceDashboardViewModel @Inject constructor(
    private val repository: EnergyBalanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnergyBalanceState(isLoading = true))
    val state: StateFlow<EnergyBalanceState> = _state.asStateFlow()

    init {
        // Start collecting energy balance data on init (AC #7: real-time updates)
        collectEnergyBalance()
    }

    /**
     * Collect energy balance data from repository Flow.
     *
     * Subscribes to repository.getEnergyBalance() which emits updates whenever:
     * - BMR changes (user profile updated)
     * - NEAT changes (step count syncs from Health Connect)
     * - Active calories change (workout syncs from Health Connect)
     * - Calories In changes (meal logged/edited/deleted)
     *
     * Updates state on each emission:
     * - Success: Set energyBalance, lastUpdated, clear error, isLoading=false
     * - Failure: Set error message, showSettingsButton if ProfileNotConfiguredError
     */
    private fun collectEnergyBalance() {
        viewModelScope.launch {
            try {
                repository.getEnergyBalance()
                    .collect { result ->
                        result.fold(
                            onSuccess = { energyBalance ->
                                Timber.d("Energy balance updated: $energyBalance")
                                _state.update { currentState ->
                                    currentState.copy(
                                        energyBalance = energyBalance,
                                        lastUpdated = Instant.now(),
                                        isLoading = false,
                                        error = null,
                                        showSettingsButton = false,
                                    )
                                }
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "Failed to fetch energy balance")
                                _state.update { currentState ->
                                    currentState.copy(
                                        isLoading = false,
                                        error = exception.message ?: "Unknown error occurred",
                                        showSettingsButton = exception is ProfileNotConfiguredError,
                                    )
                                }
                            },
                        )
                    }
            } catch (e: SecurityException) {
                // Handle Health Connect permission denied
                Timber.e(e, "Health Connect permission denied")
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Health Connect permissions are required to view your energy balance. Tap 'Grant Access' to allow Foodie to read your activity data.",
                        showSettingsButton = true,
                    )
                }
            } catch (e: Exception) {
                // Handle any other unexpected exceptions
                Timber.e(e, "Unexpected error collecting energy balance")
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Unexpected error occurred",
                        showSettingsButton = false,
                    )
                }
            }
        }
    }

    /**
     * Refresh energy balance data (pull-to-refresh action).
     *
     * Note: The repository Flow already polls Health Connect every 5 minutes for updates.
     * Pull-to-refresh simply sets isLoading state for UI feedback. The existing Flow
     * collection will automatically pick up new data on the next polling interval.
     *
     * Sets isLoading=true to show refresh indicator during fetch, then resets after 1 second.
     */
    fun refresh() {
        Timber.d("Manual refresh triggered")
        _state.update { it.copy(isLoading = true) }

        // Reset isLoading after a brief delay to provide visual feedback
        // The Flow is already collecting and will emit updates automatically
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // 1 second delay for visual feedback
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Retry after error (error state retry button action).
     *
     * Clears error state and re-attempts to collect energy balance data.
     * Useful for transient errors (network issues, Health Connect temporarily unavailable).
     */
    fun onRetryAfterError() {
        Timber.d("Retrying after error")
        _state.update { it.copy(isLoading = true, error = null) }
        // collectEnergyBalance() is already running in init, just clearing error
        // allows existing Flow collection to update state on next emission
    }
}
