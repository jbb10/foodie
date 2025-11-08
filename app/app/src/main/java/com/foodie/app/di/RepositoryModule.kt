package com.foodie.app.di

import com.foodie.app.data.repository.MealRepositoryImpl
import com.foodie.app.domain.repository.MealRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository module providing repository implementations.
 *
 * Uses @Binds to map repository interfaces to their implementations.
 * All repositories are scoped as Singletons to ensure single source of truth.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Binds MealRepository interface to its Health Connect implementation.
     *
     * @Singleton ensures only one instance exists throughout the app lifecycle.
     */
    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository
}
