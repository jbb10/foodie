package com.foodie.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [DeepLinkTestHelper] utility functions.
 *
 * Tests the helper methods that don't require Android framework dependencies.
 * Intent creation and navigation controller tests are in instrumentation tests.
 */
class DeepLinkTestHelperTest {
    
    @Test
    fun getCommonTestUris_containsMealListUri() {
        val uris = DeepLinkTestHelper.getCommonTestUris()
        
        assertThat(uris).containsKey("meal_list")
        assertThat(uris["meal_list"]).isEqualTo("foodie://meals")
    }
    
    @Test
    fun getCommonTestUris_containsMealDetailUri() {
        val uris = DeepLinkTestHelper.getCommonTestUris()
        
        assertThat(uris).containsKey("meal_detail_valid")
        assertThat(uris["meal_detail_valid"]).isEqualTo("foodie://meals/test-meal-123")
    }
    
    @Test
    fun getCommonTestUris_containsLegacyHomeUri() {
        val uris = DeepLinkTestHelper.getCommonTestUris()
        
        assertThat(uris).containsKey("home_legacy")
        assertThat(uris["home_legacy"]).isEqualTo("foodie://home")
    }
    
    @Test
    fun getCommonTestUris_containsMalformedUris() {
        val uris = DeepLinkTestHelper.getCommonTestUris()
        
        assertThat(uris).containsKey("malformed_no_host")
        assertThat(uris).containsKey("malformed_invalid_host")
        assertThat(uris).containsKey("malformed_invalid_meal_id")
    }
    
    @Test
    fun getCommonTestUris_returnsExpectedNumberOfEntries() {
        val uris = DeepLinkTestHelper.getCommonTestUris()
        
        assertThat(uris).hasSize(6)
    }
}
