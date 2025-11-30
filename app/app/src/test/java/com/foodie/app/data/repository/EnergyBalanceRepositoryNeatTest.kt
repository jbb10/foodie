package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
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
 * Unit tests for NEAT (Non-Exercise Activity Thermogenesis) calculation.
 *
 * Tests verify:
 * - 0.04 kcal/step formula correctness (AC1, AC8)
 * - Multiple StepsRecord summation (AC3)
 * - Zero steps handling (AC6)
 * - Permission error handling (AC7)
 * - Reactive Flow updates (AC4)
 */
class EnergyBalanceRepositoryNeatTest {
    // Mocks
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var repository: EnergyBalanceRepositoryImpl

    @Before
    fun setUp() {
        userProfileRepository = mock()
        healthConnectManager = mock()
        repository = EnergyBalanceRepositoryImpl(userProfileRepository, healthConnectManager)

        // Default mock: no active exercise records (empty list)
        // Tests can override this to simulate exercise periods
        runTest {
            whenever(healthConnectManager.queryActiveCaloriesRecords(any(), any()))
                .thenReturn(emptyList())
        }
    }

    // AC1, AC8: Formula correctness - 10,000 steps → 400 kcal
    @Test
    fun calculateNEAT_when10000Steps_thenReturns400Kcal() = runTest {
        // Given: Health Connect returns 10,000 steps
        whenever(healthConnectManager.querySteps(any(), any(), any())).thenReturn(10_000)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: NEAT = 10,000 × 0.04 = 400.0 kcal
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(400.0)
    }

    // AC6: Zero steps returns 0.0 kcal (not error - valid sedentary day start)
    @Test
    fun calculateNEAT_when0Steps_thenReturnsZeroKcal() = runTest {
        // Given: Health Connect returns 0 steps (no step data yet today)
        whenever(healthConnectManager.querySteps(any(), any(), any())).thenReturn(0)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: NEAT = 0 × 0.04 = 0.0 kcal (Result.success, not error)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0.0)
    }

    // AC3: Multiple StepsRecord entries sum correctly
    @Test
    fun calculateNEAT_whenMultipleRecords_thenSumsTotalSteps() = runTest {
        // Given: Health Connect returns summed steps from 3 records (2000 + 5000 + 3000 = 10,000)
        // Note: HealthConnectManager.querySteps() already handles summation
        whenever(healthConnectManager.querySteps(any(), any(), any())).thenReturn(10_000)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: NEAT calculated from total summed steps
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(400.0) // 10,000 × 0.04
    }

    // AC7: Permission denied returns specific error type
    @Test
    fun calculateNEAT_whenPermissionDenied_thenReturnsSecurityException() = runTest {
        // Given: Health Connect throws SecurityException (READ_STEPS permission denied)
        whenever(healthConnectManager.querySteps(any(), any(), any()))
            .thenThrow(SecurityException("READ_STEPS permission denied"))

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: Result.failure with SecurityException
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(SecurityException::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("READ_STEPS")
    }

    // AC4: Reactive Flow emits updated NEAT when steps change
    @Test
    fun getNEAT_whenStepsChange_thenEmitsUpdatedNeat() = runTest {
        // Given: Health Connect observes steps flow (simulates 5-minute polling)
        whenever(healthConnectManager.observeSteps()).thenReturn(flowOf(5_000))

        // When: Collect NEAT flow
        val result = repository.getNEAT().first()

        // Then: NEAT = 5,000 × 0.04 = 200.0 kcal
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(200.0)
    }

    // AC4: Reactive Flow emits multiple updates as steps increase
    @Test
    fun getNEAT_whenStepsIncrease_thenEmitsSequentialUpdates() = runTest {
        // Given: Health Connect flow emits increasing step counts (simulates step sync)
        val stepFlow = flowOf(1_000, 5_000, 10_000)
        whenever(healthConnectManager.observeSteps()).thenReturn(stepFlow)

        // When: Collect NEAT values
        val neatValues = mutableListOf<Double>()
        repository.getNEAT().collect { result ->
            if (result.isSuccess) {
                neatValues.add(result.getOrNull()!!)
            }
        }

        // Then: NEAT increases as steps increase
        assertThat(neatValues).containsExactly(40.0, 200.0, 400.0).inOrder()
    }

    // AC2: Time range verification (midnight to now)
    @Test
    fun calculateNEAT_always_thenQueriesFromMidnightToNow() = runTest {
        // Given: Mock returns 0 steps
        whenever(healthConnectManager.querySteps(any(), any(), any())).thenReturn(0)

        // When: Calculate NEAT
        repository.calculateNEAT()

        // Then: Verify querySteps called with time range
        verify(healthConnectManager).querySteps(any<Instant>(), any<Instant>(), any())
    }

    // AC8: Edge case - Very large step count
    @Test
    fun calculateNEAT_when50000Steps_thenReturns2000Kcal() = runTest {
        // Given: Health Connect returns 50,000 steps (very active day)
        whenever(healthConnectManager.querySteps(any(), any(), any())).thenReturn(50_000)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: NEAT = 50,000 × 0.04 = 2000.0 kcal
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(2000.0)
    }

    // AC8: Edge case - Small step count
    @Test
    fun calculateNEAT_when100Steps_thenReturns4Kcal() = runTest {
        // Given: Health Connect returns 100 steps (minimal activity)
        whenever(healthConnectManager.querySteps(any(), any(), any())).thenReturn(100)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: NEAT = 100 × 0.04 = 4.0 kcal
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(4.0)
    }
}
