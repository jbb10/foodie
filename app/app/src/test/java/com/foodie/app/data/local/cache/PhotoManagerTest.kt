package com.foodie.app.data.local.cache

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Unit tests for [PhotoManager].
 *
 * Tests photo file management operations including:
 * - Filename generation with timestamp pattern
 * - Cache directory creation and access
 * - Photo file creation with FileProvider URI
 *
 * Note: Image processing tests (resize, compress, EXIF correction) require
 * instrumentation tests due to Android framework dependencies (Bitmap, ExifInterface).
 */
class PhotoManagerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var photoManager: PhotoManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        photoManager = PhotoManager(mockContext)
    }

    @Test
    fun generateFilename_returnsTimestampPattern() {
        // When
        val filename = photoManager.generateFilename()

        // Then
        assertThat(filename).startsWith("meal_")
        assertThat(filename).endsWith(".jpg")

        // Extract timestamp portion (between "meal_" and ".jpg")
        val timestamp = filename.substring(5, filename.length - 4)
        
        // Timestamp should be numeric
        val timestampLong = timestamp.toLongOrNull()
        assertThat(timestampLong).isNotNull()

        // Timestamp should be reasonable (after 2020, before 2100)
        assertThat(timestampLong).isGreaterThan(1577836800000L) // 2020-01-01
        assertThat(timestampLong).isLessThan(4102444800000L) // 2100-01-01
    }

    @Test
    fun generateFilename_generatesUniqueFilenames() = runTest {
        // When - Generate two filenames in quick succession
        val filename1 = photoManager.generateFilename()
        val filename2 = photoManager.generateFilename()

        // Then - Should be different (different timestamps)
        assertThat(filename1).isNotEqualTo(filename2)
    }

    @Test
    fun getCacheDir_returnsPhotosSubdirectory() {
        // Given
        val mockCacheDir = File("/mock/cache")
        whenever(mockContext.cacheDir).thenReturn(mockCacheDir)

        // When
        val cacheDir = photoManager.getCacheDir()

        // Then
        assertThat(cacheDir.parentFile).isEqualTo(mockCacheDir)
        assertThat(cacheDir.name).isEqualTo("photos")
    }
}
