package com.foodie.app.ui.screens.energybalance

import androidx.lifecycle.SavedStateHandle
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

/**
 * Unit tests for EnergyBalanceDashboardViewModel date navigation functionality.
 *
 * Tests date navigation methods (onPreviousDay, onNextDay, onTodayClicked),
 * state updates, repository queries with correct dates, and SavedStateHandle persistence.
 *
 * **Test Coverage:**
 * - AC #1: onPreviousDay updates selectedDate to date.minusDays(1)
 * - AC #4: onNextDay updates selectedDate to date.plusDays(1) when selectedDate < today
 * - AC #5: onNextDay does nothing when selectedDate == today
 * - AC #6: onTodayClicked resets selectedDate to LocalDate.now()
 * - AC #10: selectedDate persists in SavedStateHandle
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnergyBalanceDashboardViewModelDateNavigationTest {

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
    fun onPreviousDay_whenViewingToday_thenUpdatesSelectedDateToYesterday() = runTest {
        // Given: ViewModel viewing today
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val energyBalance = createTestEnergyBalance()

        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: User taps Previous Day button
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: selectedDate is updated to yesterday
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(yesterday)

        // And: Repository queried with yesterday's date
        verify(repository).getEnergyBalance(yesterday)
    }

    @Test
    fun onPreviousDay_whenViewingYesterday_thenUpdatesSelectedDateToTwoDaysAgo() = runTest {
        // Given: ViewModel viewing yesterday (simulate user already navigated back once)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val energyBalance = createTestEnergyBalance()

        // Initialize with today
        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )
        whenever(repository.getEnergyBalance(twoDaysAgo)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Navigate to yesterday first
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // When: User taps Previous Day button again
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: selectedDate is updated to 2 days ago
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(twoDaysAgo)

        // And: Repository queried with 2 days ago date
        verify(repository).getEnergyBalance(twoDaysAgo)
    }

    @Test
    fun onNextDay_whenViewingYesterday_thenUpdatesSelectedDateToToday() = runTest {
        // Given: ViewModel viewing yesterday
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val energyBalance = createTestEnergyBalance()

        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Navigate to yesterday
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // When: User taps Next Day button
        viewModel.onNextDay()
        advanceUntilIdle()

        // Then: selectedDate is updated to today
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(today)
    }

    @Test
    fun onNextDay_whenViewingToday_thenDoesNotChangeDate() = runTest {
        // Given: ViewModel viewing today
        val today = LocalDate.now()
        val energyBalance = createTestEnergyBalance()

        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        val initialState = viewModel.state.value
        assertThat(initialState.selectedDate).isEqualTo(today)

        // When: User taps Next Day button (should be disabled in UI, but test defensively)
        viewModel.onNextDay()
        advanceUntilIdle()

        // Then: selectedDate remains today (cannot view future dates)
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(today)
    }

    @Test
    fun onTodayClicked_whenViewingHistoricalDate_thenResetsSelectedDateToToday() = runTest {
        // Given: ViewModel viewing 7 days ago
        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(7)
        val energyBalance = createTestEnergyBalance()

        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )
        whenever(repository.getEnergyBalance(sevenDaysAgo)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Navigate to 7 days ago
        repeat(7) {
            viewModel.onPreviousDay()
            advanceUntilIdle()
        }

        assertThat(viewModel.state.value.selectedDate).isEqualTo(sevenDaysAgo)

        // When: User taps "Today" button
        viewModel.onTodayClicked()
        advanceUntilIdle()

        // Then: selectedDate is reset to today
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(today)
    }

    @Test
    fun selectedDateChange_whenNavigatingDays_thenTriggersRepositoryQueryWithNewDate() = runTest {
        // Given: ViewModel viewing today
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val energyBalance = createTestEnergyBalance()

        whenever(repository.getEnergyBalance(today)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: User navigates to yesterday
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: Repository queried with yesterday's date
        verify(repository).getEnergyBalance(yesterday)

        // When: User navigates back to today
        viewModel.onNextDay()
        advanceUntilIdle()

        // Then: State is back to today
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(today)
    }

    @Test
    fun selectedDate_whenPersisted_thenRestoresFromSavedStateHandle() = runTest {
        // Given: SavedStateHandle with persisted date (yesterday)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        savedStateHandle["selected_date"] = yesterday.toString()

        val energyBalance = createTestEnergyBalance()
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        // When: ViewModel is recreated (simulates process death + restoration)
        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // Then: selectedDate is restored from SavedStateHandle
        val state = viewModel.state.value
        assertThat(state.selectedDate).isEqualTo(yesterday)

        // And: Repository queried with restored date
        verify(repository).getEnergyBalance(yesterday)
    }

    @Test
    fun selectedDate_whenChanged_thenPersistsToSavedStateHandle() = runTest {
        // Given: ViewModel viewing today
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val energyBalance = createTestEnergyBalance()

        whenever(repository.getEnergyBalance(any())).thenReturn(
            flow { emit(Result.success(energyBalance)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: User navigates to yesterday
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: selectedDate is persisted to SavedStateHandle
        val persistedDate = savedStateHandle.get<String>("selected_date")
        assertThat(persistedDate).isEqualTo(yesterday.toString())
    }

    @Test
    fun historicalDate_whenNoMeals_thenReturnsEnergyBalanceWithZeroCaloriesIn() = runTest {
        // Given: Repository returns energy balance with 0 calories for historical date
        val yesterday = LocalDate.now().minusDays(1)
        val energyBalanceNoMeals = EnergyBalance(
            bmr = 1800.0,
            neat = 320.0,
            activeCalories = 0.0,
            tdee = 2120.0,
            caloriesIn = 0.0,
            deficitSurplus = 2120.0,
        )

        whenever(repository.getEnergyBalance(LocalDate.now())).thenReturn(
            flow { emit(Result.success(createTestEnergyBalance())) },
        )
        whenever(repository.getEnergyBalance(yesterday)).thenReturn(
            flow { emit(Result.success(energyBalanceNoMeals)) },
        )

        viewModel = EnergyBalanceDashboardViewModel(repository, savedStateHandle)
        advanceUntilIdle()

        // When: User navigates to yesterday (no meals logged)
        viewModel.onPreviousDay()
        advanceUntilIdle()

        // Then: State shows energy balance with 0 calories in
        val state = viewModel.state.value
        assertThat(state.energyBalance?.caloriesIn).isEqualTo(0.0)
        assertThat(state.energyBalance?.deficitSurplus).isEqualTo(2120.0)
    }

    /**
     * Helper function to create test EnergyBalance data.
     */
    private fun createTestEnergyBalance() = EnergyBalance(
        bmr = 1800.0,
        neat = 320.0,
        activeCalories = 450.0,
        tdee = 2570.0,
        caloriesIn = 2200.0,
        deficitSurplus = 370.0,
    )
}
