package com.foodie.app.data.local.cache

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for PhotoManager cache statistics methods.
 *
 * Tests verify:
 * - getCacheStats returns accurate size and count
 * - Oldest photo age calculated correctly
 * - Empty cache returns zero stats
 *
 * Story 4.4: Photo Retention and Cleanup - Task 6
 */
@HiltAndroidTest
class PhotoManagerCacheStatsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var photoManager: PhotoManager

    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        hiltRule.inject()
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
    fun testGetCacheStats_emptyCache_returnsZeroStats() = runBlocking {
        // Arrange: Cache is empty (setUp cleaned it)

        // Act
        val stats = photoManager.getCacheStats()

        // Assert
        assertEquals(0L, stats.totalSizeBytes)
        assertEquals(0, stats.photoCount)
        assertNull(stats.oldestPhotoAgeHours)
    }

    @Test
    fun testGetCacheStats_singlePhoto_returnsCorrectStats() = runBlocking {
        // Arrange: Create one photo file
        val photo = File(cacheDir, "meal_single.jpg")
        photo.createNewFile()
        photo.writeText("test content 12345") // 18 bytes (writeText adds newline on some platforms)

        val fiveHoursAgo = System.currentTimeMillis() - (5 * 60 * 60 * 1000L)
        photo.setLastModified(fiveHoursAgo)

        // Act
        val stats = photoManager.getCacheStats()

        // Assert
        assertEquals(18L, stats.totalSizeBytes)
        assertEquals(1, stats.photoCount)
        assertEquals(5, stats.oldestPhotoAgeHours)
    }

    @Test
    fun testGetCacheStats_multiplePhotos_calculatesCorrectTotals() = runBlocking {
        // Arrange: Create multiple photos with different sizes and ages
        val photo1 = File(cacheDir, "meal_1.jpg")
        photo1.createNewFile()
        photo1.writeText("content1") // 8 bytes
        photo1.setLastModified(System.currentTimeMillis() - (10 * 60 * 60 * 1000L)) // 10h old

        val photo2 = File(cacheDir, "meal_2.jpg")
        photo2.createNewFile()
        photo2.writeText("content22") // 9 bytes
        photo2.setLastModified(System.currentTimeMillis() - (20 * 60 * 60 * 1000L)) // 20h old (oldest)

        val photo3 = File(cacheDir, "meal_3.jpg")
        photo3.createNewFile()
        photo3.writeText("content333") // 10 bytes
        photo3.setLastModified(System.currentTimeMillis() - (5 * 60 * 60 * 1000L)) // 5h old

        // Act
        val stats = photoManager.getCacheStats()

        // Assert
        assertEquals(27L, stats.totalSizeBytes) // 8 + 9 + 10
        assertEquals(3, stats.photoCount)
        assertEquals(20, stats.oldestPhotoAgeHours) // photo2 is oldest
    }

    @Test
    fun testGetCacheStats_largeFiles_calculatesCorrectSize() = runBlocking {
        // Arrange: Create larger photo files
        val photo1 = File(cacheDir, "meal_large1.jpg")
        photo1.createNewFile()
        photo1.writeText("X".repeat(1024 * 50)) // 50 KB

        val photo2 = File(cacheDir, "meal_large2.jpg")
        photo2.createNewFile()
        photo2.writeText("Y".repeat(1024 * 100)) // 100 KB

        // Act
        val stats = photoManager.getCacheStats()

        // Assert
        assertEquals(1024L * 150, stats.totalSizeBytes) // 150 KB total
        assertEquals(2, stats.photoCount)
    }

    @Test
    fun testGetCacheStats_freshPhoto_returnsZeroAge() = runBlocking {
        // Arrange: Create photo with current timestamp
        val photo = File(cacheDir, "meal_fresh.jpg")
        photo.createNewFile()
        photo.writeText("fresh content")
        // lastModified is ~now (just created)

        // Act
        val stats = photoManager.getCacheStats()

        // Assert
        assertEquals(1, stats.photoCount)
        // Age should be 0 hours (created just now, integer division truncates)
        assertEquals(0, stats.oldestPhotoAgeHours)
    }
}
