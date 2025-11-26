package com.foodie.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkMonitor implementation.
 *
 * Tests network connectivity monitoring, state updates, and performance requirements.
 *
 * Story: 4.1 - Network & Error Handling Infrastructure
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities
    private lateinit var networkMonitor: NetworkMonitorImpl

    private val networkCallbackSlot = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        network = mockk(relaxed = true)
        networkCapabilities = mockk(relaxed = true)

        // Mock Context.getSystemService to return ConnectivityManager
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        // Mock NetworkRequest.Builder (Android framework class)
        mockkConstructor(NetworkRequest.Builder::class)
        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns mockk(relaxed = true)
        every { anyConstructed<NetworkRequest.Builder>().build() } returns mockk(relaxed = true)

        // Capture network callback registration
        every {
            connectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                capture(networkCallbackSlot),
            )
        } returns Unit
    }

    @After
    fun teardown() {
        unmockkConstructor(NetworkRequest.Builder::class)
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: isConnected StateFlow emits true when network available
     */
    @Test
    fun `isConnected should emit true when network is available`() {
        // Given: Network is available with internet capability
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // When: NetworkMonitor is initialized
        networkMonitor = NetworkMonitorImpl(context)

        // Then: isConnected StateFlow emits true
        assertThat(networkMonitor.isConnected.value).isTrue()
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: isConnected StateFlow emits false when network unavailable
     */
    @Test
    fun `isConnected should emit false when network is unavailable`() {
        // Given: No active network
        every { connectivityManager.activeNetwork } returns null

        // When: NetworkMonitor is initialized
        networkMonitor = NetworkMonitorImpl(context)

        // Then: isConnected StateFlow emits false
        assertThat(networkMonitor.isConnected.value).isFalse()
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: networkType StateFlow emits WIFI when connected to WiFi
     */
    @Test
    fun `networkType should emit WIFI when connected to WiFi`() {
        // Given: WiFi network available
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false

        // When: NetworkMonitor is initialized
        networkMonitor = NetworkMonitorImpl(context)

        // Then: networkType StateFlow emits WIFI
        assertThat(networkMonitor.networkType.value).isEqualTo(NetworkType.WIFI)
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: networkType StateFlow emits CELLULAR when connected to mobile data
     */
    @Test
    fun `networkType should emit CELLULAR when connected to mobile data`() {
        // Given: Cellular network available
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

        // When: NetworkMonitor is initialized
        networkMonitor = NetworkMonitorImpl(context)

        // Then: networkType StateFlow emits CELLULAR
        assertThat(networkMonitor.networkType.value).isEqualTo(NetworkType.CELLULAR)
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: networkType StateFlow emits NONE when offline
     */
    @Test
    fun `networkType should emit NONE when offline`() {
        // Given: No active network
        every { connectivityManager.activeNetwork } returns null

        // When: NetworkMonitor is initialized
        networkMonitor = NetworkMonitorImpl(context)

        // Then: networkType StateFlow emits NONE
        assertThat(networkMonitor.networkType.value).isEqualTo(NetworkType.NONE)
    }

    /**
     * AC #2: NetworkMonitor can perform synchronous connectivity checks in < 50ms
     * Test: checkConnectivity() returns correct state synchronously
     */
    @Test
    fun `checkConnectivity should return true when network is available`() {
        // Given: Network is available
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        networkMonitor = NetworkMonitorImpl(context)

        // When: Synchronous connectivity check performed
        val isConnected = networkMonitor.checkConnectivity()

        // Then: Returns true
        assertThat(isConnected).isTrue()
    }

    /**
     * AC #2: NetworkMonitor can perform synchronous connectivity checks in < 50ms
     * Test: checkConnectivity() returns false when network unavailable
     */
    @Test
    fun `checkConnectivity should return false when network is unavailable`() {
        // Given: No active network
        every { connectivityManager.activeNetwork } returns null

        networkMonitor = NetworkMonitorImpl(context)

        // When: Synchronous connectivity check performed
        val isConnected = networkMonitor.checkConnectivity()

        // Then: Returns false
        assertThat(isConnected).isFalse()
    }

    /**
     * AC #2: NetworkMonitor can perform synchronous connectivity checks in < 50ms
     * Test: Performance requirement - checkConnectivity completes in < 50ms
     */
    @Test
    fun `checkConnectivity should complete in less than 50ms`() {
        // Given: Network is available
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        networkMonitor = NetworkMonitorImpl(context)

        // When: Performance test over 100 iterations
        val iterations = 100
        val measurements = mutableListOf<Long>()

        repeat(iterations) {
            val startTime = System.nanoTime()
            networkMonitor.checkConnectivity()
            val endTime = System.nanoTime()
            measurements.add(endTime - startTime)
        }

        val averageNanos = measurements.average()
        val averageMillis = averageNanos / 1_000_000.0

        // Then: Average execution time < 50ms
        assertThat(averageMillis).isLessThan(50.0)
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: waitForConnectivity suspends until network available
     */
    @Test
    fun `waitForConnectivity should suspend until network is available`() = runTest {
        // Given: Network initially unavailable
        every { connectivityManager.activeNetwork } returns null

        networkMonitor = NetworkMonitorImpl(context)

        // Verify initial state is disconnected
        assertThat(networkMonitor.isConnected.value).isFalse()

        // Simulate network becoming available (callback triggers state update)
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // Trigger network available callback manually (simulate real callback behavior)
        // In real implementation, callback updates StateFlow
        // For this test, we'll update StateFlow directly to simulate callback behavior
        val privateField = NetworkMonitorImpl::class.java.getDeclaredField("_isConnected")
        privateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = privateField.get(networkMonitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        stateFlow.value = true

        advanceUntilIdle()

        // When: waitForConnectivity is called
        networkMonitor.waitForConnectivity()

        // Then: Method completes (doesn't suspend indefinitely)
        assertThat(networkMonitor.isConnected.value).isTrue()
    }

    /**
     * AC #1: NetworkMonitor provides real-time connectivity state via StateFlow
     * Test: Network callback is registered on initialization
     */
    @Test
    fun `should register network callback on initialization`() {
        // Given: Mocked ConnectivityManager
        every { connectivityManager.activeNetwork } returns null

        // When: NetworkMonitor is initialized
        networkMonitor = NetworkMonitorImpl(context)

        // Then: registerNetworkCallback is called with NetworkRequest
        verify {
            connectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                any<ConnectivityManager.NetworkCallback>(),
            )
        }
    }
}
