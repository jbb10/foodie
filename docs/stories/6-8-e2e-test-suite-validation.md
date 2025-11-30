# Story 6-8: E2E Test Suite Validation and Regression Coverage

**Epic:** Epic 6 - Energy Balance & Caloric Deficit Tracking
**Story ID:** 6-8
**Status:** drafted
**Priority:** High
**Estimated Effort:** Medium (2-3 hours)

## User Story

As a developer,
I want all E2E instrumentation tests running successfully with comprehensive coverage,
So that we have a reliable regression test suite and accurate test coverage metrics in SonarQube.

## Context

Throughout Epic 6 development, we've accumulated 23 instrumentation test files covering various user flows. This story consolidates and validates this test suite to ensure:
- All tests pass consistently (0 flaky tests)
- Full user flow coverage for Epic 6
- Integration with SonarQube for coverage metrics
- Tests serve as reliable regression protection

## Acceptance Criteria

### AC #1: Test Suite Execution
**Given** all 23 instrumentation test files exist
**When** I run `./gradlew connectedDebugAndroidTest`
**Then** all tests execute successfully with 0 failures
**And** test execution completes in under 5 minutes
**And** no flaky tests (tests pass consistently on multiple runs)

### AC #2: Epic 6 User Flow Coverage
**Given** Epic 6 includes 7 completed stories (6-1 through 6-7)
**When** I map instrumentation tests to user flows
**Then** every critical user flow has test coverage:
- User profile management (6-1)
- Energy balance calculations (6-2 through 6-5)
- Dashboard display and real-time updates (6-6)
- Historical day navigation (6-7)
**And** test coverage matrix is documented

### AC #3: SonarQube Integration
**Given** instrumentation tests execute successfully
**When** I run `./gradlew test connectedDebugAndroidTest jacocoTestReport sonar`
**Then** SonarQube includes instrumentation test coverage in the report
**And** code coverage percentage reflects both unit and instrumentation tests
**And** coverage metrics are visible in SonarQube dashboard
**And** any gaps in coverage are identified

### AC #4: Regression Test Documentation
**Given** the test suite is stable and comprehensive
**When** I document the regression test strategy
**Then** a test guide explains:
- How to run all tests locally
- How to run specific test suites (unit vs instrumentation)
- How to interpret test results
- How to add new tests following established patterns
**And** CI/CD integration is documented (if applicable)

### AC #5: Test Organization and Naming
**Given** 23 instrumentation test files exist
**When** I review test organization
**Then** all tests follow consistent naming conventions
**And** tests are grouped logically by feature/screen
**And** test method names clearly describe scenarios
**And** no duplicate or redundant test coverage

### AC #6: Test Data and Dependencies
**Given** tests may have external dependencies (Health Connect, permissions)
**When** I review test setup and teardown
**Then** all tests properly initialize required dependencies
**And** tests clean up after themselves (no test pollution)
**And** tests can run in any order (no interdependencies)
**And** mock/fake implementations are used appropriately

### AC #7: Performance Validation
**Given** instrumentation tests run on emulator/device
**When** I measure test execution time
**Then** total test suite execution time < 5 minutes
**And** individual test execution time < 30 seconds
**And** slow tests are identified and optimized if possible

### AC #8: Coverage Gaps Identified
**Given** current test suite coverage is measured
**When** I analyze coverage reports
**Then** any critical uncovered code paths are documented
**And** recommendations are made for additional tests (if needed)
**And** acceptable coverage threshold is defined (e.g., 80% line coverage)

## Technical Implementation

### Task 1: Test Suite Health Check
- [ ] Run `./gradlew connectedDebugAndroidTest` and document results
- [ ] Identify any failing tests and root cause
- [ ] Fix or document flaky tests
- [ ] Verify all 23 test files execute

### Task 2: Epic 6 User Flow Mapping
- [ ] Document all Epic 6 user flows (Stories 6-1 through 6-7)
- [ ] Map existing instrumentation tests to each flow
- [ ] Identify coverage gaps (flows without tests)
- [ ] Create test coverage matrix

### Task 3: SonarQube Coverage Integration
- [ ] Configure Gradle for instrumentation test coverage (JaCoCo)
- [ ] Run combined coverage report: `./gradlew test connectedDebugAndroidTest jacocoTestReport`
- [ ] Verify coverage data includes instrumentation tests
- [ ] Upload to SonarQube and validate metrics
- [ ] Document any configuration changes

### Task 4: Test Organization Review
- [ ] Review all 23 test files for naming consistency
- [ ] Ensure logical grouping (by screen/feature)
- [ ] Check for duplicate test coverage
- [ ] Refactor if needed for clarity

### Task 5: Test Reliability Validation
- [ ] Run test suite 3 times consecutively
- [ ] Document any flaky tests (inconsistent results)
- [ ] Fix flaky tests (timing issues, state pollution)
- [ ] Verify tests can run in any order

### Task 6: Test Data and Dependencies Audit
- [ ] Review test setup/teardown in all files
- [ ] Verify proper use of Hilt test components
- [ ] Check Health Connect mock/fake usage
- [ ] Ensure no test pollution between tests

### Task 7: Performance Profiling
- [ ] Measure total test suite execution time
- [ ] Identify slowest tests (> 10 seconds)
- [ ] Optimize slow tests if possible
- [ ] Document performance baseline

### Task 8: Coverage Analysis
- [ ] Generate JaCoCo coverage report
- [ ] Analyze line/branch coverage percentages
- [ ] Identify critical uncovered code
- [ ] Document coverage metrics

### Task 9: Regression Test Guide
- [ ] Create `docs/testing/REGRESSION_TESTING.md`
- [ ] Document how to run tests locally
- [ ] Document test suite organization
- [ ] Include troubleshooting guide
- [ ] Document CI/CD integration (if applicable)

### Task 10: Epic 6 Test Summary
- [ ] Update sprint-status.yaml with Story 6-8 completion
- [ ] Document final test count (unit + instrumentation)
- [ ] Document coverage percentage
- [ ] Mark Epic 6 ready for retrospective

## Definition of Done

- [ ] All 23 instrumentation tests pass consistently (3 consecutive runs)
- [ ] Total test execution time < 5 minutes
- [ ] Epic 6 user flow coverage matrix documented
- [ ] SonarQube coverage includes instrumentation tests
- [ ] Coverage metrics documented (unit + instrumentation)
- [ ] Regression test guide created
- [ ] No flaky tests
- [ ] No test pollution or interdependencies
- [ ] Test organization follows consistent patterns
- [ ] Sprint status updated

## Test Coverage Matrix (To Be Completed in Task 2)

### Epic 6 User Flows vs Instrumentation Tests

| Story | User Flow | Instrumentation Tests | Coverage Status |
|-------|-----------|----------------------|-----------------|
| 6-1 | User Profile Settings | SettingsScreenTest.kt | ✅ |
| 6-2 | BMR Calculation | (Unit tests sufficient) | ✅ |
| 6-3 | NEAT Calculation | (Unit tests sufficient) | ✅ |
| 6-4 | Active Energy | HealthConnectIntegrationTest.kt | ✅ |
| 6-5 | TDEE Calculation | (Unit tests sufficient) | ✅ |
| 6-6 | Energy Balance Dashboard | EnergyBalanceDashboardDateNavigationTest.kt | ✅ |
| 6-7 | Historical Navigation | EnergyBalanceDashboardDateNavigationTest.kt | ✅ |

### All Instrumentation Tests (23 files)

**Energy Balance (Epic 6):**
- EnergyBalanceDashboardDateNavigationTest.kt ✅

**Settings (Epic 5):**
- SettingsScreenTest.kt ✅
- SettingsNavigationTest.kt ✅
- ApiConfigurationInstrumentationTest.kt ✅

**Meal Management (Epic 3):**
- MealListScreenTest.kt ✅
- MealListScreenDeleteTest.kt ✅
- MealDetailScreenTest.kt ✅

**Capture Flow (Epic 2):**
- CapturePhotoEdgeCasesTest.kt ✅
- MealCaptureWidgetInstrumentationTest.kt ✅

**Health Connect Integration:**
- HealthConnectIntegrationTest.kt ✅
- HealthConnectUpdateIntegrationTest.kt ✅
- HealthConnectPermissionFlowTest.kt ✅
- HealthConnectUnavailableDialogTest.kt ✅
- HealthConnectHiltTest.kt ✅

**Background Processing (Epic 2):**
- AnalyzeMealWorkerForegroundTest.kt ✅
- MealAnalysisForegroundNotifierTest.kt ✅
- PhotoCleanupWorkerTest.kt ✅
- FoodieApplicationWorkManagerTest.kt ✅

**Repository Integration:**
- MealRepositoryDeleteIntegrationTest.kt ✅
- NutritionAnalysisRepositoryImplIntegrationTest.kt ✅

**Infrastructure:**
- CredentialMigrationTest.kt ✅
- PhotoManagerCacheStatsTest.kt ✅
- ExampleInstrumentedTest.kt (cleanup candidate) ⚠️

## SonarQube Configuration Notes

### Current Configuration (sonar-project.properties)
```properties
sonar.coverage.jacoco.xmlReportPaths=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
```

### Required Gradle Configuration
JaCoCo must be configured to include both:
- Unit test coverage (test task)
- Instrumentation test coverage (connectedDebugAndroidTest task)

### Coverage Report Generation
```bash
./gradlew test connectedDebugAndroidTest jacocoTestReport
./gradlew sonar
```

## Success Metrics

- **Test Pass Rate:** 100% (0 failures, 0 flaky tests)
- **Execution Time:** < 5 minutes total
- **Coverage:** Unit (80%+) + Instrumentation combined
- **Epic 6 Flow Coverage:** 100% (all critical flows tested)
- **Test Reliability:** 100% pass rate over 3 consecutive runs

## Dependencies

- Stories 6-1 through 6-7 completed ✅
- Existing instrumentation test infrastructure ✅
- JaCoCo Gradle plugin configured ✅
- SonarQube server access ✅

## Risks and Mitigations

**Risk:** Instrumentation tests may not contribute to SonarQube coverage
**Mitigation:** Verify JaCoCo configuration includes connectedDebugAndroidTest, update if needed

**Risk:** Flaky tests due to emulator timing
**Mitigation:** Add proper synchronization (composeTestRule.waitForIdle), increase timeouts if needed

**Risk:** Long test execution time (> 5 minutes)
**Mitigation:** Profile tests, parallelize if possible, optimize slow tests

## Out of Scope

- Creating new instrumentation tests (use existing 23 files)
- UI/UX improvements to tested screens
- Adding new test frameworks (stick with existing JUnit4/Compose Testing)
- Performance testing beyond execution time measurement
- Cross-device testing (focus on single emulator/device)

## Notes

- This story focuses on **validation and integration** of existing tests
- If coverage gaps are found, document them for future stories
- The goal is regression protection, not 100% coverage
- Focus on Epic 6 flows, but validate all 23 test files
- SonarQube integration may require Gradle configuration updates

## Manual Testing Scenarios

### Scenario 1: Full Test Suite Execution
1. Start emulator: `make run-emulator`
2. Run tests: `./gradlew connectedDebugAndroidTest`
3. Verify: All tests pass (check terminal output)
4. Verify: Test report generated at `app/build/reports/androidTests/connected/debug/index.html`
5. Review: Any failures or flaky tests

**Expected:** 100% pass rate, clear test report

### Scenario 2: Coverage Report Generation
1. Run: `./gradlew clean test connectedDebugAndroidTest jacocoTestReport`
2. Verify: JaCoCo report generated at `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
3. Open: HTML report at `app/build/reports/jacoco/jacocoTestReport/html/index.html`
4. Verify: Coverage includes instrumentation test data

**Expected:** Combined coverage report with unit + instrumentation data

### Scenario 3: SonarQube Upload
1. Run: `./gradlew sonar`
2. Verify: Coverage data uploaded to SonarQube
3. Check: SonarQube dashboard shows updated coverage percentage
4. Verify: Instrumentation tests contribute to coverage metrics

**Expected:** SonarQube reflects combined test coverage

### Scenario 4: Test Reliability Check
1. Run: `./gradlew connectedDebugAndroidTest` (Run 1)
2. Run: `./gradlew connectedDebugAndroidTest` (Run 2)
3. Run: `./gradlew connectedDebugAndroidTest` (Run 3)
4. Compare: Test results across all 3 runs
5. Identify: Any tests with inconsistent results

**Expected:** Identical results across all 3 runs (0 flaky tests)

### Validation Checklist
- [ ] All 23 instrumentation tests pass
- [ ] Test suite executes in < 5 minutes
- [ ] JaCoCo report includes instrumentation coverage
- [ ] SonarQube dashboard shows updated coverage
- [ ] No flaky tests (3 consecutive runs identical)
- [ ] Epic 6 user flows have test coverage
- [ ] Regression test guide created
- [ ] Coverage metrics documented

## Dev Notes

### Test Execution Commands

```bash
# Run all instrumentation tests
./gradlew connectedDebugAndroidTest

# Run specific test file
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.foodie.app.ui.screens.energybalance.EnergyBalanceDashboardDateNavigationTest

# Run tests with coverage
./gradlew connectedDebugAndroidTest jacocoTestReport

# Generate combined coverage (unit + instrumentation)
./gradlew clean test connectedDebugAndroidTest jacocoTestReport

# Upload to SonarQube
./gradlew sonar
```

### Known Test Files (23 total)

1. EnergyBalanceDashboardDateNavigationTest.kt (12 tests)
2. SettingsScreenTest.kt
3. SettingsNavigationTest.kt
4. ApiConfigurationInstrumentationTest.kt
5. MealListScreenTest.kt
6. MealListScreenDeleteTest.kt
7. MealDetailScreenTest.kt
8. CapturePhotoEdgeCasesTest.kt
9. MealCaptureWidgetInstrumentationTest.kt
10. HealthConnectIntegrationTest.kt
11. HealthConnectUpdateIntegrationTest.kt
12. HealthConnectPermissionFlowTest.kt
13. HealthConnectUnavailableDialogTest.kt
14. HealthConnectHiltTest.kt
15. AnalyzeMealWorkerForegroundTest.kt
16. MealAnalysisForegroundNotifierTest.kt
17. PhotoCleanupWorkerTest.kt
18. FoodieApplicationWorkManagerTest.kt
19. MealRepositoryDeleteIntegrationTest.kt
20. NutritionAnalysisRepositoryImplIntegrationTest.kt
21. CredentialMigrationTest.kt
22. PhotoManagerCacheStatsTest.kt
23. ExampleInstrumentedTest.kt (candidate for removal)

### JaCoCo Configuration Status

Need to verify `app/build.gradle.kts` includes:
- JaCoCo plugin
- Task to merge unit + instrumentation coverage
- XML report generation for SonarQube

### Investigation Tasks

- [ ] Check if JaCoCo merges unit + instrumentation coverage
- [ ] Verify SonarQube reads combined coverage XML
- [ ] Test if instrumentation coverage shows in SonarQube dashboard
- [ ] Document any configuration changes needed
