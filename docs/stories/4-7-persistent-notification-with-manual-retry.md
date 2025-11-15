# Story 4.7: Persistent Notification with Manual Retry

Status: drafted

## Change Log

| Date | Author | Change |
|------|--------|--------|
| 2025-11-14 | BMad | Story created based on deferred items from Story 4.2 - Manual retry button in persistent notification |

## Story

As a user,
I want a retry button in the notification when meal analysis fails,
So that I can easily retry the analysis without recapturing the photo.

## Acceptance Criteria

**Given** all automatic retry attempts have been exhausted (4 attempts total)
**When** the meal analysis fails
**Then** a persistent notification is displayed with title "Meal analysis failed"

**And** the notification body shows the specific error category: "Network problem", "Storage problem", "Service unavailable", or "Unknown error"

**And** the notification includes a "Retry" action button

**And** the notification remains visible until user dismisses it or retries successfully

**And** when user taps the "Retry" button, a new WorkManager task is created to reprocess the retained photo

**And** when retry is initiated, the persistent notification is replaced with the standard foreground analysis notification showing "Analyzing meal..."

**And** the photo used for retry is the same photo from the original failed attempt (retained in cache)

**And** successful retry completes normally: saves to Health Connect, deletes photo, dismisses notification

**And** if retry fails again, the cycle repeats (up to 4 attempts again with exponential backoff)

## Tasks / Subtasks

- [ ] **Task 1: Documentation Research & Technical Validation** ⚠️ COMPLETE BEFORE PROCEEDING TO IMPLEMENTATION

  **Objective:** Understand Android notification actions and WorkManager task creation from PendingIntent

  **Required Research:**
  1. Review Android notification action documentation
     - Starting point: https://developer.android.com/develop/ui/views/notifications/build-notification#Actions
     - Focus: addAction() API, PendingIntent configuration for WorkManager
  
  2. Review WorkManager enqueue from BroadcastReceiver pattern
     - File: Review existing AnalyzeMealWorker enqueue logic in CapturePhotoViewModel
     - Focus: How to enqueue WorkRequest with same photo URI from notification action
  
  3. Review notification channels and persistence
     - File: `app/src/main/java/com/foodie/app/ui/notifications/MealAnalysisForegroundNotifier.kt`
     - Focus: How to create persistent notification vs dismissible foreground notification
  
  4. Review error classification for notification text
     - File: `app/src/main/java/com/foodie/app/domain/error/ErrorHandler.kt`
     - Focus: Map ErrorType to user-friendly category ("Network problem", "Storage problem", etc.)
  
  5. Validate assumptions:
     - PendingIntent can trigger WorkManager task enqueue
     - Photo URI can be passed through notification action intent extras
     - Persistent notification survives app termination
     - Notification action works when app is in background
  
  **Deliverable Checkpoint:** ✅ REQUIRED
  Document findings in Dev Notes before proceeding to Task 2:
  - [ ] Notification action architecture defined (BroadcastReceiver vs Service vs direct WorkManager enqueue)
  - [ ] Photo URI persistence strategy confirmed
  - [ ] Error category mapping defined (ErrorType → notification body text)
  - [ ] Risks/unknowns flagged for review (notification limits, PendingIntent mutability flags)
  
  ⚠️ Do NOT proceed to implementation tasks until research checkpoint is complete

- [ ] **Task 2: Create Persistent Notification Builder** (AC: #1, #2, #4)
  - [ ] Create new method in MealAnalysisForegroundNotifier: `createPersistentErrorNotification(errorType: ErrorType, photoUri: String): Notification`
  - [ ] Set notification title: "Meal analysis failed"
  - [ ] Map ErrorType to notification body text:
    - NetworkError → "Network problem"
    - ServerError → "Service unavailable"
    - UnknownError → "Unknown error"
    - (Add more mappings based on ErrorType sealed class)
  - [ ] Set notification priority to PRIORITY_DEFAULT (persistent but not alarming)
  - [ ] Set ongoing = false (user can dismiss)
  - [ ] Set autoCancel = false (stays visible after tap)
  - [ ] Use existing notification channel ID from foreground service

- [ ] **Task 3: Add Retry Action Button to Notification** (AC: #3, #5)
  - [ ] Create PendingIntent for retry action pointing to RetryBroadcastReceiver
  - [ ] Pass photo URI and error type as intent extras
  - [ ] Add action to notification: `addAction(R.drawable.ic_retry, "Retry", retryPendingIntent)`
  - [ ] Set PendingIntent flags: `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` (Android 12+)
  - [ ] Ensure retry icon resource exists or create simple retry icon

- [ ] **Task 4: Implement RetryBroadcastReceiver** (AC: #5, #6)
  - [ ] Create `data/worker/RetryBroadcastReceiver` extending BroadcastReceiver
  - [ ] Extract photo URI from intent extras in onReceive()
  - [ ] Enqueue AnalyzeMealWorker using WorkManager.getInstance().enqueueUniqueWork()
  - [ ] Use same WorkRequest configuration as CapturePhotoViewModel (BackoffPolicy, constraints)
  - [ ] Cancel the persistent notification after enqueueing work
  - [ ] Log retry action: `Timber.i("Manual retry initiated for photo: $photoUri")`
  - [ ] Register receiver in AndroidManifest.xml

- [ ] **Task 5: Update AnalyzeMealWorker Retry Exhaustion Logic** (AC: #1, #2, #7)
  - [ ] Modify retry exhaustion handling in AnalyzeMealWorker.doWork()
  - [ ] When runAttemptCount + 1 >= MAX_ATTEMPTS and error is retryable:
    - Do NOT delete photo (retain for manual retry)
    - Get ErrorType from errorHandler.classify(exception)
    - Show persistent notification via foregroundNotifier.createPersistentErrorNotification()
    - Return Result.failure() with error data
  - [ ] When runAttemptCount + 1 >= MAX_ATTEMPTS and error is non-retryable:
    - Delete photo (user cannot fix by retrying)
    - Show error notification without retry button
    - Return Result.failure()

- [ ] **Task 6: Handle Notification Replacement on Retry** (AC: #6)
  - [ ] In RetryBroadcastReceiver.onReceive(), cancel persistent error notification before enqueueing work
  - [ ] Use NotificationManager.cancel(PERSISTENT_ERROR_NOTIFICATION_ID)
  - [ ] AnalyzeMealWorker will show foreground analysis notification when work starts
  - [ ] Verify notification IDs don't conflict (use different ID for persistent vs foreground)

- [ ] **Task 7: Implement Retry Cycle Logic** (AC: #8, #9)
  - [ ] Manual retry starts fresh retry counter (runAttemptCount = 0)
  - [ ] Exponential backoff applies to manual retry attempts (same as original)
  - [ ] Successful retry follows normal flow: save to HC, delete photo, dismiss notification
  - [ ] Failed retry after 4 attempts shows persistent notification again
  - [ ] Ensure photo is retained across multiple manual retry cycles until success

- [ ] **Task 8: Map ErrorType to User-Friendly Categories** (AC: #2)
  - [ ] Add method to ErrorHandler or create NotificationMessageMapper utility
  - [ ] Map ErrorType to notification body strings:
    - ErrorType.NetworkError → "Network problem"
    - ErrorType.ServerError → "Service unavailable"
    - ErrorType.AuthError → "Authentication problem"
    - ErrorType.RateLimitError → "Too many requests"
    - ErrorType.ParseError → "Service problem"
    - ErrorType.ValidationError → "Request problem"
    - ErrorType.PermissionDenied → "Storage problem"
    - ErrorType.HealthConnectUnavailable → "Health Connect problem"
    - ErrorType.UnknownError → "Unknown error"
  - [ ] Return concise category string suitable for notification body

- [ ] **Task 9: Unit Tests for Retry Notification** (AC: All)
  - [ ] Test: `testPersistentNotificationCreated()` - Verify notification has correct title, body, action
  - [ ] Test: `testRetryActionPendingIntent()` - Verify PendingIntent contains correct extras
  - [ ] Test: `testErrorTypeToCategory()` - Verify ErrorType maps to correct category string
  - [ ] Test: `testPhotoRetainedOnRetryExhaustion()` - Verify photo not deleted when retryable error exhausted
  - [ ] Test: `testPhotoDeletedOnNonRetryableExhaustion()` - Verify photo deleted for non-retryable errors

- [ ] **Task 10: Instrumentation Tests for Retry Flow** (AC: All)
  - [ ] Test: `testManualRetryEnqueuesWork()` - Tap retry button, verify WorkManager task created
  - [ ] Test: `testNotificationReplacedOnRetry()` - Persistent notification dismissed when retry starts
  - [ ] Test: `testSuccessfulManualRetry()` - Manual retry succeeds, data saved, photo deleted
  - [ ] Test: `testFailedManualRetry()` - Manual retry fails, persistent notification shown again
  - [ ] Test: `testPhotoRetentionAcrossRetryCycles()` - Verify same photo used across multiple retry cycles

- [ ] **Task 11: Documentation and Integration Notes** (AC: All)
  - [ ] Add KDoc comments to RetryBroadcastReceiver
  - [ ] Document notification action architecture in Dev Notes
  - [ ] Update architecture.md with manual retry flow diagram
  - [ ] Document notification ID constants and their usage

## Definition of Done

This story is considered COMPLETE only when ALL of the following are satisfied:

### Implementation & Quality
- [ ] All acceptance criteria are met with verified evidence (file:line references)
- [ ] All tasks and subtasks are completed and checked off
- [ ] Code follows project architecture patterns and conventions (MVVM, Repository pattern)
- [ ] All new code has appropriate error handling
- [ ] Persistent notification survives app termination and device reboot
- [ ] Code is reviewed (senior developer or AI code review workflow)

### Testing Requirements
- [ ] **Unit tests written** for notification builder, error mapping, photo retention logic (8+ tests)
- [ ] **All unit tests passing** (`./gradlew test` executes successfully with zero failures)
- [ ] **Instrumentation tests written** for retry flow, notification actions (5+ tests as listed in Task 10)
- [ ] **All instrumentation tests passing** (`./gradlew connectedAndroidTest` succeeds)
- [ ] No test coverage regressions (existing 280 tests still pass)

### Documentation
- [ ] Inline code documentation (KDocs) added for RetryBroadcastReceiver and notification methods
- [ ] Dev Notes section includes retry architecture and notification flow
- [ ] Architecture.md updated with manual retry flow
- [ ] AndroidManifest.xml updated with RetryBroadcastReceiver registration

### Story File Completeness
- [ ] Dev Agent Record updated with completion notes and file list
- [ ] Change Log entry added summarizing implementation
- [ ] Story status updated to "review" (pending approval) or "done" (if approved)

### Testing Standards Summary:
- **Unit Tests Required:** Notification builder, error category mapping, photo retention, PendingIntent configuration
- **Instrumentation Tests Required:** Notification action tap, WorkManager enqueue, retry cycle, notification replacement
- **Test Naming Convention:** `methodName_whenCondition_thenExpectedResult` or `feature should behavior when condition`
- **Assertion Library:** Truth library for readable assertions (`assertThat(x).isEqualTo(y)`)
- **Mocking:** Use MockK for mocking NotificationManager, WorkManager, dependencies

## User Demo

**Purpose**: Demonstrate manual retry button in persistent notification after automatic retry exhaustion.

### Prerequisites
- Android device or emulator running the app
- Foodie app installed with valid Azure OpenAI configuration
- Home screen widget added
- Ability to toggle airplane mode during demo

### Demo Steps
1. **Setup:** Ensure device is in airplane mode (offline)
2. **Capture Photo:** Tap home screen widget, capture food photo, confirm
3. **Observe Auto Retry:** Notification shows "Retrying analysis... (attempt 2/4)", (attempt 3/4), (attempt 4/4)
4. **Observe Retry Exhaustion:** After 4 failed attempts, persistent notification appears: "Meal analysis failed" with body "Network problem"
5. **Restore Network:** Turn off airplane mode (simulate fixing network issue)
6. **Tap Retry Button:** Tap "Retry" action button on notification
7. **Observe Manual Retry:** Notification changes to "Analyzing meal..." (foreground notification)
8. **Verify Success:** After retry succeeds, notification dismisses and data appears in meal list
9. **Verify Photo Deleted:** Photo should be deleted after successful manual retry

### Expected Behavior
- Persistent notification appears after 4 failed automatic retry attempts
- Notification body shows specific error category (Network problem, Service unavailable, etc.)
- "Retry" button triggers new analysis attempt
- Notification transitions from persistent to foreground during retry
- Successful manual retry completes normally (save to HC, delete photo)
- Failed manual retry shows persistent notification again

### Validation Checklist
- [ ] Persistent notification appears after automatic retry exhaustion
- [ ] Notification body shows correct error category
- [ ] "Retry" action button is visible and tappable
- [ ] Tapping retry starts new WorkManager task
- [ ] Persistent notification dismissed when retry starts
- [ ] Foreground notification shown during manual retry
- [ ] Successful manual retry saves data and deletes photo
- [ ] Failed manual retry shows persistent notification again
- [ ] No crashes or errors during retry flow

## Dev Notes

### Implementation Summary

**Objective:**
Implement persistent notification with manual retry button when automatic retries are exhausted. Users can tap "Retry" to reprocess the retained photo without recapturing.

**Key Components:**
1. **MealAnalysisForegroundNotifier**: Add `createPersistentErrorNotification()` method
2. **RetryBroadcastReceiver**: Handle notification action tap, enqueue WorkManager task
3. **AnalyzeMealWorker**: Update retry exhaustion logic to show persistent notification
4. **ErrorHandler/Mapper**: Map ErrorType to user-friendly category strings

**Notification Architecture:**
- Persistent notification ID: Different from foreground notification (avoid conflicts)
- Action button: "Retry" with PendingIntent to RetryBroadcastReceiver
- Notification body: Error category (Network problem, Service unavailable, etc.)
- Notification stays visible until dismissed or successful retry

**Retry Flow:**
```
1. Automatic retry exhausted (4 attempts)
   └─> Show persistent notification with retry button
   
2. User taps "Retry" button
   └─> RetryBroadcastReceiver.onReceive()
   └─> Cancel persistent notification
   └─> Enqueue AnalyzeMealWorker with same photo URI
   
3. AnalyzeMealWorker starts
   └─> Show foreground notification "Analyzing meal..."
   └─> Attempt 1, 2, 3, 4 with exponential backoff
   
4a. Success path
   └─> Save to Health Connect
   └─> Delete photo
   └─> Dismiss notification
   
4b. Failure path (4 attempts exhausted)
   └─> Show persistent notification again
   └─> Retain photo for next manual retry
```

**Photo Retention Policy Update:**
- Retryable errors: Retain photo after automatic retry exhaustion (for manual retry)
- Non-retryable errors: Delete photo after exhaustion (user can't fix by retrying)
- Manual retry: Use same photo from cache
- Photo persists across multiple manual retry cycles until success
- 24-hour cleanup still applies (PhotoCleanupWorker from future story)

**Error Category Mapping:**
| ErrorType | Notification Body |
|-----------|-------------------|
| NetworkError | "Network problem" |
| ServerError | "Service unavailable" |
| AuthError | "Authentication problem" |
| RateLimitError | "Too many requests" |
| ParseError | "Service problem" |
| ValidationError | "Request problem" |
| PermissionDenied | "Storage problem" |
| HealthConnectUnavailable | "Health Connect problem" |
| UnknownError | "Unknown error" |

### Project Structure Notes

**New Files:**
- `app/src/main/java/com/foodie/app/data/worker/RetryBroadcastReceiver.kt` - Handles notification retry action
- (Optional) `app/src/main/java/com/foodie/app/domain/error/NotificationMessageMapper.kt` - Maps ErrorType to notification text

**Modified Files:**
- `app/src/main/java/com/foodie/app/ui/notifications/MealAnalysisForegroundNotifier.kt` - Add createPersistentErrorNotification()
- `app/src/main/java/com/foodie/app/data/worker/AnalyzeMealWorker.kt` - Update retry exhaustion to show persistent notification
- `app/src/main/AndroidManifest.xml` - Register RetryBroadcastReceiver

**Dependencies from Previous Stories:**
- Story 4-1: ErrorHandler, ErrorType (for error classification)
- Story 4-2: AnalyzeMealWorker retry logic, NetworkMonitor (retry foundation)
- Story 2-8: MealAnalysisForegroundNotifier (notification infrastructure)

### Learnings from Previous Stories

**From Story 4-2 (API Retry Logic with Exponential Backoff) (Status: done)**

- **Retry Exhaustion Logic**: `runAttemptCount + 1 >= MAX_ATTEMPTS` at `AnalyzeMealWorker.kt:197,314` - modify to show persistent notification instead of immediate failure
- **Photo Retention**: Photos already retained during retries, just need to extend retention after exhaustion for retryable errors
- **ErrorHandler Integration**: Use `errorHandler.classify(exception)` and `errorHandler.isRetryable(errorType)` to determine if persistent notification should have retry button
- **Notification Updates**: Already using `setForeground(foregroundNotifier.createForegroundInfo())` - add persistent notification creation method

**From Story 2-8 (Foreground Analysis Service) (Status: done)**

- **Notification Infrastructure**: MealAnalysisForegroundNotifier at `app/src/main/java/com/foodie/app/ui/notifications/MealAnalysisForegroundNotifier.kt` - extend with persistent notification method
- **Notification Channel**: Channel already exists (ID: "meal_analysis_channel") - reuse for persistent notification
- **NotificationManager Access**: Already injected via Hilt - use same instance for persistent notifications

### References

- [Android Notification Actions](https://developer.android.com/develop/ui/views/notifications/build-notification#Actions) - Official docs for notification action buttons
- [WorkManager Enqueue](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work) - Enqueueing work from BroadcastReceiver
- [PendingIntent Mutability](https://developer.android.com/guide/components/intents-filters#CreateImmutablePendingIntents) - FLAG_IMMUTABLE requirements for Android 12+
- [Story 4-2 Implementation](4-2-api-retry-logic-with-exponential-backoff.md) - Automatic retry foundation
- [Story 4-1 Implementation](4-1-network-error-handling-infrastructure.md) - ErrorHandler and ErrorType patterns
- [Epic 4 Tech Spec](tech-spec-epic-4.md) - Overall retry and error handling strategy

## Dev Agent Record

### Context Reference

- [Story Context XML](4-7-persistent-notification-with-manual-retry.context.xml) - **TO BE CREATED**

### Agent Model Used

(To be populated during implementation)

### Debug Log References

(To be populated during implementation)

### Completion Notes List

(To be populated during implementation)

### File List

(To be populated during implementation)
