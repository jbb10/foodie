// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

// Detekt configuration for Kotlin static analysis
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")

    // Enable parallel execution
    parallel = true

    // Auto-correct when possible
    autoCorrect = true
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detekt.get()}")
}

// Spotless configuration for code formatting
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "android" to "true",
                    "max_line_length" to "off",
                    "indent_size" to "4",
                    "continuation_indent_size" to "4",
                    "insert_final_newline" to "true",
                    "trim_trailing_whitespace" to "true",
                    "ij_kotlin_imports_layout" to "*",
                    "ij_kotlin_allow_trailing_comma" to "true",
                    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                    "ktlint_function-naming" to "disabled",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_max-line-length" to "disabled",
                    "ktlint_standard_discouraged-comment-location" to "disabled",
                ),
            )
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_max-line-length" to "disabled",
                    "ktlint_standard_discouraged-comment-location" to "disabled",
                ),
            )
    }

    // XML formatting disabled - Prettier has dependency conflicts and XML formatting is not critical
    // format("xml") {
    //     target("**/*.xml")
    //     targetExclude("**/build/**", "**/.*")
    // }
}
