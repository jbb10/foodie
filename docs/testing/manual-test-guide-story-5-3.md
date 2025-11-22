# Manual Test Guide: Story 5.3 - Model Selection and Configuration

**Date:** 2025-11-22  
**Story:** 5.3 - Model Selection and Configuration  
**Tester:** _________  
**Device/Emulator:** _________  
**Android Version:** _________

## Prerequisites

- Foodie app installed (Story 5.3 build)
- Valid Azure OpenAI API key and endpoint (from Story 5.2 setup)
- Internet connectivity for API tests
- Access to Azure Portal to verify available deployments (optional)

---

## Test Scenario 1: Model Description Display

**Objective:** Verify model description text is visible and helpful

### Steps:
1. Launch Foodie app
2. Navigate to Settings (tap three-dot menu → Settings)
3. Scroll to "API Configuration" section
4. Locate "Model Deployment Name" field

### Expected Results:
- [ ] Model field displays current value (default "gpt-4.1" on fresh install)
- [ ] Description text visible below model field
- [ ] Description reads: "Default: gpt-4.1 (Advanced reasoning and vision capabilities). Enter your custom deployment name if needed."
- [ ] Description text is smaller/lighter than field label (Material3 bodySmall style)

---

## Test Scenario 2: Default Model Value

**Objective:** Verify fresh installation defaults to "gpt-4.1"

### Steps:
1. **Fresh install:** Uninstall Foodie, reinstall from latest build
2. Launch app, navigate to Settings
3. Check "Model Deployment Name" field value

### Expected Results:
- [ ] Model field shows "gpt-4.1" as placeholder or default value
- [ ] No error messages displayed
- [ ] Description text present

---

## Test Scenario 3: Custom Model Name Entry

**Objective:** Verify custom deployment names can be entered and saved

### Steps:
1. Navigate to Settings → API Configuration
2. Tap "Model Deployment Name" field
3. Clear existing value
4. Enter custom model name: `gpt-4o-mini`
5. Tap "Save Configuration" button
6. Verify success message displays
7. Exit and re-enter Settings

### Expected Results:
- [ ] Field accepts custom name `gpt-4o-mini`
- [ ] "Configuration saved successfully" message appears
- [ ] After re-entering Settings, field shows `gpt-4o-mini`
- [ ] Description text remains visible

### Variations to Test:
- [ ] Custom deployment name with hyphens: `my-deployment-123`
- [ ] Base model names: `gpt-4.1`, `gpt-4o-mini`, `gpt-4o`
- [ ] Long deployment name (20+ characters)

---

## Test Scenario 4: Test Connection with Default Model

**Objective:** Verify test connection validates model availability

### Prerequisites:
- Valid API key and endpoint configured from Story 5.2
- Default model "gpt-4.1" should exist in your Azure resource

### Steps:
1. Navigate to Settings → API Configuration
2. Ensure model field shows "gpt-4.1"
3. Ensure API key and endpoint are configured (from Story 5.2)
4. Tap "Test Connection" button
5. Wait for result (up to 30 seconds)

### Expected Results:
- [ ] Loading indicator appears during test
- [ ] Success message: "API configuration valid ✓"
- [ ] No error messages
- [ ] Test button becomes enabled again

---

## Test Scenario 5: Test Connection with Invalid Model

**Objective:** Verify model-specific error message for unavailable deployment

### Steps:
1. Navigate to Settings → API Configuration
2. Change model to invalid name: `invalid-model-xyz`
3. Tap "Test Connection" button
4. Wait for error result

### Expected Results:
- [ ] Error message displays
- [ ] Message references deployment name: "Deployment 'invalid-model-xyz' not available in your Azure resource"
- [ ] Error message is clear and actionable
- [ ] Test button becomes enabled again

---

## Test Scenario 6: Test Connection with Blank Model

**Objective:** Verify validation error for empty model field

### Steps:
1. Navigate to Settings → API Configuration
2. Clear model field completely (delete all text)
3. Tap "Test Connection" button

### Expected Results:
- [ ] Error message: "API configuration incomplete" or "Model name required"
- [ ] Test connection does not attempt API call
- [ ] Field validation triggers before network request

---

## Test Scenario 7: Model Persistence Across App Restart

**Objective:** Verify model selection persists after app restart

### Steps:
1. Navigate to Settings
2. Set model to custom value: `my-custom-model`
3. Tap "Save Configuration"
4. Close Foodie app completely (swipe away from recents)
5. Reopen Foodie app
6. Navigate to Settings

### Expected Results:
- [ ] Model field shows `my-custom-model` (persisted value)
- [ ] No reset to default value
- [ ] Configuration intact

---

## Test Scenario 8: Meal Capture with Custom Model

**Objective:** Verify API calls use configured model

### Prerequisites:
- Valid API configuration with custom model name
- Access to ADB logcat (optional, for verification)

### Steps:
1. Configure custom model in Settings: `gpt-4o-mini`
2. Save configuration
3. Navigate to home screen
4. Tap meal capture widget
5. Capture a food photo
6. Confirm photo
7. Wait for analysis to complete

### Expected Results:
- [ ] Photo analysis completes successfully
- [ ] Nutrition data saved to Health Connect
- [ ] No errors related to model configuration

### Optional ADB Verification:
```bash
adb logcat | grep "model"
```
- [ ] Log shows API request uses configured model name

---

## Test Scenario 9: Model Description Readability

**Objective:** Verify description text is readable and helpful

### Steps:
1. Navigate to Settings → API Configuration
2. Read model description text
3. Test on different screen sizes/orientations (if applicable)

### Expected Results:
- [ ] Description text readable without scrolling
- [ ] Text provides clear guidance:
  - Default model name ("gpt-4.1")
  - Model capabilities ("Advanced reasoning and vision")
  - Instruction for custom names ("Enter your custom deployment name if needed")
- [ ] No text cutoff or overlap
- [ ] Adequate spacing between field and description

---

## Edge Cases & Error Handling

### EC1: Model Field with Special Characters
**Steps:** Enter model name with special chars: `model_name!@#$%`  
**Expected:** Field accepts input (Azure allows various chars in deployment names)

### EC2: Very Long Model Name
**Steps:** Enter 50+ character model name  
**Expected:** Field scrolls horizontally or wraps text, no crash

### EC3: Network Timeout During Test Connection
**Steps:** Disable wifi/mobile data mid-test  
**Expected:** "Connection failed. Check internet." error message

### EC4: Model Field Focus/Blur Behavior
**Steps:** Tap into model field, then tap out without changing value  
**Expected:** No unintended state changes, description remains visible

---

## Summary Checklist

- [ ] All 9 test scenarios executed
- [ ] All edge cases tested
- [ ] No crashes observed
- [ ] Description text helpful and readable
- [ ] Model persistence working
- [ ] Test connection validates model correctly
- [ ] Custom model names accepted

---

## Notes / Issues Found

_Use this space to document any bugs, unexpected behavior, or suggestions for improvement:_

---

## Sign-Off

- [ ] Manual testing complete
- [ ] All critical issues resolved or documented
- [ ] Story ready for approval

**Tester Signature:** _________  
**Date:** _________
