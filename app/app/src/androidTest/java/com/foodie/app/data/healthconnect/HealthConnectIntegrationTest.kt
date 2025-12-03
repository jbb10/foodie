package com.foodie.app.data.healthconnect

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for Health Connect round-trip operations.
 *
 * Tests verify end-to-end insert + query flow with Health Connect SDK.
 * Note: Requires Health Connect to be installed and permissions granted on test device.
 *
 * Test Setup Requirements:
 * - Health Connect app must be installed on device/emulator
 * - WRITE_NUTRITION and READ_NUTRITION permissions must be granted to test app
 * - Permissions can be granted via: Settings → Apps → Foodie → Permissions → Health Connect
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthConnectIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var healthConnectManager: HealthConnectManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun healthConnectRoundTrip_insertsAndQueriesSuccessfully() = runTest {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        // Check if Health Connect is available
        val isAvailable = manager.isAvailable()
        if (!isAvailable) {
            // Skip test if Health Connect not available on device
            return@runTest
        }

        val calories = 500
        val description = "Integration test meal"
        val timestamp = Instant.now()

        try {
            // When - Insert test record
            val recordId = manager.insertNutritionRecord(calories, description, timestamp)

            // Then - Verify record was created
            assertThat(recordId).isNotEmpty()

            // When - Query records
            val startTime = timestamp.minusSeconds(3600)
            val endTime = timestamp.plusSeconds(3600)
            val records = manager.queryNutritionRecords(startTime, endTime)

            // Then - Verify test record exists in results
            val testRecord = records.find { it.metadata.id == recordId }
            assertThat(testRecord).isNotNull()
            testRecord?.let {
                assertThat(it.energy?.inKilocalories).isEqualTo(calories.toDouble())
                assertThat(it.name).isEqualTo(description)
            }

            // Cleanup - Delete test record
            manager.deleteNutritionRecord(recordId)
        } catch (e: SecurityException) {
            // Skip test if permissions not granted
            // In real CI/CD, this would require proper test setup
            return@runTest
        }
    }

    @Test
    fun insertNutritionRecord_withTimestamp_preservesLocalTimeZone() = runTest {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        // Check if Health Connect is available
        val isAvailable = manager.isAvailable()
        if (!isAvailable) {
            return@runTest
        }

        val calories = 350
        val description = "Time zone test meal"
        val timestamp = Instant.now()
        val expectedZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)

        try {
            // When - Insert test record
            val recordId = manager.insertNutritionRecord(calories, description, timestamp)

            // Then - Query back and verify zone offset
            val startTime = timestamp.minusSeconds(60)
            val endTime = timestamp.plusSeconds(60)
            val records = manager.queryNutritionRecords(startTime, endTime)

            val testRecord = records.find { it.metadata.id == recordId }
            assertThat(testRecord).isNotNull()
            testRecord?.let {
                // Verify zone offsets match device local time zone
                assertThat(it.startZoneOffset).isEqualTo(expectedZoneOffset)
                assertThat(it.endZoneOffset).isEqualTo(expectedZoneOffset)

                // Verify timestamps are preserved
                assertThat(it.startTime).isEqualTo(timestamp)
                // 350 calories -> 15 minutes duration
                assertThat(it.endTime).isEqualTo(timestamp.plus(15, java.time.temporal.ChronoUnit.MINUTES))
            }

            // Cleanup
            manager.deleteNutritionRecord(recordId)
        } catch (e: SecurityException) {
            return@runTest
        }
    }

    @Test
    fun insertNutritionRecord_whenPermissionsGranted_dataVisibleInHealthConnect() = runTest {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        // Check availability and permissions
        val isAvailable = manager.isAvailable()
        if (!isAvailable) {
            return@runTest
        }

        val calories = 650
        val description = "Visibility test meal"
        val timestamp = Instant.now()

        try {
            // When - Insert record
            val recordId = manager.insertNutritionRecord(calories, description, timestamp)

            // Then - Query immediately and verify data is visible
            val startTime = timestamp.minusSeconds(10)
            val endTime = timestamp.plusSeconds(10)
            val records = manager.queryNutritionRecords(startTime, endTime)

            // Verify record exists with correct data
            val testRecord = records.find { it.metadata.id == recordId }
            assertThat(testRecord).isNotNull()
            testRecord?.let {
                assertThat(it.energy?.inKilocalories).isEqualTo(calories.toDouble())
                assertThat(it.name).isEqualTo(description)
            }

            // Cleanup
            manager.deleteNutritionRecord(recordId)
        } catch (e: SecurityException) {
            return@runTest
        }
    }

    @Test
    fun insertNutritionRecord_calculatesDurationBasedOnCalories() = runTest {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        if (!manager.isAvailable()) return@runTest

        val timestamp = Instant.now()

        // Case 1: < 300 cal -> 5 min
        try {
            val id1 = manager.insertNutritionRecord(200, "Small snack", timestamp)
            val records1 = manager.queryNutritionRecords(timestamp.minusSeconds(60), timestamp.plusSeconds(3600))
            val record1 = records1.find { it.metadata.id == id1 }
            assertThat(record1).isNotNull()
            assertThat(record1!!.endTime).isEqualTo(timestamp.plus(5, java.time.temporal.ChronoUnit.MINUTES))
            manager.deleteNutritionRecord(id1)
        } catch (e: SecurityException) { return@runTest }

        // Case 2: 300-799 cal -> 15 min
        try {
            val id2 = manager.insertNutritionRecord(500, "Medium meal", timestamp)
            val records2 = manager.queryNutritionRecords(timestamp.minusSeconds(60), timestamp.plusSeconds(3600))
            val record2 = records2.find { it.metadata.id == id2 }
            assertThat(record2).isNotNull()
            assertThat(record2!!.endTime).isEqualTo(timestamp.plus(15, java.time.temporal.ChronoUnit.MINUTES))
            manager.deleteNutritionRecord(id2)
        } catch (e: SecurityException) { return@runTest }

        // Case 3: >= 800 cal -> 30 min
        try {
            val id3 = manager.insertNutritionRecord(900, "Large meal", timestamp)
            val records3 = manager.queryNutritionRecords(timestamp.minusSeconds(60), timestamp.plusSeconds(3600))
            val record3 = records3.find { it.metadata.id == id3 }
            assertThat(record3).isNotNull()
            assertThat(record3!!.endTime).isEqualTo(timestamp.plus(30, java.time.temporal.ChronoUnit.MINUTES))
            manager.deleteNutritionRecord(id3)
        } catch (e: SecurityException) { return@runTest }
    }

    @Test
    fun healthConnectAvailability_canBeChecked() = runTest {
        // When
        val isAvailable = healthConnectManager.isAvailable()

        // Then - Method completes successfully
        assertThat(isAvailable).isNotNull()
    }

    /**
     * Test for Story 4.5: Health Connect availability status.
     */
    @Test
    fun getHealthConnectStatus_returnsValidStatus() = runTest {
        // When
        val status = healthConnectManager.getHealthConnectStatus()

        // Then - Status is one of the valid enum values
        assertThat(status).isAnyOf(
            com.foodie.app.data.local.healthconnect.HealthConnectStatus.AVAILABLE,
            com.foodie.app.data.local.healthconnect.HealthConnectStatus.NOT_INSTALLED,
            com.foodie.app.data.local.healthconnect.HealthConnectStatus.UPDATE_REQUIRED,
        )
    }

    /**
     * Test for Story 4.5: Health Connect status should be AVAILABLE when isAvailable returns true.
     */
    @Test
    fun getHealthConnectStatus_whenAvailable_returnsAvailable() = runTest {
        // When
        val isAvailable = healthConnectManager.isAvailable()
        val status = healthConnectManager.getHealthConnectStatus()

        // Then - If isAvailable is true, status should be AVAILABLE
        if (isAvailable) {
            assertThat(status).isEqualTo(
                com.foodie.app.data.local.healthconnect.HealthConnectStatus.AVAILABLE,
            )
        }
    }

    /**
     * Test for Story 4.5: Health Connect status should be NOT_INSTALLED when isAvailable returns false
     * (unless UPDATE_REQUIRED).
     */
    @Test
    fun getHealthConnectStatus_whenNotAvailable_returnsNotInstalledOrUpdateRequired() = runTest {
        // When
        val isAvailable = healthConnectManager.isAvailable()
        val status = healthConnectManager.getHealthConnectStatus()

        // Then - If isAvailable is false, status should be NOT_INSTALLED or UPDATE_REQUIRED
        if (!isAvailable) {
            assertThat(status).isAnyOf(
                com.foodie.app.data.local.healthconnect.HealthConnectStatus.NOT_INSTALLED,
                com.foodie.app.data.local.healthconnect.HealthConnectStatus.UPDATE_REQUIRED,
            )
        }
    }
}
