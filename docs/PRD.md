# Foodie - Product Requirements Document

**Author:** BMad
**Date:** 2025-11-07
**Version:** 1.0

---

## Executive Summary

**Vision:** Make calorie tracking invisible - capture food in 2 seconds with one hand while holding your plate, eliminating the speed and accuracy bottlenecks that make manual tracking unsustainable for body recomposition goals.

Foodie is a native Android app that transforms calorie tracking from a 30+ second manual entry process into a 5-second camera snap. Users activate a home screen widget, unlock with biometrics, photograph their meal one-handed, pocket their phone, and AI analysis completes in the background with automatic save to Google Health Connect. Photos are ephemeral - deleted immediately after extracting caloric data.

Built for personal use to enable sustainable body recomposition through precision tracking without disrupting daily life or requiring cognitive overhead during meals.

### What Makes This Special

**"Invisible tracking that makes precision nutrition sustainable."**

The magic moment: Standing in a canteen line holding your plate, you tap a widget, unlock with your thumb, snap with one hand in 2-3 seconds, pocket your phone, and the calories are automatically logged by the time you sit down - no typing, no menus, no friction.

This product eliminates the fundamental speed-accuracy tradeoff that kills all calorie tracking adherence. Manual entry is too slow and inaccurate for body recomposition goals. Foodie makes tracking disappear into the background of daily life.

---

## Project Classification

**Technical Type:** Mobile App (Native Android)
**Domain:** General Consumer Software
**Complexity:** Medium (API integration, Health Connect, background processing)
**Field Type:** Greenfield
**Development Approach:** AI-assisted (100% LLM-generated code under senior engineer direction)

**Project Context:**
- Primary goal: Personal learning (spec-driven development, Android, Health Connect, Azure OpenAI)
- Secondary goal: Solve personal body recomposition tracking need
- Built for single user initially, architecture allows future expansion
- No commercial plans for MVP, "never say never" for future

---

## Success Criteria

### Primary Success Criterion

**Speed Validation: Average capture time ≤ 5 seconds from widget tap to photo saved**

This is the core hypothesis - can invisible tracking actually work in real life? The app must be fast enough to use while holding a plate in a canteen line (one-handed operation, ~2 seconds for snap).

**Measurement:** Time from home screen widget activation → photo captured → return to previous activity

**Success Threshold:** ≤ 5 seconds average across typical usage scenarios

### Secondary Success Criteria

**Sustained Usage: Still tracking on workdays after 30 days**
- Proves the tool doesn't create tracking fatigue
- Target: 80%+ workdays logged over 30-day period
- Workday-focused metric (weekends/vacations deliberately excluded)

**AI Accuracy: Trust-based validation**
- Azure OpenAI estimates validated as "pretty happy with results" during informal testing
- Manual correction capability built into evening review workflow
- Action threshold: Investigate only if estimates become "crazy off" during real usage
- Edit frequency not formally tracked - corrections expected and acceptable

### Metrics Explicitly Excluded

**Body Composition Progress:** Not measured within app
- User tracks separately with smart scale
- Future opportunity: Could integrate scale data for energy balance insights (V2.0+)

**AI Edit Rate:** Not formally measured
- Trust-based approach with manual override capability
- Investigate only if patterns emerge during usage

---

## Product Scope

### MVP - V1.0 (Minimum Viable Product)

**Core Capture Flow:**
- Home screen widget for quick camera access (requires device unlock with biometric for fast access)
- Single photo capture per meal entry
- Azure OpenAI GPT-4.1 API integration with structured JSON output: `{calories: number, description: string}`
- Background processing: snap → pocket phone → AI analyses → auto-save
- Automatic save to Google Health Connect when analysis completes
- Ephemeral photo storage: delete immediately after extracting caloric data

**Data Management:**
- List view showing recent entries (date, time, description, calories)
- Edit capability: tap entry → modify calories and/or description → save to Health Connect
- Delete capability: remove entries from Google Health Connect
- Full CRUD operations for nutrition data

**Technical Foundation:**
- Client-only architecture (no backend server, no database, no user accounts)
- Direct API flow: Android app → Azure OpenAI → Health Connect (local device storage)
- Native Android development (Kotlin)
- Azure OpenAI GPT-4.1 (multimodal vision model)
- Google Health Connect API for local data persistence

### Growth Features (V2.0 - Post-MVP)

**Energy Balance & Caloric Deficit Tracking:**
- User profile settings (sex, age, weight, height with Health Connect pre-population)
- Basal Metabolic Rate (BMR) calculation using Mifflin-St Jeor equation
  - Males: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
  - Females: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161
- Passive energy expenditure (NEAT) from step data using 0.04 kcal/step formula
- Active energy expenditure from Health Connect ActiveEnergyBurned
- Total Daily Energy Expenditure (TDEE) = BMR + NEAT + Active
- Calories In vs Calories Out dashboard with deficit/surplus visualization
- Scientific formulas: fully transparent and controllable calculations

**Enhanced Nutrition Tracking:**
- Macros tracking (protein, carbs, fat) in addition to calories
- Barcode scanning + OpenFoodFacts integration for packaged foods
- Offline capability: photo queuing when network unavailable with automatic retry
- Smart auto-categorization (breakfast/lunch/dinner/snacks based on time)
- Daily summary notifications

**Model & Prompt Optimization:**
- Test bench for model comparison (GPT-4.1 vs alternatives)
- Prompt variation testing framework
- Accuracy evaluation against known-calorie photo dataset
- Cost/accuracy reporting to optimize API usage

**Additional Enhancements:**
- Smart scale integration for body composition trend analysis
- Custom analytics and trend visualization

### Vision Features (V3.0+ - Future)

**Advanced Capture:**
- Before/after photos for portion size adjustment
- Family meal portions: "I ate 1/4 of this casserole"
- Voice annotations for context or corrections

**Intelligence:**
- Recipe recognition from home-cooked meals
- Allergy warnings based on user profile
- Food safety/freshness detection
- Video/burst mode for 3D food volume estimation

---

## Mobile App Specific Requirements

### Platform Support

**Target Platform:** Native Android
- **Minimum SDK:** Android 9 (API 28) - Required for Health Connect compatibility
- **Target SDK:** Android 15 (API 35) - Latest stable release
- **Compile SDK:** Android 15 (API 35)
- **Development Language:** Kotlin 2.2.21
- **Architecture Pattern:** MVVM (Model-View-ViewModel) with Repository pattern
- **Build Tools:**
  - Android Gradle Plugin: 8.13.0
  - Gradle: 8.13
  - JDK: 17
  - SDK Build Tools: 35.0.0

**Platform Rationale:**
- Google Health Connect is Android-exclusive API
- Native development ensures best widget performance and camera integration
- No cross-platform complexity needed for personal tool
- Full access to Android background processing APIs

**Excluded Platforms:**
- iOS: Not supported (Health Connect unavailable, personal tool doesn't need cross-platform)
- Web: Not applicable (requires native camera and widget APIs)
- Desktop: Not applicable (mobile-first use case)

### Device Features & Permissions

**Required Device Capabilities:**
- Camera (rear camera preferred for food photography)
- Internet connectivity (for Azure OpenAI API calls)
- Storage (minimal - ephemeral photos deleted after processing)

**Required Permissions:**
- `CAMERA` - Capture food photos
- `INTERNET` - Azure OpenAI API communication
- `READ_MEDIA_IMAGES` / `WRITE_EXTERNAL_STORAGE` - Temporary photo storage
- Health Connect permissions:
  - `READ_NUTRITION` - View existing nutrition entries
  - `WRITE_NUTRITION` - Save calorie data to Health Connect
  - `READ_WEIGHT` - Read user weight for BMR calculation (V2.0+)
  - `WRITE_WEIGHT` - Save manually entered weight to Health Connect (V2.0+)
  - `READ_HEIGHT` - Read user height for BMR calculation (V2.0+)
  - `WRITE_HEIGHT` - Save manually entered height to Health Connect (V2.0+)
  - `READ_STEPS` - Read daily step count for NEAT calculation (V2.0+)
  - `READ_ACTIVE_CALORIES_BURNED` - Read active energy expenditure (V2.0+)

**Permission Handling:**
- Request camera permission on first widget activation
- Request Health Connect permissions on first app launch
- Clear permission rationale: "Foodie needs camera access to analyse your meals and Health Connect access to save your nutrition data locally on your device."
- Graceful degradation: App unusable without required permissions (core functionality depends on them)

### Home Screen Widget Specification

**Widget Type:** Home Screen Quick Action Widget (Button-style)
- Single tap action: Launch camera directly from home screen
- Requires device unlock (biometric recommended for speed)
- Minimal UI: App icon + "Log Meal" text
- Size: Standard small widget (2x1 or 2x2 grid cells)

**Widget Behaviour:**
- Tap → Immediate camera launch (after device unlock if locked)
- Photo capture → Return to home screen (or previous app)
- Background processing begins automatically
- No widget state changes (remains static for speed)

**Widget Performance Requirements:**
- Launch latency: < 3 seconds from device wake to camera ready (with biometric unlock)
- One-handed operation: Widget placement supports thumb access on home screen
- No network dependency for launch (offline camera access, queue photo for later processing)

**Platform Note:**
Android does not support third-party lock screen widgets on phones. Lock screen shortcuts are limited to system apps only (camera, torch, wallet, etc.). Home screen widget with biometric unlock provides the fastest third-party app access experience (typically 2-3 seconds total).

**Implementation Notes:**
- Android 12+ widget API using Jetpack Glance
- Use Glance composables for widget UI (modern Compose-like API)
- `PendingIntent` to launch camera activity directly via deep link
- Consider `CameraX` library for consistent camera behaviour
- Widget updates not needed (static "Log Meal" button)
- Widget configured for home screen placement only

### Camera & Photo Capture

**Camera Interface:**
- Use built-in Android camera intent (system camera app)
- Leverage stock camera UI if it meets speed and usability requirements
- Full-screen camera view (maximize food visibility)
- Auto-focus on centre (food typically centreed in frame)
- Flash: User-controlled via system camera (auto-flash often inaccurate for food)

**Photo Specifications:**
- Resolution: Max 2MP (sufficient for AI analysis, minimizes upload time)
- Format: JPEG (universal compatibility, good compression)
- Orientation: Auto-rotate based on device sensors
- Quality: 80% compression (balance between file size and AI accuracy)

**Capture Flow:**
- Widget tap → Camera opens full-screen
- User frames food → Single tap to capture
- Preview screen with retake option (important for blurry/shaken photos from one-handed use)
- User confirms or retakes
- Return to previous screen after confirmation

**Rationale for Retake:**
Walking with plate while taking single-handed photos often results in blurry/shaken images. Quick retake capability ensures usable photos without requiring a second widget activation.

**One-Handed Optimization:**
- Large capture button (easy thumb reach)
- Volume button alternative for capture (accessibility)
- Minimal confirmation steps (just retake vs. accept)

### Background Processing Architecture

**Processing Flow:**
1. Photo captured → Saved to temporary local storage
2. Camera closes → User returns to previous activity
3. Background service initiates Azure OpenAI API call
4. API response parsed → Nutrition data extracted
5. Data saved to Google Health Connect
6. Temporary photo deleted immediately
7. Silent completion (no notification unless error)

**Background Service Requirements:**
- Foreground Service with notification (Android 8+ requirement for background work)
- Notification: "Analyzing meal..." with app icon (minimal intrusion)
- Auto-dismiss notification on completion
- Network failure handling: Retry up to 3 times with exponential backoff
- Battery optimization: Use WorkManager for deferred processing if immediate retry fails

**Error Handling:**
- Network unavailable: Show persistent notification "Meal photo saved, will analyse when online"
- API failure: Retry up to 3 times with exponential backoff
- After 3 failed retries: Show error notification with manual retry option
- Health Connect unavailable: Log error, prompt user to enable Health Connect
- Photo retention: Keep photo stored until successful API response OR all 3 retries exhausted
- Photo deletion: Delete only after successful Health Connect save OR after retry limit reached

**Performance Targets:**
- API call completion: < 10 seconds typical (depends on network)
- Photo deletion: After successful Health Connect save
- Total background time: < 15 seconds from capture to Health Connect save (excluding retries)

### Azure OpenAI Integration

**API Configuration:**
- **Endpoint:** Azure OpenAI Responses API with vision-capable model (GPT-4.1)
- **Authentication:** API key stored in app (encrypted with Android Keystore)
- **Request Format:** Responses API with multimodal input (text + base64-encoded image)
- **API Version:** v1 (Responses API)
- **Endpoint Structure:** `https://{resource-name}.openai.azure.com/openai/v1/responses`

**IMPORTANT:** This uses **Azure OpenAI Responses API**, the modern stateful API that replaces chat completions. Authentication uses `api-key` header (not `Authorization: Bearer`).

**Structured Output Request:**
```http
POST https://{your-resource-name}.openai.azure.com/openai/v1/responses
api-key: {your-api-key}
Content-Type: application/json

{
  "model": "gpt-4.1",
  "instructions": "You are a nutrition analysis assistant. Analyse the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food).",
  "input": [
    {
      "role": "user",
      "content": [
        {
          "type": "input_text",
          "text": "Analyse this meal and estimate total calories."
        },
        {
          "type": "input_image",
          "image_url": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
        }
      ]
    }
  ]
}
```

**Expected Response Format:**
```json
{
  "id": "resp_67cb61fa3a448190bcf2c42d96f0d1a8",
  "created_at": 1741408624.0,
  "model": "gpt-4o-2024-08-06",
  "object": "response",
  "status": "completed",
  "output_text": "{\"calories\": 650, \"description\": \"Grilled chicken breast with steamed broccoli and brown rice\"}",
  "usage": {
    "input_tokens": 1245,
    "output_tokens": 25,
    "total_tokens": 1270
  }
}
```

**Response Parsing:**
- Extract `output_text` field from response (for simple text responses)
- Parse output_text string as JSON: `{calories: number, description: string}`
- Extract `calories` as integer
- Extract `description` as string (max 200 characters, truncate if longer)
- Validation: Calories must be > 0 and < 5000 (sanity check)
- Fallback: If parsing fails, show error and prompt manual entry

**API Key Management:**
- Store API key in Android Keystore (encrypted hardware-backed storage)
- Never log API key in crash reports or analytics
- Allow user to update API key in settings (for future multi-user or key rotation)

**Cost Optimization:**
- Image resolution capped at 2MP (reduces token usage)
- No retry on successful response (even if inaccurate - user can edit)
- Single API call per photo (no multi-model comparison)

### Google Health Connect Integration

**Data Model:**
- **Record Type:** `NutritionRecord`
- **Fields Used:**
  - `energy`: Calories from AI analysis (in Energy units, kcal)
  - `name`: Food description from AI (optional String field)
  - `startTime`: Meal timestamp (Instant)
  - `endTime`: Calculated based on calories (<300kcal: 5min, <800kcal: 10min, >=800kcal: 15min)
  - `mealType`: Optional categorization (breakfast/lunch/dinner/snack) - deferred to V2.0

**Single Source of Truth:**
Health Connect stores ALL data - no local database needed. The `NutritionRecord.name` field supports food descriptions, eliminating the need for a separate SQLite database. This simplifies the architecture to a pure Health Connect implementation.

**CRUD Operations:**

**Create (Write):**
- Save `NutritionRecord` with `energy` and `name` fields after successful API response
- Use `HealthConnectClient.insertRecords()` API
- Handle permissions errors (prompt user to grant access)
- Example:
  ```kotlin
  NutritionRecord(
      energy = Energy.kilocalories(650.0),
      name = "Grilled chicken with rice",
      startTime = timestamp,
      endTime = timestamp.plus(10, ChronoUnit.MINUTES), // 650kcal -> 10min
      startZoneOffset = ZoneOffset.systemDefault(),
      endZoneOffset = ZoneOffset.systemDefault()
  )
  ```

**Read (List View):**
- Query `NutritionRecord` directly from Health Connect
- Use `TimeRangeFilter` for last 7 days
- Sort by `startTime` descending (newest first)
- Pagination: Load 50 entries at a time
- Access both `energy` and `name` fields for display

**Update (Edit):**
- Health Connect doesn't support direct updates
- Delete old record and insert new record with same timestamp
- Preserve original `startTime` and `endTime`
- Update both `energy` and `name` fields as needed

**Delete:**
- Use `HealthConnectClient.deleteRecords()` API
- Require confirmation dialog: "Delete this entry? This cannot be undone."
- Single operation (no dual-store sync needed)

**Health Connect Permissions Flow:**
- Request on first app launch with clear explanation
- Link to Health Connect settings if denied
- Periodic permission check (permissions can be revoked by user)

### Offline Capability (Deferred to V2.0)

**V1.0 Behaviour:**
- Requires network connectivity for AI analysis
- Show error notification if offline: "No internet - meal not logged. Retry when online?"
- Manual retry button in notification
- Photo retained for up to 3 retry attempts
- Photo deleted after successful processing OR after 3 failed retry attempts

**V2.0 Enhancement Plan:**
- Queue photos locally in encrypted storage
- Automatic retry when network detected
- Support multiple queued photos (process in order)
- Silent background processing (no user intervention)

---

## User Experience Principles

### Design Philosophy

**Invisible by Default:**
The UI should get out of the way. The best interface is the one you don't notice because the app just works. No onboarding screens, no tutorials, no "getting started" guides - just a widget that does exactly what you expect.

**Speed Over Features:**
Every feature must justify its presence by not slowing down the core 2-second capture flow. If a feature adds friction, it's not worth including. Manual entry is the enemy we're defeating.

**Trust with Transparency:**
Users trust the AI estimates but have full visibility and control. Evening review mode makes corrections easy without interrupting the meal capture flow. Transparency builds trust over time.

**Mobile-First Interaction Patterns:**
- Large touch targets (minimum 48dp for accessibility)
- Thumb-optimized layouts (important actions within easy reach)
- Minimal text input (cameras and taps, not keyboards)
- Dark mode support (late-night meal logging)

### Key User Flows

**Primary Flow: Meal Capture**
1. User at meal → Holds plate in one hand, phone in pocket
2. Pull out phone → Wake device → Unlock with biometric (fingerprint/face)
3. Tap home screen widget with thumb
4. Camera opens full-screen
5. Frame food → Single tap to capture
6. Preview → Confirm (or retake if blurry)
7. Return to home screen or previous activity
8. (Background) AI analysis → Auto-save to Health Connect
9. (Optional) Review in evening, edit if needed

**Time Budget:** < 5 seconds for steps 2-7

**Secondary Flow: Evening Review**
1. Open Foodie app
2. Scroll list of today's entries
3. Tap entry that looks wrong
4. Modify calories or description
5. Save → Updated in Health Connect
6. Return to list

**Time Budget:** < 10 seconds per edit

**Tertiary Flow: Delete Entry**
1. Long-press entry in list
2. Confirmation dialog appears
3. Tap "Delete"
4. Entry removed from list and Health Connect

**Time Budget:** < 5 seconds

### Visual Design Principles

**Minimalist Interface:**
- Clean white/dark backgrounds
- High contrast for outdoor visibility (bright sunlight use case)
- Monochrome colour scheme (focus on data, not decoration)
- System fonts (San Francisco UI on Android)

**Data Presentation:**
- Calorie count: Large, bold, immediately scannable
- Description: Secondary text, smaller font
- Timestamp: Tertiary info, gray text
- No graphs or charts in V1.0 (use Google Fit for analytics)

**Interaction Feedback:**
- Haptic feedback on photo capture (physical confirmation)
- Visual checkmark on successful capture
- Toast message for Health Connect save errors only
- Silent success (no interruption for normal operation)

---

## Functional Requirements

### FR-1: Home Screen Widget

**Requirement:** Provide quick camera access via home screen widget

**User Story:** As a user holding my plate with phone in pocket, I want to tap a home screen widget and quickly launch the camera after unlocking my phone so I can capture my meal in under 5 seconds.

**Acceptance Criteria:**
- Widget displays on Android home screen with app icon and "Log Meal" label
- Widget size: Standard small widget (2x1 or 2x2 grid cells)
- Single tap launches camera directly after device unlock (no intermediate app screen)
- Widget launch time < 3 seconds from device wake to camera ready (with biometric unlock)
- Widget remains functional after device reboot
- Widget works without app being actively running in background
- Biometric unlock recommended for optimal speed

**Platform Note:**
Android does not support third-party lock screen widgets on phones. Lock screen shortcuts are limited to system apps only. Home screen widget with biometric unlock provides the fastest third-party app access experience.

**Technical Notes:**
- Implement using Android 12+ widget API with Jetpack Glance
- Use `PendingIntent` to trigger camera activity via deep link
- No widget configuration needed
- Static widget (no dynamic updates)
- Widget configured for home screen placement only

---

### FR-2: Food Photo Capture

**Requirement:** Capture food photos with option to retake if blurry

**User Story:** As a user walking with my plate, I want to quickly photograph my food with one hand and retake if the photo is blurry so I get usable images for AI analysis.

**Acceptance Criteria:**
- Camera launches in full-screen mode
- Use built-in Android camera intent (system camera app) if it meets speed requirements
- Support single-tap capture
- Volume button alternative for capture (one-handed accessibility)
- Preview screen shows captured photo with "Retake" and "Use Photo" options
- Photo saved to temporary storage after confirmation
- Max photo resolution: 2MP
- Photo format: JPEG with 80% compression
- Auto-rotate based on device orientation

**Technical Notes:**
- Evaluate stock camera performance vs. custom CameraX implementation
- Implement photo compression before storage
- Store temporarily in app's cache directory

---

### FR-3: AI Nutrition Analysis

**Requirement:** Analyse food photos using Azure OpenAI to extract calorie count and description

**User Story:** As a user, I want the app to automatically analyse my food photo and estimate calories so I don't have to manually look up or guess the calorie content.

**Acceptance Criteria:**
- Send photo to Azure OpenAI GPT-4.1 API after capture
- Request structured JSON response: `{calories: number, description: string}`
- Parse API response and extract calorie count (integer) and food description (string, max 200 chars)
- Validate calorie range: > 0 and < 5000 (sanity check)
- API call completes in < 10 seconds (typical network conditions)
- Handle API errors with retry logic (see FR-9)
- Store API key securely in Android Keystore

**System Prompt:**
```
You are a nutrition analysis assistant. Analyse the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food).
```

**User Prompt:**
```
Analyse this meal and estimate total calories.
```

**Technical Notes:**
- Base64-encode photo for API request
- Use Responses API with `instructions` field for system-level guidance
- Parse `output_text` field from response for nutrition data
- Never log API key in crash reports or analytics

---

### FR-4: Background Processing

**Requirement:** Process AI analysis in background without blocking user

**User Story:** As a user, I want to pocket my phone immediately after taking a photo and have the analysis complete in the background so tracking doesn't interrupt my meal.

**Acceptance Criteria:**
- Background service starts automatically after photo confirmation
- Foreground notification displays: "Analyzing meal..." during processing
- Notification auto-dismisses on successful completion
- User can return to previous activity immediately after photo capture
- Processing completes within 15 seconds (excluding network delays)
- Service survives brief app termination (use WorkManager for reliability)
- Battery-efficient implementation (don't drain battery with continuous processing)

**Technical Notes:**
- Use Foreground Service for Android 8+ compatibility
- Implement WorkManager for deferred retry if immediate processing fails
- Handle Android battery optimization restrictions

---

### FR-5: Health Connect Data Storage

**Requirement:** Save calorie data to Google Health Connect for interoperability

**User Story:** As a user, I want my calorie data saved to Health Connect so other health apps (Google Fit, etc.) can access my nutrition information.

**Acceptance Criteria:**
- Save `NutritionRecord` with `energy` and `name` fields to Health Connect after successful AI analysis
- Use `HealthConnectClient.insertRecords()` API
- Request Health Connect permissions on first app launch
- Display clear permission rationale to user
- Handle permission denial gracefully (link to Health Connect settings)

**Data Flow:**
1. AI returns calories + description
2. Save `energy` (calories) and `name` (description) to Health Connect
3. Update UI list view by querying Health Connect

**Technical Notes:**
- Health Connect `NutritionRecord.name` field stores food descriptions
- No local database needed - Health Connect is single source of truth
- Simplifies architecture with single data store

---

### FR-6: Meal Entry List View

**Requirement:** Display recent meal entries for review and editing

**User Story:** As a user reviewing my day in the evening, I want to see a list of all my logged meals with calories and descriptions so I can verify accuracy and make corrections if needed.

**Acceptance Criteria:**
- Display list of nutrition entries from last 7 days (default view)
- Show for each entry: timestamp, food description, calorie count
- Sort newest first (reverse chronological)
- Pagination: Load 50 entries at a time
- Pull-to-refresh to reload from Health Connect and local database
- Tap entry to open edit screen
- Long-press entry to show delete confirmation
- Empty state message: "No meals logged yet. Use the widget to capture your first meal!"

**UI Layout:**
```
[Date Header: Today / Yesterday / Nov 7]
  12:30 PM - Grilled chicken with rice - 650 cal
  08:15 AM - Oatmeal with berries - 320 cal
[Date Header: Yesterday]
  ...
```

**Technical Notes:**
- Query directly from Health Connect using `TimeRangeFilter`
- Implement efficient RecyclerView with ViewHolder pattern
- Cache query results in ViewModel for smooth scrolling

---

### FR-7: Edit Meal Entry

**Requirement:** Allow manual correction of calorie count and description

**User Story:** As a user, I want to edit calories and descriptions when the AI estimate seems incorrect so I can maintain accurate nutrition tracking.

**Acceptance Criteria:**
- Tap entry in list view → Opens edit screen
- Edit screen shows: description (editable text field), calories (editable number field), timestamp (read-only)
- Save button updates both Health Connect (`energy` field) and local database (calories + description)
- Cancel button discards changes and returns to list
- Validation: Calories must be > 0 and < 5000
- Show toast confirmation: "Entry updated" on successful save
- Update list view immediately after save

**Edit Screen Layout:**
```
[Back Button] Edit Meal

Description:
[Grilled chicken with rice          ]

Calories:
[650                                 ]

Captured: Nov 7, 2025 at 12:30 PM

[Cancel]  [Save]
```

**Technical Notes:**
- Delete old `NutritionRecord` from Health Connect
- Insert new record with updated `energy` and `name` fields
- Preserve original `startTime` and `endTime` (immutable)

---

### FR-8: Delete Meal Entry

**Requirement:** Remove unwanted meal entries

**User Story:** As a user, I want to delete incorrect or duplicate entries so my nutrition log stays accurate.

**Acceptance Criteria:**
- Long-press entry in list view → Shows confirmation dialog
- Confirmation dialog: "Delete this entry? This cannot be undone." with Cancel/Delete buttons
- Delete button removes entry from both Health Connect and local database
- Entry disappears from list view immediately
- Show toast confirmation: "Entry deleted"
- No undo capability (permanent deletion)

**Technical Notes:**
- Use `HealthConnectClient.deleteRecords()` to remove from Health Connect
- Single operation (no dual-store complexity)

---

### FR-9: Network Error Handling & Retry

**Requirement:** Handle network failures gracefully with automatic retry

**User Story:** As a user in an area with poor connectivity, I want the app to retry failed API calls automatically so I don't lose my meal entries.

**Acceptance Criteria:**
- Detect network unavailability before API call
- Show notification: "No internet - meal saved, will analyse when online"
- Retry API call up to 3 times with exponential backoff (1s, 2s, 4s delays)
- After 3 failures: Show persistent notification "Meal analysis failed. Tap to retry manually."
- Manual retry button in notification re-triggers API call
- Keep photo stored during retry attempts
- Delete photo after successful save OR after all 3 retries exhausted
- Update notification status during retries: "Retrying analysis... (attempt 2/3)"

**Retry Logic:**
- Attempt 1: Immediate (0s delay)
- Attempt 2: After 1 second
- Attempt 3: After 2 seconds  
- Attempt 4: After 4 seconds
- After attempt 4 fails: Show manual retry notification

**Technical Notes:**
- Use WorkManager for reliable background retry
- Persist photo path and retry count in WorkManager data
- Implement exponential backoff to avoid hammering API

---

### FR-10: Settings & API Key Management

**Requirement:** Allow user to configure Azure OpenAI API key

**User Story:** As a user, I want to enter my Azure OpenAI API key so the app can access the AI service for nutrition analysis.

**Acceptance Criteria:**
- Settings screen accessible from app menu
- API Key field (masked text input for security)
- Save button encrypts and stores key in Android Keystore
- Test connection button validates API key by sending test request
- Show validation result: "API key valid ✓" or "Invalid API key - check your credentials"
- Pre-populated API key if already configured
- Clear instructions: "Enter your Azure OpenAI API key. Get one at portal.azure.com"

**Settings Screen Layout:**
```
Settings

Azure OpenAI API Key:
[•••••••••••••••••••••••••        ]

[Test Connection]

[Save]
```

**Technical Notes:**
- Store in Android Keystore (hardware-backed encryption)
- Never log key in crash reports or analytics
- Validate format before saving (check for common errors)

---

## Non-Functional Requirements

### NFR-1: Performance

**Speed is Critical - The Core Value Proposition**

**Requirements:**
- **Widget Launch:** < 500ms from tap to camera ready
- **Photo Capture:** < 2 seconds for single-tap capture
- **Total Capture Flow:** < 5 seconds from widget tap to return to previous activity
- **API Response:** < 10 seconds typical (network dependent)
- **Background Processing:** < 15 seconds total (capture to Health Connect save)
- **List View Load:** < 500ms to display 7 days of entries
- **Edit Screen:** < 200ms to open from list tap

**Rationale:**
The entire product value hinges on being faster than manual entry (30+ seconds). Any performance degradation undermines the core promise of "invisible tracking."

**Testing:**
- Measure actual timings during real-world usage
- Test on mid-range Android devices (not just flagship phones)
- Profile network latency impacts on background processing

---

### NFR-2: Reliability

**Requirements:**
- **Crash Rate:** < 1% of sessions (app should rarely crash)
- **Data Loss:** Zero tolerance - never lose a captured meal photo before successful Health Connect save OR retry exhaustion
- **Background Service:** Must survive brief app termination and device sleep
- **Data Consistency:** Health Connect is single source of truth (no sync conflicts)

**Critical Paths:**
1. Photo capture → temporary storage (must never fail)
2. API response → Health Connect save (retry until success or limit reached)
3. Edit operation → delete and re-insert in Health Connect

**Error Recovery:**
- All critical operations wrapped in try-catch with logging
- Automatic retry for transient failures (network, API timeouts)
- Manual retry option for persistent failures
- Clear error messages to user (never silent failures)

---

### NFR-3: Security & Privacy

**Requirements:**
- **API Key Storage:** Encrypted in Android Keystore (hardware-backed)
- **Photo Storage:** Temporary only - delete after successful processing or retry exhaustion
- **Network Communication:** HTTPS only for Azure OpenAI API calls
- **No Photo Cloud Storage:** Photos never uploaded to any storage service (only sent to Azure OpenAI API for analysis)
- **Local Data:** Health Connect handles encryption at rest
- **Logging:** Never log sensitive data (API keys, photo paths, user data)

**Privacy Principles:**
- Minimal data collection (only calories and descriptions)
- No analytics or tracking (personal tool, no telemetry)
- No user accounts or authentication required
- Data stays on device except API calls to Azure OpenAI
- User has full control to delete all data

---

### NFR-4: Usability

**Requirements:**
- **One-Handed Operation:** All primary flows (capture, review, edit) must be usable with thumb on single hand
- **Touch Targets:** Minimum 48dp for all tappable elements (Android accessibility guideline)
- **No Onboarding:** App must be immediately usable without tutorials or setup wizards
- **Error Messages:** Clear, actionable language (avoid technical jargon)
- **Dark Mode:** Full support for system dark theme
- **Accessibility:** Basic screen reader support (content descriptions for images/buttons)

**Design Constraints:**
- Large buttons for primary actions
- High contrast for outdoor visibility
- Minimal text input (rely on camera and taps)
- Forgiving interaction (undo for edits, confirmation for deletes)

---

### NFR-5: Maintainability

**Requirements:**
- **Code Quality:** 100% AI-generated code under senior engineer oversight
- **Architecture:** MVVM with clear separation of concerns (UI, ViewModel, Repository, Data)
- **Documentation:** Inline comments for complex logic, README with setup instructions
- **Testing:** Unit tests for critical business logic (API parsing, data sync, retry logic)
- **Version Control:** Git with meaningful commit messages
- **Dependencies:** Minimal external libraries (reduce maintenance burden)

**AI Development Workflow:**
- Write specifications before generating code
- Review and refactor AI-generated code
- Test each feature in isolation before integration
- Iterate on specs, not ad-hoc code changes

---

### NFR-6: Scalability (Future-Proofing)

**V1.0 Scope:**
- Single user (no multi-user support)
- Client-only architecture (no backend server)
- Local data storage only

**Future Considerations (V2.0+):**
- Architecture supports adding backend if needed (clean data layer abstraction)
- Health Connect integration allows other apps to access nutrition data
- Modular design allows adding features without major refactoring
- API integration abstracted to allow swapping Azure OpenAI for alternative services

**Non-Goals for V1.0:**
- Handling thousands of entries (optimize only if needed after real usage)
- Multi-device sync (single device only)
- Cloud backup (local data only)

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)

**Core Infrastructure:**
- Project setup with Android Studio, Kotlin, MVVM architecture
- Lock screen widget implementation (static button)
- Camera integration (evaluate stock camera vs CameraX)
- Photo capture and temporary storage
- Basic UI structure (list view scaffold)

**Milestone:** Widget launches camera, photo can be captured and stored temporarily

**Success Criteria:**
- Widget visible on lock screen
- Camera opens in < 500ms
- Photo saved to cache directory

---

### Phase 2: AI Integration (Week 1-2)

**Azure OpenAI Connection:**
- API key storage in Android Keystore
- Settings screen for API key entry
- HTTP client for Azure OpenAI API calls
- Base64 image encoding
- JSON response parsing (calories + description)
- Error handling for API failures

**Milestone:** Photo successfully analysed by AI, calories extracted

**Success Criteria:**
- API call completes in < 10 seconds
- Structured JSON response parsed correctly
- API key securely stored and retrieved

---

### Phase 3: Data Persistence (Week 2)

**Health Connect Integration:**
- Health Connect permissions flow
- Write `NutritionRecord` with `energy` and `name` fields
- Read operations for list view using `TimeRangeFilter`
- Update operations (delete + re-insert pattern)
- Delete operations

**Milestone:** Calorie and description data saved to Health Connect

**Success Criteria:**
- Health Connect permissions granted
- Data visible in Google Fit or other Health Connect apps
- Both calories and descriptions displayed correctly

---

### Phase 4: Background Processing (Week 2)

**Async Operations:**
- Foreground Service implementation
- WorkManager setup for retry logic
- Background notification ("Analyzing meal...")
- Network error detection
- Exponential backoff retry (3 attempts)
- Photo deletion after successful save or retry exhaustion

**Milestone:** End-to-end flow works - widget → camera → AI → Health Connect (background)

**Success Criteria:**
- User can return to previous activity immediately after photo capture
- Background processing completes in < 15 seconds
- Retry logic handles network failures gracefully

---

### Phase 5: CRUD Operations (Week 2)

**Data Management UI:**
- List view with recent entries (7 days) from Health Connect
- Edit screen (calories + description)
- Delete confirmation dialog
- Update operations (delete + re-insert in Health Connect)
- Pull-to-refresh functionality

**Milestone:** Full CRUD operations functional

**Success Criteria:**
- Entries displayed correctly in list from Health Connect
- Edit updates Health Connect record (delete + re-insert)
- Delete removes from Health Connect
- UI updates immediately after changes

---

### Phase 6: Polish & Testing (Week 2)

**Final Touches:**
- Dark mode support
- Error message improvements
- Performance profiling and optimization
- Manual testing of all flows
- Edge case testing (network failures, permission denials, etc.)
- Basic unit tests for critical logic

**Milestone:** MVP ready for real-world usage

**Success Criteria:**
- All functional requirements (FR-1 to FR-10) implemented
- Performance targets met (NFR-1)
- Zero known critical bugs
- Manual testing with 5+ real meals successful

---

## Testing Strategy

### Manual Testing (Primary Validation)

**Real-World Usage Testing:**
- Test with actual meals (breakfast, lunch, dinner, snacks)
- Measure capture time with stopwatch (target < 5 seconds)
- Verify AI accuracy with spot checks (cross-reference with nutrition databases)
- Test one-handed operation while holding plate
- Test in various lighting conditions (indoor, outdoor, low light)

**Edge Cases:**
- Network failures during API call
- Camera permission denied
- Health Connect not installed or permission denied
- Device reboot (widget persistence)
- App force-stopped by user or system
- Low battery / battery optimization

**Performance Testing:**
- Measure widget launch latency on mid-range device
- Profile background processing time
- Monitor battery drain during 5 meal captures
- Test with poor network connectivity (3G simulation)

---

### Automated Testing (Minimal for V1.0)

**Unit Tests (Critical Logic Only):**
- API response parsing (JSON to calories + description)
- Calorie validation (range check: 0-5000)
- Retry logic (exponential backoff calculation)
- Health Connect record creation and updates

**Test Coverage Target:** 30-40% (focus on business logic, not UI)

**No UI Testing:** Manual testing sufficient for personal tool (avoid automation overhead)

---

## Edge Cases & Error Scenarios

### Camera & Photo Capture

**Edge Case:** Camera permission denied
- **Handling:** Show rationale dialog, link to app settings, disable widget functionality

**Edge Case:** Photo is corrupted or failed to save
- **Handling:** Show error notification "Photo capture failed. Please try again."

**Edge Case:** User takes photo in extreme low light (black image)
- **Handling:** Allow AI to attempt analysis, user can retake if needed, manual edit available

---

### Network & API

**Edge Case:** No network connectivity
- **Handling:** Queue photo locally, show notification "Will analyse when online", retry on network restore

**Edge Case:** Azure OpenAI API is down or rate-limited
- **Handling:** Retry with exponential backoff, manual retry button after 3 failures

**Edge Case:** API returns invalid JSON or unexpected format
- **Handling:** Log error, show notification "Analysis failed - please edit manually", save entry with 0 calories + "Unknown food"

**Edge Case:** API estimates are wildly inaccurate (e.g., 10,000 calories for salad)
- **Handling:** Validation check (> 5000 cal triggers warning), user can edit, learn from patterns over time

---

### Health Connect & Data

**Edge Case:** Health Connect not installed on device
- **Handling:** Show setup dialog "Install Health Connect from Play Store", link provided

**Edge Case:** Health Connect permissions revoked mid-usage
- **Handling:** Detect on next save attempt, re-request permissions, queue entry locally until granted

**Edge Case:** Health Connect data appears corrupted or incomplete
- **Handling:** Show error message, allow user to manually retry query, log error for investigation

---

### Background Processing

**Edge Case:** App killed by system during background processing
- **Handling:** WorkManager reschedules task, processing resumes when app restarts

**Edge Case:** Device sleeps during API call
- **Handling:** Foreground Service prevents immediate sleep, use wake lock if needed

**Edge Case:** User force-stops app
- **Handling:** WorkManager task persists, retries after app restart

---

### Lock Screen Widget

**Edge Case:** Device security policy requires unlock for camera
- **Handling:** Widget still launches camera, system prompts for unlock (graceful degradation)

**Edge Case:** Widget removed from lock screen by user
- **Handling:** App still functional, user can re-add widget from settings

---

## References & Dependencies

### Input Documents

- **Product Brief:** `/Users/jbjornsson/source/foodie/docs/product-brief-foodie-2025-11-07.md`
  - Source of core vision, problem statement, and initial feature ideas
  - User context and learning goals
  
- **Brainstorming Session:** `/Users/jbjornsson/source/foodie/docs/bmm-brainstorming-session-2025-11-07.md`
  - "First Principles" insight: Simplest possible implementation
  - Architectural philosophy: Client-only, ephemeral photos

---

### Technical Dependencies

**Android Platform:**
- Minimum SDK: Android 9 (API 28)
- Target SDK: Android 15 (API 35)
- Compile SDK: Android 15 (API 35)
- Kotlin version: 2.2.21
- Android Gradle Plugin: 8.13.0
- Gradle: 8.13
- JDK: 17
- SDK Build Tools: 35.0.0

**Key Libraries:**
- Google Health Connect SDK (nutrition data storage - single source of truth)
- Android Keystore API (API key encryption)
- WorkManager (background processing)
- CameraX (optional - if stock camera doesn't meet requirements)
- Retrofit / OkHttp (Azure OpenAI API calls)

**External Services:**
- Azure OpenAI GPT-4.1 API (multimodal vision model)
- Google Health Connect (local device API, not cloud service)

---

### Related Standards & Guidelines

**Android Development:**
- Material Design 3 (UI components and patterns)
- Android Accessibility Guidelines (48dp touch targets, content descriptions)
- Android Background Work Guidelines (Foreground Service, WorkManager)

**Health Data:**
- Google Health Connect API Documentation
- FHIR standards (potential future consideration for data export)

**API Integration:**
- Azure OpenAI API Documentation
- JSON Schema for structured outputs
- HTTPS/TLS security standards

---

## Next Steps

### Immediate Actions (After PRD Approval)

1. **Architecture Document** (Next)
   - System architecture diagram (MVVM, Repository pattern)
   - Data flow diagrams (photo → AI → Health Connect)
   - Component interaction specifications
   - Database schema (local SQLite)
   - API integration patterns
   
2. **Technical Specification** (If Needed)
   - Detailed implementation specs for complex components
   - Lock screen widget technical design
   - Background service architecture
   - Health Connect sync algorithm

3. **Sprint Planning**
   - Break implementation roadmap into tasks
   - Estimate effort for each task
   - Prioritize critical path (widget → camera → AI → Health Connect)
   - Define acceptance criteria for each sprint

---

### Development Phase

**Week 1-2: Implementation**
- Follow implementation roadmap (Phases 1-6)
- Daily progress tracking
- Iterative testing with real meals

**Week 3-4: Validation**
- 30-day real-world usage test
- Track success metrics (speed, sustained usage)
- Identify issues and needed adjustments

---

### Post-MVP Decisions

**After 30-Day Validation:**
- Evaluate V2.0 feature priorities based on real usage patterns
- Decide on macros tracking, dashboard, Garmin integration
- Consider home screen widget if lock screen widget proves insufficient
- Assess offline queuing necessity based on network failure frequency

---

## Summary

**Foodie PRD v1.0 - Complete**

This PRD defines a native Android app that makes calorie tracking invisible through AI-powered photo capture and automatic nutrition logging. The core innovation is eliminating the speed-accuracy tradeoff that kills manual tracking adherence.

**Key Differentiators:**
- Lock screen widget: Fastest possible access (no unlock needed)
- AI analysis: Accurate calorie estimates without manual lookup
- Background processing: User continues immediately, no waiting
- Health Connect: Data interoperability with other health apps
- Ephemeral photos: Privacy by design (photos deleted after analysis)

**Success Criteria:**
- ≤ 5 second capture time (faster than 30+ second manual entry)
- 30-day sustained usage (proves no tracking fatigue)
- Trust-based AI accuracy (manual corrections available)

**MVP Timeline:** 1-2 weeks of AI-assisted development

**Next Deliverable:** Architecture document defining technical implementation approach

---

**Document Status:**
- ✅ Product vision defined
- ✅ Success metrics established
- ✅ Scope clearly bounded (MVP, V2.0, V3.0+)
- ✅ Functional requirements specified (FR-1 to FR-10)
- ✅ Non-functional requirements defined (NFR-1 to NFR-6)
- ✅ Mobile-specific requirements detailed
- ✅ Implementation roadmap outlined
- ✅ Testing strategy documented
- ✅ Edge cases identified

**Ready for:** Architecture phase with Architect agent

---

_This PRD captures "invisible tracking that makes precision nutrition sustainable" - the magic of Foodie._

_Created through collaborative discovery between BMad and PM Agent (John)._



