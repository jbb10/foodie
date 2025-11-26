package com.foodie.app.di

import com.foodie.app.data.repository.EnergyBalanceRepositoryImpl
import com.foodie.app.data.repository.MealRepositoryImpl
import com.foodie.app.data.repository.NutritionAnalysisRepositoryImpl
import com.foodie.app.data.repository.UserProfileRepositoryImpl
import com.foodie.app.domain.repository.EnergyBalanceRepository
import com.foodie.app.domain.repository.MealRepository
import com.foodie.app.domain.repository.NutritionAnalysisRepository
import com.foodie.app.domain.repository.UserProfileRepository
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

    /**
     * Binds NutritionAnalysisRepository interface to its Azure OpenAI implementation.
     *
     * @Singleton ensures only one instance exists throughout the app lifecycle.
     */
    @Binds
    @Singleton
    abstract fun bindNutritionAnalysisRepository(impl: NutritionAnalysisRepositoryImpl): NutritionAnalysisRepository

    /**
     * Binds UserProfileRepository interface to its implementation.
     *
     * Manages user demographic profile for BMR calculation across Health Connect
     * (weight/height) and SharedPreferences (sex/age).
     *
     * @Singleton ensures only one instance exists throughout the app lifecycle.
     */
    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    /**
     * Binds EnergyBalanceRepository interface to its implementation.
     *
     * Provides BMR (Basal Metabolic Rate) calculations using the Mifflin-St Jeor equation.
     * Depends on UserProfileRepository for user demographic data.
     *
     * @Singleton ensures only one instance exists throughout the app lifecycle.
     */
    @Binds
    @Singleton
    abstract fun bindEnergyBalanceRepository(impl: EnergyBalanceRepositoryImpl): EnergyBalanceRepository
}
