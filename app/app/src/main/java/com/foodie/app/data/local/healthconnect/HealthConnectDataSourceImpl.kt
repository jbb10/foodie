package com.foodie.app.data.local.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Implementation of HealthConnectDataSource using the Health Connect SDK.
 *
 * This replaces the PLACEHOLDER implementation from Story 1.2 with full Health Connect integration.
 * Story 1.4: Health Connect Integration Setup - Complete implementation.
 */
@Singleton
class HealthConnectDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthConnectDataSource {

    companion object {
        private const val TAG = "HealthConnect"

        val REQUIRED_PERMISSIONS: Set<String> = setOf(
            "android.permission.health.READ_NUTRITION",
            "android.permission.health.WRITE_NUTRITION",
        )
    }

    private val healthConnectClient: HealthConnectClient by lazy {
        Timber.tag(TAG).d("Initializing HealthConnectClient")
        HealthConnectClient.getOrCreate(context)
    }

    override suspend fun queryNutritionRecords(startTime: Instant, endTime: Instant): List<NutritionRecord> {
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

    override suspend fun insertNutritionRecord(calories: Int, description: String, timestamp: Instant): String {
        Timber.tag(TAG).d("Inserting nutrition record: $calories kcal, '$description', at $timestamp")

        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        val durationMinutes = when {
            calories < 300 -> 5
            calories < 800 -> 10
            else -> 15
        }
        val endTime = timestamp.plus(durationMinutes.toLong(), java.time.temporal.ChronoUnit.MINUTES)

        val record = NutritionRecord(
            startTime = timestamp,
            startZoneOffset = zoneOffset,
            endTime = endTime,
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

    override suspend fun deleteNutritionRecord(recordId: String) {
        Timber.tag(TAG).d("Deleting nutrition record: $recordId")

        healthConnectClient.deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = listOf(recordId),
            clientRecordIdsList = emptyList(),
        )

        Timber.tag(TAG).i("Successfully deleted nutrition record: $recordId")
    }

    override suspend fun checkPermissions(): Boolean {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        val allGranted = grantedPermissions.containsAll(REQUIRED_PERMISSIONS)

        Timber.tag(TAG).i("Permissions check: $allGranted (granted: ${grantedPermissions.size}/${REQUIRED_PERMISSIONS.size})")
        return allGranted
    }

    suspend fun isAvailable(): Boolean {
        val available = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        Timber.tag(TAG).i("Health Connect availability: $available")
        return available
    }
}
