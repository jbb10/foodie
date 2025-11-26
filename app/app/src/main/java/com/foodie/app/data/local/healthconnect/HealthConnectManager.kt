package com.foodie.app.data.local.healthconnect

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Health Connect availability status.
 */
enum class HealthConnectStatus {
    /** Health Connect is installed and available */
    AVAILABLE,

    /** Health Connect is not installed on the device */
    NOT_INSTALLED,

    /** Health Connect needs to be updated to work properly */
    UPDATE_REQUIRED,
}

/**
 * Manager class for Health Connect SDK operations.
 *
 * Wraps all Health Connect SDK calls to provide a testable abstraction layer.
 * This singleton encapsulates SDK initialization, permissions, and data operations,
 * allowing easy mocking for unit tests.
 *
 * Architecture: HealthConnectClient (SDK) → HealthConnectManager (wrapper) → Repository → ViewModel
 *
 * @param context Application context for Health Connect client initialization
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "HealthConnect"

        /**
         * Required permissions for nutrition data access and energy balance tracking.
         */
        val REQUIRED_PERMISSIONS: Set<String> = setOf(
            "android.permission.health.READ_NUTRITION",
            "android.permission.health.WRITE_NUTRITION",
            "android.permission.health.READ_WEIGHT",
            "android.permission.health.WRITE_WEIGHT",
            "android.permission.health.READ_HEIGHT",
            "android.permission.health.WRITE_HEIGHT",
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
        )
    }

    /**
     * Lazily initialized Health Connect client.
     * Avoids blocking app startup by deferring initialization until first use.
     */
    private val healthConnectClient: HealthConnectClient by lazy {
        Timber.tag(TAG).d("Initializing HealthConnectClient")
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Checks if Health Connect is available on this device.
     *
     * Health Connect may not be installed on Android 9-13 devices (requires Play Store install).
     * Android 14+ includes Health Connect by default.
     *
     * @return true if Health Connect is available, false otherwise
     */
    suspend fun isAvailable(): Boolean {
        val available = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        Timber.tag(TAG).i("Health Connect availability: $available")
        return available
    }

    /**
     * Gets the detailed Health Connect availability status.
     *
     * @return HealthConnectStatus indicating current state (AVAILABLE, NOT_INSTALLED, UPDATE_REQUIRED)
     */
    suspend fun getHealthConnectStatus(): HealthConnectStatus {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        val status = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
            else -> HealthConnectStatus.NOT_INSTALLED
        }
        Timber.tag(TAG).i("Health Connect status: $status (SDK status: $sdkStatus)")
        return status
    }

    /**
     * Checks if all required Health Connect permissions are granted.
     *
     * @return true if READ_NUTRITION and WRITE_NUTRITION permissions are granted, false otherwise
     */
    suspend fun checkPermissions(): Boolean {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        val allGranted = grantedPermissions.containsAll(REQUIRED_PERMISSIONS)

        Timber.tag(TAG).i("Permissions check: $allGranted (granted: ${grantedPermissions.size}/${REQUIRED_PERMISSIONS.size})")
        return allGranted
    }

    /**
     * Creates an ActivityResultContract for requesting Health Connect permissions.
     *
     * This contract should be registered in the activity before onCreate() completes.
     * Usage:
     * ```
     * val requestPermissions = registerForActivityResult(
     *     healthConnectManager.createPermissionRequestContract()
     * ) { granted ->
     *     // Handle permission result
     * }
     * requestPermissions.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
     * ```
     *
     * @return ActivityResultContract for permission requests
     */
    fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    /**
     * Inserts a nutrition record into Health Connect.
     *
     * @param calories Energy content in kilocalories (kcal), must be between 1 and 5000
     * @param description Meal description/name, cannot be blank
     * @param timestamp When the meal was consumed
     * @return The unique record ID assigned by Health Connect
     * @throws SecurityException if permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     * @throws IllegalArgumentException if calories is not in range 1-5000 or description is blank
     */
    suspend fun insertNutritionRecord(
        calories: Int,
        description: String,
        timestamp: Instant,
    ): String {
        // Validation
        require(calories in 1..5000) { "Calories must be between 1 and 5000, got: $calories" }
        require(description.isNotBlank()) { "Description cannot be blank" }

        Timber.tag(TAG).d("Inserting nutrition record: $calories kcal, '$description', at $timestamp")

        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        val record = NutritionRecord(
            startTime = timestamp,
            startZoneOffset = zoneOffset,
            endTime = timestamp.plusSeconds(1),
            endZoneOffset = zoneOffset,
            energy = Energy.kilocalories(calories.toDouble()),
            name = description,
            metadata = Metadata.autoRecorded(
                device = Device(type = Device.TYPE_PHONE),
            ),
        )

        val response = healthConnectClient.insertRecords(listOf(record))
        val recordId = response.recordIdsList.first()

        Timber.tag(TAG).i("Successfully inserted nutrition record: $recordId")
        return recordId
    }

    /**
     * Queries nutrition records from Health Connect within a time range.
     *
     * @param startTime Start of the query time range (inclusive)
     * @param endTime End of the query time range (inclusive)
     * @return List of NutritionRecord objects from Health Connect
     * @throws SecurityException if permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     */
    suspend fun queryNutritionRecords(
        startTime: Instant,
        endTime: Instant,
    ): List<NutritionRecord> {
        Timber.tag(TAG).d("Querying nutrition records: $startTime to $endTime")

        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )

        val response = healthConnectClient.readRecords(request)
        val records = response.records

        Timber.tag(TAG).i("Retrieved ${records.size} nutrition records")
        return records
    }

    /**
     * Updates a nutrition record in Health Connect.
     *
     * Note: Health Connect doesn't support direct updates. This method implements the
     * delete + re-insert pattern, preserving the original timestamp.
     *
     * @param recordId The unique ID of the record to update
     * @param calories New energy content in kilocalories
     * @param description New meal description
     * @param timestamp Original timestamp (preserved)
     * @throws SecurityException if permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     */
    suspend fun updateNutritionRecord(
        recordId: String,
        calories: Int,
        description: String,
        timestamp: Instant,
    ) {
        Timber.tag(TAG).d("Updating nutrition record: $recordId")

        try {
            // Delete old record
            deleteNutritionRecord(recordId)

            // Insert new record with preserved timestamp
            val newRecordId = insertNutritionRecord(calories, description, timestamp)

            Timber.tag(TAG).i("Updated nutrition record: $recordId → $newRecordId")
        } catch (exception: Exception) {
            Timber.tag(TAG).e(exception, "Failed to update nutrition record: $recordId")
            throw exception
        }
    }

    /**
     * Deletes a nutrition record from Health Connect.
     *
     * @param recordId The unique ID of the record to delete
     * @throws SecurityException if permissions are not granted
     * @throws IllegalStateException if Health Connect is not available
     */
    suspend fun deleteNutritionRecord(recordId: String) {
        Timber.tag(TAG).d("Deleting nutrition record: $recordId")

        healthConnectClient.deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = listOf(recordId),
            clientRecordIdsList = emptyList(),
        )

        Timber.tag(TAG).i("Successfully deleted nutrition record: $recordId")
    }

    /**
     * Queries the most recent weight record from Health Connect.
     *
     * Returns the latest WeightRecord by querying with descending order and pageSize=1.
     * This provides the most current weight measurement for BMR calculation pre-population.
     *
     * **Permission Check:**
     * - Returns null if READ_WEIGHT permission not granted (graceful degradation)
     * - Caller must handle null and prompt user for manual entry or permission grant
     *
     * **Use Cases:**
     * - UserProfileRepository pre-populating weight field in Settings
     * - BMR calculator retrieving latest weight for calculations
     *
     * @return Latest WeightRecord or null if none exist or permissions denied
     */
    suspend fun queryLatestWeight(): WeightRecord? {
        Timber.tag(TAG).d("Querying latest weight record")

        // Check permissions before query (avoid crash)
        val hasPermission = healthConnectClient.permissionController.getGrantedPermissions()
            .contains("android.permission.health.READ_WEIGHT")

        if (!hasPermission) {
            Timber.tag(TAG).w("READ_WEIGHT permission not granted, returning null")
            return null
        }

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.EPOCH,
                        Instant.now().plusSeconds(60),
                    ),
                    ascendingOrder = false,
                    pageSize = 1,
                ),
            )
            val record = response.records.firstOrNull()

            if (record != null) {
                Timber.tag(TAG).i("Latest weight: ${record.weight.inKilograms} kg at ${record.time}")
            } else {
                Timber.tag(TAG).d("No weight records found")
            }

            record
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to query weight records")
            null
        }
    }

    /**
     * Queries the most recent height record from Health Connect.
     *
     * Returns the latest HeightRecord by querying with descending order and pageSize=1.
     * This provides the most current height measurement for BMR calculation pre-population.
     *
     * **Permission Check:**
     * - Returns null if READ_HEIGHT permission not granted (graceful degradation)
     * - Caller must handle null and prompt user for manual entry or permission grant
     *
     * **Use Cases:**
     * - UserProfileRepository pre-populating height field in Settings
     * - BMR calculator retrieving latest height for calculations
     *
     * @return Latest HeightRecord or null if none exist or permissions denied
     */
    suspend fun queryLatestHeight(): HeightRecord? {
        Timber.tag(TAG).d("Querying latest height record")

        // Check permissions before query (avoid crash)
        val hasPermission = healthConnectClient.permissionController.getGrantedPermissions()
            .contains("android.permission.health.READ_HEIGHT")

        if (!hasPermission) {
            Timber.tag(TAG).w("READ_HEIGHT permission not granted, returning null")
            return null
        }

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.EPOCH,
                        Instant.now().plusSeconds(60),
                    ),
                    ascendingOrder = false,
                    pageSize = 1,
                ),
            )
            val record = response.records.firstOrNull()

            if (record != null) {
                Timber.tag(TAG).i("Latest height: ${record.height.inMeters} m at ${record.time}")
            } else {
                Timber.tag(TAG).d("No height records found")
            }

            record
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to query height records")
            null
        }
    }

    /**
     * Inserts a new weight measurement to Health Connect.
     *
     * Creates a timestamped WeightRecord with Foodie as the data source.
     * This allows the app to contribute weight data when user manually enters it in Settings.
     *
     * **Permissions:**
     * - Requires WRITE_WEIGHT permission
     * - Returns Result.failure(SecurityException) if permission denied
     *
     * **Timestamp:**
     * - Uses provided timestamp (typically Instant.now() for new entries)
     * - ZoneOffset uses system default
     *
     * **Metadata:**
     * - DataOrigin: "com.foodie.app" (identifies Foodie as source in Health Connect)
     * - Allows other apps (Google Fit, Garmin) to distinguish Foodie entries
     *
     * @param weightKg Weight in kilograms (caller validates range 30-300)
     * @param timestamp When the measurement was taken
     * @return Result.success or Result.failure with SecurityException/Exception
     */
    suspend fun insertWeight(weightKg: Double, timestamp: Instant): Result<Unit> {
        Timber.tag(TAG).d("Inserting weight: $weightKg kg at $timestamp")

        // Check permissions before insert (avoid crash)
        val hasPermission = healthConnectClient.permissionController.getGrantedPermissions()
            .contains("android.permission.health.WRITE_WEIGHT")

        if (!hasPermission) {
            Timber.tag(TAG).e("WRITE_WEIGHT permission denied")
            return Result.failure(SecurityException("WRITE_WEIGHT permission denied"))
        }

        return try {
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

            val record = WeightRecord(
                weight = Mass.kilograms(weightKg),
                time = timestamp,
                zoneOffset = zoneOffset,
                metadata = Metadata.autoRecorded(
                    device = Device(type = Device.TYPE_PHONE),
                ),
            )

            healthConnectClient.insertRecords(listOf(record))

            Timber.tag(TAG).i("Successfully inserted weight record: $weightKg kg")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to insert weight record")
            Result.failure(e)
        }
    }

    /**
     * Inserts a new height measurement to Health Connect.
     *
     * Creates a timestamped HeightRecord with Foodie as the data source.
     * This allows the app to contribute height data when user manually enters it in Settings.
     *
     * **Unit Conversion:**
     * - Input: heightCm in centimeters (domain model)
     * - Storage: Length.meters(heightCm / 100.0) for Health Connect API
     * - Conversion factor: 1 meter = 100 centimeters
     *
     * **Permissions:**
     * - Requires WRITE_HEIGHT permission
     * - Returns Result.failure(SecurityException) if permission denied
     *
     * **Timestamp:**
     * - Uses provided timestamp (typically Instant.now() for new entries)
     * - ZoneOffset uses system default
     *
     * **Metadata:**
     * - DataOrigin: "com.foodie.app" (identifies Foodie as source in Health Connect)
     * - Allows other apps (Google Fit, Garmin) to distinguish Foodie entries
     *
     * @param heightCm Height in centimeters (caller validates range 100-250)
     * @param timestamp When the measurement was taken
     * @return Result.success or Result.failure with SecurityException/Exception
     */
    suspend fun insertHeight(heightCm: Double, timestamp: Instant): Result<Unit> {
        Timber.tag(TAG).d("Inserting height: $heightCm cm at $timestamp")

        // Check permissions before insert (avoid crash)
        val hasPermission = healthConnectClient.permissionController.getGrantedPermissions()
            .contains("android.permission.health.WRITE_HEIGHT")

        if (!hasPermission) {
            Timber.tag(TAG).e("WRITE_HEIGHT permission denied")
            return Result.failure(SecurityException("WRITE_HEIGHT permission denied"))
        }

        return try {
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
            val heightMeters = heightCm / 100.0 // Convert cm to meters

            val record = HeightRecord(
                height = Length.meters(heightMeters),
                time = timestamp,
                zoneOffset = zoneOffset,
                metadata = Metadata.autoRecorded(
                    device = Device(type = Device.TYPE_PHONE),
                ),
            )

            healthConnectClient.insertRecords(listOf(record))

            Timber.tag(TAG).i("Successfully inserted height record: $heightCm cm ($heightMeters m)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to insert height record")
            Result.failure(e)
        }
    }

    /**
     * Queries total step count from Health Connect within a time range.
     *
     * **Multi-Record Summation:**
     * Health Connect may store multiple StepsRecord entries per day (e.g., Garmin writes
     * separate records from Google Fit). This method sums all StepsRecord.count values
     * to get the total daily step count.
     *
     * **Permission Check:**
     * - Throws SecurityException if READ_STEPS permission not granted
     * - Caller should catch SecurityException and return appropriate error type
     *
     * **Use Cases:**
     * - NEAT calculation: querySteps(startOfDay, now) for today's passive energy
     * - Historical queries: querySteps(yesterday, yesterday + 1 day) for past days
     *
     * @param startTime Start of the query time range (inclusive)
     * @param endTime End of the query time range (inclusive)
     * @return Total step count summed across all records, or 0 if no records exist
     * @throws SecurityException if READ_STEPS permission not granted
     */
    suspend fun querySteps(startTime: Instant, endTime: Instant): Int {
        Timber.tag(TAG).d("Querying steps: $startTime to $endTime")

        // Check permissions before query (throw SecurityException to be caught by caller)
        val hasPermission = healthConnectClient.permissionController.getGrantedPermissions()
            .contains("android.permission.health.READ_STEPS")

        if (!hasPermission) {
            Timber.tag(TAG).e("READ_STEPS permission denied")
            throw SecurityException("READ_STEPS permission denied")
        }

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                ),
            )

            // Sum all StepsRecord.count values (multiple apps may write separate records)
            val totalSteps = response.records.sumOf { it.count.toInt() }

            Timber.tag(TAG).i("Total steps: $totalSteps (from ${response.records.size} records)")
            totalSteps
        } catch (e: SecurityException) {
            // Re-throw SecurityException for caller to handle
            Timber.tag(TAG).e(e, "Permission denied querying steps")
            throw e
        } catch (e: Exception) {
            // Other errors return 0 (graceful degradation)
            Timber.tag(TAG).e(e, "Failed to query steps, returning 0")
            0
        }
    }

    /**
     * Observes step count changes over time using polling.
     *
     * **Polling Strategy:**
     * Health Connect does not support real-time change listeners. This method polls
     * querySteps() every 5 minutes to detect new step data synced from Garmin, Google Fit, etc.
     *
     * **Flow Lifecycle:**
     * - Emits immediately with current step count
     * - Polls every 5 minutes while Flow is active (collector is listening)
     * - Cancels polling when Flow collector is cancelled
     *
     * **Use Cases:**
     * - EnergyBalanceRepository.getNEAT() reactive stream
     * - Dashboard real-time NEAT updates when step data syncs
     *
     * @return Flow emitting total step count every 5 minutes (from midnight to now)
     */
    fun observeSteps(): Flow<Int> = flow {
        while (true) {
            val startOfDay = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
            val now = Instant.now()
            val steps = querySteps(startOfDay, now)
            emit(steps)
            delay(5.minutes)
        }
    }

    /**
     * Queries total active calories burned from Health Connect within a time range.
     *
     * **Multi-Record Summation:**
     * Health Connect may store multiple ActiveCaloriesBurnedRecord entries per day
     * (e.g., Garmin writes one record per workout session). This method sums all
     * ActiveCaloriesBurnedRecord.energy.inKilocalories values to get total active calories.
     *
     * **Permission Check:**
     * - Throws SecurityException if READ_ACTIVE_CALORIES_BURNED permission not granted
     * - Caller should catch SecurityException and return appropriate error type
     *
     * **Use Cases:**
     * - Active Energy calculation: queryActiveCalories(startOfDay, now) for today's workout calories
     * - Historical queries: queryActiveCalories(yesterday, yesterday + 1 day) for past days
     *
     * @param startTime Start of the query time range (inclusive)
     * @param endTime End of the query time range (inclusive)
     * @return Total active calories in kcal summed across all records, or 0.0 if no records exist
     * @throws SecurityException if READ_ACTIVE_CALORIES_BURNED permission not granted
     */
    suspend fun queryActiveCalories(startTime: Instant, endTime: Instant): Double {
        Timber.tag(TAG).d("Querying active calories: $startTime to $endTime")

        // Check permissions before query (throw SecurityException to be caught by caller)
        val hasPermission = healthConnectClient.permissionController.getGrantedPermissions()
            .contains("android.permission.health.READ_ACTIVE_CALORIES_BURNED")

        if (!hasPermission) {
            Timber.tag(TAG).e("READ_ACTIVE_CALORIES_BURNED permission denied")
            throw SecurityException("READ_ACTIVE_CALORIES_BURNED permission denied")
        }

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                ),
            )

            // Sum all ActiveCaloriesBurnedRecord.energy.inKilocalories values
            // (Garmin may write multiple records - one per workout session)
            val totalActiveCalories = response.records.sumOf { it.energy.inKilocalories }

            Timber.tag(TAG).i("Total active calories: $totalActiveCalories kcal (from ${response.records.size} records)")
            totalActiveCalories
        } catch (e: SecurityException) {
            // Re-throw SecurityException for caller to handle
            Timber.tag(TAG).e(e, "Permission denied querying active calories")
            throw e
        } catch (e: Exception) {
            // Other errors return 0.0 (graceful degradation)
            Timber.tag(TAG).e(e, "Failed to query active calories, returning 0.0")
            0.0
        }
    }

    /**
     * Observes active calories burned changes over time using polling.
     *
     * **Polling Strategy:**
     * Health Connect does not support real-time change listeners. This method polls
     * queryActiveCalories() every 5 minutes to detect new workout data synced from
     * Garmin Connect (typical 5-15 minute sync delay after workout completion).
     *
     * **Flow Lifecycle:**
     * - Emits immediately with current active calories
     * - Polls every 5 minutes while Flow is active (collector is listening)
     * - Cancels polling when Flow collector is cancelled
     *
     * **Use Cases:**
     * - EnergyBalanceRepository.getActiveCalories() reactive stream
     * - Dashboard real-time Active Energy updates when Garmin syncs workouts
     *
     * @return Flow emitting total active calories in kcal every 5 minutes (from midnight to now)
     */
    fun observeActiveCalories(): Flow<Double> = flow {
        while (true) {
            val startOfDay = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
            val now = Instant.now()
            val activeCalories = queryActiveCalories(startOfDay, now)
            emit(activeCalories)
            delay(5.minutes)
        }
    }
}
