# Story 9.1: FoodieSeeder - Health Connect Test Data Broadcast Tool

Status: drafted

## Story

As a developer running automated E2E tests,
I want a sidecar tool that seeds and cleans Health Connect test data via ADB broadcast commands,
So that I can guarantee deterministic test scenarios with clean state setup and teardown.

## Acceptance Criteria

### AC-1: Sidecar Module Architecture
**Given** the FoodieSeeder tool must be separate from the main app
**When** the module structure is created
**Then** a new Android module `foodieseeder` exists in the project
**And** the module has its own `build.gradle.kts` with separate applicationId `com.foodie.seeder`
**And** the module is emulator-only (not deployed to production devices)
**And** the module declares Health Connect permissions: `READ_NUTRITION` and `WRITE_NUTRITION`
**And** the module has minimal dependencies (Health Connect SDK, AndroidX Core only)

### AC-2: WRITE_NUTRITION Broadcast Receiver
**Given** tests need to seed specific nutrition data
**When** an ADB broadcast is sent with action `com.foodie.seeder.WRITE_NUTRITION`
**Then** the broadcast receiver receives the intent
**And** the receiver extracts extras: `calories` (double), `description` (string), `timestamp` (optional long, defaults to now)
**And** the receiver validates: calories > 0 and < 10000, description is not empty
**And** a `NutritionRecord` is written to Health Connect with the provided data
**And** a Toast or Log confirms successful write: "Seeded: {calories} cal - {description}"
**And** validation errors log clearly: "Invalid data: {reason}"

### AC-3: DELETE_ALL_NUTRITION Broadcast Receiver  
**Given** tests need a clean slate before execution
**When** an ADB broadcast is sent with action `com.foodie.seeder.DELETE_ALL_NUTRITION`
**Then** the broadcast receiver receives the intent
**And** all `NutritionRecord` entries from the last 7 days are queried from Health Connect
**And** all queried records are deleted using `HealthConnectClient.deleteRecords()`
**And** a Toast or Log confirms: "Deleted {count} nutrition records"
**And** if Health Connect is unavailable, logs error: "Health Connect not available"

### AC-4: ADB Command Integration
**Given** the broadcast receivers are implemented
**When** I run ADB commands from terminal
**Then** the following commands work successfully:

**Seed data:**
```bash
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION \
  --es description "Test Meal" \
  --ed calories 650.0
```

**Clean data (7-day window):**
```bash
adb shell am broadcast -a com.foodie.seeder.DELETE_ALL_NUTRITION
```

**Seed with timestamp:**
```bash
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION \
  --es description "Yesterday's Meal" \
  --ed calories 500.0 \
  --el timestamp 1733011200000
```

**And** all commands return immediately without blocking terminal
**And** success/failure is visible in Logcat with tag "FoodieSeeder"

### AC-5: Health Connect Permissions Flow
**Given** the seeder tool needs Health Connect permissions
**When** the seeder app is installed on emulator
**Then** a minimal UI activity exists that requests Health Connect permissions on first launch
**And** the activity displays text: "FoodieSeeder - Test Data Tool. Grant Health Connect permissions to enable test data seeding."
**And** permissions are requested using standard Health Connect permission flow
**And** once granted, the activity displays: "Ready. Use ADB broadcast commands to seed/clean data."
**And** the activity has no other UI (minimal surface area)

### AC-6: Verification Script
**Given** the tool must be easily validated
**When** a verification script is run
**Then** a script exists at `scripts/verify-seeder.sh` that:
1. Seeds a test record via ADB (650 calories, "Verification Meal")
2. Verifies the record appears in the main Foodie app meal list
3. Deletes all records via ADB
4. Verifies the Foodie app meal list is empty
5. Prints PASS/FAIL for each step
**And** the script exits with code 0 on success, non-zero on failure
**And** the script includes clear output messages for each verification step

### AC-7: E2E Test Orchestration Script
**Given** Maestro E2E tests require data setup/teardown
**When** the orchestration script is created
**Then** a script exists at `scripts/run-e2e-tests.sh` that:
1. Cleans all Health Connect data (DELETE_ALL_NUTRITION)
2. Seeds required test data for each test scenario
3. Runs the Maestro test
4. Cleans data again after test completion
5. Repeats for all Maestro test files in `.maestro/` folder
**And** the script logs each step clearly
**And** the script captures Maestro test output
**And** the script reports overall pass/fail summary at the end
**And** the script exits with non-zero code if any test fails

### AC-8: No UI Interaction Required
**Given** the tool is for automated testing only
**When** test data is seeded or cleaned
**Then** no UI interaction is required (broadcasts work in background)
**And** no notifications are displayed to the user
**And** all operations complete silently except for Logcat logging
**And** the seeder app can be running in background or not running (broadcasts wake it up)

### AC-9: README Documentation
**Given** developers need to use this tool
**When** the README is created
**Then** a file exists at `foodieseeder/README.md` with sections:
- **Purpose**: Testing tool for deterministic E2E tests
- **Installation**: How to build and install on emulator
- **ADB Commands**: Full command reference with examples
- **Verification**: How to run verification script
- **E2E Integration**: How to use with Maestro tests
- **Troubleshooting**: Common issues and solutions
**And** the main project README links to the seeder documentation
**And** all examples are copy-paste ready

### AC-10: Build Configuration
**Given** the seeder is a separate module
**When** the module is built
**Then** the module builds successfully with `./gradlew :foodieseeder:assembleDebug`
**And** the APK is generated at `foodieseeder/build/outputs/apk/debug/foodieseeder-debug.apk`
**And** the APK can be installed with `adb install -r foodieseeder-debug.apk`
**And** the main app and seeder can be installed simultaneously (different applicationIds)
**And** a Makefile target exists: `make install-seeder` to build and install

## Tasks / Subtasks

### Task 1: Documentation Research & Technical Validation ⚠️ COMPLETE BEFORE PROCEEDING TO IMPLEMENTATION

**Objective:** Validate BroadcastReceiver approach for Health Connect data manipulation and identify platform constraints

**Required Research:**
1. Review official documentation for BroadcastReceiver and Health Connect SDK:
   - BroadcastReceiver: https://developer.android.com/reference/android/content/BroadcastReceiver
   - Health Connect SDK: https://developer.android.com/health-and-fitness/guides/health-connect
   - ADB broadcast commands: https://developer.android.com/tools/adb#am

2. Validate assumptions:
   - ✓ BroadcastReceiver can be triggered via `adb shell am broadcast`
   - ✓ Sidecar app can access Health Connect if permissions are granted
   - ✓ Multiple apps can write to same Health Connect data store
   - ✓ NutritionRecord supports programmatic creation with custom timestamps
   - ✓ Bulk deletion of records is supported by Health Connect API

3. Identify constraints:
   - Minimum Android version for Health Connect (API 28+)
   - Health Connect app must be installed on emulator
   - Broadcast receiver registration requirements (manifest vs runtime)
   - Permission scopes and cross-app data visibility

**Deliverable Checkpoint:** ✅ REQUIRED

Document findings in Dev Notes before proceeding to Task 2:
- [ ] BroadcastReceiver pattern validated for ADB commands
- [ ] Health Connect multi-app write confirmed
- [ ] Timestamp manipulation validated
- [ ] Bulk deletion approach confirmed
- [ ] Alternative approaches considered (if needed)

⚠️ Do NOT proceed to implementation tasks until research checkpoint is complete

### Task 2: Create Sidecar Module Structure (AC-1, AC-10)
- [ ] Create new Android module: `foodieseeder/` in project root
  - [ ] Add module to `settings.gradle.kts`: `include(":foodieseeder")`
  - [ ] Create `foodieseeder/build.gradle.kts` with separate applicationId
  - [ ] Set minSdk 28, targetSdk 35 (match main app)
  - [ ] Add Health Connect dependency: `androidx.health.connect:connect-client`
- [ ] Configure build for emulator-only deployment
  - [ ] Add buildType debug only (no release variant)
  - [ ] Add Makefile target `install-seeder` for build and install
- [ ] Create minimal AndroidManifest.xml
  - [ ] Declare applicationId: `com.foodie.seeder`
  - [ ] Request Health Connect permissions: `READ_NUTRITION`, `WRITE_NUTRITION`
  - [ ] Register launcher activity for permission setup

### Task 3: Implement WRITE_NUTRITION Broadcast Receiver (AC-2, AC-4)
- [ ] Create `SeedNutritionReceiver.kt` extending BroadcastReceiver
  - [ ] Register in manifest with action `com.foodie.seeder.WRITE_NUTRITION`
  - [ ] Extract intent extras: `description` (string), `calories` (double), `timestamp` (long, optional)
  - [ ] Validate extracted data (calories 1-10000, description non-empty)
- [ ] Implement Health Connect write logic
  - [ ] Initialize HealthConnectClient
  - [ ] Create NutritionRecord with energy and name fields
  - [ ] Set startTime/endTime from timestamp or now()
  - [ ] Call insertRecords() and handle errors
- [ ] Add logging with tag "FoodieSeeder"
  - [ ] Log success: "Seeded: {calories} cal - {description}"
  - [ ] Log validation errors clearly
  - [ ] Log Health Connect errors with stack trace
- [ ] Test with ADB command:
  ```bash
  adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION \
    --es description "Test" --ed calories 500.0
  ```

### Task 4: Implement DELETE_ALL_NUTRITION Broadcast Receiver (AC-3, AC-4)
- [ ] Create `CleanNutritionReceiver.kt` extending BroadcastReceiver
  - [ ] Register in manifest with action `com.foodie.seeder.DELETE_ALL_NUTRITION`
  - [ ] No intent extras required (deletes all from last 7 days)
- [ ] Implement Health Connect bulk delete logic
  - [ ] Initialize HealthConnectClient
  - [ ] Query NutritionRecords with TimeRangeFilter (last 7 days)
  - [ ] Extract record IDs from query results
  - [ ] Call deleteRecords() with list of IDs
  - [ ] Handle errors gracefully
- [ ] Add logging
  - [ ] Log before deletion: "Deleting {count} nutrition records..."
  - [ ] Log after deletion: "Deleted {count} nutrition records"
  - [ ] Log if no records found: "No records to delete"
  - [ ] Log errors with stack trace
- [ ] Test with ADB command:
  ```bash
  adb shell am broadcast -a com.foodie.seeder.DELETE_ALL_NUTRITION
  ```

### Task 5: Create Permission Request Activity (AC-5)
- [ ] Create `SeederPermissionActivity.kt` with minimal UI
  - [ ] Display app title: "FoodieSeeder - Test Data Tool"
  - [ ] Show instruction text about Health Connect permissions
  - [ ] Request Health Connect permissions on first launch
  - [ ] Show "Ready" message after permissions granted
- [ ] Implement permission flow
  - [ ] Check if permissions already granted
  - [ ] Launch Health Connect permission intent if not granted
  - [ ] Handle permission result
  - [ ] Store granted state in SharedPreferences
- [ ] Set as launcher activity in manifest
- [ ] Test on emulator:
  - [ ] Launch app, grant permissions
  - [ ] Verify permissions visible in Health Connect settings
  - [ ] Verify broadcasts work after permissions granted

### Task 6: Create Verification Script (AC-6)
- [ ] Create `scripts/verify-seeder.sh` with executable permissions
- [ ] Implement verification steps:
  - [ ] Step 1: Clean data (`DELETE_ALL_NUTRITION` broadcast)
  - [ ] Step 2: Seed test record (650 cal, "Verification Meal")
  - [ ] Step 3: Wait 2 seconds for Health Connect sync
  - [ ] Step 4: Launch Foodie app to meal list (optional: use ADB to trigger)
  - [ ] Step 5: Manual verification prompt or automated check
  - [ ] Step 6: Clean data again
  - [ ] Step 7: Manual verification of empty list
- [ ] Add clear console output for each step
  - [ ] Print PASS/FAIL status
  - [ ] Use colours (green/red) if supported
  - [ ] Exit with code 0 on success, non-zero on failure
- [ ] Add usage instructions in script comments
- [ ] Test script end-to-end on emulator

### Task 7: Create E2E Test Orchestration Script (AC-7)
- [ ] Create `scripts/run-e2e-tests.sh` with executable permissions
- [ ] Implement orchestration logic:
  - [ ] Find all `.maestro/*.yaml` test files
  - [ ] For each test file:
    - [ ] Clean Health Connect data (DELETE_ALL_NUTRITION)
    - [ ] Seed test-specific data (parse from test file or use defaults)
    - [ ] Run Maestro test: `maestro test {file}`
    - [ ] Capture test output and exit code
    - [ ] Clean data after test
    - [ ] Log test result (PASS/FAIL)
  - [ ] Print summary: X tests passed, Y tests failed
  - [ ] Exit with non-zero if any test failed
- [ ] Add configuration section at top of script
  - [ ] MAESTRO_TIMEOUT: default 60 seconds
  - [ ] TEST_DATA_DIR: directory with test data configs
  - [ ] CLEANUP_ON_FAILURE: true/false (clean even if test fails)
- [ ] Add verbose logging option (--verbose flag)
- [ ] Test with subset of Maestro tests first

### Task 8: Add Seeder Documentation (AC-9)
- [ ] Create `foodieseeder/README.md` with sections:
  - [ ] **Purpose**: Testing tool for E2E test determinism
  - [ ] **Installation**: Build and install steps
  - [ ] **ADB Commands**: Complete reference with examples
    - [ ] WRITE_NUTRITION with all parametres
    - [ ] DELETE_ALL_NUTRITION
    - [ ] Timestamp format explanation
  - [ ] **Verification**: How to run verify-seeder.sh
  - [ ] **E2E Integration**: How to use run-e2e-tests.sh
  - [ ] **Troubleshooting**: Common issues
    - [ ] Health Connect not installed
    - [ ] Permissions not granted
    - [ ] Broadcast not received (app not installed)
- [ ] Update main project `README.md`:
  - [ ] Add "Testing Tools" section
  - [ ] Link to FoodieSeeder documentation
  - [ ] Explain when to use the seeder
- [ ] Add inline code documentation (KDocs)
  - [ ] Document BroadcastReceiver classes
  - [ ] Document Health Connect operations
  - [ ] Document script usage in comments

### Task 9: Integration Testing (AC-8, End-to-End Validation)
- [ ] Test seeder installation
  - [ ] Build with `./gradlew :foodieseeder:assembleDebug`
  - [ ] Install APK on emulator
  - [ ] Launch app and grant permissions
  - [ ] Verify both apps installed simultaneously
- [ ] Test WRITE_NUTRITION broadcast
  - [ ] Seed 3 different meals with varying calories
  - [ ] Verify records appear in Foodie app immediately
  - [ ] Check Health Connect app shows records
  - [ ] Verify Logcat logs are clear
- [ ] Test DELETE_ALL_NUTRITION broadcast
  - [ ] Seed 5 records
  - [ ] Run delete broadcast
  - [ ] Verify Foodie app meal list is empty
  - [ ] Verify Health Connect shows 0 nutrition records
- [ ] Test no-UI requirement
  - [ ] Close seeder app
  - [ ] Send broadcasts with app not running
  - [ ] Verify broadcasts still work (app wakes up)
  - [ ] Verify no notifications shown
- [ ] Run verification script
  - [ ] Execute `./scripts/verify-seeder.sh`
  - [ ] Verify all steps PASS
  - [ ] Fix any failures
- [ ] Run E2E orchestration script
  - [ ] Execute `./scripts/run-e2e-tests.sh` with 2-3 Maestro tests
  - [ ] Verify data cleanup between tests
  - [ ] Verify tests run in isolation
  - [ ] Check summary output is accurate

### Task 10: Unit Testing (DoD Requirement)
- [ ] Create `foodieseeder/src/test/` directory structure
- [ ] Write unit tests for data validation logic
  - [ ] Test calorie range validation (0-10000)
  - [ ] Test description non-empty validation
  - [ ] Test timestamp parsing (long to Instant)
  - [ ] Test error messages for invalid input
- [ ] Write unit tests for intent extra extraction
  - [ ] Test extracting string extras
  - [ ] Test extracting double extras
  - [ ] Test extracting long extras (optional)
  - [ ] Test handling missing required extras
- [ ] Run tests: `./gradlew :foodieseeder:test`
- [ ] Verify all tests passing (minimum 5 unit tests)

## Definition of Done

This story is considered COMPLETE only when ALL of the following are satisfied:

### Implementation & Quality
- [x] All acceptance criteria are met with verified evidence (file:line references)
- [x] All tasks and subtasks are completed and checked off
- [x] Code follows project architecture patterns and conventions
- [x] All new/modified code has appropriate error handling
- [x] Code is reviewed (senior developer or AI code review workflow)

### Testing Requirements
- [x] **Unit tests written** for validation logic, intent parsing, and edge cases
- [x] **All unit tests passing** (`./gradlew :foodieseeder:test` succeeds with zero failures)
- [x] **Integration tests**: Verification script and E2E orchestration script tested manually
- [x] **End-to-end validation**: All 10 acceptance criteria verified on emulator
- [x] No test coverage regressions in main app (seeder is separate module)

### Documentation
- [x] FoodieSeeder README complete with all sections
- [x] Main project README updated with testing tools section
- [x] Inline KDocs added for BroadcastReceiver classes
- [x] Scripts include usage instructions in comments
- [x] Dev Notes section includes implementation learnings and references

### Story File Completeness
- [x] Dev Agent Record updated with completion notes and file list
- [x] Change Log entry added summarizing what was implemented
- [x] Story status updated to "review" (pending approval) or "done" (if approved)

### Specific FoodieSeeder Validation
- [x] Both apps installed simultaneously on emulator (main + seeder)
- [x] Health Connect permissions granted for seeder
- [x] ADB broadcast commands work without UI interaction
- [x] Verification script passes all steps
- [x] At least 2 Maestro E2E tests run successfully with orchestration script
- [x] Makefile target `make install-seeder` working

## User Demo

**Purpose**: Demonstrate the FoodieSeeder tool enabling deterministic E2E testing to stakeholders.

### Prerequisites
- Android emulator running with Health Connect installed
- Foodie app installed and configured
- FoodieSeeder app installed with permissions granted
- Terminal access for ADB commands

### Demo Steps

1. **Show Current State**
   - Open Foodie app, navigate to meal list
   - Show current meals (if any exist)

2. **Demonstrate Data Cleanup**
   - Run ADB command: `adb shell am broadcast -a com.foodie.seeder.DELETE_ALL_NUTRITION`
   - Show Logcat output: "Deleted X nutrition records"
   - Refresh Foodie app meal list
   - **Expected**: Meal list is empty

3. **Demonstrate Data Seeding**
   - Run ADB command to seed meal:
     ```bash
     adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION \
       --es description "Demo Breakfast" --ed calories 450.0
     ```
   - Show Logcat output: "Seeded: 450.0 cal - Demo Breakfast"
   - Refresh Foodie app meal list
   - **Expected**: "Demo Breakfast - 450 cal" appears in list

4. **Demonstrate Multiple Meals**
   - Seed 2 more meals with different calories
   - Show all 3 meals in Foodie app list
   - **Expected**: All seeded meals visible immediately

5. **Demonstrate Verification Script**
   - Run: `./scripts/verify-seeder.sh`
   - Show script output with PASS/FAIL for each step
   - **Expected**: All steps PASS

6. **Demonstrate E2E Test Integration**
   - Run: `./scripts/run-e2e-tests.sh` (with 1-2 quick tests)
   - Show orchestration: clean → seed → test → clean
   - Show summary output
   - **Expected**: Tests run in isolation with clean state

### Expected Behaviour
- **No UI interaction required**: All operations happen via ADB commands
- **Immediate sync**: Seeded data appears in Foodie app within 1-2 seconds
- **Clean slate**: DELETE_ALL_NUTRITION ensures deterministic starting point
- **Cross-app visibility**: Seeder writes data visible to main Foodie app

### Validation Checklist
- [x] ADB commands execute without errors
- [x] Logcat shows clear success/failure messages
- [x] Seeded data appears in Foodie app immediately
- [x] Cleanup removes all test data
- [x] No notifications or UI interruptions during operations
- [x] Scripts run to completion with clear output

## Dev Notes

### Architecture Alignment

**Module Structure:**
- FoodieSeeder is a **separate Android application module** (not a library)
- Uses different applicationId: `com.foodie.seeder` (vs main app `com.foodie.app`)
- Shares Health Connect data store with main app (same data, different apps)
- Emulator-only deployment (never installed on production devices)

**BroadcastReceiver Pattern:**
- Standard Android broadcast pattern for IPC (Inter-Process Communication)
- Receivers wake up on demand when broadcast is sent
- No service or long-running process needed
- Minimal battery/performance impact

**Health Connect Integration:**
- Uses same `androidx.health.connect:connect-client` SDK as main app
- Requires same permissions: `READ_NUTRITION`, `WRITE_NUTRITION`
- Writes to shared Health Connect data store (cross-app visibility)
- Follows same data model: `NutritionRecord` with `energy` and `name` fields

### Testing Strategy

**Why This Story Is Critical:**
- Current blocker: Maestro E2E tests cannot guarantee clean Health Connect state
- Health Connect has no CLI for data manipulation
- Manual UI taps are slow, brittle, and non-deterministic
- This tool enables true E2E test automation with deterministic setup/teardown

**Integration with Existing Tests:**
- Maestro tests in `.maestro/` folder can now have clean state
- Each test gets fresh data via seed commands
- Tests can be run in parallel (after cleanup between runs)
- Scripts orchestrate entire test suite automatically

### Project Structure Notes

```
foodie/
├── app/                          # Main Foodie application
├── foodieseeder/                 # NEW: Test data seeder tool
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/foodie/seeder/
│   │   │   │   ├── SeedNutritionReceiver.kt
│   │   │   │   ├── CleanNutritionReceiver.kt
│   │   │   │   └── SeederPermissionActivity.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/foodie/seeder/
│   │           └── ValidationTest.kt
│   ├── build.gradle.kts
│   └── README.md
├── scripts/                      # NEW: Test automation scripts
│   ├── verify-seeder.sh         # Seeder validation script
│   └── run-e2e-tests.sh         # E2E test orchestration
├── .maestro/                     # Existing Maestro tests
└── settings.gradle.kts           # Updated to include :foodieseeder
```

### ADB Broadcast Command Reference

**Seed Nutrition Data:**
```bash
# Basic usage
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION \
  --es description "Lunch" --ed calories 650.0

# With timestamp (Unix milliseconds)
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION \
  --es description "Yesterday's Dinner" \
  --ed calories 800.0 \
  --el timestamp 1733011200000

# Multiple meals (run multiple times)
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION --es description "Breakfast" --ed calories 300.0
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION --es description "Lunch" --ed calories 600.0
adb shell am broadcast -a com.foodie.seeder.WRITE_NUTRITION --es description "Dinner" --ed calories 900.0
```

**Clean All Nutrition Data:**
```bash
# Deletes all records from last 7 days
adb shell am broadcast -a com.foodie.seeder.DELETE_ALL_NUTRITION
```

**Check Logcat for Confirmation:**
```bash
# Filter for seeder logs
adb logcat -s FoodieSeeder:* AndroidRuntime:E
```

### Troubleshooting Guide

**Issue: Broadcast not received**
- Ensure seeder app is installed: `adb shell pm list packages | grep seeder`
- Reinstall if needed: `make install-seeder`
- Check app is not force-stopped (broadcasts don't wake force-stopped apps)

**Issue: "Health Connect not available"**
- Install Health Connect from Play Store on emulator
- Verify installed: `adb shell pm list packages | grep healthconnect`

**Issue: Permission errors**
- Launch seeder app UI and grant Health Connect permissions
- Check permissions: Settings → Apps → FoodieSeeder → Permissions

**Issue: Data not appearing in main app**
- Wait 2-3 seconds for Health Connect sync
- Pull-to-refresh in Foodie app meal list
- Check Health Connect app directly to confirm data exists

### References

- **BroadcastReceiver**: [Android Developer Docs](https://developer.android.com/reference/android/content/BroadcastReceiver)
- **Health Connect SDK**: [Developer Guide](https://developer.android.com/health-and-fitness/guides/health-connect)
- **ADB Broadcast Commands**: [ADB Reference](https://developer.android.com/tools/adb#am)
- **Architecture Pattern**: [Source: docs/architecture.md#Health-Connect-Integration]
- **Epic 6 Test Coverage**: [Source: docs/epics.md#Story-6-8-E2E-Test-Suite-Validation]
- **Maestro E2E Tests**: [Source: .maestro/ directory]

### Learnings from Previous Story

**From Story 6.8 (E2E Test Suite Validation):**

This story directly addresses the blocker identified in Story 6.8: inability to guarantee clean Health Connect state for deterministic E2E testing.

**Key Takeaways:**
- Maestro tests validated but limited by manual data setup
- Health Connect has no CLI for programmatic data manipulation
- Need reliable setup/teardown for repeatable test runs
- ADB broadcast pattern provides solution without modifying main app

**Why BroadcastReceiver Approach:**
- No modification to main Foodie app required
- Keeps test tooling separate from production code
- Enables automation without UI interaction
- Works even when seeder app is not actively running

[Source: stories/6-8-e2e-test-suite-validation.md#Dev-Agent-Record]

## Dev Agent Record

### Context Reference

<!-- Path(s) to story context XML will be added here by context workflow -->

### Agent Model Used

<!-- Will be filled by dev agent -->

### Debug Log References

<!-- Will be filled by dev agent during implementation -->

### Completion Notes List

<!-- Dev agent fills this section with:
- New patterns/services created
- Architectural decisions made
- Technical debt deferred
- Warnings for next story
- Key learnings
-->

### File List

<!-- Dev agent fills this section with:
- NEW: files created
- MODIFIED: files changed
- DELETED: files removed
-->

## Change Log

| Date | Author | Change |
|------|--------|--------|
| 2025-11-30 | SM Agent (Bob) | Story drafted - FoodieSeeder sidecar tool for E2E test data management via ADB broadcasts |
