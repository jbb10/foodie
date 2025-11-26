package com.foodie.app.data.repository

import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.model.ValidationError
import com.foodie.app.domain.repository.UserProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

/**
 * Unit tests for EnergyBalanceRepositoryImpl BMR calculation.
 *
 * **Test Coverage:**
 * - Mifflin-St Jeor formula correctness (male/female)
 * - Known test case validation (30y male, 75.5kg, 178cm = ~1715 kcal)
 * - Known test case validation (30y female, 60kg, 165cm = ~1320 kcal)
 * - Null profile handling (profile not configured)
 * - Invalid profile handling (validation errors)
 * - Boundary value testing (min/max age, weight, height)
 * - Reactive flow updates (getBMR)
 */
class EnergyBalanceRepositoryTest {

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var energyBalanceRepository: EnergyBalanceRepositoryImpl

    @Before
    fun setup() {
        userProfileRepository = mock()
        energyBalanceRepository = EnergyBalanceRepositoryImpl(userProfileRepository)
    }

    // ========================================
    // Known Test Cases (AC #7)
    // ========================================

    @Test
    fun calculateBMR_whenMale30y75kg178cm_thenReturnsApprox1715() = runTest {
        // Given: Male, 30 years old, 75.5kg, 178cm
        val birthDate = LocalDate.now().minusYears(30)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDate,
            weightKg = 75.5,
            heightCm = 178.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: BMR should be approximately 1715 kcal/day
        assertThat(result.isSuccess).isTrue()
        val bmr = result.getOrThrow()

        // Formula: (10 * 75.5) + (6.25 * 178) - (5 * 30) + 5 = 755 + 1112.5 - 150 + 5 = 1722.5
        assertThat(bmr).isWithin(10.0).of(1722.5)
    }

    @Test
    fun calculateBMR_whenFemale30y60kg165cm_thenReturnsApprox1320() = runTest {
        // Given: Female, 30 years old, 60kg, 165cm
        val birthDate = LocalDate.now().minusYears(30)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDate,
            weightKg = 60.0,
            heightCm = 165.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: BMR should be approximately 1320 kcal/day
        assertThat(result.isSuccess).isTrue()
        val bmr = result.getOrThrow()

        // Formula: (10 * 60) + (6.25 * 165) - (5 * 30) - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        assertThat(bmr).isWithin(10.0).of(1320.25)
    }

    // ========================================
    // Male/Female Formula Verification (AC #2, #3)
    // ========================================

    @Test
    fun calculateBMR_whenMale_thenUsesPlus5Adjustment() = runTest {
        // Given: Male profile
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(25),
            weightKg = 70.0,
            heightCm = 175.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Formula uses +5 sex adjustment
        // (10 * 70) + (6.25 * 175) - (5 * 25) + 5 = 700 + 1093.75 - 125 + 5 = 1673.75
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isWithin(0.01).of(1673.75)
    }

    @Test
    fun calculateBMR_whenFemale_thenUsesMinus161Adjustment() = runTest {
        // Given: Female profile
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(25),
            weightKg = 70.0,
            heightCm = 175.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Formula uses -161 sex adjustment
        // (10 * 70) + (6.25 * 175) - (5 * 25) - 161 = 700 + 1093.75 - 125 - 161 = 1507.75
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isWithin(0.01).of(1507.75)
    }

    // ========================================
    // Boundary Value Testing (AC #7 - Task 3.4)
    // ========================================

    @Test
    fun calculateBMR_whenMinAge13_thenCalculatesCorrectly() = runTest {
        // Given: Minimum age (13 years)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(13),
            weightKg = 50.0,
            heightCm = 150.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Calculation succeeds
        assertThat(result.isSuccess).isTrue()
        // Formula: (10 * 50) + (6.25 * 150) - (5 * 13) + 5 = 500 + 937.5 - 65 + 5 = 1377.5
        assertThat(result.getOrThrow()).isWithin(0.01).of(1377.5)
    }

    @Test
    fun calculateBMR_whenMaxAge120_thenCalculatesCorrectly() = runTest {
        // Given: Maximum age (120 years)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(120),
            weightKg = 50.0,
            heightCm = 150.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Calculation succeeds
        assertThat(result.isSuccess).isTrue()
        // Formula: (10 * 50) + (6.25 * 150) - (5 * 120) - 161 = 500 + 937.5 - 600 - 161 = 676.5
        assertThat(result.getOrThrow()).isWithin(0.01).of(676.5)
    }

    @Test
    fun calculateBMR_whenMinWeight30kg_thenCalculatesCorrectly() = runTest {
        // Given: Minimum weight (30 kg)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(20),
            weightKg = 30.0,
            heightCm = 150.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Calculation succeeds
        assertThat(result.isSuccess).isTrue()
        // Formula: (10 * 30) + (6.25 * 150) - (5 * 20) - 161 = 300 + 937.5 - 100 - 161 = 976.5
        assertThat(result.getOrThrow()).isWithin(0.01).of(976.5)
    }

    @Test
    fun calculateBMR_whenMaxWeight300kg_thenCalculatesCorrectly() = runTest {
        // Given: Maximum weight (300 kg)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(40),
            weightKg = 300.0,
            heightCm = 200.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Calculation succeeds
        assertThat(result.isSuccess).isTrue()
        // Formula: (10 * 300) + (6.25 * 200) - (5 * 40) + 5 = 3000 + 1250 - 200 + 5 = 4055
        assertThat(result.getOrThrow()).isWithin(0.01).of(4055.0)
    }

    @Test
    fun calculateBMR_whenMinHeight100cm_thenCalculatesCorrectly() = runTest {
        // Given: Minimum height (100 cm)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(25),
            weightKg = 50.0,
            heightCm = 100.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Calculation succeeds
        assertThat(result.isSuccess).isTrue()
        // Formula: (10 * 50) + (6.25 * 100) - (5 * 25) + 5 = 500 + 625 - 125 + 5 = 1005
        assertThat(result.getOrThrow()).isWithin(0.01).of(1005.0)
    }

    @Test
    fun calculateBMR_whenMaxHeight250cm_thenCalculatesCorrectly() = runTest {
        // Given: Maximum height (250 cm)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(30),
            weightKg = 100.0,
            heightCm = 250.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Calculation succeeds
        assertThat(result.isSuccess).isTrue()
        // Formula: (10 * 100) + (6.25 * 250) - (5 * 30) - 161 = 1000 + 1562.5 - 150 - 161 = 2251.5
        assertThat(result.getOrThrow()).isWithin(0.01).of(2251.5)
    }

    // ========================================
    // Invalid Profile Handling (AC #6)
    // ========================================

    @Test
    fun calculateBMR_whenAgeTooYoung_thenReturnsError() = runTest {
        // Given: Age < 13 (12 years old)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(12),
            weightKg = 50.0,
            heightCm = 150.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("Age must be between 13 and 120")
    }

    @Test
    fun calculateBMR_whenAgeTooOld_thenReturnsError() = runTest {
        // Given: Age > 120 (121 years old)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(121),
            weightKg = 50.0,
            heightCm = 150.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("Age must be between 13 and 120")
    }

    @Test
    fun calculateBMR_whenWeightTooLow_thenReturnsError() = runTest {
        // Given: Weight < 30 kg (29.9 kg)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(20),
            weightKg = 29.9,
            heightCm = 150.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("Weight must be between 30 and 300 kg")
    }

    @Test
    fun calculateBMR_whenWeightTooHigh_thenReturnsError() = runTest {
        // Given: Weight > 300 kg (300.1 kg)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(30),
            weightKg = 300.1,
            heightCm = 170.0
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("Weight must be between 30 and 300 kg")
    }

    @Test
    fun calculateBMR_whenHeightTooLow_thenReturnsError() = runTest {
        // Given: Height < 100 cm (99.9 cm)
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(25),
            weightKg = 50.0,
            heightCm = 99.9
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("Height must be between 100 and 250 cm")
    }

    @Test
    fun calculateBMR_whenHeightTooHigh_thenReturnsError() = runTest {
        // Given: Height > 250 cm (250.1 cm)
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = LocalDate.now().minusYears(30),
            weightKg = 70.0,
            heightCm = 250.1
        )

        // When: Calculate BMR
        val result = energyBalanceRepository.calculateBMR(profile)

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("Height must be between 100 and 250 cm")
    }

    // ========================================
    // Reactive Flow - getBMR() (AC #5)
    // ========================================

    @Test
    fun getBMR_whenProfileNull_thenReturnsProfileNotConfiguredError() = runTest {
        // Given: User profile repository returns null (profile not configured)
        whenever(userProfileRepository.getUserProfile()).thenReturn(flowOf(null))

        // When: Observe BMR
        val result = energyBalanceRepository.getBMR().first()

        // Then: Returns failure with ProfileNotConfiguredError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ProfileNotConfiguredError::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("User profile must be configured")
    }

    @Test
    fun getBMR_whenProfileValid_thenCalculatesBMRCorrectly() = runTest {
        // Given: Valid user profile
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(30),
            weightKg = 75.5,
            heightCm = 178.0
        )
        whenever(userProfileRepository.getUserProfile()).thenReturn(flowOf(profile))

        // When: Observe BMR
        val result = energyBalanceRepository.getBMR().first()

        // Then: Returns success with calculated BMR
        assertThat(result.isSuccess).isTrue()
        val bmr = result.getOrThrow()
        assertThat(bmr).isWithin(10.0).of(1722.5)
    }

    @Test
    fun getBMR_whenProfileInvalid_thenReturnsValidationError() = runTest {
        // Given: Invalid profile (age too young)
        val invalidProfile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = LocalDate.now().minusYears(12),
            weightKg = 50.0,
            heightCm = 150.0
        )
        whenever(userProfileRepository.getUserProfile()).thenReturn(flowOf(invalidProfile))

        // When: Observe BMR
        val result = energyBalanceRepository.getBMR().first()

        // Then: Returns failure with ValidationError
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
    }
}
