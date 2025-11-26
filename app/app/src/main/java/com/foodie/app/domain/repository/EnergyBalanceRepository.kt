package com.foodie.app.domain.repository

import com.foodie.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for energy balance calculations.
 *
 * Provides Basal Metabolic Rate (BMR) calculations using the Mifflin-St Jeor equation,
 * the most accurate predictor of resting metabolic rate for normal-weight and obese individuals.
 *
 * **BMR Formula (Mifflin-St Jeor):**
 * - Males: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
 * - Females: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161
 *
 * **Data Flow:**
 * - BMR calculation depends on complete UserProfile (sex, age, weight, height)
 * - Reactive flow updates when underlying profile changes
 *
 * @see UserProfile for required profile fields and validation
 */
interface EnergyBalanceRepository {
    /**
     * Calculates Basal Metabolic Rate (BMR) for a given user profile.
     *
     * Uses the Mifflin-St Jeor equation to calculate daily resting energy expenditure.
     * This represents the calories burned at complete rest to maintain vital functions.
     *
     * **Formula:**
     * - Male: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
     * - Female: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161
     *
     * @param profile Complete user profile with sex, age, weight, height
     * @return Result.success(bmr) with BMR in kcal/day, or Result.failure if profile invalid
     */
    suspend fun calculateBMR(profile: UserProfile): Result<Double>

    /**
     * Observes BMR with reactive updates when user profile changes.
     *
     * Automatically recalculates BMR when:
     * - Age changes (birthday passed)
     * - Weight changes (user updates weight)
     * - Height changes (user updates height)
     * - Sex changes (rare but possible profile correction)
     *
     * **Use Cases:**
     * - Dashboard displaying current BMR
     * - TDEE calculation (BMR is a component)
     * - Real-time energy balance tracking
     *
     * @return Flow emitting Result.success(bmr) when profile valid, Result.failure when profile missing/invalid
     */
    fun getBMR(): Flow<Result<Double>>
}
