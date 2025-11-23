# Foodie - MVVM Architecture Guide

This README documents the MVVM (Model-View-ViewModel) architecture pattern and navigation structure for the Foodie app. All features follow these patterns for consistency, testability, and maintainability.

## Table of Contents

- [Quick Start & Configuration](#quick-start--configuration)
  - [Deploy to Phone](#deploy-to-phone)
  - [Azure OpenAI Setup](#azure-openai-setup)
- [Architecture Overview](#architecture-overview)
- [Navigation Structure](#navigation-structure)
- [Layer Responsibilities](#layer-responsibilities)
- [Creating New Features](#creating-new-features)
- [Error Handling Pattern](#error-handling-pattern)
- [Reference Implementations](#reference-implementations)
- [Testing Patterns](#testing-patterns)

---

## Quick Start & Configuration

### Deploy to Phone

**Connect your phone:**
```bash
# USB: Enable Developer Options + USB Debugging on phone, then plug in
# Wireless: Settings → Developer Options → Wireless Debugging → Pair

adb devices  # should show your phone
```

**Using Make (recommended):**
```bash
# From repository root
make install-debug    # Build and install debug version
make install-release  # Build and install release version
```

**Using Gradle directly:**
```bash
cd app
./gradlew installDebug   # Debug version (for testing)
./gradlew installRelease # Release version (for daily use)
```

**Both at once:**
Debug and release can coexist if you want to test changes without losing your production data. Just install both.

### Azure OpenAI Setup

The app requires Azure OpenAI credentials to analyze meal photos and estimate calories:

1. **Copy the template:**
   ```bash
   cp local.properties.template local.properties
   ```

2. **Get your Azure OpenAI credentials:**
   - Go to [Azure Portal](https://portal.azure.com)
   - Navigate to your Azure OpenAI resource
   - Copy the API key and endpoint URL

3. **Edit `local.properties`:**
   ```properties
   azure.openai.api.key="your-api-key-here"
   azure.openai.endpoint="https://your-resource.openai.azure.com"
   azure.openai.model="gpt-4.1"
   ```

4. **Build the app:**
   ```bash
   ./gradlew assembleDebug
   ```

**Note:** `local.properties` is git-ignored for security. Never commit API keys to version control.

**Future:** Story 5.2 will move credentials to in-app settings with EncryptedSharedPreferences.

### Permissions

**Android 13+ Notification Permission**  
On Android 13+, the app prompts for notification permission when you capture your first meal. Grant it so you can see analysis progress.

---

## Architecture Overview

Foodie uses Clean Architecture with MVVM pattern, organized into distinct layers with clear dependencies:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌────────────────────┐        ┌──────────────────────┐    │
│  │  Compose Screens   │───────▶│    ViewModels        │    │
│  │  (SampleScreen)    │        │  (@HiltViewModel)    │    │
│  └────────────────────┘        └──────────────────────┘    │
│         │ observes                      │ uses              │
│         │ StateFlow                     ▼                   │
└─────────┼───────────────────────────────────────────────────┘
          │
          │
┌─────────┼───────────────────────────────────────────────────┐
│         │               Domain Layer                         │
│         │  ┌─────────────────────┐  ┌──────────────────┐   │
│         │  │ Repository          │  │  Domain Models   │   │
│         └─▶│ Interfaces          │  │  (MealEntry)     │   │
│            │ (MealRepository)    │  └──────────────────┘   │
│            └─────────────────────┘                          │
│                     ▲                                        │
└─────────────────────┼────────────────────────────────────────┘
                      │ implements
                      │
┌─────────────────────┼────────────────────────────────────────┐
│                     │          Data Layer                     │
│            ┌────────┴─────────────┐                          │
│            │ Repository            │                          │
│            │ Implementations       │                          │
│            │ (MealRepositoryImpl)  │                          │
│            └────────┬──────────────┘                          │
│                     │ uses                                    │
│                     ▼                                         │
│            ┌─────────────────────┐                           │
│            │  Data Sources        │                           │
│            │  (HealthConnect,     │                           │
│            │   Azure OpenAI)      │                           │
│            └─────────────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

**Key Principles:**

1. **Unidirectional Data Flow**: Data flows down (from Repository → ViewModel → UI), events flow up (UI → ViewModel → Repository)
2. **Dependency Inversion**: Upper layers depend on abstractions (interfaces), not concrete implementations
3. **Single Responsibility**: Each layer has one clear purpose
4. **Testability**: Interfaces enable easy mocking and unit testing

---

## Navigation Structure

Foodie uses Jetpack Navigation Compose with a single-activity architecture. All screens are Composable functions, and navigation is type-safe using a sealed class pattern.

### Navigation Flow

```
MainActivity (hosts NavGraph)
    │
    └──▶ NavHost (startDestination: MealList)
         │
         ├──▶ MealList (home) ────┬──▶ MealDetail/{mealId}
         │                         │
         │                         └──▶ Settings
         │
         └──▶ Deep Link: foodie://home → MealList
```

### Screen Routes (Type-Safe Navigation)

All navigation routes are defined in `ui/navigation/Screen.kt` as a sealed class:

```kotlin
sealed class Screen(val route: String) {
    object MealList : Screen("meal_list")                    // Home screen
    object MealDetail : Screen("meal_detail/{mealId}") {     // Edit meal screen
        fun createRoute(mealId: String) = "meal_detail/$mealId"
    }
    object Settings : Screen("settings")                     // Settings screen
}
```

### How to Navigate

**From NavGraph (wiring callbacks):**
```kotlin
// In NavGraph.kt composable definitions
MealListScreen(
    onMealClick = { mealId ->
        navController.navigate(Screen.MealDetail.createRoute(mealId))
    },
    onSettingsClick = {
        navController.navigate(Screen.Settings.route)
    }
)
```

**From Screens (using callbacks):**
```kotlin
// Screens receive callbacks, NOT NavController
@Composable
fun MealListScreen(
    onMealClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    // UI calls callbacks on user action
    Button(onClick = { onMealClick("meal-123") }) {
        Text("View Meal")
    }
}
```

**Why callbacks instead of NavController?**
- ✅ Screens remain testable in isolation
- ✅ Screens don't need to know about navigation implementation
- ✅ Easy to create `@Preview` functions
- ✅ Clear separation of concerns

### Adding a New Screen

1. **Add route to Screen.kt:**
```kotlin
sealed class Screen(val route: String) {
    // ... existing routes ...
    object NewFeature : Screen("new_feature")
}
```

2. **Create screen Composable:**
```kotlin
// ui/screens/newfeature/NewFeatureScreen.kt
@Composable
fun NewFeatureScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Feature") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Screen content
    }
}
```

3. **Add to NavGraph:**
```kotlin
// In NavGraph.kt
composable(route = Screen.NewFeature.route) {
    NewFeatureScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

4. **Navigate to it from other screens:**
```kotlin
// From calling screen
SomeScreen(
    onNewFeatureClick = { navController.navigate(Screen.NewFeature.route) }
)
```

### Deep Linking

Deep links allow external sources (widgets, notifications) to launch specific screens.

**Current deep links:**
- `foodie://home` → Opens MealList screen
- `foodie://capture` → Opens MealList screen (used by home screen widget)

**Configuration:**
```kotlin
// In NavGraph.kt
composable(
    route = Screen.MealList.route,
    deepLinks = listOf(
        navDeepLink { uriPattern = "foodie://home" },
        navDeepLink { uriPattern = "foodie://capture" }
    )
) { /* ... */ }

// AndroidManifest.xml (already configured)
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="foodie" android:host="home" />
    <data android:scheme="foodie" android:host="capture" />
</intent-filter>
```

**Testing deep links:**
```bash
adb shell am start -W -a android.intent.action.VIEW -d "foodie://home" com.foodie.app
adb shell am start -W -a android.intent.action.VIEW -d "foodie://capture" com.foodie.app
```

### Home Screen Widget

The Foodie app includes a home screen widget for quick meal capture access.

**Implementation:**
- Built with Jetpack Glance (Compose-like API for widgets)
- Stateless design - no periodic updates required
- Launches app via `foodie://capture` deep link
- Requires device unlock (home screen widget)

**Key Files:**
- `ui/widget/MealCaptureWidget.kt` - Widget implementation using Glance
- `ui/widget/MealCaptureWidgetReceiver.kt` - Widget receiver
- `res/xml/glance_widget_info.xml` - Widget configuration
- `res/layout/widget_meal_capture.xml` - Placeholder layout

**Adding to Home Screen:**
1. Long-press on home screen
2. Tap "Widgets"
3. Find "Foodie" category
4. Drag "Log Meal" widget (2x1 size) to desired location

**Technical Details:**
- Widget shows app icon + "Log Meal" text
- Tapping widget launches app to MealList after unlock (camera integration in future story)
- PendingIntent uses `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` for Android 12+ security
- No background processing or battery impact (static widget)
- Persists across device reboots
- Performance: < 3 seconds from device wake to camera ready (with biometric unlock)

**Platform Note:**
Android does not support third-party lock screen widgets on phones. Lock screen shortcuts
are limited to system apps only (camera, torch, wallet, etc.). Home screen widgets provide
the best quick-access experience for third-party apps.

### Back Stack Behavior

- **MealList** is the start destination (root of back stack)
  - Pressing back on MealList exits the app
- **MealDetail** and **Settings** are pushed onto the back stack
  - Pressing back returns to MealList
- System back button and TopAppBar back arrow behave identically

**Example navigation flow:**
```
App Launch → MealList
  ↓ tap meal
MealDetail
  ↓ back
MealList
  ↓ tap settings
Settings
  ↓ back
MealList
  ↓ back
App exits
```

---

## Layer Responsibilities

### UI Layer (`ui/`)

**Purpose:** Present data to the user and handle user interactions

**Components:**
- **Screens** (Composable functions): Render UI based on state
- **ViewModels**: Manage UI state and business logic
- **State classes**: Immutable data classes representing UI state

**Rules:**
- ✅ Use `hiltViewModel()` for ViewModel injection
- ✅ Collect state with `collectAsStateWithLifecycle()`
- ✅ Never call repositories directly - always go through ViewModel
- ✅ Handle configuration changes automatically via ViewModel
- ❌ No business logic in Composables
- ❌ No direct data source access

**Example Screen:**
```kotlin
@Composable
fun MyFeatureScreen(
    viewModel: MyFeatureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    Scaffold { paddingValues ->
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> ErrorMessage(state.error)
            else -> SuccessContent(state.data)
        }
    }
}
```

### Domain Layer (`domain/`)

**Purpose:** Define business models and data contracts (no framework dependencies)

**Components:**
- **Models** (`domain/model/`): Business entities (MealEntry, NutritionData)
- **Repository Interfaces** (`domain/repository/`): Data access contracts
- **Use Cases** (future): Complex business logic operations

**Rules:**
- ✅ Pure Kotlin - NO Android framework dependencies
- ✅ Models enforce business rules in `init {}` blocks
- ✅ All repository methods return `Result<T>` or `Flow<Result<T>>`
- ✅ Use sealed classes for type-safe state (AnalysisStatus, Result)
- ❌ No implementation details
- ❌ No database, network, or UI code

**Example Model:**
```kotlin
data class MealEntry(
    val id: String,
    val timestamp: Instant,
    val description: String,
    val calories: Int
) {
    init {
        require(calories in 1..5000) {
            "Calories must be between 1 and 5000, got $calories"
        }
        require(description.isNotBlank()) {
            "Description cannot be blank"
        }
    }
}
```

**Example Repository Interface:**
```kotlin
interface MealRepository {
    fun getMealHistory(): Flow<Result<List<MealEntry>>>
    suspend fun updateMeal(id: String, calories: Int, description: String): Result<Unit>
    suspend fun deleteMeal(id: String): Result<Unit>
}
```

### Data Layer (`data/`)

**Purpose:** Implement data access and manage external data sources

**Components:**
- **Repository Implementations** (`data/repository/`): Implement domain interfaces
- **Data Sources** (`data/local/`, `data/remote/`): Direct access to external systems
- **DTOs** (Data Transfer Objects): API/database-specific models

**Rules:**
- ✅ Implement domain repository interfaces
- ✅ Handle errors and map to user-friendly messages
- ✅ Transform external data to domain models
- ✅ Use Timber for logging
- ✅ All repositories are `@Singleton` scoped
- ❌ Never expose data source types to domain layer
- ❌ No UI logic

**Example Repository Implementation:**
```kotlin
@Singleton
class MealRepositoryImpl @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource
) : MealRepository {
    
    override fun getMealHistory(): Flow<Result<List<MealEntry>>> = flow {
        emit(Result.Loading)
        try {
            val records = healthConnectDataSource.queryNutritionRecords(
                startTime = Instant.now().minus(30, ChronoUnit.DAYS),
                endTime = Instant.now()
            )
            val meals = records.map { /* transform to MealEntry */ }
            emit(Result.Success(meals))
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch meal history")
            emit(Result.Error(e, "Failed to load meals"))
        }
    }
}
```

---

## Creating New Features

### Step 1: Define Domain Models

Create data classes in `domain/model/` with validation:

```kotlin
// domain/model/UserProfile.kt
data class UserProfile(
    val id: String,
    val name: String,
    val email: String
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(email.contains("@")) { "Invalid email format" }
    }
}
```

### Step 2: Define Repository Interface

Create interface in `domain/repository/`:

```kotlin
// domain/repository/UserRepository.kt
interface UserRepository {
    /**
     * Fetches the current user profile.
     * 
     * @return Result.Success with UserProfile, or Result.Error on failure
     */
    suspend fun getUserProfile(): Result<UserProfile>
    
    /**
     * Updates the user's name.
     * 
     * @param newName The new name (must be non-blank)
     * @return Result.Success on completion, or Result.Error on failure
     */
    suspend fun updateUserName(newName: String): Result<Unit>
}
```

### Step 3: Implement Repository

Create implementation in `data/repository/`:

```kotlin
// data/repository/UserRepositoryImpl.kt
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiDataSource: ApiDataSource
) : UserRepository {
    
    override suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val response = apiDataSource.fetchUserProfile()
            val profile = UserProfile(
                id = response.id,
                name = response.name,
                email = response.email
            )
            Result.Success(profile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user profile")
            Result.Error(e, "Could not load your profile. Please try again.")
        }
    }
    
    override suspend fun updateUserName(newName: String): Result<Unit> {
        return try {
            apiDataSource.updateUserName(newName)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user name")
            Result.Error(e, "Could not update your name. Please try again.")
        }
    }
}
```

### Step 4: Bind Repository in Hilt

Add binding to `di/RepositoryModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

### Step 5: Create UI State

Define state in `ui/yourfeature/`:

```kotlin
// ui/profile/ProfileState.kt
data class ProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isSaving: Boolean = false
)
```

### Step 6: Create ViewModel

Extend `BaseViewModel` and use StateFlow:

```kotlin
// ui/profile/ProfileViewModel.kt
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = userRepository.getUserProfile()) {
                is Result.Success -> {
                    _state.value = ProfileState(
                        isLoading = false,
                        profile = result.data
                    )
                }
                is Result.Error -> {
                    logError("LoadProfile", result.exception, result.message)
                    _state.value = ProfileState(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {
                    // Already set isLoading = true
                }
            }
        }
    }
    
    fun updateName(newName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            
            when (val result = userRepository.updateUserName(newName)) {
                is Result.Success -> {
                    // Reload profile to get updated data
                    loadProfile()
                }
                is Result.Error -> {
                    logError("UpdateName", result.exception, result.message)
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
```

### Step 7: Create Compose Screen

Use `hiltViewModel()` and collect state:

```kotlin
// ui/profile/ProfileScreen.kt
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show errors in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.profile != null -> {
                    ProfileContent(
                        profile = state.profile!!,
                        isSaving = state.isSaving,
                        onUpdateName = { newName ->
                            viewModel.updateName(newName)
                        }
                    )
                }
            }
        }
    }
}
```

---

## Background Processing

Foodie uses WorkManager for reliable, constraint-based background processing. This ensures photo analysis and Health Connect data syncing happen even when the app is backgrounded or the device restarts.

### Architecture

```
CapturePhotoViewModel
    │ enqueueUniqueWork()
    ▼
WorkManager
    │ schedules with constraints
    ▼
AnalyzeMealWorker (CoroutineWorker)
    │
    ├──▶ NutritionAnalysisRepository.analyzePhoto()
    │       │
    │       └──▶ Azure OpenAI API
    │
    ├──▶ HealthConnectManager.insertNutritionRecord()
    │       │
    │       └──▶ Health Connect
    │
    └──▶ PhotoManager.deletePhoto()
```

### Worker: AnalyzeMealWorker

**Location:** `app/src/main/java/com/foodie/app/data/worker/AnalyzeMealWorker.kt`

Coordinates the complete meal photo processing workflow:

1. **Photo Analysis**: Calls Azure OpenAI Vision API to analyze meal photo
2. **Health Connect Save**: Stores nutrition data in Health Connect
3. **Photo Cleanup**: Deletes temporary photo file after successful processing
4. **Performance Monitoring**: Logs API duration, HC save duration, and total time (warns if >20s)

**Key Features:**
- `@HiltWorker` for dependency injection (NutritionAnalysisRepository, HealthConnectManager, PhotoManager)
- Network constraint ensures API calls only when online
- Exponential backoff retry (1s, 2s, 4s delays, max 4 attempts)
- Error classification: retryable (IOException, SocketTimeoutException) vs non-retryable
- Photo cleanup on success/max-retries/non-retryable errors; keeps only for SecurityException
- Comprehensive logging for debugging and performance tracking

**Usage:**

```kotlin
// In CapturePhotoViewModel.onUsePhoto()
val workRequest = OneTimeWorkRequestBuilder<AnalyzeMealWorker>()
    .setInputData(
        workDataOf(
            KEY_PHOTO_URI to photoUri.toString(),
            KEY_TIMESTAMP to timestamp.toEpochMilli()
        )
    )
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        1L,
        TimeUnit.SECONDS
    )
    .build()

workManager.enqueueUniqueWork(
    "analyze_meal_$timestamp",
    ExistingWorkPolicy.KEEP,
    workRequest
)
```

### WorkManager Configuration

**DI Module:** `di/WorkManagerModule.kt`

```kotlin
@Provides
@Singleton
fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
    return WorkManager.getInstance(context)
}

@Provides
@Singleton
fun provideWorkManagerConfiguration(): Configuration {
    return Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
        .build()
}
```

**Application Class:** `FoodieApplication.kt`

- Extends `Application`
- Annotated with `@HiltAndroidApp` for DI
- WorkManager auto-initializes using configuration from WorkManagerModule

### Retry Logic

**Retryable Errors:**
- `IOException`: Network issues (no connection, DNS resolution failed, etc.)
- `SocketTimeoutException`: API timeout (server slow to respond)

**Non-Retryable Errors:**
- `IllegalArgumentException`: Invalid input data (bad photo URI, missing timestamp)
- `SecurityException`: Permission denied (can't access photo file)
- `HttpException` with 400-level status: Client errors (invalid API key, bad request format)
- All other exceptions

**Retry Schedule:**
1. Attempt 1: Immediate execution when enqueued
2. Attempt 2: 1 second backoff
3. Attempt 3: 2 seconds backoff (exponential)
4. Attempt 4: 4 seconds backoff (final attempt)

After 4 attempts, worker returns `Result.failure()` and photo is deleted.

### Testing

**Unit Tests:** `app/src/test/java/com/foodie/app/data/worker/AnalyzeMealWorkerTest.kt`

- Mock NutritionAnalysisRepository, HealthConnectManager, PhotoManager
- Test success path, retry logic, error handling, photo cleanup
- Verify logging and performance monitoring

**Integration Tests:** `app/src/androidTest/java/com/foodie/app/data/worker/AnalyzeMealWorkerIntegrationTest.kt`

- Use `TestListenableWorkerBuilder` from WorkManager testing library
- Test worker execution with real WorkManager context
- Verify retry behavior with exponential backoff
- Test timeout scenarios and constraint enforcement

**Running Tests:**

```bash
# Unit tests
./gradlew :app:test

# Integration tests (requires emulator/device)
./gradlew :app:connectedAndroidTest
```

**Using Make (from repository root):**

```bash
make test-unit            # Run all unit tests
make test-instrumentation # Run all instrumentation tests (requires device/emulator)
make test-all             # Run all tests (unit + instrumentation)
```

Run `make help` to see all available commands.

### Performance Expectations

- **API Analysis**: < 10 seconds (typical: 3-5s for 2MB photo)
- **Health Connect Save**: < 1 second
- **Total Processing**: < 12 seconds (warns in logs if >20s)
- **Battery Impact**: Minimal (work scheduled only when network available, no polling)

### Debugging

**Logs:** Filter by `AnalyzeMealWorker` tag

```bash
adb logcat -s AnalyzeMealWorker
```

**Key Log Messages:**
- `Starting meal analysis for photo: <uri>`
- `API analysis completed in <ms>ms`
- `Health Connect save completed in <ms>ms`
- `Total processing time: <ms>ms` (WARNING if >20s)
- `Photo deleted successfully` / `Failed to delete photo`
- `Error analyzing meal: <message>` (on retryable errors)
- `Non-retryable error, failing worker: <message>` (on permanent failures)

**WorkManager Inspection:**

```kotlin
// Check work status in debug mode
val workInfos = workManager.getWorkInfosForUniqueWork("analyze_meal_$timestamp").get()
workInfos.forEach { workInfo ->
    Timber.d("Work status: ${workInfo.state}")
}
```

---

## Error Handling Pattern

### Result Wrapper

All repository operations return `Result<T>` for consistent error handling:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
    
    fun getOrNull(): T?
    fun exceptionOrNull(): Throwable?
    fun isSuccess(): Boolean
    fun isError(): Boolean
}
```

### Using Result in ViewModels

```kotlin
when (val result = repository.fetchData()) {
    is Result.Success -> {
        // Update state with data
        _state.value = state.copy(data = result.data)
    }
    is Result.Error -> {
        // Log technical details
        Timber.e(result.exception, "Operation failed")
        // Show user-friendly message
        _state.value = state.copy(error = result.message)
    }
    is Result.Loading -> {
        _state.value = state.copy(isLoading = true)
    }
}
```

### Flow Extension for Result

Wrap Flow emissions automatically:

```kotlin
// In repository
override fun getMealHistory(): Flow<Result<List<MealEntry>>> {
    return flow {
        val meals = dataSource.queryMeals()
        emit(meals)
    }.asResult() // Wraps in Loading → Success/Error
}
```

### Error Message Guidelines

- **User Messages**: Short, actionable, non-technical
  - ✅ "Could not load meals. Please check your connection."
  - ❌ "HttpException: 500 Internal Server Error"
  
- **Logging**: Include full exception and context
  ```kotlin
  Timber.e(exception, "Failed to fetch meals for user $userId")
  ```

---

## Reference Implementations

### SampleViewModel

**Location:** `app/src/main/java/com/foodie/app/ui/sample/SampleViewModel.kt`

Demonstrates the complete MVVM pattern:
- ✅ `@HiltViewModel` annotation for DI
- ✅ Constructor injection of repository
- ✅ StateFlow for reactive state management
- ✅ `viewModelScope` for coroutine lifecycle
- ✅ Result wrapper handling (Loading, Success, Error)
- ✅ Timber logging with BaseViewModel helpers
- ✅ Clear separation of concerns

### SampleScreen

**Location:** `app/src/main/java/com/foodie/app/ui/sample/SampleScreen.kt`

Demonstrates Compose UI best practices:
- ✅ `hiltViewModel()` for ViewModel injection
- ✅ `collectAsStateWithLifecycle()` for state collection
- ✅ Scaffold with SnackbarHost for error display
- ✅ LaunchedEffect for side effects
- ✅ Multiple `@Preview` functions
- ✅ Separation of UI logic into smaller composables

### MealRepository

**Location:** `app/src/main/java/com/foodie/app/domain/repository/MealRepository.kt`

Demonstrates repository interface pattern:
- ✅ Flow<Result<T>> for reactive streams
- ✅ suspend functions for one-shot operations
- ✅ Comprehensive KDoc documentation
- ✅ Clear method contracts

### MealRepositoryImpl

**Location:** `app/src/main/java/com/foodie/app/data/repository/MealRepositoryImpl.kt`

Demonstrates repository implementation:
- ✅ `@Singleton` scoping
- ✅ Constructor injection of data sources
- ✅ Error handling with try-catch
- ✅ Timber logging for debugging
- ✅ User-friendly error messages
- ✅ Data transformation (external → domain models)

---

## Testing Patterns

### Domain Model Tests

Test validation logic:

```kotlin
@Test
fun `constructor should throw exception when calories is less than 1`() {
    val exception = assertThrows(IllegalArgumentException::class.java) {
        MealEntry(
            id = "id",
            timestamp = Instant.now(),
            description = "Invalid meal",
            calories = 0
        )
    }
    assertThat(exception.message).contains("Calories must be between 1 and 5000")
}
```

### Repository Tests

Mock data sources and verify behavior:

```kotlin
@Test
fun `getMealHistory should return success with meals when data source returns records`() = runTest {
    // Given
    val mockRecords = listOf(/* mock data */)
    whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
        .thenReturn(mockRecords)
    
    // When
    val result = mealRepository.getMealHistory().first()
    
    // Then
    assertThat(result.isSuccess()).isTrue()
    assertThat(result.getOrNull()).hasSize(mockRecords.size)
}
```

### ViewModel Tests

Mock repositories and verify state updates:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: MyRepository
    private lateinit var viewModel: MyViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadData should update state to loading then success`() = runTest {
        // Given
        val mockData = listOf(/* test data */)
        whenever(repository.getData()).thenReturn(Result.Success(mockData))
        viewModel = MyViewModel(repository)
        
        // When
        viewModel.loadData()
        
        // Then
        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.data).hasSize(mockData.size)
        assertThat(state.error).isNull()
    }
}
```

### Test Naming Convention

```
methodName should expectedBehavior when condition
```

Examples:
- `getMealHistory should return success when data source returns valid records`
- `updateMeal should return error when calories is out of range`
- `loadData should set loading state while repository is fetching`

### Testing Tools

- **JUnit 4**: Test framework
- **Mockito**: Mocking dependencies
- **Truth**: Fluent assertions (`assertThat(x).isEqualTo(y)`)
- **kotlinx-coroutines-test**: `runTest`, `UnconfinedTestDispatcher`
- **InstantTaskExecutorRule**: For ViewModel testing

---

## Best Practices Summary

### DO ✅

- Use `@HiltViewModel` and `hiltViewModel()` for dependency injection
- Collect state with `collectAsStateWithLifecycle()` in Compose
- Return `Result<T>` from all repository methods
- Log errors with Timber (technical details)
- Provide user-friendly error messages
- Use `viewModelScope` for coroutines in ViewModels
- Make domain models validate themselves in `init` blocks
- Write comprehensive unit tests with mocks
- Use sealed classes for type-safe state
- Document with KDoc comments

### DON'T ❌

- Access repositories directly from UI - always use ViewModel
- Put business logic in Composables
- Expose data source types outside the data layer
- Use LiveData (use StateFlow instead)
- Hardcode strings (use string resources)
- Ignore exceptions (always handle and log)
- Put Android dependencies in domain layer
- Create ViewModels manually (use Hilt injection)

---

## Need Help?

- **Sample Implementation**: Check `ui/sample/` package for complete working example
- **Architecture Docs**: See `/docs/architecture.md` for detailed system design
- **Tech Specs**: See `/docs/tech-spec-epic-*.md` for feature specifications
- **Story Context**: Each story has a `.context.xml` file with implementation guidance

---

**Version:** 1.0 (Story 1.2)  
**Last Updated:** 2025-11-08  
**Maintainer:** Foodie Development Team
