# foodie - Epic Breakdown

**Author:** BMad
**Date:** 2025-11-08
**Project Level:** Medium
**Target Scale:** Personal tool (single user MVP)

---

## Overview

This document provides the complete epic and story breakdown for foodie, decomposing the requirements from the [PRD](./PRD.md) into implementable stories.

### Epic Structure

The work is organized into 5 epics that follow a natural implementation sequence:

1. **Foundation & Infrastructure** - Establish technical foundation
2. **AI-Powered Meal Capture** - Core "invisible tracking" flow
3. **Data Management & Review** - Enable review and corrections
4. **Error Handling & Reliability** - Production-ready resilience
5. **Configuration & Polish** - User customization and finalization

Each epic is independently deliverable and adds incremental value. Dependencies flow forward only.

---

## Epic 1: Foundation & Infrastructure

**Goal:** Establish the technical foundation, project structure, and core architecture patterns that enable all subsequent feature development.

**Value:** Nothing else can be built without this base. Creates a stable foundation for rapid feature development while ensuring code quality and maintainability.

---

### Story 1.1: Project Setup and Build Configuration

As a developer,
I want a properly configured Android project with all required dependencies,
So that I can begin feature development with a stable build environment.

**Acceptance Criteria:**

**Given** a new Android project needs to be created
**When** the project is initialized with the required configuration
**Then** the build completes successfully with all dependencies resolved

**And** the project structure follows Android best practices (src/main/java, res/, manifests)

**And** build tools are configured: Gradle 8.13, Android Gradle Plugin 8.13.0, Kotlin 2.2.21, JDK 17

**And** minimum SDK is Android 9 (API 28), target and compile SDK is Android 15 (API 35)

**And** core dependencies are added: AndroidX libraries, Kotlin coroutines, Jetpack Compose (or XML layouts)

**And** Health Connect SDK dependency is included

**And** networking libraries for HTTP calls are configured (OkHttp or Retrofit)

**Prerequisites:** None (first story in project)

**Technical Notes:** Use Android Studio project wizard with Empty Activity template. Configure build.gradle files with proper version catalogs for dependency management. Enable Kotlin coroutines for async operations.

---

### Story 1.2: MVVM Architecture Foundation

As a developer,
I want a clear MVVM architecture with repository pattern,
So that the codebase is maintainable and follows Android best practices.

**Acceptance Criteria:**

**Given** feature development requires a consistent architectural pattern
**When** the base architecture classes are created
**Then** ViewModel base classes exist with lifecycle management

**And** Repository pattern interfaces are defined for data access

**And** Data source abstractions exist for external APIs and local storage

**And** Dependency injection framework is configured (Hilt or Koin)

**And** SampleViewModel + Repository + DataSource demonstrate the pattern

**Prerequisites:** Story 1.1 (project setup must be complete)

**Technical Notes:** Create base classes: BaseViewModel, BaseRepository. Use Hilt for dependency injection. Establish clear separation: View (UI) → ViewModel (business logic) → Repository (data coordination) → DataSource (API/DB access). Create a simple example showing the full stack to validate the pattern.

---

### Story 1.3: Core Navigation and Screen Structure

As a developer,
I want the main app navigation and screen structure defined,
So that features can be integrated into a cohesive user flow.

**Acceptance Criteria:**

**Given** the app requires multiple screens (list view, edit screen, settings)
**When** the navigation framework is implemented
**Then** navigation component is configured with navigation graph

**And** main activity hosts the navigation controller

**And** placeholder screens exist for: List View, Edit Screen, Settings Screen

**And** navigation between screens works with proper back stack handling

**And** deep linking is configured for widget intents

**Prerequisites:** Story 1.1 (project must exist), Story 1.2 (MVVM architecture defined)

**Technical Notes:** Use Jetpack Navigation component. Define navigation graph in XML. Create Fragment or Composable placeholders for each screen. Configure SafeArgs for type-safe navigation parameters. Set up deep link handling for widget → camera flow.

---

### Story 1.4: Health Connect Integration Setup

As a developer,
I want Health Connect SDK properly integrated with permissions handling,
So that the app can read and write nutrition data to the local device storage.

**Acceptance Criteria:**

**Given** the app needs to store nutrition data in Health Connect
**When** the Health Connect integration is configured
**Then** Health Connect SDK is initialized in the application class

**And** permission request flow is implemented for READ_NUTRITION and WRITE_NUTRITION

**And** Health Connect availability check is performed on app launch

**And** graceful handling exists if Health Connect is not installed (link to Play Store)

**And** repository class exists for Health Connect operations (placeholder methods)

**And** sample write + read operation demonstrates successful integration

**Prerequisites:** Story 1.1 (dependencies must be configured), Story 1.2 (repository pattern established)

**Technical Notes:** Initialize HealthConnectClient in Application class. Implement ActivityResultContract for permissions. Check Health Connect availability using SDK methods. Create HealthConnectRepository with methods: insertNutrition(), queryNutrition(), deleteNutrition(). Test with simple NutritionRecord write and verify in Google Fit or Health Connect app.

---

### Story 1.5: Logging and Error Handling Framework

As a developer,
I want consistent logging and error handling patterns,
So that debugging is efficient and errors are handled gracefully.

**Acceptance Criteria:**

**Given** the app will encounter errors (network failures, API errors, permission denials)
**When** the error handling framework is implemented
**Then** centralized logging utility exists with log levels (DEBUG, INFO, WARN, ERROR)

**And** error handling wrapper exists for try-catch blocks with logging

**And** user-facing error messages are defined (no technical jargon)

**And** crash reporting is configured (Firebase Crashlytics or similar, optional for MVP)

**And** all API calls and critical operations are wrapped with error handling

**Prerequisites:** Story 1.2 (architecture must be established)

**Technical Notes:** Create Logger utility class wrapping Android Log. Define sealed class for Result<Success, Error> pattern. Create error message mapping (technical error → user-friendly message). Set up Crashlytics if desired (optional for personal tool). Establish convention: all repository methods return Result<T>.

---

## Epic 2: AI-Powered Meal Capture

**Goal:** Deliver the core "invisible tracking" experience - enable users to snap a photo, have it analyzed by AI, and automatically save to Health Connect in under 5 seconds.

**Value:** This is the fundamental product innovation. Without this working end-to-end, the app has no value. Proves the central hypothesis that invisible tracking is faster than manual entry.

---

### Story 2.1: Lock Screen Widget Implementation

As a user,
I want to tap a lock screen widget to instantly launch the camera,
So that I can capture my meal in 2 seconds without unlocking my phone.

**Acceptance Criteria:**

**Given** I have the app installed and the widget added to my lock screen
**When** I tap the lock screen widget
**Then** the camera launches in less than 500ms

**And** no device unlock is required (camera accessible from lock screen)

**And** the widget displays the app icon with "Log Meal" text

**And** the widget uses the smallest available lock screen widget size

**And** the widget remains functional after device reboot

**Prerequisites:** Story 1.3 (navigation and deep linking must be configured)

**Technical Notes:** Use Android 12+ lock screen widget API with RemoteViews. Create AppWidgetProvider for lock screen. Use PendingIntent to launch camera activity directly via deep link. Test on devices with different lock screen security settings. Widget should be stateless (no updates needed).

---

### Story 2.2: Camera Integration with Photo Capture

As a user,
I want to quickly photograph my food with one hand and retake if blurry,
So that I get a clear image for AI analysis while holding my plate.

**Acceptance Criteria:**

**Given** the camera is launched from the widget
**When** I frame my food and tap the capture button
**Then** a photo is captured at maximum 2MP resolution

**And** the photo is saved as JPEG with 80% compression quality

**And** a preview screen shows the captured photo with "Retake" and "Use Photo" buttons

**And** the capture button is large enough for easy thumb access (minimum 48dp)

**And** volume buttons can be used as alternative capture method

**And** auto-rotate handles device orientation correctly

**And** tapping "Use Photo" saves to app cache directory and proceeds to background processing

**And** tapping "Retake" returns to camera without saving

**Prerequisites:** Story 1.3 (navigation configured), Story 2.1 (widget can launch camera)

**Technical Notes:** Evaluate stock camera intent vs CameraX library for performance. Implement full-screen camera view. Save photos to getCacheDir() for automatic cleanup. Apply compression before saving. Handle camera permissions (request on first launch). Test one-handed usability on actual device.

---

### Story 2.3: Azure OpenAI API Client

As a developer,
I want to send food photos to Azure OpenAI and receive structured calorie estimates,
So that users get automatic nutrition data without manual entry.

**Acceptance Criteria:**

**Given** a food photo exists in temporary storage
**When** the API client sends the photo for analysis
**Then** the photo is base64-encoded and included in the request payload

**And** the request uses Azure OpenAI Responses API endpoint format: `https://{resource}.openai.azure.com/openai/v1/responses`

**And** authentication uses `api-key` header (not Bearer token)

**And** the request uses the `model` field to specify the deployment name (e.g., "gpt-4.1")

**And** the request uses `instructions` field for system-level guidance: "You are a nutrition analysis assistant. Analyze the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food)."

**And** the request uses `input` array with multimodal content containing text prompt and base64 image

**And** the input includes user message with text: "Analyze this meal and estimate total calories." and image type "input_image" with base64 data URL

**And** the response contains `output_text` field with JSON: `{calories: number, description: string}`

**And** validation ensures calories > 0 and < 5000

**And** the API call completes in under 10 seconds for typical network conditions

**And** API key and endpoint are retrieved from secure storage (Android Keystore)

**Prerequisites:** Story 1.1 (networking libraries configured), Story 1.5 (error handling framework exists)

**Technical Notes:** Create AzureOpenAIClient class with analyze() method using Responses API. Use OkHttp or Retrofit for HTTP calls. Endpoint is `https://{resource}.openai.azure.com/openai/v1/responses` (no deployment in path, deployment specified in `model` field). Implement timeout of 15 seconds. Return Result<NutritionData, ApiError>. Test with sample food images. Both API key and base endpoint URL (resource name) must be configured by user in settings. The model/deployment name is specified in the request body `model` field.

---

### Story 2.4: Background Processing Service

As a user,
I want the AI analysis to happen in the background after I capture a photo,
So that I can immediately return to my previous activity without waiting.

**Acceptance Criteria:**

**Given** a photo has been captured and confirmed
**When** background processing begins
**Then** a foreground service starts with notification: "Analyzing meal..."

**And** the user can immediately return to lock screen or previous app

**And** the service calls Azure OpenAI API with the photo

**And** the notification displays the app icon and minimal text

**And** the notification auto-dismisses on successful completion

**And** the entire process (photo → API → save) completes in under 15 seconds typical

**And** the temporary photo is deleted after successful processing

**And** the service survives brief app termination

**Prerequisites:** Story 2.2 (photo capture working), Story 2.3 (API client implemented), Story 1.4 (Health Connect repository exists)

**Technical Notes:** Create ForegroundService for Android 8+ compatibility. Use WorkManager for reliable background execution. Service should: 1) Read photo from cache, 2) Call API, 3) Save to Health Connect, 4) Delete photo. Handle service lifecycle properly. Test battery impact and Android battery optimization restrictions.

---

### Story 2.5: Health Connect Nutrition Data Save

As a user,
I want my meal calories and description automatically saved to Health Connect,
So that my nutrition data is available to other health apps without manual entry.

**Acceptance Criteria:**

**Given** the Azure OpenAI API returns calorie estimate and food description
**When** the data is saved to Health Connect
**Then** a NutritionRecord is created with energy field (calories in kcal)

**And** the name field contains the food description from AI

**And** startTime and endTime are set to the photo capture timestamp

**And** the record is inserted using HealthConnectClient.insertRecords()

**And** the save operation completes successfully with confirmation

**And** the data is immediately visible in Google Fit or other Health Connect apps

**And** errors are handled gracefully (permission issues, Health Connect unavailable)

**Prerequisites:** Story 1.4 (Health Connect integration setup), Story 2.3 (API returns structured data), Story 2.4 (background service coordinates the flow)

**Technical Notes:** Implement HealthConnectRepository.insertNutrition(calories, description, timestamp) method. Create NutritionRecord with Energy.kilocalories() and proper time zones. Handle permission errors with user notification. Verify data appears in Health Connect by querying it back. No local database needed - Health Connect is single source of truth.

---

### Story 2.6: End-to-End Capture Flow Integration

As a user,
I want the complete flow from widget tap to saved data to work seamlessly,
So that I can track meals in under 5 seconds without any friction.

**Acceptance Criteria:**

**Given** all individual components are implemented
**When** I perform the complete capture flow
**Then** widget tap → camera launch happens in < 500ms

**And** photo capture + confirmation takes < 5 seconds total

**And** I can return to previous activity immediately after confirmation

**And** background processing completes in < 15 seconds

**And** the nutrition data appears in Health Connect automatically

**And** the temporary photo is deleted after successful save

**And** the entire flow feels seamless with no visible delays or interruptions

**And** haptic feedback occurs on photo capture (physical confirmation)

**And** visual checkmark briefly displays on successful capture

**Prerequisites:** All stories in Epic 2 (2.1 through 2.5)

**Technical Notes:** This is an integration story - no new code, but validates all pieces work together. Test the complete flow on real device with actual food photos. Measure timing at each step. Verify photo deletion. Check Health Connect for saved data. Test one-handed operation while holding a plate (actual use case). Document any performance issues discovered.

---

## Epic 3: Data Management & Review

**Goal:** Enable users to view, edit, and delete their nutrition entries, providing transparency and control over their tracking data.

**Value:** Trust with transparency - users can review AI estimates and make corrections during evening review without disrupting the meal capture flow. Makes the app practical for daily use.

---

### Story 3.1: Meal Entry List View

As a user,
I want to see a list of my recent meal entries with calories and descriptions,
So that I can review what I've tracked and verify accuracy.

**Acceptance Criteria:**

**Given** I have logged meals in Health Connect
**When** I open the Foodie app to the main screen
**Then** a list displays all nutrition entries from the last 7 days

**And** entries are sorted newest first (reverse chronological)

**And** each entry shows: timestamp (e.g., "12:30 PM"), food description, calorie count

**And** entries are grouped by date headers ("Today", "Yesterday", "Nov 7")

**And** the list loads in under 500ms

**And** pagination loads 50 entries at a time for smooth scrolling

**And** pull-to-refresh reloads data from Health Connect

**And** empty state displays: "No meals logged yet. Use the widget to capture your first meal!"

**And** tapping an entry navigates to the edit screen

**And** long-pressing an entry shows delete confirmation dialog

**Prerequisites:** Story 1.4 (Health Connect integration), Story 2.5 (data is being saved to Health Connect), Story 1.3 (navigation configured)

**Technical Notes:** Query Health Connect using TimeRangeFilter for last 7 days. Use RecyclerView with ViewHolder pattern for efficient scrolling. Implement ViewModel to cache query results. Format timestamps with relative dates. Use SwipeRefreshLayout for pull-to-refresh. Group entries by date using header items in adapter.

---

### Story 3.2: Edit Meal Entry Screen

As a user,
I want to edit the calories and description of a meal entry,
So that I can correct inaccurate AI estimates.

**Acceptance Criteria:**

**Given** I tap a meal entry from the list view
**When** the edit screen opens
**Then** the screen displays the current description in an editable text field

**And** the screen displays the current calories in an editable number field

**And** the screen displays the timestamp as read-only (e.g., "Captured: Nov 7, 2025 at 12:30 PM")

**And** a "Save" button is prominently displayed

**And** a "Cancel" button allows discarding changes

**And** calorie validation ensures value > 0 and < 5000

**And** description field has maximum length of 200 characters

**And** the edit screen opens in under 200ms

**Prerequisites:** Story 3.1 (list view can navigate to edit screen), Story 1.4 (Health Connect repository has update methods)

**Technical Notes:** Create EditMealFragment/Screen with two input fields. Use ViewModel to hold entry data and handle save logic. Implement form validation before save. Large touch targets for input fields (minimum 48dp). Show keyboard automatically when description field focused.

---

### Story 3.3: Update Health Connect Entry

As a user,
I want my edits to be saved back to Health Connect,
So that my corrected data is available to all health apps.

**Acceptance Criteria:**

**Given** I have edited a meal entry and tap "Save"
**When** the save operation executes
**Then** the old NutritionRecord is deleted from Health Connect

**And** a new NutritionRecord is inserted with updated calories and description

**And** the original timestamp (startTime/endTime) is preserved

**And** a toast message displays: "Entry updated"

**And** I am navigated back to the list view

**And** the list view shows the updated entry immediately

**And** the update is visible in other Health Connect apps (Google Fit)

**And** errors are handled gracefully (show error message if Health Connect operation fails)

**Prerequisites:** Story 3.2 (edit screen UI exists), Story 1.4 (Health Connect repository configured)

**Technical Notes:** Implement HealthConnectRepository.updateNutrition() method using delete + insert pattern. Preserve metadata (timestamps, zones). Use coroutines for async operation. Handle permission errors. Refresh list view data after successful update.

---

### Story 3.4: Delete Meal Entry

As a user,
I want to delete incorrect or duplicate entries,
So that my nutrition log stays accurate.

**Acceptance Criteria:**

**Given** I long-press a meal entry in the list view
**When** the delete confirmation dialog appears
**Then** the dialog displays: "Delete this entry? This cannot be undone."

**And** the dialog has "Cancel" and "Delete" buttons

**And** tapping "Cancel" dismisses the dialog with no changes

**And** tapping "Delete" removes the entry from Health Connect

**And** the entry disappears from the list view immediately

**And** a toast message displays: "Entry deleted"

**And** the deletion is permanent (no undo capability)

**And** the deletion is reflected in other Health Connect apps

**Prerequisites:** Story 3.1 (list view with long-press handler), Story 1.4 (Health Connect repository has delete method)

**Technical Notes:** Implement long-press listener on RecyclerView items. Show AlertDialog for confirmation. Use HealthConnectClient.deleteRecords() API. Remove item from adapter after successful deletion. Handle errors gracefully (show error toast if deletion fails).

---

### Story 3.5: Data Refresh and Sync

As a user,
I want the list view to always show current data from Health Connect,
So that I see accurate information including changes made by other apps.

**Acceptance Criteria:**

**Given** nutrition data may be modified by other Health Connect apps
**When** I open or refresh the Foodie app
**Then** the list view queries fresh data from Health Connect

**And** pull-to-refresh manually triggers a data reload

**And** the app automatically refreshes when returning to foreground

**And** loading indicator displays during refresh

**And** the refresh completes in under 1 second

**And** entries added/modified by other apps appear correctly

**And** no local caching causes stale data issues

**Prerequisites:** Story 3.1 (list view implementation), Story 1.4 (Health Connect queries working)

**Technical Notes:** Implement lifecycle-aware data loading (onResume triggers refresh). Use SwipeRefreshLayout for manual refresh. Query Health Connect on each screen load - no persistent local cache needed. Health Connect is single source of truth. Show shimmer or progress indicator during load. Handle empty results gracefully.

---

## Epic 4: Error Handling & Reliability

**Goal:** Ensure the app works reliably in real-world conditions including poor network connectivity, API failures, and edge cases.

**Value:** Production readiness - makes the core capture flow robust enough for daily use. Prevents data loss and frustrating user experiences.

---

### Story 4.1: Network Connectivity Detection

As a user,
I want the app to detect when I'm offline before attempting API calls,
So that I get immediate feedback instead of waiting for timeouts.

**Acceptance Criteria:**

**Given** network connectivity may be unavailable or unstable
**When** a photo capture is confirmed
**Then** the app checks network connectivity before starting background processing

**And** if offline, a notification displays: "No internet - meal saved, will analyze when online"

**And** the photo is retained in temporary storage for later processing

**And** the notification includes a manual retry button

**And** the app monitors network state changes

**And** when connectivity is restored, a retry attempt is initiated automatically

**And** the network check completes in under 100ms

**Prerequisites:** Story 2.4 (background processing service exists), Story 1.5 (error handling framework)

**Technical Notes:** Use ConnectivityManager to check network availability. Register NetworkCallback to monitor connectivity changes. Store pending photo paths in WorkManager for reliable retry. Implement notification with action button for manual retry. Test on device with airplane mode toggle.

---

### Story 4.2: API Retry Logic with Exponential Backoff

As a user,
I want the app to automatically retry failed API calls,
So that temporary network issues don't cause lost meal entries.

**Acceptance Criteria:**

**Given** an Azure OpenAI API call fails due to network or server error
**When** the retry logic activates
**Then** the app retries up to 3 additional times (4 total attempts)

**And** retry delays use exponential backoff: 0s (immediate), 1s, 2s, 4s

**And** the notification updates during retries: "Retrying analysis... (attempt 2/4)"

**And** each retry uses the same photo from temporary storage

**And** after successful retry, processing continues normally (save to Health Connect, delete photo)

**And** after all retries exhausted, a persistent notification displays: "Meal analysis failed. Tap to retry manually."

**And** the photo is retained for manual retry

**And** manual retry button re-initiates the API call

**Prerequisites:** Story 2.3 (API client exists), Story 2.4 (background service), Story 4.1 (network detection)

**Technical Notes:** Implement retry logic in WorkManager with backoff policy. Use WorkRequest.Builder.setBackoffCriteria() for exponential delays. Track retry count in work data. Update foreground notification with current attempt number. Differentiate between retryable errors (network, timeout, 5xx) and non-retryable errors (4xx, invalid response). Manual retry creates new WorkRequest.

---

### Story 4.3: API Error Classification and Handling

As a user,
I want clear, actionable error messages when something goes wrong,
So that I understand what happened and how to fix it.

**Acceptance Criteria:**

**Given** various types of API errors can occur
**When** an error is encountered
**Then** network timeouts show: "Request timed out. Check your internet connection."

**And** authentication errors (401/403) show: "API key invalid. Check settings."

**And** server errors (5xx) show: "Service temporarily unavailable. Will retry automatically."

**And** rate limit errors (429) show: "Too many requests. Please wait a moment."

**And** invalid response format shows: "Unexpected response from AI service."

**And** each error type triggers appropriate handling (retry for 5xx, don't retry for 4xx)

**And** errors are logged with full context for debugging

**And** critical errors show persistent notifications requiring user action

**Prerequisites:** Story 2.3 (API client), Story 1.5 (error handling framework), Story 4.2 (retry logic)

**Technical Notes:** Create sealed class ApiError with subtypes: NetworkError, AuthError, ServerError, RateLimitError, ParseError. Map HTTP status codes to error types. Define user-facing messages for each error type. Log technical details (status code, response body) but show friendly messages to user. Only retry NetworkError and ServerError (5xx).

---

### Story 4.4: Photo Retention and Cleanup

As a user,
I want the app to manage temporary photos safely,
So that successful entries are cleaned up but failed entries can be retried.

**Acceptance Criteria:**

**Given** photos are stored temporarily during processing
**When** processing completes successfully
**Then** the photo is deleted immediately after Health Connect save

**And** when all retry attempts are exhausted, the photo is deleted

**And** when manual retry succeeds, the photo is deleted

**And** photos are never retained longer than 24 hours (automatic cleanup)

**And** photo cleanup happens in background (doesn't block UI)

**And** storage space is minimized (only pending photos retained)

**And** app cache directory is used (system can clear if storage low)

**Prerequisites:** Story 2.2 (photos being saved to cache), Story 2.4 (background processing), Story 4.2 (retry logic)

**Technical Notes:** Save photos to getCacheDir() for automatic system cleanup. Implement WorkManager periodic task for 24-hour cleanup. Track photo paths in WorkManager data. Delete photos in finally block after processing. Test cleanup by checking cache directory before/after. Verify system cache clearing works correctly.

---

### Story 4.5: Health Connect Permission and Availability Handling

As a user,
I want clear guidance when Health Connect is unavailable or permissions are denied,
So that I can fix the issue and continue tracking.

**Acceptance Criteria:**

**Given** Health Connect may be unavailable or permissions may be revoked
**When** the app attempts to save nutrition data
**Then** if Health Connect is not installed, show: "Health Connect required. Install from Play Store?" with link

**And** if permissions are denied, show: "Nutrition permissions required. Grant access in Settings?" with link to settings

**And** if permissions are revoked mid-session, detect and re-request on next operation

**And** permission checks happen before API call (fail fast, don't waste API request)

**And** pending photos are retained if permission issues occur

**And** clear instructions guide user through permission grant flow

**And** app re-checks Health Connect availability on each foreground return

**Prerequisites:** Story 1.4 (Health Connect integration), Story 2.5 (saving to Health Connect), Story 4.2 (retry for permission errors)

**Technical Notes:** Check Health Connect availability using SDK.isAvailable(). Implement permission check before each write operation. Use Intent to launch Play Store for Health Connect install. Use Intent to launch app settings for permission grant. Show AlertDialog with clear instructions. Retry operation after user returns from settings. Test permission revocation scenarios.

---

### Story 4.6: Graceful Degradation and User Feedback

As a user,
I want the app to handle edge cases gracefully with helpful feedback,
So that I'm never left wondering what's happening.

**Acceptance Criteria:**

**Given** various edge cases and error conditions exist
**When** an edge case occurs
**Then** camera permission denied shows: "Camera access required for meal tracking" with settings link

**And** API key missing shows: "Configure Azure OpenAI key in Settings"

**And** invalid API response (non-JSON) shows error and offers manual entry option

**And** extremely low storage shows: "Storage full. Free up space to continue."

**And** all notifications are dismissible but persistent for errors requiring action

**And** foreground service notification handles Android 13+ permissions

**And** haptic feedback and visual cues confirm successful operations

**And** no silent failures occur (user always gets feedback)

**Prerequisites:** All previous Epic 4 stories, Story 1.5 (error handling framework)

**Technical Notes:** Implement comprehensive error handling in all critical paths. Use NotificationManager for persistent notifications with actions. Request POST_NOTIFICATIONS permission on Android 13+. Add haptic feedback using Vibrator service. Show SnackBar or Toast for transient feedback. Create fallback UI for all failure modes. Test all edge cases systematically.

---

## Epic 5: Configuration & Polish

**Goal:** Enable user customization through settings and finalize the user experience with polish features like dark mode and accessibility.

**Value:** Completes the MVP with essential configuration capabilities and UX refinements that make the app feel professional and production-ready.

---

### Story 5.1: Settings Screen Foundation

As a user,
I want a settings screen where I can configure app preferences,
So that I can customize the app to my needs.

**Acceptance Criteria:**

**Given** the app requires user configuration
**When** I navigate to the settings screen
**Then** a settings screen is accessible from the main app menu or toolbar

**And** the settings screen uses Android PreferenceScreen framework

**And** the screen displays organized sections for different setting categories

**And** all settings persist across app restarts using SharedPreferences

**And** the screen follows Material Design guidelines

**And** navigation back to main screen works properly

**Prerequisites:** Story 1.3 (navigation configured)

**Technical Notes:** Create SettingsFragment using PreferenceFragmentCompat. Define preferences in XML (res/xml/preferences.xml). Use PreferenceManager for data persistence. Add menu item to main screen toolbar for settings access. Implement proper preference categories and summaries.

---

### Story 5.2: Azure OpenAI API Key and Endpoint Configuration

As a user,
I want to securely enter and store my Azure OpenAI API key and endpoint,
So that the app can access the AI service for nutrition analysis.

**Acceptance Criteria:**

**Given** the app requires an API key and endpoint to function
**When** I open the settings screen
**Then** an API key field is available (EditTextPreference)

**And** an endpoint field is available for the Azure resource URL

**And** a model/deployment name field is available (e.g., "gpt-4.1")

**And** the API key field is masked for security (shows dots instead of characters)

**And** the endpoint field accepts base endpoint URL (e.g., "https://my-resource.openai.azure.com")

**And** the API key is stored encrypted in Android Keystore

**And** the endpoint and model name are stored in SharedPreferences

**And** a "Test Connection" button validates the API key, endpoint, and model configuration

**And** test connection sends a simple request to the Responses API endpoint

**And** validation result displays: "API configuration valid ✓" or "Invalid configuration - check your credentials, endpoint, and model name"

**And** clear instructions explain: "Enter your Azure OpenAI API key, endpoint URL, and model deployment name. Get these at portal.azure.com"

**And** the API key field shows masked preview if already configured

**And** the endpoint and model fields show their values if already configured

**And** all values are retrievable by the API client when needed

**Prerequisites:** Story 5.1 (settings screen exists), Story 2.3 (API client can use the configuration)

**Technical Notes:** Use Android Keystore for API key encryption. Create KeyStoreManager utility class. Store endpoint URL and model name in SharedPreferences (not sensitive). Implement test connection that calls Responses API with minimal request using configured values. Show ProgressDialog during validation. Validate endpoint format before saving (must be valid URL). Never log the API key. The Responses API uses simpler v1 endpoint structure.

---

### Story 5.3: Model Selection and Configuration

As a user,
I want to select which Azure OpenAI model to use for analysis,
So that I can optimize for speed, cost, or accuracy based on my needs.

**Acceptance Criteria:**

**Given** Azure OpenAI supports vision-capable models
**When** I configure the model in settings
**Then** a model selection field exists with the option: "gpt-4.1"

**And** I can also enter a custom model/deployment name

**And** the selected model is used in the `model` field of Responses API requests

**And** helpful descriptions explain: "gpt-4.1: Advanced reasoning and vision capabilities"

**And** the test connection validates the selected model is available in the configured deployment

**And** model selection is stored in SharedPreferences

**And** the model selection defaults to "gpt-4.1" for new installations

**Prerequisites:** Story 5.1 (settings screen), Story 5.2 (API configuration)

**Technical Notes:** Use ListPreference or custom preference for model selection. The Responses API accepts the model name directly in the request body `model` field - no need to construct deployment paths. Store model selection in SharedPreferences. Provide sensible defaults but allow customization for users with specific deployments.

---

### Story 5.4: Dark Mode Support

As a user,
I want the app to support dark mode,
So that I can use it comfortably in low-light conditions (late-night meal logging).

**Acceptance Criteria:**

**Given** users may prefer dark mode for comfort
**When** dark mode is enabled (system-wide or in-app)
**Then** all screens use dark theme colors

**And** the theme follows Material Design dark theme guidelines

**And** text remains legible with proper contrast ratios

**And** camera preview maintains natural colors (not inverted)

**And** notifications use dark styling when appropriate

**And** the app respects system dark mode setting

**And** optional in-app theme selector: "System Default", "Light", "Dark"

**And** theme preference persists across app restarts

**Prerequisites:** Story 1.1 (project setup), Story 5.1 (settings for theme preference)

**Technical Notes:** Define dark theme in res/values-night/themes.xml. Use Material Design color palette for dark mode. Test all screens in both themes. Use AppCompatDelegate.setDefaultNightMode() for theme switching. Add theme preference to settings. Ensure camera maintains proper exposure in dark mode.

---

### Story 5.5: Accessibility Improvements

As a user with accessibility needs,
I want the app to work with screen readers and support large text,
So that I can use the app regardless of visual limitations.

**Acceptance Criteria:**

**Given** users may have accessibility requirements
**When** accessibility features are enabled
**Then** all buttons and images have content descriptions

**And** the app supports TalkBack screen reader

**And** touch targets are minimum 48dp (WCAG compliance)

**And** text scales properly with system font size settings

**And** color contrast ratios meet WCAG AA standards (4.5:1 for normal text)

**And** important information is not conveyed by color alone

**And** focus order is logical for keyboard/D-pad navigation

**And** the camera capture button is easily discoverable with TalkBack

**Prerequisites:** All UI screens implemented (Epics 2 and 3)

**Technical Notes:** Add contentDescription to all ImageView and ImageButton elements. Test with TalkBack enabled. Use sp units for text sizes (respect system font scaling). Run Accessibility Scanner tool. Verify touch target sizes using layout bounds. Test with large font sizes (Settings → Display → Font Size).

---

### Story 5.6: Performance Optimization and Polish

As a user,
I want the app to feel fast and polished,
So that the experience meets the "invisible tracking" promise.

**Acceptance Criteria:**

**Given** the app is functionally complete
**When** performance optimization is applied
**Then** all screen transitions are smooth (60fps)

**And** list view scrolling has no jank or stuttering

**And** image loading uses efficient caching and compression

**And** memory usage stays within reasonable limits (< 100MB typical)

**And** cold app launch takes < 2 seconds

**And** battery impact is minimal (efficient background processing)

**And** APK size is optimized (< 10MB for MVP)

**And** no memory leaks in ViewModels or Activities

**And** smooth animations enhance the capture flow (fade-ins, slide transitions)

**Prerequisites:** All functional stories complete (Epics 1-4)

**Technical Notes:** Use Android Profiler to identify bottlenecks. Implement proper image caching (Glide or Coil). Use RecyclerView.setHasFixedSize() for performance. Enable ProGuard/R8 for code shrinking. Remove unused resources. Test on mid-range device (not just flagship). Add subtle animations for state transitions. Verify no leaked contexts using LeakCanary.

---

### Story 5.7: User Onboarding (First Launch)

As a new user,
I want clear guidance on first launch,
So that I understand how to set up and use the app.

**Acceptance Criteria:**

**Given** the app is launched for the first time
**When** the app detects first launch
**Then** a brief welcome message displays explaining the core concept

**And** the user is prompted to add the lock screen widget

**And** widget addition instructions are clear (Settings → Lock screen → Add widget)

**And** Health Connect permissions are requested with clear rationale

**And** Azure OpenAI configuration prompt directs to settings (API key, endpoint, deployment)

**And** the onboarding flow takes < 2 minutes to complete

**And** the user can skip onboarding if desired

**And** onboarding only shows once (first launch detection via SharedPreferences)

**And** after onboarding, user is on the main screen ready to use the app

**Prerequisites:** Story 5.1 (settings configured), Story 1.4 (Health Connect permissions), Story 2.1 (widget implemented)

**Technical Notes:** Create simple onboarding flow (3-4 screens max). Use ViewPager2 for slide-through onboarding. Detect first launch using SharedPreferences flag. Show visual guides for widget setup. Request Health Connect permissions during onboarding. Provide "Skip" option on each screen. Keep messaging minimal - focus on getting user to first capture quickly.

---

### Story 5.8: Final Integration Testing and Bug Fixes

As a developer,
I want to perform comprehensive integration testing,
So that the MVP is stable and ready for daily use.

**Acceptance Criteria:**

**Given** all features are implemented
**When** integration testing is performed
**Then** complete end-to-end flow works on physical device

**And** all user stories have been manually tested and verified

**And** edge cases are tested: poor network, permission denials, low storage

**And** performance targets are met: < 5s capture, < 500ms list load

**And** no critical bugs exist that prevent core functionality

**And** known issues are documented with workarounds

**And** the app is tested on at least 2 different Android devices

**And** different Android versions are tested (minimum API 28, target API 35)

**And** battery usage is acceptable for daily tracking (3-5 captures per day)

**Prerequisites:** All stories in Epics 1-5 complete

**Technical Notes:** Create test checklist covering all user stories. Test on real devices (not just emulator). Use Android Debug Bridge (adb) for detailed logging. Monitor battery usage with Battery Historian. Test with actual food photos in realistic scenarios (holding plate, walking, poor lighting). Document any performance issues or bugs in issue tracker. Fix critical bugs before MVP release.

---

## Epic Breakdown Summary

### Total Story Count: 33 Stories

**Epic 1: Foundation & Infrastructure** - 5 stories
- Project setup, architecture, navigation, Health Connect, error handling

**Epic 2: AI-Powered Meal Capture** - 6 stories  
- Widget, camera, API integration, background processing, Health Connect save, integration

**Epic 3: Data Management & Review** - 5 stories
- List view, edit screen, update logic, delete, refresh/sync

**Epic 4: Error Handling & Reliability** - 6 stories
- Network detection, retry logic, error classification, photo cleanup, permissions, graceful degradation

**Epic 5: Configuration & Polish** - 8 stories
- Settings foundation, API configuration, dark mode, accessibility, performance, onboarding, final testing

### Implementation Sequence

All epics follow forward dependencies only:
1. Epic 1 establishes foundation
2. Epic 2 delivers core value (capture flow)
3. Epic 3 enables review and corrections
4. Epic 4 ensures production reliability
5. Epic 5 completes the experience

Each epic is independently deliverable and adds incremental value. Stories within epics should be completed sequentially, though some parallelization is possible within epics (e.g., Epic 2 stories 2.1, 2.2, 2.3 can be developed in parallel before integration).

---

_For implementation: Use the architecture and implementation workflows to generate detailed technical specifications and code for each story._


