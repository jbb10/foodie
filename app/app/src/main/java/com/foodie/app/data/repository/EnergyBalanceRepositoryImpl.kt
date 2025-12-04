package com.foodie.app.data.repository

import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.domain.model.EnergyBalance
import com.foodie.app.domain.model.UserProfile
import com.foodie.app.domain.repository.EnergyBalanceRepository
import com.foodie.app.domain.repository.UserProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
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
     * Calculates NEAT from current day step count, excluding steps during active exercise.
     *
     * **Formula:**
     * Passive Calories = steps × 0.04 kcal/step
     *
     * **Exercise Step Exclusion:**
     * Steps taken during active exercise periods (from ActiveCaloriesBurnedRecord) are
     * excluded from the NEAT calculation. This prevents double-counting since active
     * exercise calories already account for steps taken during the workout.
     *
     * **Scientific Rationale:**
     * - NEAT = Non-Exercise Activity Thermogenesis (passive movement only)
     * - Active calories from Garmin already include energy from steps during exercise
     * - Including exercise steps in both NEAT and Active would overestimate TDEE
     *
     * **Implementation:**
     * 1. Query ActiveCaloriesBurnedRecord to get exercise time ranges
     * 2. Query StepsRecord excluding steps that overlap with exercise periods
     * 3. Calculate NEAT using only non-exercise steps
     *
     * **Time Range:**
     * - Queries from midnight (local timezone) to current time
     * - Represents "today's NEAT" in progress
     *
     * **Multi-Record Handling:**
     * - Sums all StepsRecord entries (Garmin, Google Fit write separate records)
     * - Filters out StepsRecord that overlap with ActiveCaloriesBurnedRecord time ranges
     * - HealthConnectManager.querySteps() handles summation and filtering
     *
     * **Zero Steps:**
     * - Returns Result.success(0.0) if no step data (valid sedentary day start)
     * - Not an error condition
     *
     * **Permission Errors:**
     * - Catches SecurityException from HealthConnectManager.querySteps()
     * - Returns Result.failure(SecurityException) for caller to handle
     * - Active calories permission errors are gracefully handled (assumes no exercise)
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

            // Query active exercise records to get time ranges for exclusion
            val exerciseTimeRanges = try {
                val activeRecords = healthConnectManager.queryActiveCaloriesRecords(startOfDay, now)
                activeRecords.map { record -> record.startTime to record.endTime }
            } catch (e: SecurityException) {
                // READ_ACTIVE_CALORIES_BURNED permission denied - proceed without exclusion
                Timber.tag(TAG).w("Cannot exclude exercise steps - READ_ACTIVE_CALORIES_BURNED permission denied")
                emptyList()
            }

            // Query total steps, excluding steps during exercise periods
            val steps = healthConnectManager.querySteps(startOfDay, now, exerciseTimeRanges)

            // Calculate NEAT: steps × 0.04 kcal/step
            val neat = steps * KCAL_PER_STEP

            Timber.tag(TAG).d("NEAT calculated: $neat kcal (from $steps non-exercise steps)")
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

    /**
     * Calculates TDEE using Flow combine() operator.
     *
     * **Reactive Strategy:**
     * - Combines getBMR(), getNEAT(), getActiveCalories() using combine()
     * - Emits new TDEE whenever ANY component Flow emits
     * - Waits for all three Flows to emit at least once before first emission
     *
     * **Error Handling:**
     * - Unwraps Result<Double> from each component Flow
     * - Defaults to 0.0 for failed Results (permission denied, profile not configured)
     * - Never fails - always returns Double (graceful degradation)
     *
     * **Formula:**
     * TDEE = BMR + NEAT + Active
     *
     * @return Flow emitting TDEE in kcal/day (sum of available components)
     */
    override fun getTDEE(): Flow<Double> {
        return combine(
            getBMR(),
            getNEAT(),
            getActiveCalories(),
        ) { bmrResult, neatResult, activeResult ->
            // Unwrap Results, defaulting to 0.0 for errors (graceful degradation)
            val bmr = bmrResult.getOrElse { error ->
                Timber.tag(TAG).w("TDEE calculation - BMR unavailable: ${error.message}")
                0.0
            }
            val neat = neatResult.getOrElse { error ->
                Timber.tag(TAG).w("TDEE calculation - NEAT unavailable: ${error.message}")
                0.0
            }
            val active = activeResult.getOrElse { error ->
                Timber.tag(TAG).w("TDEE calculation - Active unavailable: ${error.message}")
                0.0
            }

            // Calculate TDEE: BMR + NEAT + Active
            val tdee = bmr + neat + active
            Timber.tag(TAG).d("TDEE calculated: $tdee kcal/day (BMR: $bmr, NEAT: $neat, Active: $active)")
            tdee
        }
    }

    /**
     * Calculates Calories In from today's nutrition records.
     *
     * **Data Flow:**
     * 1. Calculate time range: midnight to now (local timezone)
     * 2. Query all nutrition records (throws SecurityException if permission denied)
     * 3. Sum energy.inKilocalories from all records
     * 4. Return total as Result.success or Result.failure
     *
     * **Zero Meals:**
     * - Returns Result.success(0.0) if no meal data (valid fasting day)
     * - Not an error condition
     *
     * **Permission Errors:**
     * - Catches SecurityException from HealthConnectManager.queryNutritionRecords()
     * - Returns Result.failure(SecurityException) for caller to handle
     *
     * @return Result.success(caloriesIn) in kcal, or Result.failure if permission denied
     */
    override suspend fun calculateCaloriesIn(): Result<Double> {
        return try {
            // Calculate time range: midnight to now (local timezone)
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
            val now = Instant.now()

            // Query all nutrition records (throws SecurityException if permission denied)
            val nutritionRecords = healthConnectManager.queryNutritionRecords(startOfDay, now)

            // Sum total calories from all records
            val caloriesIn = nutritionRecords.sumOf { record ->
                record.energy?.inKilocalories ?: 0.0
            }

            Timber.tag(TAG).d("Calories In calculated: $caloriesIn kcal (from ${nutritionRecords.size} meals)")
            Result.success(caloriesIn)
        } catch (e: SecurityException) {
            // READ_NUTRITION permission denied
            Timber.tag(TAG).w("Calories In calculation failed - READ_NUTRITION permission denied")
            Result.failure(e)
        } catch (e: Exception) {
            // Unexpected error (should not happen - HealthConnectManager handles most errors)
            Timber.tag(TAG).e(e, "Unexpected error calculating Calories In")
            Result.failure(e)
        }
    }

    /**
     * Calculates daily macros totals from nutrition records.
     *
     * **Data Source:**
     * - Queries Health Connect NutritionRecords for today (midnight to now)
     * - Sums protein, carbs, fat from Mass fields (protein, totalCarbohydrate, totalFat)
     *
     * **Backward Compatibility:**
     * - Legacy records without macros fields default to 0g (treated as null?.inGrams ?: 0.0)
     *
     * **Epic 7 Feature:**
     * - Enables macros tracking dashboard UI
     * - Returns Triple(protein, carbs, fat) in grams
     *
     * @return Result.success(Triple(protein, carbs, fat)) in grams, or Result.failure on permission error
     */
    private suspend fun calculateMacros(): Result<Triple<Double, Double, Double>> {
        return try {
            // Calculate time range: midnight to now (local timezone)
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
            val now = Instant.now()

            // Query all nutrition records
            val nutritionRecords = healthConnectManager.queryNutritionRecords(startOfDay, now)

            // Sum macros (protein, carbs, fat) from Mass fields
            var totalProtein = 0.0
            var totalCarbs = 0.0
            var totalFat = 0.0

            nutritionRecords.forEach { record ->
                totalProtein += record.protein?.inGrams ?: 0.0
                totalCarbs += record.totalCarbohydrate?.inGrams ?: 0.0
                totalFat += record.totalFat?.inGrams ?: 0.0
            }

            Timber.tag(TAG).d("Macros calculated: Protein=${totalProtein}g, Carbs=${totalCarbs}g, Fat=${totalFat}g (from ${nutritionRecords.size} meals)")
            Result.success(Triple(totalProtein, totalCarbs, totalFat))
        } catch (e: SecurityException) {
            Timber.tag(TAG).w("Macros calculation failed - READ_NUTRITION permission denied")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unexpected error calculating macros")
            Result.failure(e)
        }
    }

    /**
     * Observes Calories In with reactive updates.
     *
     * **Data Flow:**
     * 1. Poll Health Connect every 5 minutes
     * 2. Calculate Calories In from nutrition records
     * 3. Emit Result.success(caloriesIn) or Result.failure on permission error
     *
     * **Update Interval:**
     * - 5 minutes (Health Connect does not support real-time change listeners)
     * - Detects new meals added via Foodie app, MyFitnessPal, etc.
     *
     * **Permission Errors:**
     * - If READ_NUTRITION permission denied, emits Result.failure(SecurityException)
     * - Caller should prompt for permission or gracefully degrade
     *
     * @return Flow emitting Result<Double> with Calories In every 5 minutes
     */
    override fun getCaloriesIn(): Flow<Result<Double>> {
        return flow {
            while (true) {
                emit(calculateCaloriesIn())
                delay(5.minutes)
            }
        }
    }

    /**
     * Observes complete energy balance with reactive updates.
     *
     * **Reactive Strategy:**
     * - Combines getBMR(), getNEAT(), getActiveCalories(), getCaloriesIn() using combine()
     * - Emits new EnergyBalance whenever ANY component Flow emits
     * - Waits for all four Flows to emit at least once before first emission
     *
     * **Date Support:**
     * - For historical dates: Queries Health Connect data for that specific day (midnight to midnight)
     * - For historical dates: Uses queryWeightForDate() for accurate BMR, falls back to current profile weight
     * - For today: Polls Health Connect every 5 minutes for real-time updates
     *
     * **Error Handling:**
     * - If BMR unavailable (profile not configured), returns Result.failure
     * - If NEAT/Active/CaloriesIn unavailable (permissions denied), defaults to 0.0 for those components
     * - Graceful degradation: partial data acceptable (e.g., BMR + NEAT only)
     *
     * **Domain Model Construction:**
     * - bmr: From getBMR() Result (using historical weight if date != today)
     * - neat: From getNEAT() Result (default 0.0 if error)
     * - activeCalories: From getActiveCalories() Result (default 0.0 if error)
     * - caloriesIn: From getCaloriesIn() Result (default 0.0 if error)
     * - tdee: Computed as bmr + neat + activeCalories
     * - deficitSurplus: Computed as tdee - caloriesIn
     *
     * @param date The date to query energy balance for (defaults to today)
     * @return Flow emitting Result<EnergyBalance> with complete energy data
     */
    override fun getEnergyBalance(date: LocalDate): Flow<Result<EnergyBalance>> {
        // For historical dates, use one-shot calculation (no polling)
        // For today, use polling Flows for real-time updates
        return if (date < LocalDate.now()) {
            // Historical date - single emission
            flow {
                emit(calculateEnergyBalanceForDate(date))
            }
        } else {
            // Today - reactive polling
            flow {
                while (true) {
                    val result = calculateCurrentEnergyBalance()
                    emit(result)
                    delay(5.minutes)
                }
            }
        }
    }

    /**
     * Calculates energy balance for the current moment.
     * Helper for reactive polling flow to avoid inline lambda continue issues.
     */
    private suspend fun calculateCurrentEnergyBalance(): Result<EnergyBalance> {
        // Collect latest values from each component
        val bmrResult = getBMR().firstOrNull()
        val neatResult = getNEAT().firstOrNull()
        val activeResult = getActiveCalories().firstOrNull()
        val caloriesInResult = calculateCaloriesIn()
        val macrosResult = calculateMacros()

        // Check if BMR is available (required)
        if (bmrResult == null) {
            return Result.failure(ProfileNotConfiguredError("User profile must be configured to calculate BMR"))
        }

        val bmr = bmrResult.getOrElse { error ->
            Timber.tag(TAG).w("EnergyBalance unavailable - BMR error: ${error.message}")
            return Result.failure(error)
        }

        // Unwrap other Results, defaulting to 0.0 for errors (graceful degradation)
        val neat = neatResult?.getOrElse { error ->
            Timber.tag(TAG).w("EnergyBalance - NEAT unavailable, defaulting to 0.0: ${error.message}")
            0.0
        } ?: 0.0

        val active = activeResult?.getOrElse { error ->
            Timber.tag(TAG).w("EnergyBalance - Active unavailable, defaulting to 0.0: ${error.message}")
            0.0
        } ?: 0.0

        val caloriesIn = caloriesInResult.getOrElse { error ->
            Timber.tag(TAG).w("EnergyBalance - CaloriesIn unavailable, defaulting to 0.0: ${error.message}")
            0.0
        }

        // Get macros (default to 0.0 if error)
        val (protein, carbs, fat) = macrosResult.getOrElse { error ->
            Timber.tag(TAG).w("EnergyBalance - Macros unavailable, defaulting to 0.0: ${error.message}")
            Triple(0.0, 0.0, 0.0)
        }

        // Calculate derived fields
        val tdee = bmr + neat + active
        val deficitSurplus = tdee - caloriesIn

        // Construct EnergyBalance domain model
        val energyBalance = EnergyBalance(
            bmr = bmr,
            neat = neat,
            activeCalories = active,
            tdee = tdee,
            caloriesIn = caloriesIn,
            deficitSurplus = deficitSurplus,
            totalProtein = protein,
            totalCarbs = carbs,
            totalFat = fat,
        )

        Timber.tag(TAG).d(
            "EnergyBalance calculated: TDEE=$tdee kcal (BMR=$bmr + NEAT=$neat + Active=$active), " +
                "CaloriesIn=$caloriesIn, Deficit/Surplus=$deficitSurplus, Macros: P=${protein}g C=${carbs}g F=${fat}g",
        )

        return Result.success(energyBalance)
    }

    /**
     * Calculates energy balance for a specific historical date.
     *
     * **One-Shot Calculation:**
     * - Queries Health Connect once for that day's data (no polling)
     * - Uses TimeRangeFilter.between(startOfDay, endOfDay)
     * - Queries historical weight for accurate BMR
     *
     * **Historical Weight:**
     * - Calls healthConnectManager.queryWeightForDate(date)
     * - Falls back to current user profile weight if no historical weight found
     *
     * @param date The historical date to calculate energy balance for
     * @return Result<EnergyBalance> for that date
     */
    private suspend fun calculateEnergyBalanceForDate(date: LocalDate): Result<EnergyBalance> {
        return try {
            // Get user profile for BMR calculation - get first emission from Flow
            val profile = userProfileRepository.getUserProfile().firstOrNull()
                ?: return Result.failure(ProfileNotConfiguredError("User profile must be configured to calculate BMR"))

            // Query historical weight for more accurate BMR
            val historicalWeight = healthConnectManager.queryWeightForDate(date)
            val weightKg = historicalWeight?.weight?.inKilograms ?: profile.weightKg

            // Create profile with historical weight for BMR calculation
            val profileWithHistoricalWeight = profile.copy(weightKg = weightKg)

            // Calculate BMR with historical weight
            val bmrResult = calculateBMR(profileWithHistoricalWeight)
            val bmr = bmrResult.getOrElse { error ->
                Timber.tag(TAG).w("BMR calculation failed: ${error.message}")
                return Result.failure(error)
            }

            // Calculate time range for this date
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            // Query historical active exercise records to get time ranges for step exclusion
            val exerciseTimeRanges = try {
                val activeRecords = healthConnectManager.queryActiveCaloriesRecords(startOfDay, endOfDay)
                activeRecords.map { record -> record.startTime to record.endTime }
            } catch (e: SecurityException) {
                Timber.tag(TAG).w("Cannot exclude exercise steps - READ_ACTIVE_CALORIES_BURNED permission denied")
                emptyList()
            }

            // Query historical step count for NEAT, excluding exercise steps
            val steps = try {
                healthConnectManager.querySteps(startOfDay, endOfDay, exerciseTimeRanges)
            } catch (e: SecurityException) {
                Timber.tag(TAG).w("NEAT unavailable - READ_STEPS permission denied")
                0 // Default to 0 steps if permission denied
            }
            val neat = steps.toDouble() * KCAL_PER_STEP

            // Query historical active calories
            val active = try {
                healthConnectManager.queryActiveCalories(startOfDay, endOfDay)
            } catch (e: SecurityException) {
                Timber.tag(TAG).w("Active unavailable - READ_ACTIVE_CALORIES_BURNED permission denied")
                0.0 // Default to 0 if permission denied
            }

            // Query historical nutrition records for Calories In and macros
            var caloriesIn = 0.0
            var totalProtein = 0.0
            var totalCarbs = 0.0
            var totalFat = 0.0

            try {
                val nutritionRecords = healthConnectManager.queryNutritionRecords(startOfDay, endOfDay)
                caloriesIn = nutritionRecords.sumOf { record ->
                    record.energy?.inKilocalories ?: 0.0
                }
                // Sum macros
                nutritionRecords.forEach { record ->
                    totalProtein += record.protein?.inGrams ?: 0.0
                    totalCarbs += record.totalCarbohydrate?.inGrams ?: 0.0
                    totalFat += record.totalFat?.inGrams ?: 0.0
                }
            } catch (e: SecurityException) {
                Timber.tag(TAG).w("CaloriesIn/Macros unavailable - READ_NUTRITION permission denied")
                // All defaults to 0.0 already set above
            }

            // Calculate derived fields
            val tdee = bmr + neat + active
            val deficitSurplus = tdee - caloriesIn

            // Construct EnergyBalance domain model
            val energyBalance = EnergyBalance(
                bmr = bmr,
                neat = neat,
                activeCalories = active,
                tdee = tdee,
                caloriesIn = caloriesIn,
                deficitSurplus = deficitSurplus,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
            )

            Timber.tag(TAG).d(
                "Historical EnergyBalance for $date: TDEE=$tdee kcal (BMR=$bmr + NEAT=$neat + Active=$active), " +
                    "CaloriesIn=$caloriesIn, Deficit/Surplus=$deficitSurplus, Macros: P=${totalProtein}g C=${totalCarbs}g F=${totalFat}g",
            )

            return Result.success(energyBalance)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to calculate historical energy balance for $date")
            return Result.failure(e)
        }
    }
}

/**
 * Error indicating user profile is not configured.
 *
 * @param message Human-readable error message for UI display
 */
class ProfileNotConfiguredError(message: String) : Exception(message)
