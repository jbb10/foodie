package com.foodie.app

import org.junit.Test
import org.junit.Assert.*

/**
 * Build configuration tests to verify project setup.
 * These tests validate that the build is configured correctly per AC requirements.
 */
class BuildConfigurationTest {

    @Test
    fun `build configuration should use correct package name`() {
        // Verify the package is com.foodie.app not com.example.foodie
        val packageName = BuildConfig.APPLICATION_ID
        assertEquals("com.foodie.app", packageName)
    }

    @Test
    fun `build configuration should have correct version`() {
        // Verify version code and name are set
        assertEquals(1, BuildConfig.VERSION_CODE)
        assertEquals("1.0", BuildConfig.VERSION_NAME)
    }
}
