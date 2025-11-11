# Testing Standards and Definition of Done

**Version:** 1.0  
**Last Updated:** 2025-11-09  
**Applies To:** All user stories in all epics

---

## Purpose

This document defines mandatory testing requirements and Definition of Done criteria for all user stories in the project. These standards ensure code quality, prevent regressions, and maintain a reliable codebase.

---

## Definition of Done

Every user story is considered **COMPLETE** only when ALL of the following criteria are satisfied:

### ✅ Implementation & Quality

- [ ] All acceptance criteria are met with verified evidence (file:line references)
- [ ] All tasks and subtasks are completed and checked off
- [ ] Code follows project architecture patterns and conventions (MVVM, Repository pattern, etc.)
- [ ] All new/modified code has appropriate error handling with `Result<T>` wrapper pattern
- [ ] Code is reviewed (senior developer or AI code review workflow)
- [ ] No compiler warnings or lint errors introduced

### ✅ Testing Requirements

#### **Unit Tests - MANDATORY for Every Story**

- [ ] **Unit tests written** for all new/modified:
  - Business logic and use cases
  - Repository implementations
  - ViewModels and state management
  - Domain models with validation
  - Utility functions and extensions
  - Data transformations and mappers

- [ ] **All unit tests passing** - `./gradlew test` executes successfully with **zero failures**

- [ ] **Test coverage** - No coverage regressions (all existing tests still pass)

#### **Instrumentation Tests - CONDITIONAL**

Instrumentation tests are **REQUIRED** when the story involves:

1. **UI Workflows & Navigation**
   - Screen-to-screen navigation flows
   - User interaction sequences
   - Dialog and bottom sheet interactions
   - Widget and lock screen components

2. **Android Platform APIs**
   - Camera integration
   - Health Connect operations
   - Permission request flows
   - File system operations
   - Background services and WorkManager

3. **End-to-End User Flows**
   - Complete user journeys spanning multiple screens
   - Integration between UI → ViewModel → Repository → Data Source
   - Scenarios requiring actual device/emulator validation

- [ ] **Instrumentation tests written** (if applicable based on above criteria)
- [ ] **All instrumentation tests passing** - `./gradlew connectedAndroidTest` succeeds (if applicable)

#### **Physical Device Testing - CONDITIONAL** ⚠️ ADDED FROM EPIC 2 RETROSPECTIVE (AI-5)

Physical device testing is **REQUIRED** when the story involves:

1. **WorkManager & Background Processing**
   - WorkManager jobs and worker implementations
   - Background task execution and retry logic
   - Work constraints and scheduling behavior
   - **Rationale:** Emulator WorkManager caching can mask real behavior (Epic 2 Story 2-5 discovery)

2. **Notifications & Foreground Services**
   - Notification channel creation and display
   - Notification permission flows (Android 13+)
   - Foreground service notifications
   - Silent vs. visible notification behavior

3. **Performance-Sensitive Features**
   - Camera launch timing and responsiveness
   - API call latency and timeout behavior
   - UI animation smoothness (Material 3 transitions)
   - Large data set rendering performance

4. **Platform-Specific Behavior**
   - Deep linking from widgets or external apps
   - Permission request flows (camera, notifications, Health Connect)
   - Hardware interactions (camera, sensors)
   - System integration points

- [ ] **Physical device testing completed** (if applicable based on above criteria)
- [ ] **Device model & Android version documented** in Dev Notes (e.g., "Pixel 8 Pro, Android 16")
- [ ] **Real-world timing metrics captured** where applicable (e.g., "< 3s wake-to-camera")
- [ ] **Platform-specific behaviors validated** (e.g., notification permission, foreground service)

**Emulator Testing Autonomy:** ✅ ADDED FROM EPIC 2 RETROSPECTIVE (AI-2)

Developers have **full autonomy** to boot emulator when needed for testing. 10-second emulator startup delays are **acceptable** for quality validation. No permission required - use your judgment.

#### **Test Quality Standards**

- [ ] Tests follow naming convention: `methodName_whenCondition_thenExpectedResult` or `feature should behavior when condition`
- [ ] Tests use Truth library assertions for readability: `assertThat(actual).isEqualTo(expected)`
- [ ] Tests properly mock dependencies using Mockito/Mockito-Kotlin
- [ ] Tests use `runTest` from kotlinx-coroutines-test for suspend functions
- [ ] Tests are isolated (no dependencies between tests, no shared mutable state)

### ✅ Documentation

- [ ] **Inline code documentation** - KDocs added for all public APIs and complex logic
- [ ] **README updates** - Project documentation updated if new features/patterns introduced
- [ ] **Dev Notes populated** - Story Dev Notes section includes:
  - Implementation learnings and key decisions
  - References to architecture documents with [Source: path#section] format
  - Known limitations or technical debt notes
  - Patterns/services created for reuse by future stories

### ✅ Story File Completeness

- [ ] **Dev Agent Record updated** with:
  - Context reference (path to .context.xml file)
  - Agent model used
  - Debug log references (if any issues encountered)
  - Completion notes summarizing what was implemented
  
- [ ] **File List section updated** with ALL files:
  - Created files (NEW)
  - Modified files (MODIFIED)
  - Deleted files (DELETED)
  - Paths relative to repository root

- [ ] **Change Log entry added** with:
  - Date and agent/developer name
  - Brief summary of what was implemented
  - Status transitions (backlog → drafted → ready → in-progress → review → done)

- [ ] **Story status updated** to:
  - "review" (if pending code review)
  - "done" (if approved and all DoD criteria met)

---

## Testing Framework and Tools

### Unit Testing Stack

- **Test Runner:** JUnit 4.13.2
- **Assertions:** Truth library (com.google.truth:truth)
- **Mocking:** Mockito 5.14.2, Mockito-Kotlin 5.4.0
- **Coroutines Testing:** kotlinx-coroutines-test
- **Location:** `app/src/test/java/com/foodie/app/`

### Instrumentation Testing Stack

- **Test Runner:** AndroidJUnit4
- **UI Testing:** Jetpack Compose UI Test (androidx.compose.ui:ui-test-junit4)
- **Assertions:** Truth library + Compose test assertions
- **Hilt Testing:** Hilt Android Testing (com.google.dagger:hilt-android-testing)
- **Location:** `app/src/androidTest/java/com/foodie/app/`

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run unit tests with coverage report
./gradlew test jacocoTestReport

# Run instrumentation tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.foodie.app.data.repository.MealRepositoryImplTest"

# Run all tests (unit + instrumentation)
./gradlew test connectedAndroidTest
```

---

## Test Patterns and Examples

### Unit Test Pattern - Repository

```kotlin
@Test
fun `getMealHistory should return success with meal entries when Health Connect query succeeds`() = runTest {
    // Given
    val mockRecords = listOf(/* mock NutritionRecord data */)
    whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
        .thenReturn(mockRecords)
    
    // When
    val result = mealRepository.getMealHistory()
    
    // Then
    assertThat(result).isInstanceOf(Result.Success::class.java)
    assertThat((result as Result.Success).data).hasSize(mockRecords.size)
    verify(healthConnectDataSource).queryNutritionRecords(any(), any())
}

@Test
fun `getMealHistory should return error when Health Connect throws exception`() = runTest {
    // Given
    whenever(healthConnectDataSource.queryNutritionRecords(any(), any()))
        .thenThrow(SecurityException("Permission denied"))
    
    // When
    val result = mealRepository.getMealHistory()
    
    // Then
    assertThat(result).isInstanceOf(Result.Error::class.java)
    assertThat((result as Result.Error).exception).isInstanceOf(SecurityException::class.java)
}
```

### Unit Test Pattern - ViewModel

```kotlin
@Test
fun `loadData should update state to loading then success when repository returns data`() = runTest {
    // Given
    val mockData = listOf(/* mock domain models */)
    whenever(repository.getData()).thenReturn(Result.Success(mockData))
    
    // When
    viewModel.loadData()
    advanceUntilIdle() // Process all coroutines
    
    // Then
    assertThat(viewModel.state.value.isLoading).isFalse()
    assertThat(viewModel.state.value.data).isEqualTo(mockData)
    assertThat(viewModel.state.value.error).isNull()
}
```

### Instrumentation Test Pattern - Navigation

```kotlin
@Test
fun navigationToDetailScreen_displaysCorrectMealId() {
    // Given
    val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    navController.navigatorProvider.addNavigator(ComposeNavigator())
    val mealId = "test-meal-123"
    
    // When
    composeTestRule.setContent {
        NavHost(navController, startDestination = "list") {
            composable("list") {
                MealListScreen(onMealClick = { id -> 
                    navController.navigate("detail/$id")
                })
            }
            composable("detail/{mealId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("mealId")
                MealDetailScreen(mealId = id ?: "")
            }
        }
    }
    
    composeTestRule.onNodeWithText("Test Meal").performClick()
    
    // Then
    assertThat(navController.currentBackStackEntry?.destination?.route)
        .isEqualTo("detail/{mealId}")
    composeTestRule.onNodeWithText("Editing meal: $mealId").assertIsDisplayed()
}
```

### Instrumentation Test Pattern - Platform API

```kotlin
@Test
fun healthConnectRoundTrip_insertsAndRetrievesData() = runTest {
    // Given
    val calories = 500
    val description = "Test meal"
    val timestamp = Instant.now()
    
    // When - Insert
    val recordId = healthConnectManager.insertNutritionRecord(calories, description, timestamp)
    assertThat(recordId).isNotEmpty()
    
    // When - Query
    val records = healthConnectManager.queryNutritionRecords(
        startTime = timestamp.minus(1, ChronoUnit.HOURS),
        endTime = timestamp.plus(1, ChronoUnit.HOURS)
    )
    
    // Then
    val testRecord = records.find { it.metadata.id == recordId }
    assertThat(testRecord).isNotNull()
    assertThat(testRecord!!.energy.inKilocalories).isEqualTo(calories.toDouble())
    assertThat(testRecord.name).isEqualTo(description)
    
    // Cleanup
    healthConnectManager.deleteNutritionRecord(recordId)
}
```

---

## Story-Specific Testing Guidance

### When to Skip Instrumentation Tests

You may skip instrumentation tests ONLY for stories that:

1. **Pure configuration** (build setup, dependency updates)
2. **Internal utilities** (Result wrapper, extension functions)
3. **Simple data models** (domain models with only validation logic)
4. **Documentation-only changes**

**All other stories MUST have unit tests, and conditionally instrumentation tests based on the criteria above.**

### Test Coverage Targets

- **Unit Test Coverage:** Aim for 80%+ coverage of business logic (repositories, ViewModels, use cases)
- **Critical Paths:** 100% coverage for security-sensitive operations (API key handling, Health Connect permissions)
- **UI Tests:** Focus on happy paths and critical error scenarios (not exhaustive UI state testing)

### When Tests Can Be Mocked vs Real

**Use Mocks:**
- Unit tests for repositories (mock data sources)
- Unit tests for ViewModels (mock repositories)
- Unit tests for domain logic (mock dependencies)

**Use Real Implementations:**
- Instrumentation tests for Health Connect integration (real SDK)
- Instrumentation tests for Camera (real camera preview)
- E2E tests validating complete user flows

---

## Enforcement

### Automated Checks

- CI/CD pipeline runs `./gradlew test` on every commit (unit tests must pass)
- Pull requests require instrumentation test results (if applicable)
- Code coverage reports generated and tracked over time

### Code Review Checklist

Senior developers (or AI code review workflow) MUST verify:

1. All DoD checkboxes are marked complete
2. Test files exist for new logic (search for `*Test.kt` files)
3. Test execution logs show passing results
4. No test coverage regressions introduced

### Story Cannot Be Marked "Done" Unless:

- ✅ All unit tests pass
- ✅ All instrumentation tests pass (if applicable)
- ✅ Code review approves the implementation
- ✅ All DoD criteria are verified

---

## References

- **Testing Patterns:** See existing test files in `app/src/test/` and `app/src/androidTest/`
- **Architecture Document:** `/docs/architecture.md` - Testing approach section
- **Tech Spec:** Epic-specific tech specs include testing requirements
- **Example Stories:**
  - Story 1.2 - MVVM Architecture Foundation (60 unit tests, excellent patterns)
  - Story 1.3 - Navigation (instrumentation tests for UI flows)
  - Story 1.4 - Health Connect Integration (unit + instrumentation tests)

---

## Revision History

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-11-09 | 1.0 | Initial testing standards and DoD definition | PM Agent (John) |

---

**All future user stories MUST reference this document and comply with these standards.**
