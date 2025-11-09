# Story 2.0: Deep Linking Validation

Status: backlog

## Story

As a developer,
I want to validate navigation deep linking works correctly in realistic meal capture scenarios,
So that Epic 2 meal capture flow triggers (widget, FAB) integrate seamlessly without navigation regressions.

## Acceptance Criteria

1. Deep links to MealListScreen work correctly from external triggers
2. Deep links to MealDetailScreen with meal ID parameter work correctly
3. Navigation back stack behaves correctly after deep link navigation
4. Deep linking works when app is in background, foreground, or killed states
5. All existing navigation tests continue to pass (no regressions)
6. Deep link URIs follow Android best practices and architecture.md patterns

## Tasks / Subtasks

- [ ] **Task 1: Review existing deep linking implementation** (AC: #1, #2, #6)
  - [ ] Read Story 1-3 completion document for deep linking patterns
  - [ ] Review `NavGraph.kt` for current deep link configurations
  - [ ] Review architecture.md for deep linking URI patterns
  - [ ] Document current deep link URIs in Dev Notes

- [ ] **Task 2: Create deep link test utilities** (AC: #1, #2, #3, #4)
  - [ ] Create `DeepLinkTestHelper.kt` in `test/util/` package
  - [ ] Add helper functions: `createDeepLinkIntent(uri)`, `launchAppWithDeepLink(uri, state)`
  - [ ] Add helper for back stack verification: `assertBackStackContains(destinations)`
  - [ ] Write unit tests for deep link test helpers

- [ ] **Task 3: Test deep link to MealListScreen** (AC: #1, #3, #4)
  - [ ] Write instrumentation test: app in killed state → deep link → MealListScreen shown
  - [ ] Write instrumentation test: app in background → deep link → MealListScreen navigates
  - [ ] Write instrumentation test: app in foreground on different screen → deep link → MealListScreen navigates
  - [ ] Verify back stack after deep link (should have MealListScreen only, no previous screens)
  - [ ] Test deep link with `foodie://meals` URI
  - [ ] All tests must pass

- [ ] **Task 4: Test deep link to MealDetailScreen** (AC: #2, #3, #4)
  - [ ] Create test meal entry in Health Connect for testing
  - [ ] Write instrumentation test: app in killed state → deep link with meal ID → MealDetailScreen shown
  - [ ] Write instrumentation test: app in background → deep link with meal ID → MealDetailScreen navigates
  - [ ] Write instrumentation test: app in foreground → deep link with meal ID → MealDetailScreen navigates
  - [ ] Verify back stack after deep link (should have MealListScreen → MealDetailScreen)
  - [ ] Test deep link with `foodie://meals/{mealId}` URI
  - [ ] Test error handling: invalid meal ID → show error, navigate to MealListScreen
  - [ ] All tests must pass

- [ ] **Task 5: Test deep link error scenarios** (AC: #6)
  - [ ] Test malformed URI → app handles gracefully (no crash)
  - [ ] Test invalid meal ID → error message shown, navigate to safe screen
  - [ ] Test deep link while permissions missing → handle gracefully
  - [ ] Verify logging occurs for all error scenarios (Timber.e)
  - [ ] All error tests must pass

- [ ] **Task 6: Validate Epic 2 integration readiness** (AC: #1, #3)
  - [ ] Document deep link URI patterns for Epic 2 stories in Dev Notes
  - [ ] Verify widget can launch app via deep link (create placeholder widget test intent)
  - [ ] Verify FAB can trigger navigation via deep link (simulate FAB action)
  - [ ] Document any changes needed for Epic 2 meal capture flow
  - [ ] Add recommendations to Dev Notes for Epic 2 stories

- [ ] **Task 7: Regression testing** (AC: #5)
  - [ ] Run all existing Story 1-3 navigation tests
  - [ ] Verify all 33 navigation tests still pass
  - [ ] Run full test suite: `./gradlew test connectedAndroidTest`
  - [ ] Fix any test failures
  - [ ] Document any navigation pattern improvements in Dev Notes

- [ ] **Task 8: Documentation and completion** (AC: All)
  - [ ] Update Dev Notes with deep link URI patterns
  - [ ] Document test coverage summary (number of deep link tests added)
  - [ ] Document learnings: deep link best practices, gotchas, recommendations for Epic 2
  - [ ] Update Dev Agent Record with completion notes
  - [ ] Add Change Log entry summarizing validation results

## Definition of Done

This story is considered COMPLETE only when ALL of the following are satisfied:

### Implementation & Quality
- [ ] All acceptance criteria are met with verified evidence (file:line references)
- [ ] All tasks and subtasks are completed and checked off
- [ ] Code follows project architecture patterns and conventions
- [ ] All new/modified code has appropriate error handling
- [ ] Code is reviewed (senior developer or AI code review workflow)

### Testing Requirements
- [ ] **Unit tests written** for all new business logic, repositories, ViewModels, domain models, and utility functions
- [ ] **All unit tests passing** (`./gradlew test` executes successfully with zero failures)
- [ ] **Instrumentation tests written** IF this story involves:
  - User-facing UI workflows or navigation flows ✅ (deep linking is navigation)
  - Android platform API integration (Camera, Health Connect, Permissions, etc.)
  - End-to-end user flows spanning multiple components ✅ (deep link flow)
  - Complex user interactions requiring device/emulator validation ✅
- [ ] **All instrumentation tests passing** (`./gradlew connectedAndroidTest` succeeds, if applicable)
- [ ] No test coverage regressions (existing tests still pass)

### Documentation
- [ ] Inline code documentation (KDocs) added for public APIs and complex logic
- [ ] README or relevant docs updated if new features/patterns introduced
- [ ] Dev Notes section includes implementation learnings and references

### Story File Completeness
- [ ] Dev Agent Record updated with completion notes and file list
- [ ] Change Log entry added summarizing what was implemented
- [ ] Story status updated to "review" (pending approval) or "done" (if approved)

**Testing Standards Summary:**
- **Unit Tests Required:** Always, for any story with business logic or data handling
- **Instrumentation Tests Required:** Conditional - only for UI flows, platform integration, or E2E scenarios
- **Test Naming Convention:** `methodName_whenCondition_thenExpectedResult` or `feature should behavior when condition`
- **Assertion Library:** Truth library for readable assertions (`assertThat(x).isEqualTo(y)`)
- **Mocking:** Use Mockito/Mockito-Kotlin for dependency mocking in unit tests

## User Demo

**Purpose**: Validate deep linking works for stakeholders without technical knowledge.

### Prerequisites
- Android emulator or device running the app
- App installed and Health Connect permissions granted
- At least one test meal entry in Health Connect (can use data from Story 1-4 tests)

### Demo Steps

**Demo 1: Deep Link to Meal List**
1. Close the app completely (swipe away from recent apps)
2. From terminal, run: `adb shell am start -a android.intent.action.VIEW -d "foodie://meals"`
3. **Expected**: App launches directly to Meal List screen showing meal entries

**Demo 2: Deep Link to Meal Detail**
1. Close the app completely
2. Note a meal ID from the meal list (or use test meal ID from tests)
3. From terminal, run: `adb shell am start -a android.intent.action.VIEW -d "foodie://meals/{mealId}"`
4. **Expected**: App launches directly to Meal Detail screen for that specific meal

**Demo 3: Deep Link While App Running**
1. Open app and navigate to Settings screen (if it exists, or any screen other than Meal List)
2. From terminal, run: `adb shell am start -a android.intent.action.VIEW -d "foodie://meals"`
3. **Expected**: App navigates to Meal List screen from current location

### Expected Behavior
- No crashes or errors during deep link navigation
- Correct screen displayed based on URI
- Back button works correctly after deep link (closes app from MealListScreen)
- Deep links work from killed, background, and foreground app states

### Validation Checklist
- [ ] MealListScreen deep link works from killed app
- [ ] MealDetailScreen deep link works from killed app
- [ ] Deep link works when app is already running
- [ ] Back navigation behaves correctly
- [ ] No errors in logcat during deep link flow

## Dev Notes

### Purpose and Context

Story 2.0 is a **preparation story** created during Epic 1 retrospective to validate navigation deep linking before Epic 2 meal capture implementation begins. This ensures:

1. **No Regressions**: Deep linking tested in Epic 1 Story 1-3 continues to work
2. **Epic 2 Readiness**: Widget and FAB triggers in Epic 2 can rely on deep linking patterns
3. **Realistic Validation**: Tests simulate actual meal capture flow triggers (widget → app launch)
4. **Confidence**: Epic 2 stories can proceed without navigation uncertainty

### Deep Linking Architecture

**URI Patterns (from Story 1-3):**
- Meal List: `foodie://meals`
- Meal Detail: `foodie://meals/{mealId}`

**Implementation Location:**
- `ui/navigation/NavGraph.kt` - deep link configurations
- Uses Jetpack Navigation Compose deep link support

**Key Requirements:**
- URIs must follow Android deep link best practices
- Must work in killed, background, and foreground states
- Must handle invalid/malformed URIs gracefully
- Must maintain correct back stack behavior

### Testing Strategy

**Unit Tests:**
- Deep link test helper utilities
- URI parsing and validation logic

**Instrumentation Tests (Primary Focus):**
- Deep link from killed app state
- Deep link from background state
- Deep link from foreground state (different screen)
- Back stack verification after deep link
- Error handling for invalid URIs/IDs

**Regression Tests:**
- All 33 existing navigation tests from Story 1-3 must pass
- No changes to navigation code unless fixing bugs

### Epic 2 Integration Notes

**Stories that will use deep linking:**
- Story 2.1 (Lock Screen Widget): Will use `foodie://meals` to launch app
- Story 2.6 (End-to-End Flow): May use deep links for post-capture navigation
- Future stories: Any external trigger (notifications, shortcuts)

**Recommendations for Epic 2:**
- Widget should launch app with `foodie://meals` deep link
- FAB can use direct navigation (already in app) or deep link for consistency
- Consider deep link for post-analysis success notification (navigate to new meal detail)

### Project Structure Notes

**New Files:**
- `app/src/test/java/com/foodie/app/util/DeepLinkTestHelper.kt` - Test utilities
- `app/src/androidTest/java/com/foodie/app/ui/navigation/DeepLinkTest.kt` - Instrumentation tests

**Modified Files:**
- None expected (validation only, no implementation changes)

**Alignment with Project Structure:**
- Test files follow existing structure from Story 1-3
- Uses same testing patterns (JUnit, Espresso, Truth)
- Leverages existing navigation configuration

### Learnings from Previous Story

**From Story 1-3 (Core Navigation and Screen Structure):**

- **Deep Linking Setup**: Deep link configuration exists in `NavGraph.kt` for meal routes
- **Navigation Testing**: 33 navigation tests established testing patterns
- **Back Stack Management**: Navigation follows single-activity pattern with Compose navigation
- **Testing Approach**: Instrumentation tests use `composeTestRule` for navigation validation

[Source: docs/stories/1-3-core-navigation-and-screen-structure.md]

### References

- [Source: docs/architecture.md#Navigation]
- [Source: docs/stories/1-3-core-navigation-and-screen-structure.md]
- [Source: https://developer.android.com/jetpack/compose/navigation#deeplinks]
- [Source: docs/retrospectives/epic-1-retrospective.md#Action-Items] (AI-3: Create Story 2.0)

## Dev Agent Record

### Context Reference

<!-- Path(s) to story context XML will be added here by context workflow -->

### Agent Model Used

<!-- To be filled during implementation -->

### Debug Log References

<!-- To be filled during implementation -->

### Completion Notes List

<!-- To be filled during implementation -->

### File List

<!-- To be filled during implementation -->
