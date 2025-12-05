# Manual Test Guide: Story 5.5 - Accessibility Improvements

**Story:** 5.5 Accessibility Improvements  
**Test Date:** 2025-11-23  
**Tester:** _(To be filled during testing)_  
**Device:** _(To be filled during testing - recommend Pixel 8 Pro Android 14+)_  
**Build:** _(To be filled during testing)_

## Overview

This manual test guide validates accessibility improvements including TalkBack support, WCAG AA compliance, touch target sizing, text scaling, and keyboard navigation.

**Test Scope:**
- TalkBack screen reader navigation across all screens
- Large font size scaling (Settings → Display → Font Size → Largest)
- Touch target validation with Layout Inspector / Accessibility Scanner
- Colour contrast visual inspection
- D-pad/keyboard navigation (if device supports)

**Prerequisites:**
- Android device with TalkBack support
- Android Accessibility Scanner installed: https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor
- Layout Inspector (optional, for touch target measurement)

---

## Test Scenarios

### Scenario 1: TalkBack Navigation - Meal List Screen

**Objective:** Verify all interactive elements announce correctly with TalkBack

**Steps:**
1. Enable TalkBack: Settings → Accessibility → TalkBack → Toggle ON
2. Launch Foodie app
3. Navigate MealListScreen with swipe gestures:
   - Swipe right to advance to next element
   - Swipe left to go back to previous element
   - Double-tap to activate focused element

**Expected TalkBack Announcements:**
- **App Title:** "Foodie" (announcement only, not interactive)
- **Settings IconButton:** "Open settings, button"
- **Meal Entry Card (example):** "Meal entry, 650 calories, Grilled chicken with rice, Today at 12:30 PM, button"
  - Should merge description, calories, timestamp into single announcement
  - Should announce as "button" (clickable)
- **Empty State (if no meals):** "No meals logged yet. Take a photo to get started"

**Validation:**
- [ ] Settings button announces "Open settings, button"
- [ ] Each meal card announces as single merged description with calories, description, timestamp
- [ ] All elements are reachable via swipe gestures
- [ ] Double-tap activates meal cards (opens detail screen)
- [ ] No silent/unlabeled elements

**Notes:** _____________________________________________________

---

### Scenario 2: TalkBack Navigation - Settings Screen

**Objective:** Verify Settings preferences announce current values

**Steps:**
1. With TalkBack enabled, tap Settings button on MealListScreen
2. Navigate Settings screen with swipe gestures
3. Listen to announcements for each preference item

**Expected TalkBack Announcements:**
- **Back Button:** "Navigate up, button" (Material 3 default)
- **Settings Title:** "Settings"
- **Category Headers:** "API Configuration" (announcement only)
- **API Key Field:** "API Key, configured with ending 1234" (or "not configured")
- **Endpoint Field:** "Azure OpenAI Endpoint, current value: https://..." (or "not configured")
- **Model Field:** "Model Deployment Name, current value: gpt-4.1" (or "not configured")
- **Save Configuration Button:** "Save Configuration, button"
- **Test Connection Button:** "Test Connection, button"
- **Theme Radio Buttons:** "System Default, selected" / "Light, not selected" / "Dark, not selected"
- **Accessibility Preference:** "Accessibility, Configure in Story 5.5"
- **Version Preference:** "Version, 1.0.0 (MVP)"

**Validation:**
- [ ] Back button is first focusable element
- [ ] API Key announces configuration state (configured/not configured)
- [ ] Endpoint and Model announce current values
- [ ] Theme radio buttons announce selection state
- [ ] All preference items are reachable and announce correctly
- [ ] No silent/unlabeled elements

**Notes:** _____________________________________________________

---

### Scenario 3: TalkBack Navigation - Camera Capture Flow

**Objective:** Verify camera button is discoverable with TalkBack

**Steps:**
1. With TalkBack enabled, navigate to camera capture
   - From widget: Launch widget (deep link to camera)
   - Expected: System camera app launches (uses ACTION_IMAGE_CAPTURE intent)
2. System camera app handles TalkBack announcements

**Expected Behaviour:**
- **Widget Tap:** Launches system camera (TalkBack announces "Camera" or app-specific label)
- **Camera Controls:** System camera app provides TalkBack support (shutter button, mode switcher, etc.)
- **Note:** Custom camera implementation not used - system camera handles accessibility

**Validation:**
- [ ] Widget tap is discoverable with TalkBack
- [ ] System camera launches and TalkBack continues working
- [ ] Camera shutter button announced by system camera app
- [ ] Photo capture flow completes successfully with TalkBack enabled

**Notes:** _____________________________________________________

---

### Scenario 4: Large Font Size Testing

**Objective:** Verify text scales correctly with Largest system font

**Steps:**
1. Disable TalkBack (Settings → Accessibility → TalkBack → OFF)
2. Set font size to Largest: Settings → Display → Font Size → Largest
3. Return to Foodie app and inspect all screens

**Expected Behaviour - MealListScreen:**
- All text increases in size proportionally
- Meal descriptions wrap to multiple lines (no truncation)
- Meal timestamps and calories scale correctly
- Empty state text scales and remains centreed
- No text clipping or overflow

**Expected Behaviour - SettingsScreen:**
- Preference titles scale correctly
- API Key/Endpoint/Model input fields scale (text and labels)
- Theme radio button labels scale
- No text clipping in OutlinedTextFields
- Buttons expand vertically to accommodate larger text

**Expected Behaviour - General:**
- All Material 3 typography scales (headlineMedium, bodyLarge, labelSmall, etc.)
- No hardcoded fontSize values override scaling
- UI remains usable (no overlapping elements)

**Validation:**
- [ ] MealListScreen: Text scales, no clipping/truncation
- [ ] Meal descriptions wrap to multiple lines correctly
- [ ] SettingsScreen: All preference titles and values scale
- [ ] Input fields (API Key, Endpoint, Model) display large text correctly
- [ ] Buttons expand to fit larger text
- [ ] No UI layout breakage at Largest font size

**Notes:** _____________________________________________________

---

### Scenario 5: Touch Target Validation with Accessibility Scanner

**Objective:** Verify all touch targets meet 48dp minimum (WCAG compliance)

**Steps:**
1. Install Android Accessibility Scanner from Play Store
2. Enable scanner: Settings → Accessibility → Accessibility Scanner → Toggle ON
3. Navigate to Foodie app MealListScreen
4. Tap Accessibility Scanner FAB (floating button on screen edge)
5. Review scan report for touch target violations
6. Repeat for SettingsScreen

**Expected Results:**
- **IconButton (Settings):** 48dp x 48dp (Material 3 default) ✅
- **MealEntryCard:** Full-width, height > 48dp (3 text lines + padding) ✅
- **Buttons (Save, Test):** Height ≥ 48dp (Material 3 Button default) ✅
- **Theme Radio Buttons:** Row with RadioButton + Text ≥ 48dp touch area ✅
- **Spacing:** 8dp minimum between adjacent items ✅

**Scanner Report Expected:**
- **Touch Target Violations:** 0 HIGH priority issues
- **Acceptable:** 0-2 MEDIUM priority issues (if any edge cases)
- **Colour Contrast:** 0 violations (Material 3 palette WCAG AA compliant)
- **Content Descriptions:** 0 violations (all added in implementation)

**Validation:**
- [ ] Accessibility Scanner reports 0 CRITICAL issues
- [ ] Accessibility Scanner reports 0 HIGH severity touch target violations
- [ ] No colour contrast violations reported
- [ ] No missing content description violations
- [ ] Screenshot/note any MEDIUM or LOW issues for future work

**Notes:** _____________________________________________________

---

### Scenario 6: Colour Contrast Visual Inspection

**Objective:** Verify colour contrast meets WCAG AA in both light and dark modes

**Steps:**
1. Set theme to Light mode: Settings → Appearance → Theme → Light
2. Visually inspect text readability on all screens
3. Set theme to Dark mode: Settings → Appearance → Theme → Dark
4. Visually inspect text readability on all screens

**Expected - Light Mode:**
- Black text (#191C1A) on light background (#FBFDF9): Highly readable ✅ (16.5:1)
- Green primary (#006C4C) on light background: Clear and distinct ✅ (7.8:1)
- Error red (#BA1A1A) on light background: Readable for warnings ✅
- No low-contrast gray text that's hard to read

**Expected - Dark Mode:**
- Light text (#E1E3DF) on dark background (#191C1A): Highly readable ✅ (12.8:1)
- Green primary (#6CDBAC) on dark background: Clear and distinct ✅ (8.2:1)
- Error pink (#FFB4AB) on dark background: Readable for warnings ✅
- No eye-straining bright white text

**Expected - Non-Colour Information:**
- Error states use icon + red colour (not colour alone)
- Success states use checkmark icon + green
- Theme selection uses radio button + text label

**Validation:**
- [ ] Light mode: All text clearly readable, no low-contrast issues
- [ ] Dark mode: All text clearly readable, no eye strain
- [ ] Primary colours distinct in both modes
- [ ] Error/success states convey information via icons + colour
- [ ] No information conveyed by colour alone

**Notes:** _____________________________________________________

---

### Scenario 7: Keyboard/D-Pad Navigation (Optional)

**Objective:** Verify logical focus order with keyboard/D-pad

**Prerequisites:** Bluetooth keyboard or device with D-pad support

**Steps:**
1. Connect Bluetooth keyboard or use device D-pad
2. Navigate MealListScreen with Tab key (or D-pad down)
3. Navigate SettingsScreen with Tab key (or D-pad down)
4. Observe focus indicators and traversal order

**Expected Focus Order - MealListScreen:**
1. Settings IconButton (toolbar)
2. First meal entry card (if meals exist)
3. Second meal entry card
4. Subsequent meal entries (top-to-bottom)

**Expected Focus Order - SettingsScreen:**
1. Back button (toolbar)
2. API Key input field
3. Endpoint input field
4. Model input field
5. Save Configuration button
6. Test Connection button
7. Theme radio buttons (System, Light, Dark)
8. Accessibility preference
9. Version preference

**Expected Behaviour:**
- Focus indicators visible (Material 3 default blue outline)
- Tab advances to next focusable element (top-to-bottom, left-to-right)
- Shift+Tab goes to previous element
- Enter/Space activates focused element
- All interactive elements reachable via keyboard

**Validation:**
- [ ] Focus order is logical (top-to-bottom, left-to-right)
- [ ] Focus indicators visible for all elements
- [ ] Tab key advances focus correctly
- [ ] Enter/Space activates buttons and cards
- [ ] No keyboard traps (can navigate back with Shift+Tab)

**Notes:** _____________________________________________________

---

## Test Summary

### Overall Results

**Test Date:** ____________  
**Tester:** ____________  
**Device:** ____________  
**Build Version:** ____________

**TalkBack Testing:**
- [ ] All scenarios passed
- [ ] Minor issues found (describe in notes)
- [ ] Blocker issues found (requires fixes)

**Large Font Testing:**
- [ ] All screens scale correctly at Largest font
- [ ] Minor layout issues (acceptable)
- [ ] Major layout breakage (requires fixes)

**Touch Target Testing:**
- [ ] Accessibility Scanner: 0 critical issues
- [ ] Accessibility Scanner: 0 HIGH severity issues
- [ ] MEDIUM/LOW issues documented

**Colour Contrast:**
- [ ] Light mode: All text readable
- [ ] Dark mode: All text readable
- [ ] Non-colour information verified

**Keyboard Navigation (if tested):**
- [ ] Focus order logical
- [ ] All elements reachable
- [ ] Focus indicators visible

### Issues Found

| Severity | Screen | Issue Description | AC Violated |
|----------|--------|-------------------|-------------|
| _Example: HIGH_ | _MealListScreen_ | _Settings button no TalkBack label_ | _AC #1_ |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |

### Acceptance Criteria Verification

- [ ] **AC #1:** All buttons and images have content descriptions ✓ (TalkBack testing)
- [ ] **AC #2:** App supports TalkBack screen reader ✓ (TalkBack navigation scenarios)
- [ ] **AC #3:** Touch targets minimum 48dp ✓ (Accessibility Scanner validation)
- [ ] **AC #4:** Text scales with system font size ✓ (Large font testing)
- [ ] **AC #5:** Colour contrast meets WCAG AA 4.5:1 ✓ (Visual inspection + calculated ratios)
- [ ] **AC #6:** Information not colour-only ✓ (Visual inspection of error/success states)
- [ ] **AC #7:** Focus order logical for keyboard/D-pad ✓ (Keyboard navigation testing)
- [ ] **AC #8:** Camera button discoverable with TalkBack ✓ (Camera flow testing)

### Recommendations

**Immediate Actions Required:**
- _List any blocker issues that must be fixed before story completion_

**Future Enhancements (V2.0):**
- _List any MEDIUM/LOW severity issues from Accessibility Scanner_
- _List any usability improvements discovered during testing_

### Sign-Off

**Tester Signature:** ___________________  
**Date:** ___________________

**Developer Acknowledgment:** ___________________  
**Date:** ___________________

---

## Notes and Observations

### TalkBack Usage Tips
- **Enable:** Settings → Accessibility → TalkBack → ON
- **Navigate:** Swipe right (next), swipe left (previous)
- **Activate:** Double-tap
- **Disable:** Two-finger long press screen or volume keys shortcut

### Accessibility Scanner Tips
- **Install:** Play Store → "Accessibility Scanner"
- **Enable:** Settings → Accessibility → Accessibility Scanner → ON
- **Scan:** Tap blue FAB on screen edge
- **Report:** Review violations by severity (HIGH, MEDIUM, LOW)

### Colour Contrast Calculation (Reference)
Contrast ratios calculated using WebAIM Contrast Checker:
- **Light theme onBackground/background:** #191C1A / #FBFDF9 = 16.5:1
- **Dark theme onBackground/background:** #E1E3DF / #191C1A = 12.8:1
- **WCAG AA Requirement:** 4.5:1 for normal text, 3:1 for large text (≥18pt)

All Foodie app colour combinations exceed WCAG AA standards.
