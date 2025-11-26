package com.foodie.app.domain.repository

import com.foodie.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for energy balance calculations.
 *
 * Provides Basal Metabolic Rate (BMR) calculations using the Mifflin-St Jeor equation,
 * and Non-Exercise Activity Thermogenesis (NEAT) calculations from daily step count.
 *
 * **BMR Formula (Mifflin-St Jeor):**
 * - Males: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
 * - Females: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161
 *
 * **NEAT Formula:**
 * - Passive Calories = steps × 0.04 kcal/step (peer-reviewed constant)
 * - Based on Levine, J. A. (2002) "Non-exercise activity thermogenesis (NEAT)"
 *
 * **Data Flow:**
 * - BMR calculation depends on complete UserProfile (sex, age, weight, height)
 * - NEAT calculation depends on Health Connect step data for current day
 * - Reactive flows update when underlying data changes
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

    /**
     * Calculates Non-Exercise Activity Thermogenesis (NEAT) from current day step count.
     *
     * Uses the peer-reviewed formula: Passive Calories = steps × 0.04 kcal/step
     * (Levine, J. A. 2002. "Non-exercise activity thermogenesis (NEAT)")
     *
     * **Data Source:**
     * - Queries Health Connect StepsRecord from midnight to current time (local timezone)
     * - Sums all step records (Garmin, Google Fit may write separate entries)
     *
     * **Zero Steps Handling:**
     * - Returns Result.success(0.0) if no step data exists (valid sedentary day start)
     * - Not an error - user may not have walked yet today
     *
     * **Permission Handling:**
     * - Returns Result.failure(SecurityException) if READ_STEPS permission denied
     * - Caller should prompt user for permission or gracefully degrade
     *
     * @return Result.success(neat) with NEAT in kcal, or Result.failure if permission denied
     */
    suspend fun calculateNEAT(): Result<Double>

    /**
     * Observes NEAT with reactive updates when step data changes.
     *
     * Polls Health Connect every 5 minutes to detect new step data synced from:
     * - Garmin Connect
     * - Google Fit
     * - Samsung Health
     * - Other step tracking apps
     *
     * **Polling Interval:**
     * - 5 minutes (Health Connect does not support real-time change listeners)
     * - Balances battery life with near-real-time updates
     *
     * **Use Cases:**
     * - Dashboard displaying current NEAT
     * - TDEE calculation (NEAT is a component: TDEE = BMR + NEAT + Active)
     * - Real-time energy balance tracking
     *
     * @return Flow emitting Result.success(neat) with NEAT in kcal, or Result.failure if permission denied
     */
    fun getNEAT(): Flow<Result<Double>>
}
