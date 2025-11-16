package com.foodie.app.data.local.healthconnect

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect availability status.
 */
enum class HealthConnectStatus {
    /** Health Connect is installed and available */
    AVAILABLE,
    /** Health Connect is not installed on the device */
    NOT_INSTALLED,
    /** Health Connect needs to be updated to work properly */
    UPDATE_REQUIRED
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
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthConnect"
        
        /**
         * Required permissions for nutrition data access.
         */
        val REQUIRED_PERMISSIONS: Set<String> = setOf(
            "android.permission.health.READ_NUTRITION",
            "android.permission.health.WRITE_NUTRITION"
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
        timestamp: Instant
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
                device = Device(type = Device.TYPE_PHONE)
            )
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
        endTime: Instant
    ): List<NutritionRecord> {
        Timber.tag(TAG).d("Querying nutrition records: $startTime to $endTime")
        
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
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
        timestamp: Instant
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
            clientRecordIdsList = emptyList()
        )
        
        Timber.tag(TAG).i("Successfully deleted nutrition record: $recordId")
    }
}
