# Product Brief: Foodie

**Date:** 2025-11-07  
**Author:** BMad  
**Status:** Draft

---

## Executive Summary

**Vision:** Make calorie tracking invisible - capture food in 2 seconds with one hand while holding your plate, eliminating the speed and accuracy bottlenecks that make manual tracking unsustainable for body recomposition goals.

**The Problem:** Manual calorie tracking creates two critical failures: (1) Speed friction - requires focus, typing, and 30+ seconds per entry, making it too slow for real-life scenarios like standing in a canteen line, and (2) Accuracy uncertainty - manual estimates are unreliable guesswork that derails precision-dependent fitness goals like building muscle while losing fat.

**The Solution:** AI-powered "invisible tracking" - home screen widget → camera snap → Azure OpenAI analysis → auto-save to Google Health Connect. Photos processed in background, deleted after analysis. User reviews and edits later if needed. No backend server, no photo storage, no social features - just fast, accurate data capture.

**Target Impact:** Enable sustainable body recomposition through precision calorie tracking that doesn't disrupt daily life or require cognitive overhead during meals.

---

## Core Vision

### Problem Statement

**Speed Bottleneck:**  
Current calorie tracking (MyFitnessPal manual entry) is too slow for real-world scenarios. Requires opening app, navigating menus, typing estimates, and maintaining focus for 30+ seconds per entry. When you're standing in a canteen holding a plate, you need one-handed capture in ~2 seconds. Manual tracking can't deliver this - it interrupts the dining experience and creates social awkwardness.

**Accuracy Bottleneck:**  
Manual calorie estimates are unreliable guesswork. Users have no way to verify if "chicken with rice" is 500 or 800 calories. For casual tracking this might suffice, but body recomposition (simultaneously building muscle and losing fat) requires precision - inaccurate data can derail multi-month fitness goals.

### Problem Impact

**Personal Stakes:**  
The user is pursuing body recomposition - building muscle mass while reducing body fat percentage. This goal demands calorie precision because:
- Small daily deficits required (100-300 cal) to preserve muscle while losing fat
- Protein intake must be tracked to support muscle growth
- Inaccurate estimates make it impossible to know if you're in the right calorie range
- Multi-month commitment means tracking fatigue is guaranteed - only invisible systems survive

**Why This Matters:**  
If tracking takes cognitive effort or interrupts daily life, adherence collapses within weeks. The fitness goal fails not from lack of discipline, but from tool inadequacy. Solving speed + accuracy unlocks sustainable body recomposition.

### Proposed Solution

**"Invisible Tracking" Flow:**
1. **Lock/home screen widget** → instant camera access (no app launch)
2. **Snap photo** while walking to table (one-handed, 2 seconds)
3. **Pocket phone** - AI processes in background
4. **Auto-save** to Google Health Connect when analysis completes
5. **Optional review** in evening - edit if needed, otherwise trust the AI

**Key Innovation:** Photos are ephemeral transport mechanism, not data. Delete immediately after extracting calories + description. No backend server needed - just client → Azure OpenAI API → local device storage via Health Connect.

**Success Criteria:** Must be faster + more accurate than current MyFitnessPal manual entry flow.

---

## Target Users

### Primary User

**Personal Tool:** Built primarily for BMad's own use - someone with body recomposition goals who needs precision tracking without friction.

**User Profile:**
- Already understands calorie tracking, macros, and energy balance
- Experienced with fitness/nutrition concepts (no education needed)
- Values speed and discretion over social features
- Willing to trust AI estimates with manual override capability
- Uses Android device (Google Health Connect requirement)

**Use Context:**
- Eats out frequently (canteen, restaurants)
- Tracks 3-5 meals/snacks daily
- Reviews data in evening, not during meals
- May expand to family/friends informally if successful

**Explicit Non-Goals:**
- Not targeting broad consumer market (for now)
- Not educating beginners on caloric deficit or macro concepts
- Not building social/community features
- Not monetizing (personal project, commercial opportunity TBD)

**Future Consideration:** While built as personal tool, architecture allows future expansion if value proves significant. "Never say never" - but MVP focus is solving one person's problem exceptionally well.

---

## Success Metrics

### Primary Success Criterion

**Speed Validation:** Average capture time ≤ 5 seconds from widget tap to photo saved
- This is the core hypothesis - can invisible tracking actually work in real life?
- Measured from: home screen widget activation → photo captured → return to previous activity
- Target: Fast enough to use while holding plate in canteen line (one-handed, 2 seconds for snap)

### Secondary Success Criteria

**Sustained Usage:** Still tracking on workdays after 30 days
- Proves the tool doesn't create tracking fatigue
- Workday-focused metric (weekends/vacations deliberately excluded - intentional breaks are healthy)
- Success = 80%+ workdays logged over 30-day period

**AI Accuracy (Trust-Based):** Ad-hoc validation via ChatGPT shows reasonable estimates
- No formal accuracy testing required
- User already validated Azure OpenAI estimates informally - "pretty happy with results"
- Action threshold: Only investigate if estimates become "crazy off" during real usage
- Edit frequency not tracked - corrections are expected and acceptable

### Metrics Explicitly Excluded

**Body Composition Progress:** Not measured within app
- App doesn't track body metrics (weight, fat %, muscle mass)
- User tracks separately with smart scale
- Future opportunity: Could integrate scale data for energy balance insights (V2.0+)

**Edit Frequency / AI Reliability:** Not formally measured
- AI accuracy acceptable based on prior testing
- Manual corrections expected and built into workflow (evening review mode)
- Trust-based approach: investigate only if patterns emerge during usage

---

## MVP Scope

### V1.0 Features (In Scope)

**Core Capture Flow:**
- Home screen widget for instant camera access
- Photo capture (single photo per entry)
- Azure OpenAI API integration with simple structured JSON output: `{calories: number, description: string}`
- Background processing (snap → pocket phone → AI analyzes)
- Auto-save to Google Health Connect when analysis completes
- Ephemeral photo storage (delete immediately after extracting data)

**Data Management:**
- Minimal list view showing recent entries (date, description, calories)
- Edit capability: tap entry → modify calories and/or description
- Delete capability: remove entries from Google Health Connect
- CRUD operations for all nutrition data

**Technical Architecture:**
- Client-only architecture (no backend server, no database, no user accounts)
- Direct API calls: Android app → Azure OpenAI → Health Connect (local device storage)
- Native Android development (Kotlin/Java)
- Azure OpenAI GPT-4o (multimodal model)

### V1.0 Features (Explicitly Out of Scope)

**Deferred to V2.0:**
- Custom dashboard/analytics (use Google Fit or any Health Connect-compatible app for data viewing)
- Macros tracking (protein, carbs, fat)
- Garmin Connect integration for energy balance
- Smart auto-categorization (breakfast/lunch/dinner/snacks)
- Daily summary notifications
- Lock screen widget (home screen widget only in V1.0)

**Deferred to V3.0+:**
- Before/after photos for portion adjustment
- Family meal portions ("I ate 1/4 of this")
- Allergy warnings
- Voice annotations

**Deferred to V4.0+:**
- Barcode scanning + OpenFoodFacts integration
- Video/burst mode for 3D food scanning
- Recipe recognition
- Food safety/freshness detection

### Key Technical Decisions

**Platform:** Native Android (Kotlin/Java)
- Rationale: Google Health Connect is Android-exclusive API
- Best widget performance and platform integration
- No cross-platform complexity needed for personal tool

**AI Model:** Azure OpenAI GPT-4o (multimodal)
- Proven accurate in informal testing via ChatGPT
- Supports structured JSON outputs for reliable parsing
- User already has Azure OpenAI API access

**Data Storage:** Google Health Connect (local device)
- Eliminates need for backend server and database
- Privacy by default (data stays on device)
- Interoperability with other health apps (Google Fit, etc.)

**Architecture Philosophy:** Client-only, ephemeral photos, structured outputs
- Simplest possible implementation for fastest development
- Aligns with "First Principles" insight from brainstorming session

---

## Market Context

### Why Build This?

**Primary Motivation:** Personal learning and skill development
- Practice spec-driven development methodology
- Hands-on experience with native Android development
- Learn Google Health Connect API integration
- Experiment with Azure OpenAI multimodal API integration
- Build real software that solves a personal problem

**Secondary Motivation:** Solve personal need better than existing tools
- MyFitnessPal offers paid photo-based tracking (untested by user)
- Existing solutions likely don't match "invisible tracking" philosophy
- Full control over UX, speed optimization, and feature roadmap
- No subscription costs or vendor lock-in

**Competitive Landscape:** Not a primary concern
- Built for personal use, not competing in market
- If existing solutions already work perfectly, this is still valuable as learning project
- Flexibility to iterate based on real usage without business constraints

### Strategic Value

**Learning Goals:**
- Spec-driven development (BMAD Method workflow)
- Native Android development patterns
- Health Connect API implementation
- Azure OpenAI structured outputs integration
- Widget development and background processing
- Real-world problem-solving with modern tools

**Personal Tool Benefits:**
- Solves actual body recomposition tracking need
- Full customization freedom
- Privacy by design (no data leaves device except API calls)
- Foundation for future enhancements if successful

---

## Risks and Constraints

### Development Risks

**High Priority:**
1. **Google Health Connect API Complexity** - Unknown learning curve for permissions, data formats, and integration patterns
   - Mitigation: Research API docs thoroughly before implementation, build proof-of-concept early
   
2. **Scope Creep Temptation** - Easy to add "just one more feature" and delay MVP shipping
   - Mitigation: Strict adherence to V1.0 feature list, defer all enhancements to V2.0+

**Medium Priority:**
3. **Android Background Processing Reliability** - Battery optimization might kill background API calls
   - Mitigation: Test on real device, implement proper foreground service if needed
   
4. **AI Accuracy Edge Cases** - Soups, sauces, mixed dishes, complex meals might be consistently inaccurate
   - Mitigation: Trust-based approach, manual corrections available, monitor for patterns during usage

### Technical Constraints (Accepted)

**Required Dependencies:**
- Android device with Google Health Connect support
- Active Azure OpenAI API access (available via workplace)
- Network connectivity during meal times for real-time processing

**Development Approach:**
- LLM-assisted code generation (100% AI-written code)
- Senior software engineer oversight and architectural direction
- No prior Kotlin/Android development experience required (learning by directing)

### Non-Risks (Validated as Acceptable)

**Cost:** Azure OpenAI API costs negligible at 3-5 photos/day with workplace access

**Widget Performance:** Validated feasible - MyFitnessPal demonstrates fast widget → camera flow for barcode scanning

**Native Android Learning Curve:** Non-blocking - LLM will write code under senior engineer direction

**Offline Mode:** Deferred to V2.0 as enhancement (offline photo queuing with automatic processing when network available)

### Future Opportunities

**V2.0 Offline Enhancement:**
- Queue photos locally when network unavailable
- Automatically detect network availability
- Process queued photos in background when online
- No user intervention required (invisible retry logic)

---

## Timeline and Development Approach

### Development Strategy

**AI-Assisted Development:**
- 100% code generated by LLM (GitHub Copilot, ChatGPT, etc.)
- Senior software engineer provides architectural direction and oversight
- High-level understanding of Android architecture sufficient (no deep Kotlin expertise needed)
- Learning through directing rather than writing

**Spec-Driven Approach:**
- Follow BMAD Method workflow (brainstorming → product brief → PRD → architecture → implementation)
- Clear specifications before code generation
- Iterate on specs, not ad-hoc code changes

### Estimated Timeline

**MVP (V1.0) Development:** 1-2 weeks
- Week 1: Setup + core flow (widget → camera → API → Health Connect)
- Week 2: CRUD operations, polish, testing with real meals

**Validation Period:** 30 days after MVP completion
- Track usage on workdays (80%+ target)
- Validate speed hypothesis (≤5 seconds capture time)
- Identify accuracy issues or needed adjustments

**V2.0 Planning:** After validation period
- Decide on dashboard, macros tracking, Garmin integration based on real usage patterns
- Add offline photo queuing if network dependency proves problematic

### Success Gates

**MVP Complete:** When all V1.0 features functional and tested with 5+ real meals

**Validation Complete:** After 30 days of workday usage (24+ days tracked)

**V2.0 Decision Point:** Evaluate whether enhanced features justified based on sustained usage and personal value

---

## Next Steps

### Immediate Actions

1. **Complete Product Brief** ✓ (this document)
2. **Create Product Requirements Document (PRD)** - Detailed functional/technical specifications
3. **Architecture Document** - Technical design, API integration patterns, Health Connect schema
4. **Sprint Planning** - Break down MVP into implementable tasks for LLM-assisted development

### Phase 1: Planning (Next)

**Product Manager Agent** will create PRD covering:
- Detailed user stories and acceptance criteria
- API request/response schemas
- Health Connect data model
- Widget specifications
- Error handling requirements
- Edge case definitions

### Phase 2: Solutioning

**Architect Agent** will design:
- Android app architecture (MVVM, repository pattern, etc.)
- Azure OpenAI integration approach
- Health Connect data persistence layer
- Widget implementation strategy
- Background processing architecture

### Phase 3: Implementation

**Developer Agent** will generate:
- Kotlin code for all components
- Widget implementation
- Health Connect integration
- Azure OpenAI API client
- UI screens (list view, edit/delete)

---

## Document Status

**Version:** 1.0 (Draft)  
**Last Updated:** 2025-11-07  
**Next Review:** After PRD completion  
**Workflow Status:** Product Brief complete, ready for PRD phase

