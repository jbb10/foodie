# Story 3.4 - Delete Meal Entry - Manual Testing Guide

**Date:** 2025-11-12  
**Tester:** Manual validation required  
**Test Environment:** Pixel_8_Pro Emulator (Android 14) OR Physical Device  

---

## Prerequisites

1. ✅ Foodie app installed on emulator
2. ⚠️ Health Connect permissions granted (do this first - see Step 1 below)
3. 📱 At least 2-3 meal entries already logged (via widget capture flow)

---

## Step 1: Grant Health Connect Permissions

**CRITICAL: Do this first or the app won't work**

1. Open the **Foodie** app on your emulator/device
2. If prompted for Health Connect permissions:
   - Tap **Allow** when permission dialog appears
   - If no dialog appears, you may need to navigate to Settings → Apps → Foodie → Permissions → Health Connect and grant manually

**Alternative:** Create meal entries first using the widget:
1. Add the Foodie home screen widget
2. Tap the widget to capture 2-3 test meals
3. This will trigger permission flow automatically

---

## Step 2: Cross-App Validation Setup (AC #8)

**Install Google Fit** (if testing cross-app sync):

### On Emulator:
```bash
# Google Fit may not be available on emulator
# You can test with Health Connect system app instead
```

### On Physical Device:
1. Install Google Fit from Play Store
2. Grant Health Connect permissions to Google Fit
3. Verify Foodie meal entries appear in Google Fit → Nutrition section

---

## Test Cases to Execute

### TC-1: Long-Press Gesture Detection (AC #1)

**Steps:**
1. Open Foodie app
2. View the meal list screen (main screen)
3. **Long-press** (tap and hold for ~1 second) on any meal entry card

**Expected Result:**
- ✅ Dialog appears immediately
- ✅ Dialog title: "Delete Entry"
- ✅ Dialog message: "Delete this entry? This cannot be undone."
- ✅ Two buttons visible: "Cancel" (left), "Delete" (right, red text)

**Pass/Fail:** ______

---

### TC-2: Cancel Button (AC #3)

**Steps:**
1. Long-press a meal entry
2. Tap the **Cancel** button

**Expected Result:**
- ✅ Dialog dismisses immediately
- ✅ Meal entry still visible in list
- ✅ No changes to data

**Pass/Fail:** ______

---

### TC-3: Delete Button - Success Flow (AC #4, #5, #6)

**Steps:**
1. Note the total number of entries in the list (e.g., 3 entries)
2. Long-press a meal entry
3. Tap the **Delete** button

**Expected Result:**
- ✅ Dialog dismisses immediately
- ✅ Toast/Snackbar message appears: "Entry deleted"
- ✅ Entry disappears from list immediately (no manual refresh needed)
- ✅ Total entry count decreased by 1 (e.g., now shows 2 entries)
- ✅ Delete completes in < 1 second (feels instant)

**Pass/Fail:** ______

---

### TC-4: Normal Tap Still Works

**Steps:**
1. **Single tap** (not long-press) on a meal entry

**Expected Result:**
- ✅ Navigates to meal detail/edit screen (NOT delete dialog)
- ✅ Long-press functionality doesn't interfere with normal navigation

**Pass/Fail:** ______

---

### TC-5: Scroll Gesture Not Affected

**Steps:**
1. Scroll up and down the meal list

**Expected Result:**
- ✅ Scrolling works normally
- ✅ Long-press gesture doesn't interfere with scroll

**Pass/Fail:** ______

---

### TC-6: Delete Last Entry - Empty State

**Steps:**
1. Delete all meal entries until only one remains
2. Long-press the last entry
3. Tap **Delete**

**Expected Result:**
- ✅ Entry deleted
- ✅ Empty state message displays: "No meals logged yet. Use the widget to capture your first meal!"

**Pass/Fail:** ______

---

### TC-7: Deletion is Permanent (AC #7)

**Steps:**
1. Delete a meal entry
2. Close and reopen the app
3. Check if deleted entry reappears

**Expected Result:**
- ✅ Deleted entry does NOT reappear
- ✅ No "undo" or "restore" option available anywhere in the app
- ✅ Deletion is permanent

**Pass/Fail:** ______

---

### TC-8: Cross-App Sync (AC #8) - **CRITICAL FOR STORY COMPLETION**

**Prerequisites:** Google Fit OR Health Connect system app installed

#### Option A: Google Fit (Preferred)

**Steps:**
1. Open **Google Fit** app
2. Navigate to: Journal → Nutrition
3. **Verify:** Foodie meal entries are visible in Google Fit
4. Note a specific meal entry (e.g., "Chicken Salad, 450 kcal")
5. Return to **Foodie** app
6. Long-press the same meal entry
7. Tap **Delete**
8. Return to **Google Fit** app
9. Refresh the Nutrition section (pull-to-refresh)

**Expected Result:**
- ✅ Deleted entry NO LONGER appears in Google Fit
- ✅ Deletion synced immediately across apps
- ✅ No "undo" or "restore" option in Google Fit

**Pass/Fail:** ______

#### Option B: Health Connect System App (Fallback)

**Steps:**
1. Open **Settings** → **Apps** → **Health Connect**
2. Tap **Data and access** → **Nutrition**
3. **Verify:** Foodie meal entries are visible
4. Note a specific meal entry
5. Return to **Foodie** app
6. Delete the noted entry
7. Return to **Health Connect** app → Nutrition
8. Refresh the view

**Expected Result:**
- ✅ Deleted entry NO LONGER appears in Health Connect
- ✅ Deletion is atomic and immediate

**Pass/Fail:** ______

---

## Performance Validation

**Measurement:** Time from tapping "Delete" to entry disappearing from list

**Method:** Use a stopwatch or count "one-thousand-one" (1 second)

**Steps:**
1. Long-press a meal entry
2. Tap **Delete**
3. Time how long until entry disappears

**Expected Result:**
- ✅ Delete completes in < 1 second (should feel instant, typically < 200ms)

**Actual Time:** _______ ms/seconds

**Pass/Fail:** ______

---

## Summary Report

**Date Tested:** _______  
**Environment:** Emulator / Physical Device (circle one)  
**Total Test Cases:** 9  
**Passed:** _______  
**Failed:** _______  

### Failed Test Cases (if any):

1. TC# ___: _______________________________
2. TC# ___: _______________________________

### Notes/Issues Found:

______________________________________________
______________________________________________
______________________________________________

---

## Completion Criteria

**Story 3.4 can be marked DONE when:**

- ✅ All 9 test cases PASS
- ✅ TC-8 (Cross-App Sync) validated on Google Fit OR Health Connect system app
- ✅ Performance < 1 second confirmed
- ✅ No crashes or errors during testing
- ✅ Instrumentation tests written (already completed)
- ✅ This manual testing report filled out and added to story Dev Notes

---

**Tester Signature:** _______________________  
**Date:** _______________________
