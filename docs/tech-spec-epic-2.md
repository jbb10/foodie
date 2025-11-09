# Epic Technical Specification: AI-Powered Meal Capture

Date: 2025-11-09
Author: BMad
Epic ID: 2
Status: Draft

---

## Overview

Epic 2 delivers the core "invisible tracking" innovation that defines the Foodie value proposition: enabling users to capture, analyze, and save meal nutrition data in under 5 seconds with minimal friction. This epic implements the complete end-to-end flow from lock screen widget activation through AI-powered nutrition analysis to automatic Health Connect storage, all happening in the background while users continue with their activities.

The epic builds on the foundation established in Epic 1 (MVVM architecture, Health Connect integration, error handling framework) and introduces the three critical technical integrations: Android lock screen widget for instant camera access, Azure OpenAI Responses API for multimodal image analysis, and WorkManager for reliable background processing with retry logic. Success is measured by the speed validation criterion: average capture time ≤ 5 seconds from widget tap to photo confirmation.

## Objectives and Scope

**In Scope:**
- Lock screen widget implementation using Android 12+ lock screen quick action API with sub-500ms launch time
- Camera integration using system camera intent with one-handed operation, retake capability, and 2MP JPEG compression
- Azure OpenAI Responses API client with GPT-4.1 multimodal vision model, structured JSON output parsing (calories + description)
- WorkManager background processing with network-constrained execution, exponential backoff retry (3 attempts), and automatic photo cleanup
- Health Connect `NutritionRecord` persistence using `energy` field (calories) and `name` field (description) with timestamp preservation
- End-to-end integration validation demonstrating complete flow: widget → camera → AI analysis → Health Connect save → photo deletion
- Deep linking configuration for widget-to-camera navigation
- Story 2.0 (Deep Linking Validation) ensuring robust navigation from widget and future external triggers

**Out of Scope:**
- List view UI for reviewing meals (Epic 3)
- Edit and delete operations (Epic 3)
- Advanced error handling UX (Epic 4 - network detection, API error classification, manual retry UI)
- Settings screen for API key management (Epic 5)
- Offline queuing and deferred processing (V2.0)
- Multi-photo capture or before/after comparisons (V3.0)

**Success Criteria:**
- Widget launches camera in < 500ms (measured from tap to camera ready state)
- Photo capture + confirmation completes in < 5 seconds (one-handed operation validated)
- Background processing completes in < 15 seconds typical (widget → camera → AI → Health Connect save)
- Azure OpenAI API returns structured nutrition data with 95%+ success rate on valid food images
- WorkManager retry logic handles transient network failures with zero data loss
- Temporary photos are deleted after successful Health Connect save or retry exhaustion
- Deep linking navigation from widget and external sources works reliably across all app states

## System Architecture Alignment

This epic implements the AI-powered capture flow layer defined in the Architecture Document, integrating across all architectural tiers:

**UI Layer (ui/ package):**
- Lock screen widget using `GlanceAppWidget` (Jetpack Glance for widgets) with `PendingIntent` deep link to camera activity
- Camera activity using system camera intent (`MediaStore.ACTION_IMAGE_CAPTURE`) with preview confirmation UI
- No ViewModel needed for camera flow (stateless, intent-based navigation)

**Data Layer (data/ package):**
- **Remote Data Source:** `AzureOpenAiApi` Retrofit interface calling Azure OpenAI Responses API endpoint with multimodal vision request
- **Worker:** `AnalyzeMealWorker` coordinating photo → API → Health Connect → cleanup flow using WorkManager constraints
- **Repository:** `NutritionAnalysisRepository` implementing domain interface, wrapping API calls and error handling
- **Health Connect:** Extends `HealthConnectManager` from Epic 1 with nutrition record insertion using both `energy` and `name` fields

**Dependency Injection (di/ package):**
- `NetworkModule` provides configured Retrofit instance with Azure OpenAI base URL from SecurePreferences (API key + endpoint injected via `AuthInterceptor`)
- `WorkManagerModule` provides `HiltWorkerFactory` for dependency injection into `AnalyzeMealWorker`

**Technology Stack Integration:**
- **WorkManager 2.9.1:** Background processing with network constraints, exponential backoff, and zero foreground service overhead
- **Retrofit 2.11.0 + OkHttp 4.12.0:** HTTP client for Azure OpenAI API with logging interceptor (debug mode), authentication interceptor (API key header)
- **Jetpack Glance:** Modern widget framework for lock screen quick actions (Material3-aligned)
- **Health Connect 1.1.0:** `NutritionRecord` creation with both `energy` (calories in kcal) and `name` (food description) fields
- **Kotlin Coroutines + Flow:** Async API calls, repository Flow emissions, WorkManager suspend functions

**Health Connect as Single Source of Truth:**
Nutrition data (calories + descriptions) saved exclusively to Health Connect using `NutritionRecord.energy` and `NutritionRecord.name` fields. No local database needed - Epic 3 will query Health Connect directly for list view.

**Deep Linking Architecture:**
Navigation uses Jetpack Navigation Compose with deep link routes. Widget `PendingIntent` triggers `foodie://capture` deep link, which the NavGraph routes to camera activity regardless of app state (cold start, background, foreground).

## Detailed Design

### Services and Modules

**Lock Screen Widget Module (ui/widget/):**

| Component | Responsibility | Key Methods | Dependencies |
|-----------|----------------|-------------|--------------|
| `MealCaptureWidget` (GlanceAppWidget) | Lock screen quick action widget rendering | `Content()` composable, `provideGlance()` | Navigation deep link URI |
| `MealCaptureWidgetReceiver` (GlanceAppWidgetReceiver) | Widget lifecycle and update handling | `onUpdate()`, `onEnabled()` | MealCaptureWidget |

**Widget Configuration:**
- Widget type: Lock screen quick action (single button)
- Display: App icon + "Log Meal" text
- Action: Deep link to `foodie://capture` route
- Size: Small/medium (lock screen constraint)
- Update frequency: Static (no periodic updates needed)

**Camera Module (ui/camera/):**

| Component | Responsibility | Implementation |
|-----------|----------------|----------------|
| `CameraActivity` | Launches system camera, handles result | `ActivityResultContract` for `ACTION_IMAGE_CAPTURE` |
| Photo Storage | Saves captured image to cache | `context.cacheDir/photos/{timestamp}.jpg` with 2MP resize + 80% JPEG compression |
| Preview Screen | Confirms or retakes photo | Compose UI with Image preview, "Retake" and "Use Photo" buttons |

**Azure OpenAI API Module (data/remote/api/):**

| Component | Responsibility | Input | Output | Error Handling |
|-----------|----------------|-------|--------|----------------|
| `AzureOpenAiApi` (Retrofit interface) | Define API endpoint contract | `@POST("openai/v1/responses")` | `AzureResponseResponse` | Retrofit exception mapping |
| `AuthInterceptor` | Inject API key header | SecurePreferences API key | Modified OkHttp Request | 401/403 handling |
| `NutritionAnalysisRepository` | Coordinate API calls, parse responses | Photo URI | `Result<NutritionData>` | Network, parse, validation errors |

**Background Processing Module (data/worker/):**

| Component | Responsibility | Execution Context | Constraints | Retry Policy |
|-----------|----------------|-------------------|-------------|--------------|
| `AnalyzeMealWorker` (CoroutineWorker) | Orchestrate photo → API → Health Connect → cleanup | WorkManager background thread | NetworkType.CONNECTED | Exponential backoff, max 3 retries (1s, 2s, 4s delays) |
| `WorkManagerModule` (Hilt DI) | Configure WorkManager with HiltWorkerFactory | Application onCreate() | None | N/A |

**Worker Responsibilities:**
1. Read photo URI from WorkManager input data
2. Load and base64-encode image
3. Call `NutritionAnalysisRepository.analyzePhoto()`
4. Parse API response to extract calories + description
5. Validate nutrition data (calories in 1..5000 range)
6. Save to Health Connect via `HealthConnectManager.insertNutritionRecord()`
7. Delete photo file from cache directory
8. Return `Result.success()` or `Result.retry()` based on error type

**Health Connect Extension (data/local/healthconnect/):**

Extends `HealthConnectManager` from Epic 1 with enhanced nutrition record insertion:

```kotlin
suspend fun insertNutritionRecord(
    calories: Int,
    description: String,
    timestamp: Instant
): String {
    val record = NutritionRecord(
        energy = Energy.kilocalories(calories.toDouble()),
        name = description,  // NEW: Food description storage
        startTime = timestamp,
        endTime = timestamp,
        startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
        endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
    )
    
    val response = healthConnectClient.insertRecords(listOf(record))
    return response.recordIdsList.first()  // Returns HC record ID
}
```

### Data Models and Contracts

**Domain Models (domain/model/):**

```kotlin
/**
 * Nutrition data parsed from Azure OpenAI API response
 */
data class NutritionData(
    val calories: Int,
    val description: String
) {
    init {
        require(calories in 1..5000) { "Calories must be between 1 and 5000" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(description.length <= 200) { "Description must be <= 200 characters" }
    }
}

/**
 * Background processing status for UI feedback
 */
sealed class AnalysisStatus {
    object Idle : AnalysisStatus()
    data class Analyzing(val progress: Float? = null) : AnalysisStatus()
    data class Success(val data: NutritionData) : AnalysisStatus()
    data class Error(val message: String, val retryable: Boolean) : AnalysisStatus()
}
```

**API Data Transfer Objects (data/remote/dto/):**

```kotlin
/**
 * Azure OpenAI Responses API request with multimodal input
 */
data class AzureResponseRequest(
    val model: String,  // "gpt-4.1" or deployment name
    val instructions: String,  // System-level instructions
    val input: List<InputItem>  // Multimodal input array
)

data class InputItem(
    val role: String,  // "user"
    val content: List<ContentItem>
)

data class ContentItem(
    val type: String,  // "input_text" or "input_image"
    val text: String? = null,
    val image_url: String? = null  // Base64 data URL: "data:image/jpeg;base64,..."
)

/**
 * Azure OpenAI Responses API response
 */
data class AzureResponseResponse(
    val id: String,
    val created_at: Double,
    val model: String,
    val `object`: String,  // "response"
    val status: String,  // "completed", "in_progress", "failed"
    val output_text: String?,  // Direct text output (used for simple JSON responses)
    val output: List<OutputItem>?,  // Structured output
    val usage: Usage
)

data class OutputItem(
    val id: String,
    val type: String,
    val content: List<OutputContent>?,
    val role: String?
)

data class OutputContent(
    val type: String,
    val text: String
)

data class Usage(
    val input_tokens: Int,
    val output_tokens: Int,
    val total_tokens: Int
)

/**
 * Parsed nutrition from API output_text field
 * Expected format: {"calories": 650, "description": "Grilled chicken with rice"}
 */
data class ApiNutritionResponse(
    val calories: Int,
    val description: String
)
```

**WorkManager Data Keys (data/worker/AnalyzeMealWorker.kt):**

```kotlin
object WorkerKeys {
    const val KEY_PHOTO_URI = "photo_uri"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_ERROR = "error_message"
    const val KEY_RETRY_COUNT = "retry_count"
}
```

### APIs and Interfaces

**Azure OpenAI Responses API Integration:**

**Endpoint Configuration:**
```kotlin
// di/NetworkModule.kt
@Provides
@Singleton
fun provideRetrofit(
    okHttpClient: OkHttpClient,
    securePreferences: SecurePreferences
): Retrofit {
    val baseUrl = securePreferences.getEndpoint() 
        ?: "https://YOUR-RESOURCE.openai.azure.com/"
    
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

@Provides
@Singleton
fun provideOkHttpClient(
    authInterceptor: AuthInterceptor,
    loggingInterceptor: HttpLoggingInterceptor
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
```

**Authentication Interceptor:**
```kotlin
// data/remote/interceptor/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val securePreferences: SecurePreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = securePreferences.getApiKey() 
            ?: throw IllegalStateException("API key not configured")
        
        val request = chain.request().newBuilder()
            .addHeader("api-key", apiKey)  // Azure OpenAI uses "api-key" header
            .addHeader("Content-Type", "application/json")
            .build()
        
        return chain.proceed(request)
    }
}
```

**Retrofit Interface:**
```kotlin
// data/remote/api/AzureOpenAiApi.kt
interface AzureOpenAiApi {
    @POST("openai/v1/responses")
    suspend fun analyzeNutrition(
        @Body request: AzureResponseRequest
    ): AzureResponseResponse
}
```

**Repository Implementation:**
```kotlin
// data/repository/NutritionAnalysisRepositoryImpl.kt
class NutritionAnalysisRepositoryImpl @Inject constructor(
    private val azureOpenAiApi: AzureOpenAiApi,
    private val imageUtils: ImageUtils,
    @ApplicationContext private val context: Context
) : NutritionAnalysisRepository {
    
    override suspend fun analyzePhoto(photoUri: Uri): Result<NutritionData> {
        return try {
            // Load and encode image
            val base64Image = imageUtils.encodeImageToBase64(photoUri)
            
            // Build request
            val request = AzureResponseRequest(
                model = "gpt-4.1",  // Or from SecurePreferences
                instructions = SYSTEM_INSTRUCTIONS,
                input = listOf(
                    InputItem(
                        role = "user",
                        content = listOf(
                            ContentItem(
                                type = "input_text",
                                text = "Analyze this meal and estimate total calories."
                            ),
                            ContentItem(
                                type = "input_image",
                                image_url = "data:image/jpeg;base64,$base64Image"
                            )
                        )
                    )
                )
            )
            
            // Call API
            val response = azureOpenAiApi.analyzeNutrition(request)
            
            // Parse output_text as JSON
            val nutritionData = parseNutritionResponse(response.output_text)
            
            Result.success(nutritionData)
            
        } catch (e: IOException) {
            Timber.e(e, "Network error analyzing photo")
            Result.error(e, "Network error - will retry")
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "Failed to parse API response")
            Result.error(e, "Invalid response format")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error analyzing photo")
            Result.error(e, e.message ?: "Unknown error")
        }
    }
    
    private fun parseNutritionResponse(outputText: String?): NutritionData {
        if (outputText == null) {
            throw JsonSyntaxException("output_text is null")
        }
        
        val apiResponse = Gson().fromJson(outputText, ApiNutritionResponse::class.java)
        return NutritionData(apiResponse.calories, apiResponse.description)
    }
    
    companion object {
        private const val SYSTEM_INSTRUCTIONS = 
            "You are a nutrition analysis assistant. Analyze the food image and " +
            "return ONLY a JSON object with two fields: calories (number) and " +
            "description (string describing the food)."
    }
}
```

**Health Connect Interface Extension:**

```kotlin
// Extends HealthConnectManager from Epic 1
suspend fun HealthConnectManager.insertNutritionWithDescription(
    calories: Int,
    description: String,
    timestamp: Instant = Instant.now()
): String {
    return insertNutritionRecord(calories, description, timestamp)
}
```

### Workflows and Sequencing

**Complete End-to-End Flow:**

```
[User Action] Tap Lock Screen Widget
    ↓
[Widget] PendingIntent triggers deep link: foodie://capture
    ↓
[NavGraph] Routes to CameraActivity (handles cold start, background, foreground states)
    ↓
[CameraActivity] Launches system camera intent (ACTION_IMAGE_CAPTURE)
    ↓
[System Camera] User captures photo → Returns URI to CameraActivity
    ↓
[CameraActivity] Shows preview with "Retake" and "Use Photo" buttons
    ↓
[User Action] Taps "Use Photo"
    ↓
[CameraActivity] Saves photo to cache: context.cacheDir/photos/{timestamp}.jpg
    │             Compresses to 2MP, 80% JPEG quality
    │             Stores URI and timestamp
    ↓
[CameraActivity] Enqueues WorkManager job:
    │             WorkManager.enqueue(
    │                 OneTimeWorkRequestBuilder<AnalyzeMealWorker>()
    │                     .setInputData(workDataOf(
    │                         KEY_PHOTO_URI to photoUri.toString(),
    │                         KEY_TIMESTAMP to timestamp.epochSecond
    │                     ))
    │                     .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
    │                     .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.SECONDS)
    │                     .build()
    │             )
    ↓
[CameraActivity] Finishes, user returns to previous activity (or lock screen)
    ↓
[WorkManager] Schedules AnalyzeMealWorker when network available
    ↓
═══════════════════════════════════════════════════════════════════
[Background Thread] AnalyzeMealWorker.doWork()
═══════════════════════════════════════════════════════════════════
    ↓
[Worker] Reads photo URI from input data
    ↓
[Worker] Loads photo file, validates exists
    ↓
[Worker] Calls NutritionAnalysisRepository.analyzePhoto(uri)
    ↓
[Repository] Base64-encodes image via ImageUtils
    ↓
[Repository] Builds AzureResponseRequest with multimodal input
    ↓
[Repository] Calls AzureOpenAiApi.analyzeNutrition(request)
    ↓
[Retrofit + OkHttp] 
    │ AuthInterceptor adds api-key header
    │ Sends POST to https://{resource}.openai.azure.com/openai/v1/responses
    │ Waits for response (timeout: 30s read, 15s connect)
    ↓
[Azure OpenAI API] Processes image with GPT-4.1 vision model
    │               Returns JSON: {"calories": 650, "description": "..."}
    ↓
[Repository] Parses response.output_text as JSON
    │          Validates calories in 1..5000 range
    │          Returns Result.success(NutritionData)
    ↓
[Worker] Receives NutritionData from repository
    ↓
[Worker] Calls HealthConnectManager.insertNutritionRecord(calories, description, timestamp)
    ↓
[HealthConnectManager] Creates NutritionRecord with:
    │                   - energy: Energy.kilocalories(calories)
    │                   - name: description
    │                   - startTime/endTime: timestamp
    ↓
[Health Connect SDK] Persists record to local Health Connect database
    ↓
[Worker] Receives Health Connect record ID
    ↓
[Worker] Deletes photo file: File(photoUri.path).delete()
    ↓
[Worker] Returns Result.success()
    ↓
[WorkManager] Marks work as completed, removes from queue
    ↓
[End] Nutrition data available in Health Connect for Epic 3 list view
```

**Error Handling Paths:**

```
[Network Error in Repository]
    ↓ IOException caught
    ↓ Returns Result.error(e, "Network error")
    ↓
[Worker] Checks if runAttemptCount < 3
    ↓ Yes → Returns Result.retry()
    ↓ WorkManager reschedules with exponential backoff (1s, 2s, 4s)
    ↓ Retries up to 3 additional times (4 total attempts)
    ↓
[After 3 Retries] Returns Result.failure()
    ↓ Photo remains in cache for manual retry (Epic 4)
    ↓ WorkManager marks as failed

[API Parse Error in Repository]
    ↓ JsonSyntaxException caught
    ↓ Returns Result.error(e, "Invalid response")
    ↓
[Worker] Non-retryable error → Returns Result.failure()
    ↓ Photo deleted (invalid data, no point retrying)
    ↓ Error logged for debugging

[Validation Error in Worker]
    ↓ Calories outside 1..5000 range
    ↓ Timber.w("Invalid calorie estimate: $calories")
    ↓ Returns Result.failure()
    ↓ Photo deleted
    ↓ Manual retry available (Epic 4)
```

## Non-Functional Requirements

### Performance

**Critical Performance Targets (Core Value Proposition):**

- **Widget Launch Time:** < 500ms from tap to camera ready
  - Measurement: `System.currentTimeMillis()` from widget `onClick()` to camera activity `onResume()`
  - Optimization: Use system camera intent (fastest launch), minimize deep link processing
  - Validation: Test on mid-range Android devices (not just flagship phones)

- **Photo Capture Flow:** < 5 seconds total (widget tap → photo confirmed → return to previous activity)
  - Measurement: Widget tap timestamp to CameraActivity finish timestamp
  - Optimization: Single-tap capture, large touch targets, minimal preview UI
  - One-handed validation: Test with thumb-only operation while holding device

- **Background Processing:** < 15 seconds typical (photo confirmed → Health Connect saved)
  - Measurement: WorkManager enqueue timestamp to Health Connect insert timestamp
  - Components: API call (< 10s typical), base64 encoding (< 1s), Health Connect write (< 1s)
  - Network dependency: Actual time varies with network latency (monitored, not enforced)

- **API Response Time:** < 10 seconds typical for Azure OpenAI analysis
  - Timeout configuration: 30s read timeout, 15s connect timeout in OkHttp
  - Retry on timeout: Network error handling triggers WorkManager retry
  - Monitoring: Log API call duration with Timber for performance tracking

- **Image Processing:** < 2 seconds for compression and base64 encoding
  - Target photo size: 2MP max resolution, 80% JPEG quality
  - Compression before encoding: Reduce file size to ~200-500KB typical
  - Implementation: Use Android Bitmap APIs with efficient memory handling

**Battery Impact:**
- WorkManager handles background execution efficiently (no foreground service needed)
- Network constraint ensures work only executes when connected (avoids battery-draining retries)
- Photo deletion immediately after processing prevents storage bloat
- No wake locks or continuous background services

**Memory Management:**
- Photo loaded into memory only during base64 encoding
- Large bitmap recycled immediately after encoding
- No photo caching in memory (disk cache only, temporary)
- WorkManager manages worker lifecycle to prevent memory leaks

### Security

**API Key Protection:**
- Storage: Android Keystore with AES256_GCM encryption (hardware-backed on supported devices)
- Access: SecurePreferences abstraction prevents direct key exposure
- Transmission: HTTPS only (enforced by Retrofit base URL validation)
- Logging: API key never logged in debug logs or crash reports (AuthInterceptor sanitizes logs)
- Configuration: User-provided key in Settings screen (Epic 5)

**Photo Privacy:**
- Ephemeral storage: Photos saved to `context.cacheDir/photos/` (app-private directory)
- Automatic cleanup: Deleted immediately after successful Health Connect save OR retry exhaustion
- No cloud upload: Photos sent only to Azure OpenAI API for analysis (not stored in any cloud storage)
- Cache clearing: Android system can clear cache directory if storage low
- 24-hour maximum retention: Periodic cleanup task (Epic 4) removes photos older than 24 hours

**Network Communication:**
- HTTPS enforcement: Retrofit base URL must use `https://` scheme
- Certificate pinning: Not implemented in V1.0 (Azure OpenAI uses standard certificates)
- Request/response encryption: TLS 1.2+ provided by OkHttp
- No sensitive data in URLs: API key in header, photo data in POST body

**Data Minimization:**
- Only calories and description stored (no raw photos retained)
- No analytics or telemetry (personal tool, no tracking)
- No user accounts or authentication required
- Health Connect handles encryption at rest for nutrition data

### Reliability/Availability

**Zero Data Loss Guarantee:**
- Photo persists in cache until successful Health Connect save OR retry limit reached
- WorkManager ensures reliable background execution (survives app termination, device reboot)
- Health Connect provides durable local storage (no sync conflicts, no cloud dependencies)
- Transaction pattern: Save to Health Connect → Confirm success → Delete photo (never reverse order)

**Network Failure Handling:**
- Automatic retry: Up to 3 additional attempts (4 total) with exponential backoff (1s, 2s, 4s delays)
- Retry criteria: Network errors (IOException), API timeouts, 5xx server errors
- Non-retryable errors: 4xx client errors (invalid request), JSON parse errors, validation failures
- WorkManager constraints: NetworkType.CONNECTED ensures work only runs when network available
- Manual retry option: Epic 4 adds UI for user-triggered retry after all automatic attempts fail

**Background Service Reliability:**
- WorkManager guarantees: Work executes even if app killed or device rebooted
- No foreground service needed: WorkManager handles reliability without persistent notification
- Constraint satisfaction: Work deferred until network available (automatic execution when constraint met)
- Battery optimization: Works correctly even with aggressive battery saver settings

**Health Connect Availability:**
- Availability check: Verify Health Connect installed on app launch (Epic 1)
- Permission handling: Request READ_NUTRITION and WRITE_NUTRITION on first app launch
- Graceful degradation: Epic 4 adds error handling for Health Connect unavailability
- No fallback storage: Health Connect is required for app functionality (no local database alternative)

**Crash Recovery:**
- WorkManager handles worker crashes: Automatically retries on crash (counts toward retry limit)
- Photo retention: Remains in cache if worker crashes mid-processing
- State recovery: No in-memory state needed (all state in WorkManager input data)
- Error logging: Timber logs exceptions for crash report analysis

### Observability

**Logging Strategy:**

**Debug Mode (BuildConfig.DEBUG = true):**
- Network traffic: OkHttp LoggingInterceptor at BODY level (full request/response logging)
- Worker execution: Timber.d() logs at each workflow step (photo loaded, API called, HC saved, photo deleted)
- API responses: Log token usage, response status, parsing results
- Performance: Log duration of key operations (API call time, image encoding time)

**Release Mode (BuildConfig.DEBUG = false):**
- Errors only: Timber.e() for exceptions, Timber.w() for validation failures
- No sensitive data: API keys, photo data, user details never logged
- Crash reporting: Prepared for Firebase Crashlytics integration (not implemented in V1.0)
- Minimal performance logging: Critical path timing only (widget launch, background processing duration)

**Key Metrics to Log:**

```kotlin
// Performance metrics
Timber.d("Widget launch time: ${launchDuration}ms")
Timber.d("API call completed in ${apiDuration}ms, tokens: ${usage.total_tokens}")
Timber.d("Background processing total: ${totalDuration}ms")

// Success/failure tracking
Timber.i("Meal analysis successful: ${calories} cal, ${description}")
Timber.w("API response validation failed: calories=$calories (out of range)")
Timber.e(exception, "Network error on attempt ${runAttemptCount}/3")

// State transitions
Timber.d("WorkManager job enqueued: ${workRequest.id}")
Timber.d("Photo deleted: ${photoFile.absolutePath}")
Timber.i("Health Connect record created: $recordId")
```

**Error Reporting:**
- Exception context: Include retry count, photo URI (path only, not image data), API response status
- User-facing errors: Map technical exceptions to friendly messages (handled in Epic 4)
- No PII logging: Exclude food descriptions, user data, API keys from error logs
- Debug information: Stack traces in debug mode, message-only in release mode

**WorkManager Monitoring:**
- Work status: Use `WorkManager.getWorkInfoByIdLiveData()` to monitor progress (Epic 4 UI)
- Retry tracking: Log each retry attempt with reason and delay
- Failure analysis: Log final failure reason after retry exhaustion
- Completion metrics: Track success rate, average duration, retry frequency

## Dependencies and Integrations

**Gradle Dependencies (from app/build.gradle.kts and Epic 1):**

| Dependency | Version | Purpose | Epic 2 Usage |
|------------|---------|---------|--------------|
| `androidx.work:work-runtime-ktx` | 2.9.1 | WorkManager for background processing | AnalyzeMealWorker execution with retry logic |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | HTTP client for REST APIs | Azure OpenAI API calls |
| `com.squareup.retrofit2:converter-gson` | 2.11.0 | JSON serialization/deserialization | API request/response parsing |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client foundation | Network communication |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | HTTP request/response logging | Debug mode network inspection |
| `androidx.glance:glance-appwidget` | 1.1.1 | Modern widget framework | Lock screen widget implementation |
| `androidx.health.connect:connect-client` | 1.1.0 | Health Connect SDK | Nutrition data persistence (from Epic 1) |
| `com.jakewharton.timber:timber` | 5.0.1 | Logging framework | Error logging and performance tracking (from Epic 1) |
| `androidx.security:security-crypto` | 1.1.0-alpha06 | Encrypted preferences | API key storage (from Epic 1) |
| `com.google.dagger:hilt-android` | 2.51.1 | Dependency injection | Module and worker injection (from Epic 1) |

**New Dependencies Added in Epic 2:**
- `androidx.glance:glance-appwidget:1.1.1` - Lock screen widget support

**External Service Integrations:**

**Azure OpenAI Service:**
- **Service:** Azure OpenAI Responses API (NOT public OpenAI API)
- **Model:** GPT-4.1 (multimodal vision-capable model)
- **Endpoint Pattern:** `https://{your-resource-name}.openai.azure.com/openai/v1/responses`
- **Authentication:** API key in `api-key` header (NOT `Authorization: Bearer` token)
- **Configuration Required:**
  - Azure OpenAI resource name (endpoint URL base)
  - API key (from Azure Portal)
  - Model/deployment name (e.g., "gpt-4.1")
- **Cost Considerations:** Token usage per request (~1200-1500 input tokens per 2MP image, ~25 output tokens)
- **Rate Limits:** Azure OpenAI enforces per-minute request limits (varies by subscription tier)
- **Documentation:** https://learn.microsoft.com/en-us/azure/ai-services/openai/reference

**Google Health Connect:**
- **Service:** Local Android API for health and fitness data
- **API Level:** Minimum Android 9 (API 28), optimal Android 14+ (API 34)
- **Permissions Required:** `READ_NUTRITION`, `WRITE_NUTRITION`
- **Data Model:** `NutritionRecord` with `energy` (calories) and `name` (description) fields
- **Integration Point:** HealthConnectManager (implemented in Epic 1, extended in Epic 2)
- **Availability Check:** `HealthConnectClient.isAvailable(context)` on app launch
- **Play Store Dependency:** Health Connect app must be installed (prompts user if missing)

**System Camera Integration:**
- **Android API:** `MediaStore.ACTION_IMAGE_CAPTURE` intent
- **Requirements:** Camera permission (`CAMERA`)
- **Photo Storage:** Temporary file in app cache directory
- **Result Handling:** `ActivityResultContract<Uri, Boolean>` for photo capture result
- **Fallback:** No CameraX dependency (system camera provides fastest launch time)

**File System Dependencies:**

```kotlin
// Photo cache directory structure
{app-data-dir}/cache/photos/
├── meal_1699550430000.jpg  // Timestamp-based naming
├── meal_1699550731000.jpg
└── meal_1699551022000.jpg  // Deleted after successful processing

// Cleanup policy:
// - Delete immediately after Health Connect save
// - Delete after retry exhaustion (3 failed attempts)
// - Periodic cleanup: Remove photos older than 24 hours (Epic 4)
// - System cleanup: Android can clear cache if storage low
```

**Deep Linking Configuration:**

```kotlin
// AndroidManifest.xml
<activity android:name=".ui.MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="foodie" android:host="capture" />
    </intent-filter>
</activity>

// NavGraph.kt
NavHost(navController, startDestination = "home") {
    composable(
        route = "camera",
        deepLinks = listOf(navDeepLink { uriPattern = "foodie://capture" })
    ) {
        CameraScreen(...)
    }
}
```

**Network Requirements:**
- **Connectivity:** Internet connection required for API calls (WiFi or mobile data)
- **Bandwidth:** ~500KB per API request (base64-encoded 2MP JPEG)
- **Reliability:** Retry logic handles transient network failures
- **Offline Handling:** WorkManager defers execution until network available (Epic 4 adds user notification)

**Android System Requirements:**
- **Minimum SDK:** API 28 (Android 9.0) - Health Connect compatibility
- **Target SDK:** API 35 (Android 15)
- **Lock Screen Widget:** API 31+ (Android 12+) for Glance lock screen widgets
- **Camera Access:** Camera permission required on first widget activation
- **Storage:** Minimal (photos deleted immediately, only nutrition metadata persisted)

## Acceptance Criteria (Authoritative)

**Derived from PRD FR-1 through FR-5 and Epic 2 Stories:**

**AC-1: Lock Screen Widget Functionality (Story 2.1)**
- Widget displays on Android lock screen with app icon and "Log Meal" label
- Widget size is smallest available for lock screen (small/medium constraint)
- Single tap launches camera directly without device unlock (if security policy allows)
- Widget launch time < 500ms from tap to camera ready state
- Widget remains functional after device reboot
- Widget works without app actively running in background
- Deep link `foodie://capture` routes to camera screen regardless of app state (cold start, background, foreground)

**AC-2: Photo Capture with Retake Option (Story 2.2)**
- System camera intent launches in full-screen mode
- User can capture photo with single tap or volume button
- Preview screen shows captured photo with "Retake" and "Use Photo" buttons
- Photo saved to app cache directory only after "Use Photo" confirmation
- Photo resolution capped at 2MP maximum
- Photo format is JPEG with 80% compression quality
- Auto-rotate handles device orientation correctly
- Capture button is large enough for easy thumb access (minimum 48dp)
- "Retake" button returns to camera without saving photo
- "Use Photo" button saves photo and initiates background processing

**AC-3: Azure OpenAI API Integration (Story 2.3)**
- Photo is base64-encoded before sending to API
- API request uses Azure OpenAI Responses API endpoint format: `https://{resource}.openai.azure.com/openai/v1/responses`
- Authentication uses `api-key` header (NOT `Authorization: Bearer`)
- Request uses `model` field to specify deployment name (e.g., "gpt-4.1")
- Request uses `instructions` field for system-level guidance
- Request uses `input` array with multimodal content (text prompt + base64 image)
- Response contains `output_text` field with JSON: `{"calories": number, "description": string}`
- Calories are validated: must be > 0 and < 5000
- Description is validated: must not be blank, maximum 200 characters
- API call completes within 30-second timeout (typical < 10 seconds)
- API key and endpoint retrieved from SecurePreferences (EncryptedSharedPreferences)
- Network errors return Result.error for retry handling
- Parse errors are logged and return Result.failure (non-retryable)

**AC-4: Background Processing with WorkManager (Story 2.4)**
- WorkManager job enqueued immediately after "Use Photo" confirmation
- User can return to previous activity (or lock screen) without waiting
- Worker executes only when network connection available (NetworkType.CONNECTED constraint)
- Worker calls Azure OpenAI API with photo
- Worker parses response to extract calories and description
- Worker validates nutrition data (calories in 1..5000 range)
- Worker saves to Health Connect on successful API response
- Worker deletes photo file after successful Health Connect save
- Entire background process completes in < 15 seconds typical (network dependent)
- Retry logic: Up to 3 additional attempts (4 total) with exponential backoff (1s, 2s, 4s delays)
- Retry triggers on: Network errors (IOException), API timeouts, 5xx server errors
- Non-retryable errors: 4xx client errors, JSON parse errors, validation failures
- Photo persists in cache during retry attempts
- Photo deleted after retry limit exhausted (even on failure)
- Worker survives app termination and device sleep (WorkManager reliability)

**AC-5: Health Connect Nutrition Data Save (Story 2.5)**
- `NutritionRecord` created with `energy` field containing calories in kcal
- `NutritionRecord` includes `name` field containing food description from AI
- `startTime` and `endTime` set to photo capture timestamp
- Record inserted using `HealthConnectClient.insertRecords()` API
- Save operation returns Health Connect record ID on success
- Data immediately visible in Google Fit or other Health Connect apps
- Permission errors handled gracefully (logged, worker returns failure)
- Health Connect unavailability handled gracefully (logged, worker returns failure)

**AC-6: End-to-End Integration (Story 2.6)**
- Complete flow works seamlessly: widget tap → camera → photo capture → background processing → Health Connect save
- Widget tap to camera launch: < 500ms
- Photo capture + confirmation: < 5 seconds total (one-handed operation validated)
- User can return to previous activity immediately after confirmation
- Background processing completes in < 15 seconds typical
- Nutrition data appears in Health Connect automatically
- Temporary photo deleted after successful save
- Haptic feedback occurs on photo capture (physical confirmation)
- Visual confirmation displays briefly on successful capture (checkmark or toast)
- Entire flow feels seamless with no visible delays or interruptions

**AC-7: Deep Linking Validation (Story 2.0)**
- Deep link `foodie://capture` navigates to camera screen from cold start
- Deep link navigates to camera screen when app in background
- Deep link navigates to camera screen when app in foreground (different screen)
- Deep link from widget PendingIntent works correctly
- Deep link from external source (ADB test, browser) works correctly
- Navigation back stack preserved correctly (back button returns to previous screen)
- Deep link parameter validation prevents invalid routes
- Deep link error handling provides user feedback for malformed URIs

## Traceability Mapping

| Acceptance Criterion | Spec Section(s) | Component(s)/API(s) | Test Strategy |
|---------------------|-----------------|---------------------|---------------|
| **AC-1: Lock Screen Widget** | Services and Modules → Lock Screen Widget Module, Workflows → Deep Link Navigation | MealCaptureWidget (GlanceAppWidget), MealCaptureWidgetReceiver, PendingIntent with foodie://capture deep link | Unit test: Widget configuration, deep link URI construction<br>Integration test: Widget tap triggers CameraActivity launch<br>Performance test: Measure launch time on mid-range device |
| **AC-2: Photo Capture** | Services and Modules → Camera Module, Data Models → WorkManager Data Keys | CameraActivity, System Camera Intent (ACTION_IMAGE_CAPTURE), ImageUtils (compression), File storage in cache/photos/ | Unit test: Image compression to 2MP + 80% JPEG quality<br>Integration test: Camera intent launch and result handling<br>Manual test: One-handed operation with thumb, retake functionality |
| **AC-3: Azure OpenAI API** | APIs and Interfaces → Azure OpenAI Integration, Data Models → API DTOs | AzureOpenAiApi (Retrofit), AuthInterceptor, NutritionAnalysisRepository, Gson parser | Unit test: Request body serialization, response parsing, validation logic<br>Integration test: Mock API responses for success/error scenarios<br>Contract test: Verify API request format matches Azure OpenAI spec |
| **AC-4: Background Processing** | Services and Modules → Background Processing Module, Workflows → Complete End-to-End Flow | AnalyzeMealWorker, WorkManager with constraints and backoff policy, NutritionAnalysisRepository | Unit test: Worker logic with mocked repository and Health Connect<br>Integration test: WorkManager execution with network constraint<br>Retry test: Simulate network failures, verify exponential backoff |
| **AC-5: Health Connect Save** | APIs and Interfaces → Health Connect Extension, Data Models → NutritionData | HealthConnectManager.insertNutritionRecord(), NutritionRecord with energy + name fields | Unit test: NutritionRecord creation with correct field values<br>Integration test: Insert and query from Health Connect SDK<br>Verification: Check data in Google Fit app |
| **AC-6: End-to-End Integration** | Workflows → Complete End-to-End Flow (all sections) | All components integrated: Widget → Camera → Worker → API → Health Connect | End-to-end test: Full flow with real API calls (using test API key)<br>Performance test: Measure total time from widget tap to HC save<br>Manual test: Real-world usage with actual food photos, one-handed operation |
| **AC-7: Deep Linking** | System Architecture Alignment → Deep Linking Architecture, Workflows → Deep Link Navigation | NavGraph with deep link routes, MainActivity intent filters, NavController | Unit test: Deep link URI parsing and route matching<br>Integration test: Navigate via deep link from various app states<br>ADB test: `adb shell am start -d "foodie://capture"` |

**Cross-Reference to PRD Requirements:**

| PRD Requirement | Epic 2 Acceptance Criteria | Implementation Status |
|-----------------|---------------------------|----------------------|
| FR-1: Lock Screen Widget | AC-1 | Fully implemented in Story 2.1 |
| FR-2: Food Photo Capture | AC-2 | Fully implemented in Story 2.2 |
| FR-3: AI Nutrition Analysis | AC-3 | Fully implemented in Story 2.3 |
| FR-4: Background Processing | AC-4 | Fully implemented in Story 2.4 |
| FR-5: Health Connect Data Storage | AC-5 | Fully implemented in Story 2.5 |
| NFR-1: Performance (Speed) | AC-6 (timing requirements) | Validated in Story 2.6 integration testing |
| NFR-2: Reliability (Retry Logic) | AC-4 (retry behavior) | Implemented in Story 2.4 |
| NFR-3: Security (API Key) | AC-3 (SecurePreferences) | Uses Epic 1 infrastructure |

## Risks, Assumptions, Open Questions

**Risks:**

**R-1: Widget Launch Performance on Low-End Devices**
- **Risk:** Lock screen widget launch may exceed 500ms target on budget Android devices with limited RAM
- **Impact:** Core value proposition ("invisible tracking") degraded if widget feels slow
- **Mitigation:** Use system camera intent (fastest option), minimize deep link processing overhead, test on mid-range devices during development
- **Contingency:** If < 500ms unachievable, adjust success criteria to < 1 second with documented device limitations

**R-2: Azure OpenAI API Rate Limiting**
- **Risk:** User may hit Azure OpenAI rate limits during high-frequency meal logging (e.g., multiple snacks in short period)
- **Impact:** API calls fail with 429 status, retry logic exhausts attempts, user sees error notification
- **Mitigation:** Document rate limits in user guidance (Epic 5), implement rate limit detection (check 429 status code)
- **Contingency:** Epic 4 adds manual retry UI, user can retry after rate limit window expires

**R-3: AI Accuracy Variance on Unusual Foods**
- **Risk:** Azure OpenAI may return inaccurate calorie estimates for uncommon foods, mixed dishes, or poor quality photos
- **Impact:** User loses trust in AI estimates, increases manual corrections (edit frequency)
- **Mitigation:** Accept variance as expected behavior (per PRD: "trust-based validation"), provide easy edit capability in Epic 3
- **Contingency:** Future V2.0 could add confidence scores, allow user to flag inaccurate estimates for retraining

**R-4: Health Connect Availability on Older Devices**
- **Risk:** Health Connect may not be available on Android 9-13 devices (requires separate app install from Play Store)
- **Impact:** App unusable if Health Connect not installed, user must install additional app
- **Mitigation:** Check availability on app launch (Epic 1), provide clear prompt to install from Play Store
- **Contingency:** If Health Connect adoption too low, consider fallback local database (contradicts architecture decision, last resort only)

**R-5: WorkManager Execution Delays on Battery Saver**
- **Risk:** Aggressive battery saver settings may delay WorkManager execution beyond 15-second target
- **Impact:** User doesn't see immediate background processing, may think app failed
- **Mitigation:** Document expected behavior in user guidance, WorkManager guarantees eventual execution
- **Contingency:** Epic 4 adds foreground notification showing "Analyzing meal..." with progress indication

**R-6: Photo Compression Quality Loss**
- **Risk:** Compressing photos to 2MP + 80% JPEG may degrade image quality enough to impact AI accuracy
- **Impact:** More incorrect calorie estimates, increased edit frequency
- **Mitigation:** Test with real food photos at different compression levels, validate AI accuracy doesn't significantly degrade
- **Contingency:** If accuracy drops > 10%, increase compression quality to 90% or max resolution to 3MP

**Assumptions:**

**A-1: Azure OpenAI Service Availability**
- **Assumption:** Azure OpenAI Responses API will remain available and stable with < 1% downtime
- **Validation:** Monitor API status page, implement retry logic for transient failures
- **Impact if False:** App unusable during outages, user cannot log meals (no offline mode in V1.0)

**A-2: User Has Azure OpenAI API Key**
- **Assumption:** User can obtain Azure OpenAI API key (requires Azure subscription)
- **Validation:** Provide setup instructions in Epic 5 settings screen
- **Impact if False:** App cannot function without API key (core dependency)

**A-3: System Camera Performance**
- **Assumption:** Android system camera intent launches faster than CameraX custom implementation
- **Validation:** Benchmark both approaches during Story 2.2 implementation
- **Impact if False:** Switch to CameraX if system camera too slow (adds dependency complexity)

**A-4: One-Handed Operation is Achievable**
- **Assumption:** Users can capture usable photos one-handed while holding a plate
- **Validation:** Manual testing with actual use case (holding plate, thumb-only operation)
- **Impact if False:** May need two-handed workflow or voice activation (V2.0 enhancement)

**A-5: Health Connect Name Field Supports Descriptions**
- **Assumption:** `NutritionRecord.name` field can store food descriptions without length limitations
- **Validation:** Test with 200-character descriptions, verify no truncation
- **Impact if False:** May need to truncate descriptions or store in separate metadata field

**Open Questions:**

**Q-1: Should widget support home screen placement in addition to lock screen?**
- **Context:** PRD focuses on lock screen for speed, home screen widget offers alternative access
- **Decision Needed:** Epic 2 scope or defer to V2.0?
- **Recommendation:** Defer to V2.0 - lock screen widget validates core hypothesis first

**Q-2: How should app handle multiple concurrent photo captures?**
- **Context:** User could theoretically tap widget multiple times before first photo processed
- **Decision Needed:** Queue multiple WorkManager jobs or prevent concurrent captures?
- **Recommendation:** Allow queueing (WorkManager handles concurrency), Epic 4 adds UI to show pending analyses

**Q-3: Should photo deletion be user-configurable (keep photos for debugging)?**
- **Context:** Developer may want to inspect photos to debug AI accuracy issues
- **Decision Needed:** Add debug mode setting to preserve photos?
- **Recommendation:** Add build config flag (not user setting) - photos retained in debug builds, deleted in release builds

**Q-4: What should happen if Health Connect permissions revoked mid-session?**
- **Context:** User could revoke permissions while app running or WorkManager job processing
- **Decision Needed:** Re-request permissions or fail gracefully?
- **Recommendation:** Epic 4 adds permission check before API call, prompts user to re-grant if revoked

**Q-5: Should app support custom AI prompts for special dietary needs?**
- **Context:** User with specific dietary restrictions may want tailored AI instructions (e.g., "focus on protein content")
- **Decision Needed:** Hardcode system instructions or make configurable?
- **Recommendation:** Hardcode in V1.0 (simpler), V2.0 could add custom prompt templates

## Test Strategy Summary

**Unit Testing (JUnit + Mockito):**

**Data Layer:**
- `NutritionAnalysisRepositoryTest`: Mock Retrofit API, test request building, response parsing, error handling, validation logic
- `AnalyzeMealWorkerTest`: Mock repository and Health Connect, test doWork() flow, retry logic, photo cleanup
- `ImageUtilsTest`: Test base64 encoding, image compression to 2MP + 80% JPEG, memory management

**Domain Layer:**
- `NutritionDataTest`: Validate init block constraints (calories 1..5000, description not blank, length <= 200)
- `ResultTest`: Test success/error/loading state handling, getOrNull(), exceptionOrNull() extensions

**Coverage Target:** 80%+ for critical business logic (repository, worker, data models)

**Integration Testing (AndroidTest + Espresso):**

**Widget Integration:**
- `MealCaptureWidgetTest`: Verify widget displays correctly, tap triggers PendingIntent with correct deep link URI
- `DeepLinkNavigationTest`: Verify `foodie://capture` routes to camera screen from various app states (cold start, background, foreground)

**API Integration:**
- `AzureOpenAiApiIntegrationTest`: Use WireMock to simulate API responses, test success/error scenarios, verify request format
- `RetrofitConfigTest`: Verify AuthInterceptor adds api-key header, base URL configuration, timeout values

**Health Connect Integration:**
- `HealthConnectManagerTest`: Insert NutritionRecord with energy + name, query records, verify data persists correctly
- Use test doubles for Health Connect SDK (avoid real Health Connect dependency in CI)

**WorkManager Integration:**
- `AnalyzeMealWorkerIntegrationTest`: Enqueue worker with test constraints, verify execution when network available, test retry behavior

**Coverage Target:** 70%+ for integration points (API, Health Connect, WorkManager)

**End-to-End Testing (Manual + Automated):**

**Full Flow Validation:**
1. Launch app on physical device with real Azure OpenAI API key configured
2. Tap lock screen widget → Verify camera launches in < 500ms
3. Capture food photo (e.g., sandwich) → Confirm photo preview displays
4. Tap "Use Photo" → Verify return to previous activity immediately
5. Wait 15 seconds → Check Health Connect for new NutritionRecord
6. Verify record contains realistic calories (e.g., 400-600 for sandwich) and description (e.g., "turkey sandwich")
7. Verify photo deleted from cache directory
8. Open Google Fit → Verify nutrition data visible

**Performance Validation:**
- Measure widget launch time (instrument with `System.currentTimeMillis()` logs)
- Measure background processing duration (WorkManager start to Health Connect save)
- Test on mid-range device (e.g., Pixel 6a, Samsung Galaxy A-series)

**Error Scenario Testing:**
- Simulate network failure (airplane mode) → Verify WorkManager retry with exponential backoff
- Simulate API timeout (mock slow response) → Verify retry logic triggers
- Simulate API error (invalid API key) → Verify error logged, worker fails gracefully
- Simulate Health Connect unavailable → Verify error handling, user notification

**Accessibility Testing:**
- Verify large touch targets (48dp minimum) on camera capture button
- Test volume button capture alternative
- Verify screen reader support for widget and camera UI

**Battery Impact Testing:**
- Monitor battery drain during typical usage (10-15 meal captures per day)
- Verify no wake locks or continuous background services
- Test with aggressive battery saver settings enabled

**Regression Testing:**
- Re-run Epic 1 tests to ensure foundation not broken (Health Connect CRUD, error handling, logging)
- Verify navigation back stack correct after widget deep link
- Test app behavior after device reboot (widget remains functional, pending WorkManager jobs resume)

**Test Automation Strategy:**
- Unit tests: Run on every commit (CI pipeline)
- Integration tests: Run on PR merge (nightly CI build)
- End-to-end tests: Manual execution before release (automated E2E deferred to V2.0)

**Test Data:**
- Use sample food images for consistent API testing (sandwich, salad, pizza, burger)
- Mock API responses with realistic calorie ranges (200-800 cal typical)
- Test edge cases: very low calories (50), very high calories (2000+), invalid responses (no JSON)

**Success Metrics:**
- All unit tests pass (100% on critical paths)
- Integration tests pass (95%+ - allow for flaky network tests)
- End-to-end flow completes successfully on 3+ different Android devices
- Performance targets met (widget < 500ms, background < 15s typical)
- Zero data loss during error scenario testing (photo retained until HC save or retry exhaustion)
