package com.foodie.app.ui.base

import androidx.lifecycle.ViewModel
import timber.log.Timber

/**
 * Base ViewModel class providing common functionality for all ViewModels in the app.
 *
 * This class establishes patterns for:
 * - Lifecycle logging for debugging
 * - Future common error handling utilities
 * - Shared state management patterns
 *
 * Usage:
 * ```
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val repository: MyRepository
 * ) : BaseViewModel() {
 *     // ViewModel implementation
 * }
 * ```
 *
 * All ViewModels should extend this class to ensure consistent behavior and
 * make future architectural changes easier to propagate.
 */
abstract class BaseViewModel : ViewModel() {

    init {
        Timber.d("${this::class.simpleName} created")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("${this::class.simpleName} cleared")
    }

    /**
     * Helper method for logging errors consistently across ViewModels.
     *
     * @param tag Context tag for the error (e.g., "LoadData", "SaveEntry")
     * @param throwable The exception that occurred
     * @param message Additional context message
     */
    protected fun logError(tag: String, throwable: Throwable, message: String = "") {
        Timber.e(throwable, "[${this::class.simpleName}][$tag] $message")
    }

    /**
     * Helper method for logging debug information.
     *
     * @param tag Context tag for the log
     * @param message Debug message
     */
    protected fun logDebug(tag: String, message: String) {
        Timber.d("[${this::class.simpleName}][$tag] $message")
    }
}
