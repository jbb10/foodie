package com.foodie.app.data.local.healthconnect

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for HealthConnectManager constants and validation.
 *
 * Note: Actual Health Connect SDK behavior is tested in HealthConnectIntegrationTest.
 * Unit tests for business logic with mocked HealthConnectManager are in HealthConnectRepositoryTest.
 *
 * This test class validates:
 * - Required permissions configuration
 * - Input validation logic (tested at repository level via HealthConnectRepositoryTest)
 *
 * See HealthConnectIntegrationTest for:
 * - Real insertNutritionRecord() behavior with Health Connect SDK
 * - NutritionRecord field creation (energy, name, timestamps, zone offsets)
 * - Time zone handling validation
 * - Record ID extraction
 * - SecurityException handling
 */
class HealthConnectManagerTest {

    @Test
    fun `REQUIRED_PERMISSIONS contains READ_NUTRITION and WRITE_NUTRITION`() {
        // When
        val permissions = HealthConnectManager.REQUIRED_PERMISSIONS

        // Then
        assertThat(permissions).hasSize(8)
        assertThat(permissions).contains("android.permission.health.READ_NUTRITION")
        assertThat(permissions).contains("android.permission.health.WRITE_NUTRITION")
        assertThat(permissions).contains("android.permission.health.READ_WEIGHT")
        assertThat(permissions).contains("android.permission.health.WRITE_WEIGHT")
        assertThat(permissions).contains("android.permission.health.READ_HEIGHT")
        assertThat(permissions).contains("android.permission.health.WRITE_HEIGHT")
        assertThat(permissions).contains("android.permission.health.READ_STEPS")
        assertThat(permissions).contains("android.permission.health.READ_ACTIVE_CALORIES_BURNED")
    }

    @Test
    fun `REQUIRED_PERMISSIONS contains correct number of permissions`() {
        // Then
        assertThat(HealthConnectManager.REQUIRED_PERMISSIONS).isNotEmpty()
        assertThat(HealthConnectManager.REQUIRED_PERMISSIONS.size).isEqualTo(8)
    }

    @Test
    fun `REQUIRED_PERMISSIONS format is correct`() {
        // Then - All permissions start with android.permission.health
        HealthConnectManager.REQUIRED_PERMISSIONS.forEach { permission ->
            assertThat(permission).startsWith("android.permission.health.")
        }
    }
}
