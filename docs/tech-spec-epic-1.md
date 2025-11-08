# Epic Technical Specification: Foundation & Infrastructure

Date: 2025-11-08
Author: BMad
Epic ID: 1
Status: Draft

---

## Overview

Epic 1 establishes the complete technical foundation for the Foodie Android application. This epic creates the project structure, implements the MVVM architectural pattern with dependency injection, configures core navigation flows, integrates Google Health Connect for data persistence, and establishes robust error handling patterns. All subsequent epics depend on the infrastructure established here.

The epic delivers a fully buildable Android application with working Health Connect integration, a sample MVVM stack demonstrating best practices, and a comprehensive error handling framework. While no user-facing features are complete, the technical foundation enables rapid development of the AI meal capture and data management features in later epics.

## Objectives and Scope

**In Scope:**
- Android project initialization with Gradle 8.13, AGP 8.13.0, Kotlin 2.2.21
- Dependency configuration: Jetpack Compose, Hilt DI, Retrofit, WorkManager, Health Connect SDK
- MVVM architecture implementation with base classes and sample stack (BaseViewModel, BaseRepository)
- Navigation framework using Jetpack Navigation Compose with deep linking support
- Health Connect SDK integration with permission handling and CRUD operations
- Centralized error handling with Result<T> wrapper and user-friendly error messages
- Logging framework using Timber with production-safe configuration

**Out of Scope:**
- Camera integration (Epic 2)
- Azure OpenAI API client implementation (Epic 2)
- Lock screen widget (Epic 2)
- UI screens beyond placeholders (Epic 2, 3, 5)
- WorkManager background processing implementation (Epic 2)
- Actual meal capture or data management features

**Success Criteria:**
- Project builds successfully on Android Studio with zero compilation errors
- Health Connect integration demonstrates successful write and read of sample NutritionRecord
- Sample MVVM stack (SampleViewModel → SampleRepository → HealthConnectManager) validates architectural pattern
- Navigation between placeholder screens works with proper back stack handling
- Error handling framework captures and logs all exceptions with user-friendly messages

## System Architecture Alignment

This epic implements the foundational layers defined in the Architecture Document:

**Project Structure:** Creates the complete `com.foodie.app` package structure with data/, domain/, ui/, and di/ layers following clean architecture principles.

**Technology Stack Integration:**
- UI Framework: Jetpack Compose (BOM 2024.10.01) with Material3
- Dependency Injection: Hilt 2.51.1 with AppModule, NetworkModule, RepositoryModule
- HTTP Client: Retrofit 2.11.0 configured but not yet used (Epic 2)
- Data Persistence: Health Connect 1.1.0-alpha10 as single source of truth
- Async Processing: Kotlin Coroutines 1.9.0 with Flow and StateFlow
- Logging: Timber 5.0.1 with debug/release tree configuration

**Architectural Patterns:**
- MVVM with repository pattern: UI Layer → Domain Layer → Data Layer
- Dependency inversion: Interfaces in domain layer, implementations in data layer
- Reactive state management: StateFlow in ViewModels, collectAsStateWithLifecycle in Compose
- Error handling: Result<T> sealed class for consistent error propagation

**Health Connect as Single Source of Truth:**
Per architecture decision, all nutrition data (calories, descriptions, timestamps) is stored in Health Connect using `NutritionRecord.energy` and `NutritionRecord.name` fields. No local database (Room) is needed.

## Detailed Design

### Services and Modules

**Hilt Dependency Injection Modules:**

| Module | Provides | Scope | Consumers |
|--------|----------|-------|-----------|
| AppModule | Application context, SecurePreferences, HealthConnectManager | Singleton | All layers |
| NetworkModule | Retrofit instance, OkHttp client, AzureOpenAiApi interface | Singleton | Repositories, Workers |
| RepositoryModule | Repository interface bindings | Singleton | ViewModels, Use Cases |
| WorkManagerModule | HiltWorkerFactory configuration | Singleton | WorkManager |

**Core Services:**

1. **HealthConnectManager** (data/local/healthconnect/)
   - Purpose: Wrapper for Health Connect SDK operations
   - Responsibilities: Insert, query, update, delete NutritionRecords
   - Key Methods:
     - `insertNutritionRecord(calories: Int, description: String, timestamp: Instant): String` - Returns HC record ID
     - `queryNutritionRecords(startTime: Instant, endTime: Instant): List<NutritionRecord>`
     - `updateNutritionRecord(recordId: String, calories: Int, description: String)` - Delete + re-insert pattern
     - `deleteNutritionRecord(recordId: String)`
     - `checkPermissions(): Boolean`
     - `requestPermissions(activity: ComponentActivity)`

2. **SecurePreferences** (data/local/datastore/)
   - Purpose: Encrypted storage for API keys and sensitive configuration
   - Technology: EncryptedSharedPreferences with AES256_GCM
   - Stored Values: Azure OpenAI API key, endpoint URL, model deployment name
   - Key Methods:
     - `saveApiKey(key: String)`
     - `getApiKey(): String?`
     - `saveEndpoint(url: String)`
     - `getEndpoint(): String?`
     - `saveModel(modelName: String)`
     - `getModel(): String?`

3. **Logger Utility** (util/)
   - Purpose: Centralized logging with production safety
   - Implementation: Timber with custom ReleaseTree
   - Debug Mode: Full logging to Logcat
   - Release Mode: ERROR and WARN only (prepared for Crashlytics integration)
   - Usage: `Timber.d()`, `Timber.i()`, `Timber.w()`, `Timber.e()`

### Data Models and Contracts

**Domain Models (domain/model/):**

```kotlin
// MealEntry.kt - Domain representation of a meal
data class MealEntry(
    val id: String,              // Health Connect record ID
    val timestamp: Instant,      // Meal capture time
    val description: String,     // AI-generated food description
    val calories: Int            // Calorie estimate
) {
    init {
        require(calories in 1..5000) { "Calories must be between 1 and 5000" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}

// NutritionData.kt - Parsed AI response
data class NutritionData(
    val calories: Int,
    val description: String
) {
    init {
        require(calories in 1..5000) { "Calories must be between 1 and 5000" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}

// AnalysisStatus.kt - Background processing state
sealed class AnalysisStatus {
    object Idle : AnalysisStatus()
    object Analyzing : AnalysisStatus()
    data class Success(val data: NutritionData) : AnalysisStatus()
    data class Error(val message: String) : AnalysisStatus()
}
```

**Repository Interfaces (domain/repository/):**

```kotlin
// MealRepository.kt
interface MealRepository {
    fun getMealHistory(): Flow<Result<List<MealEntry>>>
    suspend fun updateMeal(id: String, calories: Int, description: String): Result<Unit>
    suspend fun deleteMeal(id: String): Result<Unit>
}

// NutritionAnalysisRepository.kt (placeholder for Epic 2)
interface NutritionAnalysisRepository {
    suspend fun analyzePhoto(photoUri: Uri): Result<NutritionData>
}
```

**Error Handling Model (util/):**

```kotlin
// Result.kt - Sealed class for consistent error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
    
    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(exception: Throwable, userMessage: String) = Error(exception, userMessage)
        fun loading() = Loading
    }
}

// Extension for Flow
fun <T> Flow<T>.asResult(): Flow<Result<T>> = flow {
    emit(Result.loading())
    try {
        collect { value -> emit(Result.success(value)) }
    } catch (e: Exception) {
        emit(Result.error(e, e.toUserMessage()))
    }
}
```

### APIs and Interfaces

**Health Connect API Integration:**

```kotlin
// HealthConnectManager.kt
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }
    
    /**
     * Check if Health Connect is available on the device
     */
    suspend fun isAvailable(): Boolean {
        return HealthConnectClient.isAvailable(context)
    }
    
    /**
     * Check if nutrition permissions are granted
     */
    suspend fun checkPermissions(): Boolean {
        val permissions = setOf(
            HealthPermission.createReadPermission(NutritionRecord::class),
            HealthPermission.createWritePermission(NutritionRecord::class)
        )
        return healthConnectClient.permissionController.getGrantedPermissions(permissions)
            .containsAll(permissions)
    }
    
    /**
     * Request nutrition permissions
     */
    fun requestPermissions(activity: ComponentActivity) {
        val permissions = setOf(
            HealthPermission.createReadPermission(NutritionRecord::class),
            HealthPermission.createWritePermission(NutritionRecord::class)
        )
        
        activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.isEmpty()) {
                Timber.w("Health Connect permissions denied")
            } else {
                Timber.i("Health Connect permissions granted: $granted")
            }
        }.launch(permissions)
    }
    
    /**
     * Insert a nutrition record with calories and description
     * @return Health Connect record ID
     */
    suspend fun insertNutritionRecord(
        calories: Int,
        description: String,
        timestamp: Instant
    ): String {
        val record = NutritionRecord(
            energy = Energy.kilocalories(calories.toDouble()),
            name = description,
            startTime = timestamp,
            endTime = timestamp,
            startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
            endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
        )
        
        val response = healthConnectClient.insertRecords(listOf(record))
        return response.recordIdsList.first()
    }
    
    /**
     * Query nutrition records within a time range
     */
    suspend fun queryNutritionRecords(
        startTime: Instant = Instant.now().minus(30, ChronoUnit.DAYS),
        endTime: Instant = Instant.now()
    ): List<NutritionRecord> {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }
    
    /**
     * Update a nutrition record (delete + re-insert pattern)
     */
    suspend fun updateNutritionRecord(
        recordId: String,
        newCalories: Int,
        newDescription: String,
        originalTimestamp: Instant
    ) {
        deleteNutritionRecord(recordId)
        insertNutritionRecord(newCalories, newDescription, originalTimestamp)
    }
    
    /**
     * Delete a nutrition record
     */
    suspend fun deleteNutritionRecord(recordId: String) {
        healthConnectClient.deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = listOf(recordId),
            clientRecordIdsList = emptyList()
        )
    }
}
```

**Retrofit Configuration (for Epic 2, configured here):**

```kotlin
// NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        securePreferences: SecurePreferences
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val apiKey = securePreferences.getApiKey() ?: ""
                val request = chain.request().newBuilder()
                    .addHeader("api-key", apiKey)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        securePreferences: SecurePreferences
    ): Retrofit {
        val baseUrl = securePreferences.getEndpoint() 
            ?: "https://placeholder.openai.azure.com/" // Will be configured in settings
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAzureOpenAiApi(retrofit: Retrofit): AzureOpenAiApi {
        return retrofit.create(AzureOpenAiApi::class.java)
    }
}
```

**Navigation Configuration:**

```kotlin
// NavGraph.kt
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MealList.route
    ) {
        composable(Screen.MealList.route) {
            MealListScreen(
                onMealClick = { mealId ->
                    navController.navigate(Screen.MealDetail.createRoute(mealId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.MealDetail.route,
            arguments = listOf(
                navArgument("mealId") { type = NavType.StringType }
            )
        ) {
            MealDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object MealList : Screen("meal_list")
    object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: String) = "meal_detail/$mealId"
    }
    object Settings : Screen("settings")
}
```

### Workflows and Sequencing

**Health Connect Integration Flow:**

1. **App Launch → Permission Check**
   - FoodieApplication.onCreate() initializes Hilt
   - MainActivity checks Health Connect availability
   - If not installed: Show dialog with Play Store link
   - Check permissions status
   - If not granted: Request permissions via PermissionController

2. **Sample Write/Read Operation** (validates integration)
   - User action (test button in UI)
   - ViewModel calls `healthConnectManager.insertNutritionRecord(500, "Test meal", Instant.now())`
   - Health Connect saves NutritionRecord
   - ViewModel calls `healthConnectManager.queryNutritionRecords()`
   - Results displayed in UI confirming round-trip success

3. **Error Handling Flow**
   - Any Health Connect operation wrapped in try-catch
   - Exceptions mapped to Result.Error with user-friendly messages
   - ViewModel emits error state
   - UI displays Snackbar or error message
   - Timber logs technical details for debugging

**Navigation Flow:**

```
MainActivity (Compose setContent)
    ↓
NavHost with NavController
    ↓
Screen.MealList (default) → Placeholder list screen
    ├─→ Tap meal → Navigate to Screen.MealDetail
    └─→ Tap settings → Navigate to Screen.Settings
```

**Dependency Injection Flow:**

```
@HiltAndroidApp FoodieApplication
    ↓
Hilt generates DI graph
    ↓
@AndroidEntryPoint MainActivity
    ↓
@HiltViewModel injected into Composables via hiltViewModel()
    ↓
ViewModels receive Repository via constructor injection
    ↓
Repositories receive HealthConnectManager via constructor injection
```

## Non-Functional Requirements

### Performance

**Build Performance:**
- Clean build time: < 60 seconds on modern development machine
- Incremental build: < 10 seconds for single file change
- Use Gradle configuration cache and build cache
- Enable parallel execution in gradle.properties

**Runtime Performance:**
- App cold start: < 2 seconds to first screen render
- Health Connect query (30 days): < 500ms for typical dataset (< 100 entries)
- Navigation transitions: 60fps smooth animations
- Memory footprint: < 100MB typical usage

**Optimization Strategies:**
- Lazy initialization of heavyweight objects (HealthConnectClient)
- Use remember and derivedStateOf in Compose to minimize recomposition
- Enable R8 code shrinking and ProGuard for release builds

### Security

**API Key Storage:**
- Azure OpenAI API key stored in EncryptedSharedPreferences
- Encryption: AES256-GCM with hardware-backed MasterKey
- No API key in logs, crash reports, or analytics
- API key never transmitted except in HTTPS requests to Azure

**Health Connect Security:**
- Permissions requested with clear rationale: "Foodie needs to save your nutrition data to Health Connect for use with other health apps"
- Permission state checked before every Health Connect operation
- Graceful error handling if permissions revoked

**Network Security:**
- All API calls use HTTPS (enforced by Azure OpenAI endpoint)
- No certificate pinning needed (Azure uses standard CA certificates)
- Network security config: cleartext traffic disabled

**ProGuard Rules:**
- Obfuscate release builds to protect logic
- Keep Health Connect SDK classes from obfuscation
- Keep Retrofit interfaces and models from obfuscation
- Keep Hilt generated code from obfuscation

### Reliability/Availability

**Error Recovery:**
- All async operations return Result<T> for explicit error handling
- Network failures logged but don't crash app
- Health Connect unavailable: Show user-friendly message with retry option
- Permissions denied: Link to app settings with explanation

**State Preservation:**
- ViewModels survive configuration changes (screen rotation)
- Navigation state preserved in SavedStateHandle
- No data loss during process death (Health Connect is persistent)

**Testing Strategy for Reliability:**
- Unit tests for all repository operations
- Mock HealthConnectClient for deterministic testing
- Test error paths: permissions denied, HC unavailable, network failures
- Integration tests for critical paths

### Observability

**Logging Levels:**
- **DEBUG:** Detailed flow information (ViewModel actions, repository calls)
  - Example: `Timber.d("Loading meal history from Health Connect")`
- **INFO:** Significant events (successful operations, state transitions)
  - Example: `Timber.i("Health Connect permissions granted")`
- **WARN:** Recoverable errors, unexpected states
  - Example: `Timber.w("API key not configured, skipping background sync")`
- **ERROR:** Exceptions, failures requiring attention
  - Example: `Timber.e(exception, "Failed to save meal to Health Connect")`

**Structured Logging:**
```kotlin
// Tag conventions for filtering
Timber.tag("HealthConnect").d("Querying nutrition records")
Timber.tag("Navigation").d("Navigating to meal detail: $mealId")
Timber.tag("DI").d("Providing Retrofit instance")
```

**Production Monitoring (V2.0):**
- Prepare for Firebase Crashlytics integration
- ReleaseTree configured to send ERROR/WARN to crash reporting
- Breadcrumb logging for debugging production issues

**Performance Monitoring:**
- Use Android Studio Profiler during development
- Monitor startup time, memory usage, frame drops
- Log slow operations: `if (duration > 500) Timber.w("Slow query: ${duration}ms")`

## Dependencies and Integrations

**Build Dependencies (build.gradle.kts):**

```kotlin
dependencies {
    // Compose BOM - Version alignment for all Compose libraries
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Retrofit & OkHttp (configured, used in Epic 2)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // WorkManager (configured, used in Epic 2)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    
    // Secure Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.4.4")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

**External Integrations:**

| Integration | Version | Purpose | Configuration |
|-------------|---------|---------|---------------|
| Google Health Connect | 1.1.0-alpha10 | Data persistence | Permissions requested on first launch |
| Azure OpenAI | N/A (configured) | AI analysis (Epic 2) | API key/endpoint in EncryptedSharedPreferences |
| Timber | 5.0.1 | Logging | Initialized in Application.onCreate() |

**Manifest Permissions:**

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.health.READ_NUTRITION" />
<uses-permission android:name="android.permission.health.WRITE_NUTRITION" />

<!-- Health Connect availability check -->
<queries>
    <package android:name="com.google.android.apps.healthdata" />
</queries>
```

## Acceptance Criteria (Authoritative)

All acceptance criteria extracted from Epic 1 stories in epics.md:

**Story 1.1: Project Setup and Build Configuration**
1. Build completes successfully with all dependencies resolved
2. Project structure follows Android best practices (src/main/java, res/, manifests)
3. Build tools configured: Gradle 8.13, AGP 8.13.0, Kotlin 2.2.21, JDK 17
4. Min SDK Android 9 (API 28), target/compile SDK Android 15 (API 35)
5. Core dependencies added: AndroidX, Kotlin coroutines, Jetpack Compose
6. Health Connect SDK dependency included
7. Networking libraries configured (OkHttp/Retrofit)

**Story 1.2: MVVM Architecture Foundation**
1. ViewModel base classes exist with lifecycle management
2. Repository pattern interfaces defined for data access
3. Data source abstractions exist for external APIs and local storage
4. Dependency injection framework configured (Hilt)
5. SampleViewModel + Repository + DataSource demonstrate the pattern

**Story 1.3: Core Navigation and Screen Structure**
1. Navigation component configured with navigation graph
2. Main activity hosts the navigation controller
3. Placeholder screens exist for: List View, Edit Screen, Settings Screen
4. Navigation between screens works with proper back stack handling
5. Deep linking configured for widget intents

**Story 1.4: Health Connect Integration Setup**
1. Health Connect SDK initialized in application class
2. Permission request flow implemented for READ_NUTRITION and WRITE_NUTRITION
3. Health Connect availability check performed on app launch
4. Graceful handling if Health Connect not installed (link to Play Store)
5. Repository class exists for Health Connect operations (placeholder methods)
6. Sample write + read operation demonstrates successful integration

**Story 1.5: Logging and Error Handling Framework**
1. Centralized logging utility exists with log levels (DEBUG, INFO, WARN, ERROR)
2. Error handling wrapper exists for try-catch blocks with logging
3. User-facing error messages defined (no technical jargon)
4. Crash reporting configured (optional for MVP)
5. All API calls and critical operations wrapped with error handling

## Traceability Mapping

| AC# | Spec Section | Component | Test Idea |
|-----|--------------|-----------|-----------|
| 1.1.1 | Dependencies | build.gradle.kts | Verify all dependencies resolve, build succeeds |
| 1.1.2 | Project Structure | Project layout | Verify folder structure matches Android standards |
| 1.1.3 | Dependencies | build.gradle.kts | Assert Gradle/AGP/Kotlin versions in build files |
| 1.1.4 | Dependencies | build.gradle.kts | Assert minSdk=28, targetSdk=35, compileSdk=35 |
| 1.1.5 | Dependencies | build.gradle.kts | Verify AndroidX, Coroutines, Compose dependencies |
| 1.1.6 | Dependencies | build.gradle.kts | Assert Health Connect SDK in dependencies |
| 1.1.7 | Dependencies | build.gradle.kts | Assert Retrofit and OkHttp in dependencies |
| 1.2.1 | MVVM Architecture | BaseViewModel | Unit test ViewModel lifecycle |
| 1.2.2 | MVVM Architecture | Repository interfaces | Verify interface contracts exist |
| 1.2.3 | MVVM Architecture | Data source abstractions | Verify HealthConnectManager exists |
| 1.2.4 | MVVM Architecture | Hilt modules | Verify @HiltAndroidApp and modules |
| 1.2.5 | MVVM Architecture | Sample stack | Integration test full MVVM flow |
| 1.3.1 | Navigation | NavGraph.kt | Verify NavHost configuration |
| 1.3.2 | Navigation | MainActivity | Assert NavController hosted |
| 1.3.3 | Navigation | Screens | UI test navigation to each screen |
| 1.3.4 | Navigation | NavController | Test back stack behavior |
| 1.3.5 | Navigation | Deep linking | Test deep link intent handling |
| 1.4.1 | Health Connect | FoodieApplication | Verify HealthConnectClient initialized |
| 1.4.2 | Health Connect | HealthConnectManager | Test permission request flow |
| 1.4.3 | Health Connect | MainActivity | Test availability check on launch |
| 1.4.4 | Health Connect | HealthConnectManager | Mock unavailable, verify Play Store link |
| 1.4.5 | Health Connect | HealthConnectRepository | Verify CRUD method signatures |
| 1.4.6 | Health Connect | HealthConnectManager | Integration test: write + read NutritionRecord |
| 1.5.1 | Error Handling | Logger utility (Timber) | Verify log levels work |
| 1.5.2 | Error Handling | Result<T> wrapper | Unit test error propagation |
| 1.5.3 | Error Handling | Error messages | Verify user-friendly message mapping |
| 1.5.4 | Error Handling | Crashlytics (optional) | Verify ReleaseTree configuration |
| 1.5.5 | Error Handling | All operations | Code review: verify try-catch coverage |

## Risks, Assumptions, Open Questions

**Risks:**

1. **Risk:** Health Connect SDK is alpha (1.1.0-alpha10) - API stability concerns
   - **Mitigation:** Wrap all HC calls in repository layer for easy updates, monitor SDK releases
   - **Impact:** Medium - May require refactoring if API changes

2. **Risk:** Android 12+ lock screen widget API compatibility varies by device manufacturer
   - **Mitigation:** Test on multiple devices (Samsung, Google Pixel, OnePlus), document limitations
   - **Impact:** Low (Epic 2 concern) - Core app functionality works without widget

3. **Risk:** Health Connect may not be installed on all Android 9+ devices
   - **Mitigation:** Check availability, link to Play Store, clear messaging to user
   - **Impact:** Low - User can install HC, app guides them

**Assumptions:**

1. **Assumption:** Target device is Android 9+ (API 28+) with Google Play Services
   - **Validation:** Confirmed in PRD (minimum SDK = API 28)

2. **Assumption:** Health Connect stores both calories AND descriptions in NutritionRecord.name field
   - **Validation:** Confirmed in Health Connect SDK documentation (name is optional String field)

3. **Assumption:** Hilt is preferred over Koin for dependency injection
   - **Validation:** Confirmed in Architecture Document decision table

4. **Assumption:** No local database (Room) needed since Health Connect is single source of truth
   - **Validation:** Confirmed in Architecture Document, PRD specifies HC as data store

**Open Questions:**

1. **Question:** Should we implement Firebase Crashlytics in V1.0 or defer to V2.0?
   - **Decision Needed By:** Story 1.5 implementation
   - **Recommendation:** Configure ReleaseTree for Crashlytics but don't integrate in V1.0 (personal tool doesn't need it initially)

2. **Question:** What level of Health Connect error detail should we show users?
   - **Decision Needed By:** Story 1.5 implementation
   - **Recommendation:** Generic messages ("Unable to save nutrition data") + link to Settings, log technical details

3. **Question:** Should deep linking support http/https URLs or app-specific scheme?
   - **Decision Needed By:** Story 1.3 implementation
   - **Recommendation:** Use foodie:// scheme for widget → camera deep link

## Test Strategy Summary

**Unit Testing (JUnit + Mockito):**
- **Target Coverage:** 80%+ for repositories, use cases, ViewModels
- **Focus Areas:**
  - Repository implementations (mock HealthConnectManager)
  - Result<T> error handling paths
  - ViewModel state management
  - Use case business logic
- **Tooling:** JUnit 4.13.2, Mockito 5.14.2, Mockito-Kotlin 5.4.0, Truth assertions
- **Example Test:**
  ```kotlin
  @Test
  fun `insertMeal returns success when Health Connect saves record`() = runTest {
      val calories = 500
      val description = "Test meal"
      val timestamp = Instant.now()
      
      whenever(healthConnectManager.insertNutritionRecord(calories, description, timestamp))
          .thenReturn("record-id-123")
      
      val result = mealRepository.insertMeal(calories, description, timestamp)
      
      assertThat(result).isInstanceOf(Result.Success::class.java)
      verify(healthConnectManager).insertNutritionRecord(calories, description, timestamp)
  }
  ```

**Integration Testing (Instrumentation):**
- **Target Coverage:** Critical paths (Health Connect round-trip, navigation flows)
- **Focus Areas:**
  - Health Connect write + read validation
  - Navigation between screens with state preservation
  - Permission request flows
- **Tooling:** AndroidJUnit, Espresso, Compose UI Test
- **Example Test:**
  ```kotlin
  @Test
  fun healthConnect_writeAndRead_succeeds() = runTest {
      val calories = 650
      val description = "Grilled chicken"
      
      val recordId = healthConnectManager.insertNutritionRecord(
          calories, description, Instant.now()
      )
      
      val records = healthConnectManager.queryNutritionRecords()
      
      assertThat(records).isNotEmpty()
      assertThat(records.first().energy.inKilocalories).isEqualTo(calories.toDouble())
      assertThat(records.first().name).isEqualTo(description)
  }
  ```

**Manual Testing Checklist:**
- [ ] Build succeeds on fresh Android Studio install
- [ ] App launches on physical device (Android 9+)
- [ ] Health Connect availability check works when HC not installed
- [ ] Permission request flow completes successfully
- [ ] Sample Health Connect write appears in Google Fit app
- [ ] Navigation works between all placeholder screens
- [ ] Back button behavior correct (doesn't exit app on first back)
- [ ] Deep link intent navigates to correct screen
- [ ] Logs appear in Logcat with correct tags and levels
- [ ] Error messages are user-friendly (no stack traces shown to user)

**Performance Testing:**
- Measure cold start time with Android Studio Profiler (target < 2 seconds)
- Monitor memory usage during typical navigation flows (target < 100MB)
- Verify Health Connect query performance with 100+ mock records (target < 500ms)

**Continuous Integration (Future):**
- GitHub Actions workflow: Build + unit tests on every commit
- Automated APK generation for manual testing
- Code coverage reporting (target 80%+ for repositories and ViewModels)

---

**Epic Completion Criteria:**

This epic is considered complete when:
1. ✅ All 5 stories (1.1 - 1.5) pass their acceptance criteria
2. ✅ Project builds successfully with zero compilation errors or warnings
3. ✅ Health Connect integration demonstrates successful round-trip (write + read)
4. ✅ Sample MVVM stack validates architectural pattern
5. ✅ All unit tests pass with 80%+ coverage for core components
6. ✅ Manual testing checklist completed on physical device
7. ✅ Epic marked as "contexted" in sprint-status.yaml

**Next Steps After Epic 1:**
- Epic 2: AI-Powered Meal Capture (camera, Azure OpenAI, background processing)
- Technical debt to address: None expected from foundational epic
- Architecture validation: Review actual implementation against this spec
