# Epic Technical Specification: Data Management & Review

Date: 2025-11-12
Author: BMad
Epic ID: 3
Status: Draft

---

## Overview

Epic 3 enables users to view, edit, and delete their nutrition entries through a modern Jetpack Compose UI that queries Health Connect as the single source of truth. This epic provides transparency and control over AI-generated meal data, supporting the "trust with transparency" principle where users can review and correct estimates during evening review without disrupting the meal capture flow. The implementation leverages existing MVVM architecture established in Epic 1, Health Connect integration from Epic 1.4, and follows Material 3 design patterns demonstrated in Epic 2.

The epic delivers a complete CRUD interface (Read via list view, Update via edit screen, Delete via confirmation dialog) with reactive state management using StateFlow, lifecycle-aware data loading, and proper error handling. All data operations interact directly with Health Connect NutritionRecord API - no local database layer is needed.

## Objectives and Scope

**In Scope:**
- Meal entry list view with 7-day history showing timestamp, description, and calories
- Date-grouped display with relative date headers ("Today", "Yesterday", "Nov 7")
- Reverse chronological sorting (newest first)
- Pull-to-refresh functionality with loading indicators
- Edit screen for modifying calories and description fields
- Update operations using Health Connect delete+insert pattern
- Delete confirmation dialog with permanent removal
- Empty state handling ("No meals logged yet...")
- Lifecycle-aware data refresh (onResume triggers reload)
- Navigation integration with existing NavGraph
- Material 3 UI components and theming
- Error handling for Health Connect permission issues
- Performance targets: <500ms list load, <200ms edit screen open, <1s refresh

**Out of Scope (Deferred to Later Epics):**
- Meal type categorization (breakfast/lunch/dinner) - V2.0 feature
- Custom date range filtering beyond 7 days - V2.0 feature
- Batch operations (select multiple, delete all) - V2.0 feature
- Search/filter functionality - V2.0 feature
- Export data functionality - V2.0 feature
- Undo delete capability - explicitly excluded (permanent deletion)
- Local caching layer - Health Connect is single source of truth
- Offline editing - requires network for Health Connect operations

## System Architecture Alignment

This epic aligns with the established **client-only architecture** using:

**UI Layer:**
- `MealListScreen.kt` - Jetpack Compose screen consuming StateFlow from ViewModel
- `MealDetailScreen.kt` - Edit screen with form validation and navigation
- Material 3 components: `LazyColumn`, `Card`, `OutlinedTextField`, `Button`, `Dialog`
- Navigation via existing `NavGraph` with type-safe route parameters

**Domain Layer:**
- `GetMealHistoryUseCase` - Orchestrates Health Connect query with date filtering
- `UpdateMealEntryUseCase` - Validates input and delegates to repository
- `DeleteMealEntryUseCase` - Handles confirmation logic and repository call
- Domain model: `MealEntry(id, calories, description, timestamp, recordId)`

**Data Layer:**
- `HealthConnectRepository` - Wraps Health Connect API with Result<T> error handling
- Methods: `queryNutritionRecords()`, `updateNutritionRecord()`, `deleteNutritionRecord()`
- Direct Health Connect integration via `HealthConnectManager` from Epic 1.4
- No local database - Health Connect stores all data in `NutritionRecord.energy` and `NutritionRecord.name` fields

**Constraints:**
- Health Connect permissions required (`READ_NUTRITION`, `WRITE_NUTRITION`)
- Minimum API 28 (Health Connect requirement)
- Query performance depends on Health Connect API (typically <500ms for 7 days)
- Network connectivity not required for Health Connect operations (local device storage)
- Updates use delete+insert pattern (Health Connect doesn't support direct updates)

## Detailed Design

### Services and Modules

| Component | Responsibility | Inputs | Outputs | Owner |
|-----------|---------------|--------|---------|-------|
| `MealListScreen` | Renders list of meal entries, handles user interactions | `StateFlow<MealListState>` from ViewModel | Navigation events, delete confirmations | UI Layer |
| `MealListViewModel` | Manages list state, coordinates use cases, exposes StateFlow | Use case results, user events | `StateFlow<MealListState>` | UI Layer |
| `MealDetailScreen` | Edit form with validation, save/cancel actions | Navigation args (recordId, calories, description, timestamp) | Navigation back event | UI Layer |
| `MealDetailViewModel` | Form state management, validation, save coordination | Form inputs, save events | `StateFlow<MealDetailState>` | UI Layer |
| `GetMealHistoryUseCase` | Queries Health Connect with 7-day filter, maps to domain model | None (uses current timestamp) | `Flow<Result<List<MealEntry>>>` | Domain Layer |
| `UpdateMealEntryUseCase` | Validates input, delegates update to repository | `MealEntry` with new values | `Result<Unit>` | Domain Layer |
| `DeleteMealEntryUseCase` | Delegates deletion to repository | `recordId: String` | `Result<Unit>` | Domain Layer |
| `HealthConnectRepository` | Wraps Health Connect CRUD operations with error handling | Query filters, NutritionRecord data | `Flow<Result<T>>` or `Result<T>` | Data Layer |
| `HealthConnectManager` | Direct Health Connect API interaction (from Epic 1.4) | HealthConnectClient operations | Health Connect results or exceptions | Data Layer |

### Data Models and Contracts

**Domain Model:**
```kotlin
data class MealEntry(
    val id: String,              // Unique identifier for UI list keys
    val recordId: String,        // Health Connect record ID (for updates/deletes)
    val calories: Int,           // Energy in kcal
    val description: String,     // Food description from AI or user edit
    val timestamp: Instant,      // When meal was logged (startTime from NutritionRecord)
    val zoneOffset: ZoneOffset   // Timezone for correct timestamp display
)
```

**UI State Models:**
```kotlin
data class MealListState(
    val mealsByDate: Map<String, List<MealEntry>> = emptyMap(), // "Today" -> [meals...]
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val emptyStateVisible: Boolean = false
)

data class MealDetailState(
    val recordId: String,
    val calories: String,          // String for TextField binding
    val description: String,
    val timestamp: String,         // Formatted read-only display
    val isSaving: Boolean = false,
    val error: String? = null,
    val caloriesError: String? = null,  // Validation error for calories field
    val descriptionError: String? = null // Validation error for description field
)
```

**Health Connect Mapping:**
```kotlin
// Conversion from Health Connect NutritionRecord to domain MealEntry
fun NutritionRecord.toMealEntry(): MealEntry {
    return MealEntry(
        id = metadata.id,           // Health Connect assigns unique ID
        recordId = metadata.id,
        calories = energy.inKilocalories.toInt(),
        description = name ?: "",   // NutritionRecord.name field stores description
        timestamp = startTime,
        zoneOffset = startZoneOffset ?: ZoneOffset.UTC
    )
}

// Conversion from MealEntry to Health Connect NutritionRecord (for updates)
fun MealEntry.toNutritionRecord(): NutritionRecord {
    return NutritionRecord(
        energy = Energy.kilocalories(calories.toDouble()),
        name = description,
        startTime = timestamp,
        endTime = timestamp,
        startZoneOffset = zoneOffset,
        endZoneOffset = zoneOffset
    )
}
```

### APIs and Interfaces

**Repository Interface:**
```kotlin
interface HealthConnectRepository {
    /**
     * Query nutrition records from Health Connect for the last 7 days
     * Returns Flow for reactive updates when data changes
     */
    fun getMealHistory(): Flow<Result<List<MealEntry>>>
    
    /**
     * Update existing meal entry using delete+insert pattern
     * Health Connect doesn't support direct updates, so we delete old record
     * and insert new record with same timestamp
     * 
     * @param recordId Health Connect record ID to update
     * @param calories New calorie value (must be 1-5000)
     * @param description New food description (max 200 chars)
     * @return Result.Success or Result.Error
     */
    suspend fun updateMealEntry(
        recordId: String,
        calories: Int,
        description: String
    ): Result<Unit>
    
    /**
     * Delete nutrition record from Health Connect
     * Permanent deletion - cannot be undone
     * 
     * @param recordId Health Connect record ID to delete
     * @return Result.Success or Result.Error
     */
    suspend fun deleteMealEntry(recordId: String): Result<Unit>
}
```

**ViewModel Interfaces (Events):**
```kotlin
// MealListViewModel events
sealed class MealListEvent {
    object Refresh : MealListEvent()
    data class DeleteMeal(val recordId: String) : MealListEvent()
    data class NavigateToDetail(val entry: MealEntry) : MealListEvent()
}

// MealDetailViewModel events
sealed class MealDetailEvent {
    data class CaloriesChanged(val value: String) : MealDetailEvent()
    data class DescriptionChanged(val value: String) : MealDetailEvent()
    object SaveClicked : MealDetailEvent()
    object CancelClicked : MealDetailEvent()
}
```

### Workflows and Sequencing

**Story 3.1: List View Load Sequence**
```
1. User opens Foodie app
2. MainActivity launches with NavGraph
3. NavGraph navigates to MealListScreen (start destination)
4. MealListViewModel.init():
   - Call getMealHistoryUseCase.invoke()
   - Emit MealListState(isLoading = true)
5. GetMealHistoryUseCase:
   - Calculate startTime = now() - 7.days
   - Call repository.getMealHistory()
6. HealthConnectRepository:
   - Query HealthConnectClient.readRecords(
       recordType = NutritionRecord::class,
       timeRangeFilter = TimeRangeFilter(startTime, now())
     )
   - Map NutritionRecord list to MealEntry list
   - Emit Result.Success(meals)
7. ViewModel receives Flow emission:
   - Group meals by date ("Today", "Yesterday", "Nov 7")
   - Update state: MealListState(mealsByDate = grouped, isLoading = false)
8. MealListScreen recomposes:
   - Render LazyColumn with date headers
   - Display MealEntryCard for each entry
   
Time: <500ms from step 3 to step 8
```

**Story 3.2-3.3: Edit and Update Sequence**
```
1. User taps meal entry in list
2. MealListViewModel emits NavigateToDetail event
3. NavController navigates to MealDetailScreen with args:
   - recordId, calories, description, timestamp
4. MealDetailScreen renders form with pre-filled values
5. User modifies calories or description
6. MealDetailViewModel validates on each change:
   - Calories: must be 1-5000, numeric only
   - Description: max 200 characters
   - Update state with validation errors if invalid
7. User taps Save button
8. MealDetailViewModel.onEvent(SaveClicked):
   - Final validation check
   - If valid, call updateMealEntryUseCase.invoke()
   - Emit MealDetailState(isSaving = true)
9. UpdateMealEntryUseCase:
   - Call repository.updateMealEntry(recordId, calories, description)
10. HealthConnectRepository.updateMealEntry():
    - Read original NutritionRecord to get timestamp/metadata
    - Delete old record: client.deleteRecords([recordId])
    - Insert new record: client.insertRecords([
        NutritionRecord(
          energy = new calories,
          name = new description,
          startTime = original timestamp,
          endTime = original timestamp
        )
      ])
    - Return Result.Success
11. ViewModel receives success:
    - Navigate back to MealListScreen
    - Show Toast "Entry updated"
12. MealListScreen automatically refreshes (lifecycle onResume)
    - Updated entry appears in list
    
Time: <200ms for steps 3-4, <1s for steps 8-11
```

**Story 3.4: Delete Sequence**
```
1. User long-presses meal entry in list
2. MealListScreen shows AlertDialog:
   - Title: "Delete Entry"
   - Message: "Delete this entry? This cannot be undone."
   - Buttons: Cancel, Delete
3. User taps Delete
4. MealListViewModel.onEvent(DeleteMeal(recordId)):
   - Call deleteMealEntryUseCase.invoke(recordId)
   - Update state: isLoading = true (optional loading indicator)
5. DeleteMealEntryUseCase:
   - Call repository.deleteMealEntry(recordId)
6. HealthConnectRepository.deleteMealEntry():
   - Call client.deleteRecords([recordId])
   - Return Result.Success
7. ViewModel receives success:
   - Remove meal from state.mealsByDate
   - Show Toast "Entry deleted"
   - Dismiss dialog
8. MealListScreen recomposes without deleted entry

Time: <1s for steps 4-8
```

**Story 3.5: Refresh Sequence**
```
Scenario A: Pull-to-refresh
1. User pulls down on list (SwipeRefresh gesture)
2. MealListViewModel.onEvent(Refresh):
   - Emit state(isRefreshing = true)
   - Call getMealHistoryUseCase.invoke()
3. [Same flow as initial load - steps 5-8 from Story 3.1]
4. Emit state(isRefreshing = false)

Scenario B: Lifecycle refresh
1. User returns to app from background
2. MealListScreen.LaunchedEffect(lifecycle.currentState):
   - Detect RESUMED state
   - Call viewModel.refresh()
3. [Same flow as pull-to-refresh]

Time: <1s for refresh completion
```

## Non-Functional Requirements

### Performance

**List Load Performance:**
- Target: <500ms from screen render to data displayed
- Measurement: Time from `MealListScreen` composition to first frame with data
- Strategy:
  - Health Connect queries are local (no network latency)
  - Use lazy loading with `LazyColumn` (only renders visible items)
  - Pagination: Load 50 entries at a time (7 days typically <30 entries)
  - Date grouping happens in ViewModel (one-time operation on data load)
- Acceptable degradation: Up to 1 second on low-end devices (API 28 minimum)

**Edit Screen Performance:**
- Target: <200ms from tap to edit screen displayed
- Measurement: Time from `navigateToDetail()` call to `MealDetailScreen` first frame
- Strategy:
  - Navigation passes data as arguments (no async loading needed)
  - Pre-populate form fields from navigation args
  - Lightweight screen (2 text fields, 2 buttons)
- No degradation expected (pure UI rendering)

**Refresh Performance:**
- Target: <1 second from pull gesture to updated data displayed
- Measurement: Time from `isRefreshing = true` to `isRefreshing = false`
- Strategy:
  - Health Connect query is local (fast)
  - Reuse existing ViewModel state structure
  - Show loading indicator to communicate progress
- Acceptable degradation: Up to 2 seconds if Health Connect has large dataset

**Memory Efficiency:**
- Max memory overhead: <10MB for list of 100 entries
- Strategy:
  - `LazyColumn` only allocates visible items
  - `MealEntry` is lightweight data class (< 100 bytes per entry)
  - No bitmap/image loading in list view
  - ViewModels cleared when navigating away

### Security

**Health Connect Permissions:**
- **Required permissions:** `READ_NUTRITION`, `WRITE_NUTRITION`
- **Permission flow:**
  1. Check permissions before each Health Connect operation
  2. If denied, show clear error message with link to app settings
  3. Re-request permissions on next app launch if previously denied
- **Threat model:**
  - Local device storage only (Health Connect is on-device)
  - No network transmission of meal data
  - No authentication/authorization needed (single-user app)
  - Android permission system protects Health Connect data from other apps

**Data Privacy:**
- All meal data stays on device (Health Connect is local)
- No analytics tracking of meal entries
- No cloud backup of Health Connect data (user's responsibility via Google backup)
- Logging excludes sensitive meal descriptions (only log record IDs)

**Input Validation:**
- **Calories field:**
  - Type: Integer only (reject non-numeric input)
  - Range: 1-5000 kcal (sanity check)
  - Empty field: Show error "Calories required"
- **Description field:**
  - Type: String
  - Max length: 200 characters (enforced by TextField)
  - Empty allowed (defaults to empty string)
  - No special character restrictions (allow emoji, international text)

### Reliability/Availability

**Error Handling:**
- **Health Connect unavailable:**
  - Show error dialog: "Health Connect is required. Install from Play Store?"
  - Provide deep link to Play Store Health Connect listing
  - Block app functionality until Health Connect available
- **Permissions denied:**
  - Show error message: "Nutrition permissions required. Grant access in Settings?"
  - Provide deep link to app settings
  - Allow retry after user grants permissions
- **Query failures:**
  - Show error state in list view with retry button
  - Log error with Timber for debugging
  - Don't crash app (show user-friendly message)
- **Update/Delete failures:**
  - Show Toast with error message: "Failed to update entry. Please try again."
  - Don't navigate away from edit screen (allow user to retry)
  - Log error details for debugging

**Data Consistency:**
- **Health Connect is single source of truth:**
  - No local caching that could become stale
  - Every screen load queries Health Connect fresh
  - Updates immediately reflected via Flow emissions
- **Update pattern (delete+insert):**
  - Preserve original timestamp exactly
  - Preserve original timezone offset
  - If delete succeeds but insert fails: Log error, show user message
  - User can retry update (delete already happened, so original data lost)
- **Optimistic UI updates:**
  - Not implemented - wait for Health Connect confirmation
  - Prevents showing stale data if operation fails

**Offline Behavior:**
- Health Connect operations are local (no network required)
- No offline considerations needed for this epic
- Epic 4 addresses network failures for AI analysis flow

### Observability

**Logging Strategy:**
- Use Timber for all logging (configured in Epic 1.5)
- Log levels:
  - **DEBUG:** Health Connect query timing, record counts
  - **INFO:** User actions (delete meal, update meal)
  - **WARN:** Permission denied, query returned 0 results
  - **ERROR:** Health Connect API failures, parsing errors

**Key Log Points:**
```kotlin
// List loading
Timber.d("Querying Health Connect for meals: startTime=$startTime, endTime=$endTime")
Timber.d("Loaded ${meals.size} meals in ${elapsedMs}ms")

// Updates
Timber.i("Updating meal: recordId=$recordId, calories=$calories")
Timber.i("Meal updated successfully: recordId=$recordId")

// Deletes
Timber.i("Deleting meal: recordId=$recordId")
Timber.i("Meal deleted successfully: recordId=$recordId")

// Errors
Timber.e("Failed to query Health Connect", exception)
Timber.e("Failed to update meal: recordId=$recordId", exception)
Timber.w("Health Connect permissions denied")
```

**Metrics to Track (Future):**
- List load time (p50, p95, p99)
- Refresh frequency (how often users pull-to-refresh)
- Edit frequency (% of meals that get edited)
- Delete frequency (% of meals that get deleted)
- Error rates by operation type

**No Crash Reporting:**
- Epic 1.5 deferred Crashlytics to optional
- For personal tool, Logcat debugging is sufficient
- Future: Add Firebase Crashlytics if expanding to multi-user

## Dependencies and Integrations

**Internal Dependencies (from Previous Epics):**
- **Epic 1.2 (MVVM Architecture):** ViewModel pattern, Repository pattern, Hilt DI
- **Epic 1.3 (Navigation):** NavGraph, deep linking, SafeArgs for type-safe parameters
- **Epic 1.4 (Health Connect):** HealthConnectManager, permission handling, NutritionRecord CRUD
- **Epic 1.5 (Error Handling):** Timber logging, Result<T> wrapper, error message mapping
- **Epic 2.6 (Health Connect Save):** Existing NutritionRecord data to query/display

**External Dependencies:**
```kotlin
// Health Connect (from Epic 1.4)
implementation("androidx.health.connect:connect-client:1.1.0")

// Compose UI (from Epic 1.1)
val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
implementation(composeBom)
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation (from Epic 1.3)
implementation("androidx.navigation:navigation-compose:2.8.4")

// Lifecycle & ViewModel (from Epic 1.2)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Hilt DI (from Epic 1.2)
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-android-compiler:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Kotlin Coroutines (from Epic 1.2)
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// Timber Logging (from Epic 1.5)
implementation("com.jakewharton.timber:timber:5.0.1")
```

**No New Dependencies Required** - All libraries already included in project from Epic 1.

**Integration Points:**
- **Health Connect API:** `HealthConnectClient.readRecords()`, `deleteRecords()`, `insertRecords()`
- **Navigation:** `NavController.navigate()` with MealEntry data as args
- **Material 3 Theme:** Use existing theme from Epic 1 (colors, typography, shapes)
- **Hilt Modules:** Register repositories and use cases in existing DI graph

## Acceptance Criteria (Authoritative)

### AC-1: List View Displays Recent Meals
**Given** I have logged meals in Health Connect over the past 7 days  
**When** I open the Foodie app to the main screen  
**Then** a list displays all nutrition entries from the last 7 days  
**And** entries are sorted newest first (reverse chronological)  
**And** each entry shows timestamp, food description, and calorie count  
**And** entries are grouped by date headers ("Today", "Yesterday", "Nov 7")  
**And** the list loads in under 500ms

### AC-2: Empty State
**Given** I have not logged any meals in Health Connect  
**When** I open the Foodie app  
**Then** an empty state displays: "No meals logged yet. Use the widget to capture your first meal!"

### AC-3: Pull-to-Refresh
**Given** I am viewing the meal list  
**When** I pull down on the list (swipe-to-refresh gesture)  
**Then** a loading indicator displays  
**And** the list reloads data from Health Connect  
**And** the refresh completes in under 1 second  
**And** new entries appear if added by other apps

### AC-4: Navigate to Edit Screen
**Given** I am viewing the meal list  
**When** I tap a meal entry  
**Then** the edit screen opens in under 200ms  
**And** the screen displays the current description in an editable text field  
**And** the screen displays the current calories in an editable number field  
**And** the screen displays the timestamp as read-only text

### AC-5: Edit and Save Meal Entry
**Given** I am on the edit screen  
**When** I modify the calories or description and tap "Save"  
**Then** the old NutritionRecord is deleted from Health Connect  
**And** a new NutritionRecord is inserted with updated values  
**And** the original timestamp is preserved exactly  
**And** a toast message displays: "Entry updated"  
**And** I am navigated back to the list view  
**And** the list view shows the updated entry immediately

### AC-6: Calorie Validation
**Given** I am on the edit screen  
**When** I enter calories less than 1 or greater than 5000  
**Then** an error message displays: "Calories must be between 1 and 5000"  
**And** the Save button is disabled or shows error state

### AC-7: Description Validation
**Given** I am on the edit screen  
**When** I enter a description longer than 200 characters  
**Then** the text field enforces the 200 character limit  
**And** no error message is shown (hard limit enforcement)

### AC-8: Cancel Edit
**Given** I am on the edit screen with unsaved changes  
**When** I tap "Cancel"  
**Then** I am navigated back to the list view  
**And** no changes are saved to Health Connect

### AC-9: Delete Meal Entry
**Given** I am viewing the meal list  
**When** I long-press a meal entry  
**Then** a confirmation dialog displays: "Delete this entry? This cannot be undone."  
**And** the dialog has "Cancel" and "Delete" buttons

### AC-10: Confirm Delete
**Given** the delete confirmation dialog is showing  
**When** I tap "Delete"  
**Then** the entry is removed from Health Connect  
**And** the entry disappears from the list view immediately  
**And** a toast message displays: "Entry deleted"  
**And** the deletion is reflected in other Health Connect apps

### AC-11: Cancel Delete
**Given** the delete confirmation dialog is showing  
**When** I tap "Cancel"  
**Then** the dialog dismisses  
**And** no changes are made to the meal entry

### AC-12: Lifecycle Refresh
**Given** I am viewing the meal list  
**When** I switch to another app and then return to Foodie  
**Then** the list automatically refreshes from Health Connect  
**And** new entries appear if added while app was in background

### AC-13: Permission Handling
**Given** Health Connect permissions are denied  
**When** the app attempts to query meal data  
**Then** an error message displays: "Nutrition permissions required. Grant access in Settings?"  
**And** a button provides deep link to app settings

### AC-14: Health Connect Unavailable
**Given** Health Connect is not installed on the device  
**When** I open the Foodie app  
**Then** an error dialog displays: "Health Connect is required. Install from Play Store?"  
**And** a button provides deep link to Play Store Health Connect listing

### AC-15: Update Error Handling
**Given** I am editing a meal entry  
**When** the Health Connect update operation fails (network/permission issue)  
**Then** a toast displays: "Failed to update entry. Please try again."  
**And** I remain on the edit screen with my changes preserved  
**And** I can retry the save operation

## Traceability Mapping

| AC | Spec Section | Component | Test Idea |
|----|--------------|-----------|-----------|
| AC-1 | Workflows: List View Load | MealListViewModel, GetMealHistoryUseCase, HealthConnectRepository | Mock Health Connect with 10 meals over 7 days, verify list displays all, sorted newest first, grouped by date |
| AC-2 | Data Models: MealListState.emptyStateVisible | MealListScreen | Mock Health Connect returning empty list, verify empty state text displays |
| AC-3 | Workflows: Refresh Sequence | MealListViewModel.onEvent(Refresh) | Trigger refresh event, verify isRefreshing state toggles, verify new query sent |
| AC-4 | APIs: NavigateToDetail event | MealListViewModel, Navigation | Simulate tap event, verify navigation args passed correctly, verify screen loads <200ms |
| AC-5 | Workflows: Edit and Update Sequence | UpdateMealEntryUseCase, HealthConnectRepository.updateMealEntry() | Mock delete+insert success, verify original timestamp preserved, verify navigation back |
| AC-6 | NFR: Input Validation | MealDetailViewModel validation logic | Enter calories 0, 5001, -100, verify error messages, verify Save disabled |
| AC-7 | NFR: Input Validation | MealDetailScreen TextField maxLength | Type 201 characters, verify only 200 appear in field |
| AC-8 | Workflows: Edit Sequence step 7 | MealDetailViewModel.onEvent(CancelClicked) | Modify fields, tap Cancel, verify navigation back, verify no Health Connect call |
| AC-9 | Workflows: Delete Sequence steps 1-2 | MealListScreen long-press handler | Long-press meal entry, verify AlertDialog shows with correct text |
| AC-10 | Workflows: Delete Sequence steps 3-8 | DeleteMealEntryUseCase, HealthConnectRepository.deleteMealEntry() | Mock delete success, verify meal removed from state, verify Toast shown |
| AC-11 | Workflows: Delete Sequence step 3 | AlertDialog Cancel button | Tap Cancel in delete dialog, verify dialog dismisses, verify no Health Connect call |
| AC-12 | Workflows: Refresh Sequence Scenario B | MealListScreen lifecycle observer | Simulate app backgrounded and resumed, verify refresh triggered automatically |
| AC-13 | NFR: Health Connect Permissions | HealthConnectRepository error handling | Mock permission denied exception, verify error message shown, verify settings link |
| AC-14 | NFR: Health Connect Permissions | HealthConnectManager availability check | Mock Health Connect unavailable, verify error dialog, verify Play Store link |
| AC-15 | NFR: Error Handling - Update failures | UpdateMealEntryUseCase error path | Mock Health Connect update failure, verify error Toast, verify screen state preserved |

## Risks, Assumptions, Open Questions

### Risks

**R-1: Health Connect Query Performance**
- **Risk:** Querying 7 days of data may exceed 500ms target on low-end devices
- **Likelihood:** Low (Health Connect is local, optimized by Google)
- **Impact:** Medium (degrades user experience but doesn't block functionality)
- **Mitigation:** 
  - Test on API 28 device (minimum supported)
  - If slow, reduce default range to 3 days
  - Add pagination for larger datasets

**R-2: Delete+Insert Race Condition**
- **Risk:** If delete succeeds but insert fails, user loses original data
- **Likelihood:** Very Low (Health Connect operations are atomic)
- **Impact:** High (data loss is unacceptable)
- **Mitigation:**
  - Log all update operations with full data
  - Show clear error message if insert fails
  - Future: Implement undo/backup mechanism in Epic 4

**R-3: Timezone Handling**
- **Risk:** Meal timestamps may display incorrectly if user changes timezone
- **Likelihood:** Low (personal tool, infrequent travel)
- **Impact:** Low (cosmetic issue, data integrity maintained)
- **Mitigation:**
  - Store ZoneOffset with each record
  - Display timestamps in original timezone (when captured)
  - Future: Add timezone conversion in settings

### Assumptions

**A-1: Health Connect Availability**
- Assumption: Users have Health Connect installed (required for app to function)
- Validation: Check at app launch, prompt installation if missing
- Fallback: App is unusable without Health Connect (by design)

**A-2: Single User Context**
- Assumption: Only one user per device (no multi-user support needed)
- Validation: Android user profiles handle device sharing
- Fallback: N/A (out of scope for MVP)

**A-3: 7-Day History is Sufficient**
- Assumption: Users primarily review recent meals, not full history
- Validation: Test with real usage over 30 days
- Fallback: Can extend to 30 days if needed (query parameter change)

**A-4: No Local Caching Needed**
- Assumption: Health Connect queries are fast enough (<500ms) to query on every load
- Validation: Performance testing on API 28 device
- Fallback: Add in-memory cache if queries are slow (StateFlow already provides caching)

### Open Questions

**Q-1: Date Grouping Format**
- Question: Should we use "Today/Yesterday" or absolute dates for all headers?
- Options:
  - A) Relative for <3 days, absolute for older ("Today", "Yesterday", "Nov 9")
  - B) Always absolute ("Nov 12", "Nov 11", "Nov 10")
  - C) Mix relative + absolute ("Today (Nov 12)", "Yesterday (Nov 11)")
- Recommendation: **Option A** (matches Google Fit, common pattern)
- Decision needed: Before Story 3.1 implementation

**Q-2: Empty State Action**
- Question: Should empty state include a "Capture Meal" button to launch widget?
- Options:
  - A) Text only (current spec)
  - B) Text + button to launch camera directly
  - C) Text + link to widget setup instructions
- Recommendation: **Option A** (keep it simple, users already have widget)
- Decision needed: Before Story 3.1 implementation

**Q-3: Pagination Strategy**
- Question: 50 entries per page is arbitrary - do we need pagination at all for 7 days?
- Options:
  - A) No pagination, load all 7 days (typically <30 entries)
  - B) Pagination with 50 per page (current spec)
  - C) Infinite scroll with 20 per page
- Recommendation: **Option A** (simpler implementation, 7 days is small dataset)
- Decision needed: Before Story 3.1 implementation

**Q-4: Meal Type Display**
- Question: Should we show meal type (breakfast/lunch/dinner) in list view?
- Context: PRD defers meal type categorization to V2.0
- Options:
  - A) Don't show meal type (align with PRD)
  - B) Show meal type if available in Health Connect (future-proof)
- Recommendation: **Option A** (stick to MVP scope)
- Decision needed: Confirmed in PRD review

## Test Strategy Summary

### Unit Testing
- **ViewModel Tests:**
  - MealListViewModel: Test state emissions for load, refresh, delete, error cases
  - MealDetailViewModel: Test form validation, save logic, cancel logic
  - Use MockK or Mockito to mock use cases
  - Verify StateFlow emissions match expected states
  - Test edge cases: empty list, single entry, 100+ entries
  
- **Use Case Tests:**
  - GetMealHistoryUseCase: Mock repository, verify 7-day filter applied
  - UpdateMealEntryUseCase: Verify validation logic (calories 1-5000, description ≤200 chars)
  - DeleteMealEntryUseCase: Mock repository, verify pass-through logic
  
- **Repository Tests:**
  - HealthConnectRepository: Mock HealthConnectManager
  - Test query mapping (NutritionRecord → MealEntry)
  - Test update logic (delete + insert with preserved timestamp)
  - Test error handling (permissions, unavailable, query failures)

### Integration Testing (Instrumentation Tests)
- **Screen Tests:**
  - MealListScreen: Verify list renders, pull-to-refresh works, long-press shows dialog
  - MealDetailScreen: Verify form validation, navigation, save/cancel
  - Use Compose UI test framework (`composeTestRule.setContent { }`)
  - Mock ViewModel state, verify UI reacts correctly
  
- **Navigation Tests:**
  - Verify tap navigation from list to detail with correct args
  - Verify back navigation after save/cancel
  - Use `TestNavHostController` for navigation testing

### Manual Testing Checklist
- [ ] Install app on API 28 device (minimum SDK)
- [ ] Verify Health Connect permission flow
- [ ] Log 10 meals over 7 days using widget (from Epic 2)
- [ ] Open app, verify all meals display grouped by date
- [ ] Tap meal, edit calories, save, verify update appears
- [ ] Long-press meal, delete, verify removal
- [ ] Pull-to-refresh, verify loading indicator
- [ ] Background app, modify meal in Google Fit, resume app, verify update
- [ ] Deny permissions, verify error message and settings link
- [ ] Uninstall Health Connect, verify error dialog and Play Store link
- [ ] Test performance: <500ms list load, <200ms edit screen, <1s refresh

### Performance Testing
- Use Android Profiler to measure:
  - Time from screen composition to data displayed
  - Memory allocation during list scroll
  - Health Connect query duration
- Target devices:
  - High-end: Pixel 8 Pro (Android 15)
  - Low-end: API 28 emulator (minimum supported)

### Coverage Goals
- **Unit tests:** >80% coverage for ViewModels, Use Cases, Repositories
- **Integration tests:** Cover all AC scenarios (15 tests minimum, one per AC)
- **Manual tests:** Full checklist completion on physical device

---

**End of Epic 3 Technical Specification**
