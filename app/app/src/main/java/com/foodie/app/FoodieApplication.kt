package com.foodie.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.foodie.app.data.worker.PhotoCleanupWorker
import com.foodie.app.data.worker.foreground.MealAnalysisNotificationSpec
import com.foodie.app.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application class for the Foodie app.
 *
 * Responsibilities:
 * - Initialize Timber logging (DebugTree for debug builds, ReleaseTree for release)
 * - Hilt dependency injection initialization (via @HiltAndroidApp)
 * - WorkManager configuration with HiltWorkerFactory for dependency injection
 * - Schedule periodic background tasks (photo cleanup)
 *
 * WorkManager uses HiltWorkerFactory (injected by Hilt)
 * for dependency injection into workers (@HiltWorker annotation).
 */
@HiltAndroidApp
class FoodieApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("FoodieApplication initialized with HiltWorkerFactory")

        MealAnalysisNotificationSpec.ensureChannel(this)
        
        // Schedule periodic photo cleanup (Story 4.4)
        schedulePhotoCleanup()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    /**
     * Schedules periodic photo cleanup task.
     *
     * Cleanup runs daily at 3am (low-usage time) to delete photos older than 24 hours.
     * Uses KEEP policy to prevent duplicate schedules across app restarts.
     *
     * Constraints:
     * - Device idle: Ensures cleanup runs when device not actively used
     *
     * Story 4.4: Photo Retention and Cleanup
     */
    private fun schedulePhotoCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(true)
            .build()
        
        // Calculate initial delay to align first run with 3am
        val initialDelaySeconds = calculateDelayUntil3AM()
        
        val cleanupRequest = PeriodicWorkRequestBuilder<PhotoCleanupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "photo_cleanup_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
        
        Timber.d(
            "Photo cleanup scheduled: daily at 3am " +
            "(initial delay: ${initialDelaySeconds / 3600}h ${(initialDelaySeconds % 3600) / 60}m)"
        )
    }
    
    /**
     * Calculates seconds until next 3am occurrence.
     *
     * If current time is before 3am today, returns delay until 3am today.
     * If current time is after 3am today, returns delay until 3am tomorrow.
     *
     * @return Delay in seconds until next 3am
     */
    private fun calculateDelayUntil3AM(): Long {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(3, 0) // 3:00 AM
        
        var next3AM = now.with(targetTime)
        
        // If 3am today has passed, schedule for 3am tomorrow
        if (now.isAfter(next3AM)) {
            next3AM = next3AM.plusDays(1)
        }
        
        return Duration.between(now, next3AM).seconds
    }
}

