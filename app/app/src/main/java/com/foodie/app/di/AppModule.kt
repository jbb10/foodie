package com.foodie.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import com.foodie.app.data.network.NetworkMonitor
import com.foodie.app.data.network.NetworkMonitorImpl
import com.foodie.app.data.repository.PreferencesRepository
import com.foodie.app.data.repository.PreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level dependency module.
 * Provides application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    /**
     * Binds NetworkMonitor interface to NetworkMonitorImpl.
     *
     * NetworkMonitor is a singleton that monitors network connectivity in real-time.
     * Used by repositories and background workers for pre-flight connectivity checks.
     *
     * Story: 4.1 - Network & Error Handling Infrastructure
     */
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: NetworkMonitorImpl): NetworkMonitor

    /**
     * Binds PreferencesRepository interface to PreferencesRepositoryImpl.
     *
     * PreferencesRepository provides reactive access to SharedPreferences.
     * Used by SettingsViewModel for preference CRUD and observation.
     *
     * Story: 5.1 - Settings Screen Foundation
     */
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    /**
     * Binds WorkerFactory interface to HiltWorkerFactory.
     *
     * HiltWorkerFactory enables Hilt dependency injection in Workers.
     * Automatically provided by Hilt Work library.
     * This binding allows tests to inject WorkerFactory.
     *
     * NOTE: Added to fix PhotoCleanupWorkerTest from Epic 4, not part of Story 5.1 scope.
     * See Story 4.4 for context on Worker testing infrastructure.
     */
    @Binds
    @Singleton
    abstract fun bindWorkerFactory(impl: HiltWorkerFactory): WorkerFactory

    companion object {
        /**
         * Provides SharedPreferences singleton.
         *
         * Uses "foodie_prefs" as preference file name for all app preferences.
         * MODE_PRIVATE ensures preferences are only accessible to this app.
         *
         * Note: Standard SharedPreferences for non-sensitive data (Story 5.1).
         * Story 5.2 will introduce EncryptedSharedPreferences for API key storage.
         *
         * Story: 5.1 - Settings Screen Foundation
         */
        @Provides
        @Singleton
        fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
            return context.getSharedPreferences("foodie_prefs", Context.MODE_PRIVATE)
        }
    }
}

