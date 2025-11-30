package com.foodie.app.ui.screens.energybalance

import com.foodie.app.domain.model.EnergyBalance
import java.time.Instant

/**
 * UI state for the Energy Balance Dashboard screen.
 *
 * Represents all possible states of the dashboard:
 * - Loading: Initial data fetch in progress
 * - Success: EnergyBalance data loaded and displayed
 * - Empty: No meals logged today (caloriesIn = 0, energyBalance = null)
 * - Error: Data fetch failed (permission denied, profile not configured, etc.)
 *
 * **State Transitions:**
 * ```
 * Initial (isLoading=true) → Success (energyBalance != null)
 *                          → Empty (energyBalance = null, error = null)
 *                          → Error (error != null, showSettingsButton based on error type)
 * ```
 *
 * **Real-Time Updates:**
 * State updates automatically when repository emits new EnergyBalance data:
 * - Meal logged → caloriesIn increases → deficitSurplus decreases
 * - Steps sync → NEAT updates → TDEE increases → deficitSurplus increases
 * - Workout sync → activeCalories updates → TDEE increases
 *
 * **Date Navigation:**
 * - selectedDate defaults to LocalDate.now() (today)
 * - Users can navigate to previous/next days to view historical data
 * - Date persists across app lifecycle via SavedStateHandle
 *
 * @property energyBalance Complete energy balance data (null if loading, empty, or error)
 * @property isLoading True during initial load or pull-to-refresh
 * @property error Error message to display (null if no error)
 * @property lastUpdated Timestamp of last successful data fetch (for "Last updated: X minutes ago")
 * @property showSettingsButton True if error is ProfileNotConfiguredError (show "Open Settings" action)
 * @property selectedDate Date currently being viewed (defaults to today, persisted across lifecycle)
 */
data class EnergyBalanceState(
    val energyBalance: EnergyBalance? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Instant? = null,
    val showSettingsButton: Boolean = false,
    val selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
)
