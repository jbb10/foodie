package com.foodie.app.di

import com.foodie.app.data.local.healthconnect.HealthConnectDataSource
import com.foodie.app.data.local.healthconnect.HealthConnectDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Data source module providing data source implementations.
 *
 * Data sources are the lowest level of the data layer, handling direct
 * interactions with external systems (Health Connect, APIs, databases, etc.).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    /**
     * Binds HealthConnectDataSource interface to its implementation.
     *
     * @Singleton ensures only one instance interacts with Health Connect.
     */
    @Binds
    @Singleton
    abstract fun bindHealthConnectDataSource(
        impl: HealthConnectDataSourceImpl,
    ): HealthConnectDataSource
}
