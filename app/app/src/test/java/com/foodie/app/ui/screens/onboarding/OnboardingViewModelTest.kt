package com.foodie.app.ui.screens.onboarding

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.local.preferences.OnboardingPreferences
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.repository.PreferencesRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OnboardingViewModel].
 *
 * Validates state management for onboarding flow:
 * - Health Connect permission status
 * - API configuration status
 * - Current page tracking
 * - Onboarding completion flag
 *
 * Story: 5.7 - User Onboarding (First Launch)
 * AC: #4 (Health Connect permissions), #5 (API configuration), #8 (first-launch detection)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var onboardingPreferences: OnboardingPreferences
    private lateinit var securePreferences: SecurePreferences
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var viewModel: OnboardingViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        healthConnectManager = mockk(relaxed = true)
        onboardingPreferences = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)

        // Default mock behaviors
        coEvery { healthConnectManager.checkPermissions() } returns false
        every { securePreferences.hasApiKey() } returns false
        coEvery { preferencesRepository.getString("pref_azure_endpoint", "") } returns ""

        viewModel = OnboardingViewModel(
            healthConnectManager,
            onboardingPreferences,
            securePreferences,
            preferencesRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // When: ViewModel is initialized
        val state = viewModel.state.value

        // Then: Default values should be set
        assertThat(state.currentPage).isEqualTo(0)
        assertThat(state.totalPages).isEqualTo(4)
        assertThat(state.healthConnectPermissionsGranted).isFalse()
        assertThat(state.apiConfigured).isFalse()
    }

    @Test
    fun `checkHealthConnectPermissions should update state on grant`() = runTest {
        // Given: Health Connect permissions granted
        coEvery { healthConnectManager.checkPermissions() } returns true

        // When: Check permissions
        viewModel.checkHealthConnectPermissions()
        advanceUntilIdle()

        // Then: State should update to granted
        assertThat(viewModel.state.value.healthConnectPermissionsGranted).isTrue()
    }

    @Test
    fun `checkHealthConnectPermissions should update state on deny`() = runTest {
        // Given: Health Connect permissions denied
        coEvery { healthConnectManager.checkPermissions() } returns false

        // When: Check permissions
        viewModel.checkHealthConnectPermissions()
        advanceUntilIdle()

        // Then: State should update to not granted
        assertThat(viewModel.state.value.healthConnectPermissionsGranted).isFalse()
    }

    @Test
    fun `checkApiConfigurationStatus should update state when configured`() = runTest {
        // Given: API key and endpoint configured
        every { securePreferences.hasApiKey() } returns true
        coEvery { preferencesRepository.getString("pref_azure_endpoint", "") } returns "https://test.openai.azure.com"

        // When: Check API configuration status
        viewModel.checkApiConfigurationStatus()
        advanceUntilIdle()

        // Then: State should update to configured
        assertThat(viewModel.state.value.apiConfigured).isTrue()
    }

    @Test
    fun `checkApiConfigurationStatus should update state when not configured (no API key)`() = runTest {
        // Given: No API key (endpoint exists)
        every { securePreferences.hasApiKey() } returns false
        coEvery { preferencesRepository.getString("pref_azure_endpoint", "") } returns "https://test.openai.azure.com"

        // When: Check API configuration status
        viewModel.checkApiConfigurationStatus()
        advanceUntilIdle()

        // Then: State should update to not configured
        assertThat(viewModel.state.value.apiConfigured).isFalse()
    }

    @Test
    fun `checkApiConfigurationStatus should update state when not configured (no endpoint)`() = runTest {
        // Given: API key exists but no endpoint
        every { securePreferences.hasApiKey() } returns true
        coEvery { preferencesRepository.getString("pref_azure_endpoint", "") } returns ""

        // When: Check API configuration status
        viewModel.checkApiConfigurationStatus()
        advanceUntilIdle()

        // Then: State should update to not configured
        assertThat(viewModel.state.value.apiConfigured).isFalse()
    }

    @Test
    fun `setCurrentPage should update current page`() {
        // Given: Initial page is 0
        assertThat(viewModel.state.value.currentPage).isEqualTo(0)

        // When: Set current page to 2
        viewModel.setCurrentPage(2)

        // Then: State should update
        assertThat(viewModel.state.value.currentPage).isEqualTo(2)
    }

    @Test
    fun `markOnboardingCompleted should call OnboardingPreferences`() {
        // When: Mark onboarding completed
        viewModel.markOnboardingCompleted()

        // Then: Should delegate to OnboardingPreferences
        verify { onboardingPreferences.markOnboardingCompleted() }
    }

    @Test
    fun `onHealthConnectPermissionResult should update state when all permissions granted`() {
        // Given: All required permissions granted
        val grantedPermissions = setOf(
            "android.permission.health.READ_NUTRITION",
            "android.permission.health.WRITE_NUTRITION",
            "android.permission.health.READ_WEIGHT",
            "android.permission.health.WRITE_WEIGHT",
            "android.permission.health.READ_HEIGHT",
            "android.permission.health.WRITE_HEIGHT",
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED",
        )

        // When: Handle permission result
        viewModel.onHealthConnectPermissionResult(grantedPermissions)

        // Then: State should update to granted
        assertThat(viewModel.state.value.healthConnectPermissionsGranted).isTrue()
    }

    @Test
    fun `onHealthConnectPermissionResult should update state when permissions denied`() {
        // Given: No permissions granted
        val grantedPermissions = emptySet<String>()

        // When: Handle permission result
        viewModel.onHealthConnectPermissionResult(grantedPermissions)

        // Then: State should update to not granted
        assertThat(viewModel.state.value.healthConnectPermissionsGranted).isFalse()
    }

    @Test
    fun `onHealthConnectPermissionResult should update state when partial permissions granted`() {
        // Given: Only READ_NUTRITION granted (WRITE_NUTRITION missing)
        val grantedPermissions = setOf("android.permission.health.READ_NUTRITION")

        // When: Handle permission result
        viewModel.onHealthConnectPermissionResult(grantedPermissions)

        // Then: State should update to not granted (requires all permissions)
        assertThat(viewModel.state.value.healthConnectPermissionsGranted).isFalse()
    }

    @Test
    fun `checkHealthConnectPermissions should handle exceptions gracefully`() = runTest {
        // Given: HealthConnectManager throws exception
        coEvery { healthConnectManager.checkPermissions() } throws SecurityException("Test exception")

        // When: Check permissions
        viewModel.checkHealthConnectPermissions()
        advanceUntilIdle()

        // Then: State should update to not granted (error handled)
        assertThat(viewModel.state.value.healthConnectPermissionsGranted).isFalse()
    }

    @Test
    fun `checkApiConfigurationStatus should handle exceptions gracefully`() = runTest {
        // Given: SecurePreferences throws exception
        every { securePreferences.hasApiKey() } throws Exception("Test exception")

        // When: Check API configuration status
        viewModel.checkApiConfigurationStatus()
        advanceUntilIdle()

        // Then: State should update to not configured (error handled)
        assertThat(viewModel.state.value.apiConfigured).isFalse()
    }
}
