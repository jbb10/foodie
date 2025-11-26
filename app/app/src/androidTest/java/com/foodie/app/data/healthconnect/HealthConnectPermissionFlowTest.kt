package com.foodie.app.data.healthconnect

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for Health Connect permission request flow.
 *
 * Tests verify that permission request mechanisms work correctly with the Android system.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthConnectPermissionFlowTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var healthConnectManager: HealthConnectManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun permissionRequestContract_isCreatedSuccessfully() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = HealthConnectManager(context)

        // When
        val contract = manager.createPermissionRequestContract()

        // Then
        assertThat(contract).isNotNull()
    }

    @Test
    fun checkPermissions_executesWithoutError() = runTest {
        // Given - Injected HealthConnectManager

        // When
        val hasPermissions = healthConnectManager.checkPermissions()

        // Then - Method completes successfully
        assertThat(hasPermissions).isNotNull()
    }

    @Test
    fun requiredPermissions_containsCorrectPermissions() {
        // When
        val permissions = HealthConnectManager.REQUIRED_PERMISSIONS

        // Then
        assertThat(permissions).containsExactly(
            "android.permission.health.READ_NUTRITION",
            "android.permission.health.WRITE_NUTRITION",
            "android.permission.health.READ_WEIGHT",
            "android.permission.health.WRITE_WEIGHT",
            "android.permission.health.READ_HEIGHT",
            "android.permission.health.WRITE_HEIGHT",
        )
    }
}
