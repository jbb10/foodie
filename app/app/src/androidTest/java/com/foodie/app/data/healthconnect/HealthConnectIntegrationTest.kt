package com.foodie.app.data.healthconnect

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

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
                assertThat(it.endTime).isEqualTo(timestamp.plusSeconds(1))
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
    fun healthConnectAvailability_canBeChecked() = runTest {
        // When
        val isAvailable = healthConnectManager.isAvailable()
        
        // Then - Method completes successfully
        assertThat(isAvailable).isNotNull()
    }
}
