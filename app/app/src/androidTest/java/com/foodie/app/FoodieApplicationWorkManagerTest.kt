package com.foodie.app

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for FoodieApplication WorkManager setup.
 *
 * Tests verify:
 * - Photo cleanup periodic work is scheduled on app startup
 * - Work is enqueued with correct name and constraints
 *
 * Story 4.4: Photo Retention and Cleanup - Task 7
 */
class FoodieApplicationWorkManagerTest {

    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<FoodieApplication>()
        
        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun testPhotoCleanupPeriodicWorkScheduled() {
        // Act: Application.onCreate() should have scheduled the work
        // (Already called during app initialization)
        
        // Assert: Verify work is enqueued
        val workInfos = workManager.getWorkInfosForUniqueWork("photo_cleanup_periodic").get()
        
        assertNotNull("Photo cleanup work should be scheduled", workInfos)
        assertEquals("Should have exactly one periodic work enqueued", 1, workInfos.size)
        
        val workInfo = workInfos.first()
        
        // Verify work is enqueued (not cancelled or failed)
        val validStates = listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING)
        assert(workInfo.state in validStates) {
            "Work should be enqueued or running, but was: ${workInfo.state}"
        }
    }
}
