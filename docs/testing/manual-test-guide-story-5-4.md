# Manual Test Guide - Story 5.4: Dark Mode Support

**Story:** Dark Mode Support  
**Date:** 2025-11-23  
**Tester:** BMad / Team Member  
**Device:** [Device model and Android version]  
**Build:** [App version/commit]

## Prerequisites

- Android device or emulator running Android 10+ (dark mode support)
- Foodie app installed (Story 5.4 build)
- Valid Azure OpenAI configuration from Story 5.2 (for end-to-end testing)

---

## Test Scenarios

### Scenario 1: System Theme Following (Default Behavior)

**Objective:** Verify app follows device system dark mode toggle by default (AC-6)

**Steps:**
1. Fresh install of Foodie app (or clear app data)
2. Device Settings → Display → Dark theme: OFF (light mode)
3. Open Foodie app
4. Navigate to Settings → Theme preference
5. Verify "System Default" is selected
6. Navigate to Meal List screen
7. Observe app color scheme (should be light)
8. Device Settings → Display → Dark theme: ON (dark mode)
9. Return to Foodie app (don't close)
10. Observe app color scheme (should switch to dark automatically)

**Expected Results:**
- Fresh install defaults to "System Default" theme (AC-8)
- App displays light colors when system dark mode OFF
- App switches to dark colors when system dark mode ON
- Theme change is smooth with no flicker or crash

**Variations:**
- Repeat test on Android 11, 12, 13, 14 (different OS versions)
- Test with device set to dark mode BEFORE first app launch

---

### Scenario 2: Manual Theme Selection - Light Mode

**Objective:** Verify in-app theme selector forces light mode (AC-7)

**Steps:**
1. Device Settings → Display → Dark theme: ON (system is dark)
2. Open Foodie app
3. Navigate to Settings → Theme preference
4. Select "Light" option (tap radio button)
5. Observe immediate theme change in Settings screen
6. Navigate to Meal List screen
7. Observe colors (should be light despite system being dark)
8. Device Settings → Display → Dark theme: OFF then ON
9. Return to Foodie app
10. Verify app remains in light mode (does not follow system)

**Expected Results:**
- Light mode radio button selected in Settings
- App displays light color palette (white/light backgrounds, dark text)
- Light mode persists even when system is in dark mode
- Theme selection overrides system preference (AC-7)

---

### Scenario 3: Manual Theme Selection - Dark Mode

**Objective:** Verify in-app theme selector forces dark mode (AC-7)

**Steps:**
1. Device Settings → Display → Dark theme: OFF (system is light)
2. Open Foodie app
3. Navigate to Settings → Theme preference
4. Select "Dark" option (tap radio button)
5. Observe immediate theme change in Settings screen
6. Navigate to Meal List screen
7. Observe colors (should be dark despite system being light)
8. Device Settings → Display → Dark theme: ON then OFF
9. Return to Foodie app
10. Verify app remains in dark mode (does not follow system)

**Expected Results:**
- Dark mode radio button selected in Settings
- App displays dark color palette (dark backgrounds, light text)
- Dark mode persists even when system is in light mode
- Theme selection overrides system preference (AC-7)

---

### Scenario 4: Theme Persistence Across App Restarts

**Objective:** Verify theme preference persists after app restart (AC-8)

**Steps:**
1. Open Foodie app
2. Navigate to Settings → Theme preference
3. Select "Dark" option
4. Close app completely (swipe away from recents)
5. Reopen Foodie app
6. Navigate to Settings → Theme preference
7. Verify "Dark" is still selected
8. Verify app displays dark colors
9. Change theme to "Light"
10. Close and reopen app
11. Verify "Light" persists

**Expected Results:**
- Dark theme selection persists after app restart (AC-8)
- Light theme selection persists after app restart (AC-8)
- System Default selection persists after app restart (AC-8)
- No theme flicker on app launch (opens with correct theme immediately)

---

### Scenario 5: All Screens Dark Mode Compatibility

**Objective:** Verify all screens render correctly in dark mode (AC-1, AC-3)

**Steps:**
1. Set app theme to "Dark" in Settings
2. Navigate to Meal List screen
   - Verify dark background color
   - Verify light text on dark backgrounds (meal entry cards)
   - Verify FAB uses dark theme primary color
   - Verify toolbar/navigation bar uses dark theme colors
3. Tap a meal entry to open Meal Detail screen
   - Verify dark background
   - Verify light text for meal name, calories, nutrients
   - Verify edit fields have proper contrast
   - Verify save button uses dark theme primary color
4. Navigate to Settings screen
   - Verify dark background
   - Verify preference items have light text
   - Verify section headers visible
   - Verify API configuration fields have proper contrast
5. Return to Meal List, tap FAB to capture meal
6. On capture screen (if custom), verify dark colors
7. Verify no screens have white backgrounds in dark mode

**Expected Results:**
- All screens use dark theme colors (dark backgrounds, light text) (AC-1)
- Text remains legible with proper contrast in all screens (AC-3)
- No hardcoded white/light colors break dark theme
- Material 3 color scheme applied consistently

---

### Scenario 6: Text Contrast Validation

**Objective:** Verify text meets WCAG AA contrast ratios in both modes (AC-3)

**Tools Required:**
- Android Accessibility Scanner (install from Play Store)
- Or Chrome DevTools for color contrast checking

**Steps:**
1. Install Android Accessibility Scanner
2. Enable Accessibility Scanner (Settings → Accessibility → Accessibility Scanner → ON)
3. Open Foodie app in Light mode (theme = "Light")
4. Scan Meal List screen with Accessibility Scanner
5. Verify zero contrast warnings
6. Scan Settings screen with Accessibility Scanner
7. Verify zero contrast warnings
8. Switch app to Dark mode (theme = "Dark")
9. Scan Meal List screen again
10. Verify zero contrast warnings
11. Scan Settings screen again
12. Verify zero contrast warnings

**Expected Results:**
- Light mode: All text meets 4.5:1 contrast ratio (WCAG AA) (AC-3)
- Dark mode: All text meets 4.5:1 contrast ratio (WCAG AA) (AC-3)
- Accessibility Scanner reports zero contrast issues
- Body text, headers, button text all have sufficient contrast
- OnSurface/OnBackground colors provide proper readability

---

### Scenario 7: Camera Preview Natural Colors

**Objective:** Verify camera preview maintains natural colors in dark mode (AC-4)

**Steps:**
1. Set app theme to "Dark"
2. Navigate to Meal List screen
3. Tap FAB to launch camera capture
4. Point camera at a colorful object (e.g., red apple, green salad)
5. Observe camera preview in real-time
6. Verify colors appear natural (not dark-tinted)
7. Capture photo
8. Verify preview image shows natural colors
9. Repeat test in Light mode for comparison

**Expected Results:**
- Camera preview shows natural colors regardless of app theme (AC-4)
- Dark mode does NOT apply dark filter/tint to camera preview
- System camera intent handles color rendering automatically
- Captured photo colors match real-world colors

**Note:** This test uses system camera intent from Story 2.3, which maintains natural colors automatically. No code changes were needed for AC-4.

---

### Scenario 8: Notification Dark Styling

**Objective:** Verify foreground notification uses dark styling when appropriate (AC-5)

**Steps:**
1. Device Settings → Display → Dark theme: ON
2. Set app theme to "Dark" (or "System Default")
3. Open Foodie app
4. Capture a meal photo (trigger analysis)
5. Pull down notification shade
6. Observe "Analyzing meal..." foreground notification
7. Verify notification background is dark
8. Verify notification text is light (readable on dark background)
9. Device Settings → Display → Dark theme: OFF
10. Capture another meal photo
11. Observe notification in light mode
12. Verify notification background is light

**Expected Results:**
- Notification uses dark styling when system is in dark mode (AC-5)
- Notification uses light styling when system is in light mode
- Text remains readable in both notification styles
- Material 3 notification styling inherits system theme automatically

**Note:** NotificationCompat (Story 2.8) handles dark mode automatically. Android system applies theme to Material-styled notifications.

---

### Scenario 9: Material Design 3 Compliance

**Objective:** Verify dark theme follows Material Design 3 guidelines (AC-2)

**Reference:** https://m3.material.io/styles/color/dark-theme/overview

**Steps:**
1. Set app theme to "Dark"
2. Navigate to Meal List screen
3. Verify background color is near-black (#191C1A or similar)
4. Verify primary color uses lighter tint (green family: #6CDBAC)
5. Verify surface colors have slight elevation contrast
6. Verify error colors are readable (light red on dark)
7. Compare dark palette to Material 3 dark theme guidelines
8. Check that light theme uses darker primary color (#006C4C)

**Expected Results:**
- Dark theme uses Material 3 recommended dark color palette (AC-2)
- Background is true dark (#191C1A) suitable for OLED displays
- Primary color is light tint (#6CDBAC) for dark backgrounds
- Surface elevation uses Material 3 recommended contrast
- Color palette follows green theme (food/health focus)

**Material 3 Checklist:**
- [ ] Dark background (#191C1A)
- [ ] Light primary on dark (#6CDBAC)
- [ ] Surface variant for elevation
- [ ] Error colors readable (#FFB4AB on dark)
- [ ] Contrast ratios meet WCAG AA

---

### Scenario 10: Theme Switching Smoothness

**Objective:** Verify theme changes are smooth with no visual glitches

**Steps:**
1. Open Foodie app in Light mode
2. Navigate to Settings → Theme preference
3. Select "Dark" option
4. Observe transition speed and smoothness
5. Navigate to Meal List screen during/after transition
6. Verify no flicker, white flashes, or visual artifacts
7. Switch theme rapidly: Dark → Light → System Default → Dark
8. Verify app remains stable, no crashes
9. Rotate device while switching themes
10. Verify theme persists across rotation

**Expected Results:**
- Theme changes apply immediately (reactive Compose recomposition)
- No activity recreation (Compose-only approach)
- No visual glitches, flickers, or white flashes during transition
- App remains stable during rapid theme switching
- Theme persists across device rotation

---

## Edge Cases

### Edge Case 1: System Theme Change While App in Background

**Steps:**
1. Open Foodie app with theme set to "System Default"
2. Press home button (app goes to background)
3. Device Settings → Display → Dark theme: Toggle ON/OFF
4. Return to Foodie app (tap app icon or recents)
5. Verify app theme matches new system preference

**Expected:** App updates theme when resumed to match system preference

---

### Edge Case 2: Theme Preference Migration from Old App Version

**Steps:**
1. Uninstall Foodie app
2. Reinstall app (simulates fresh install)
3. Navigate to Settings → Theme preference
4. Verify default is "System Default"
5. No crash, no theme flicker on first launch

**Expected:** Fresh install defaults to "System Default" theme (AC-8)

---

### Edge Case 3: Dark Mode with Dynamic Color (Android 12+)

**Objective:** Verify dynamic color (Material You) works with dark mode

**Requires:** Android 12+ device with wallpaper-based color theming enabled

**Steps:**
1. Device Settings → Wallpaper & style → Enable themed icons/colors
2. Open Foodie app with theme set to "Dark"
3. Verify app uses dynamic dark color scheme (matches wallpaper colors)
4. Change device wallpaper to different color palette
5. Verify app dynamically adjusts dark colors

**Expected:** 
- Android 12+ uses dynamic dark color scheme when available
- Android 11 and below use static dark palette from Color.kt
- Fallback to static palette works correctly

**Note:** Dynamic color feature in Theme.kt (line 47-50) provides this automatically.

---

### Edge Case 4: Theme Preference with Cleared App Data

**Steps:**
1. Set app theme to "Dark" in Settings
2. Device Settings → Apps → Foodie → Storage → Clear data
3. Reopen Foodie app
4. Navigate to Settings → Theme preference
5. Verify theme resets to "System Default"

**Expected:** Cleared data resets theme preference to default (System Default)

---

## Validation Checklist

Complete the following checklist after running all test scenarios:

### Acceptance Criteria Validation
- [ ] **AC-1:** All screens use dark theme colors in dark mode (Scenario 5)
- [ ] **AC-2:** Theme follows Material Design 3 dark theme guidelines (Scenario 9)
- [ ] **AC-3:** Text remains legible with proper contrast ratios (Scenario 6)
- [ ] **AC-4:** Camera preview maintains natural colors (Scenario 7)
- [ ] **AC-5:** Notifications use dark styling when appropriate (Scenario 8)
- [ ] **AC-6:** App respects system dark mode setting (Scenario 1)
- [ ] **AC-7:** In-app theme selector works: System Default, Light, Dark (Scenarios 2, 3)
- [ ] **AC-8:** Theme preference persists across app restarts (Scenario 4)

### Quality Checks
- [ ] No crashes during theme switching (all scenarios)
- [ ] No visual glitches or flickers during theme changes (Scenario 10)
- [ ] Theme changes apply immediately without activity recreation (Scenario 2, 3)
- [ ] All screens tested in both light and dark modes (Scenario 5)
- [ ] Accessibility Scanner reports zero contrast issues (Scenario 6)
- [ ] Edge cases handled gracefully (Edge Cases 1-4)

### Additional Validation
- [ ] Screenshots captured for both light and dark modes (optional, for documentation)
- [ ] Tested on multiple Android versions (10, 11, 12, 13, 14)
- [ ] Tested on both OLED and LCD displays (true black vs backlit dark)
- [ ] Material 3 color palette verified against design guidelines

---

## Test Results Summary

**Date Executed:** [Date]  
**Tester:** [Name]  
**Device(s) Tested:** [List devices and Android versions]  
**Build Version:** [App version/commit hash]

### Scenarios Passed: __ / 10
### Edge Cases Passed: __ / 4
### Overall Result: ✅ PASS / ❌ FAIL

### Issues Found:
1. [Issue description, if any]
2. [Issue description, if any]

### Notes:
[Any additional observations or recommendations]

---

## Appendix: Known Limitations

1. **Dynamic Color (Material You):** Only available on Android 12+. Android 11 and below use static color palette from Color.kt.
2. **Instrumentation Tests:** Project-wide environmental issue prevents UI instrumentation tests. Manual testing is primary validation method.
3. **Notification Dark Mode:** Android system handles notification theming automatically for Material-styled notifications. App does not control notification colors directly.
4. **Camera Colors:** System camera intent (Story 2.3) maintains natural colors automatically. No app-level customization possible.

---

## Sign-Off

**Tester Signature:** ___________________  
**Date:** ___________________  
**Approved for Production:** ☐ YES ☐ NO (specify issues)
