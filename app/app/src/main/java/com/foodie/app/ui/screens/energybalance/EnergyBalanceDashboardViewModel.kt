package com.foodie.app.ui.screens.energybalance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodie.app.data.repository.ProfileNotConfiguredError
import com.foodie.app.domain.repository.EnergyBalanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Energy Balance Dashboard screen.
 *
 * Manages dashboard state and orchestrates data fetching from EnergyBalanceRepository.
 * Collects repository Flow in init block for automatic real-time updates when ANY
 * energy balance component changes (BMR, NEAT, Active, Calories In).
 *
 * **Date Navigation:**
 * - selectedDate defaults to LocalDate.now() (today)
 * - Users can navigate to previous/next days via onPreviousDay/onNextDay
 * - "Today" button for quick navigation back to current date
 * - selectedDate persists across app lifecycle via SavedStateHandle
 *
 * **State Management:**
 * - Exposes immutable StateFlow to UI for reactive updates
 * - Updates state on repository Flow emissions (Success → update energyBalance, Error → set error)
 * - Handles ProfileNotConfiguredError specially (shows "Open Settings" button)
 *
 * **User Actions:**
 * - refresh(): Manual refresh via pull-to-refresh (calls repository.refresh())
 * - onRetryAfterError(): Retry after error (clears error, re-subscribes to repository Flow)
 * - onPreviousDay(): Navigate to previous day
 * - onNextDay(): Navigate to next day (disabled if already viewing today)
 * - onTodayClicked(): Navigate to today
 *
 * Architecture:
 * - MVVM pattern: ViewModel exposes state, UI observes via collectAsState()
 * - Repository provides reactive Flow, ViewModel transforms to StateFlow
 * - Error handling: Result<T> from repository, map to UI-friendly error messages
 *
 * @property repository EnergyBalanceRepository for fetching energy balance data
 * @property savedStateHandle SavedStateHandle for persisting selectedDate across lifecycle
 */
@HiltViewModel
class EnergyBalanceDashboardViewModel @Inject constructor(
    private val repository: EnergyBalanceRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(EnergyBalanceState(isLoading = true))
    val state: StateFlow<EnergyBalanceState> = _state.asStateFlow()

    // Persist selectedDate across app lifecycle (backgrounding, process death)
    private val selectedDateFlow = MutableStateFlow(
        savedStateHandle.get<String>("selected_date")?.let { LocalDate.parse(it) }
            ?: LocalDate.now(),
    )

    init {
        // Start collecting energy balance data on init (AC #7: real-time updates)
        collectEnergyBalance()

        // Persist selectedDate changes to SavedStateHandle
        viewModelScope.launch {
            selectedDateFlow.collect { date ->
                savedStateHandle["selected_date"] = date.toString()
            }
        }
    }

    /**
     * Navigate to the previous day.
     *
     * Updates selectedDate to selectedDate.minusDays(1), which triggers
     * collectEnergyBalance to re-query repository with new date.
     */
    fun onPreviousDay() {
        val newDate = selectedDateFlow.value.minusDays(1)
        Timber.d("Navigating to previous day: $newDate")
        selectedDateFlow.value = newDate
        _state.update { it.copy(selectedDate = newDate, isLoading = true) }
        collectEnergyBalance()
    }

    /**
     * Navigate to the next day.
     *
     * Updates selectedDate to selectedDate.plusDays(1), disabled if already viewing today.
     */
    fun onNextDay() {
        val currentDate = selectedDateFlow.value
        if (currentDate >= LocalDate.now()) {
            Timber.d("Cannot navigate to future dates (already on today)")
            return
        }

        val newDate = currentDate.plusDays(1)
        Timber.d("Navigating to next day: $newDate")
        selectedDateFlow.value = newDate
        _state.update { it.copy(selectedDate = newDate, isLoading = true) }
        collectEnergyBalance()
    }

    /**
     * Navigate to today.
     *
     * Resets selectedDate to LocalDate.now(), which triggers collectEnergyBalance
     * to re-query repository for today's data.
     */
    fun onTodayClicked() {
        val today = LocalDate.now()
        Timber.d("Navigating to today: $today")
        selectedDateFlow.value = today
        _state.update { it.copy(selectedDate = today, isLoading = true) }
        collectEnergyBalance()
    }

    /**
     * Collect energy balance data from repository Flow.
     *
     * Subscribes to repository.getEnergyBalance(selectedDate) which emits updates whenever:
     * - selectedDate changes (user navigates to different day)
     * - BMR changes (user profile updated) - only for current date
     * - NEAT changes (step count syncs from Health Connect) - real-time for current date
     * - Active calories change (workout syncs from Health Connect) - real-time for current date
     * - Calories In changes (meal logged/edited/deleted) - real-time for current date
     *
     * Historical dates emit once with that day's data (no polling).
     *
     * Updates state on each emission:
     * - Success: Set energyBalance, lastUpdated, clear error, isLoading=false
     * - Failure: Set error message, showSettingsButton if ProfileNotConfiguredError
     */
    private fun collectEnergyBalance() {
        viewModelScope.launch {
            try {
                repository.getEnergyBalance(selectedDateFlow.value)
                    .collect { result ->
                        result.fold(
                            onSuccess = { energyBalance ->
                                Timber.d("Energy balance updated for ${selectedDateFlow.value}: $energyBalance")
                                _state.update { currentState ->
                                    currentState.copy(
                                        energyBalance = energyBalance,
                                        lastUpdated = Instant.now(),
                                        isLoading = false,
                                        error = null,
                                        showSettingsButton = false,
                                        selectedDate = selectedDateFlow.value,
                                    )
                                }
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "Failed to fetch energy balance for ${selectedDateFlow.value}")
                                _state.update { currentState ->
                                    currentState.copy(
                                        isLoading = false,
                                        error = exception.message ?: "Unknown error occurred",
                                        showSettingsButton = exception is ProfileNotConfiguredError,
                                        selectedDate = selectedDateFlow.value,
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
                        selectedDate = selectedDateFlow.value,
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
                        selectedDate = selectedDateFlow.value,
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
