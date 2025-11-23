# Manual Test Guide - Story 5.6: Performance Optimization and Polish

**Story**: 5-6-performance-optimization-and-polish  
**Date Created**: 2025-11-23  
**Device Requirements**: Mid-range Android device (Samsung Galaxy A53, Pixel 6a, or equivalent)  
**Tools Required**: Android Studio, ADB, GPU Rendering Profile, Android Profiler, LeakCanary (auto-installed in debug builds)

---

## Overview

This manual test guide validates the performance optimizations and polish implemented in Story 5.6. Testing focuses on objective measurements using Android performance profiling tools rather than subjective assessments.

### Acceptance Criteria Validation Map

| Scenario | AC # | Acceptance Criterion |
|----------|------|---------------------|
| 1 | #5 | Cold app launch takes < 2 seconds |
| 2 | #1 | All screen transitions are smooth (60fps) |
| 3 | #2 | List view scrolling has no jank or stuttering |
| 4 | #4 | Memory usage stays within reasonable limits (< 100MB typical) |
| 5 | #6 | Battery impact is minimal (efficient background processing) |
| 6 | #7 | APK size is optimized (< 10MB for MVP) |
| 7 | #1, #9 | Smooth animations enhance the capture flow (fade-ins, slide transitions) |
| 8 | #8 | No memory leaks in ViewModels or Activities |

---

## Prerequisites

### 1. Enable Developer Options on Device
1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times to enable Developer Options
3. Go to **Settings → System → Developer Options**

### 2. Enable GPU Rendering Profile
1. In Developer Options, find **Monitoring** section
2. Enable **GPU Rendering Profile** → **On screen as bars**
3. Green bars below 16ms line = 60fps (good)
4. Red bars above 16ms line = dropped frames (bad)

### 3. Install Foodie Debug Build
```bash
cd /Users/jbjornsson/source/foodie/app
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Ensure Test Data Exists
- Have at least 50+ meal entries in Health Connect for scrolling test
- If needed, use the widget to capture multiple meals

---

## Scenario 1: Cold App Launch Timing (AC #5)

**Objective**: Verify cold launch completes in ≤ 2000ms (2 seconds)

### Test Steps

1. **Force-stop the app** to ensure cold start:
   ```bash
   adb shell am force-stop com.foodie.app
   ```

2. **Wait 2 seconds** for process to fully terminate:
   ```bash
   sleep 2
   ```

3. **Measure launch time** using ADB:
   ```bash
   adb shell am start -W -n com.foodie.app/.MainActivity
   ```

4. **Look for TotalTime value** in output (milliseconds):
   ```
   ThisTime: 450
   TotalTime: 505
   WaitTime: 510
   ```

5. **Repeat 3 times** to get average (cold starts vary):
   ```bash
   for i in 1 2 3; do
     adb shell am force-stop com.foodie.app
     sleep 2
     adb shell am start -W -n com.foodie.app/.MainActivity 2>&1 | grep "TotalTime"
   done
   ```

### Expected Results
- **TotalTime**: ≤ 2000ms (2 seconds) on mid-range device
- **Average**: Calculate (run1 + run2 + run3) / 3
- **Pass Criteria**: Average TotalTime ≤ 2000ms

### Validation Checklist
- [ ] TotalTime Run 1: ______ ms
- [ ] TotalTime Run 2: ______ ms
- [ ] TotalTime Run 3: ______ ms
- [ ] Average TotalTime: ______ ms (≤ 2000ms)
- [ ] AC #5 PASSED: Cold launch < 2 seconds

---

## Scenario 2: Screen Transition Smoothness (AC #1)

**Objective**: Verify all screen transitions render at 60fps (green bars < 16ms)

### Test Steps

1. **Enable GPU Rendering Profile** (see Prerequisites)
2. **Launch Foodie app** on device
3. **Observe green/red bars** at bottom of screen during navigation

4. **Test MealList → Settings transition**:
   - From MealListScreen, tap Settings icon (top-right)
   - Watch GPU bars during slide transition
   - Green bars should stay **below 16ms line** (60fps)

5. **Test Settings → MealList transition**:
   - From SettingsScreen, tap back button
   - Watch GPU bars during slide transition
   - Green bars should stay **below 16ms line**

6. **Test MealList → MealDetail transition**:
   - From MealListScreen, tap any meal entry
   - Watch GPU bars during slide transition
   - Green bars should stay **below 16ms line**

7. **Test MealDetail → MealList transition**:
   - From MealDetailScreen, tap back button
   - Watch GPU bars during slide transition
   - Green bars should stay **below 16ms line**

### Expected Results
- **All transitions**: Green bars consistently below 16ms line
- **No red spikes**: No bars extending above 16ms line during transitions
- **Smooth animation**: Visually smooth slide animations (no stuttering)

### Validation Checklist
- [ ] MealList → Settings: Green bars < 16ms
- [ ] Settings → MealList: Green bars < 16ms
- [ ] MealList → MealDetail: Green bars < 16ms
- [ ] MealDetail → MealList: Green bars < 16ms
- [ ] No red frame spikes observed
- [ ] AC #1 PASSED: Screen transitions smooth (60fps)

---

## Scenario 3: List Scrolling Performance (AC #2)

**Objective**: Verify MealListScreen scrolls smoothly with 50+ entries (no jank)

### Prerequisites
- Ensure Health Connect has **50+ meal entries**
- If not, capture meals using widget until count met

### Test Steps

1. **Launch Foodie app** and navigate to MealListScreen
2. **Verify meal count**: Should see 50+ entries grouped by date
3. **Enable GPU Rendering Profile** (see Prerequisites)

4. **Fast fling scroll test**:
   - Swipe up quickly from bottom to top (fast fling)
   - Observe GPU bars during scroll
   - Green bars should stay **below 16ms line**

5. **Slow deliberate scroll test**:
   - Slowly scroll down through all entries
   - Observe GPU bars during scroll
   - Green bars should stay **below 16ms line**

6. **Reverse scroll test**:
   - Scroll up from bottom to top slowly
   - Observe GPU bars during scroll
   - Green bars should stay **below 16ms line**

7. **Visual observation**:
   - No stuttering or jank visible
   - Meal cards render smoothly
   - Date headers stay visible during scroll

### Expected Results
- **GPU bars**: Consistently green (< 16ms) during all scroll types
- **No jank**: Smooth scrolling motion, instant response to touch
- **No layout shifts**: Date headers and cards render without jumping

### Validation Checklist
- [ ] Fast fling scroll: Green bars < 16ms
- [ ] Slow scroll down: Green bars < 16ms
- [ ] Slow scroll up: Green bars < 16ms
- [ ] No visible stuttering or jank
- [ ] Instant response to touch input
- [ ] AC #2 PASSED: List scrolling smooth with 50+ entries

---

## Scenario 4: Memory Usage Monitoring (AC #4)

**Objective**: Verify memory stays < 100MB typical, < 150MB peak

### Test Steps

1. **Open Android Profiler in Android Studio**:
   - View → Tool Windows → Profiler
   - Click "+" to start new session
   - Select "Foodie" app process

2. **Switch to Memory tab** in Profiler

3. **Perform typical usage** (5-10 minutes):
   - **Launch app** (observe initial memory allocation)
   - **Browse meal list** (scroll through 50+ entries)
   - **Navigate to Settings** and back
   - **Tap meal entry** to view MealDetailScreen
   - **Edit meal** (change description, calories)
   - **Save changes** and return to list
   - **Delete a meal** (long-press → confirm delete)
   - **Pull-to-refresh** meal list
   - **Capture 2-3 new meals** (widget → camera → confirm)
   - **Wait for WorkManager** to process captures

4. **Observe memory graph**:
   - Blue line = Total memory allocation
   - Look for typical range (should be < 100MB)
   - Look for peak during WorkManager processing (should be < 150MB)
   - Check for continuous upward trend (indicates memory leak)

5. **Force garbage collection** to verify cleanup:
   - Click "Initiate garbage collection" button in Profiler
   - Memory should drop significantly
   - Steady-state after GC should be < 100MB

### Expected Results
- **Typical usage**: Memory < 100MB during browsing/editing
- **Peak during WorkManager**: Memory < 150MB during meal analysis
- **No upward trend**: Memory stabilizes, no continuous growth
- **GC cleanup**: Memory drops significantly after forcing GC

### Validation Checklist
- [ ] Typical memory during browsing: ______ MB (< 100MB)
- [ ] Peak memory during WorkManager: ______ MB (< 150MB)
- [ ] No continuous upward trend observed
- [ ] Memory drops after GC to ______ MB
- [ ] AC #4 PASSED: Memory usage within limits

---

## Scenario 5: Battery Impact Testing (AC #6)

**Objective**: Validate negligible battery impact for typical usage (3-5 captures/day)

### Test Steps (24-hour test)

1. **Fully charge device** to 100%
2. **Enable Battery Historian collection**:
   ```bash
   adb shell dumpsys batterystats --enable full-wake-history
   adb shell dumpsys batterystats --reset
   ```

3. **Use Foodie normally over 24 hours**:
   - Capture 3-5 meals using widget
   - Edit 2 meal entries
   - Delete 1 entry
   - Browse meal list occasionally
   - Do NOT use app excessively (normal usage)

4. **After 24 hours, collect bugreport**:
   ```bash
   adb bugreport > bugreport.zip
   ```

5. **Upload to Battery Historian**:
   - Go to https://bathist.ef.lc/
   - Upload `bugreport.zip`
   - Wait for analysis to complete

6. **Analyze Foodie battery consumption**:
   - Find "Foodie" in app list
   - Check percentage of total battery usage
   - Look for wake locks (should be minimal)
   - Check network usage during meal analysis

### Expected Results
- **Battery impact**: < 2% over 24 hours with 3-5 captures
- **Wake locks**: Minimal (only during WorkManager execution)
- **Network usage**: Spikes during meal analysis only (not continuous polling)
- **Background**: No background activity when app idle

### Validation Checklist
- [ ] 24-hour battery usage: ______ % (< 2%)
- [ ] Wake locks: Minimal (only during WorkManager)
- [ ] Network: Only during meal analysis
- [ ] No continuous background activity
- [ ] AC #6 PASSED: Battery impact minimal

---

## Scenario 6: APK Size Verification (AC #7)

**Objective**: Verify release APK size < 10MB

### Test Steps

1. **Build release APK** with ProGuard and resource shrinking:
   ```bash
   cd /Users/jbjornsson/source/foodie/app
   ./gradlew :app:assembleRelease
   ```

2. **Check APK file size**:
   ```bash
   ls -lh app/build/outputs/apk/release/app-release-unsigned.apk
   ```

3. **Expected output** (example):
   ```
   -rw-r--r--  1 user  staff   5.5M Nov 23 17:27 app-release-unsigned.apk
   ```

4. **Analyze APK contents** (optional):
   - In Android Studio: Build → Analyze APK
   - Select `app-release-unsigned.apk`
   - Review largest files/resources
   - Verify resources were shrunk
   - Check code was minified (classes.dex should be optimized)

### Expected Results
- **APK size**: < 10MB (current: ~5.5MB)
- **Resources**: Shrunk (unused resources removed)
- **Code**: Minified (ProGuard/R8 applied)

### Validation Checklist
- [ ] Release APK size: ______ MB (< 10MB)
- [ ] ProGuard enabled: `isMinifyEnabled = true` ✓
- [ ] Resource shrinking enabled: `isShrinkResources = true` ✓
- [ ] APK Analyzer shows optimized classes.dex
- [ ] AC #7 PASSED: APK size < 10MB

---

## Scenario 7: Animation Polish Validation (AC #1, #9)

**Objective**: Verify smooth animations enhance user experience

### Test Steps

1. **Test meal capture fade-in animation**:
   - Tap widget to capture meal
   - Take photo with camera
   - Confirm/save photo
   - Wait for meal to appear in list
   - **Expected**: Meal entry fades in smoothly when added to list

2. **Test Settings slide transition**:
   - From MealListScreen, tap Settings icon
   - **Expected**: Settings screen slides in from right (300ms, smooth easing)
   - Tap back button
   - **Expected**: Settings screen slides out to right (250ms, smooth easing)

3. **Test MealDetail slide transition**:
   - From MealListScreen, tap any meal entry
   - **Expected**: MealDetail screen slides in from right (300ms, smooth easing)
   - Tap back button
   - **Expected**: MealDetail screen slides out to right (250ms, smooth easing)

4. **Test theme switching animation** (from Story 5.4):
   - Go to Settings → Appearance → Dark mode
   - Toggle dark mode on/off
   - **Expected**: Smooth activity recreation with crossfade (no harsh blink)

### Expected Results
- **Fade-in**: Meal entry appears smoothly after capture (not instant pop-in)
- **Slide transitions**: Smooth Material 3 SharedAxis X-axis transitions
- **Theme switch**: Smooth crossfade during activity recreation
- **No jank**: All animations render at 60fps (green GPU bars)

### Validation Checklist
- [ ] Meal capture fade-in animation smooth
- [ ] Settings slide-in transition smooth (300ms)
- [ ] Settings slide-out transition smooth (250ms)
- [ ] MealDetail slide-in transition smooth
- [ ] MealDetail slide-out transition smooth
- [ ] Theme switch crossfade smooth (no harsh blink)
- [ ] All animations render at 60fps (green GPU bars)
- [ ] AC #9 PASSED: Animations enhance experience

---

## Scenario 8: LeakCanary Memory Leak Detection (AC #8)

**Objective**: Verify no memory leaks in ViewModels or Activities

### Prerequisites
- LeakCanary is auto-installed in debug builds (Story 5.6)
- LeakCanary notification shows on device when leak detected

### Test Steps

1. **Install debug build** (LeakCanary included):
   ```bash
   cd /Users/jbjornsson/source/foodie/app
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Launch Foodie app** on device

3. **Perform complete user flow** (exercise all screens/ViewModels):
   - Browse meal list
   - Navigate to Settings → change theme → back to list
   - Tap meal entry → edit description → save → back to list
   - Delete a meal (long-press → confirm)
   - Pull-to-refresh meal list
   - Capture meal with widget → camera → confirm
   - Wait for WorkManager to process
   - Navigate back and forth between screens multiple times
   - Rotate device (if applicable) to test configuration changes

4. **Force app to background** (trigger leak detection):
   - Press home button
   - Wait 5 seconds
   - Check for LeakCanary notification

5. **Return to app and background again**:
   - Open app
   - Use app briefly
   - Press home button
   - Wait 5 seconds

6. **Check for leak notifications**:
   - Swipe down notification shade
   - Look for "LeakCanary" notifications
   - If notification exists, tap to view leak trace

### Expected Results
- **No leak notifications**: LeakCanary should NOT show any notifications
- **ViewModels cleared**: All ViewModels properly scope coroutines and clear observers
- **Activities not leaked**: No Activity references held after destruction

### Validation Checklist
- [ ] Complete user flow performed (all screens exercised)
- [ ] App backgrounded multiple times (leak detection triggered)
- [ ] No LeakCanary notifications observed
- [ ] No memory leaks in ViewModels
- [ ] No memory leaks in Activities
- [ ] AC #8 PASSED: No memory leaks detected

---

## Tool Usage Instructions

### Android Profiler Setup

1. **Open Android Studio**
2. **View → Tool Windows → Profiler**
3. **Click "+" to start new profiling session**
4. **Select device** and choose "Foodie" process
5. **Tabs available**:
   - CPU: Analyze method traces and thread activity
   - Memory: Monitor heap allocations and GC events
   - Network: Track network requests and data transfer
   - Energy: Battery usage profiling

### GPU Rendering Profile Interpretation

- **Green bars**: Frame rendered in < 16ms (60fps) ✅ Good
- **Yellow bars**: Frame rendered in 16-32ms (30-60fps) ⚠️ Marginal
- **Red bars**: Frame rendered in > 32ms (< 30fps) ❌ Bad (dropped frames)

**16ms line** is the 60fps threshold:
- 1 frame at 60fps = 16.67ms
- Consistently below 16ms line = smooth 60fps animation

### Battery Historian Analysis

1. **Collect bugreport after 24 hours**:
   ```bash
   adb bugreport > bugreport.zip
   ```

2. **Upload to https://bathist.ef.lc/**

3. **Key metrics to check**:
   - **Battery drain**: Total % consumed by Foodie
   - **Wake locks**: Should be minimal (only during WorkManager)
   - **Network**: Should spike only during meal analysis
   - **Screen on/off**: Most battery use should be during screen-on

### APK Analyzer Usage

1. **In Android Studio**: Build → Analyze APK
2. **Select APK file**: `app/build/outputs/apk/release/app-release-unsigned.apk`
3. **Review contents**:
   - **classes.dex**: Should be minified/optimized
   - **resources.arsc**: Resource table (check size)
   - **res/**: Drawable and layout resources (should be shrunk)
   - **lib/**: Native libraries (if any)
4. **Compare raw vs download size** (Google Play compression)

---

## Baseline vs Optimized Metrics Comparison

| Metric | Baseline (Pre-Story 5.6) | Optimized (Post-Story 5.6) | Target | Pass/Fail |
|--------|--------------------------|----------------------------|--------|-----------|
| Cold launch time | ~420ms | ______ ms | ≤ 2000ms | [ ] |
| Screen transitions (fps) | ______ | ______ | 60fps (< 16ms) | [ ] |
| List scrolling (fps) | ______ | ______ | 60fps (< 16ms) | [ ] |
| Memory typical | ______ MB | ______ MB | < 100MB | [ ] |
| Memory peak | ______ MB | ______ MB | < 150MB | [ ] |
| Release APK size | 5.8 MB | ______ MB | < 10MB | [ ] |
| Battery (24hr, 5 captures) | ______ % | ______ % | < 2% | [ ] |
| Memory leaks | ______ | 0 | 0 | [ ] |

---

## Overall Acceptance Criteria Checklist

- [ ] AC #1: All screen transitions are smooth (60fps) ✅
- [ ] AC #2: List view scrolling has no jank or stuttering ✅
- [ ] AC #3: Image loading uses efficient caching and compression ✅ (verified in Story 2.3)
- [ ] AC #4: Memory usage stays within reasonable limits (< 100MB typical) ✅
- [ ] AC #5: Cold app launch takes < 2 seconds ✅
- [ ] AC #6: Battery impact is minimal (efficient background processing) ✅
- [ ] AC #7: APK size is optimized (< 10MB for MVP) ✅
- [ ] AC #8: No memory leaks in ViewModels or Activities ✅
- [ ] AC #9: Smooth animations enhance the capture flow ✅

---

## Testing Notes

### Device Recommendation
- **Recommended**: Samsung Galaxy A53, Pixel 6a (mid-range)
- **Avoid**: Flagship devices (Pixel 8 Pro, Samsung S24) - they mask performance issues
- **Reason**: Mid-range devices expose bottlenecks that flagship devices hide

### Testing Environment
- **Network**: Test on both WiFi and mobile data (battery impact)
- **Battery**: Start with 100% charge for battery testing
- **Storage**: Ensure device has > 1GB free space
- **Health Connect**: Install from Play Store if not present

### Known Limitations
- **Baseline Profiles**: Not implemented in Story 5.6 (future optimization opportunity)
- **Macrobenchmark**: Not implemented (future systematic performance testing)
- **Mid-range device**: Testing done on Pixel 8 Pro (flagship) - re-test on mid-range recommended

---

## Test Execution Record

**Tester**: ______________________  
**Date**: ______________________  
**Device**: ______________________ (Model, Android Version)  
**Build**: ______________________  

**Overall Result**: [ ] PASS [ ] FAIL

**Notes**:
_______________________________________________________________
_______________________________________________________________
_______________________________________________________________
