# Story 2-1: Fix Hilt + Compose Test Infrastructure

**Status**: ready-for-dev  
**Epic**: Epic 2 - Meal Capture Flow  
**Story Points**: 5  
**Priority**: HIGH (Blocking - Must fix before continuing Epic 2)  
**Created**: 2025-11-09  
**Sprint**: Current

---

## Story

**As a** developer  
**I want** instrumentation tests for Compose screens with Hilt ViewModels to execute successfully  
**So that** we can verify navigation, deep linking, and screen behavior with automated tests instead of manual validation

## Background

During Story 2-0 (Deep Linking Validation), we discovered that 31 instrumentation tests fail to execute due to a Hilt + Compose testing architecture incompatibility. The deep links work perfectly in production (validated via adb), but we cannot write automated tests for them.

**Root Cause**: Composables using `hiltViewModel()` require an Activity annotated with `@AndroidEntryPoint`, but `createComposeRule()` creates a basic `ComponentActivity` that doesn't have Hilt support.

**Impact**: 
- NavGraphTest: 9 tests blocked
- DeepLinkTest: 15 tests blocked  
- MealListScreenTest: 7 tests blocked
- **Total**: 31 tests cannot execute

**Current Workaround**: Manual validation via adb commands for deep links

## Problem Statement

### Failed Tests Location
```
app/src/androidTest/java/com/foodie/app/ui/navigation/NavGraphTest.kt (9 tests)
app/src/androidTest/java/com/foodie/app/ui/navigation/DeepLinkTest.kt (15 tests)
app/src/androidTest/java/com/foodie/app/ui/screens/meallist/MealListScreenTest.kt (7 tests)
```

### Error Message
```
java.lang.IllegalStateException: Given component holder class androidx.activity.ComponentActivity 
does not implement interface dagger.hilt.internal.GeneratedComponent or interface 
dagger.hilt.internal.GeneratedComponentManager
    at dagger.hilt.EntryPoints.get(EntryPoints.java:62)
```

### What We Tried (All Failed)
1. **`createComposeRule()` with `@HiltAndroidTest`** → Creates basic ComponentActivity, not Hilt-enabled
2. **`createAndroidComposeRule<MainActivity>()`** → Activity already sets content in onCreate(), cannot call setContent() in tests
3. **`createAndroidComposeRule<HiltTestActivity>()`** → Package/process mismatch errors

## Acceptance Criteria

### AC #1: Test Infrastructure Works
**Given** a Compose screen using `hiltViewModel()`  
**When** I write an instrumentation test using the proper test pattern  
**Then** the test executes successfully with Hilt dependency injection working

### AC #2: All Blocked Tests Pass
**Given** the test infrastructure is fixed  
**When** I run `./gradlew :app:connectedDebugAndroidTest`  
**Then** all 31 previously blocked tests execute and pass

### AC #3: Documentation Updated
**Given** the solution is implemented  
**When** developers write new Compose + Hilt tests  
**Then** clear documentation and examples guide them to use the correct pattern

## Technical Research

### Official Documentation Sources

#### 1. Jetpack Compose Testing
**URL**: https://developer.android.com/develop/ui/compose/testing  
**Key Sections**:
- "Set up your test" - When to use `createComposeRule` vs `createAndroidComposeRule`
- Rule selection guidance for different testing scenarios

**Critical Quote**:
> "use createAndroidComposeRule<YourActivity>() if you need access to an activity"

#### 2. Compose Test Rules API Reference
**URL**: https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary  
**Key APIs**:
- `createComposeRule()` - Activity-less testing, you call `setContent()`
- `createAndroidComposeRule<A>()` - Uses real Activity, DON'T call `setContent()` if Activity already sets it

#### 3. Hilt Testing Guide
**URL**: https://developer.android.com/training/dependency-injection/hilt-testing  
**Key Sections**:
- UI test setup with `@HiltAndroidTest`
- `HiltAndroidRule` usage
- `@BindValue` for test dependencies
- Multiple TestRule objects ordering

**Critical Quote**:
> "You must annotate any UI test that uses Hilt with @HiltAndroidTest"

#### 4. Hilt + Compose Integration
**URL**: https://developer.android.com/develop/ui/compose/libraries#hilt  
**Key Section**: "Hilt and Navigation"

**Critical Quote**:
> "When using Navigation Compose, always use the hiltViewModel composable function to obtain an instance of your @HiltViewModel annotated ViewModel. **This works with fragments or activities that are annotated with @AndroidEntryPoint.**"

**This confirms**: `hiltViewModel()` REQUIRES `@AndroidEntryPoint` Activity

#### 5. Architecture Samples (Official Reference Implementation)
**URL**: https://github.com/android/architecture-samples  
**Branch**: main (Compose + Hilt implementation)
**Key Files to Study**:
- Test setup patterns for Compose + Hilt
- How they test screens with `hiltViewModel()`
- ViewModel injection in tests

## Solution Options

### Option A: Refactor Tests to Provide Test ViewModels (RECOMMENDED)
**Approach**: Stop using `hiltViewModel()` in tests, provide fake/test ViewModels explicitly

**Pattern**:
```kotlin
@Test
fun myTest() {
    val fakeViewModel = FakeMealListViewModel()
    
    composeTestRule.setContent {
        MealListScreen(
            onMealClick = {},
            onSettingsClick = {},
            viewModel = fakeViewModel  // Explicit test ViewModel
        )
    }
    
    // Test assertions
}
```

**Pros**:
- ✅ Screens already designed for this (ViewModel as parameter with default)
- ✅ Complete test isolation
- ✅ Fast test execution
- ✅ Full control over ViewModel state
- ✅ Official Android best practice

**Cons**:
- ⚠️ Requires creating fake ViewModels
- ⚠️ Requires creating fake repositories
- ⚠️ More test infrastructure setup

**Effort**: Medium (need to create fakes, but proper long-term solution)

### Option B: Use ActivityScenario for Integration Tests
**Approach**: Test the full Activity with real Hilt injection

**Pattern**:
```kotlin
@HiltAndroidTest
class MealListIntegrationTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @Test
    fun testFullFlow() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        // Use Espresso or Compose testing APIs to interact
        // Cannot inject TestNavHostController
    }
}
```

**Pros**:
- ✅ Tests real Hilt injection
- ✅ Integration test coverage

**Cons**:
- ⚠️ Cannot inject TestNavHostController for navigation assertions
- ⚠️ Slower test execution
- ⚠️ Less control over state

**Effort**: Low (change test approach)

### Option C: Create Hybrid Approach
**Approach**: Use Option A for unit tests, Option B for integration tests

**Pros**:
- ✅ Best of both worlds
- ✅ Unit tests fast and isolated
- ✅ Integration tests verify real behavior

**Cons**:
- ⚠️ More tests to maintain

**Effort**: Medium-High

## Tasks

### Task 1: Research Architecture Samples Implementation
- [ ] Clone https://github.com/android/architecture-samples
- [ ] Study test setup in androidTest folder
- [ ] Identify patterns for testing Compose + Hilt screens
- [ ] Document findings in this story

### Task 2: Create Test Infrastructure
- [ ] Create `FakeMealListViewModel` in `app/src/androidTest/.../fakes/`
- [ ] Create `FakeHealthConnectManager` for ViewModel dependency
- [ ] Create test data builders/factories
- [ ] Document fake creation pattern for future ViewModels

### Task 3: Update NavGraphTest (9 tests)
- [ ] Refactor to use `createComposeRule()` with explicit ViewModels
- [ ] Update all 9 test cases
- [ ] Verify all tests pass

### Task 4: Update DeepLinkTest (15 tests)
- [ ] Refactor to use explicit ViewModels
- [ ] Update all 15 test cases
- [ ] Verify all tests pass

### Task 5: Update MealListScreenTest (7 tests)
- [ ] Refactor to use `FakeMealListViewModel`
- [ ] Update all 7 test cases
- [ ] Verify all tests pass

### Task 6: Create Testing Documentation
- [ ] Create `docs/testing/compose-hilt-testing-guide.md`
- [ ] Document correct patterns for future tests
- [ ] Include code examples
- [ ] Link to official documentation

### Task 7: Verification
- [ ] Run full test suite: `./gradlew :app:connectedDebugAndroidTest`
- [ ] Verify all 31 tests pass
- [ ] Verify no new tests added to regression
- [ ] Update Story 2-0 documentation to reference this fix

## Definition of Done

- [ ] All 31 blocked instrumentation tests execute and pass
- [ ] Test infrastructure (fakes, builders) created and documented
- [ ] Testing guide created for future developers
- [ ] No manual validation required for deep links
- [ ] Code reviewed and approved
- [ ] Documentation updated

## Links & References

### Official Documentation
- [Compose Testing Overview](https://developer.android.com/develop/ui/compose/testing)
- [Compose Test Rules API](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary)
- [Hilt Testing Guide](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Hilt + Compose Integration](https://developer.android.com/develop/ui/compose/libraries#hilt)
- [Architecture Samples (Reference)](https://github.com/android/architecture-samples)

### Related Stories
- Story 2-0: Deep Linking Validation (where regression was discovered)

### Technical Notes
- Screens are already designed correctly: `viewModel: MealListViewModel = hiltViewModel()`
- This is a test infrastructure issue, not a production bug
- Deep links verified working via manual adb testing

## Dev Agent Record

### Context Reference
- Story: 2-0-deep-linking-validation.md
- Story Context: 2-0-deep-linking-validation.context.xml

### Progress Tracking
- Status: ready-for-dev
- Blocked By: None
- Blocking: Epic 2 Story 2.1 (Lock Screen Widget)

### Notes
- This is a blocking story - must be completed before Epic 2 continues
- High priority due to test regression impact
- Manual validation confirmed production works, so not a critical production bug
- Good opportunity to establish proper test patterns for the project
