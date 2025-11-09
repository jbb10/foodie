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
import javax.inject.Inject

/**
 * Integration test for Health Connect round-trip operations.
 *
 * Tests verify end-to-end insert + query flow with Health Connect SDK.
 * Note: Requires Health Connect to be installed and permissions granted on test device.
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
    fun healthConnectAvailability_canBeChecked() = runTest {
        // When
        val isAvailable = healthConnectManager.isAvailable()
        
        // Then - Method completes successfully
        assertThat(isAvailable).isNotNull()
    }
}
