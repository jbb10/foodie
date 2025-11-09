# Epic 2 Story Creation Notes

**⚠️ CRITICAL: READ THIS BEFORE CREATING EPIC 2 STORIES ⚠️**

**Date Created:** November 9, 2025  
**Source:** Epic 1 Retrospective Action Items  
**Purpose:** Guidance for SM agent when creating Epic 2 stories

**Discovery Points:**
1. **Referenced in epics.md** - Epic 2 header includes pointer to this file
2. **Referenced in retrospective** - Epic 1 retrospective Preparation Tasks section links here
3. **Required for Stories 2.3 and 2.4** - Contains mandatory task additions and Dev Notes sections

**What's Inside:**
- Story 2.3: Azure OpenAI Playwright documentation research guidance
- Story 2.4: WorkManager basic setup task (MUST be first tasks) + Health Connect Playwright guidance
- Templates ready to copy into story files during creation

---

## Story 2.3: Azure OpenAI API Client

### Required Dev Notes Additions

**Add to Dev Notes Section:**

```markdown
### Documentation Research Strategy

**REQUIRED: Use Playwright MCP for Azure OpenAI Documentation Exploration**

This story involves Azure OpenAI Responses API integration which has extensive, multi-layered official documentation. Use Playwright browser navigation tool for efficient research during implementation.

**Starting Points:**
- Azure OpenAI Reference: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference
- Azure OpenAI Responses API: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/chatgpt
- GPT-4 Vision Documentation: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/gpt-with-vision

**Focus Areas for Playwright Exploration:**
- Multimodal input patterns (text + base64 image)
- Responses API request/response structure
- Error handling and retry patterns
- Authentication patterns (api-key header, not Bearer token)
- Rate limiting and quota management
- Vision model capabilities and limitations

**Playwright Benefits:**
- Navigate complex Azure docs hierarchy interactively
- Capture code snippets and examples directly
- Follow cross-references between Responses API, Chat Completions, Vision guides
- Document learnings in Story 2.3 completion notes

**Documentation from architecture.md:**
- Base docs already captured in Official Documentation References section
- Use Playwright to explore advanced patterns not in architecture.md
```

---

## Story 2.4: Background Processing Service

### Required Task Additions

**Add to First Tasks (before main implementation):**

```markdown
- [ ] **Task 1: WorkManager Basic Setup** (Foundation)
  - [ ] Add WorkManager dependency verification in `gradle/libs.versions.toml` (should exist from Story 1-1)
  - [ ] Create `WorkManagerModule.kt` in `di/` package
  - [ ] Provide WorkManager instance with Hilt injection
  - [ ] Configure WorkManager initialization in `FoodieApplication.kt`
  - [ ] Create basic WorkManager configuration (constraints, backoff policy)
  - [ ] Write unit tests for WorkManager module
  - [ ] Verify WorkManager setup works: `./gradlew test`
```

### Required Dev Notes Additions

**Add to Dev Notes Section:**

```markdown
### WorkManager Foundation Setup

**First Tasks Include Basic WorkManager Configuration:**

This story begins with WorkManager module setup to establish the foundation before implementing AnalyzeMealWorker. This ensures:

1. **Dependencies Ready**: WorkManager properly injected via Hilt
2. **Configuration Established**: Constraints, backoff policy, retry logic configured
3. **Testing Foundation**: WorkManager test utilities available
4. **Clear Separation**: Setup separate from worker implementation

**WorkManager Setup Details:**
- Module location: `com.foodie.app.di.WorkManagerModule`
- Configuration in: `FoodieApplication.onCreate()`
- Default constraints: NetworkType.CONNECTED
- Retry policy: Exponential backoff, max 3 retries
- No foreground service needed (WorkManager handles reliability)

[Source: docs/retrospectives/epic-1-retrospective.md#Action-Items] (AI-4)

---

### Documentation Research Strategy

**REQUIRED: Use Playwright MCP for Health Connect Documentation Exploration**

This story involves Health Connect integration for meal metadata and advanced NutritionRecord usage. Use Playwright browser navigation tool for efficient research during implementation.

**Starting Points:**
- Health Connect Guide: https://developer.android.com/health-and-fitness/guides/health-connect
- NutritionRecord API: https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/NutritionRecord
- Health Connect Permissions: https://developer.android.com/health-and-fitness/guides/health-connect/permissions

**Focus Areas for Playwright Exploration:**
- Meal metadata patterns (meal type, timing)
- Query optimization for large datasets
- Advanced NutritionRecord usage (beyond calories + description)
- Background sync patterns
- Data integrity and conflict resolution

**Playwright Benefits:**
- Navigate Health Connect docs interactively
- Explore advanced patterns not in architecture.md
- Capture code examples for meal metadata
- Document learnings in Story 2.4 completion notes

**Documentation from architecture.md:**
- Basic CRUD patterns already documented from Story 1-4
- Use Playwright for advanced patterns (metadata, optimization)

[Source: docs/retrospectives/epic-1-retrospective.md#Action-Items] (AI-5)
```

---

## How to Use This Document

**For SM Agent (Bob):**

When running `*create-story` workflow for Epic 2 stories:

1. **Story 2.3 (Azure OpenAI API Client):**
   - Read "Story 2.3" section above
   - Add "Documentation Research Strategy" to Dev Notes
   - Include Playwright guidance with Azure OpenAI focus areas

2. **Story 2.4 (Background Processing Service):**
   - Read "Story 2.4" section above
   - Add "Task 1: WorkManager Basic Setup" to FIRST tasks (before other tasks)
   - Add both "WorkManager Foundation Setup" and "Documentation Research Strategy" sections to Dev Notes
   - Include Playwright guidance with Health Connect focus areas

3. **Other Epic 2 Stories:**
   - Check if story involves complex SDK (Health Connect, Android Platform APIs)
   - If yes, consider adding Playwright documentation guidance
   - Follow pattern from Story 2.3/2.4 sections above

**Completion Tracking:**
- AI-4: ✅ Documented (WorkManager setup in Story 2.4 notes)
- AI-5: ✅ Documented (Health Connect Playwright research in Story 2.4 notes)
- AI-6: ✅ Documented (Azure OpenAI Playwright research in Story 2.3 notes)

---

**Reference:** Epic 1 Retrospective Action Items (AI-4, AI-5, AI-6)  
**Location:** `docs/epic-2-story-notes.md`  
**Maintained By:** Bob (Scrum Master)
