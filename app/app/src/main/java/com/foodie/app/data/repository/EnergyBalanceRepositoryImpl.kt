package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.repository.EnergyBalanceRepository
import com.foodie.app.domain.repository.UserProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Implementation of EnergyBalanceRepository.
 *
 * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor equation,
 * and Non-Exercise Activity Thermogenesis (NEAT) from daily step count.
 *
 * **BMR Formula (Mifflin-St Jeor):**
 * - Male: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
 * - Female: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161
 *
 * **NEAT Formula:**
 * - Passive Calories = steps × 0.04 kcal/step
 * - Peer-reviewed source: Levine, J. A. (2002). "Non-exercise activity thermogenesis (NEAT)"
 *
 * **Architecture:**
 * - Depends on UserProfileRepository for profile data
 * - Depends on HealthConnectManager for step data
 * - Pure calculation logic (no storage)
 * - Returns Result<Double> for error handling
 *
 * @param userProfileRepository Source of user demographic data
 * @param healthConnectManager Source of Health Connect step data
 */
@Singleton
class EnergyBalanceRepositoryImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val healthConnectManager: HealthConnectManager,
) : EnergyBalanceRepository {

    companion object {
        private const val TAG = "EnergyBalanceRepository"

        /**
         * NEAT calculation constant: calories per step.
         *
         * **Peer-Reviewed Source:**
         * Levine, J. A. (2002). "Non-exercise activity thermogenesis (NEAT)".
         * Best Practice & Research Clinical Endocrinology & Metabolism, 16(4), 679-702.
         *
         * **Scientific Basis:**
         * 0.04 kcal/step is the accepted constant for average adult step energy expenditure.
         * NEAT includes all energy expended for activities that are not sleeping, eating,
         * or sports-like exercise (walking, fidgeting, occupational activities).
         */
        private const val KCAL_PER_STEP = 0.04
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

    /**
     * Calculates NEAT from current day step count.
     *
     * **Formula:**
     * Passive Calories = steps × 0.04 kcal/step
     *
     * **Time Range:**
     * - Queries from midnight (local timezone) to current time
     * - Represents "today's NEAT" in progress
     *
     * **Multi-Record Handling:**
     * - Sums all StepsRecord entries (Garmin, Google Fit write separate records)
     * - HealthConnectManager.querySteps() handles summation
     *
     * **Zero Steps:**
     * - Returns Result.success(0.0) if no step data (valid sedentary day start)
     * - Not an error condition
     *
     * **Permission Errors:**
     * - Catches SecurityException from HealthConnectManager.querySteps()
     * - Returns Result.failure(SecurityException) for caller to handle
     *
     * @return Result.success(neat) in kcal, or Result.failure if permission denied
     */
    override suspend fun calculateNEAT(): Result<Double> {
        return try {
            // Calculate time range: midnight to now (local timezone)
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
            val now = Instant.now()

            // Query total steps (throws SecurityException if permission denied)
            val steps = healthConnectManager.querySteps(startOfDay, now)

            // Calculate NEAT: steps × 0.04 kcal/step
            val neat = steps * KCAL_PER_STEP

            Timber.tag(TAG).d("NEAT calculated: $neat kcal (from $steps steps)")
            Result.success(neat)
        } catch (e: SecurityException) {
            // READ_STEPS permission denied
            Timber.tag(TAG).w("NEAT calculation failed - READ_STEPS permission denied")
            Result.failure(e)
        } catch (e: Exception) {
            // Unexpected error (should not happen - HealthConnectManager handles most errors)
            Timber.tag(TAG).e(e, "Unexpected error calculating NEAT")
            Result.failure(e)
        }
    }

    /**
     * Observes NEAT with reactive updates.
     *
     * **Data Flow:**
     * 1. Collect step count from HealthConnectManager.observeSteps()
     * 2. Calculate NEAT: steps × 0.04 kcal/step
     * 3. Emit Result.success(neat) or Result.failure on permission error
     *
     * **Update Interval:**
     * - Polls Health Connect every 5 minutes
     * - Detects new step data synced from Garmin, Google Fit, etc.
     *
     * **Permission Errors:**
     * - If READ_STEPS permission denied, emits Result.failure(SecurityException)
     * - Caller should prompt for permission or gracefully degrade
     *
     * @return Flow emitting Result<Double> with NEAT in kcal every 5 minutes
     */
    override fun getNEAT(): Flow<Result<Double>> {
        return healthConnectManager.observeSteps().map { steps ->
            try {
                val neat = steps * KCAL_PER_STEP
                Timber.tag(TAG).d("NEAT updated: $neat kcal (from $steps steps)")
                Result.success(neat)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error calculating NEAT from step flow")
                Result.failure(e)
            }
        }
    }

    /**
     * Calculates Active Energy Expenditure from workout calories.
     *
     * **Data Flow:**
     * 1. Calculate time range: midnight to now (local timezone)
     * 2. Query total active calories (throws SecurityException if permission denied)
     * 3. Return active calories as Result.success or Result.failure
     *
     * **Zero Workouts:**
     * - Returns Result.success(0.0) if no workout data (valid rest day)
     * - Not an error condition
     *
     * **Permission Errors:**
     * - Catches SecurityException from HealthConnectManager.queryActiveCalories()
     * - Returns Result.failure(SecurityException) for caller to handle
     *
     * @return Result.success(activeCalories) in kcal, or Result.failure if permission denied
     */
    override suspend fun calculateActiveCalories(): Result<Double> {
        return try {
            // Calculate time range: midnight to now (local timezone)
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
            val now = Instant.now()

            // Query total active calories (throws SecurityException if permission denied)
            val activeCalories = healthConnectManager.queryActiveCalories(startOfDay, now)

            Timber.tag(TAG).d("Active Energy calculated: $activeCalories kcal")
            Result.success(activeCalories)
        } catch (e: SecurityException) {
            // READ_ACTIVE_CALORIES_BURNED permission denied
            Timber.tag(TAG).w("Active Energy calculation failed - READ_ACTIVE_CALORIES_BURNED permission denied")
            Result.failure(e)
        } catch (e: Exception) {
            // Unexpected error (should not happen - HealthConnectManager handles most errors)
            Timber.tag(TAG).e(e, "Unexpected error calculating Active Energy")
            Result.failure(e)
        }
    }

    /**
     * Observes Active Energy Expenditure with reactive updates.
     *
     * **Data Flow:**
     * 1. Collect active calories from HealthConnectManager.observeActiveCalories()
     * 2. Emit Result.success(activeCalories) or Result.failure on permission error
     *
     * **Update Interval:**
     * - Polls Health Connect every 5 minutes
     * - Detects new workout data synced from Garmin Connect (5-15 minute delay)
     *
     * **Permission Errors:**
     * - If READ_ACTIVE_CALORIES_BURNED permission denied, emits Result.failure(SecurityException)
     * - Caller should prompt for permission or gracefully degrade
     *
     * @return Flow emitting Result<Double> with Active Energy in kcal every 5 minutes
     */
    override fun getActiveCalories(): Flow<Result<Double>> {
        return healthConnectManager.observeActiveCalories().map { activeCalories ->
            try {
                Timber.tag(TAG).d("Active Energy updated: $activeCalories kcal")
                Result.success(activeCalories)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error calculating Active Energy from flow")
                Result.failure(e)
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
