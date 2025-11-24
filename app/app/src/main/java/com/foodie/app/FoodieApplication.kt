package com.foodie.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.foodie.app.data.local.preferences.SecurePreferences
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
 * - Migrate BuildConfig credentials to EncryptedSharedPreferences (Story 5.2, one-time)
 *
 * WorkManager uses HiltWorkerFactory (injected by Hilt)
 * for dependency injection into workers (@HiltWorker annotation).
 */
@HiltAndroidApp
class FoodieApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var securePreferences: SecurePreferences

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

        // Migrate BuildConfig credentials to EncryptedSharedPreferences (Story 5.2)
        migrateCredentialsIfNeeded()

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

    /**
     * Migrates BuildConfig credentials to EncryptedSharedPreferences.
     *
     * One-time migration from Story 2.4 (BuildConfig) to Story 5.2 (EncryptedSharedPreferences).
     * Runs on first launch after Story 5.2 deployment, then sets flag to prevent re-runs.
     *
     * Migration strategy:
     * - Check if migration already completed via SharedPreferences flag
     * - If BuildConfig values exist, migrate to SecurePreferences + SharedPreferences
     * - Set migration flag to true (prevents repeated attempts)
     * - On error: Log and skip (user can configure manually via Settings)
     *
     * Story 5.2: Azure OpenAI API Key and Endpoint Configuration
     */
    private fun migrateCredentialsIfNeeded() {
        val prefs = getSharedPreferences("foodie_prefs", MODE_PRIVATE)
        val migrated = prefs.getBoolean("credentials_migrated", false)

        if (migrated) {
            Timber.d("Credentials already migrated, skipping")
            return
        }

        try {
            val apiKey = BuildConfig.AZURE_OPENAI_API_KEY
            val endpoint = BuildConfig.AZURE_OPENAI_ENDPOINT
            val model = BuildConfig.AZURE_OPENAI_MODEL

            if (apiKey.isNotBlank() && apiKey != "\"\"") {
                // Migrate API key to EncryptedSharedPreferences
                securePreferences.setAzureOpenAiApiKey(apiKey)

                // Migrate endpoint and model to standard SharedPreferences
                prefs.edit()
                    .putString("pref_azure_endpoint", endpoint)
                    .putString("pref_azure_model", model)
                    .putBoolean("credentials_migrated", true)
                    .apply()

                Timber.i("Credentials migrated successfully from BuildConfig to EncryptedSharedPreferences")
            } else {
                // Mark migration complete even if BuildConfig empty (prevents repeated attempts)
                prefs.edit().putBoolean("credentials_migrated", true).apply()
                Timber.d("No BuildConfig credentials to migrate")
            }
        } catch (e: Exception) {
            Timber.e(e, "Credential migration failed - user must configure manually via Settings")
            // Don't set migration flag on failure - retry next launch
        }
    }
}

