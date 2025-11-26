plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "com.foodie.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.foodie.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.foodie.app.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Azure OpenAI Configuration from local.properties
        // Story 2.4: Temporary BuildConfig approach until Story 5.2 implements EncryptedSharedPreferences
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        buildConfigField(
            "String",
            "AZURE_OPENAI_API_KEY",
            properties.getProperty("azure.openai.api.key", "\"\""),
        )
        buildConfigField(
            "String",
            "AZURE_OPENAI_ENDPOINT",
            properties.getProperty("azure.openai.endpoint", "\"https://your-resource.openai.azure.com\""),
        )
        buildConfigField(
            "String",
            "AZURE_OPENAI_MODEL",
            properties.getProperty("azure.openai.model", "\"gpt-4.1\""),
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        // Strict compilation - treat all warnings as errors
        allWarningsAsErrors = true

        // Enable additional warnings
        freeCompilerArgs += listOf(
            "-Xjsr305=strict", // Strict null-safety for Java interop
            "-opt-in=kotlin.RequiresOptIn", // Opt-in APIs
            "-Xcontext-receivers", // Context receivers (experimental)
        )
    }

    // Android Lint Configuration
    lint {
        // Enable all checks by default
        checkAllWarnings = true

        // Treat warnings as errors
        warningsAsErrors = true

        // Abort build on errors
        abortOnError = true

        // Continue checking even if errors are found
        checkReleaseBuilds = true

        // Enable text report
        textReport = true
        textOutput = file("stdout")

        // Enable HTML report
        htmlReport = true
        htmlOutput = file("${project.buildDir}/reports/lint/lint-report.html")

        // Enable XML report for CI/CD
        xmlReport = true
        xmlOutput = file("${project.buildDir}/reports/lint/lint-report.xml")

        // Enable SARIF report for GitHub integration
        sarifReport = true
        sarifOutput = file("${project.buildDir}/reports/lint/lint-report.sarif")

        // Use baseline file to suppress existing issues
        baseline = file("lint-baseline.xml")

        // Disable specific checks that may be too noisy or not applicable
        disable += listOf(
            "ObsoleteLintCustomCheck", // For migration periods
            "GradleDependency", // Managed separately
        )

        // Enable specific important checks
        enable += listOf(
            "Interoperability", // Kotlin/Java interop issues
            "UnusedResources", // Dead code detection
            "IconMissingDensityFolder", // Missing icon variants
            "InvalidPackage", // Invalid dependencies
        )

        // Treat specific checks as errors
        error += listOf(
            "StopShip", // TODOs marked for removal
            "NewApi", // API version issues
            "InlinedApi", // Inlined constants
            "ObsoleteSdkInt", // Outdated SDK checks
        )

        // Treat specific checks as warnings
        warning += listOf(
            "UnusedResources",
            "IconDensities",
        )

        // Informational only
        informational += listOf(
            "LogConditional",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            // Enable JaCoCo coverage for unit tests
            it.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }

    packaging {
        resources {
            // Allow runtime to merge duplicate legal notices from test libs
            pickFirsts += "META-INF/LICENSE.md"
            pickFirsts += "META-INF/LICENSE-notice.md"
            pickFirsts += "META-INF/NOTICE.md"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Glance (Jetpack Widgets)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Health Connect
    implementation(libs.androidx.health.connect)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image Loading
    implementation(libs.coil.compose)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.arch.core.testing)

    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.android.compiler)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}

// JaCoCo Code Coverage Configuration
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    // Note: createDebugCoverageReport requires device/emulator and working test runner
    // Currently disabled due to HiltTestRunner configuration issues

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*_Hilt*.class",
        "**/Hilt_*.class",
        "**/*_Factory.class",
        "**/*_MembersInjector.class",
        "**/*Module.class",
        "**/*Module$*.class",
        "**/*Component.class",
        "**/*Component$*.class",
        "**/*ComposableSingletons$*.class",
        "**/*_Impl.class",
        "**/*_Impl$*.class",
    )

    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(project.buildDir) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
            )
        },
    )
}
