package com.foodie.app.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.foodie.app.data.local.cache.PhotoManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

/**
 * Unit tests for PhotoCleanupWorker.
 *
 * Tests verify:
 * - Photos older than 24 hours are deleted
 * - Photos younger than 24 hours are retained
 * - Cleanup logs metrics correctly
 * - Cleanup handles file deletion errors gracefully
 * - Worker returns success even with partial failures
 *
 * Story 4.4: Photo Retention and Cleanup
 */
@HiltAndroidTest
class PhotoCleanupWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var photoManager: PhotoManager

    @Inject
    lateinit var workerFactory: WorkerFactory

    private lateinit var context: Context
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        hiltRule.inject()
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        cacheDir = photoManager.getCacheDir()
        
        // Clean cache directory before each test
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    @After
    fun tearDown() {
        // Clean up after tests
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun testPhotoOlderThan24Hours_isDeleted() = runBlocking {
        // Arrange: Create a photo file and set its last modified time to 25 hours ago
        val oldFile = File(cacheDir, "meal_old_photo.jpg")
        oldFile.createNewFile()
        oldFile.writeText("old photo content")
        
        val twentyFiveHoursAgo = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        oldFile.setLastModified(twentyFiveHoursAgo)
        
        assertTrue("Old file should exist before cleanup", oldFile.exists())
        
        // Act: Run the cleanup worker
        val worker = TestListenableWorkerBuilder<PhotoCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        
        val result = worker.doWork()
        
        // Assert: Worker succeeds and old file is deleted
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("Old file should be deleted after cleanup", !oldFile.exists())
    }

    @Test
    fun testPhotoYoungerThan24Hours_isRetained() = runBlocking {
        // Arrange: Create a photo file with current timestamp (< 24 hours old)
        val recentFile = File(cacheDir, "meal_recent_photo.jpg")
        recentFile.createNewFile()
        recentFile.writeText("recent photo content")
        
        // File is fresh (just created), lastModified is ~now
        assertTrue("Recent file should exist before cleanup", recentFile.exists())
        
        // Act: Run the cleanup worker
        val worker = TestListenableWorkerBuilder<PhotoCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        
        val result = worker.doWork()
        
        // Assert: Worker succeeds and recent file is retained
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("Recent file should be retained after cleanup", recentFile.exists())
    }

    @Test
    fun testMultiplePhotosWithMixedAges_correctDeletion() = runBlocking {
        // Arrange: Create multiple photos with different ages
        val oldFile1 = File(cacheDir, "meal_old1.jpg")
        oldFile1.createNewFile()
        oldFile1.setLastModified(System.currentTimeMillis() - (30 * 60 * 60 * 1000L)) // 30 hours
        
        val oldFile2 = File(cacheDir, "meal_old2.jpg")
        oldFile2.createNewFile()
        oldFile2.setLastModified(System.currentTimeMillis() - (48 * 60 * 60 * 1000L)) // 48 hours
        
        val recentFile1 = File(cacheDir, "meal_recent1.jpg")
        recentFile1.createNewFile()
        recentFile1.setLastModified(System.currentTimeMillis() - (12 * 60 * 60 * 1000L)) // 12 hours
        
        val recentFile2 = File(cacheDir, "meal_recent2.jpg")
        recentFile2.createNewFile()
        // Fresh file (just created)
        
        assertTrue("Setup: All files should exist", 
            oldFile1.exists() && oldFile2.exists() && recentFile1.exists() && recentFile2.exists())
        
        // Act: Run the cleanup worker
        val worker = TestListenableWorkerBuilder<PhotoCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        
        val result = worker.doWork()
        
        // Assert: Old files deleted, recent files retained
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("Old files should be deleted", !oldFile1.exists() && !oldFile2.exists())
        assertTrue("Recent files should be retained", recentFile1.exists() && recentFile2.exists())
    }

    @Test
    fun testEmptyCacheDirectory_returnsSuccess() = runBlocking {
        // Arrange: Ensure cache directory is empty (setUp already cleaned it)
        assertEquals("Cache should be empty", 0, cacheDir.listFiles()?.size ?: 0)
        
        // Act: Run the cleanup worker
        val worker = TestListenableWorkerBuilder<PhotoCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        
        val result = worker.doWork()
        
        // Assert: Worker succeeds even with no files to clean
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testCleanupLogsMetrics_verifiesNoExceptions() = runBlocking {
        // Arrange: Create photos to generate cleanup activity
        val oldFile = File(cacheDir, "meal_to_delete.jpg")
        oldFile.createNewFile()
        oldFile.writeText("content to measure size")
        oldFile.setLastModified(System.currentTimeMillis() - (25 * 60 * 60 * 1000L))
        
        val recentFile = File(cacheDir, "meal_to_keep.jpg")
        recentFile.createNewFile()
        recentFile.writeText("content to keep")
        
        // Act: Run the cleanup worker (logs will be written to Logcat)
        val worker = TestListenableWorkerBuilder<PhotoCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        
        val result = worker.doWork()
        
        // Assert: Worker completes successfully (logging doesn't throw exceptions)
        assertEquals(ListenableWorker.Result.success(), result)
        
        // Note: Actual log verification would require Logcat inspection or test log interceptor
        // This test verifies that logging code doesn't break the worker execution
    }

    @Test
    fun testReadOnlyFile_handlesDeletionError() = runBlocking {
        // Arrange: Create a file and make it read-only (simulates permission error)
        val readOnlyFile = File(cacheDir, "meal_readonly.jpg")
        readOnlyFile.createNewFile()
        readOnlyFile.setLastModified(System.currentTimeMillis() - (25 * 60 * 60 * 1000L))
        readOnlyFile.setReadOnly()
        
        // Act: Run the cleanup worker
        val worker = TestListenableWorkerBuilder<PhotoCleanupWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        
        val result = worker.doWork()
        
        // Assert: Worker succeeds even if file deletion fails
        assertEquals("Worker should succeed despite deletion failure", 
            ListenableWorker.Result.success(), result)
        
        // Cleanup: Reset permissions for tearDown
        readOnlyFile.setWritable(true)
    }
}
