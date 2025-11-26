# Definition of Done

**Last Updated:** 2025-11-24  
**Version:** 1.0  
**Applies To:** All Foodie project stories and tasks

## Purpose

This document defines the quality standards and completion criteria that **MUST** be met before any story can be considered "done" in the Foodie project. These standards ensure maintainability, prevent technical debt accumulation, and enable sustainable AI-driven development.

---

## Implementation & Quality

### Code Quality Standards

- [ ] **All acceptance criteria met** with verified evidence (tests passing, manual validation screenshots where applicable)
- [ ] **All tasks and subtasks completed** and marked [x] in story file
- [ ] **Code follows project architecture patterns:**
  - MVVM architecture: ViewModel → Repository → DataSource
  - Result<T> pattern for all repository methods (type-safe error handling)
  - Hilt dependency injection (@Inject constructors, @Module classes, @HiltViewModel)
  - Compose UI patterns (stateless composables, state hoisting, ViewModel integration)
  - Material 3 theming and accessibility patterns
- [ ] **Kotlin coding conventions followed:**
  - Null safety patterns (safe calls, elvis operators used appropriately)
  - Coroutine patterns for async operations
  - Naming conventions (camelCase functions, PascalCase classes)
  - No unused imports, parameters, or dead code
- [ ] **No code duplication** (extract common logic to shared utilities/helpers)
- [ ] **Cognitive complexity manageable** (methods <15 complexity threshold)
- [ ] **Security best practices applied:**
  - No hardcoded credentials or tokens
  - Sensitive data stored using EncryptedSharedPreferences
  - No API keys or credentials logged
  - Secure logging patterns (appropriate log levels, masked data)

### SonarQube Quality Gate

**Critical Requirement:** Run SonarQube scan **after implementation** and **before marking story complete**

**AUTOMATED QUALITY GATE PROCESS (Workflow Step 5.5):**

The dev-story workflow now includes a **mandatory automated quality gate** (Step 5.5) that executes BEFORE story completion:

```bash
# Step 1: Lint and Auto-fix (MANDATORY)
make lint-fix

# Step 2: SonarQube Scan (MANDATORY)
make sonar

# Step 3: If violations detected → Fix → Loop back to Step 1
```

**Quality Gate Loop Logic:**
1. **Lint-fix executes** - All code formatting and static analysis auto-fixes applied
2. **SonarQube scan runs** - Full test suite + coverage + quality analysis
3. **Dashboard reviewed** - Agent checks for BLOCKER/CRITICAL/MAJOR violations
4. **If violations found:**
   - Agent works with user to fix issues
   - Tests re-run to prevent regressions
   - Loop restarts from Step 1 (lint-fix → sonar)
5. **If clean or acceptable:** Proceed to story completion

**Why This Matters (From Epic 5 Retrospective):**
> "I have worked on projects like this where I let the quality slide too far and the project ended up in a situation where the AI was unable to continue. It was stuck in a loop of trying to fix its own errors, while creating others at the same time."

**Key Insight:** Technical debt creates "error-fixing loops" that AI agents cannot escape. The automated quality gate prevents this by catching issues immediately, not after accumulation.

#### Required Process:
1. **After all implementation and tests complete, run SonarQube scan:**
   ```bash
   # Ensure SonarQube server is running (localhost:9000)
   sonar-scanner
   ```

2. **Review SonarQube dashboard for new issues** (http://localhost:9000/dashboard?id=Foodie)

3. **Apply severity-based thresholds:**

   | Severity | Threshold | Action Required |
   |----------|-----------|-----------------|
   | **BLOCKER** | 0 | **MUST FIX** before story completion - no exceptions |
   | **CRITICAL** | 0 | **MUST FIX** or document as known issue with resolution plan |
   | **MAJOR** | < 5 new issues | Review and decide: fix immediately OR defer with clear justification in story Dev Notes |
   | **MINOR** | Track but don't block | Address incrementally, prioritize high-impact issues |
   | **INFO** | Track but don't block | Consider for future cleanup sprints |

4. **Quality Ratings must meet thresholds:**
   - **Security Rating:** A or B (no critical vulnerabilities)
   - **Reliability Rating:** A or B (no critical bugs)
   - **Maintainability Rating:** A (technical debt ratio < 5%)

5. **If violations found:**
   - **BLOCKER/CRITICAL:** Fix immediately before proceeding
   - **MAJOR:** Fix or document deferral with justification in Dev Notes
   - **Run unit tests after each fix** to prevent regressions
   - **Re-scan with SonarQube** to verify resolution

6. **Document SonarQube results in story file:**
   - Add entry to Dev Notes: "SonarQube scan completed - [X] new issues, [Y] resolved"
   - If deferrals: Document justification (e.g., "MAJOR issue in edge case code, deferred - extensive refactor required")

#### Why SonarQube Integration Matters

From Epic 5 Retrospective (BMad, Project Lead):
> "I have worked on projects like this where I let the quality slide too far and the project ended up in a situation where the AI was unable to continue. It was stuck in a loop of trying to fix its own errors, while creating others at the same time."

**Key Insight:** Technical debt creates "error-fixing loops" that AI agents cannot escape. Proactive quality management through SonarQube scanning is not optional - it's a **functional requirement** for AI-driven development continuity.

---

## Testing Requirements

### Unit Tests

- [ ] **All existing unit tests passing** (`./gradlew test` succeeds with 100% pass rate)
- [ ] **No test regressions introduced** by implementation changes
- [ ] **New tests created for new functionality:**
  - Business logic changes → Unit tests for ViewModel, Repository, DataSource layers
  - Edge cases and error handling scenarios covered
  - Null safety and boundary conditions tested
- [ ] **Test naming conventions followed:**
  - Pattern: `methodName_whenCondition_thenExpectedResult` OR
  - Pattern: `feature should behavior when condition` (descriptive with spaces - Android convention)
- [ ] **Test execution time remains fast** (< 60 seconds for full suite)
  - Optimize slow tests (mock expensive operations, avoid Thread.sleep)
- [ ] **Assertion library used correctly:**
  - Truth library: `assertThat(actual).isEqualTo(expected)`
  - Clear, readable assertions with descriptive failure messages

### Integration/Instrumentation Tests

- [ ] **Integration tests added where appropriate:**
  - Critical user flows (e.g., end-to-end capture flow, data management)
  - Component interactions requiring Android framework (Room, WorkManager, Health Connect)
- [ ] **Instrumentation tests run and passing** (`./gradlew connectedAndroidTest`)
  - **Note:** Currently 111 instrumentation tests (31 non-valuable tests deleted per Epic 5 Retrospective)
- [ ] **Physical device testing for Android-specific features:**
  - Pixel 8 Pro Android 16 used for manual validation
  - Camera integration, permissions, Health Connect, notifications

### Manual Testing

- [ ] **Smoke test completed on physical device** (Pixel 8 Pro Android 16):
  - **Core capture flow:** Widget → Camera → Analysis → Health Connect save (< 5 seconds)
  - **Settings:** API configuration, test connection, dark mode toggle
  - **Data management:** List view, edit meal, delete meal, pull-to-refresh
- [ ] **Performance validated:**
  - Cold launch time < 1 second
  - UI transitions smooth (60fps)
  - List scrolling buttery smooth
  - Memory usage within limits (no leaks)
- [ ] **Accessibility verified:**
  - Content descriptions present for all interactive elements
  - Touch targets ≥ 48dp
  - Keyboard navigation functional
  - Screen reader compatible

---

## Documentation

- [ ] **Story file updated with implementation details:**
  - Dev Agent Record → Debug Log: Implementation approach, key decisions
  - Dev Agent Record → Completion Notes: Summary of changes, follow-ups
  - File List: All new, modified, or deleted files (paths relative to repo root)
  - Change Log: Entry with date, author, changes summary
  - Status: Updated to "review" (pending approval) or "done" (if approved)
- [ ] **Inline code documentation added where needed:**
  - Complex algorithms explained with comments
  - Non-obvious architectural decisions documented
  - TODO comments for known follow-ups or tech debt
- [ ] **User-facing documentation updated (if applicable):**
  - README.md if new features or setup steps added
  - User Demo section in story file updated with validation checklist

---

## Build & Release

- [ ] **Project builds successfully:**
  ```bash
  ./gradlew assembleDebug     # Debug build
  ./gradlew assembleRelease   # Release build
  ```
- [ ] **Zero compilation errors or warnings** (address Lint warnings where appropriate)
- [ ] **No new Gradle deprecations introduced** (use recommended APIs)
- [ ] **APK size within limits** (< 10MB for personal app)
- [ ] **ProGuard/R8 rules configured** (if obfuscation needed for release)

---

## Process Integration

- [ ] **Story marked "review" in sprint-status.yaml** after implementation complete
- [ ] **Code review requested** (via `code-review` workflow or team process)
- [ ] **Peer feedback addressed** before marking "done"
- [ ] **Retrospective notes captured** (if Epic-level, document learnings)

---

## Checklist Summary

**Before marking story "review" or "done", verify ALL of the following:**

1. ✅ All acceptance criteria met and verified
2. ✅ All tasks/subtasks completed and marked [x]
3. ✅ Code follows project architecture patterns (MVVM, Result<T>, Hilt DI, Compose UI)
4. ✅ SonarQube scan completed with 0 BLOCKER, 0 CRITICAL, <5 new MAJOR issues
5. ✅ Security/Reliability/Maintainability ratings meet thresholds (A or B)
6. ✅ All unit tests passing (100% pass rate, fast execution <60s)
7. ✅ Manual smoke test on physical device completed
8. ✅ Story file updated (Dev Notes, File List, Change Log, Status)
9. ✅ Build succeeds with zero errors
10. ✅ Story marked "review" in sprint-status.yaml

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-24 | Developer Agent (Amelia) | Initial DoD created with SonarQube integration, story 5-9 deliverable |

---

## References

- **Story 5-9:** SonarQube Technical Debt Resolution (systematic cleanup workflow)
- **Epic 5 Retrospective:** AI-driven development quality insights, error-fixing loops
- **Architecture.md:** MVVM patterns, Result<T> error handling, testing strategy
- **Story 5-2:** SecurePreferencesManager implementation (security patterns)
- **Story 5-8:** Final integration testing methodology (manual testing approach)
