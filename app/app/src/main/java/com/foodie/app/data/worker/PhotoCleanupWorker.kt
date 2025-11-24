package com.foodie.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.foodie.app.data.local.cache.PhotoManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for cleaning up stale photos from the cache directory.
 *
 * Purpose:
 * - Delete photos older than 24 hours to prevent indefinite storage accumulation
 * - Run periodically (daily) via WorkManager PeriodicWorkRequest
 * - Complement photo deletion logic in AnalyzeMealWorker (immediate cleanup)
 *
 * Cleanup Strategy:
 * - Photos deleted immediately: Success, non-retryable errors (NoFoodDetected)
 * - Photos retained: Retryable errors (network failures, timeouts)
 * - This worker: Cleanup abandoned/forgotten photos after 24-hour grace period
 *
 * Architecture:
 * - @HiltWorker enables constructor dependency injection
 * - CoroutineWorker provides suspend function support
 * - Runs in background (WorkManager constraints: device idle)
 * - Scheduled in FoodieApplication.onCreate()
 *
 * Usage:
 * ```
 * val workRequest = PeriodicWorkRequestBuilder<PhotoCleanupWorker>(24, TimeUnit.HOURS)
 *     .setConstraints(Constraints.Builder()
 *         .setRequiresDeviceIdle(true)
 *         .build())
 *     .setInitialDelay(calculateDelayUntil3AM(), TimeUnit.SECONDS)
 *     .build()
 * workManager.enqueueUniquePeriodicWork(
 *     "photo_cleanup_periodic",
 *     ExistingPeriodicWorkPolicy.KEEP,
 *     workRequest
 * )
 * ```
 */
@HiltWorker
class PhotoCleanupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoManager: PhotoManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PhotoCleanupWorker"
        
        /**
         * Photos older than this threshold will be deleted.
         * 24 hours = 86,400,000 milliseconds
         */
        private const val MAX_PHOTO_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Performs periodic cleanup of stale photos.
     *
     * Flow:
     * 1. Get cache directory from PhotoManager
     * 2. Iterate all files in directory
     * 3. Calculate age of each file (current time - lastModified)
     * 4. Delete files older than 24 hours
     * 5. Track deletion metrics (count, total size)
     * 6. Log cleanup summary
     *
     * Error Handling:
     * - Individual file deletion failures logged but don't abort cleanup
     * - SecurityException, IOException caught and logged
     * - Always returns Result.success() (fail-soft approach)
     *
     * @return Result.success() with cleanup metrics, even if partial failures occur
     */
    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Timber.tag(TAG).i("Photo cleanup started - checking cache directory")

        val cacheDir = photoManager.getCacheDir()
        
        if (!cacheDir.exists()) {
            Timber.tag(TAG).d("Cache directory does not exist, nothing to clean up")
            return Result.success()
        }

        val files = cacheDir.listFiles() ?: emptyArray()
        
        if (files.isEmpty()) {
            Timber.tag(TAG).d("Cache directory is empty, nothing to clean up")
            return Result.success()
        }

        var deletedCount = 0
        var deletedSizeBytes = 0L
        var retainedCount = 0
        var errorCount = 0

        for (file in files) {
            try {
                val age = startTime - file.lastModified()
                val ageHours = age / (1000 * 60 * 60)
                
                if (age > MAX_PHOTO_AGE_MS) {
                    // Photo is older than 24 hours - delete it
                    val fileSize = file.length()
                    
                    if (file.delete()) {
                        deletedCount++
                        deletedSizeBytes += fileSize
                        Timber.tag(TAG).d(
                            "Deleted photo: ${file.name} (age: ${ageHours}h, size: ${fileSize / 1024}KB)"
                        )
                    } else {
                        // Deletion failed but don't abort cleanup
                        errorCount++
                        Timber.tag(TAG).w(
                            "Failed to delete photo: ${file.name} (age: ${ageHours}h)"
                        )
                    }
                } else {
                    // Photo is younger than 24 hours - retain for potential retry
                    retainedCount++
                    Timber.tag(TAG).d(
                        "Retained photo: ${file.name} (age: ${ageHours}h)"
                    )
                }
            } catch (e: SecurityException) {
                // Permission denied - log and continue
                errorCount++
                Timber.tag(TAG).w(e, "Permission denied deleting photo: ${file.name}")
            } catch (e: Exception) {
                // Unexpected error - log and continue
                errorCount++
                Timber.tag(TAG).w(e, "Error processing photo: ${file.name}")
            }
        }

        // Calculate remaining cache usage
        val remainingFiles = cacheDir.listFiles() ?: emptyArray()
        val remainingSizeBytes = remainingFiles.sumOf { it.length() }

        val duration = System.currentTimeMillis() - startTime
        val deletedSizeMB = deletedSizeBytes / (1024.0 * 1024.0)
        val remainingSizeMB = remainingSizeBytes / (1024.0 * 1024.0)

        Timber.tag(TAG).i(
            "Photo cleanup complete in ${duration}ms - " +
            "deleted $deletedCount files (${String.format("%.2f", deletedSizeMB)}MB), " +
            "retained $retainedCount files, " +
            "errors: $errorCount"
        )

        Timber.tag(TAG).i(
            "Current cache size: ${String.format("%.2f", remainingSizeMB)}MB, " +
            "${remainingFiles.size} photos"
        )

        return Result.success()
    }
}
