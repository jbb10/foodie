# Story {{epic_num}}.{{story_num}}: {{story_title}}

Status: drafted

## Story

As a {{role}},
I want {{action}},
so that {{benefit}}.

## Acceptance Criteria

1. [Add acceptance criteria from epics/PRD]

## Tasks / Subtasks

- [ ] Task 1 (AC: #)
  - [ ] Subtask 1.1
- [ ] Task 2 (AC: #)
  - [ ] Subtask 2.1

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

**Testing Standards Summary:**
- **Unit Tests Required:** Always, for any story with business logic or data handling
- **Instrumentation Tests Required:** Conditional - only for UI flows, platform integration, or E2E scenarios
- **Test Naming Convention:** `methodName_whenCondition_thenExpectedResult` or `feature should behavior when condition`
- **Assertion Library:** Truth library for readable assertions (`assertThat(x).isEqualTo(y)`)
- **Mocking:** Use Mockito/Mockito-Kotlin for dependency mocking in unit tests

## Dev Notes

- Relevant architecture patterns and constraints
- Source tree components to touch
- Testing standards summary

### Project Structure Notes

- Alignment with unified project structure (paths, modules, naming)
- Detected conflicts or variances (with rationale)

### References

- Cite all technical details with source paths and sections, e.g. [Source: docs/<file>.md#Section]

## Dev Agent Record

### Context Reference

<!-- Path(s) to story context XML will be added here by context workflow -->

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
