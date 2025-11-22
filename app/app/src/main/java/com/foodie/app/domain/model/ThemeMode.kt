package com.foodie.app.domain.model

/**
 * Theme mode preference options for the Foodie app.
 *
 * Determines whether the app uses light mode, dark mode, or follows system theme preference.
 * Stored in SharedPreferences as string value (key: "pref_theme_mode").
 *
 * @property value String value stored in SharedPreferences ("system", "light", or "dark")
 * @property displayName User-facing label displayed in Settings UI
 *
 * Story 5.4: Dark Mode Support (AC-7, AC-8)
 */
enum class ThemeMode(val value: String, val displayName: String) {
    /**
     * Follow device system theme preference (Settings → Display → Dark theme).
     * Default mode for new installations.
     */
    SYSTEM_DEFAULT("system", "System Default"),

    /**
     * Force light mode regardless of system setting.
     */
    LIGHT("light", "Light"),

    /**
     * Force dark mode regardless of system setting.
     */
    DARK("dark", "Dark");

    companion object {
        /**
         * Convert SharedPreferences string value to ThemeMode enum.
         *
         * @param value String value from SharedPreferences ("system", "light", "dark")
         * @return Corresponding ThemeMode, defaults to SYSTEM_DEFAULT for invalid values
         */
        fun fromValue(value: String): ThemeMode =
            values().find { it.value == value } ?: SYSTEM_DEFAULT

        /**
         * Default theme mode for new installations.
         */
        val DEFAULT = SYSTEM_DEFAULT
    }
}
