# Story 1.1: Project Setup and Build Configuration

Status: ready-for-dev

## Story

As a developer,
I want a properly configured Android project with all required dependencies,
So that I can begin feature development with a stable build environment.

## Acceptance Criteria

1. Build completes successfully with all dependencies resolved
2. Project structure follows Android best practices (src/main/java, res/, manifests)
3. Build tools configured: Gradle 8.13, AGP 8.13.0, Kotlin 2.2.21, JDK 17
4. Min SDK Android 9 (API 28), target/compile SDK Android 15 (API 35)
5. Core dependencies added: AndroidX, Kotlin coroutines, Jetpack Compose
6. Health Connect SDK dependency included
7. Networking libraries configured (OkHttp/Retrofit)

## Tasks / Subtasks

- [ ] **Task 1: Create Android project structure** (AC: #1, #2)
  - [ ] Initialize empty Android project via Android Studio or command line
  - [ ] Configure package structure: `com.foodie.app` with data/, domain/, ui/, di/ layers
  - [ ] Verify standard Android directories exist: src/main/java, res/, AndroidManifest.xml
  - [ ] Create placeholder MainActivity.kt with basic Compose setup

- [ ] **Task 2: Configure build tools and SDK versions** (AC: #3, #4)
  - [ ] Set Gradle version to 8.13 in gradle-wrapper.properties
  - [ ] Configure Android Gradle Plugin (AGP) 8.13.0 in project-level build.gradle.kts
  - [ ] Set Kotlin version to 2.2.21 in project-level build.gradle.kts
  - [ ] Configure minSdk = 28, targetSdk = 35, compileSdk = 35 in app-level build.gradle.kts
  - [ ] Verify JDK 17 is configured in Java toolchain settings

- [ ] **Task 3: Add core dependencies** (AC: #5)
  - [ ] Add Compose BOM 2024.10.01 for version alignment
  - [ ] Add AndroidX core libraries: core-ktx, lifecycle-runtime-ktx, activity-compose
  - [ ] Add Jetpack Compose dependencies: ui, ui-graphics, ui-tooling-preview, material3
  - [ ] Add Kotlin coroutines: kotlinx-coroutines-android 1.9.0
  - [ ] Add lifecycle-runtime-compose and lifecycle-viewmodel-compose

- [ ] **Task 4: Add Health Connect SDK** (AC: #6)
  - [ ] Add Health Connect dependency: androidx.health.connect:connect-client:1.1.0-alpha10
  - [ ] Add required manifest permissions: READ_NUTRITION, WRITE_NUTRITION
  - [ ] Add Health Connect availability query in manifest

- [ ] **Task 5: Configure networking libraries** (AC: #7)
  - [ ] Add Retrofit 2.11.0 with Gson converter
  - [ ] Add OkHttp 4.12.0 with logging interceptor
  - [ ] Configure basic network security (HTTPS only)
  - [ ] Add internet permission to AndroidManifest.xml

- [ ] **Task 6: Configure Hilt dependency injection** (AC: #1)
  - [ ] Add Hilt Android 2.51.1 and compiler dependencies
  - [ ] Add KSP plugin for annotation processing
  - [ ] Create FoodieApplication class with @HiltAndroidApp annotation
  - [ ] Register application class in AndroidManifest.xml

- [ ] **Task 7: Add testing dependencies** (AC: #1)
  - [ ] Add JUnit 4.13.2 for unit testing
  - [ ] Add Mockito 5.14.2 and Mockito-Kotlin 5.4.0 for mocking
  - [ ] Add kotlinx-coroutines-test for async testing
  - [ ] Add Truth library for assertions
  - [ ] Add Espresso and Compose UI test dependencies for instrumentation

- [ ] **Task 8: Configure build variants and ProGuard** (AC: #1)
  - [ ] Configure debug and release build types
  - [ ] Enable R8 minification for release builds
  - [ ] Create proguard-rules.pro with basic keep rules
  - [ ] Configure BuildConfig fields if needed

- [ ] **Task 9: Verify build success** (AC: #1)
  - [ ] Run `./gradlew clean build` and verify zero errors
  - [ ] Run `./gradlew dependencies` to verify all dependencies resolve
  - [ ] Test debug build installs on emulator or device
  - [ ] Verify project opens successfully in Android Studio or VS Code

## Dev Notes

### Project Structure Notes

The project follows standard Android architecture with clean separation of concerns:

```
com.foodie.app/
├── FoodieApplication.kt           # Hilt application class
├── MainActivity.kt                # Single activity with Compose
├── di/                           # Dependency injection modules
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── WorkManagerModule.kt
├── data/                         # Data layer
│   ├── local/                    # Local data sources
│   │   ├── datastore/           # Secure preferences
│   │   └── healthconnect/       # Health Connect manager
│   ├── remote/                   # Remote data sources
│   │   ├── api/                 # Retrofit interfaces
│   │   └── dto/                 # Data transfer objects
│   └── repository/              # Repository implementations
├── domain/                       # Business logic layer
│   ├── model/                   # Domain models
│   └── usecase/                 # Use cases
└── ui/                          # Presentation layer
    ├── theme/                   # Compose theme
    ├── navigation/              # Navigation graph
    ├── components/              # Reusable UI components
    └── screens/                 # Screen implementations
```

**Key Architectural Decisions:**
- **MVVM Pattern:** ViewModel → Repository → DataSource for clear separation
- **Hilt DI:** Compile-time dependency injection for better performance and type safety
- **Jetpack Compose:** Modern declarative UI framework
- **Health Connect Single Source of Truth:** No local database needed (Room not included)
- **VS Code Development:** Command-line Gradle builds, Android Emulator for testing

### Build Configuration Details

**Gradle Configuration (gradle.properties):**
```properties
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC
org.gradle.caching=true
org.gradle.parallel=true
android.useAndroidX=true
kotlin.code.style=official
```

**Version Catalog Approach:**
All dependency versions are managed directly in build.gradle.kts files for this small project. For larger projects, consider using Gradle version catalogs.

**JDK 17 Configuration:**
Required for AGP 8.13.0. Ensure JDK 17 is installed and configured in VS Code settings:
```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home",
      "default": true
    }
  ]
}
```

### Testing Standards

**Unit Test Requirements:**
- All repository methods must have unit tests (target 80% coverage)
- Use Mockito to mock dependencies (HealthConnectManager, API interfaces)
- Use Truth assertions for cleaner test code
- Follow naming pattern: `methodName_whenCondition_thenExpectedResult`

**Test Example:**
```kotlin
@Test
fun `build configuration should use correct SDK versions`() {
    // This test would verify build.gradle.kts configuration
    // (Manual verification in this story)
}
```

### References

**Technical Specifications:**
- [Source: docs/architecture.md#Project-Structure] - Complete package structure and layer definitions
- [Source: docs/architecture.md#Technology-Stack-Details] - Dependency versions and rationale
- [Source: docs/architecture.md#Dependencies-and-Integrations] - Full dependency list from tech spec
- [Source: docs/tech-spec-epic-1.md#Dependencies-and-Integrations] - Epic 1 specific build dependencies

**Acceptance Criteria Source:**
- [Source: docs/epics.md#Story-1.1] - User story and acceptance criteria
- [Source: docs/tech-spec-epic-1.md#Acceptance-Criteria] - Detailed AC with traceability

**Build Tool Documentation:**
- Android Gradle Plugin 8.13.0: https://developer.android.com/studio/releases/gradle-plugin
- Kotlin 2.2.21: https://kotlinlang.org/docs/releases.html
- Gradle 8.13: https://gradle.org/releases/

**Key Configuration Files:**
- Project-level `build.gradle.kts`: Plugin versions, repositories
- App-level `build.gradle.kts`: Dependencies, SDK versions, build types
- `gradle-wrapper.properties`: Gradle version
- `AndroidManifest.xml`: Permissions, application class registration

### Development Environment Setup

**Prerequisites:**
- VS Code with Kotlin and Gradle extensions
- Android SDK Command Line Tools installed
- JDK 17 configured
- Android Emulator (Pixel 8, API 35, ARM64) created

**First Build Commands:**
```bash
# Verify Gradle wrapper
./gradlew --version

# Sync dependencies
./gradlew build

# List all dependencies
./gradlew dependencies

# Install debug build on emulator
./gradlew installDebug

# Run unit tests
./gradlew test
```

### Known Issues and Considerations

**Health Connect Alpha Dependency:**
- Using version 1.1.0-alpha10 (latest available)
- Monitor for API changes in future releases
- Wrap all Health Connect calls in repository layer for easy updates

**Compose Compiler Version:**
- Kotlin 2.2.21 requires compatible Compose compiler version
- Using Compose BOM 2024.10.01 which includes compatible compiler extension (1.5.15)

**ProGuard Rules:**
- Must keep Health Connect SDK classes from obfuscation
- Must keep Retrofit interfaces and models
- Must keep Hilt generated code

**Build Performance:**
- Enable Gradle daemon and parallel execution
- Use build cache for faster incremental builds
- Allocate sufficient memory (4GB recommended)

## Dev Agent Record

### Context Reference

- docs/stories/1-1-project-setup-and-build-configuration.context.xml

### Agent Model Used

<!-- Agent model name and version will be recorded here after implementation -->

### Debug Log References

<!-- Links to debug sessions or log files will be added during implementation -->

### Completion Notes List

<!-- Agent will add implementation notes here:
- New patterns/services created
- Architectural deviations or decisions made
- Technical debt deferred to future stories
- Warnings or recommendations for next story
- Interfaces/methods created for reuse
-->

### File List

<!-- Agent will list files created/modified during implementation:
NEW:
- path/to/new/file.kt

MODIFIED:
- path/to/modified/file.kt

DELETED:
- path/to/deleted/file.kt
-->
