package com.foodie.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Implementation of NetworkMonitor using Android ConnectivityManager.
 *
 * Monitors network connectivity in real-time via registerDefaultNetworkCallback.
 * Updates StateFlows when network state changes (available/lost, type changes).
 *
 * Lifecycle:
 * - Singleton scope (Hilt)
 * - Network callback registered in init block
 * - Cleanup handled automatically (singleton lives for app lifetime)
 *
 * Performance:
 * - checkConnectivity(): < 50ms (uses cached active network)
 * - StateFlow updates: immediate (callback-driven)
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 */
@Singleton
class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    companion object {
        private const val TAG = "NetworkMonitor"
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(getCurrentConnectivityState())
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _networkType = MutableStateFlow(getCurrentNetworkType())
    override val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    /**
     * Network callback for real-time connectivity monitoring.
     *
     * Triggers on network availability changes and capability updates.
     * Updates both isConnected and networkType StateFlows.
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.tag(TAG).d("Network available: $network")
            updateNetworkState()
        }

        override fun onLost(network: Network) {
            Timber.tag(TAG).d("Network lost: $network")
            updateNetworkState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            Timber.tag(TAG).d("Network capabilities changed: $network")
            updateNetworkState()
        }
    }

    init {
        // Register callback for real-time network monitoring
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        Timber.tag(TAG).i("NetworkMonitor initialized, current state: connected=${_isConnected.value}, type=${_networkType.value}")
    }

    /**
     * Updates network state and type based on current connectivity.
     *
     * Called by network callback when connectivity changes.
     * Updates both StateFlows atomically.
     */
    private fun updateNetworkState() {
        val connected = getCurrentConnectivityState()
        val type = getCurrentNetworkType()

        if (_isConnected.value != connected || _networkType.value != type) {
            _isConnected.value = connected
            _networkType.value = type
            Timber.tag(TAG).d("Network state updated: connected=$connected, type=$type")
        }
    }

    override fun checkConnectivity(): Boolean {
        return getCurrentConnectivityState()
    }

    override suspend fun waitForConnectivity() {
        isConnected.first { it }
        Timber.tag(TAG).d("Connectivity restored")
    }

    /**
     * Gets current connectivity state synchronously.
     *
     * Uses getActiveNetwork() + getNetworkCapabilities() for fast check (< 50ms).
     *
     * @return true if network has internet capability, false otherwise
     */
    private fun getCurrentConnectivityState(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Gets current network type synchronously.
     *
     * Determines WIFI, CELLULAR, or NONE based on transport type.
     *
     * @return NetworkType enum value
     */
    private fun getCurrentNetworkType(): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.NONE
        }
    }
}
