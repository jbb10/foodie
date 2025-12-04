# Story 7.2: Enhanced Passive Calorie Calculation

Status: drafted

## Story

As a user,
I want accurate passive calorie (NEAT) calculations based on Health Connect data,
so that I can see accurate energy expenditure regardless of my activity type (cycling, lifting, etc.) and avoid inaccuracies from step-based estimations.

## Acceptance Criteria

1. [ ] Legacy step-multiplier path removed from codebase.
2. [ ] Both aggregate requests (TotalCaloriesBurned, ActiveCaloriesBurned) return successfully when HC permission is granted.
3. [ ] If HC permission is revoked, UI shows reconnect prompt (no crash, no silent zero).
4. [ ] At 00:01 local time Passive = 0 (±1 kcal).
5. [ ] On a multi-workout day, (Passive + Active + BmrElapsed) ≈ TotalHC (±5 %).
6. [ ] Travel test: start day in UTC-5, fly to UTC-8 at noon; algorithm still returns reasonable totals (does not reset or drop morning calories).
7. [ ] DST spring-forward day: algorithm completes without negative Passive and without crash.
8. [ ] Guardrail: if rawPassive > plausibleMax (dailyBmr × 3.0), value is retained and 'HighPassive' telemetry event is logged (no truncation).
9. [ ] Ratio telemetry must be within ±5 % except when rawPassive > plausibleMax.

## Tasks / Subtasks

- [ ] **Task 1: Documentation Research & Technical Validation** ⚠️ COMPLETE BEFORE PROCEEDING TO IMPLEMENTATION
  - [ ] Review official documentation for Health Connect `AggregateRequest` and `TimeRangeFilter`. Use the fetch tool to read these URLs: https://developer.android.com/reference/kotlin/androidx/health/connect/client/request/AggregateRequest and https://developer.android.com/reference/kotlin/androidx/health/connect/client/time/TimeRangeFilter
  - [ ] Validate assumptions: Garmin/HC writes "Total Calories Burned" stream correctly.
  - [ ] Identify constraints: Permission handling, timezone shifts.
  - [ ] **Deliverable Checkpoint:** Document findings in Dev Notes.

- [ ] Task 2: Remove Legacy Logic (AC: 1)
  - [ ] Locate and remove existing step-multiplier NEAT calculation.
  - [ ] Ensure no regressions in dependency graphs.

- [ ] Task 3: Implement Core Algorithm Infrastructure (AC: 2, 4, 6, 7)
  - [ ] Implement `startOfDay` determination (handling timezones).
  - [ ] Persist `startOfDayInstant` and `zoneOffsetAtMidnight` in SharedPreferences and reload on startup.
  - [ ] Implement `AggregateRequest` for `TotalCaloriesBurnedRecord.ENERGY_TOTAL`.
  - [ ] Implement `ReadRecordsRequest` for `ExerciseSessionRecord` to isolate intentional workouts.
  - [ ] Ensure no `DataOriginFilter` is used (allow HC to merge).

- [ ] Task 4: Implement Calculation Logic (AC: 4, 5, 8, 9)
  - [ ] Implement BMR subtraction logic: `rawPassive = TotalHC – bmrElapsed – ActiveHC`.
  - [ ] Implement lower-bound clamping: `Passive = max(rawPassive, 0)`.
  - [ ] Implement plausibility check: `plausibleMax = dailyBmr × 3.0`.
  - [ ] Handle edge cases (negative results, implausibly high values with telemetry logging).

- [ ] Task 5: Error Handling & UI Feedback (AC: 3)
  - [ ] Return sealed result to distinguish `PermissionsMissing` from `HcUnavailable`.
  - [ ] Implement UI prompt for reconnecting permissions.
  - [ ] Ensure no silent fallbacks to zero unless clamped.

- [ ] Task 6: Telemetry & Logging (AC: 8, 9)
  - [ ] Log ratio: `(Passive + ActiveHC + bmrElapsed) / TotalHC`.
  - [ ] Validate ratio is within 0.95-1.05 (±5 %) except when rawPassive > plausibleMax.
  - [ ] Log 'HighPassive' event when rawPassive > plausibleMax.
  - [ ] Log distinct origins in Total and Active streams.

- [ ] Task 7: Testing & Validation (AC: All)
  - [ ] Unit tests for algorithm logic (timezones, clamping, BMR subtraction).
  - [ ] Instrumentation tests for HC integration (mocking HC if possible or using integration tests).
  - [ ] Manual validation using QA Test Matrix.

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
  - User-facing UI workflows or navigation flows
  - Android platform API integration (Camera, Health Connect, Permissions, etc.)
  - End-to-end user flows spanning multiple components
  - Complex user interactions requiring device/emulator validation
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

## QA Test Matrix

- [ ] Permission revoked / re-granted mid-day.
- [ ] Watch off until 10 AM (phone sensors only); watch on after 10 AM.
- [ ] Sub-60-second workout; ActiveHC must be > 0 or Passive ratio alert raised.
- [ ] <60-second manual activity → Passive ratio still within ±5 %.
- [ ] Battery pull / device reboot; cached startOfDay reused correctly.
- [ ] Process restart after mid-day time-zone change; startOfDay persists correctly.

## Dev Notes

### Prescriptive Algorithm

**Definitions** (all values from 00:00 local-day start to now):

- `TotalHC` = Aggregate( TotalCaloriesBurnedRecord.ENERGY_TOTAL )
- `ActiveHC` = Sum of `activeCaloriesBurned` from all **ExerciseSessionRecords** in time range.
    - *NOTE: Do NOT use ActiveCaloriesBurnedRecord (Aggregate) as this includes NEAT and will result in 0 Passive calories.*
- `dailyBmr` = Mifflin-St Jeor(userProfile)
- `bmrPerMinute` = dailyBmr / 1440 // constant divisor; ±4 % DST skew accepted on spring-forward days (this is NOT a bug)
- `minutesElapsed` = Minutes between startOfDay and now
- `bmrElapsed` = bmrPerMinute × minutesElapsed
- `rawPassive` = TotalHC – bmrElapsed – ActiveHC
- `plausibleMax` = dailyBmr × 3.0 // ≈5–7k kcal for most users
- `Passive` = max(rawPassive, 0) // lower-bound only; no upper truncation
- If `rawPassive > plausibleMax`: log 'HighPassive' event but retain full value

### Implementation Details (Kotlin reference)

**a) Determine startOfDay once per local-day:**

```kotlin
val zone         = ZoneId.systemDefault()
val now          = Instant.now()
val startOfDay   = LocalDate.now(zone)
                     .atStartOfDay(zone)
                     .toInstant()
// Persist startOfDayInstant and zoneOffsetAtMidnight in SharedPreferences.
// Reload on app startup to survive process restarts and mid-day time-zone changes.
```

**b) Data Calls (Total Aggregate vs Session List):**

```kotlin
// 1. Get TOTAL Energy (Everything: BMR + NEAT + Workouts)
val totalReq = AggregateRequest(
    metrics         = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
)

// 2. Get WORKOUT Energy Only (Strictly tracked sessions)
val sessionsReq = ReadRecordsRequest(
    recordType      = ExerciseSessionRecord::class,
    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
)

// Execution
val totalRes = healthConnectClient.aggregate(totalReq)
val sessionsRes = healthConnectClient.readRecords(sessionsReq)

// 3. Summation
val totalEnergy = totalRes[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
val activeEnergy = sessionsRes.records.sumOf { 
    it.activeCaloriesBurned?.inKilocalories ?: 0.0 
}
```

**c) Error surfacing:**

Return a sealed result so the UI can distinguish `PermissionsMissing` from `HcUnavailable`. Do **not** silently fall back to zero unless clamped.

**d) Telemetry / sanity checks:**

```kotlin
val plausibleMax = dailyBmr * 3.0
val ratio = (passive + activeEnergy + bmrElapsed) / totalEnergy

// Log ratio and validate within ±5% (except when rawPassive > plausibleMax)
if (ratio < 0.95 || ratio > 1.05) {
    if (rawPassive <= plausibleMax) {
        log("RatioOutOfBounds", ratio)
    }
}

// Log high passive events (no truncation)
if (rawPassive > plausibleMax) {
    log("HighPassive", rawPassive)
}

// Log distinct data origins
log("distinctOriginsInTotal", totalRes.dataOrigins.size)
log("distinctOriginsInSessions", sessionsRes.records.map { it.metadata.dataOrigin }.distinct().size)
```

## Dev Agent Record

### Context Reference

<!-- Path(s) to story context XML will be added here by context workflow -->

### Agent Model Used

Gemini 3 Pro (Preview)

### Debug Log References

### Completion Notes List

### File List
