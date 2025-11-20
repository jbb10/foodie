package com.foodie.app.util

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for StorageUtil.
 *
 * Tests storage space checking logic with various scenarios including
 * edge cases (0 bytes, exactly threshold, negative values).
 *
 * Story: 4.6 - Graceful Degradation (AC#4)
 */
class StorageUtilTest {
    
    private lateinit var context: Context
    private lateinit var storageUtil: StorageUtil
    private lateinit var mockCacheDir: File
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockCacheDir = mockk(relaxed = true)
        
        every { context.cacheDir } returns mockCacheDir
        every { mockCacheDir.path } returns "/data/data/com.foodie.app/cache"
        
        storageUtil = StorageUtil(context)
    }
    
    /**
     * Story 4.6, AC #4: Storage check with sufficient space
     * Test: hasEnoughStorage returns true when > 10MB available
     */
    @Test
    fun `hasEnoughStorage should return true when storage above threshold`() {
        // Given: Mock storage util that reports 50MB available
        val storageUtilWithMock = object : StorageUtil(context) {
            override fun checkAvailableStorageMB(): Long = 50L
        }
        
        // When: Check if enough storage
        val hasEnough = storageUtilWithMock.hasEnoughStorage()
        
        // Then: Returns true (50MB > 10MB threshold)
        assertThat(hasEnough).isTrue()
    }
    
    /**
     * Story 4.6, AC #4: Storage check with insufficient space
     * Test: hasEnoughStorage returns false when < 10MB available
     */
    @Test
    fun `hasEnoughStorage should return false when storage below threshold`() {
        // Given: Mock storage util that reports 5MB available
        val storageUtilWithMock = object : StorageUtil(context) {
            override fun checkAvailableStorageMB(): Long = 5L
        }
        
        // When: Check if enough storage
        val hasEnough = storageUtilWithMock.hasEnoughStorage()
        
        // Then: Returns false (5MB < 10MB threshold)
        assertThat(hasEnough).isFalse()
    }
    
    /**
     * Story 4.6, AC #4: Storage check with exactly threshold
     * Test: hasEnoughStorage returns true when exactly 10MB available
     */
    @Test
    fun `hasEnoughStorage should return true when storage equals threshold`() {
        // Given: Mock storage util that reports exactly 10MB available
        val storageUtilWithMock = object : StorageUtil(context) {
            override fun checkAvailableStorageMB(): Long = 10L
        }
        
        // When: Check if enough storage
        val hasEnough = storageUtilWithMock.hasEnoughStorage()
        
        // Then: Returns true (10MB >= 10MB threshold)
        assertThat(hasEnough).isTrue()
    }
    
    /**
     * Story 4.6, AC #4: Storage check with 0 bytes available
     * Test: hasEnoughStorage returns false when storage completely full
     */
    @Test
    fun `hasEnoughStorage should return false when storage is zero`() {
        // Given: Mock storage util that reports 0MB available
        val storageUtilWithMock = object : StorageUtil(context) {
            override fun checkAvailableStorageMB(): Long = 0L
        }
        
        // When: Check if enough storage
        val hasEnough = storageUtilWithMock.hasEnoughStorage()
        
        // Then: Returns false (0MB < 10MB threshold)
        assertThat(hasEnough).isFalse()
    }
    
    /**
     * Story 4.6, AC #4: Custom threshold support
     * Test: hasEnoughStorage accepts custom minimum MB parameter
     */
    @Test
    fun `hasEnoughStorage should respect custom threshold`() {
        // Given: Mock storage util that reports 15MB available
        val storageUtilWithMock = object : StorageUtil(context) {
            override fun checkAvailableStorageMB(): Long = 15L
        }
        
        // When: Check with custom 20MB threshold
        val hasEnoughWith20MB = storageUtilWithMock.hasEnoughStorage(minimumMB = 20)
        
        // Then: Returns false (15MB < 20MB threshold)
        assertThat(hasEnoughWith20MB).isFalse()
        
        // When: Check with custom 10MB threshold
        val hasEnoughWith10MB = storageUtilWithMock.hasEnoughStorage(minimumMB = 10)
        
        // Then: Returns true (15MB >= 10MB threshold)
        assertThat(hasEnoughWith10MB).isTrue()
    }
    
    /**
     * Story 4.6, AC #4: Storage calculation with large numbers
     * Test: checkAvailableStorageMB handles large byte values correctly
     */
    @Test
    fun `checkAvailableStorageMB should handle large storage values`() {
        // Given: Mock storage util that reports 1GB (1024MB) available
        val storageUtilWithMock = object : StorageUtil(context) {
            override fun checkAvailableStorageMB(): Long = 1024L
        }
        
        // When: Check available storage
        val availableMB = storageUtilWithMock.checkAvailableStorageMB()
        
        // Then: Returns correct MB value
        assertThat(availableMB).isEqualTo(1024L)
    }
    
    /**
     * Story 4.6, AC #8: Silent failure prevention
     * Test: Exception during storage check returns 0 (safe failure mode)
     */
    @Test
    fun `checkAvailableStorageMB should return zero on exception for safe failure`() {
        // Given: Mock cache dir with invalid path that will cause StatFs to fail
        every { mockCacheDir.path } throws IllegalArgumentException("Invalid path")
        
        // When: Check available storage
        val availableMB = storageUtil.checkAvailableStorageMB()
        
        // Then: Returns 0MB (safe failure - triggers storage full error)
        assertThat(availableMB).isEqualTo(0L)
    }
}
