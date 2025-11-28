package com.foodie.app.ui.screens.energybalance

import com.foodie.app.data.repository.ProfileNotConfiguredError
import com.foodie.app.domain.model.EnergyBalance
import com.foodie.app.domain.repository.EnergyBalanceRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for EnergyBalanceDashboardViewModel.
 *
 * Tests state management, repository Flow collection, refresh(), and error handling.
 * Uses StandardTestDispatcher for coroutine testing and Mockito for repository mocking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnergyBalanceDashboardViewModelTest {

    private lateinit var repository: EnergyBalanceRepository
    private lateinit var viewModel: EnergyBalanceDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_whenRepositoryEmitsSuccess_thenUpdatesStateWithEnergyBalance() = runTest {
        // Given: Repository emits successful energy balance
        val energyBalance = createTestEnergyBalance()
        whenever(repository.getEnergyBalance()).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        // When: ViewModel is created (init block collects Flow)
        viewModel = EnergyBalanceDashboardViewModel(repository)
        advanceUntilIdle()

        // Then: State is updated with energy balance data
        val state = viewModel.state.value
        assertThat(state.energyBalance).isEqualTo(energyBalance)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.lastUpdated).isNotNull()
    }

    @Test
    fun init_whenRepositoryEmitsFailure_thenUpdatesStateWithError() = runTest {
        // Given: Repository emits failure
        whenever(repository.getEnergyBalance()).thenReturn(
            flow { emit(Result.failure(Exception("Network error"))) },
        )

        // When: ViewModel is created
        viewModel = EnergyBalanceDashboardViewModel(repository)
        advanceUntilIdle()

        // Then: State shows error
        val state = viewModel.state.value
        assertThat(state.energyBalance).isNull()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo("Network error")
        assertThat(state.showSettingsButton).isFalse()
    }

    @Test
    fun init_whenRepositoryEmitsProfileNotConfiguredError_thenSetsShowSettingsButton() = runTest {
        // Given: Repository emits ProfileNotConfiguredError
        whenever(repository.getEnergyBalance()).thenReturn(
            flow { emit(Result.failure(ProfileNotConfiguredError("Profile not configured"))) },
        )

        // When: ViewModel is created
        viewModel = EnergyBalanceDashboardViewModel(repository)
        advanceUntilIdle()

        // Then: State shows error with settings button
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo("Profile not configured")
        assertThat(state.showSettingsButton).isTrue()
    }

    @Test
    fun refresh_whenCalled_thenSetsIsLoadingTrue() = runTest {
        // Given: ViewModel with initial state
        whenever(repository.getEnergyBalance()).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository)
        advanceUntilIdle()

        // When: refresh() is called
        viewModel.refresh()

        // Then: isLoading is set to true
        val state = viewModel.state.value
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun onRetryAfterError_whenCalled_thenClearsErrorAndSetsIsLoading() = runTest {
        // Given: ViewModel in error state
        whenever(repository.getEnergyBalance()).thenReturn(
            flow { emit(Result.failure(Exception("Error"))) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository)
        advanceUntilIdle()

        // Verify initial error state
        assertThat(viewModel.state.value.error).isNotNull()

        // When: onRetryAfterError() is called
        viewModel.onRetryAfterError()

        // Then: Error is cleared and isLoading is true
        val state = viewModel.state.value
        assertThat(state.error).isNull()
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun realTimeUpdates_whenRepositoryEmitsNewData_thenStateUpdates() = runTest {
        // Given: Repository emits multiple energy balance updates
        val energyBalance1 = createTestEnergyBalance(caloriesIn = 1500.0)
        val energyBalance2 = createTestEnergyBalance(caloriesIn = 2000.0)

        whenever(repository.getEnergyBalance()).thenReturn(
            flow {
                emit(Result.success(energyBalance1))
                emit(Result.success(energyBalance2))
            },
        )

        // When: ViewModel collects Flow
        viewModel = EnergyBalanceDashboardViewModel(repository)
        advanceUntilIdle()

        // Then: State reflects latest emission
        val state = viewModel.state.value
        assertThat(state.energyBalance).isEqualTo(energyBalance2)
        assertThat(state.energyBalance?.caloriesIn).isEqualTo(2000.0)
    }

    // Helper function to create test energy balance
    private fun createTestEnergyBalance(
        bmr: Double = 1650.0,
        neat: Double = 400.0,
        activeCalories: Double = 450.0,
        tdee: Double = 2500.0,
        caloriesIn: Double = 2000.0,
        deficitSurplus: Double = 500.0,
    ) = EnergyBalance(
        bmr = bmr,
        neat = neat,
        activeCalories = activeCalories,
        tdee = tdee,
        caloriesIn = caloriesIn,
        deficitSurplus = deficitSurplus,
    )
}
