package com.foodie.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for ThemeMode enum.
 *
 * Story 5.4: Dark Mode Support
 */
class ThemeModeTest {

    @Test
    fun `fromValue with system returns SYSTEM_DEFAULT`() {
        val result = ThemeMode.fromValue("system")
        assertThat(result).isEqualTo(ThemeMode.SYSTEM_DEFAULT)
    }

    @Test
    fun `fromValue with light returns LIGHT`() {
        val result = ThemeMode.fromValue("light")
        assertThat(result).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `fromValue with dark returns DARK`() {
        val result = ThemeMode.fromValue("dark")
        assertThat(result).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `fromValue with invalid value returns SYSTEM_DEFAULT`() {
        val result = ThemeMode.fromValue("invalid")
        assertThat(result).isEqualTo(ThemeMode.SYSTEM_DEFAULT)
    }

    @Test
    fun `fromValue with empty string returns SYSTEM_DEFAULT`() {
        val result = ThemeMode.fromValue("")
        assertThat(result).isEqualTo(ThemeMode.SYSTEM_DEFAULT)
    }

    @Test
    fun `DEFAULT constant is SYSTEM_DEFAULT`() {
        assertThat(ThemeMode.DEFAULT).isEqualTo(ThemeMode.SYSTEM_DEFAULT)
    }

    @Test
    fun `SYSTEM_DEFAULT has correct value and displayName`() {
        assertThat(ThemeMode.SYSTEM_DEFAULT.value).isEqualTo("system")
        assertThat(ThemeMode.SYSTEM_DEFAULT.displayName).isEqualTo("System Default")
    }

    @Test
    fun `LIGHT has correct value and displayName`() {
        assertThat(ThemeMode.LIGHT.value).isEqualTo("light")
        assertThat(ThemeMode.LIGHT.displayName).isEqualTo("Light")
    }

    @Test
    fun `DARK has correct value and displayName`() {
        assertThat(ThemeMode.DARK.value).isEqualTo("dark")
        assertThat(ThemeMode.DARK.displayName).isEqualTo("Dark")
    }
}
