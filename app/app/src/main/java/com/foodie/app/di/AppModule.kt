package com.foodie.app.di

import com.foodie.app.data.network.NetworkMonitor
import com.foodie.app.data.network.NetworkMonitorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
}

