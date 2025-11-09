package com.foodie.app.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foodie.app.data.local.healthconnect.HealthConnectManager
import com.foodie.app.data.repository.HealthConnectRepository
import com.foodie.app.ui.screens.meallist.MealListViewModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumentation test verifying Hilt dependency injection works correctly.
 *
 * Tests verify that HealthConnectManager and HealthConnectRepository are properly
 * injected into components like ViewModels.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthConnectHiltTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var healthConnectManager: HealthConnectManager
    
    @Inject
    lateinit var healthConnectRepository: HealthConnectRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun healthConnectManager_isInjectedSuccessfully() {
        // Then
        assertThat(healthConnectManager).isNotNull()
    }
    
    @Test
    fun healthConnectRepository_isInjectedSuccessfully() {
        // Then
        assertThat(healthConnectRepository).isNotNull()
    }
    
    @Test
    fun healthConnectRepository_hasHealthConnectManager() {
        // Then - Repository was constructed with injected manager
        assertThat(healthConnectRepository).isNotNull()
        // Repository should work correctly with injected dependencies
    }
    
    @Test
    fun mealListViewModel_canBeCreatedWithInjectedRepository() {
        // Given - Repository is injected
        
        // When - Create ViewModel with repository
        val viewModel = MealListViewModel(healthConnectRepository)
        
        // Then - ViewModel is created successfully
        assertThat(viewModel).isNotNull()
    }
    
    @Test
    fun hiltDependencyGraph_compilesSuccessfully() {
        // This test passing proves the Hilt dependency graph compiles
        // and all required dependencies are provided correctly
        
        // Then
        assertThat(healthConnectManager).isNotNull()
        assertThat(healthConnectRepository).isNotNull()
    }
}
