package com.foodie.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level dependency module.
 * Provides application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Application-level dependencies will be provided here
    // Examples: Analytics, Crash reporting, etc.
}
