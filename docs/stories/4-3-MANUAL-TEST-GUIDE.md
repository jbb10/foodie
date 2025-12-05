# Story 4.3: Manual Testing Guide
## API Error Classification and Handling

**Device:** Pixel 8 Pro - Android 16
**Date:** 2025-11-15
**Tester:** BMad

---

## Test Scenarios

### ✅ Scenario 1: Authentication Error (Invalid API Key)

**Objective:** Verify AuthError shows "API key invalid. Check settings." with "Open Settings" action button

**Prerequisites:**
- App installed and Health Connect permissions granted
- Home screen widget added

**Steps:**
1. **Invalidate API Key:**
   - Open terminal and run:
     ```bash
     adb shell
     cd /sdcard/Android/data/com.foodie.app/files
     echo "AZURE_OPENAI_API_KEY=invalid-key-test-12345" > local.properties
     echo "AZURE_OPENAI_ENDPOINT=https://fake-endpoint.openai.azure.com/" >> local.properties
     exit
     ```
   - OR modify `app/local.properties` and reinstall app

2. **Trigger Analysis:**
   - Tap home screen widget
   - Grant camera permission if needed
   - Take photo of food
   - Tap "Use Photo"

3. **Verify Error Notification:**
   - Wait for analysis to fail (~5-10 seconds)
   - Pull down notification shade
   - **VERIFY:** Notification title: "Configuration Error"
   - **VERIFY:** Message: "API key invalid. Check settings."
   - **VERIFY:** Action button: "Open Settings"

4. **Test Action Button:**
   - Tap "Open Settings" button
   - **VERIFY:** App opens (currently logs action, Settings screen not implemented yet)
   - **CHECK LOGS:** `adb logcat | grep "Deep link action: Open Settings"`

**Expected Result:**
- ✅ Error notification appears with correct title and message
- ✅ "Open Settings" button visible
- ✅ Tapping button opens app (deep link logged)
- ✅ Notification can be dismissed

**Actual Result:**
- [ ] PASS / [ ] FAIL
- Notes: _______________________________________________

---

### ✅ Scenario 2: Network Error (Immediate Notification)

**Objective:** Verify NetworkError shows "Request timed out" with "Retry" action button **immediately** when network unavailable

**Prerequisites:**
- Valid API key restored (or use original `local.properties`)
- Home screen widget added

**Steps:**
1. **Setup Network Failure:**
   - Take photo of food via widget
   - Confirm photo
   - **IMMEDIATELY** enable airplane mode on phone (within 1-2 seconds of confirming)

2. **Observe Notifications:**
   - Initial: "Analyzing your meal..." notification appears briefly
   - **After ~1-2 seconds:** Foreground notification **DISAPPEARS** (this is EXPECTED - Worker stops)
   - **Error notification appears immediately** (not after retry exhaustion)
   - **VERIFY:** Notification title: "Network Error"
   - **VERIFY:** Message: "Request timed out. Check your internet connection."
   - **VERIFY:** Action button: "Retry"

3. **Test Automatic Retry:**
   - **Keep error notification visible**
   - Disable airplane mode (restore WiFi/data)
   - **VERIFY:** Worker automatically retries (may see brief "Analyzing..." notification)
   - **VERIFY:** Analysis completes successfully
   - **VERIFY:** Error notification dismissed automatically on success

4. **Alternative: Test Manual Retry:**
   - (If you want to test manual retry instead of automatic)
   - Keep airplane mode ON
   - Tap "Retry" button in error notification
   - **VERIFY:** Notification dismisses
   - **VERIFY:** Worker attempts retry (will fail again without network)
   - **CHECK LOGS:** `adb logcat | grep "Retry requested"`

**Expected Result:**
- ✅ Foreground notification disappears when network check fails (~1-2 sec)
- ✅ **Error notification appears IMMEDIATELY** (not after 30s retry exhaustion)
- ✅ "Retry" button visible and functional
- ✅ Worker automatically retries when network restored
- ✅ Manual retry also works (via notification button)

**Why Foreground Notification Disappears:**
- When Worker detects no network, it returns `Result.retry()`
- WorkManager removes foreground notification when Worker stops executing
- **Error notification persists** to give user visibility and control
- This is expected Android WorkManager behaviour

**Actual Result:**
- [ ] PASS / [ ] FAIL
- Notes: _______________________________________________

---

### ✅ Scenario 3: Server Error (503 Service Unavailable)

**Objective:** Verify ServerError shows "Service temporarily unavailable. Will retry automatically."

**Prerequisites:**
- This requires mocking the API to return 503
- **SIMPLIFIED TEST:** Verify via logs that ServerError is classified correctly

**Alternative Test (Log Verification):**
1. Check ErrorHandlerTest passes:
   ```bash
   cd /Users/jbjornsson/source/foodie/app
   ./gradlew test --tests ErrorHandlerTest
   ```

2. **VERIFY:** Test `classify should return ServerError for HTTP 503` passes

**Expected Result:**
- ✅ ServerError correctly classified (verified in unit tests)
- ✅ Message: "Service temporarily unavailable. Will retry automatically."
- ✅ No action button (automatic retry)

**Actual Result:**
- [ ] PASS / [ ] FAIL (via unit test)
- Notes: _______________________________________________

---

### ✅ Scenario 4: Parse Error (Malformed JSON)

**Objective:** Verify ParseError shows "Unexpected response from AI service."

**Prerequisites:**
- This requires mocking the API to return invalid JSON
- **SIMPLIFIED TEST:** Verify via unit tests

**Test (Unit Test Verification):**
1. Check ErrorHandlerTest passes:
   ```bash
   ./gradlew test --tests ErrorHandlerTest
   ```

2. **VERIFY:** Test `getUserMessage should return parse error message for ParseError` passes

**Expected Result:**
- ✅ ParseError correctly classified (verified in unit tests)
- ✅ Message: "Unexpected response from AI service."

**Actual Result:**
- [ ] PASS / [ ] FAIL (via unit test)
- Notes: _______________________________________________

---

### ✅ Scenario 5: Permission Denied (Health Connect)

**Objective:** Verify PermissionDenied shows "Health Connect permissions required" with "Grant Access" action

**Steps:**
1. **Revoke Health Connect Permissions:**
   - Open Settings → Apps → Foodie → Permissions
   - OR: Settings → Health Connect → App Permissions → Foodie
   - Revoke nutrition read/write permissions

2. **Trigger Analysis:**
   - Take photo via widget
   - Confirm photo
   - Wait for analysis to complete

3. **Verify Error Notification:**
   - **VERIFY:** Notification title: "Permissions Required"
   - **VERIFY:** Message: "Health Connect permissions required. Tap to grant access."
   - **VERIFY:** Action button: "Grant Access"

4. **Test Action Button:**
   - Tap "Grant Access" button
   - **VERIFY:** Health Connect permission screen launches
   - Grant permissions
   - **VERIFY:** Can retry analysis successfully

**Expected Result:**
- ✅ Permission error notification appears
- ✅ "Grant Access" button visible
- ✅ Tapping button launches permission flow

**Actual Result:**
- [ ] PASS / [ ] FAIL
- Notes: _______________________________________________

---

## Comprehensive Logging Verification

**Monitor all error scenarios:**

```bash
# Terminal 1: Monitor AnalyseMealWorker errors
adb logcat | grep -E "(AnalyseMealWorker|NotificationHelper|RetryAnalysis)"

# Terminal 2: Monitor error classifications
adb logcat | grep -E "(ErrorHandler|classify|getUserMessage)"

# Terminal 3: Monitor deep link actions
adb logcat | grep -E "(Deep link|OPEN_SETTINGS|GRANT_PERMISSIONS)"
```

**Verify Log Patterns:**
- ✅ Error classification logs: "Error type: NetworkError, Attempt: 4/4, Photo: ..."
- ✅ Notification display logs: "Persistent notification shown for AuthError with Settings action"
- ✅ Retry action logs: "Retry requested for work <uuid>, photoUri=..."
- ✅ Deep link logs: "Deep link action: Open Settings"

---

## Error Message Verification Checklist

**All error messages must be user-friendly (no technical jargon):**

| Error Type | Expected Message | Action Button | Verified |
|-----------|------------------|---------------|----------|
| NetworkError | "Request timed out. Check your internet connection." | "Retry" | [ ] |
| AuthError | "API key invalid. Check settings." | "Open Settings" | [ ] |
| ServerError | "Service temporarily unavailable. Will retry automatically." | None | [ ] |
| RateLimitError | "Too many requests. Please wait a moment." | None | [ ] |
| ParseError | "Unexpected response from AI service." | None | [ ] |
| PermissionDenied | "Health Connect permissions required. Tap to grant access." | "Grant Access" | [ ] |

---

## Quick Test Commands

```bash
# View recent notifications
adb shell dumpsys notification --noredact | grep -A 20 "com.foodie.app"

# Clear all notifications
adb shell service call notification 1

# Monitor WorkManager
adb logcat | grep -E "WM-WorkSpec|AnalyseMealWorker"

# Check notification channels
adb shell dumpsys notification | grep -A 5 "meal_analysis"

# Force stop app and restart
adb shell am force-stop com.foodie.app
adb shell am start -n com.foodie.app/.MainActivity
```

---

## Test Results Summary

**Date:** _______________
**Tester:** BMad

| Scenario | Status | Notes |
|----------|--------|-------|
| 1. Auth Error | [ ] PASS / [ ] FAIL | |
| 2. Network Error | [ ] PASS / [ ] FAIL | |
| 3. Server Error | [ ] PASS / [ ] FAIL | |
| 4. Parse Error | [ ] PASS / [ ] FAIL | |
| 5. Permission Denied | [ ] PASS / [ ] FAIL | |

**Overall Result:** [ ] ALL PASS / [ ] FAILURES DETECTED

**Issues Found:**
1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

**Sign-off:**
- [ ] All critical error messages display correctly
- [ ] All action buttons function as expected
- [ ] Deep linking works for Settings and Permissions
- [ ] No crashes or unexpected behaviour
- [ ] Ready for code review

---

## Cleanup After Testing

```bash
# Restore original API configuration
# Copy your backed-up local.properties back:
# adb push /path/to/backup/local.properties /sdcard/Android/data/com.foodie.app/files/

# Uninstall test build
adb uninstall com.foodie.app

# Or keep app and just clear data
adb shell pm clear com.foodie.app
```
