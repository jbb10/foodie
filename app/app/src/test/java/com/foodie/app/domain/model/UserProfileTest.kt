package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for UserProfile domain model validation.
 *
 * Tests validation ranges for age (13-120), weight (30-300 kg), height (100-250 cm).
 * Uses Truth assertions for readable test failures.
 */
class UserProfileTest {

    /**
     * Helper function to create a birth date for a specific age.
     * Creates a date such that the person is exactly that age today.
     */
    private fun birthDateForAge(age: Int): LocalDate {
        return LocalDate.now().minusYears(age.toLong())
    }

    @Test
    fun `validate should return success when all fields are valid`() {
        // Given: Valid profile with typical adult values
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(30),
            weightKg = 75.5,
            heightCm = 178.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validate should fail when age is less than 13`() {
        // Given: Profile with age below minimum
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDateForAge(12),
            weightKg = 50.0,
            heightCm = 150.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation fails with specific age error
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(ValidationError::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Age must be between 13 and 120")
    }

    @Test
    fun `validate should fail when age is greater than 120`() {
        // Given: Profile with age above maximum
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(121),
            weightKg = 80.0,
            heightCm = 175.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation fails with specific age error
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Age must be between 13 and 120")
    }

    @Test
    fun `validate should succeed when age is exactly 13`() {
        // Given: Profile with age at minimum boundary
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDateForAge(13),
            weightKg = 45.0,
            heightCm = 155.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validate should succeed when age is exactly 120`() {
        // Given: Profile with age at maximum boundary
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(120),
            weightKg = 70.0,
            heightCm = 165.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validate should fail when weight is less than 30 kg`() {
        // Given: Profile with weight below minimum
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDateForAge(25),
            weightKg = 29.9,
            heightCm = 160.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation fails with specific weight error
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Weight must be between 30 and 300 kg")
    }

    @Test
    fun `validate should fail when weight is greater than 300 kg`() {
        // Given: Profile with weight above maximum
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(40),
            weightKg = 300.1,
            heightCm = 180.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation fails with specific weight error
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Weight must be between 30 and 300 kg")
    }

    @Test
    fun `validate should succeed when weight is exactly 30 kg`() {
        // Given: Profile with weight at minimum boundary
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDateForAge(18),
            weightKg = 30.0,
            heightCm = 150.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validate should succeed when weight is exactly 300 kg`() {
        // Given: Profile with weight at maximum boundary
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(35),
            weightKg = 300.0,
            heightCm = 200.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validate should fail when height is less than 100 cm`() {
        // Given: Profile with height below minimum
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDateForAge(20),
            weightKg = 50.0,
            heightCm = 99.9
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation fails with specific height error
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Height must be between 100 and 250 cm")
    }

    @Test
    fun `validate should fail when height is greater than 250 cm`() {
        // Given: Profile with height above maximum
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(28),
            weightKg = 90.0,
            heightCm = 250.1
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation fails with specific height error
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Height must be between 100 and 250 cm")
    }

    @Test
    fun `validate should succeed when height is exactly 100 cm`() {
        // Given: Profile with height at minimum boundary
        val profile = UserProfile(
            sex = UserProfile.Sex.FEMALE,
            birthDate = birthDateForAge(15),
            weightKg = 40.0,
            heightCm = 100.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `validate should succeed when height is exactly 250 cm`() {
        // Given: Profile with height at maximum boundary
        val profile = UserProfile(
            sex = UserProfile.Sex.MALE,
            birthDate = birthDateForAge(25),
            weightKg = 120.0,
            heightCm = 250.0
        )

        // When: Validating profile
        val result = profile.validate()

        // Then: Validation succeeds
        assertThat(result.isSuccess).isTrue()
    }
}
