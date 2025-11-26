package com.foodie.app.data.repository

import android.content.SharedPreferences
import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PreferencesRepositoryImpl.
 *
 * Tests preference CRUD operations, reactive Flow emissions, and error handling.
 * Uses MockK for mocking SharedPreferences and standard Flow testing.
 *
 * Test coverage:
 * - String preference get/set operations
 * - Boolean preference get/set operations
 * - Reactive Flow emissions on preference changes
 * - Default value handling
 * - Clear all preferences
 */
class PreferencesRepositoryTest {

    private lateinit var repository: PreferencesRepositoryImpl
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var securePreferences: SecurePreferences
    private lateinit var azureOpenAiApi: AzureOpenAiApi
    private lateinit var gson: Gson

    @Before
    fun setup() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        azureOpenAiApi = mockk(relaxed = true)
        gson = Gson()

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.clear() } returns editor

        repository = PreferencesRepositoryImpl(sharedPreferences, securePreferences, azureOpenAiApi, gson)
    }

    @Test
    fun `getString returns correct value`() = runTest {
        // Given: SharedPreferences contains a string value
        every { sharedPreferences.getString("pref_test", "") } returns "test_value"

        // When: Getting string preference
        val result = repository.getString("pref_test")

        // Then: Correct value returned
        assertThat(result).isEqualTo("test_value")
    }

    @Test
    fun `getString returns default value when not set`() = runTest {
        // Given: SharedPreferences does not contain key
        every { sharedPreferences.getString("pref_test", "default") } returns "default"

        // When: Getting string preference with default
        val result = repository.getString("pref_test", "default")

        // Then: Default value returned
        assertThat(result).isEqualTo("default")
    }

    @Test
    fun `setString saves correctly`() = runTest {
        // When: Setting string preference
        val result = repository.setString("pref_test", "test_value")

        // Then: SharedPreferences editor called with correct parameters
        verify { editor.putString("pref_test", "test_value") }
        verify { editor.apply() }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `getBoolean returns correct value`() = runTest {
        // Given: SharedPreferences contains a boolean value
        every { sharedPreferences.getBoolean("pref_test", false) } returns true

        // When: Getting boolean preference
        val result = repository.getBoolean("pref_test")

        // Then: Correct value returned
        assertThat(result).isTrue()
    }

    @Test
    fun `getBoolean returns default value when not set`() = runTest {
        // Given: SharedPreferences does not contain key
        every { sharedPreferences.getBoolean("pref_test", true) } returns true

        // When: Getting boolean preference with default
        val result = repository.getBoolean("pref_test", true)

        // Then: Default value returned
        assertThat(result).isTrue()
    }

    @Test
    fun `setBoolean saves correctly`() = runTest {
        // When: Setting boolean preference
        val result = repository.setBoolean("pref_test", true)

        // Then: SharedPreferences editor called with correct parameters
        verify { editor.putBoolean("pref_test", true) }
        verify { editor.apply() }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `observePreferences emits current values immediately`() = runTest {
        // Given: SharedPreferences contains preferences
        val preferences = mapOf(
            "pref_test1" to "value1",
            "pref_test2" to "value2",
        )
        every { sharedPreferences.all } returns preferences

        // When: Observing preferences
        val emittedPrefs = repository.observePreferences().first()

        // Then: Current values emitted immediately
        assertThat(emittedPrefs).isEqualTo(preferences)
    }

    @Test
    fun `observeString emits current value immediately`() = runTest {
        // Given: SharedPreferences contains a string value
        every { sharedPreferences.getString("pref_test", null) } returns "test_value"

        // When: Observing string preference
        val value = repository.observeString("pref_test").first()

        // Then: Current value emitted immediately
        assertThat(value).isEqualTo("test_value")
    }

    @Test
    fun `observeString emits null when preference not set`() = runTest {
        // Given: SharedPreferences does not contain key
        every { sharedPreferences.getString("pref_test", null) } returns null

        // When: Observing string preference
        val value = repository.observeString("pref_test").first()

        // Then: Null emitted
        assertThat(value).isNull()
    }

    @Test
    fun `observeBoolean emits current value immediately`() = runTest {
        // Given: SharedPreferences contains a boolean value
        every { sharedPreferences.contains("pref_test") } returns true
        every { sharedPreferences.getBoolean("pref_test", false) } returns true

        // When: Observing boolean preference
        val value = repository.observeBoolean("pref_test").first()

        // Then: Current value emitted immediately
        assertThat(value).isTrue()
    }

    @Test
    fun `observeBoolean emits null when preference not set`() = runTest {
        // Given: SharedPreferences does not contain key
        every { sharedPreferences.contains("pref_test") } returns false

        // When: Observing boolean preference
        val value = repository.observeBoolean("pref_test").first()

        // Then: Null emitted
        assertThat(value).isNull()
    }

    @Test
    fun `clearAll removes all preferences`() = runTest {
        // When: Clearing all preferences
        val result = repository.clearAll()

        // Then: Editor clear called
        verify { editor.clear() }
        verify { editor.apply() }
        assertThat(result.isSuccess).isTrue()
    }
}
