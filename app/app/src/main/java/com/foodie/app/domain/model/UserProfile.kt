package com.foodie.app.domain.model

import java.time.LocalDate

/**
 * User demographic profile for BMR (Basal Metabolic Rate) calculation.
 *
 * Captures the four required inputs for the Mifflin-St Jeor equation:
 * - Sex: Required for different BMR formulas (males +5, females -161)
 * - Birth Date: Used to calculate age; metabolic rate decreases with age (-5 × age_years)
 * - Weight: Primary factor in energy expenditure (10 × weight_kg)
 * - Height: Secondary factor in energy expenditure (6.25 × height_cm)
 *
 * **Storage Architecture:**
 * - Sex & Birth Date: SharedPreferences (not available in Health Connect)
 * - Weight & Height: Health Connect WeightRecord/HeightRecord (two-way sync)
 *
 * **Validation Ranges:**
 * - Age: 13-120 years (minimum user age, maximum reasonable lifespan)
 * - Weight: 30-300 kg (covers vast majority of adult populations)
 * - Height: 100-250 cm (covers vast majority of adult populations)
 *
 * @property sex Biological sex for BMR calculation (Male/Female)
 * @property birthDate Birth date used to calculate age (resulting age must be 13-120)
 * @property weightKg Weight in kilograms (30-300)
 * @property heightCm Height in centimeters (100-250)
 */
data class UserProfile(
    val sex: Sex,
    val birthDate: LocalDate,
    val weightKg: Double,
    val heightCm: Double
) {
    /**
     * Biological sex for BMR calculation.
     *
     * Note: This reflects the biological sex used in the Mifflin-St Jeor equation,
     * which is based on physiological differences in metabolic rate.
     */
    enum class Sex {
        MALE,
        FEMALE
    }

    /**
     * Calculates current age from birth date.
     *
     * @param referenceDate Optional reference date for age calculation (defaults to today)
     * @return Age in years
     */
    fun calculateAge(referenceDate: LocalDate = LocalDate.now()): Int {
        var age = referenceDate.year - birthDate.year
        if (referenceDate.monthValue < birthDate.monthValue ||
            (referenceDate.monthValue == birthDate.monthValue && referenceDate.dayOfMonth < birthDate.dayOfMonth)
        ) {
            age--
        }
        return age
    }

    /**
     * Validates all profile fields against acceptable ranges.
     *
     * @return Result.success if all fields valid, Result.failure with ValidationError if any field out of range
     */
    fun validate(): Result<Unit> {
        val age = calculateAge()
        return when {
            age !in 13..120 -> Result.failure(
                ValidationError("Age must be between 13 and 120")
            )
            weightKg !in 30.0..300.0 -> Result.failure(
                ValidationError("Weight must be between 30 and 300 kg")
            )
            heightCm !in 100.0..250.0 -> Result.failure(
                ValidationError("Height must be between 100 and 250 cm")
            )
            else -> Result.success(Unit)
        }
    }
}

/**
 * Validation error for user profile fields.
 *
 * @param message Human-readable error message for UI display
 */
class ValidationError(message: String) : Exception(message)
