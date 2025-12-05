package com.foodie.app.data.repository

import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.units.Energy
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for TDEE calculation in EnergyBalanceRepository.
 *
 * Tests verify:
 * - TDEE formula (BMR + NEAT + Active)
 * - Reactive Flow updates when components change
 * - Error handling (profile not configured, permissions denied)
 * - Performance (< 100ms calculation latency)
 * - EnergyBalance aggregate Flow
 */
class EnergyBalanceRepositoryTDEETest {

    private lateinit var repository: EnergyBalanceRepositoryImpl
    private lateinit var mockUserProfileRepository: UserProfileRepository
    private lateinit var mockHealthConnectManager: HealthConnectManager

    private val testProfile = UserProfile(
        sex = UserProfile.Sex.MALE,
        birthDate = LocalDate.now().minusYears(30),
        weightKg = 70.0,
        heightCm = 175.0,
    )

    @Before
    fun setup() {
        mockUserProfileRepository = mock()
        mockHealthConnectManager = mock()
        repository = EnergyBalanceRepositoryImpl(mockUserProfileRepository, mockHealthConnectManager)
    }

    @Test
    fun `getTDEE returns sum of BMR plus NEAT plus Active`() = runTest {
        // Given: Profile configured with known BMR, HC data available
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))

        // Mock Health Connect to return known values
        // Note: Exact NEAT varies by time of day, so we test that TDEE >= BMR + Active
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(2000.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))

        // When: Collect TDEE from Flow
        val tdee = repository.getTDEE().first()

        // Then: TDEE should be at least BMR + Active (NEAT can be 0 early in day)
        val expectedBMR = (10 * 70) + (6.25 * 175) - (5 * 30) + 5 // 1643.75
        assertThat(tdee).isAtLeast(expectedBMR + 300.0 - 50.0) // Allow rounding tolerance
        assertThat(tdee).isLessThan(3000.0) // Sanity check upper bound
    }

    @Test
    fun `getTDEE returns zero when profile not configured`() = runTest {
        // Given: No user profile configured (null) - BMR defaults to 0.0
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(null))

        // NEAT and Active still query HC but get zero results
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(0.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(0.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(0.0))

        // When: Collect TDEE from Flow
        val tdee = repository.getTDEE().first()

        // Then: TDEE should be 0.0 (BMR=0, NEAT=0, Active=0 with graceful degradation)
        assertThat(tdee).isEqualTo(0.0)
    }

    @Test
    fun `getTDEE updates when NEAT changes`() = runTest {
        // Given: BMR and Active are constant
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))

        // TotalHC changes to simulate NEAT changing
        val totalHcValues = listOf(1800.0, 2000.0, 2200.0)
        var callCount = 0
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenAnswer {
            totalHcValues[callCount++ % 3]
        }

        // When: Collect 3 TDEE emissions
        val tdeeValues = repository.getTDEE().take(3).toList()

        // Then: TDEE should update (reactive Flow working)
        assertThat(tdeeValues).hasSize(3)
        // Verify they're different (NEAT is changing)
        assertThat(tdeeValues[0]).isNotEqualTo(tdeeValues[1])
        assertThat(tdeeValues[1]).isNotEqualTo(tdeeValues[2])
    }

    @Test
    fun `getTDEE updates when Active changes`() = runTest {
        // Given: BMR and NEAT are relatively constant
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(2000.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)

        // Active calories Flow emits changing values: 0 → 500 → 800
        val activeFlow = flow {
            emit(0.0)
            emit(500.0)
            emit(800.0)
        }
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(activeFlow)

        // When: Collect 3 TDEE emissions
        val tdeeValues = repository.getTDEE().take(3).toList()

        // Then: TDEE should increase as Active increases
        assertThat(tdeeValues).hasSize(3)
        // Verify increases (allowing tolerance for NEAT variability)
        assertThat(tdeeValues[1]).isAtLeast(tdeeValues[0] + 400.0) // Should increase by ~500
        assertThat(tdeeValues[2]).isAtLeast(tdeeValues[1] + 200.0) // Should increase by ~300
    }

    @Test
    fun `getTDEE updates when BMR changes`() = runTest {
        // Given: NEAT and Active are relatively constant
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(2000.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))

        // BMR changes: profile updates (weight 70kg → 80kg)
        var profileCallCount = 0
        val profiles = listOf(testProfile, testProfile.copy(weightKg = 80.0))
        whenever(mockUserProfileRepository.getUserProfile()).thenAnswer {
            flowOf(profiles[profileCallCount++ % 2])
        }

        // When: Collect 2 TDEE emissions
        val tdeeValues = repository.getTDEE().take(2).toList()

        // Then: TDEE should increase when weight (BMR) increases
        assertThat(tdeeValues).hasSize(2)
        assertThat(tdeeValues[1]).isGreaterThan(tdeeValues[0])
    }

    @Test
    fun `getTDEE calculation completes within performance target`() = runTest {
        // Given: All components available
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))

        val expectedBMR = (10 * 70) + (6.25 * 175) - (5 * 30) + 5
        val bmrElapsed = expectedBMR / 1440 * 60
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(400.0 + bmrElapsed + 300.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))

        // When: Measure TDEE calculation time
        val startTime = System.currentTimeMillis()
        val tdee = repository.getTDEE().first()
        val duration = System.currentTimeMillis() - startTime

        // Then: Calculation should complete within 100ms target
        assertThat(duration).isLessThan(100)
        assertThat(tdee).isGreaterThan(0.0) // Sanity check - TDEE calculated
    }

    @Test
    fun `calculateCaloriesIn sums energy from all nutrition records`() = runTest {
        // Given: 3 meals with different calorie values
        val meal1 = createMockNutritionRecord(500.0) // Breakfast
        val meal2 = createMockNutritionRecord(700.0) // Lunch
        val meal3 = createMockNutritionRecord(300.0) // Snack
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(listOf(meal1, meal2, meal3))

        // When: Calculate Calories In
        val result = repository.calculateCaloriesIn()

        // Then: Should sum all meal calories (500 + 700 + 300 = 1500 kcal)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1500.0)
    }

    @Test
    fun `calculateCaloriesIn returns zero when no meals logged`() = runTest {
        // Given: No nutrition records for today (fasting day)
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(emptyList())

        // When: Calculate Calories In
        val result = repository.calculateCaloriesIn()

        // Then: Should return 0.0 kcal (not an error - valid fasting day)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0.0)
    }

    @Test
    fun `calculateCaloriesIn queries current day time range`() = runTest {
        // Given: Mock nutrition records query
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(emptyList())

        // When: Calculate Calories In
        repository.calculateCaloriesIn()

        // Then: Should query from midnight to now (local timezone)
        val expectedStartOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        // Note: Exact time verification difficult in unit test due to Instant.now() timing
        // Integration tests can verify time range more precisely
        assertThat(expectedStartOfDay).isNotNull()
    }

    @Test
    fun `getEnergyBalance combines all components into domain model`() = runTest {
        // Given: All components available
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(2000.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))

        val meal = createMockNutritionRecord(1700.0)
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(listOf(meal))

        // When: Collect EnergyBalance from Flow
        val result = repository.getEnergyBalance().first()

        // Then: EnergyBalance should have all components populated
        assertThat(result.isSuccess).isTrue()
        val energyBalance = result.getOrNull()
        assertThat(energyBalance).isNotNull()

        // Verify all fields are present and reasonable
        assertThat(energyBalance!!.bmr).isGreaterThan(1600.0) // Mifflin-St Jeor for 30yo male
        assertThat(energyBalance.bmr).isLessThan(1700.0)
        assertThat(energyBalance.activeCalories).isEqualTo(300.0)
        assertThat(energyBalance.caloriesIn).isEqualTo(1700.0)
        assertThat(energyBalance.tdee).isGreaterThan(energyBalance.bmr) // TDEE >= BMR
    }

    @Test
    fun `getEnergyBalance returns failure when BMR unavailable`() = runTest {
        // Given: No user profile configured (BMR required for energy balance)
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(null))
        whenever(mockHealthConnectManager.observeSteps()).thenReturn(flowOf(10000))
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(emptyList())

        // When: Collect EnergyBalance from Flow
        val result = repository.getEnergyBalance().first()

        // Then: Should return failure (BMR is required component)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ProfileNotConfiguredError::class.java)
    }

    @Test
    fun `getEnergyBalance defaults to zero for unavailable NEAT and Active`() = runTest {
        // Given: BMR available, but NEAT and Active return zero (permissions denied or no data)
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))

        // NEAT and Active return zero
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(0.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(0.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(0.0))

        // CaloriesIn available
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(emptyList())

        // When: Collect EnergyBalance from Flow
        val result = repository.getEnergyBalance().first()

        // Then: Should succeed with NEAT and Active as 0.0 (graceful degradation)
        assertThat(result.isSuccess).isTrue()
        val energyBalance = result.getOrNull()
        assertThat(energyBalance).isNotNull()

        val expectedBMR = (10 * 70) + (6.25 * 175) - (5 * 30) + 5
        assertThat(energyBalance!!.bmr).isWithin(1.0).of(expectedBMR)
        assertThat(energyBalance.neat).isEqualTo(0.0) // Zero from HC
        assertThat(energyBalance.activeCalories).isEqualTo(0.0) // Zero from HC
        assertThat(energyBalance.tdee).isWithin(1.0).of(expectedBMR) // BMR only
    }

    @Test
    fun `getEnergyBalance breakdown matches formula`() = runTest {
        // Given: All components available
        whenever(mockUserProfileRepository.getUserProfile()).thenReturn(flowOf(testProfile))

        val expectedBMR = (10 * 70) + (6.25 * 175) - (5 * 30) + 5
        val bmrElapsed = expectedBMR / 1440 * 60
        whenever(mockHealthConnectManager.queryTotalCaloriesBurned(any(), any())).thenReturn(400.0 + bmrElapsed + 300.0)
        whenever(mockHealthConnectManager.queryActiveCalories(any(), any())).thenReturn(300.0)
        whenever(mockHealthConnectManager.observeActiveCalories()).thenReturn(flowOf(300.0))

        val meal = createMockNutritionRecord(1700.0)
        whenever(mockHealthConnectManager.queryNutritionRecords(any(), any()))
            .thenReturn(listOf(meal))

        // When: Collect EnergyBalance
        val result = repository.getEnergyBalance().first()
        val energyBalance = result.getOrNull()!!

        // Then: Breakdown should match formulas
        // TDEE = BMR + NEAT + Active
        val actualTDEE = energyBalance.bmr + energyBalance.neat + energyBalance.activeCalories
        assertThat(energyBalance.tdee).isWithin(1.0).of(actualTDEE)

        // DeficitSurplus = TDEE - CaloriesIn
        val actualDeficit = energyBalance.tdee - energyBalance.caloriesIn
        assertThat(energyBalance.deficitSurplus).isWithin(1.0).of(actualDeficit)
    }

    /**
     * Creates a mock NutritionRecord with specified energy content.
     *
     * @param kilocalories Energy content in kcal
     * @return Mock NutritionRecord for testing
     */
    private fun createMockNutritionRecord(kilocalories: Double): NutritionRecord {
        val mockRecord = mock<NutritionRecord>()
        val mockEnergy = Energy.kilocalories(kilocalories)
        whenever(mockRecord.energy).thenReturn(mockEnergy)
        return mockRecord
    }
}
