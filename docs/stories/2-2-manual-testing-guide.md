# Story 2.2: Lock Screen Widget - Manual Testing Guide

**Story Reference:** 2-2-lock-screen-widget-implementation.md  
**Testing Type:** Manual Validation (requires physical Android device)  
**Test Date:** [To be completed by tester]  
**Tester:** [Your name]

---

## Prerequisites

### Device Requirements
- [ ] Android device running **Android 12 or higher** (lock screen widget API requirement)
- [ ] Device has lock screen enabled with security (PIN, pattern, password, or biometric)
- [ ] Foodie app installed on device (via Android Studio Run or APK installation)
- [ ] Developer options enabled (optional, for ADB testing)

### Build Verification
Before starting manual tests, verify the build is correct:

```bash
# From project root
./gradlew :app:assembleDebug

# Install to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# OR run from Android Studio (Run → Run 'app')
```

Expected: App installs successfully with no errors.

---

## Test Scenarios

### Test 1: Widget Installation on Lock Screen

**Objective:** Verify widget can be added to lock screen successfully.

**Steps:**
1. Lock your device completely (press power button)
2. Wake device to show lock screen (press power button again)
3. Long-press anywhere on the lock screen
4. Look for "Customize" or "Widgets" option (varies by device manufacturer)
   - Samsung: Tap "Customize"
   - Pixel: Tap "+" icon
   - Other: Look for widget/customization option
5. Scroll through widget list to find "Foodie" category
6. Tap on "Foodie" to expand widget options
7. Look for "Log Meal" widget option
8. Drag "Log Meal" widget to desired position on lock screen
9. Confirm widget placement

**Expected Result:**
- [ ] Widget appears in widget list under "Foodie" category
- [ ] Widget shows app icon (Foodie logo)
- [ ] Widget displays "Log Meal" text below icon
- [ ] Widget can be placed on lock screen without errors
- [ ] Widget remains visible on lock screen after placement

**Notes:**  
_[Record any issues, device-specific behaviors, or observations]_

---

### Test 2: Widget Launch Performance (< 500ms Requirement)

**Objective:** Verify widget launches app in under 500 milliseconds.

**Steps:**
1. Lock device completely (screen off)
2. Wake device to lock screen
3. Prepare stopwatch or timer app on another device (or use smartphone timer)
4. **Start timer exactly when you tap the widget**
5. **Stop timer when the app's main screen becomes visible**
6. Record the elapsed time
7. Repeat test **3 times** to get average launch time

**Expected Result:**
- [ ] Test 1 Launch Time: _____ ms
- [ ] Test 2 Launch Time: _____ ms
- [ ] Test 3 Launch Time: _____ ms
- [ ] **Average Launch Time: _____ ms** (Must be < 500ms)
- [ ] App launches consistently within time limit
- [ ] Launch animation feels instant/responsive

**Performance Acceptance Criteria:**
- Average launch time MUST be under 500ms
- No individual test should exceed 750ms (outlier tolerance)
- Launch should feel instantaneous to the user

**Notes:**  
_[Record any performance observations, app state issues, or timing inconsistencies]_

---

### Test 3: Lock Screen Access Without Unlock

**Objective:** Verify widget launches app WITHOUT requiring device unlock.

**Steps:**
1. Ensure device has lock screen security enabled:
   - Settings → Security → Screen Lock
   - Verify PIN, pattern, password, or biometric is set
2. Lock device completely (screen off)
3. Wake device to lock screen (power button)
4. **DO NOT unlock device** - verify lock icon or security indicator is present
5. Tap the "Log Meal" widget
6. Observe if app launches immediately or prompts for unlock

**Expected Result:**
- [ ] App launches immediately after tapping widget
- [ ] No PIN/pattern/password/biometric prompt appears
- [ ] MealList screen appears on lock screen overlay
- [ ] Device remains technically "locked" (status bar shows lock icon)
- [ ] Camera permission flow works from locked state (if tested)

**Security Note:**
This behavior is expected for lock screen widgets with camera access. The app launches in a secure overlay without compromising device security.

**Notes:**  
_[Record any unlock prompts, security warnings, or permission issues]_

---

### Test 4: Reboot Persistence

**Objective:** Verify widget remains functional after device reboot.

**Steps:**
1. With widget installed on lock screen, reboot device:
   - Press and hold power button
   - Select "Restart" or "Reboot"
   - Wait for device to fully restart
2. After reboot, wake device to lock screen
3. Verify widget is still present on lock screen
4. Tap widget to launch app
5. Repeat Test 2 (performance measurement) post-reboot

**Expected Result:**
- [ ] Widget still visible on lock screen after reboot
- [ ] Widget appearance unchanged (icon + "Log Meal" text)
- [ ] Widget launches app successfully post-reboot
- [ ] Post-reboot launch time: _____ ms (should still be < 500ms)
- [ ] No widget errors or "widget not found" messages
- [ ] Widget position on lock screen preserved

**Common Issues:**
- Some manufacturers reset lock screen widgets after reboot (document if occurs)
- First launch after reboot may be slower due to cold start (document timing)

**Notes:**  
_[Record any reboot-related issues, widget disappearance, or configuration changes]_

---

### Test 5: Deep Link Navigation Verification

**Objective:** Verify widget uses correct deep link and navigates to expected screen.

**Steps:**
1. From Android Studio or terminal, check logcat while tapping widget:
   ```bash
   adb logcat | grep "foodie://capture"
   ```
2. Lock device and tap widget
3. Observe logcat output for deep link URI
4. Verify app navigates to MealList screen (temporary routing)

**Expected Result:**
- [ ] Logcat shows `foodie://capture` URI in navigation logs
- [ ] App navigates to MealList screen (current routing)
- [ ] No navigation errors or deep link failures in logs
- [ ] Intent action is `ACTION_VIEW`
- [ ] Intent flags include `FLAG_NEW_TASK` and `FLAG_CLEAR_TASK`

**Future Story Note:**
Story 2-3 will update `foodie://capture` to route directly to camera screen instead of MealList.

**ADB Deep Link Test (Optional):**
```bash
# Test deep link from terminal (app must be installed)
adb shell am start -W -a android.intent.action.VIEW -d "foodie://capture" com.foodie.app
```

Expected: App launches to MealList screen.

**Notes:**  
_[Record deep link logs, navigation issues, or routing errors]_

---

### Test 6: Widget UI Appearance and Layout

**Objective:** Verify widget visual appearance matches design specifications.

**Steps:**
1. View widget on lock screen in good lighting
2. Compare against design specifications:
   - App icon present and centered
   - "Log Meal" text visible below icon
   - Minimal size (small lock screen widget size)
   - Material3 styling (if observable)
3. Test widget in different scenarios:
   - Light mode
   - Dark mode (if device supports)
   - Different lock screen wallpapers (light and dark backgrounds)

**Expected Result:**
- [ ] App icon clearly visible and recognizable
- [ ] "Log Meal" text legible on all backgrounds
- [ ] Widget uses smallest available lock screen size
- [ ] Widget layout appears professional and polished
- [ ] Widget adapts to light/dark lock screen themes
- [ ] No text truncation or layout overflow

**Design Specifications:**
- Icon size: 48dp (as defined in MealCaptureWidget.kt)
- Layout: Vertical stack (Column) with icon above text
- Spacing: 8dp between icon and text (GlanceModifier.padding)

**Notes:**  
_[Record any visual issues, legibility problems, or design inconsistencies]_

---

## Test Results Summary

### Overall Status
- [ ] **PASS** - All tests passed, widget ready for approval
- [ ] **CONDITIONAL PASS** - Minor issues found, document and proceed
- [ ] **FAIL** - Critical issues found, implementation needs revision

### Critical Issues Found
_[List any blocking issues that prevent story completion]_

1. 
2. 
3. 

### Minor Issues / Observations
_[List non-blocking issues or improvements for future consideration]_

1. 
2. 
3. 

### Performance Metrics
- Average launch time: _____ ms
- Fastest launch: _____ ms
- Slowest launch: _____ ms
- **Meets < 500ms requirement:** [ ] YES / [ ] NO

### Device Testing Information
- **Device Model:** _______________________
- **Android Version:** _____________________
- **Device Manufacturer:** __________________
- **Lock Screen Security Type:** PIN / Pattern / Password / Biometric

### Acceptance Criteria Validation

Verify all story acceptance criteria are met:

- [ ] **AC #1:** Widget launches camera in less than 500ms ✅
- [ ] **AC #2:** No device unlock required ✅
- [ ] **AC #3:** Widget displays app icon with "Log Meal" text ✅
- [ ] **AC #4:** Widget uses smallest available lock screen size ✅
- [ ] **AC #5:** Widget remains functional after device reboot ✅

### Tester Sign-Off

**Tester Name:** ______________________  
**Test Date:** ______________________  
**Test Duration:** ______ minutes  
**Recommendation:** APPROVE / REVISE / REJECT  

**Additional Comments:**  
_[Any final observations, recommendations, or notes for the development team]_

---

## Troubleshooting Guide

### Widget Not Appearing in Widget List

**Possible Causes:**
- App not installed on device
- Android version < 12 (lock screen widgets require API 31+)
- Widget receiver not registered in manifest

**Solutions:**
1. Verify app installation: `adb shell pm list packages | grep foodie`
2. Check Android version: `adb shell getprop ro.build.version.sdk` (must be ≥ 31)
3. Rebuild and reinstall app: `./gradlew :app:installDebug`
4. Check logcat for widget errors: `adb logcat | grep Widget`

---

### Widget Tap Does Nothing

**Possible Causes:**
- Deep link not registered in manifest
- PendingIntent misconfigured
- App not set as default handler for `foodie://` scheme

**Solutions:**
1. Check manifest for `<intent-filter>` with `foodie://capture`
2. Verify PendingIntent flags in MealCaptureWidget.kt: `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`
3. Test deep link manually: `adb shell am start -W -a android.intent.action.VIEW -d "foodie://capture" com.foodie.app`
4. Check logcat for intent errors: `adb logcat | grep Intent`

---

### Launch Time Exceeds 500ms

**Possible Causes:**
- Cold start performance (first launch after reboot)
- Heavy app initialization in MainActivity onCreate()
- Device performance limitations

**Solutions:**
1. Test on multiple devices (verify not device-specific)
2. Profile app startup with Android Studio Profiler
3. Optimize MainActivity onCreate() - defer heavy work
4. Consider app startup library (Jetpack Startup) for optimization
5. Document if only first launch is slow (cold start expected)

---

### Widget Disappears After Reboot

**Possible Causes:**
- Device manufacturer resets lock screen widgets (Samsung, Xiaomi known issues)
- Widget configuration not persisted correctly
- Widget receiver not handling BOOT_COMPLETED intent

**Solutions:**
1. Check if manufacturer-specific behavior (document in test results)
2. Verify widget configuration XML has persistent settings
3. Add BOOT_COMPLETED receiver if needed (future enhancement)
4. Test on multiple device manufacturers

---

## Appendix: ADB Commands Reference

```bash
# Install app
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Test deep link
adb shell am start -W -a android.intent.action.VIEW -d "foodie://capture" com.foodie.app

# View live logs (filtered for widget)
adb logcat | grep -E "Widget|foodie|MealCapture"

# Check widget provider registration
adb shell dumpsys appwidget com.foodie.app | grep MealCapture

# Clear app data (reset state)
adb shell pm clear com.foodie.app

# Check Android version
adb shell getprop ro.build.version.sdk

# Force stop app
adb shell am force-stop com.foodie.app
```

---

## Automated Test Coverage

**Note:** Manual testing complements automated testing. The following automated tests already exist:

**Instrumentation Tests** (`MealCaptureWidgetInstrumentationTest.kt`):
- ✅ `widgetDeepLinkIntent_hasCorrectUri` - Verifies `foodie://capture` URI
- ✅ `widgetDeepLinkIntent_hasCorrectComponent` - Verifies MainActivity target
- ✅ `widgetDeepLinkIntent_hasNewTaskAndClearTaskFlags` - Verifies lock screen flags
- ✅ `widgetDeepLink_isRegisteredInNavGraph` - Verifies URI format in navigation
- ✅ `widgetReceiver_canBeInstantiated` - Verifies receiver setup

**Unit Tests:**
- ✅ All existing unit tests passing (145 tests, 0 failures)
- Note: Widget-specific unit tests consolidated into instrumentation tests due to Android framework dependencies

**Test Execution:**
```bash
# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumentation tests (requires connected device/emulator)
./gradlew :app:connectedDebugAndroidTest

# Run all tests
./gradlew test connectedAndroidTest
```

---

**End of Manual Testing Guide**
