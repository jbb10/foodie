# Manual Testing Guide - Story 2.8

## Setup

### 1. Clear App Data (Fresh Install Test)
```bash
adb shell pm clear com.foodie.app
```

### 2. Start Log Collection
```bash
cd /Users/jbjornsson/source/foodie/app
./collect_test_logs.sh permission_flow
```
Leave this terminal running during your test. Press Ctrl+C when done.

---

## Test Scenarios

### Scenario 1: First Launch Permission Flow ✨
**Goal:** Verify all permissions requested on first app open

**Steps:**
1. Open Foodie app from launcher
2. **Expected:** Health Connect permission dialog appears
3. Grant Health Connect permissions
4. **Expected:** Notification permission dialog appears (Android 13+)
5. Grant notification permission
6. **Expected:** Main screen loads with meal list
7. Close app

**What to verify:**
- ✅ Health Connect permission requested first
- ✅ Notification permission requested second
- ✅ Both permissions granted before reaching main screen
- ✅ No errors in logs

---

### Scenario 2: Widget Flow (Happy Path) 🎯
**Goal:** Verify widget → capture → analysis works with permissions already granted

**Steps:**
1. Add Foodie widget to home screen (if not already)
2. Tap widget
3. **Expected:** Camera launches immediately (no permission prompts)
4. Take photo of food
5. **Expected:** Preview screen with "Use Photo" button
6. Tap "Use Photo"
7. **Expected:** "Analyzing meal..." notification appears
8. **Expected:** Notification auto-dismisses after 15 seconds
9. Open Health Connect or Google Fit
10. **Expected:** Meal entry appears with calories

**What to verify:**
- ✅ No permission prompts during widget flow
- ✅ Notification visible during processing
- ✅ Notification dismisses on success
- ✅ Data saved to Health Connect

---

### Scenario 3: Widget Flow (Permissions Not Granted) 🚨
**Goal:** Verify fallback permission flow when user goes straight to widget

**Steps:**
1. `adb shell pm clear com.foodie.app` (clear app data)
2. Add widget to home screen without opening app
3. Tap widget
4. Take photo
5. Tap "Use Photo"
6. **Expected:** Notification permission prompt appears
7. Grant permission
8. **Expected:** Analysis proceeds with notification

**What to verify:**
- ✅ Permission prompt appears before analysis
- ✅ Analysis continues after granting permission
- ✅ Notification visible and works correctly

---

### Scenario 4: Permission Denial Handling ❌
**Goal:** Verify error messaging when user denies notification permission

**Steps:**
1. `adb shell pm clear com.foodie.app`
2. Tap widget → take photo → tap "Use Photo"
3. **Expected:** Notification permission prompt
4. **Deny** permission
5. **Expected:** Error screen with guidance to enable notifications

**What to verify:**
- ✅ Clear error message shown
- ✅ Guidance directs user to app settings
- ✅ No crash or silent failure

---

## Log Collection

After each test scenario:
1. Press Ctrl+C in terminal to stop log collection
2. Find log file: `test_logs_permission_flow_XXXXXX.txt`
3. Share relevant excerpts or full log file

**Key log tags to look for:**
- `MainActivity` - First launch permission flow
- `CapturePhotoViewModel` - Permission checks before analysis
- `AnalyzeMealWorker` - Background processing
- `MealAnalysisForegroundNotifier` - Notification lifecycle

---

## Success Criteria

### ✅ All Tests Pass If:
1. First launch requests Health Connect + Notification permissions
2. Widget flow works without permission prompts (if first launch completed)
3. Widget flow gracefully handles missing permissions (fallback prompts)
4. Notification appears during analysis and dismisses on success
5. Data saves to Health Connect correctly
6. No crashes or silent failures
7. Logs show clean permission flow

### ⚠️ Known Issues to Watch For:
- Notification not appearing → Check channel registration in logs
- Permission prompt not showing → Check Android version (13+ required for POST_NOTIFICATIONS)
- Analysis fails silently → Check WorkManager logs for errors

---

## Quick Commands

```bash
# Clear app data and start fresh
adb shell pm clear com.foodie.app

# Check if notification permission granted
adb shell dumpsys package com.foodie.app | grep POST_NOTIFICATIONS

# Check if Health Connect permissions granted
adb shell dumpsys package com.foodie.app | grep android.permission.health

# View current notifications
adb shell dumpsys notification | grep com.foodie.app

# Reinstall app
cd /Users/jbjornsson/source/foodie/app && ./gradlew installDebug
```
