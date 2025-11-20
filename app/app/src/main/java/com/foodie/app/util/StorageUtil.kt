package com.foodie.app.util

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for checking device storage availability.
 *
 * Provides methods to check if sufficient storage space is available
 * for photo capture and processing. Uses StatFs to query filesystem
 * statistics for the cache directory.
 *
 * Threshold:
 * - 10MB minimum for photo operations
 * - Based on 2MP JPEG at 80% quality ≈ 500KB-1MB per photo
 * - Buffer allows for multiple photos + temporary processing files
 *
 * Story: 4.6 - Graceful Degradation (AC#4)
 *
 * @param context Application context for accessing cache directory
 */
@Singleton
open class StorageUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "StorageUtil"
        private const val BYTES_PER_MB = 1024 * 1024
        
        /**
         * Minimum storage threshold in MB.
         *
         * Based on:
         * - 2MP JPEG at 80% quality ≈ 500KB-1MB per photo
         * - Buffer for multiple photos (5-10 photos)
         * - Temp files during processing
         */
        const val MINIMUM_STORAGE_MB = 10
    }
    
    /**
     * Checks available storage space in the cache directory.
     *
     * Uses StatFs to query filesystem statistics. Returns available
     * bytes (not total capacity - only free space).
     *
     * Performance: < 10ms (filesystem stat call)
     *
     * @return Available storage space in megabytes
     */
    open fun checkAvailableStorageMB(): Long {
        return try {
            val cacheDir = context.cacheDir
            val stat = StatFs(cacheDir.path)
            val availableBytes = stat.availableBytes
            val availableMB = availableBytes / BYTES_PER_MB
            
            Timber.tag(TAG).d("Available storage: ${availableMB}MB (${availableBytes} bytes)")
            availableMB
        } catch (e: Exception) {
            // StatFs can throw exceptions on rare filesystem errors
            Timber.tag(TAG).e(e, "Failed to check available storage")
            // Return 0 to trigger storage full error (safe failure mode)
            0L
        }
    }
    
    /**
     * Checks if sufficient storage space is available for photo operations.
     *
     * Compares available storage against minimum threshold (default 10MB).
     * Can be customized with different threshold for special cases.
     *
     * @param minimumMB Minimum required storage in MB (default: 10MB)
     * @return true if available storage >= minimumMB, false otherwise
     */
    fun hasEnoughStorage(minimumMB: Int = MINIMUM_STORAGE_MB): Boolean {
        val availableMB = checkAvailableStorageMB()
        val hasEnough = availableMB >= minimumMB
        
        if (!hasEnough) {
            Timber.tag(TAG).w("Insufficient storage: ${availableMB}MB < ${minimumMB}MB required")
        }
        
        return hasEnough
    }
}
