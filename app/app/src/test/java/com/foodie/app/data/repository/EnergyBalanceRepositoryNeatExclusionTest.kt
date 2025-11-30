package com.foodie.app.data.repository

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.units.Energy
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for NEAT calculation with exercise step exclusion.
 *
 * Verifies that steps taken during active exercise periods are correctly
 * excluded from NEAT calculations to prevent double-counting.
 *
 * **Scientific Rationale:**
 * - NEAT = Non-Exercise Activity Thermogenesis (passive movement only)
 * - Active calories from Garmin already include energy from steps during exercise
 * - Including exercise steps in both NEAT and Active would overestimate TDEE
 */
class EnergyBalanceRepositoryNeatExclusionTest {
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

    @Test
    fun calculateNEAT_whenActiveExerciseExists_thenQueriesActiveRecordsForTimeRanges() = runTest {
        // Given: Active exercise from 8:00-9:00 AM (morning run)
        val morningRunStart = LocalDate.now()
            .atTime(8, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val morningRunEnd = morningRunStart.plusSeconds(3600) // 1 hour run

        val activeRecord = createMockActiveRecord(morningRunStart, morningRunEnd, 500.0)
        whenever(healthConnectManager.queryActiveCaloriesRecords(any(), any()))
            .thenReturn(listOf(activeRecord))

        whenever(healthConnectManager.querySteps(any(), any(), any()))
            .thenReturn(8_000) // 8000 non-exercise steps

        // When: Calculate NEAT
        repository.calculateNEAT()

        // Then: queryActiveCaloriesRecords was called to get exercise time ranges
        verify(healthConnectManager).queryActiveCaloriesRecords(any(), any())

        // And: querySteps was called with the exercise time range for exclusion
        verify(healthConnectManager).querySteps(
            any(),
            any(),
            argThat { excludeList ->
                excludeList.size == 1 &&
                    excludeList[0].first == morningRunStart &&
                    excludeList[0].second == morningRunEnd
            },
        )
    }

    @Test
    fun calculateNEAT_whenNoActiveExercise_thenQueriesStepsWithEmptyExclusionList() = runTest {
        // Given: No active exercise today (rest day)
        whenever(healthConnectManager.queryActiveCaloriesRecords(any(), any()))
            .thenReturn(emptyList())

        whenever(healthConnectManager.querySteps(any(), any(), any()))
            .thenReturn(10_000)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: Steps queried with empty exclusion list
        verify(healthConnectManager).querySteps(
            any(),
            any(),
            argThat { excludeList -> excludeList.isEmpty() },
        )

        // And: NEAT calculated from all 10,000 steps
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(400.0) // 10,000 × 0.04
    }

    @Test
    fun calculateNEAT_whenMultipleExerciseSessions_thenExcludesAllExercisePeriods() = runTest {
        // Given: Two exercise sessions today (morning run + evening lift)
        val morningRunStart = LocalDate.now()
            .atTime(8, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val morningRunEnd = morningRunStart.plusSeconds(3600) // 8:00-9:00

        val eveningLiftStart = LocalDate.now()
            .atTime(18, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val eveningLiftEnd = eveningLiftStart.plusSeconds(2700) // 18:00-18:45

        val morningRecord = createMockActiveRecord(morningRunStart, morningRunEnd, 500.0)
        val eveningRecord = createMockActiveRecord(eveningLiftStart, eveningLiftEnd, 200.0)

        whenever(healthConnectManager.queryActiveCaloriesRecords(any(), any()))
            .thenReturn(listOf(morningRecord, eveningRecord))

        whenever(healthConnectManager.querySteps(any(), any(), any()))
            .thenReturn(6_000) // 6000 non-exercise steps

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: Steps queried with both exercise time ranges for exclusion
        verify(healthConnectManager).querySteps(
            any(),
            any(),
            argThat { excludeList ->
                excludeList.size == 2 &&
                    excludeList[0].first == morningRunStart &&
                    excludeList[0].second == morningRunEnd &&
                    excludeList[1].first == eveningLiftStart &&
                    excludeList[1].second == eveningLiftEnd
            },
        )

        // And: NEAT calculated from non-exercise steps only
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(240.0) // 6,000 × 0.04
    }

    @Test
    fun calculateNEAT_whenActiveCaloriesPermissionDenied_thenProceedsWithoutExclusion() = runTest {
        // Given: Active calories permission denied (user hasn't granted permission)
        whenever(healthConnectManager.queryActiveCaloriesRecords(any(), any()))
            .thenThrow(SecurityException("READ_ACTIVE_CALORIES_BURNED permission denied"))

        whenever(healthConnectManager.querySteps(any(), any(), any()))
            .thenReturn(10_000)

        // When: Calculate NEAT
        val result = repository.calculateNEAT()

        // Then: NEAT calculation succeeds without exercise exclusion
        assertThat(result.isSuccess).isTrue()

        // And: Steps queried with empty exclusion list (graceful degradation)
        verify(healthConnectManager).querySteps(
            any(),
            any(),
            argThat { excludeList -> excludeList.isEmpty() },
        )

        // And: NEAT calculated from all steps (conservative approach when permission denied)
        assertThat(result.getOrNull()).isEqualTo(400.0) // 10,000 × 0.04
    }

    /**
     * Creates a mock ActiveCaloriesBurnedRecord for testing.
     */
    private fun createMockActiveRecord(
        startTime: Instant,
        endTime: Instant,
        calories: Double,
    ): ActiveCaloriesBurnedRecord {
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)
        return mock {
            on { this.startTime }.thenReturn(startTime)
            on { this.endTime }.thenReturn(endTime)
            on { energy }.thenReturn(Energy.kilocalories(calories))
        }
    }
}
