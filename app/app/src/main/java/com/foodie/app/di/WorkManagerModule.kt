package com.foodie.app.di

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.foodie.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for WorkManager configuration.
 *
 * Provides WorkManager Configuration with HiltWorkerFactory for dependency injection
 * into Worker classes. This enables @HiltWorker annotation for automatic DI in workers.
 *
 * Architecture:
 * - WorkManager uses HiltWorkerFactory to create worker instances
 * - @HiltWorker annotation on workers enables constructor injection
 * - Configuration controls logging level (DEBUG in debug builds, INFO in release)
 *
 * Note: FoodieApplication must implement Configuration.Provider and use the
 * Configuration provided by this module.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provides WorkManager Configuration for the application.
     *
     * Configuration:
     * - Uses HiltWorkerFactory for dependency injection
     * - DEBUG builds: Log.DEBUG level for detailed worker logs
     * - RELEASE builds: Log.INFO level to reduce noise
     *
     * @param workerFactory HiltWorkerFactory injected by Hilt
     * @return Configured WorkManager Configuration
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory,
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()
    }

    /**
     * Provides WorkManager instance for the application.
     *
     * @param context Application context
     * @return WorkManager instance
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
