package com.foodie.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * WorkManager module for background processing.
 * WorkManager dependencies will be provided here in future stories.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    // WorkManager dependencies will be added here
    // Example: HiltWorkerFactory configuration
}
