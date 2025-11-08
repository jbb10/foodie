# Foodie - Architecture Document

## Executive Summary

Foodie is a native Android application built with a modern, client-only architecture using Jetpack Compose for UI, Hilt for dependency injection, and a clean MVVM pattern. The system integrates Azure OpenAI for AI-powered nutrition analysis and Google Health Connect as the single source of truth for data persistence, with WorkManager handling reliable background processing. All architectural decisions prioritize speed (sub-5-second capture flow), reliability (zero data loss), and AI agent implementation consistency.

## Project Initialization

### Create New Android Project

```bash
# Create new Android project via Android Studio:
# File → New → New Project → Empty Activity (Compose)
# 
# Configuration:
# - Name: Foodie
# - Package name: com.foodie.app
# - Save location: [project-root]
# - Language: Kotlin
# - Minimum SDK: API 28 (Android 9.0)
# - Build configuration language: Kotlin DSL
```

### Initial Dependencies Setup

After project creation, configure `build.gradle.kts` files with these core dependencies:

```kotlin
// Project-level build.gradle.kts
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "2.2.21-1.0.28" apply false
}

// App-level build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.foodie.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.foodie.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM for version alignment
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")
    
    // Kotlin Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    
    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // Datastore (for API key storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Timber for logging
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

## Decision Summary

| Category | Decision | Version | Affects Epics | Rationale |
| -------- | -------- | ------- | ------------- | --------- |
| UI Framework | Jetpack Compose | Compose BOM 2024.10.01 | All UI epics | Modern declarative UI, better state management, Google's recommended path, easier AI code generation |
| Dependency Injection | Hilt | 2.51.1 | All epics | Official Android DI, compile-time safety, excellent Jetpack integration |
| HTTP Client | Retrofit | 2.11.0 | AI Analysis | Industry standard, type-safe API calls, excellent error handling |
| AI Model | Azure OpenAI GPT-4.1 | Latest | AI Analysis | Vision-capable model for food image analysis and calorie estimation |
| Local Database | Health Connect Only | N/A (Health Connect stores all data) | Data Management | Official Android health platform, stores calories + descriptions in NutritionRecord, single source of truth |
| Image Capture | System Camera Intent | N/A (Android System) | Photo Capture | Fastest launch time, minimal code, leverages system camera |
| Background Processing | WorkManager | 2.9.1 | AI Processing | Reliable background work, handles retries, no foreground service needed |
| Async Processing | Kotlin Coroutines + Flow | 1.9.0 | All epics | Modern async, structured concurrency, reactive streams |
| Navigation | Jetpack Navigation Compose | 2.8.4 | Navigation | Type-safe navigation, deep linking support, Compose integration |
| State Management | ViewModel + StateFlow | Lifecycle 2.8.7 | All screens | Lifecycle-aware, survives config changes, reactive UI updates |
| Testing Framework | JUnit + Mockito | JUnit 4.13.2, Mockito 5.14.2 | All components | Industry standard, comprehensive mocking, coroutines support |
| Logging | Timber | 5.0.1 | All epics | Better than Log, production-safe, extensible |
| Secure Storage | EncryptedSharedPreferences | Security Crypto 1.1.0 | API Key | Hardware-backed encryption, simple API |
| Build System | Gradle with Kotlin DSL | 8.13 | Build | Type-safe build scripts, better IDE support |
| Language | Kotlin | 2.2.21 | All | Official Android language, null safety, coroutines |
| Android Gradle Plugin | AGP | 8.13.0 | Build | Latest stable, best performance |
| JDK | Java 17 | 17 | Build | Required for AGP 8.13 |

## Project Structure

```
foodie/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/foodie/app/
│   │   │   │   ├── FoodieApplication.kt              # Application class with Hilt
│   │   │   │   ├── MainActivity.kt                   # Single activity with Compose
│   │   │   │   │
│   │   │   │   ├── di/                               # Dependency Injection modules
│   │   │   │   │   ├── AppModule.kt                  # App-level dependencies
│   │   │   │   │   ├── NetworkModule.kt              # Retrofit, OkHttp
│   │   │   │   │   ├── RepositoryModule.kt           # Repository bindings
│   │   │   │   │   └── WorkManagerModule.kt          # WorkManager dependencies
│   │   │   │   │
│   │   │   │   ├── data/                             # Data layer
│   │   │   │   │   ├── local/                        # Local data sources
│   │   │   │   │   │   ├── datastore/
│   │   │   │   │   │   │   └── SecurePreferences.kt # API key storage
│   │   │   │   │   │   └── healthconnect/
│   │   │   │   │   │       └── HealthConnectManager.kt
│   │   │   │   │   │
│   │   │   │   │   ├── remote/                       # Remote data sources
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   │   └── AzureOpenAiApi.kt    # Retrofit interface
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── NutritionRequest.kt
│   │   │   │   │   │   │   └── NutritionResponse.kt
│   │   │   │   │   │   └── interceptor/
│   │   │   │   │   │       └── AuthInterceptor.kt   # API key injection
│   │   │   │   │   │
│   │   │   │   │   ├── worker/                       # WorkManager workers
│   │   │   │   │   │   └── AnalyzeMealWorker.kt     # Background AI analysis
│   │   │   │   │   │
│   │   │   │   │   └── repository/                   # Repository implementations
│   │   │   │   │       ├── MealRepository.kt
│   │   │   │   │       ├── NutritionAnalysisRepository.kt
│   │   │   │   │       └── HealthConnectRepository.kt
│   │   │   │   │
│   │   │   │   ├── domain/                           # Business logic layer
│   │   │   │   │   ├── model/                        # Domain models
│   │   │   │   │   │   ├── MealEntry.kt
│   │   │   │   │   │   ├── NutritionData.kt
│   │   │   │   │   │   └── AnalysisStatus.kt
│   │   │   │   │   └── usecase/                      # Use cases
│   │   │   │   │       ├── CaptureMealUseCase.kt
│   │   │   │   │       ├── GetMealHistoryUseCase.kt
│   │   │   │   │       ├── UpdateMealEntryUseCase.kt
│   │   │   │   │       └── DeleteMealEntryUseCase.kt
│   │   │   │   │
│   │   │   │   ├── ui/                               # Presentation layer
│   │   │   │   │   ├── theme/                        # Compose theme
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   │
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavGraph.kt              # Navigation routes
│   │   │   │   │   │
│   │   │   │   │   ├── components/                   # Reusable UI components
│   │   │   │   │   │   ├── MealEntryCard.kt
│   │   │   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   │   │   └── ErrorMessage.kt
│   │   │   │   │   │
│   │   │   │   │   └── screens/                      # Screen implementations
│   │   │   │   │       ├── meallist/
│   │   │   │   │       │   ├── MealListScreen.kt
│   │   │   │   │       │   ├── MealListViewModel.kt
│   │   │   │   │       │   └── MealListState.kt
│   │   │   │   │       ├── mealdetail/
│   │   │   │   │       │   ├── MealDetailScreen.kt
│   │   │   │   │       │   ├── MealDetailViewModel.kt
│   │   │   │   │       │   └── MealDetailState.kt
│   │   │   │   │       └── settings/
│   │   │   │   │           ├── SettingsScreen.kt
│   │   │   │   │           ├── SettingsViewModel.kt
│   │   │   │   │           └── SettingsState.kt
│   │   │   │   │
│   │   │   │   └── util/                             # Utilities
│   │   │   │       ├── Constants.kt
│   │   │   │       ├── DateFormatter.kt
│   │   │   │       ├── ImageUtils.kt
│   │   │   │       └── Result.kt                     # Result wrapper for error handling
│   │   │   │
│   │   │   ├── res/                                  # Android resources
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── drawable/
│   │   │   │   └── xml/
│   │   │   │       └── glance_widget.xml             # Lock screen widget config
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/                                     # Unit tests
│   │   │   └── java/com/foodie/app/
│   │   │       ├── data/
│   │   │       │   └── repository/
│   │   │       │       └── MealRepositoryTest.kt
│   │   │       ├── domain/
│   │   │       │   └── usecase/
│   │   │       │       └── CaptureMealUseCaseTest.kt
│   │   │       └── ui/
│   │   │           └── viewmodel/
│   │   │               └── MealListViewModelTest.kt
│   │   │
│   │   └── androidTest/                              # Instrumentation tests
│   │       └── java/com/foodie/app/
│   │           └── ui/
│   │               └── MealListScreenTest.kt
│   │
│   ├── build.gradle.kts                              # App-level build config
│   └── proguard-rules.pro                            # ProGuard rules
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                                   # Project-level build config
├── gradle.properties
├── settings.gradle.kts
└── local.properties                                   # API keys (gitignored)
```

## Epic to Architecture Mapping

| Epic | Architecture Components | Data Flow |
| ---- | ----------------------- | --------- |
| **Meal Capture** | System Camera Intent → ImageUtils → AnalyzeMealWorker → AzureOpenAiApi → MealRepository → Health Connect | User taps widget → Camera launched → Photo captured → WorkManager queues analysis → Background processing → Results saved to Health Connect |
| **Data Management (List/Edit/Delete)** | MealListScreen → MealListViewModel → MealRepository → HealthConnectManager | ViewModel exposes StateFlow<MealListState> → Screen collects and renders → User actions trigger repository operations → Health Connect updated → Flow emits new state |
| **Settings & API Key** | SettingsScreen → SettingsViewModel → SecurePreferences (EncryptedSharedPreferences) | User enters API key → ViewModel validates → Encrypted storage → Used by AuthInterceptor in Retrofit |

## Technology Stack Details

### Core Technologies

#### Jetpack Compose (UI Framework)
- **Version:** Compose BOM 2024.10.01
- **Purpose:** Declarative UI framework for all screens
- **Key Libraries:**
  - `androidx.compose.ui` - Core UI toolkit
  - `androidx.compose.material3` - Material Design 3 components
  - `androidx.activity.activity-compose` - Activity integration
  - `androidx.lifecycle.lifecycle-runtime-compose` - Lifecycle integration

#### Hilt (Dependency Injection)
- **Version:** 2.51.1
- **Purpose:** Compile-time dependency injection across all layers
- **Modules:**
  - `AppModule` - Application-scoped dependencies (Context, SecurePreferences)
  - `NetworkModule` - Retrofit, OkHttp, API interfaces
  - `RepositoryModule` - Repository implementations
  - `WorkManagerModule` - Worker factory configuration

#### Retrofit (HTTP Client)
- **Version:** 2.11.0
- **Purpose:** Type-safe HTTP client for Azure OpenAI API
- **Configuration:**
  - Base URL: Azure OpenAI endpoint (from SecurePreferences)
  - Converters: Gson for JSON serialization
  - Interceptors: AuthInterceptor (API key), LoggingInterceptor (debug only)
  - Timeout: 30 seconds for AI analysis calls

#### WorkManager (Background Processing)
- **Version:** 2.9.1
- **Purpose:** Reliable background processing for AI analysis
- **Workers:**
  - `AnalyzeMealWorker` - Analyzes photo, calls Azure OpenAI, saves to Health Connect
- **Constraints:** NetworkType.CONNECTED
- **Retry Policy:** Exponential backoff, max 3 retries
- **No Foreground Service:** WorkManager handles background reliability

#### Kotlin Coroutines + Flow
- **Version:** 1.9.0
- **Purpose:** Asynchronous programming and reactive data streams
- **Patterns:**
  - Repository functions return `Flow<Result<T>>` for continuous updates
  - ViewModels collect Flows and expose `StateFlow<ScreenState>`
  - Use `viewModelScope` for coroutine lifecycle management

### Integration Points

#### Azure OpenAI API
- **Model:** GPT-4.1 (vision-capable model for food analysis)
- **Endpoint:** `https://{your-resource-name}.openai.azure.com/openai/deployments/{deployment-id}/chat/completions`
- **API Version:** 2024-10-21 (latest GA as of Nov 2025)
- **Authentication:** API key in `api-key` header (NOT `Authorization: Bearer`)
- **Request Format:** Chat completions with multimodal content (text + image_url with base64)
- **Response Parsing:** Extract `choices[0].message.content` and parse as JSON for `calories: Int` and `description: String`
- **Error Handling:** Network errors trigger WorkManager retry, API errors logged and user notified

#### Health Connect (Data Storage)
- **Version:** 1.1.0-alpha10
- **Integration:** `HealthConnectManager` wrapper class
- **Operations:**
  - Write: `insertNutritionRecord(calories, description, timestamp)`
  - Read: `queryNutritionRecords(startTime, endTime)`
  - Update: `deleteNutritionRecord(recordId)` + `insertNutritionRecord()` (delete and re-insert pattern)
  - Delete: `deleteNutritionRecord(recordId)`
- **Permissions:** Requested on first app launch via HealthConnectClient
- **Data Model:** 
  - `energy` field for calories
  - `name` field for food description
  - `startTime` / `endTime` for meal timestamp
  - `mealType` for categorization (V2.0)
- **Single Source of Truth:** Health Connect stores all nutrition data (calories + descriptions + timestamps)

#### System Camera
- **Integration:** `ActivityResultContract` with `MediaStore.ACTION_IMAGE_CAPTURE`
- **Flow:** 
  1. Widget/FAB tap triggers camera intent
  2. User captures photo → saved to app cache directory
  3. Uri returned to activity → passed to WorkManager
  4. WorkManager reads image, analyzes, then deletes file
- **No CameraX needed:** System camera provides fastest launch time

## Implementation Patterns

### Naming Conventions

**Naming Conventions:**
- Files: `MainActivity.kt`
- Screens: `MealListScreen.kt`
- ViewModels: `MealListViewModel.kt`
- State classes: `MealListState.kt`
- Repositories: `MealRepository.kt`
- Workers: `AnalyzeMealWorker.kt`
- Use cases: `CaptureMealUseCase.kt`

**Packages:**
- All lowercase, no underscores: `com.foodie.app.data.local.db`

**Classes:**
- PascalCase: `MealRepository`, `HealthConnectManager`
- Interfaces: Same as implementations, no "I" prefix

**Functions:**
- camelCase: `getMealHistory()`, `analyzeMeal()`
- Suspend functions: No special prefix, rely on IDE indicators

**Variables:**
- camelCase: `mealEntry`, `apiKey`, `photoUri`
- Constants: UPPER_SNAKE_CASE in `Constants.kt` object
- Private properties: No underscore prefix (Kotlin convention)

**Composables:**
- PascalCase: `MealEntryCard()`, `LoadingIndicator()`
- Preview functions: `MealListScreenPreview()`

### Code Organization Patterns

**Layer Architecture (Clean Architecture):**
```
UI Layer (screens, viewmodels, state)
    ↓ (StateFlow/Events)
Domain Layer (use cases, domain models)
    ↓ (Repository interfaces)
Data Layer (repository impls, data sources, entities)
```

**Dependency Rules:**
- UI depends on Domain
- Domain depends on Data (interfaces only)
- Data has no dependencies on UI/Domain
- All layers use Hilt for DI

**Repository Pattern:**
```kotlin
interface MealRepository {
    fun getMealHistory(): Flow<Result<List<MealEntry>>>
    suspend fun updateMeal(id: String, calories: Int, description: String): Result<Unit>
    suspend fun deleteMeal(id: String): Result<Unit>
}

class MealRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) : MealRepository {
    // Implementation delegates to Health Connect
    override fun getMealHistory(): Flow<Result<List<MealEntry>>> = flow {
        try {
            val records = healthConnectManager.queryNutritionRecords()
            emit(Result.success(records))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
```

**ViewModel Pattern:**
```kotlin
@HiltViewModel
class MealListViewModel @Inject constructor(
    private val getMealHistoryUseCase: GetMealHistoryUseCase,
    private val deleteMealUseCase: DeleteMealEntryUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(MealListState())
    val state: StateFlow<MealListState> = _state.asStateFlow()
    
    init {
        loadMeals()
    }
    
    private fun loadMeals() {
        viewModelScope.launch {
            getMealHistoryUseCase()
                .collect { result ->
                    _state.update { it.copy(
                        meals = result.getOrNull() ?: emptyList(),
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )}
                }
        }
    }
    
    fun onDeleteMeal(id: String) {
        viewModelScope.launch {
            deleteMealUseCase(id).fold(
                onSuccess = { loadMeals() },
                onFailure = { _state.update { it.copy(error = it.message) } }
            )
        }
    }
}
```

**Screen Pattern:**
```kotlin
@Composable
fun MealListScreen(
    viewModel: MealListViewModel = hiltViewModel(),
    onMealClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    MealListContent(
        state = state,
        onMealClick = onMealClick,
        onDeleteMeal = viewModel::onDeleteMeal,
        onRefresh = viewModel::refresh
    )
}

@Composable
private fun MealListContent(
    state: MealListState,
    onMealClick: (String) -> Unit,
    onDeleteMeal: (String) -> Unit,
    onRefresh: () -> Unit
) {
    // Stateless composable - easier to preview and test
}
```

### Error Handling Strategy

**Result Wrapper Pattern:**
```kotlin
// util/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data class Loading(val progress: Float? = null) : Result<Nothing>()
}

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Success -> data
    else -> null
}

fun <T> Result<T>.exceptionOrNull(): Throwable? = when (this) {
    is Error -> exception
    else -> null
}
```

**Repository Error Handling:**
```kotlin
override suspend fun updateMeal(id: String, calories: Int, description: String): Result<Unit> {
    return try {
        // Update Health Connect using delete + re-insert pattern
        healthConnectManager.deleteNutritionRecord(id)
        healthConnectManager.insertNutritionRecord(calories, description, timestamp)
        
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to update meal: $id")
        Result.Error(e)
    }
}
```

**Network Error Handling:**
```kotlin
// In AnalyzeMealWorker
override suspend fun doWork(): Result {
    return try {
        val photoUri = inputData.getString(KEY_PHOTO_URI) ?: return Result.failure()
        
        val response = azureOpenAiApi.analyzeNutrition(
            buildRequest(photoUri)
        )
        
        // Validate response
        if (response.calories !in 1..5000) {
            Timber.w("Invalid calorie estimate: ${response.calories}")
            return Result.failure(
                workDataOf(KEY_ERROR to "Invalid nutrition data")
            )
        }
        
        // Save to Health Connect
        saveMealEntry(response)
        
        // Delete photo
        deletePhoto(photoUri)
        
        Result.success()
        
    } catch (e: IOException) {
        Timber.e(e, "Network error analyzing meal")
        if (runAttemptCount < 3) {
            Result.retry() // WorkManager will retry with exponential backoff
        } else {
            Result.failure(workDataOf(KEY_ERROR to "Network error after 3 attempts"))
        }
    } catch (e: Exception) {
        Timber.e(e, "Unexpected error analyzing meal")
        Result.failure(workDataOf(KEY_ERROR to e.message))
    }
}
```

**UI Error Display:**
```kotlin
@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Snackbar(
        action = {
            onRetry?.let {
                TextButton(onClick = it) {
                    Text("Retry")
                }
            }
        },
        dismissAction = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Dismiss")
            }
        }
    ) {
        Text(message)
    }
}
```

### Logging Strategy

**Timber Configuration:**
```kotlin
// FoodieApplication.kt
class FoodieApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            // Send to crash reporting service (e.g., Firebase Crashlytics)
            // For V1.0: Just system log, no external service
        }
    }
}
```

**Logging Conventions:**
```kotlin
// Repository layer - log business logic errors
Timber.e(exception, "Failed to save meal to Health Connect")

// ViewModel layer - log user action failures
Timber.w("User attempted to delete non-existent meal: $mealId")

// Worker layer - log background processing
Timber.d("Starting meal analysis for photo: $photoUri")
Timber.i("Meal analysis completed: ${response.calories} cal")

// Network layer - use OkHttp LoggingInterceptor (debug only)
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}

// NEVER log sensitive data
// ❌ Timber.d("API Key: $apiKey")
// ❌ Timber.d("User photo: $base64Image")
// ✅ Timber.d("API call initiated")
```

## Data Architecture

### Health Connect Data Model

All nutrition data is stored exclusively in Health Connect, leveraging the `NutritionRecord` class:

```kotlin
// Health Connect stores:
// - energy (calories) via Energy field
// - name (food description) via name field
// - startTime / endTime (timestamp) via ZonedDateTime
// - mealType (categorization) for V2.0

// Example usage in HealthConnectManager
suspend fun insertNutritionRecord(
    calories: Int,
    description: String,
    timestamp: Instant
) {
    val record = NutritionRecord(
        energy = Energy.calories(calories.toDouble()),
        name = description,
        startTime = timestamp,
        endTime = timestamp,
        startZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(timestamp),
        endZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(timestamp)
    )
    
    healthConnectClient.insertRecords(listOf(record))
}

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

// Update pattern: delete + re-insert (Health Connect doesn't support direct updates)
suspend fun updateNutritionRecord(
    recordId: String,
    calories: Int,
    description: String,
    timestamp: Instant
) {
    deleteNutritionRecord(recordId)
    insertNutritionRecord(calories, description, timestamp)
}
```

### Domain Models

```kotlin
// Domain layer models (not tied to Health Connect or API)
data class MealEntry(
    val id: String,
    val timestamp: Instant,
    val description: String,
    val calories: Int
)

data class NutritionData(
    val calories: Int,
    val description: String
) {
    init {
        require(calories in 1..5000) { "Calories must be between 1 and 5000" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}
```

### Data Flow Diagram

```
User Action (Capture Photo)
    ↓
System Camera Intent
    ↓
Photo saved to cache (app/cache/photos/)
    ↓
WorkManager.enqueue(AnalyzeMealWorker)
    ↓
[Background] AnalyzeMealWorker
    ├─→ Read photo from cache
    ├─→ Base64 encode image
    ├─→ Retrofit call to Azure OpenAI
    ├─→ Parse response (calories, description)
    ├─→ Save to Health Connect (NutritionRecord with energy + name)
    └─→ Delete photo from cache
         ↓
Health Connect stores data permanently
    ↓
Repository queries Health Connect
    ↓
Repository Flow emits Result<List<MealEntry>>
    ↓
ViewModel StateFlow updates
    ↓
Compose UI recomposes with new data
```

## API Contracts

### Azure OpenAI API

**IMPORTANT:** This application uses **Azure OpenAI Service**, NOT OpenAI's public API. The endpoint structure and authentication differ from the standard OpenAI API.

**Model:** GPT-4.1 (deployed in Azure OpenAI Service)

**Retrofit Interface:**
```kotlin
interface AzureOpenAiApi {
    @POST("openai/deployments/{deploymentId}/chat/completions")
    suspend fun analyzeNutrition(
        @Path("deploymentId") deploymentId: String,  // "gpt-4.1" deployment
        @Query("api-version") apiVersion: String = "2024-10-21",
        @Body request: AzureChatCompletionRequest
    ): AzureChatCompletionResponse
}
```

**Request DTO:**
```kotlin
data class AzureChatCompletionRequest(
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat = ResponseFormat(type = "json_object"),
    val max_tokens: Int = 500,
    val temperature: Double = 1.0
)

data class ChatMessage(
    val role: String,  // "system", "user", "assistant"
    val content: List<MessageContent>
)

data class MessageContent(
    val type: String,  // "text" or "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String,  // "data:image/jpeg;base64,{BASE64_IMAGE}"
    val detail: String = "auto"  // "auto", "low", or "high"
)

data class ResponseFormat(
    val type: String  // "json_object" enables JSON mode
)
```

**Response DTO:**
```kotlin
data class AzureChatCompletionResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finish_reason: String  // "stop", "length", etc.
)

data class ResponseMessage(
    val role: String,  // "assistant"
    val content: String  // JSON string: {"calories": 650, "description": "..."}
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// Parsed nutrition data
data class ParsedNutrition(
    val calories: Int,
    val description: String
)
```

**Example Request to Azure OpenAI:**
```http
POST https://{your-resource-name}.openai.azure.com/openai/deployments/{deployment-id}/chat/completions?api-version=2024-10-21
api-key: {your-api-key}
Content-Type: application/json

{
  "messages": [
    {
      "role": "system",
      "content": [
        {
          "type": "text",
          "text": "You are a nutrition analysis assistant. Analyze the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food)."
        }
      ]
    },
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Analyze this meal and estimate total calories."
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
            "detail": "auto"
          }
        }
      ]
    }
  ],
  "response_format": {
    "type": "json_object"
  },
  "max_tokens": 500
}
```

**Example Response:**
```json
{
  "id": "chatcmpl-7R1nGnsXO8n4oi9UPz2f3UHdgAYMn",
  "created": 1686676106,
  "model": "gpt-4.1",
  "choices": [
    {
      "index": 0,
      "finish_reason": "stop",
      "message": {
        "role": "assistant",
        "content": "{\"calories\": 650, \"description\": \"Grilled chicken breast with steamed broccoli and brown rice\"}"
      }
    }
  ],
  "usage": {
    "prompt_tokens": 1245,
    "completion_tokens": 25,
    "total_tokens": 1270
  }
}
```

### Health Connect API

**Operations:**
```kotlin
class HealthConnectManager @Inject constructor(
    private val context: Context
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }
    
    suspend fun insertNutritionRecord(
        calories: Int,
        timestamp: Instant
    ): String {
        val record = NutritionRecord(
            energy = Energy.kilocalories(calories.toDouble()),
            startTime = timestamp,
            endTime = timestamp,
            startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
            endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
        )
        
        val response = healthConnectClient.insertRecords(listOf(record))
        return response.recordIdsList.first()  // Health Connect record ID
    }
    
    suspend fun updateNutritionRecord(
        recordId: String,
        newCalories: Int
    ) {
        // Health Connect doesn't support direct updates
        // Must delete and re-insert with same timestamp
        val oldRecord = queryRecordById(recordId)
        deleteNutritionRecord(recordId)
        insertNutritionRecord(newCalories, oldRecord.startTime)
    }
    
    suspend fun deleteNutritionRecord(recordId: String) {
        healthConnectClient.deleteRecords(
            recordIdsList = listOf(recordId),
            clientRecordIdsList = emptyList()
        )
    }
    
    suspend fun queryNutritionRecords(
        startTime: Instant,
        endTime: Instant
    ): List<NutritionRecord> {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }
}
```

## Security Architecture

### API Key Storage

**EncryptedSharedPreferences for Azure OpenAI Configuration:**
```kotlin
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "foodie_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Azure OpenAI requires: resource name, deployment ID, and API key
    fun saveAzureConfig(resourceName: String, deploymentId: String, apiKey: String) {
        sharedPreferences.edit()
            .putString(KEY_AZURE_RESOURCE_NAME, resourceName)
            .putString(KEY_AZURE_DEPLOYMENT_ID, deploymentId)
            .putString(KEY_AZURE_API_KEY, apiKey)
            .apply()
    }
    
    fun getAzureEndpoint(): String? {
        val resourceName = sharedPreferences.getString(KEY_AZURE_RESOURCE_NAME, null)
        return resourceName?.let { "https://$it.openai.azure.com/" }
    }
    
    fun getDeploymentId(): String? {
        return sharedPreferences.getString(KEY_AZURE_DEPLOYMENT_ID, null)
    }
    
    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_AZURE_API_KEY, null)
    }
    
    fun clearAzureConfig() {
        sharedPreferences.edit()
            .remove(KEY_AZURE_RESOURCE_NAME)
            .remove(KEY_AZURE_DEPLOYMENT_ID)
            .remove(KEY_AZURE_API_KEY)
            .apply()
    }
    
    companion object {
        private const val KEY_AZURE_RESOURCE_NAME = "azure_resource_name"
        private const val KEY_AZURE_DEPLOYMENT_ID = "azure_deployment_id"
        private const val KEY_AZURE_API_KEY = "azure_api_key"
    }
}
```

**Azure OpenAI Configuration Requirements:**
- **Resource Name:** Your Azure OpenAI resource name (e.g., "my-openai-resource")
- **Deployment ID:** The name of your GPT-4.1 model deployment (e.g., "gpt-4.1" or "gpt41")
- **API Key:** Found in Azure Portal → Azure OpenAI Service → Keys and Endpoint
- **Endpoint Format:** `https://{resource-name}.openai.azure.com/`
- **API Version:** 2024-10-21 (hardcoded in API interface)
- **Model:** GPT-4.1 with vision capabilities for multimodal food analysis

### Network Security

**HTTPS Only with Azure-Specific Headers:**
```kotlin
// NetworkModule.kt
@Provides
@Singleton
fun provideOkHttpClient(
    azureAuthInterceptor: AzureAuthInterceptor,
    loggingInterceptor: HttpLoggingInterceptor
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(azureAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}

// AzureAuthInterceptor.kt (Note: Azure OpenAI uses 'api-key' header, not 'Authorization')
class AzureAuthInterceptor @Inject constructor(
    private val securePreferences: SecurePreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = securePreferences.getApiKey()
            ?: throw IllegalStateException("Azure OpenAI API key not configured")
        
        val request = chain.request().newBuilder()
            .addHeader("api-key", apiKey)  // Azure OpenAI specific header
            .addHeader("Content-Type", "application/json")
            .build()
        
        return chain.proceed(request)
    }
}

// Retrofit configuration
@Provides
@Singleton
fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    val azureEndpoint = securePreferences.getAzureEndpoint()
        ?: "https://your-resource-name.openai.azure.com/"
    
    return Retrofit.Builder()
        .baseUrl(azureEndpoint)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

### Photo Privacy

**Ephemeral Storage:**
```kotlin
object ImageUtils {
    fun createTempPhotoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = File(context.cacheDir, "photos")
        storageDir.mkdirs()
        
        return File(storageDir, "MEAL_$timeStamp.jpg")
    }
    
    fun deletePhoto(photoUri: Uri) {
        try {
            val file = File(photoUri.path ?: return)
            if (file.exists()) {
                file.delete()
                Timber.d("Deleted photo: ${file.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete photo")
        }
    }
    
    fun encodeImageToBase64(photoUri: Uri, context: Context): String {
        val inputStream = context.contentResolver.openInputStream(photoUri)
            ?: throw IOException("Cannot open photo")
        
        val bytes = inputStream.use { it.readBytes() }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
```

**Photo Lifecycle:**
1. Camera captures → saved to `app/cache/photos/MEAL_*.jpg`
2. WorkManager reads photo → encodes to base64
3. API call completes successfully → photo deleted immediately
4. API call fails → photo retained for retry
5. After 3 failed retries → photo deleted to avoid cache bloat

## Performance Considerations

### Camera Launch Optimization

**Fast Widget Launch:**
```kotlin
// Widget implementation uses direct intent
val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
}
startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
```

**Target: < 500ms from widget tap to camera ready**

### Background Processing Strategy

**WorkManager Configuration:**
```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

val analyzeWork = OneTimeWorkRequestBuilder<AnalyzeMealWorker>()
    .setConstraints(constraints)
    .setInputData(workDataOf(
        AnalyzeMealWorker.KEY_PHOTO_URI to photoUri.toString(),
        AnalyzeMealWorker.KEY_MEAL_ID to mealId
    ))
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()

workManager.enqueueUniqueWork(
    "analyze_meal_$mealId",
    ExistingWorkPolicy.REPLACE,
    analyzeWork
)
```

**No Foreground Service:**
- WorkManager handles battery optimization automatically
- No persistent notification during processing
- Retry logic built-in with exponential backoff
- Device sleep doesn't interrupt processing

### Query Performance

**Health Connect Query Optimization:**
```kotlin
// Query with time range filter for better performance
suspend fun getMealHistory(days: Int = 30): List<NutritionRecord> {
    val endTime = Instant.now()
    val startTime = endTime.minus(days.toLong(), ChronoUnit.DAYS)
    
    val request = ReadRecordsRequest(
        recordType = NutritionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
    )
    
    return healthConnectClient.readRecords(request).records
        .sortedByDescending { it.startTime }
}

// Use pagination for large datasets (V2.0)
suspend fun getMealsPaged(pageSize: Int, pageToken: String?): ReadRecordsResponse<NutritionRecord> {
    val request = ReadRecordsRequest(
        recordType = NutritionRecord::class,
        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        pageSize = pageSize,
        pageToken = pageToken
    )
    
    return healthConnectClient.readRecords(request)
}
```

### Memory Management

**Image Handling:**
```kotlin
// Compress images before encoding to base64
fun compressImage(photoUri: Uri, context: Context): ByteArray {
    val bitmap = BitmapFactory.decodeStream(
        context.contentResolver.openInputStream(photoUri)
    )
    
    // Scale down to max 2MP
    val maxDimension = 1600
    val scale = minOf(
        maxDimension.toFloat() / bitmap.width,
        maxDimension.toFloat() / bitmap.height,
        1f
    )
    
    val scaledBitmap = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt(),
        (bitmap.height * scale).toInt(),
        true
    )
    
    // Compress to JPEG 80%
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    
    bitmap.recycle()
    scaledBitmap.recycle()
    
    return outputStream.toByteArray()
}
```

## Testing Strategy

### Unit Tests (JUnit + Mockito)

**ViewModel Tests:**
```kotlin
@ExperimentalCoroutinesApi
class MealListViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var getMealHistoryUseCase: GetMealHistoryUseCase
    
    @Mock
    private lateinit var deleteMealUseCase: DeleteMealEntryUseCase
    
    private lateinit var viewModel: MealListViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = MealListViewModel(getMealHistoryUseCase, deleteMealUseCase)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadMeals should update state with meal list`() = runTest {
        // Given
        val mockMeals = listOf(
            MealEntry("1", Instant.now(), "Chicken", 500)
        )
        whenever(getMealHistoryUseCase()).thenReturn(flowOf(Result.Success(mockMeals)))
        
        // When
        viewModel.loadMeals()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assertEquals(mockMeals, state.meals)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }
    
    @Test
    fun `deleteMeal should call use case and reload meals`() = runTest {
        // Given
        val mealId = "123"
        whenever(deleteMealUseCase(mealId)).thenReturn(Result.Success(Unit))
        
        // When
        viewModel.onDeleteMeal(mealId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(deleteMealUseCase).invoke(mealId)
        verify(getMealHistoryUseCase, times(2)).invoke() // init + after delete
    }
}
```

**Repository Tests:**
```kotlin
class MealRepositoryTest {
    
    @Mock
    private lateinit var healthConnectManager: HealthConnectManager
    
    private lateinit var repository: MealRepositoryImpl
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = MealRepositoryImpl(healthConnectManager)
    }
    
    @Test
    fun `updateMeal should delete and re-insert in Health Connect`() = runTest {
        // Given
        val mealId = "123"
        val calories = 500
        val description = "Updated meal"
        val timestamp = Instant.now()
        
        // When
        val result = repository.updateMeal(mealId, calories, description)
        
        // Then
        assertTrue(result is Result.Success)
        verify(healthConnectManager).deleteNutritionRecord(mealId)
        verify(healthConnectManager).insertNutritionRecord(eq(calories), eq(description), any())
    }
    
    @Test
    fun `updateMeal should return error when Health Connect fails`() = runTest {
        // Given
        val mealId = "123"
        val exception = IOException("Network error")
        whenever(healthConnectManager.updateNutritionRecord(any(), any()))
            .thenThrow(exception)
        
        // When
        val result = repository.updateMeal(mealId, 500, "Test")
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
}
```

### Instrumentation Tests

**Health Connect Integration Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class HealthConnectManagerTest {
    
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var manager: HealthConnectManager
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        healthConnectClient = HealthConnectClient.getOrCreate(context)
        manager = HealthConnectManager(healthConnectClient)
    }
    
    @Test
    fun insertNutritionRecordAndRetrieve() = runTest {
        // Given
        val calories = 500
        val description = "Test meal"
        val timestamp = Instant.now()
        
        // When
        manager.insertNutritionRecord(calories, description, timestamp)
        val records = manager.queryNutritionRecords()
        
        // Then
        val insertedRecord = records.firstOrNull { it.name == description }
        assertNotNull(insertedRecord)
        assertEquals(calories.toDouble(), insertedRecord?.energy?.inCalories)
        assertEquals(description, insertedRecord?.name)
    }
    
    @Test
    fun updateNutritionRecordDeletesAndReinserts() = runTest {
        // Given
        val originalCalories = 500
        val updatedCalories = 600
        val description = "Updated meal"
        val timestamp = Instant.now()
        
        // Insert original record
        manager.insertNutritionRecord(originalCalories, "Original", timestamp)
        val records = manager.queryNutritionRecords()
        val recordId = records.first().metadata.id
        
        // When
        manager.updateNutritionRecord(recordId, updatedCalories, description, timestamp)
        
        // Then
        val updatedRecords = manager.queryNutritionRecords()
        val updatedRecord = updatedRecords.firstOrNull { it.name == description }
        assertNotNull(updatedRecord)
        assertEquals(updatedCalories.toDouble(), updatedRecord?.energy?.inCalories)
    }
}
```

### Test Coverage Target

- **ViewModels:** 80%+ (business logic critical)
- **Repositories:** 80%+ (data orchestration critical)
- **Use Cases:** 70%+ (domain logic important)
- **Workers:** 60%+ (complex retry logic)
- **UI Components:** 30%+ (visual testing more important)
- **Overall:** 60%+ coverage

### User Story Testing Requirements

**CRITICAL RULE:** All user stories MUST include unit tests as part of their Definition of Done. A story cannot be marked complete without corresponding unit tests.

**Testing Requirements by Story Type:**

**Business Logic Stories (ViewModels, Use Cases, Repositories):**
- MUST have unit tests covering all logic paths
- MUST test edge cases and error scenarios
- MUST use Mockito to mock dependencies
- MUST achieve minimum 80% code coverage for new code
- Tests are written BEFORE the story is considered complete

**UI Stories (Compose Screens, Components):**
- MUST have unit tests for ViewModel logic
- SHOULD have Compose UI tests for critical interactions
- MUST test state changes and user input handling
- UI logic (state management, validation) requires unit tests

**Data Layer Stories (Health Connect, Workers, API Integration):**
- MUST have unit tests for data transformations
- MUST have instrumentation tests for Health Connect integration
- MUST test error handling and retry logic
- MUST mock external dependencies (API, Health Connect)

**Integration Stories:**
- MUST have unit tests for integration logic
- SHOULD have integration tests if crossing boundaries
- MUST test error propagation between layers

**Story Acceptance Criteria Template:**
Every user story MUST include:
```
Acceptance Criteria:
- [ ] Feature implemented according to requirements
- [ ] Unit tests written for all new business logic
- [ ] Tests pass (JUnit tests green)
- [ ] Code coverage meets minimum threshold (80% for logic)
- [ ] Edge cases and error scenarios tested
- [ ] Code reviewed and approved
```

**Example Story with Testing:**
```
Story: As a user, I want to edit meal calories so I can correct AI estimates

Acceptance Criteria:
- [ ] Edit screen displays current calories and description
- [ ] User can modify calories (numeric input, 1-5000 range)
- [ ] Save updates Health Connect using delete + re-insert pattern
- [ ] Error handling for Health Connect failures
- [ ] Unit tests for MealDetailViewModel.updateMeal()
- [ ] Unit tests for MealRepository.updateMeal()
- [ ] Unit tests for edge cases: invalid input, network errors
- [ ] Code coverage: 80%+ for new ViewModel and Repository code
```

**No Exceptions:** Stories without tests cannot be deployed or merged. This ensures code quality and prevents regression bugs as AI agents implement features.

## Development Environment

### Prerequisites

- **IDE:** VS Code (NOT Android Studio)
- **JDK:** 17 (required for AGP 8.13.0)
- **Android SDK:** Command Line Tools
- **Development Machine:** macOS (Apple Silicon M2 Pro)
- **Primary Test Device:** Android Emulator (API 35, arm64-v8a)
- **Physical Testing:** Google Pixel 8 Pro (optional, wireless ADB)
- **Gradle:** 8.13 (via Gradle Wrapper)
- **Kotlin:** 2.2.21 (configured in build.gradle.kts)

### VS Code Extensions

Install required extensions for Android/Kotlin development:

```bash
code --install-extension mathiasfrohlich.Kotlin
code --install-extension vscjava.vscode-gradle
code --install-extension redhat.java
code --install-extension richardwillis.vscode-gradle-extension-pack
```

**VS Code Settings (.vscode/settings.json):**
```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home",
      "default": true
    }
  ],
  "gradle.nestedProjects": true,
  "files.exclude": {
    "**/.gradle": true,
    "**/build": true
  }
}
```

### Android SDK Setup (Command Line)

```bash
# Download Android Command Line Tools from:
# https://developer.android.com/studio#command-tools
# Extract to: ~/Library/Android/sdk/cmdline-tools/latest

# Add to ~/.zshrc
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator

# Reload shell
source ~/.zshrc

# Install required SDK components
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager "platforms;android-28"  # Min SDK for Health Connect

# Accept licenses
sdkmanager --licenses
```

### Android Emulator Setup

```bash
# Install emulator and system image (ARM64 for M2 Pro)
sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"

# Create AVD (Android Virtual Device)
avdmanager create avd \
  --name Pixel_8_API_35 \
  --package "system-images;android-35;google_apis;arm64-v8a" \
  --device "pixel_8"

# List available AVDs
avdmanager list avd

# Launch emulator
emulator -avd Pixel_8_API_35 &

# Or launch with specific settings (more RAM, disable audio)
emulator -avd Pixel_8_API_35 -memory 4096 -no-audio &

# Verify emulator is running
adb devices
# Should show: emulator-5554    device
```

**Emulator Configuration Recommendations:**
- **RAM:** 4GB (faster performance on M2 Pro)
- **Storage:** 2GB internal storage
- **Graphics:** Automatic (hardware acceleration on M2)
- **Camera:** Webcam passthrough for realistic meal photo testing
- **Google Play Services:** Included (required for Health Connect)

**Common Emulator Commands:**

```bash
# Start emulator in background
emulator -avd Pixel_8_API_35 -no-audio &

# Start emulator with wiped data (fresh state)
emulator -avd Pixel_8_API_35 -wipe-data &

# List running emulators
adb devices

# Stop emulator
adb -s emulator-5554 emu kill

# Install APK to emulator
adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk

# Take emulator snapshot (quick restart)
# In emulator: Settings → Extended Controls → Snapshots → Save
```

### Wireless ADB Setup (Pixel 8 Pro - Optional for Real Device Testing)

**One-time setup (requires initial USB connection):**

```bash
# 1. On Pixel 8 Pro: Enable Developer Options
#    Settings → About Phone → Tap "Build Number" 7 times
#    Settings → System → Developer Options → USB Debugging → ON

# 2. Connect via USB initially to authorize computer
adb devices
# (Approve on phone when prompted)

# 3. Enable wireless debugging
adb tcpip 5555

# 4. Get phone's IP address
#    Settings → About Phone → IP Address
#    Or: adb shell ip addr show wlan0 | grep inet

# 5. Disconnect USB cable

# 6. Connect wirelessly (replace <IP> with your phone's IP)
adb connect <IP_ADDRESS>:5555

# 7. Verify wireless connection
adb devices
# Should show: <IP>:5555    device
```

**Alternative: Wireless Debugging without USB (Android 11+):**

```bash
# 1. On Pixel 8 Pro:
#    Settings → Developer Options → Wireless Debugging → ON
#    Tap "Pair device with pairing code"

# 2. Note the IP:PORT and 6-digit pairing code shown

# 3. On Mac, pair using code
adb pair <IP_ADDRESS>:<PAIRING_PORT>
# Enter 6-digit code when prompted

# 4. Connect (use main IP:PORT from Wireless Debugging screen)
adb connect <IP_ADDRESS>:5555

# 5. Verify
adb devices
```

**Daily workflow:**

```bash
# Ensure phone and Mac are on same WiFi network
adb connect <IP_ADDRESS>:5555

# If connection drops
adb disconnect
adb connect <IP_ADDRESS>:5555
```

### Setup Commands

```bash
# 1. Clone repository
git clone <repository-url>
cd foodie

# 2. Open in VS Code
code .

# 3. Create local.properties (gitignored)
echo "azure.openai.endpoint=https://your-resource.openai.azure.com/" >> local.properties
echo "azure.openai.key=your-api-key-here" >> local.properties

# 4. Start Android emulator
emulator -avd Pixel_8_API_35 -no-audio &

# 5. Wait for emulator to boot (check with adb devices)
adb wait-for-device

# 6. Sync Gradle dependencies
./gradlew build

# 7. Install on emulator
./gradlew installDebug

# 8. Launch app
adb shell am start -n com.foodie.app/.MainActivity

# 9. View logs
adb logcat -s Foodie:* AndroidRuntime:E
```

### Common Gradle Commands (Terminal)

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on emulator (or physical device if connected)
./gradlew installDebug

# Build and install in one command
./gradlew clean assembleDebug installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests (on emulator)
./gradlew connectedAndroidTest

# Check dependencies
./gradlew dependencies

# Lint check
./gradlew lint

# View test reports
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Development Workflow (VS Code + Emulator)

```bash
# 1. Start emulator (once per session)
emulator -avd Pixel_8_API_35 -no-audio &
adb wait-for-device

# 2. Make code changes in VS Code

# 3. Build and install
./gradlew installDebug

# 4. View logs (filter for app)
adb logcat -s Foodie:* AndroidRuntime:E

# 5. Clear logs and restart
adb logcat -c
adb shell am start -n com.foodie.app/.MainActivity

# 6. Run tests
./gradlew test

# 7. Debug crashes
adb logcat *:E

# 8. Test on physical device (optional)
adb connect <PIXEL_IP>:5555
adb -s <PIXEL_IP>:5555 install app/build/outputs/apk/debug/app-debug.apk
```

### Physical Device Testing (Pixel 8 Pro - Optional)

Use physical device for:
- **Camera testing:** Realistic photo capture (emulator uses webcam)
- **Performance validation:** Real-world speed testing
- **Lock screen widget:** Actual lock screen behavior
- **Final UAT:** End-to-end user acceptance testing

```bash
# Connect to Pixel 8 Pro wirelessly
adb connect <IP_ADDRESS>:5555

# Install on physical device (specify device if multiple connected)
adb -s <IP_ADDRESS>:5555 install app/build/outputs/apk/debug/app-debug.apk

# View logs from physical device
adb -s <IP_ADDRESS>:5555 logcat -s Foodie:*
```

### Debugging without Android Studio

Since VS Code doesn't have native Android debugging:

1. **Timber Logging:** Use extensive Timber.d/i/e logs throughout code
2. **adb logcat:** Monitor real-time logs in terminal
3. **Emulator Snapshots:** Save/restore emulator state for quick iteration
4. **Chrome DevTools:** Inspect network via `adb forward tcp:9222 localabstract:chrome_devtools_remote`
5. **Stetho (optional):** Database/network inspection during development
6. **Android Studio (fallback):** Available for complex debugging if needed

**Emulator Advantages for Development:**
- **Fast iteration:** No wireless connection issues, instant install
- **Snapshots:** Save app state, restore instantly
- **Automation:** Scriptable for testing (adb shell input, screenshots)
- **Consistency:** Same environment across development sessions
- **Camera testing:** Webcam passthrough for meal photos

**Note:** Primary development on emulator. Physical device (Pixel 8 Pro) for final testing and camera validation.

### Device/Emulator Requirements

- **Primary Development:** Android Emulator (API 35, ARM64 for M2 Pro)
- **API Level:** 28+ (Android 9.0+) - Health Connect minimum requirement
- **Google Play Services:** Required for Health Connect
- **Camera:** Emulator webcam passthrough for development, Pixel 8 Pro for final testing
- **RAM:** 4GB recommended for emulator on M2 Pro
- **Physical Device:** Pixel 8 Pro for final validation (wireless ADB)

### Health Connect Setup (Testing)

```bash
# Install Health Connect on test device
adb install -r HealthConnect.apk  # From Android SDK or Play Store

# Grant Health Connect permissions programmatically during testing
adb shell pm grant com.foodie.app android.permission.health.READ_NUTRITION
adb shell pm grant com.foodie.app android.permission.health.WRITE_NUTRITION
```

## Architecture Decision Records (ADRs)

### ADR-001: Client-Only Architecture (No Backend)

**Decision:** Build as client-only app without backend server

**Rationale:**
- Personal tool for single user (no multi-user requirements)
- Azure OpenAI provides external intelligence (no need for custom ML backend)
- Health Connect provides local data persistence (no cloud storage needed)
- Reduces complexity and hosting costs
- Faster development timeline

**Consequences:**
- API key stored on device (acceptable for personal use)
- No cloud sync across devices (acceptable for V1.0)
- Future multi-user support would require architecture change
- Data tied to single device + Health Connect ecosystem

### ADR-002: Jetpack Compose over XML Views

**Decision:** Use Jetpack Compose for all UI

**Rationale:**
- Modern declarative approach reduces boilerplate
- Better state management with StateFlow integration
- Easier for AI code generation (clearer patterns)
- Google's recommended path forward
- Learning investment for future Android development

**Consequences:**
- Steeper initial learning curve
- Some legacy libraries require interop layer
- Preview tooling still maturing
- Better long-term maintainability

### ADR-003: System Camera Intent over CameraX

**Decision:** Use system camera intent instead of custom camera implementation

**Rationale:**
- Fastest launch time (critical for < 500ms target)
- Minimal code to maintain
- Leverages optimized system camera
- User familiar with system camera UI
- One-handed capture already optimized by OEM

**Consequences:**
- Less customization (acceptable for simple photo capture)
- Rely on system camera quality and features
- Cannot implement custom overlays or guides
- Photo preview handled by system (good UX)

### ADR-004: WorkManager without Foreground Service

**Decision:** Use WorkManager alone, avoid Foreground Service

**Rationale:**
- WorkManager handles background reliability automatically
- No persistent notification needed (better UX)
- Battery optimization handled by framework
- Retry logic built-in
- Simpler implementation

**Consequences:**
- Processing may be delayed if device in Doze mode (acceptable for nutrition logging)
- No real-time progress updates (user pockets phone anyway)
- WorkManager constraints ensure network availability
- User notified only on final success/failure

### ADR-005: Health Connect as Single Source of Truth (No Local Database)

**Decision:** Use Health Connect exclusively for data storage, no Room/SQLite database

**Rationale:**
- Health Connect `NutritionRecord.name` field supports food descriptions
- Eliminates dual-store complexity and sync logic
- Single source of truth prevents data inconsistencies
- Other health apps can access descriptions too
- Simpler architecture with fewer dependencies
- Health Connect handles data encryption and backup

**Consequences:**
- Edit operations require delete + re-insert pattern (Health Connect limitation)
- Query performance depends on Health Connect implementation
- Simpler codebase with one data layer
- No database migration complexity
- Better interoperability with health ecosystem

### ADR-006: Hilt for Dependency Injection

**Decision:** Use Hilt over Koin or manual DI

**Rationale:**
- Official Android DI built on Dagger
- Compile-time safety (catches errors early)
- Excellent Jetpack integration (ViewModel, WorkManager)
- Annotation-based (less boilerplate than manual DI)
- Better for AI code generation (clear patterns)

**Consequences:**
- Longer build times vs Koin (acceptable tradeoff)
- More complex setup than Koin (offset by better tooling)
- Compile-time verification prevents runtime DI errors

### ADR-007: Kotlin Flow over LiveData

**Decision:** Use StateFlow and Flow instead of LiveData

**Rationale:**
- Flow is more powerful and composable
- Better Compose integration (collectAsStateWithLifecycle)
- Support for operators (map, filter, combine)
- Kotlin-first (not tied to Android)
- Modern Android recommendation

**Consequences:**
- Slightly more complex than LiveData
- Need to handle lifecycle correctly in Compose
- Better testing support (easier to mock flows)

---

### ADR-008: VS Code + Emulator Development (Not Android Studio)

**Decision:** Use VS Code with Android Emulator for primary development, Pixel 8 Pro for final testing

**Rationale:**
- **Performance:** VS Code faster and more lightweight on M2 Pro than Android Studio
- **Terminal Control:** Direct Gradle command execution, clearer build process
- **Emulator Speed:** ARM64 emulator on M2 Pro has excellent performance
- **Fast Iteration:** Emulator eliminates wireless connection issues, instant install
- **Snapshots:** Save/restore emulator state for quick testing iterations
- **Flexibility:** Better integration with Git, markdown, and shell scripts
- **Learning:** Deeper understanding of Android build system via CLI
- **Memory:** Lower footprint, faster project switching vs Android Studio

**Consequences:**
- No visual layout editor (acceptable with Compose previews)
- No integrated debugger (use extensive Timber logging + adb logcat)
- Manual logcat monitoring (acceptable with proper filtering)
- Emulator webcam for development (Pixel 8 Pro for camera validation)
- Android Studio available as fallback for complex debugging
- Physical device used for final testing only (not daily development)

**Implementation:**
- Use Kotlin/Gradle VS Code extensions for syntax highlighting
- Android Emulator (Pixel 8, API 35, ARM64) for primary development
- Terminal-based build/install/test commands
- adb logcat for runtime debugging and crash analysis
- Wireless ADB to Pixel 8 Pro for final validation and camera testing

**Testing Strategy:**
- **Daily development:** Emulator (fast, consistent, snapshotable)
- **Camera validation:** Pixel 8 Pro (realistic photo capture)
- **Performance testing:** Pixel 8 Pro (real-world speed)
- **Final UAT:** Pixel 8 Pro (lock screen widget, end-to-end)

---

## Summary

This architecture document defines a modern, clean Android application using Jetpack Compose, Hilt, MVVM, and a client-only approach. All architectural decisions prioritize:

1. **Speed:** Sub-5-second capture flow via system camera intent and WorkManager
2. **Reliability:** Zero data loss via Health Connect single-source-of-truth and WorkManager retry logic
3. **Consistency:** Clear patterns for AI agent implementation (naming conventions, error handling, logging)
4. **Maintainability:** Clean architecture with clear layer separation and dependency injection
5. **Security:** Encrypted API key storage and ephemeral photo handling
6. **Development Workflow:** VS Code + terminal on M2 Pro, Android Emulator for development, Pixel 8 Pro for final testing

**Next Steps:**
1. Initialize Android project via command line (NOT Android Studio)
2. Set up Android Emulator (Pixel 8, API 35, ARM64)
3. Configure build.gradle.kts with all dependencies
4. Implement core architecture (Hilt modules, Health Connect integration, repositories)
5. Begin feature implementation following epic sequence
6. Test on emulator during development, validate on Pixel 8 Pro before release

---

_Generated by BMAD Decision Architecture Workflow v1.3.2_  
_Date: 2025-11-08_  
_For: BMad_  
_Architect: Winston_
