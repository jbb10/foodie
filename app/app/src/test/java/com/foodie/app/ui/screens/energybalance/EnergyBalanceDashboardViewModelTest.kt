package com.foodie.app.ui.screens.energybalance

import androidx.lifecycle.SavedStateHandle
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
import java.time.LocalDate

/**
 * Unit tests for EnergyBalanceDashboardViewModel.
 *
 * Tests state management, repository Flow collection, refresh(), and error handling.
 * Uses StandardTestDispatcher for coroutine testing and Mockito for repository mocking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnergyBalanceDashboardViewModelTest {

    private lateinit var repository: EnergyBalanceRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: EnergyBalanceDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_whenRepositoryEmitsSuccess_thenUpdatesStateWithEnergyBalance() = runTest {
        // Given: Repository emits successful energy balance
        val energyBalance = createTestEnergyBalance()
        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        // When: ViewModel is created (init block collects Flow)
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
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
        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow { emit(Result.failure(Exception("Network error"))) },
        )

        // When: ViewModel is created
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
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
        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow { emit(Result.failure(ProfileNotConfiguredError("Profile not configured"))) },
        )

        // When: ViewModel is created
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Then: State shows error with settings button
        val state = viewModel.state.value
        assertThat(state.error).isEqualTo("Profile not configured")
        assertThat(state.showSettingsButton).isTrue()
    }

    @Test
    fun refresh_whenCalled_thenSetsIsLoadingTrue() = runTest {
        // Given: ViewModel with initial state
        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
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
        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow { emit(Result.failure(Exception("Error"))) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
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

        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow {
                emit(Result.success(energyBalance1))
                emit(Result.success(energyBalance2))
            },
        )

        // When: ViewModel collects Flow
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Then: State reflects latest emission
        val state = viewModel.state.value
        assertThat(state.energyBalance).isEqualTo(energyBalance2)
        assertThat(state.energyBalance?.caloriesIn).isEqualTo(2000.0)
    }

    // ========================================================================================
    // Date Navigation Tests (Story 6-7)
    // ========================================================================================

    @Test
    fun onPreviousDay_whenCalled_thenUpdatesSelectedDateToDateMinusOne() = runTest {
        // Given: ViewModel initialized with today's date
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: onPreviousDay() is called
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: selectedDate is updated to yesterday
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(yesterday)
    }

    @Test
    fun onNextDay_whenSelectedDateIsBeforeToday_thenUpdatesSelectedDateToDatePlusOne() = runTest {
        // Given: ViewModel with selectedDate = 3 days ago
        val today = LocalDate.now()
        val threeDaysAgo = today.minusDays(3)
        val twoDaysAgo = today.minusDays(2)
        savedStateHandle["selected_date"] = threeDaysAgo.toString()
        whenever(repository.getEnergyBalance(threeDaysAgo)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        whenever(repository.getEnergyBalance(twoDaysAgo)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: onNextDay() is called
        viewModel.onNextDay()
        advanceUntilIdle()

        // Then: selectedDate is updated to 2 days ago
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(twoDaysAgo)
    }

    @Test
    fun onNextDay_whenSelectedDateIsToday_thenDoesNotChangeDate() = runTest {
        // Given: ViewModel with selectedDate = today
        val today = LocalDate.now()
        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: onNextDay() is called
        viewModel.onNextDay()
        advanceUntilIdle()

        // Then: selectedDate remains today (cannot navigate to future)
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(today)
    }

    @Test
    fun onTodayClicked_whenSelectedDateIsHistorical_thenResetsSelectedDateToToday() = runTest {
        // Given: ViewModel with selectedDate = 7 days ago
        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(7)
        savedStateHandle["selected_date"] = sevenDaysAgo.toString()
        whenever(repository.getEnergyBalance(sevenDaysAgo)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: onTodayClicked() is called
        viewModel.onTodayClicked()
        advanceUntilIdle()

        // Then: selectedDate is reset to today
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(today)
    }

    @Test
    fun selectedDateChange_whenDateChanges_thenTriggersRepositoryQueryWithNewDate() = runTest {
        // Given: ViewModel initialized with today
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val energyBalanceToday = createTestEnergyBalance(caloriesIn = 2000.0)
        val energyBalanceYesterday = createTestEnergyBalance(caloriesIn = 1500.0)
        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalanceToday)) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalanceYesterday)) },
        )
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Verify initial state is today
        assertThat(viewModel.state.value.selectedDate).isEqualTo(today)
        assertThat(viewModel.state.value.energyBalance?.caloriesIn).isEqualTo(2000.0)

        // When: Navigate to yesterday
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: Repository is queried with yesterday's date and state updates
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(yesterday)
        assertThat(state.energyBalance?.caloriesIn).isEqualTo(1500.0)
    }

    @Test
    fun selectedDate_whenViewModelRecreatedWithSavedStateHandle_thenDatePersists() = runTest {
        // Given: ViewModel with selectedDate = 5 days ago, saved to SavedStateHandle
        val today = LocalDate.now()
        val fiveDaysAgo = today.minusDays(5)
        savedStateHandle["selected_date"] = fiveDaysAgo.toString()
        whenever(repository.getEnergyBalance(fiveDaysAgo)).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )

        // When: ViewModel is recreated with same SavedStateHandle (simulates process death)
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Then: selectedDate is restored from SavedStateHandle
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(fiveDaysAgo)
    }

    @Test
    fun historicalDate_whenNoMealsLogged_thenReturnsEnergyBalanceWithZeroCaloriesIn() = runTest {
        // Given: Repository returns energy balance with caloriesIn = 0 for historical date
        val yesterday = LocalDate.now().minusDays(1)
        val energyBalanceNoMeals = createTestEnergyBalance(caloriesIn = 0.0, deficitSurplus = 2500.0)
        savedStateHandle["selected_date"] = yesterday.toString()
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalanceNoMeals)) },
        )

        // When: ViewModel is created with historical date
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Then: State shows energy balance with 0 calories in
        val state = viewModel.state.value
        assertThat(state.energyBalance?.caloriesIn).isEqualTo(0.0)
        assertThat(state.energyBalance?.deficitSurplus).isEqualTo(2500.0) // Full TDEE deficit
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
