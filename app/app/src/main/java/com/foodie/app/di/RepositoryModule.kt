package com.foodie.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository module providing repository implementations.
 * Repositories will be added in subsequent stories.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repository bindings will be added here in future stories
    // Example: @Binds fun bindMealRepository(impl: MealRepositoryImpl): MealRepository
}
