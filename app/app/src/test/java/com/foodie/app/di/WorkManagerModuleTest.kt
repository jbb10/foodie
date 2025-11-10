package com.foodie.app.di

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WorkManagerModule.
 *
 * Verifies DI configuration provides:
 * - WorkManager Configuration
 * - WorkManager instance
 *
 * Note: These tests verify that the module provides the correct types
 * but don't test the actual WorkManager initialization (requires instrumentation tests).
 */
class WorkManagerModuleTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
    }

    @Test
    fun module_hasProvideWorkManagerConfigurationMethod() {
        // This test verifies the module has the required provider method
        // Actual Configuration creation requires application context (instrumentation test)
        val methods = WorkManagerModule::class.java.declaredMethods
        val configMethod = methods.find { it.name == "provideWorkManagerConfiguration" }
        
        assertThat(configMethod).isNotNull()
        assertThat(configMethod?.parameterTypes?.size).isEqualTo(1)
    }

    @Test
    fun module_hasProvideWorkManagerMethod() {
        // This test verifies the module has the required provider method
        val methods = WorkManagerModule::class.java.declaredMethods
        val workManagerMethod = methods.find { it.name == "provideWorkManager" }
        
        assertThat(workManagerMethod).isNotNull()
        assertThat(workManagerMethod?.parameterTypes?.size).isEqualTo(1)
    }
}
