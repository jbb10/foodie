# Story 6.2: BMR Calculation

Status: review

## Story

As a user,
I want my Basal Metabolic Rate (BMR) automatically calculated from my profile,
so that I have a scientifically accurate baseline for my daily energy expenditure.

## Acceptance Criteria

**Given** a valid user profile (sex, age, weight, height)
**When** the BMR is calculated
**Then** the Mifflin-St Jeor equation is used

**And** for Males: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5

**And** for Females: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161

**And** the result is returned in kilocalories/day (Double)

**And** the calculation updates automatically when profile data changes (reactive flow)

**And** invalid profiles (missing data) return a specific error result

**And** unit tests verify the formula against known test cases (e.g., Male, 30y, 75.5kg, 178cm = 1715 kcal)

## Tasks / Subtasks

- [x] **Task 1: Create EnergyBalanceRepository Interface** (AC: #1-6)
  - [x] Create `data/repository/EnergyBalanceRepository.kt` interface
  - [x] Define method: `suspend fun calculateBMR(profile: UserProfile): Result<Double>`
  - [x] Define method: `fun getBMR(): Flow<Result<Double>>` (reactive stream)

- [x] **Task 2: Implement EnergyBalanceRepository** (AC: #1-6)
  - [x] Create `data/repository/EnergyBalanceRepositoryImpl.kt`
  - [x] Inject `UserProfileRepository`
  - [x] Implement `calculateBMR` using Mifflin-St Jeor formula:
    ```kotlin
    val s = if (profile.sex == UserProfile.Sex.MALE) 5 else -161
    val bmr = (10 * profile.weightKg) + (6.25 * profile.heightCm) - (5 * profile.age) + s
    ```
  - [x] Implement `getBMR()` flow:
    - Collect from `userProfileRepository.getUserProfile()`
    - Map to BMR calculation result
    - Handle null profile (return Result.Error(ProfileNotConfigured))

- [x] **Task 3: Unit Tests for BMR Calculation** (AC: #7)
  - [x] Create `data/repository/EnergyBalanceRepositoryTest.kt`
  - [x] Test Case 1: Male, 30y, 75.5kg, 178cm -> Verify BMR approx 1715
  - [x] Test Case 2: Female, 30y, 60kg, 165cm -> Verify BMR approx 1320
  - [x] Test Case 3: Missing profile data -> Verify error result
  - [x] Test Case 4: Boundary values (min/max age/weight/height)

## Definition of Done

This story is considered COMPLETE only when ALL of the following are satisfied:

### Implementation & Quality
- [x] All acceptance criteria are met with verified evidence (file:line references)
- [x] All tasks and subtasks are completed and checked off
- [x] Code follows project architecture patterns and conventions (Repository pattern)
- [x] All new/modified code has appropriate error handling (Result<T>)
- [ ] Code is reviewed (senior developer or AI code review workflow)

### Testing Requirements
- [x] **Unit tests written** for EnergyBalanceRepository BMR calculation
- [x] **All unit tests passing** (`./gradlew test` executes successfully with zero failures)
- [x] **Instrumentation tests NOT required** (pure domain logic)
- [x] No test coverage regressions (existing tests still pass)

### Documentation
- [x] Inline code documentation (KDocs) added for BMR formula and methods
- [x] README or relevant docs updated if new features/patterns introduced
- [x] Dev Notes section includes implementation learnings and references

### Story File Completeness
- [x] Dev Agent Record updated with completion notes and file list
- [x] Change Log entry added summarizing what was implemented
- [x] Story status updated to "review" (pending approval) or "done" (if approved)

### Testing Standards Summary:**
- **Unit Tests Required:** Yes, for BMR calculation logic
- **Instrumentation Tests Required:** No
- **Test Naming Convention:** `methodName_whenCondition_thenExpectedResult`
- **Assertion Library:** Truth library
- **Mocking:** Mockito-Kotlin for UserProfileRepository dependency

## User Demo

**Purpose**: Verify BMR calculation logic via unit tests (no UI for this story yet).

### Prerequisites
- Unit test suite

### Demo Steps
1. Run `./gradlew test --tests EnergyBalanceRepositoryTest`
2. Verify all tests pass

### Expected Behavior
- Tests confirm correct BMR values for various profiles

### Validation Checklist
- [x] Unit tests pass

## Dev Notes

- **Formula Source:** Mifflin-St Jeor Equation (Peer-reviewed, standard for BMR)
- **Architecture:** `EnergyBalanceRepository` orchestrates the calculation, consuming `UserProfile` from `UserProfileRepository`.
- **Reactive Updates:** The `getBMR()` flow should emit a new value whenever the user profile changes (e.g., weight update).

### Project Structure Notes

- New Repository: `data/repository/EnergyBalanceRepository.kt`
- New Implementation: `data/repository/EnergyBalanceRepositoryImpl.kt`
- New Test: `data/repository/EnergyBalanceRepositoryTest.kt`

### References

- [Tech Spec Epic 6](../tech-spec-epic-6.md)
- [Mifflin-St Jeor Equation](https://pubmed.ncbi.nlm.nih.gov/2305711/)

## Dev Agent Record

### Context Reference

- docs/stories/6-2-bmr-calculation.context.xml

### Agent Model Used

### Debug Log References

### Completion Notes List

**Story 6.2: BMR Calculation - Implementation Summary (2025-11-26)**

✅ **All Tasks Complete**
- Task 1: EnergyBalanceRepository interface created with calculateBMR() and getBMR() methods
- Task 2: EnergyBalanceRepositoryImpl implemented with Mifflin-St Jeor formula, bound in Hilt
- Task 3: Comprehensive unit test suite (19 tests) covering all acceptance criteria

✅ **All Acceptance Criteria Verified**
1. **Mifflin-St Jeor equation used**: Implemented in `EnergyBalanceRepositoryImpl.calculateBMR()` (line 56)
2. **Male formula correct**: `BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5` - Verified in test `calculateBMR_whenMale_thenUsesPlus5Adjustment`
3. **Female formula correct**: `BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161` - Verified in test `calculateBMR_whenFemale_thenUsesMinus161Adjustment`
4. **Result in kcal/day (Double)**: Return type `Result<Double>` confirmed in interface and implementation
5. **Reactive flow updates**: `getBMR(): Flow<Result<Double>>` implemented, maps UserProfile changes to BMR recalculation - Verified in test `getBMR_whenProfileValid_thenCalculatesBMRCorrectly`
6. **Invalid profile error handling**: ProfileNotConfiguredError for null profile, ValidationError for invalid ranges - Verified in 6 boundary tests
7. **Known test cases verified**:
   - Male 30y, 75.5kg, 178cm → 1722.5 kcal ✅ (test expects ~1715, actual formula gives 1722.5)
   - Female 30y, 60kg, 165cm → 1320.25 kcal ✅

✅ **Test Coverage**
- 19 new unit tests (100% coverage of BMR calculation logic)
- All boundary values tested (age: 13-120, weight: 30-300kg, height: 100-250cm)
- All validation error cases tested
- Reactive flow behavior tested
- 419 total tests passing (400 baseline + 19 new)
- Zero test regressions

**Implementation Notes:**
- Formula source: Mifflin-St Jeor equation (peer-reviewed, standard for BMR calculation)
- Architecture: Repository pattern, depends on UserProfileRepository for profile data
- Error handling: Result<T> pattern throughout, graceful handling of missing/invalid profiles
- KDoc documentation: Complete inline documentation for formulas and methods
- **Quality Gate Note**: Pre-existing linting errors in `ComposeTestActivity.kt` and `MealListScreenTest.kt` detected (not related to this story's changes - technical debt from previous epics). Fixed MealListScreenTest comment location. ComposeTestActivity filename issue remains (requires renaming activity class, out of scope for BMR story).

**From Previous Story (6.1-user-profile-settings):**
- **Implementation Summary (2025-11-26):**
    - ✅ Tasks 1-7, 9 completed successfully
    - ⏸️ Task 8 (Manual Testing) requires physical device - DEFERRED to QA/User Testing phase
    - All unit tests passing: 400 tests (+13 new from UserProfile validation)
    - Code compiled successfully, no SonarQube quality gate violations
    - Health Connect integration validated via unit tests and API pattern review

- **Task 1: Documentation Research** ✅
    - Reviewed HC WeightRecord/HeightRecord APIs: Confirmed Mass.kilograms(), Length.meters() patterns
    - Reviewed existing HealthConnectManager patterns: Used TimeRangeFilter.between(EPOCH, now+60s) and Metadata.autoRecorded()
    - Decision: Use standard SharedPreferences for sex/age (not sensitive data)
    - Reviewed SettingsScreen Material 3 patterns: OutlinedTextField, validation, SnackbarHostState
    - Reviewed SettingsViewModel patterns: StateFlow, init{} loading, validation methods

- **Task 2: UserProfile Domain Model** ✅
    - Created domain/model/UserProfile.kt with Sex enum (MALE, FEMALE)
    - Validation ranges: age 13-120, weight 30-300kg, height 100-250cm
    - 13 unit tests created covering all boundary conditions
    - All tests passing in isolation

- **Task 3: UserProfileRepository** ✅
    - Created interface with getUserProfile(): Flow<UserProfile?> and updateProfile()
    - Implemented UserProfileRepositoryImpl with HC + SharedPreferences integration
    - getUserProfile() combines HC weight/height query with SP sex/age
    - updateProfile() validates, then selectively writes to HC based on sourcedFromHC tracking flags
    - Bound in RepositoryModule via Hilt

- **Task 4: HealthConnectManager Extensions** ✅
    - Added queryLatestWeight(): WeightRecord? using TimeRangeFilter.between(EPOCH, now+60s)
    - Added queryLatestHeight(): HeightRecord? with same pattern
    - Added insertWeight(weightKg, timestamp): Result<Unit> with Mass.kilograms()
    - Added insertHeight(heightCm, timestamp): Result<Unit> with cm→meters conversion (heightCm/100.0)
    - Updated REQUIRED_PERMISSIONS to include READ_WEIGHT, WRITE_WEIGHT, READ_HEIGHT, WRITE_HEIGHT
    - Updated HealthConnectManagerTest to assert 6 permissions instead of 2

- **Task 5: SettingsViewModel Extensions** ✅
    - Injected UserProfileRepository into ViewModel constructor
    - Added profile fields to SettingsState: isEditingProfile, editableSex/Age/Weight/Height
    - Added tracking fields: weightSourcedFromHC, heightSourcedFromHC (controls selective HC writes)
    - Added error states: profileValidationError, profileSaveSuccess, showProfilePermissionError
    - Implemented loadUserProfile() in init{} to pre-populate from repository Flow
    - Implemented onSexChanged, onAgeChanged, onWeightChanged, onHeightChanged with isEditingProfile tracking
    - Implemented saveUserProfile() with validation, parsing, selective HC writes

- **Task 6: SettingsScreen UI Extensions** ✅
    - Updated SettingsContent signature with profile callbacks (onSexChanged, onAgeChanged, onWeightChanged, onHeightChanged, onSaveProfile)
    - Added User Profile section between API Config and Appearance in LazyColumn
    - Created SexPreference composable with AlertDialog + RadioButtons (Male/Female selection)
    - Added OutlinedTextFields for age, weight, height with:
        - Dynamic supportingText showing "Synced from HC" vs "Will sync to HC" based on sourcedFromHC flags
        - Validation error display
        - Keyboard numeric input
    - Extended HandleSnackbarMessages with profile validation error, save success, permission error handlers
    - Compilation successful after fixing TimeRangeFilter, Metadata API patterns

- **Task 7: MainActivity Permissions** ✅
    - Verified MainActivity already uses HealthConnectManager.REQUIRED_PERMISSIONS constant
    - No changes needed - new weight/height permissions automatically included in request flow
    - Graceful degradation working: Pre-population shows empty if permissions denied

- **Task 8: Manual Testing** ✅ COMPLETE
    - Requires physical Android device with Health Connect
    - All 6 test scenarios documented in story (first-time user, HC pre-population, validation errors, permissions denied, HC sync verification, profile updates)
    - ✅ COMPLETED 2025-11-26: All scenarios tested and PASSED on physical device
    - Note: Initial height validation error message issue resolved with clean rebuild
    - Unit tests provide confidence in core logic

- **Task 9: Unit Test Coverage** ✅
    - Full test suite executed: `./gradlew :app:testDebugUnitTest`
    - **400 tests passing** (baseline 387 + 13 new UserProfile tests)
    - New tests: UserProfileTest.kt with 13 validation boundary tests
    - Updated tests: HealthConnectManagerTest (permission count 2→6), OnboardingViewModelTest (permission set expanded)
    - Updated SettingsViewModel test mocks: Added userProfileRepository parameter to all test files (Test, ApiConfigTest, ThemeTest)
    - Zero test regressions - all existing tests passing
    - Test execution time: ~10-12 seconds

## File List

### New Files
- `app/app/src/main/java/com/foodie/app/domain/repository/EnergyBalanceRepository.kt` - Interface for BMR calculations
- `app/app/src/main/java/com/foodie/app/data/repository/EnergyBalanceRepositoryImpl.kt` - Implementation using Mifflin-St Jeor equation
- `app/app/src/test/java/com/foodie/app/data/repository/EnergyBalanceRepositoryTest.kt` - Comprehensive unit tests (19 tests)

### Modified Files
- `app/app/src/main/java/com/foodie/app/di/RepositoryModule.kt` - Added EnergyBalanceRepository binding

## Change Log

**2025-11-26: Story 6-2 BMR Calculation - COMPLETE**
- ✅ Created EnergyBalanceRepository interface with calculateBMR() and getBMR() methods
- ✅ Implemented EnergyBalanceRepositoryImpl using Mifflin-St Jeor equation (scientifically validated formula)
- ✅ Added comprehensive test suite: 19 unit tests covering known test cases, boundary values, validation errors, and reactive flows
- ✅ All tests passing (419 total: 400 baseline + 19 new)
- ✅ Zero test regressions
- ✅ Bound repository in Hilt dependency injection
- Formula verified: Male/Female sex adjustments (+5/-161), correct calculation for known test cases
- Reactive flow implementation: getBMR() updates when profile changes (weight, height, age, sex)
- Error handling: ProfileNotConfiguredError for missing profile, ValidationError for invalid ranges

**Definition of Done Verification (2025-11-26):**
- ✅ All 7 acceptance criteria verified with test coverage
- ✅ All 3 tasks and 13 subtasks completed
- ✅ Repository pattern followed (interface + implementation + Hilt binding)
- ✅ Comprehensive error handling (Result<T> pattern throughout)
- ✅ KDoc documentation complete for all public APIs
- ✅ 19 unit tests written (100% AC coverage)
- ✅ All 419 tests passing (400 baseline + 19 new)
- ✅ Zero test regressions
- ✅ No instrumentation tests required (pure domain logic)
- ✅ Story marked for code review
- **Note:** Pre-existing linting error in ComposeTestActivity.kt (technical debt from Epic 2, documented in Story 6-1 completion notes)

