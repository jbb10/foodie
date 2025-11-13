package com.foodie.app.data.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Network connectivity monitoring service.
 *
 * Provides real-time network state updates via StateFlow and synchronous connectivity checks.
 * Used by repositories and background workers to verify connectivity before network operations.
 *
 * Architecture Pattern:
 * - Workers/Repositories → NetworkMonitor → ConnectivityManager (Android SDK)
 * - StateFlow provides reactive updates for UI components
 * - Synchronous checkConnectivity() for immediate validation without suspending
 *
 * Performance Requirements:
 * - checkConnectivity() must complete in < 50ms (verified in unit tests)
 * - StateFlow updates occur in real-time via network callback
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 */
interface NetworkMonitor {
    
    /**
     * Current network connectivity state.
     *
     * Emits true when network is available (WiFi or cellular), false when offline.
     * Updates in real-time via ConnectivityManager network callback.
     *
     * Usage:
     * ```
     * networkMonitor.isConnected.collect { connected ->
     *     if (!connected) showOfflineMessage()
     * }
     * ```
     */
    val isConnected: StateFlow<Boolean>
    
    /**
     * Current network type.
     *
     * Emits WIFI, CELLULAR, or NONE based on active network capabilities.
     * Updates in real-time when network type changes.
     */
    val networkType: StateFlow<NetworkType>
    
    /**
     * Performs synchronous network connectivity check.
     *
     * Returns current connectivity state without suspending. Useful for immediate
     * validation in non-suspending contexts (e.g., WorkManager constraints).
     *
     * Performance: Guaranteed to complete in < 50ms using getActiveNetwork() + getNetworkCapabilities().
     *
     * @return true if network is connected (WiFi or cellular), false if offline
     */
    fun checkConnectivity(): Boolean
    
    /**
     * Suspends until network connectivity is available.
     *
     * Awaits the first emission of isConnected == true. Useful for retrying
     * network operations after connectivity is restored.
     *
     * Usage:
     * ```
     * networkMonitor.waitForConnectivity()
     * // Network is now available, proceed with API call
     * ```
     */
    suspend fun waitForConnectivity()
}

/**
 * Network connection type.
 */
enum class NetworkType {
    /** Connected via WiFi */
    WIFI,
    
    /** Connected via cellular data (3G/4G/5G) */
    CELLULAR,
    
    /** No network connection */
    NONE
}
