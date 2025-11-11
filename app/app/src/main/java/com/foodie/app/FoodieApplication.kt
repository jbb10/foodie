package com.foodie.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.foodie.app.data.worker.foreground.MealAnalysisNotificationSpec
import com.foodie.app.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for the Foodie app.
 *
 * Responsibilities:
 * - Initialize Timber logging (DebugTree for debug builds, ReleaseTree for release)
 * - Hilt dependency injection initialization (via @HiltAndroidApp)
 * - WorkManager configuration with HiltWorkerFactory for dependency injection
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
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
