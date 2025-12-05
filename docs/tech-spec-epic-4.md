# Epic Technical Specification: Error Handling & Reliability

Date: 2025-11-13
Author: BMad
Epic ID: 4
Status: Draft

---

## Overview

Epic 4 transforms the Foodie application from a functional prototype into a production-ready tool by implementing comprehensive error handling, network resilience, and graceful degradation patterns. This epic ensures the core meal capture flow works reliably in real-world conditions including poor network connectivity, API failures, permission revocations, and resource constraints.

The epic delivers a robust application that never loses user data, provides clear feedback during error conditions, automatically retries transient failures, and guides users to resolution when manual intervention is required. All critical paths are hardened with retry logic, validation, and fallback behaviours that maintain the sub-5-second capture experience promise even when operating at the limits of network or system resources.

## Objectives and Scope

**In Scope:**
- Network connectivity detection and monitoring before API calls
- Exponential backoff retry logic for transient API failures (up to 3 retries)
- Comprehensive API error classification with user-friendly messaging
- Photo retention strategy: keep until successful save or retry exhaustion
- Automatic 24-hour cleanup for orphaned photos
- Health Connect permission and availability validation
- Permission re-request flows when access is revoked mid-session
- Graceful degradation with actionable error notifications
- Persistent notifications for errors requiring user intervention
- Android 13+ notification permission handling

**Out of Scope:**
- Offline queueing with automatic processing (deferred to V2.0)
- Multi-photo upload or batch processing
- User-configurable retry counts or delays
- Advanced error analytics or crash reporting dashboards
- Network quality detection (latency/bandwidth measurement)
- Background sync service beyond WorkManager

**Success Criteria:**
- Zero data loss: All captured photos are either successfully processed or retained for manual retry
- Network failures trigger automatic retry up to 3 times with exponential backoff
- Permission errors surface clear guidance to user with direct links to system settings
- Photo storage automatically cleaned up within 24 hours if retries exhausted
- All error conditions provide actionable user feedback (no silent failures)
- App remains responsive and sub-5-second capture flow is maintained even during error scenarios

## System Architecture Alignment

This epic enhances the existing architecture established in Epic 1 with reliability patterns:

**Enhanced Components:**

**AnalyseMealWorker** (data/worker/)
- Adds network connectivity check before API calls
- Implements exponential backoff retry policy (WorkManager configuration)
- Classifies API errors into retryable vs non-retryable categories
- Updates foreground notification with retry status
- Deletes photos only after successful save or all retries exhausted

**HealthConnectRepository** (data/repository/)
- Adds permission validation before all CRUD operations
- Implements permission re-request flow with user guidance
- Checks Health Connect availability before operations
- Returns structured errors with actionable user messages

**New Components:**

**NetworkMonitor** (data/network/)
- Purpose: Centralized network connectivity detection
- Responsibilities: Monitor network state, expose connectivity Flow
- Integration: Injected into AnalyseMealWorker via Hilt

**ErrorHandler** (domain/error/)
- Purpose: Map exceptions to user-friendly error messages
- Responsibilities: Classify errors, provide retry recommendations, generate notification text
- Integration: Used by repositories and workers to create Result.Error instances

**PhotoCleanupWorker** (data/worker/)
- Purpose: Periodic cleanup of orphaned photos (24-hour retention max)
- Responsibilities: Scan cache directory, delete photos older than 24 hours
- Scheduling: Daily periodic WorkManager task

**Architectural Patterns:**

**Retry Policy:**
```
Attempt 1: Immediate (0s delay)
Attempt 2: After 1 second exponential backoff
Attempt 3: After 2 seconds exponential backoff
Attempt 4: After 4 seconds exponential backoff
Final: Persistent notification with manual retry option
```

**Error Classification:**
```
Retryable Errors:
- NetworkError (IOException, SocketTimeoutException)
- ServerError (HTTP 500-599)
- HealthConnectUnavailable (temporary)

Non-Retryable Errors:
- AuthError (HTTP 401, 403 - invalid API key)
- RateLimitError (HTTP 429 - requires user delay)
- ParseError (invalid JSON response)
- ValidationError (calories outside 1-5000 range)
- PermissionDenied (Health Connect permissions revoked)
```

**Photo Lifecycle with Error Handling:**
```
1. Photo captured → saved to cache
2. WorkManager enqueued → network check
3. If offline → retain photo, schedule retry
4. API call → success: save HC + delete photo
5. API call → retryable error: backoff + retry
6. API call → non-retryable error: notify user + delete photo
7. After 3 retries exhausted → notify user + retain photo for manual retry
8. PhotoCleanupWorker → delete photos > 24 hours old
```

## Detailed Design

### Services and Modules

**NetworkMonitor Service:**

| Property | Type | Description |
|----------|------|-------------|
| isConnected | StateFlow<Boolean> | Reactive network connectivity state |
| networkType | StateFlow<NetworkType> | WIFI, CELLULAR, NONE |

| Method | Returns | Description |
|--------|---------|-------------|
| checkConnectivity() | Boolean | Synchronous connectivity check |
| waitForConnectivity() | suspend Unit | Suspends until network available |

**ErrorHandler Utility:**

| Method | Returns | Description |
|--------|---------|-------------|
| classify(exception: Throwable) | ErrorType | Maps exception to error category |
| getUserMessage(error: ErrorType) | String | User-friendly error description |
| isRetryable(error: ErrorType) | Boolean | Determines if retry should occur |
| getNotificationContent(error: ErrorType) | NotificationContent | Title, message, action text |

**PhotoCleanupWorker:**

| Property | Type | Description |
|----------|------|-------------|
| RETENTION_HOURS | Int | 24 hours maximum photo retention |
| CLEANUP_INTERVAL | Duration | Daily periodic execution |

| Method | Returns | Description |
|--------|---------|-------------|
| doWork() | Result | Scans cache, deletes old photos |
| getPhotoAge(file: File) | Duration | Calculates photo age from filename |

### Data Models and Contracts

**ErrorType Sealed Class:**

```kotlin
sealed class ErrorType {
    // Retryable errors
    data class NetworkError(val cause: Throwable) : ErrorType()
    data class ServerError(val statusCode: Int, val message: String) : ErrorType()
    data object HealthConnectUnavailable : ErrorType()
    
    // Non-retryable errors
    data class AuthError(val message: String) : ErrorType()
    data class RateLimitError(val retryAfter: Int?) : ErrorType()
    data class ParseError(val cause: Throwable) : ErrorType()
    data class ValidationError(val field: String, val reason: String) : ErrorType()
    data class PermissionDenied(val permissions: List<String>) : ErrorType()
    
    // Generic fallback
    data class UnknownError(val cause: Throwable) : ErrorType()
}
```

**NotificationContent Data Class:**

```kotlin
data class NotificationContent(
    val title: String,
    val message: String,
    val actionText: String?,
    val actionIntent: PendingIntent?,
    val isOngoing: Boolean
)
```

**WorkData Constants:**

```kotlin
object WorkDataKeys {
    const val KEY_PHOTO_URI = "photo_uri"
    const val KEY_PHOTO_TIMESTAMP = "photo_timestamp"
    const val KEY_RETRY_COUNT = "retry_count"
    const val KEY_ERROR_TYPE = "error_type"
    const val KEY_ERROR_MESSAGE = "error_message"
}
```

### APIs and Interfaces

**NetworkMonitor Interface:**

```kotlin
interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>
    val networkType: StateFlow<NetworkType>
    
    fun checkConnectivity(): Boolean
    suspend fun waitForConnectivity()
}

enum class NetworkType {
    WIFI, CELLULAR, NONE
}
```

**Enhanced AnalyseMealWorker Flow:**

```kotlin
override suspend fun doWork(): Result {
    // 1. Check network connectivity
    if (!networkMonitor.isConnected.value) {
        return if (runAttemptCount < MAX_RETRIES) {
            notifyUser("No internet - will retry when online")
            Result.retry()
        } else {
            notifyUser("Still offline - tap to retry manually", persistentNotification = true)
            Result.failure(workDataOf(KEY_ERROR_TYPE to "NetworkError"))
        }
    }
    
    // 2. Load photo from cache
    val photoUri = inputData.getString(KEY_PHOTO_URI) ?: return Result.failure()
    val photoBytes = loadPhoto(photoUri)
    
    // 3. Call Azure OpenAI API
    val apiResult = try {
        azureOpenAiApi.analyseNutrition(buildRequest(photoBytes))
    } catch (e: IOException) {
        return handleNetworkError(e)
    } catch (e: HttpException) {
        return handleHttpError(e)
    }
    
    // 4. Validate response
    val nutrition = try {
        parseAndValidate(apiResult)
    } catch (e: Exception) {
        return handleParseError(e)
    }
    
    // 5. Save to Health Connect with permission check
    val saveResult = try {
        healthConnectRepository.insertNutrition(
            calories = nutrition.calories,
            description = nutrition.description,
            timestamp = Instant.now()
        )
    } catch (e: SecurityException) {
        return handlePermissionError(e)
    }
    
    // 6. Delete photo on success
    deletePhoto(photoUri)
    
    // 7. Dismiss notification
    notificationManager.cancel(NOTIFICATION_ID)
    
    return Result.success()
}

private fun handleNetworkError(e: IOException): Result {
    Timber.e(e, "Network error analysing meal")
    return if (runAttemptCount < MAX_RETRIES) {
        updateNotification("Retrying analysis... (${runAttemptCount + 1}/$MAX_RETRIES)")
        Result.retry()
    } else {
        notifyUser("Network error after $MAX_RETRIES attempts. Tap to retry.", persistentNotification = true)
        Result.failure(workDataOf(
            KEY_ERROR_TYPE to "NetworkError",
            KEY_ERROR_MESSAGE to errorHandler.getUserMessage(ErrorType.NetworkError(e))
        ))
    }
}

private fun handleHttpError(e: HttpException): Result {
    val errorType = when (e.code()) {
        in 400..499 -> {
            if (e.code() == 401 || e.code() == 403) {
                ErrorType.AuthError("Invalid API key")
            } else if (e.code() == 429) {
                ErrorType.RateLimitError(retryAfter = e.response()?.headers()?.get("Retry-After")?.toIntOrNull())
            } else {
                ErrorType.ValidationError("API Request", e.message())
            }
        }
        in 500..599 -> ErrorType.ServerError(e.code(), e.message())
        else -> ErrorType.UnknownError(e)
    }
    
    return if (errorHandler.isRetryable(errorType) && runAttemptCount < MAX_RETRIES) {
        updateNotification("Server error - retrying... (${runAttemptCount + 1}/$MAX_RETRIES)")
        Result.retry()
    } else {
        notifyUser(errorHandler.getUserMessage(errorType), persistentNotification = true)
        deletePhoto(photoUri) // Delete photo for non-retryable errors to avoid cache bloat
        Result.failure(workDataOf(
            KEY_ERROR_TYPE to errorType::class.simpleName,
            KEY_ERROR_MESSAGE to errorHandler.getUserMessage(errorType)
        ))
    }
}

private fun handlePermissionError(e: SecurityException): Result {
    Timber.e(e, "Health Connect permission denied")
    notifyUser(
        "Health Connect permissions required. Tap to grant access.",
        persistentNotification = true,
        actionIntent = createPermissionIntent()
    )
    // Retain photo - user may grant permission and retry
    return Result.failure(workDataOf(
        KEY_ERROR_TYPE to "PermissionDenied",
        KEY_ERROR_MESSAGE to errorHandler.getUserMessage(ErrorType.PermissionDenied(emptyList()))
    ))
}

companion object {
    private const val MAX_RETRIES = 3
    private const val NOTIFICATION_ID = 1001
}
```

**Enhanced HealthConnectRepository:**

```kotlin
class HealthConnectRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val errorHandler: ErrorHandler
) : HealthConnectRepository {
    
    override suspend fun insertNutrition(
        calories: Int,
        description: String,
        timestamp: Instant
    ): Result<String> {
        // Validate Health Connect availability
        if (!healthConnectManager.isAvailable()) {
            return Result.Error(ErrorType.HealthConnectUnavailable)
        }
        
        // Check permissions
        if (!healthConnectManager.hasPermissions()) {
            return Result.Error(ErrorType.PermissionDenied(
                listOf("WRITE_NUTRITION", "READ_NUTRITION")
            ))
        }
        
        // Perform insert
        return try {
            val recordId = healthConnectManager.insertNutritionRecord(
                calories = calories,
                description = description,
                timestamp = timestamp
            )
            Result.Success(recordId)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission error saving to Health Connect")
            Result.Error(ErrorType.PermissionDenied(emptyList()))
        } catch (e: Exception) {
            Timber.e(e, "Failed to save nutrition to Health Connect")
            Result.Error(ErrorType.UnknownError(e))
        }
    }
    
    override suspend fun queryNutritionRecords(
        startTime: Instant,
        endTime: Instant
    ): Result<List<NutritionRecord>> {
        // Permission check before query
        if (!healthConnectManager.hasPermissions()) {
            return Result.Error(ErrorType.PermissionDenied(
                listOf("READ_NUTRITION")
            ))
        }
        
        return try {
            val records = healthConnectManager.queryNutritionRecords(startTime, endTime)
            Result.Success(records)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission error reading from Health Connect")
            Result.Error(ErrorType.PermissionDenied(emptyList()))
        } catch (e: Exception) {
            Timber.e(e, "Failed to query nutrition records")
            Result.Error(ErrorType.UnknownError(e))
        }
    }
}
```

### Workflows and Sequencing

**Network Error Retry Sequence:**

```
1. User confirms photo capture
   └─> WorkManager enqueues AnalyseMealWorker

2. Worker checks network connectivity
   ├─> Connected: Proceed to API call
   └─> Disconnected:
       ├─> Attempt 1-3: Schedule retry with exponential backoff
       │   └─> Show notification: "No internet - will retry when online"
       └─> Attempt 4: Show persistent notification: "Still offline - tap to retry"

3. API call succeeds
   └─> Save to Health Connect → Delete photo → Dismiss notification

4. API call fails (retryable error)
   ├─> Attempt 1-3: Schedule retry with exponential backoff
   │   └─> Update notification: "Retrying analysis... (2/3)"
   └─> Attempt 4: Persistent notification: "Analysis failed - tap to retry"

5. API call fails (non-retryable error)
   └─> Show error notification → Delete photo (prevent cache bloat)

6. Health Connect permission denied
   └─> Show notification with settings link → Retain photo for manual retry
```

**Photo Cleanup Workflow:**

```
1. PhotoCleanupWorker scheduled daily at 3 AM
   └─> WorkManager periodic task (CLEANUP_INTERVAL = 24h)

2. Worker scans getCacheDir()/photos/
   └─> For each file: Parse timestamp from filename (MEAL_yyyyMMdd_HHmmss.jpg)

3. Calculate photo age
   ├─> Age < 24 hours: Keep (pending or recent capture)
   └─> Age >= 24 hours: Delete (orphaned, retries exhausted)

4. Log cleanup results
   └─> Timber.i("PhotoCleanup: Deleted ${count} orphaned photos")
```

**Permission Re-request Flow:**

```
1. User performs action requiring Health Connect access (view list, edit entry)
   └─> Repository checks permissions via healthConnectManager.hasPermissions()

2. Permissions denied
   └─> Return Result.Error(PermissionDenied)
   
3. ViewModel receives error
   └─> Update UI state with error message + action button

4. User taps "Grant Access" button
   └─> Launch Health Connect permission request activity

5. User grants permissions in system dialog
   └─> Return to app, permission state updated

6. Retry original operation
   └─> Repository checks permissions → Success → Perform operation
```

## Non-Functional Requirements

### Performance

**Retry Timing:**
- Network connectivity check: < 50ms (ConnectivityManager.getActiveNetwork())
- Error classification: < 10ms (pattern matching on exception types)
- Photo cleanup scan: < 500ms for 50 photos in cache
- Permission validation: < 20ms (HealthConnectClient.permissionController.getGrantedPermissions())

**Retry Delays (Exponential Backoff):**
- Attempt 1: Immediate (0s)
- Attempt 2: 1 second
- Attempt 3: 2 seconds
- Attempt 4: 4 seconds
- Total max delay: 7 seconds across all retries

**Performance Impact:**
- Error handling overhead: < 50ms per operation (error classification + logging)
- Network monitoring: < 1% CPU impact (passive StateFlow collection)
- Photo cleanup: Runs during low-usage period (3 AM), minimal battery impact

### Security

**Photo Retention Security:**
- Photos stored in app cache directory (getCacheDir()) - automatically secured by Android app sandbox
- No photo data logged or transmitted except to Azure OpenAI API
- Photos deleted after successful processing or 24-hour cleanup window
- No photo backups or cloud storage

**Error Message Sanitization:**
- Technical details (stack traces, API keys) logged via Timber only in DEBUG builds
- User-facing messages never contain sensitive data (API keys, endpoints, tokens)
- Exception messages sanitized before display (remove paths, internal class names)

**Permission Validation:**
- Health Connect permissions checked before every CRUD operation
- No sensitive operations attempted without validated permissions
- Permission denial errors guide user to system settings without exposing internal state

### Reliability

**Zero Data Loss Guarantee:**
- Photos retained until successful Health Connect save OR all retries exhausted
- WorkManager persistence ensures retry survives app termination and device reboot
- Photo cleanup only deletes orphaned photos > 24 hours old (long after retry exhaustion)

**Graceful Degradation:**
- Network unavailable: User notified, automatic retry when connectivity restored
- API key invalid: Clear error message with link to settings, no retry loop
- Health Connect unavailable: Guidance to install from Play Store
- Permissions denied: Direct link to system permission settings
- Storage full: Clear error message, no photo capture until space freed

**Error Recovery:**
- All critical paths wrapped in try-catch with structured error handling
- Transient errors (network, server 5xx) trigger automatic retry
- Permanent errors (auth, validation) surface actionable guidance
- No silent failures - all errors logged and user notified

### Observability

**Logging Strategy:**

```kotlin
// Success logging
Timber.i("Meal analysis completed: ${nutrition.calories} cal, attempt ${runAttemptCount + 1}")

// Network error logging
Timber.w("Network unavailable during analysis, retry ${runAttemptCount + 1}/$MAX_RETRIES")

// API error logging (includes response for debugging)
Timber.e("API error ${e.code()}: ${e.message()}, response: ${e.response()?.errorBody()?.string()}")

// Permission error logging
Timber.e(e, "Health Connect permission denied: ${e.message}")

// Photo cleanup logging
Timber.i("PhotoCleanup: Scanned ${fileCount} photos, deleted ${deletedCount} orphaned")
```

**User-Visible Feedback:**
- Foreground service notification shows analysis progress and retry status
- Persistent notifications for errors requiring user action (permission, API key)
- Toast messages for transient errors (list refresh failed, etc.)
- SnackBars with action buttons for recoverable errors (retry, settings)

## Acceptance Criteria (Authoritative)

### AC-1: Network Detection Before API Calls
**Given** a photo has been captured and confirmed  
**When** background processing begins  
**Then** network connectivity is checked via NetworkMonitor before API call  
**And** if offline, a notification displays: "No internet - will retry when online"  
**And** the photo is retained in cache for later processing  
**And** WorkManager schedules retry with exponential backoff  
**And** network state changes trigger automatic retry when connectivity restored

### AC-2: Exponential Backoff Retry Logic
**Given** an Azure OpenAI API call fails with retryable error (network timeout, server 5xx)  
**When** retry logic activates  
**Then** the app retries up to 3 additional times (4 total attempts)  
**And** retry delays use exponential backoff: 0s, 1s, 2s, 4s  
**And** notification updates with retry status: "Retrying analysis... (2/3)"  
**And** same photo is used from cache for each retry attempt  
**And** after successful retry, processing continues normally (save to Health Connect, delete photo)

### AC-3: Retry Exhaustion Handling
**Given** API call fails 4 times (initial + 3 retries)  
**When** all retry attempts are exhausted  
**Then** a persistent notification displays: "Meal analysis failed. Tap to retry manually."  
**And** notification includes action button to retry  
**And** photo is retained in cache for manual retry  
**And** manual retry button creates new WorkManager task  
**And** notification remains visible until user dismisses or retries successfully

### AC-4: API Error Classification
**Given** various types of API errors can occur  
**When** an error response is received  
**Then** network timeouts (IOException) are classified as retryable NetworkError  
**And** authentication errors (401, 403) are classified as non-retryable AuthError  
**And** server errors (500-599) are classified as retryable ServerError  
**And** rate limit errors (429) are classified as non-retryable RateLimitError  
**And** invalid JSON responses are classified as non-retryable ParseError  
**And** retryable errors trigger exponential backoff retry  
**And** non-retryable errors show error notification and delete photo

### AC-5: User-Friendly Error Messages
**Given** an error condition occurs  
**When** the error is surfaced to the user  
**Then** network errors show: "Request timed out. Check your internet connection."  
**And** authentication errors show: "API key invalid. Check settings."  
**And** server errors show: "Service temporarily unavailable. Will retry automatically."  
**And** rate limit errors show: "Too many requests. Please wait a moment."  
**And** parse errors show: "Unexpected response from AI service."  
**And** permission errors show: "Health Connect permissions required. Tap to grant access."  
**And** all messages are actionable (guide user to resolution)

### AC-6: Photo Retention Strategy
**Given** a photo is captured and processing initiated  
**When** processing completes  
**Then** photo is deleted immediately after successful Health Connect save  
**And** photo is retained during retry attempts (up to 4 total attempts)  
**And** photo is deleted after all retries exhausted for non-retryable errors  
**And** photo is retained after retries exhausted for retryable errors (manual retry available)  
**And** photos are never retained longer than 24 hours (automatic cleanup)

### AC-7: Photo Cleanup Worker
**Given** orphaned photos may accumulate in cache  
**When** PhotoCleanupWorker executes (daily at 3 AM)  
**Then** worker scans getCacheDir()/photos/ directory  
**And** worker calculates age of each photo from filename timestamp  
**And** photos older than 24 hours are deleted  
**And** photos younger than 24 hours are retained (pending or recent)  
**And** cleanup results are logged: "PhotoCleanup: Deleted ${count} orphaned photos"  
**And** cleanup runs in background without user notification

### AC-8: Health Connect Permission Validation
**Given** Health Connect permissions may be revoked by user  
**When** a repository operation is attempted (insert, query, update, delete)  
**Then** permissions are validated before operation via healthConnectManager.hasPermissions()  
**And** if permissions denied, operation returns Result.Error(PermissionDenied)  
**And** error surfaces in UI with actionable message: "Health Connect permissions required"  
**And** "Grant Access" button launches Health Connect permission request  
**And** after user grants permissions, original operation can be retried  
**And** permission check completes in < 20ms (no blocking)

### AC-9: Health Connect Availability Check
**Given** Health Connect may not be installed on device  
**When** app attempts to use Health Connect  
**Then** availability is checked via healthConnectManager.isAvailable()  
**And** if unavailable, error message displays: "Health Connect required. Install from Play Store?"  
**And** notification includes action button to open Play Store  
**And** app checks availability on each foreground return  
**And** pending operations queue and retry after Health Connect installed

### AC-10: Graceful Degradation
**Given** various failure scenarios  
**When** app encounters errors  
**Then** camera permission denied shows settings link and disables capture  
**And** API key missing shows settings link and disables background processing  
**And** storage full shows clear message and prevents new captures  
**And** all notifications are dismissible but persistent for action-required errors  
**And** Android 13+ notification permission requested before showing notifications  
**And** no silent failures occur (all errors logged and surfaced)

## Traceability Mapping

| Acceptance Criteria | Spec Section | Component | Test Approach |
|---------------------|--------------|-----------|---------------|
| AC-1: Network Detection | Workflows and Sequencing | NetworkMonitor, AnalyseMealWorker | Unit test: Mock NetworkMonitor.isConnected = false, verify retry scheduled |
| AC-2: Exponential Backoff | APIs and Interfaces (AnalyseMealWorker) | AnalyseMealWorker, WorkManager | Instrumentation test: Trigger retries, verify delay timing |
| AC-3: Retry Exhaustion | APIs and Interfaces (handleNetworkError) | AnalyseMealWorker, NotificationManager | Unit test: runAttemptCount = 4, verify persistent notification shown |
| AC-4: Error Classification | Data Models (ErrorType) | ErrorHandler | Unit test: Map various exceptions to ErrorType, verify classification |
| AC-5: User Messages | ErrorHandler.getUserMessage() | ErrorHandler | Unit test: Verify each ErrorType returns correct user-facing message |
| AC-6: Photo Retention | APIs and Interfaces (AnalyseMealWorker) | AnalyseMealWorker | Unit test: Verify photo deletion logic based on success/failure/retry |
| AC-7: Photo Cleanup | PhotoCleanupWorker | PhotoCleanupWorker | Instrumentation test: Create old photos, run worker, verify deletion |
| AC-8: Permission Validation | HealthConnectRepository | HealthConnectRepository | Unit test: Mock hasPermissions() = false, verify Result.Error returned |
| AC-9: HC Availability | HealthConnectRepository | HealthConnectManager | Instrumentation test: Check isAvailable(), verify fallback behaviour |
| AC-10: Graceful Degradation | All components | ErrorHandler, ViewModels | Manual test: Trigger each failure mode, verify user guidance |

## Risks, Assumptions, Open Questions

### Risks

**R1: WorkManager Retry Reliability**
- **Risk:** WorkManager retry may be delayed or cancelled by Android battery optimization
- **Likelihood:** Medium (aggressive battery savers on some OEM devices)
- **Impact:** High (breaks automatic retry promise, user frustration)
- **Mitigation:** 
  - Request battery optimization exemption for Foodie app
  - Use setExpedited() for time-sensitive retries
  - Provide manual retry button as fallback
  - Test on multiple OEM devices (Samsung, Xiaomi aggressive battery savers)

**R2: Photo Cleanup Timing Conflicts**
- **Risk:** PhotoCleanupWorker deletes photo while retry is in progress
- **Likelihood:** Low (24-hour window is long after retry exhaustion)
- **Impact:** Medium (data loss if photo deleted during retry)
- **Mitigation:**
  - Check photo modification time, not just creation time
  - Add metadata file for each pending photo (mark as "in_progress")
  - Skip deletion if associated WorkManager task is still pending

**R3: Health Connect Revocation During Save**
- **Risk:** User revokes permissions between permission check and insert operation
- **Likelihood:** Low (narrow timing window)
- **Impact:** Medium (SecurityException not caught, app crash)
- **Mitigation:**
  - Wrap all Health Connect operations in try-catch SecurityException
  - Treat as non-retryable error, guide user to re-grant permissions
  - Retain photo for manual retry after permissions restored

**R4: Network State False Positives**
- **Risk:** NetworkMonitor reports connected but API call fails (captive portal, no internet access)
- **Likelihood:** Medium (common on public WiFi)
- **Impact:** Low (retry logic handles API failure)
- **Mitigation:**
  - Rely on API call failure detection rather than perfect network detection
  - Exponential backoff handles temporary connectivity issues
  - Consider adding connectivity.hasCapability(NET_CAPABILITY_VALIDATED) check

### Assumptions

**A1: WorkManager Backoff Policy**
- Assumption: WorkManager exponential backoff provides sufficiently spaced retries (1s, 2s, 4s)
- Validation: Test with actual WorkManager in instrumentation tests, measure retry timing
- Fallback: Implement custom retry scheduling if WorkManager delays are too long

**A2: 24-Hour Cleanup Window**
- Assumption: 24 hours is long enough for user to manually retry but short enough to avoid cache bloat
- Validation: Monitor photo cache size during real-world usage
- Adjustment: Reduce to 12 hours if cache grows excessively, increase to 48 hours if users report lost photos

**A3: Health Connect Always Available After Install**
- Assumption: Once Health Connect is installed, it remains available (not uninstalled mid-session)
- Validation: Check isAvailable() on each foreground return
- Fallback: Queue operations and retry if Health Connect becomes unavailable temporarily

**A4: Notification Permission Granted**
- Assumption: Users grant POST_NOTIFICATIONS permission on Android 13+ to see retry status
- Validation: Request permission on first capture attempt, explain importance
- Fallback: Fall back to silent retry if permission denied (log only, no user-visible feedback)

### Open Questions

**Q1: Should photo cleanup happen more frequently?**
- Current design: Daily at 3 AM (24-hour retention)
- Alternative: Every 6 hours (reduce cache size, more aggressive cleanup)
- Decision: Start with 24-hour daily cleanup, monitor cache size in production, adjust if needed

**Q2: Should non-retryable errors retain photos?**
- Current design: Delete photos for non-retryable errors (AuthError, ParseError) to avoid cache bloat
- Alternative: Retain all photos for manual review, user decides when to delete
- Decision: Delete for non-retryable errors (user cannot fix by retrying same photo), retain for retryable errors

**Q3: Should we add network quality detection?**
- Current design: Binary connected/disconnected check
- Alternative: Detect slow network (< 1 Mbps) and warn user before API call
- Decision: Defer to V2.0 - binary check sufficient for MVP, API timeout handles slow networks

**Q4: Should retry count be user-configurable?**
- Current design: Fixed 3 retries (4 total attempts)
- Alternative: Settings option for retry count (1-5 retries)
- Decision: Fixed for MVP (keep settings simple), consider user feedback for V2.0

**Q5: Should we implement offline queueing?**
- Current design: Retain photo, notify user when offline, automatic retry when online
- Alternative: Full offline queue with multiple pending photos, batch processing when online
- Decision: Deferred to V2.0 (adds complexity, MVP focuses on single-photo flow)

## Test Strategy Summary

### Unit Tests (70%+ Coverage)

**NetworkMonitor Tests:**
- `testIsConnectedTrue()` - Verify StateFlow emits true when network available
- `testIsConnectedFalse()` - Verify StateFlow emits false when network unavailable
- `testCheckConnectivity()` - Verify synchronous check returns correct state
- `testWaitForConnectivity()` - Verify suspends until network available

**ErrorHandler Tests:**
- `testClassifyNetworkError()` - IOException → NetworkError
- `testClassifyServerError()` - HttpException(503) → ServerError
- `testClassifyAuthError()` - HttpException(401) → AuthError
- `testClassifyRateLimitError()` - HttpException(429) → RateLimitError
- `testGetUserMessageNetworkError()` - Verify user-friendly message returned
- `testIsRetryableNetworkError()` - Verify NetworkError is retryable
- `testIsRetryableAuthError()` - Verify AuthError is non-retryable

**AnalyseMealWorker Tests:**
- `testNetworkCheckBeforeApiCall()` - Verify network checked, retry scheduled if offline
- `testExponentialBackoffRetry()` - Verify retry delays: 0s, 1s, 2s, 4s
- `testRetryExhaustion()` - After 4 attempts, verify persistent notification shown
- `testPhotoDeletedOnSuccess()` - Verify photo deleted after successful HC save
- `testPhotoRetainedDuringRetry()` - Verify photo kept during retry attempts
- `testPhotoDeletedAfterNonRetryableError()` - AuthError → delete photo
- `testPhotoRetainedAfterRetryableError()` - NetworkError → retain photo

**HealthConnectRepository Tests:**
- `testPermissionCheckBeforeInsert()` - Verify hasPermissions() checked before insert
- `testPermissionDeniedReturnsError()` - hasPermissions() = false → Result.Error(PermissionDenied)
- `testAvailabilityCheckBeforeInsert()` - Verify isAvailable() checked
- `testSecurityExceptionCaught()` - SecurityException thrown → Result.Error(PermissionDenied)

### Instrumentation Tests (50%+ Coverage)

**AnalyseMealWorker Integration Tests:**
- `testNetworkFailureTriggersRetry()` - Mock network failure, verify WorkManager reschedules
- `testRetryDelayTiming()` - Measure actual retry delays, assert within expected range
- `testPhotoCleanupAfter24Hours()` - Create old photo, run worker, verify deletion

**HealthConnectRepository Integration Tests:**
- `testPermissionRevocationMidSession()` - Revoke permissions, attempt operation, verify error handling
- `testHealthConnectUnavailableFlow()` - Uninstall HC (simulated), verify error surfaced

### Manual Testing Checklist

**Network Scenarios:**
- [ ] Enable airplane mode before capture, verify offline notification shown
- [ ] Disable airplane mode, verify automatic retry triggers
- [ ] Simulate poor network (network throttling), verify retry on timeout
- [ ] Capture on WiFi, switch to cellular mid-processing, verify completion

**API Error Scenarios:**
- [ ] Invalid API key in settings, verify AuthError notification shown
- [ ] Temporarily unreachable endpoint (mock server down), verify ServerError retry
- [ ] Send malformed request, verify ParseError handling
- [ ] Trigger rate limit (multiple rapid captures), verify RateLimitError message

**Health Connect Scenarios:**
- [ ] Revoke permissions mid-session, verify permission error on next save
- [ ] Uninstall Health Connect (if possible), verify availability error shown
- [ ] Reinstall Health Connect, verify app detects and resumes

**Photo Retention Scenarios:**
- [ ] Capture photo, verify saved to cache
- [ ] Successful processing, verify photo deleted immediately
- [ ] Network error with retry, verify photo retained during retries
- [ ] Retry exhaustion, verify photo retained for manual retry
- [ ] Wait 24+ hours, run PhotoCleanupWorker, verify old photo deleted

**Graceful Degradation:**
- [ ] Deny camera permission, verify capture disabled with settings link
- [ ] Deny notification permission (Android 13+), verify silent operation
- [ ] Fill device storage to capacity, verify storage full error shown
- [ ] Remove API key from settings, verify API key missing error on capture

### Performance Validation

**Retry Performance:**
- Measure total retry time: 0s + 1s + 2s + 4s = 7 seconds max
- Verify notification updates within 100ms of retry attempt
- Verify photo cleanup scan completes < 500ms for 50 photos

**Error Handling Overhead:**
- Measure error classification: < 10ms per exception
- Verify permission check: < 20ms per operation
- Verify network check: < 50ms per connectivity validation

---

## Dependencies and Integrations

### External Dependencies (build.gradle.kts)

**Existing Dependencies (No New Additions):**
```kotlin
// Core Android components (already configured)
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.work.runtime.ktx)

// Networking (for API error handling)
implementation(libs.retrofit)
implementation(libs.okhttp)
implementation(libs.okhttp.logging.interceptor)

// Health Connect (for permission validation)
implementation(libs.androidx.health.connect)

// Logging (for error tracking)
implementation(libs.timber)

// Testing (for unit and instrumentation tests)
testImplementation(libs.junit)
testImplementation(libs.mockito.core)
testImplementation(libs.mockk)
androidTestImplementation(libs.androidx.work.testing)
```

**No Additional Dependencies Required:** Epic 4 uses existing architecture components and enhances error handling patterns without adding new libraries.

### Integration Points

**WorkManager Integration:**
- Configure BackoffPolicy.EXPONENTIAL in AnalyseMealWorker
- Set Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
- Use setExpedited() for time-sensitive retries (Android 12+)

**Health Connect Integration:**
- Use HealthConnectClient.permissionController.getGrantedPermissions() for validation
- Use HealthConnectClient.isAvailable(context) for availability check
- Handle SecurityException on all CRUD operations

**Notification Integration:**
- Create notification channel for error notifications (ID: "meal_analysis_errors")
- Use NotificationCompat.Builder with Material You styling
- Add action buttons for retry and settings navigation
- Request POST_NOTIFICATIONS permission on Android 13+

**Repository Integration:**
- All repository methods return Result<T> with enhanced error types
- ViewModel error handling updated to display user-friendly messages
- UI components render error states with action buttons (retry, settings)

---

**Document Status:**
- ✅ Epic scope defined (error handling, retry logic, photo cleanup, permission validation)
- ✅ Architecture aligned with Epic 1 foundation
- ✅ Detailed design complete (NetworkMonitor, ErrorHandler, enhanced Worker/Repository)
- ✅ Acceptance criteria defined (10 ACs covering all error scenarios)
- ✅ Traceability mapping complete
- ✅ Test strategy comprehensive (unit, instrumentation, manual)
- ✅ Risks and open questions documented
- ✅ No new dependencies required

**Ready for:** Story creation and implementation by Dev agent

---

_This Tech Spec ensures Foodie handles real-world error conditions with zero data loss, automatic retry, and clear user guidance._

_Created through systematic analysis of PRD, Architecture, and Epic 2/3 retrospectives._
