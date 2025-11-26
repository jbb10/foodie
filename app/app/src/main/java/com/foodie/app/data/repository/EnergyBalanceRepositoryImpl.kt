package com.foodie.app.data.repository

import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.repository.EnergyBalanceRepository
import com.foodie.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EnergyBalanceRepository.
 *
 * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor equation,
 * the gold standard for estimating resting energy expenditure.
 *
 * **Formula:**
 * - Male: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
 * - Female: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161
 *
 * **Architecture:**
 * - Depends on UserProfileRepository for profile data
 * - Pure calculation logic (no storage)
 * - Returns Result<Double> for error handling
 *
 * @param userProfileRepository Source of user demographic data
 */
@Singleton
class EnergyBalanceRepositoryImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : EnergyBalanceRepository {

    companion object {
        private const val TAG = "EnergyBalanceRepository"
    }

    /**
     * Calculates BMR using Mifflin-St Jeor equation.
     *
     * **Formula Breakdown:**
     * - Base: (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years)
     * - Sex adjustment: +5 for males, -161 for females
     *
     * **Validation:**
     * - Age must be 13-120 years
     * - Weight must be 30-300 kg
     * - Height must be 100-250 cm
     * - Validation handled by UserProfile.validate()
     *
     * @param profile Complete user profile
     * @return Result.success(bmr) in kcal/day, or Result.failure with validation error
     */
    override suspend fun calculateBMR(profile: UserProfile): Result<Double> {
        return try {
            // Validate profile
            profile.validate().getOrElse { error ->
                Timber.tag(TAG).w("BMR calculation failed - invalid profile: ${error.message}")
                return Result.failure(error)
            }

            // Calculate age
            val age = profile.calculateAge()

            // Mifflin-St Jeor equation
            val sexAdjustment = if (profile.sex == UserProfile.Sex.MALE) 5 else -161
            val bmr = (10 * profile.weightKg) + (6.25 * profile.heightCm) - (5 * age) + sexAdjustment

            Timber.tag(TAG).d("BMR calculated: $bmr kcal/day (sex=${profile.sex}, age=$age, weight=${profile.weightKg}kg, height=${profile.heightCm}cm)")

            Result.success(bmr)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unexpected error calculating BMR")
            Result.failure(e)
        }
    }

    /**
     * Observes BMR with reactive updates.
     *
     * **Data Flow:**
     * 1. Collect user profile from UserProfileRepository
     * 2. If profile null (not configured), emit Result.failure
     * 3. If profile valid, calculate BMR and emit Result.success
     *
     * **Update Triggers:**
     * - Weight change (Health Connect sync or manual update)
     * - Height change (Health Connect sync or manual update)
     * - Age change (birthday passed - if using reactive LocalDate.now())
     * - Sex change (profile correction)
     *
     * **Error Cases:**
     * - Profile not configured: ProfileNotConfiguredError
     * - Profile validation failed: ValidationError
     *
     * @return Flow emitting Result<Double> with BMR in kcal/day
     */
    override fun getBMR(): Flow<Result<Double>> {
        return userProfileRepository.getUserProfile().map { profile ->
            if (profile == null) {
                Timber.tag(TAG).d("BMR unavailable - user profile not configured")
                Result.failure(ProfileNotConfiguredError("User profile must be configured to calculate BMR"))
            } else {
                calculateBMR(profile)
            }
        }
    }
}

/**
 * Error indicating user profile is not configured.
 *
 * @param message Human-readable error message for UI display
 */
class ProfileNotConfiguredError(message: String) : Exception(message)
