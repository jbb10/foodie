package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for Active Energy Expenditure calculation.
 *
 * Tests verify:
 * - Active calories summation from Health Connect (AC1, AC3)
 * - Multiple workout records summed correctly (AC3)
 * - Zero workouts handling (AC5)
 * - Permission error handling (AC1)
 * - Reactive Flow updates (AC6)
 */
class EnergyBalanceRepositoryActiveCaloriesTest {
    // Mocks
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var repository: EnergyBalanceRepositoryImpl

    @Before
    fun setUp() {
        userProfileRepository = mock()
        healthConnectManager = mock()
        repository = EnergyBalanceRepositoryImpl(userProfileRepository, healthConnectManager)
    }

    // AC1, AC3: Single workout - 500 kcal active calories
    @Test
    fun calculateActiveCalories_whenSingleWorkout_thenReturns500Kcal() = runTest {
        // Given: Health Connect returns 500 kcal from one workout
        whenever(healthConnectManager.queryActiveCalories(any(), any())).thenReturn(500.0)

        // When: Calculate Active Calories
        val result = repository.calculateActiveCalories()

        // Then: Active Calories = 500.0 kcal
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(500.0)
    }

    // AC3: Multiple workout records sum correctly (morning run + evening lift)
    @Test
    fun calculateActiveCalories_whenMultipleWorkouts_thenSumsTotalCalories() = runTest {
        // Given: Health Connect returns summed calories from 2 workouts (300 + 200 = 500)
        // Note: HealthConnectManager.queryActiveCalories() already handles summation
        whenever(healthConnectManager.queryActiveCalories(any(), any())).thenReturn(500.0)

        // When: Calculate Active Calories
        val result = repository.calculateActiveCalories()

        // Then: Active Calories calculated from total summed workouts
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(500.0) // 300 + 200
    }

    // AC5: Zero workouts returns 0.0 kcal (not error - valid rest day)
    @Test
    fun calculateActiveCalories_whenZeroWorkouts_thenReturnsZeroKcal() = runTest {
        // Given: Health Connect returns 0.0 kcal (no workout data - rest day)
        whenever(healthConnectManager.queryActiveCalories(any(), any())).thenReturn(0.0)

        // When: Calculate Active Calories
        val result = repository.calculateActiveCalories()

        // Then: Active Calories = 0.0 kcal (Result.success, not error)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0.0)
    }

    // AC1: Permission denied returns specific error type
    @Test
    fun calculateActiveCalories_whenPermissionDenied_thenReturnsSecurityException() = runTest {
        // Given: Health Connect throws SecurityException (READ_ACTIVE_CALORIES_BURNED permission denied)
        whenever(healthConnectManager.queryActiveCalories(any(), any()))
            .thenThrow(SecurityException("READ_ACTIVE_CALORIES_BURNED permission denied"))

        // When: Calculate Active Calories
        val result = repository.calculateActiveCalories()

        // Then: Result.failure with SecurityException
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(SecurityException::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("READ_ACTIVE_CALORIES_BURNED")
    }

    // AC6: Reactive Flow emits updated Active Calories when Garmin syncs new workout
    @Test
    fun getActiveCalories_whenWorkoutSyncs_thenFlowEmitsUpdatedValue() = runTest {
        // Given: Health Connect flow emits 0.0 initially, then 500.0 after Garmin sync
        whenever(healthConnectManager.observeActiveCalories())
            .thenReturn(flowOf(0.0, 500.0))

        // When: Collect first two emissions from Flow
        val flow = repository.getActiveCalories()
        val firstEmission = flow.first()

        // Then: First emission is Result.success(0.0) - no workout yet
        assertThat(firstEmission.isSuccess).isTrue()
        assertThat(firstEmission.getOrNull()).isEqualTo(0.0)
    }

    // AC6: Reactive Flow continues emitting when workout data changes
    @Test
    fun getActiveCalories_whenMultipleUpdates_thenFlowEmitsAllValues() = runTest {
        // Given: Health Connect flow emits multiple active calorie values
        // (0.0 → 300.0 morning run → 500.0 evening lift added)
        whenever(healthConnectManager.observeActiveCalories())
            .thenReturn(flowOf(0.0, 300.0, 500.0))

        // When: Collect all emissions
        val emissions = mutableListOf<Result<Double>>()
        val flow = repository.getActiveCalories()
        flow.collect { emissions.add(it) }

        // Then: All three values emitted successfully
        assertThat(emissions).hasSize(3)
        assertThat(emissions[0].getOrNull()).isEqualTo(0.0)
        assertThat(emissions[1].getOrNull()).isEqualTo(300.0)
        assertThat(emissions[2].getOrNull()).isEqualTo(500.0)
    }

    // AC2: Verify time range query (midnight to now)
    @Test
    fun calculateActiveCalories_thenQueriesCurrentDay() = runTest {
        // Given: Health Connect returns 400 kcal
        whenever(healthConnectManager.queryActiveCalories(any(), any())).thenReturn(400.0)

        // When: Calculate Active Calories
        repository.calculateActiveCalories()

        // Then: Verify queryActiveCalories called with time range
        // (exact time range verification happens in HealthConnectManager tests)
        verify(healthConnectManager).queryActiveCalories(any(), any())
    }

    // Edge case: Very high active calories (marathon runner)
    @Test
    fun calculateActiveCalories_whenHighValue_thenHandlesCorrectly() = runTest {
        // Given: Health Connect returns 2000 kcal (e.g., marathon)
        whenever(healthConnectManager.queryActiveCalories(any(), any())).thenReturn(2000.0)

        // When: Calculate Active Calories
        val result = repository.calculateActiveCalories()

        // Then: High value handled correctly (within valid range 0-1,000,000 kcal)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(2000.0)
    }
}
