# Story 4.5: Health Connect Permission and Availability Handling

Status: in-progress

## Change Log

| Date | Author | Change |
|------|--------|--------|
| 2025-11-16 | BMad (PM) | Story revised based on Story 4.3 overlap analysis - 60% already implemented, focus on HC availability |

## Story

As a user,
I want clear guidance when Health Connect is unavailable,
So that I can install it and continue tracking.

## Acceptance Criteria

**Given** Health Connect may not be installed on the device
**When** the app attempts to access Health Connect
**Then** if Health Connect is not installed, show: "Health Connect required. Install from Play Store?" with link to Play Store

**And** if user taps link, Google Play Store opens to Health Connect app page

**And** app re-checks Health Connect availability when returning from Play Store

**And** clear error message shown if HC unavailable: "Health Connect is required for nutrition tracking"

**And** app checks HC availability on each app foreground/resume (MainActivity.onResume())

**And** if permissions revoked while app backgrounded, re-request on next Health Connect operation

**And** graceful handling if user declines Play Store installation (show alternative: "Manual install required")

## Notes from Story 4.3

### Already Implemented (60% Complete)

✅ **HealthConnectPermissionFlow Component** (Bug Fix #2 - 2025-11-16)
- Location: `app/src/main/java/com/foodie/app/ui/components/HealthConnectPermissionFlow.kt` (188 lines)
- Comprehensive permission gate component ✅
- Shows OS permission dialog → education screen on denial → retry capability ✅
- Reusable across app (used in widget-first flow) ✅

✅ **Permission Denial Education Screen**
- Location: `HealthConnectPermissionFlow.kt:103-188` (HealthConnectDenialScreen composable)
- Clear instructions explaining why permissions needed ✅
- "Retry" button re-requests permissions ✅
- "Cancel" button exits flow gracefully ✅

✅ **Permission Re-Request on Operation**
- Widget-first flow now gates on permissions before camera ✅
- Permission check happens before photo capture (fail fast) ✅

✅ **Photo Retention on Permission Errors**
- Implemented via Story 4.3's photo retention fix ✅
- PermissionDenied error retains photo for retry ✅

### Remaining Work (40%)

This story focuses on:
1. ❌ **Health Connect Availability Check** - SDK.isAvailable() on app launch
2. ❌ **Play Store Redirect** - Install HC if not available
3. ❌ **Lifecycle Permission Re-Check** - MainActivity.onResume() validation

## Tasks / Subtasks

- [ ] **Task 1: Documentation Research** ⚠️ COMPLETE BEFORE PROCEEDING

  **Objective:** Understand Health Connect SDK availability checks and Play Store intents

  **Required Research:**
  1. Review Health Connect SDK documentation
     - Method: `HealthConnectClient.getSdkStatus(context)`
     - Return values: SDK_AVAILABLE, SDK_UNAVAILABLE, SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
     - Link: https://developer.android.com/health-and-fitness/guides/health-connect
  
  2. Review existing HealthConnectManager implementation
     - File: `app/src/main/java/com/foodie/app/data/local/healthconnect/HealthConnectManager.kt`
     - Check if availability check already exists
     - Review permission check implementation
  
  3. Research Play Store Intent for Health Connect
     - Package name: "com.google.android.apps.healthdata"
     - Intent action: `Intent.ACTION_VIEW` with market:// URI
  
  **Deliverable:** Document in Dev Notes:
  - SDK status check approach
  - Play Store intent pattern
  - Lifecycle hook for permission re-check (MainActivity.onResume())

- [ ] **Task 2: Add Health Connect Availability Check** (AC: #1, #4)
  - [ ] Add method to HealthConnectManager: `isHealthConnectAvailable(): Boolean`
  - [ ] Call `HealthConnectClient.getSdkStatus(context)`
  - [ ] Return true if SDK_AVAILABLE, false otherwise
  - [ ] Add method: `getHealthConnectStatus(): HealthConnectStatus` (enum: AVAILABLE, NOT_INSTALLED, UPDATE_REQUIRED)
  - [ ] Log status: `Timber.i("Health Connect status: $status")`

- [ ] **Task 3: Create Health Connect Unavailable Screen** (AC: #1, #2, #7)
  - [ ] Create composable: `HealthConnectUnavailableScreen(onInstallClick, onCancel)`
  - [ ] Show title: "Health Connect Required"
  - [ ] Show message: "Health Connect is required for nutrition tracking. Install from Play Store?"
  - [ ] Add "Install" button → calls onInstallClick()
  - [ ] Add "Cancel" button → calls onCancel()
  - [ ] Follow Material Design guidelines (similar to HealthConnectDenialScreen)

- [ ] **Task 4: Implement Play Store Intent** (AC: #2, #3)
  - [ ] Create utility method: `openHealthConnectInPlayStore(context: Context)`
  - [ ] Build Intent: `Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))`
  - [ ] Add fallback for devices without Play Store: `https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata`
  - [ ] Handle ActivityNotFoundException if Play Store not installed
  - [ ] Log action: `Timber.i("Opening Health Connect in Play Store")`

- [ ] **Task 5: Integrate Availability Check into App Launch** (AC: #5)
  - [ ] Add availability check to MainActivity.onCreate()
  - [ ] If HC not available, show HealthConnectUnavailableScreen instead of main UI
  - [ ] Store availability status in ViewModel state
  - [ ] Re-check on MainActivity.onResume() (user may have installed HC)
  - [ ] Navigate to main screen if HC becomes available

- [ ] **Task 6: Add Lifecycle Permission Re-Check** (AC: #5, #6)
  - [ ] Override MainActivity.onResume()
  - [ ] Check Health Connect availability: `healthConnectManager.isHealthConnectAvailable()`
  - [ ] Check permissions: `healthConnectManager.checkPermissions()`
  - [ ] If availability changed (installed/uninstalled), update UI state
  - [ ] If permissions revoked, update UI state (show permission gate on next HC operation)
  - [ ] Log state changes: `Timber.i("HC availability/permissions changed: available=$available, granted=$granted")`

- [ ] **Task 7: Update Permission Flow for HC Not Installed** (AC: #1, #7)
  - [ ] Modify HealthConnectPermissionFlow to check availability first
  - [ ] If HC not available, show HealthConnectUnavailableScreen instead of permission dialog
  - [ ] After Play Store return, re-check availability
  - [ ] If still unavailable, show "Manual installation required" message
  - [ ] Allow user to proceed to permission check once available

- [ ] **Task 8: Error Messaging for HC Operations** (AC: #4)
  - [ ] Update AnalyzeMealWorker error handling
  - [ ] If HC unavailable during save operation, classify as HealthConnectUnavailable error
  - [ ] ErrorHandler.getUserMessage() should return: "Health Connect is required for nutrition tracking"
  - [ ] Show persistent notification with "Install Health Connect" action (links to Play Store)

- [ ] **Task 9: Unit Tests** (AC: All)
  - [ ] Test: `testHealthConnectAvailable_returnsTrue()`
  - [ ] Test: `testHealthConnectNotInstalled_returnsFalse()`
  - [ ] Test: `testPlayStoreIntentCreated()`
  - [ ] Test: `testHealthConnectStatus_SDK_AVAILABLE()`
  - [ ] Test: `testHealthConnectStatus_SDK_UNAVAILABLE()`

- [ ] **Task 10: Integration Tests** (AC: All)
  - [ ] Test: Mock HC unavailable, verify HealthConnectUnavailableScreen shown
  - [ ] Test: Tap "Install", verify Play Store intent fired
  - [ ] Test: Return from Play Store, verify availability re-checked
  - [ ] Test: MainActivity.onResume() re-checks permissions

- [ ] **Task 11: Manual Testing** (AC: All)
  - [ ] Uninstall Health Connect (if possible on test device)
  - [ ] Launch app, verify unavailable screen shown
  - [ ] Tap "Install", verify Play Store opens to Health Connect page
  - [ ] Install Health Connect, return to app, verify main UI appears
  - [ ] Grant permissions, revoke in Settings while app backgrounded
  - [ ] Return to app, verify permission re-check triggers

## Definition of Done

- [ ] All acceptance criteria met with evidence
- [ ] All tasks completed
- [ ] Health Connect availability check implemented
- [ ] Play Store redirect working
- [ ] Lifecycle permission re-check implemented (MainActivity.onResume())
- [ ] Unit tests passing (5+ tests)
- [ ] Integration tests passing (4+ tests)
- [ ] Manual testing verified on device
- [ ] Code reviewed
- [ ] Dev Notes updated with availability check strategy
- [ ] Story status updated to "done"

## Prerequisites

- Story 1.4 (Health Connect integration)
- Story 4.3 (HealthConnectPermissionFlow component)

## Dev Notes

### Implementation Summary

This story adds Health Connect availability detection and installation guidance. The core permission flow (request, denial, retry) was implemented in Story 4.3. This story focuses on handling the case where Health Connect is not installed at all.

**Key Design Decisions:**
- Check availability on app launch AND onResume() (handles install during app lifecycle)
- Play Store intent with fallback to web URL (handles devices without Play Store)
- Graceful degradation if user declines installation (show message, don't block app)
- Reuse HealthConnectPermissionFlow component pattern (consistent UX)

**Integration with Story 4.3:**
- Story 4.3: Permission request flow (assumes HC installed)
- Story 4.5: Availability check + installation flow (prerequisite for permissions)
- Together: Complete Health Connect integration (install → permissions → usage)

### References

- [Health Connect Setup Guide](https://developer.android.com/health-and-fitness/guides/health-connect/develop/get-started)
- [HealthConnectClient.getSdkStatus()](https://developer.android.com/reference/kotlin/androidx/health/connect/client/HealthConnectClient#getSdkStatus(android.content.Context))
- [Story 4.3 Implementation](4-3-api-error-classification-and-handling.md) - Permission flow component
- [Story 1.4 Implementation](1-4-health-connect-integration-setup.md) - Initial HC integration

## Dev Agent Record

<!-- To be filled during implementation -->
