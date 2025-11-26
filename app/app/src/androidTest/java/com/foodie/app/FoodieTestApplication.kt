package com.foodie.app

import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.CustomTestApplication
import timber.log.Timber

/**
 * Base application for custom test application.
 */
open class BaseTestApplication : MultiDexApplication(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for test logging
        Timber.plant(Timber.DebugTree())
        Timber.d("FoodieTestApplication initialized")

        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .build()
}

/**
 * Custom test application that supports both Hilt and WorkManager.
 */
@CustomTestApplication(BaseTestApplication::class)
interface FoodieTestApplication
