package com.foodie.app.domain.repository

import com.foodie.app.domain.model.EnergyBalance
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

    /**
     * Calculates Active Energy Expenditure from workout calories burned.
     *
     * Queries Health Connect ActiveCaloriesBurnedRecord for current day (midnight to now),
     * sums all workout records from Garmin Connect, and returns total active calories.
     *
     * **Data Source:**
     * - Queries Health Connect ActiveCaloriesBurnedRecord from midnight to current time (local timezone)
     * - Sums all active calorie records (Garmin writes one record per workout session)
     *
     * **Zero Workouts Handling:**
     * - Returns Result.success(0.0) if no workout data exists (valid rest day)
     * - Not an error - user may not have exercised yet today
     *
     * **Permission Handling:**
     * - Returns Result.failure(SecurityException) if READ_ACTIVE_CALORIES_BURNED permission denied
     * - Caller should prompt user for permission or gracefully degrade
     *
     * @return Result.success(activeCalories) with Active Energy in kcal, or Result.failure if permission denied
     */
    suspend fun calculateActiveCalories(): Result<Double>

    /**
     * Observes Active Energy Expenditure with reactive updates when workout data changes.
     *
     * Polls Health Connect every 5 minutes to detect new workout data synced from:
     * - Garmin Connect (typical 5-15 minute sync delay after workout)
     * - Google Fit
     * - Samsung Health
     * - Other fitness tracking apps
     *
     * **Polling Interval:**
     * - 5 minutes (Health Connect does not support real-time change listeners)
     * - Accounts for Garmin sync delays (workouts appear 5-15 minutes after completion)
     *
     * **Use Cases:**
     * - Dashboard displaying current Active Energy
     * - TDEE calculation (Active is a component: TDEE = BMR + NEAT + Active)
     * - Real-time energy balance tracking
     *
     * @return Flow emitting Result.success(activeCalories) with Active Energy in kcal, or Result.failure if permission denied
     */
    fun getActiveCalories(): Flow<Result<Double>>

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) as sum of BMR, NEAT, and Active.
     *
     * Uses Kotlin Flow combine() operator to merge three energy components into single
     * reactive stream that automatically updates when ANY component changes.
     *
     * **Formula:**
     * TDEE = BMR + NEAT + Active
     *
     * **Component Sources:**
     * - BMR: getBMR() Flow (updates on profile changes)
     * - NEAT: getNEAT() Flow (updates every 5 minutes from step count)
     * - Active: getActiveCalories() Flow (updates every 5 minutes from workout data)
     *
     * **Update Triggers:**
     * - Profile changes (weight, age, height) → BMR updates → TDEE recalculates
     * - New step data synced → NEAT updates → TDEE recalculates
     * - New workout data synced → Active updates → TDEE recalculates
     *
     * **Error Handling:**
     * - If BMR unavailable (profile not configured), defaults to 0.0
     * - If NEAT unavailable (permission denied), defaults to 0.0
     * - If Active unavailable (permission denied), defaults to 0.0
     * - Always returns Double (never fails - graceful degradation)
     *
     * **Performance:**
     * - combine() overhead: < 1ms
     * - Arithmetic calculation: < 1μs
     * - Total latency: < 10ms typical
     *
     * @return Flow emitting TDEE in kcal/day (sum of available components, 0.0 for unavailable)
     */
    fun getTDEE(): Flow<Double>

    /**
     * Calculates today's Calories In from Health Connect nutrition records.
     *
     * Queries all NutritionRecord entries from midnight to current time (local timezone),
     * sums total energy consumed, and returns total calories.
     *
     * **Data Source:**
     * - Queries Health Connect NutritionRecord (from midnight to now)
     * - Sums energy.inKilocalories from all meal records
     *
     * **Zero Meals Handling:**
     * - Returns Result.success(0.0) if no meal data exists (valid fasting day)
     * - Not an error condition
     *
     * **Permission Handling:**
     * - Returns Result.failure(SecurityException) if READ_NUTRITION permission denied
     * - Caller should prompt user for permission or gracefully degrade
     *
     * @return Result.success(caloriesIn) in kcal, or Result.failure if permission denied
     */
    suspend fun calculateCaloriesIn(): Result<Double>

    /**
     * Observes Calories In with reactive updates when meal data changes.
     *
     * Polls Health Connect every 5 minutes to detect new meal entries added via:
     * - Foodie app (meal capture flow)
     * - MyFitnessPal (cross-app sync)
     * - Other nutrition tracking apps
     *
     * **Polling Interval:**
     * - 5 minutes (Health Connect does not support real-time change listeners)
     * - Balances battery life with near-real-time updates
     *
     * **Use Cases:**
     * - Dashboard displaying current Calories In
     * - Energy balance calculation (Deficit = TDEE - CaloriesIn)
     * - Real-time deficit/surplus tracking
     *
     * @return Flow emitting Result.success(caloriesIn) in kcal every 5 minutes, or Result.failure if permission denied
     */
    fun getCaloriesIn(): Flow<Result<Double>>

    /**
     * Observes complete energy balance with reactive updates.
     *
     * Combines all energy components (BMR, NEAT, Active, CaloriesIn) into single
     * EnergyBalance domain model using Kotlin Flow combine() operator.
     *
     * **Date Support:**
     * - date parameter defaults to LocalDate.now() for current day
     * - Historical dates query Health Connect data for that specific day (midnight to midnight)
     * - Historical weight tracking: Uses queryWeightForDate() for accurate BMR, falls back to current profile weight
     *
     * **Calculated Fields:**
     * - bmr: From getBMR() Flow (using historical weight if available)
     * - neat: From getNEAT() Flow (historical step count for that date)
     * - activeCalories: From getActiveCalories() Flow (historical active calories for that date)
     * - caloriesIn: From getCaloriesIn() Flow (historical meal data for that date)
     * - tdee: Computed as bmr + neat + activeCalories
     * - deficitSurplus: Computed as tdee - caloriesIn
     *
     * **Update Triggers (for current date):**
     * - Profile changes → BMR updates → EnergyBalance emits
     * - Step data synced → NEAT updates → EnergyBalance emits
     * - Workout synced → Active updates → EnergyBalance emits
     * - Meal added/edited/deleted → CaloriesIn updates → EnergyBalance emits
     *
     * **Error Handling:**
     * - If BMR unavailable (profile not configured), returns Result.failure
     * - If NEAT/Active/CaloriesIn unavailable (permissions denied), defaults to 0.0 for those components
     * - Graceful degradation: partial data acceptable (e.g., BMR + NEAT only if Active denied)
     *
     * **Performance:**
     * - combine() overhead: < 1ms for 4 Flows
     * - Domain model construction: < 1μs
     * - Historical queries complete in < 500ms
     * - Total latency: < 10ms typical for current date, < 500ms for historical
     *
     * @param date The date to query energy balance for (defaults to today)
     * @return Flow emitting Result<EnergyBalance> with complete energy data for the specified date
     */
    fun getEnergyBalance(date: java.time.LocalDate = java.time.LocalDate.now()): Flow<Result<EnergyBalance>>
}
