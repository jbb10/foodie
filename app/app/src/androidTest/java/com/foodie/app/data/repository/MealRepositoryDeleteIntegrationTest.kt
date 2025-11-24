package com.foodie.app.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodie.app.data.local.healthconnect.HealthConnectDataSourceImpl
import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.util.Result
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

/**
 * Integration test for MealRepository delete operations with Health Connect.
 *
 * Tests that delete operations correctly interact with the Health Connect API
 * and validate AC #4, #7, #8 (deletion behavior).
 *
 * NOTE: Requires Health Connect permissions to be granted before running.
 * Run: adb shell pm grant com.foodie.app android.permission.health.READ_NUTRITION
 *      adb shell pm grant com.foodie.app android.permission.health.WRITE_NUTRITION
 */
@RunWith(AndroidJUnit4::class)
class MealRepositoryDeleteIntegrationTest {

    private lateinit var context: Context
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var healthConnectDataSource: HealthConnectDataSourceImpl
    private lateinit var mealRepository: MealRepository

    private val testRecordIds = mutableListOf<String>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Check if Health Connect is available
        val availability = HealthConnectClient.getSdkStatus(context)
        assertThat(availability).isEqualTo(HealthConnectClient.SDK_AVAILABLE)

        healthConnectClient = HealthConnectClient.getOrCreate(context)
        healthConnectDataSource = HealthConnectDataSourceImpl(context)
        mealRepository = MealRepositoryImpl(healthConnectDataSource)

        // Check permissions - skip tests if not granted
        // Tests require manual permission grant:
        // adb shell pm grant com.foodie.app android.permission.health.READ_NUTRITION
        // adb shell pm grant com.foodie.app android.permission.health.WRITE_NUTRITION
        var hasPermission = false
        try {
            // Attempt a simple write operation to verify permissions
            val testTimestamp = Instant.now()
            val testRecord = NutritionRecord(
                startTime = testTimestamp,
                endTime = testTimestamp.plusSeconds(1),
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(testTimestamp),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(testTimestamp),
                energy = Energy.kilocalories(1.0),
                name = "Permission Test",
                metadata = Metadata.autoRecorded(device = Device(type = Device.TYPE_PHONE))
            )

            // Test permission in blocking call
            kotlinx.coroutines.runBlocking {
                try {
                    healthConnectClient.insertRecords(listOf(testRecord))
                    healthConnectClient.deleteRecords(
                        recordType = NutritionRecord::class,
                        recordIdsList = emptyList(),
                        clientRecordIdsList = emptyList()
                    )
                    hasPermission = true
                } catch (e: SecurityException) {
                    hasPermission = false
                }
            }
        } catch (e: Exception) {
            hasPermission = false
        }

        assumeTrue(
            "Health Connect WRITE_NUTRITION permission not granted. " +
            "Run: adb shell pm grant com.foodie.app android.permission.health.WRITE_NUTRITION",
            hasPermission
        )
    }

    @After
    fun cleanup() = runTest {
        // Clean up any test records that weren't deleted
        if (testRecordIds.isNotEmpty()) {
            try {
                healthConnectClient.deleteRecords(
                    recordType = NutritionRecord::class,
                    recordIdsList = testRecordIds,
                    clientRecordIdsList = emptyList()
                )
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        testRecordIds.clear()
    }

    @Test
    fun deleteMeal_removesRecordFromHealthConnect() = runTest {
        // Given - Insert a test nutrition record
        val timestamp = Instant.now()
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        val testRecord = NutritionRecord(
            startTime = timestamp,
            endTime = timestamp.plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            energy = Energy.kilocalories(500.0),
            name = "Test Delete Meal",
            metadata = Metadata.autoRecorded(
                device = Device(type = Device.TYPE_PHONE)
            )
        )

        val insertResponse = healthConnectClient.insertRecords(listOf(testRecord))
        val recordId = insertResponse.recordIdsList.first()
        testRecordIds.add(recordId)

        // Verify record exists
        val readRequest = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                startTime = timestamp.minusSeconds(10),
                endTime = timestamp.plusSeconds(10)
            )
        )
        val beforeDelete = healthConnectClient.readRecords(readRequest)
        val recordExists = beforeDelete.records.any { it.metadata.id == recordId }
        assertThat(recordExists).isTrue()

        // When - Delete the meal via repository
        val result = mealRepository.deleteMeal(recordId)

        // Then - Verify deletion successful (AC #4)
        assertThat(result).isInstanceOf(Result.Success::class.java)

        // Verify record no longer exists in Health Connect (AC #7, #8)
        val afterDelete = healthConnectClient.readRecords(readRequest)
        val recordStillExists = afterDelete.records.any { it.metadata.id == recordId }
        assertThat(recordStillExists).isFalse()

        // Remove from cleanup list since it's already deleted
        testRecordIds.remove(recordId)
    }

    @Test
    fun deleteMeal_withInvalidId_returnsError() = runTest {
        // Given - Non-existent record ID
        val invalidId = "non-existent-record-id-12345"

        // When - Try to delete
        val result = mealRepository.deleteMeal(invalidId)

        // Then - Verify error returned (AC #4 error case)
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val error = result as Result.Error
        assertThat(error.message).isNotEmpty()
    }

    @Test
    fun deleteMeal_isPermanent_cannotBeUndone() = runTest {
        // Given - Insert and then delete a record
        val timestamp = Instant.now()
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        val testRecord = NutritionRecord(
            startTime = timestamp,
            endTime = timestamp.plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            energy = Energy.kilocalories(400.0),
            name = "Test Permanent Delete",
            metadata = Metadata.autoRecorded(
                device = Device(type = Device.TYPE_PHONE)
            )
        )

        val insertResponse = healthConnectClient.insertRecords(listOf(testRecord))
        val recordId = insertResponse.recordIdsList.first()

        // Delete the record
        val deleteResult = mealRepository.deleteMeal(recordId)
        assertThat(deleteResult).isInstanceOf(Result.Success::class.java)

        // Then - Verify record cannot be retrieved (permanent deletion, AC #7)
        val readRequest = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                startTime = timestamp.minusSeconds(10),
                endTime = timestamp.plusSeconds(10)
            )
        )
        val afterDelete = healthConnectClient.readRecords(readRequest)
        val recordFound = afterDelete.records.any { it.metadata.id == recordId }
        assertThat(recordFound).isFalse()

        // Verify no "undo" mechanism exists - trying to delete again should fail
        val reDeleteResult = mealRepository.deleteMeal(recordId)
        assertThat(reDeleteResult).isInstanceOf(Result.Error::class.java)
    }

    @Test
    fun deleteMeal_immediatelyReflectedInQueries() = runTest {
        // Given - Insert multiple test records
        val timestamp = Instant.now()
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        val record1 = NutritionRecord(
            startTime = timestamp.minusSeconds(3600),
            endTime = timestamp.minusSeconds(3600).plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            energy = Energy.kilocalories(300.0),
            name = "Test Meal 1",
            metadata = Metadata.autoRecorded(
                device = Device(type = Device.TYPE_PHONE)
            )
        )
        val record2 = NutritionRecord(
            startTime = timestamp,
            endTime = timestamp.plusSeconds(1),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            energy = Energy.kilocalories(500.0),
            name = "Test Meal 2",
            metadata = Metadata.autoRecorded(
                device = Device(type = Device.TYPE_PHONE)
            )
        )

        val insertResponse = healthConnectClient.insertRecords(listOf(record1, record2))
        val recordId1 = insertResponse.recordIdsList[0]
        val recordId2 = insertResponse.recordIdsList[1]
        testRecordIds.addAll(listOf(recordId1, recordId2))

        // Verify both records exist
        val readRequest = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                startTime = timestamp.minusSeconds(7200),
                endTime = timestamp.plusSeconds(10)
            )
        )
        val beforeDelete = healthConnectClient.readRecords(readRequest)
        assertThat(beforeDelete.records.filter { it.metadata.id in listOf(recordId1, recordId2) })
            .hasSize(2)

        // When - Delete first record
        val deleteResult = mealRepository.deleteMeal(recordId1)
        assertThat(deleteResult).isInstanceOf(Result.Success::class.java)
        testRecordIds.remove(recordId1)

        // Then - Query immediately shows only 1 record (AC #5, #8)
        val afterDelete = healthConnectClient.readRecords(readRequest)
        val remainingRecords = afterDelete.records.filter { it.metadata.id in listOf(recordId1, recordId2) }
        assertThat(remainingRecords).hasSize(1)
        assertThat(remainingRecords.first().metadata.id).isEqualTo(recordId2)
    }
}
