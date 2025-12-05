# Brainstorming Session Results

**Session Date:** 2025-11-07
**Facilitator:** Business Analyst Mary
**Participant:** BMad

## Executive Summary

**Topic:** Foodie App - AI-Powered Food Calorie Tracking

**Session Goals:** Comprehensive exploration of features, UX, technical approaches, future growth, and business model for a mobile app that uses Azure OpenAI multimodal models to analyse food photos and estimate calories.

**Techniques Used:** What If Scenarios, SCAMPER Method, Mind Mapping, First Principles Thinking

**Total Ideas Generated:** 40+

### Key Themes Identified:

**1. Invisible Tracking Philosophy**
- Minimal cognitive load - don't interrupt the dining experience
- Silent operation (no talking to phone in public)
- Fast enough to be "stealthy" at the canteen
- Process in background, review when ready
- Zero social disruption

**2. Simplicity Through First Principles**
- Photos are transport mechanism, not data (delete after analysis)
- No backend needed - just client → API → local storage
- Start with minimal prompt, iterate based on real usage
- Client-only architecture = faster development

**3. Energy Balance, Not Just Food Tracking**
- Real goal: Calories IN vs Calories OUT
- Garmin integration reveals the bigger picture
- Daily deficit/surplus calculation is the ultimate metric

**4. Two-Mode User Experience**
- MODE 1 (Capture): Throughout day, zero friction, 3-5x daily
- MODE 2 (Review): Evening review, once daily, relaxed analysis

**5. Progressive Enhancement Strategy**
- MVP beats current MyFitnessPal manual entry flow
- Layer complexity only after core loop proves valuable
- Clear versioned roadmap from simple to sophisticated

## Technique Sessions

### Session 1: What If Scenarios (Creative Exploration)

**Goal:** Explore radical possibilities by questioning constraints

**Ideas Generated:**

**Unlimited AI Capabilities:**
- Full nutritional breakdown (not just calories) - macros, vitamins, minerals
- Food safety/freshness detection ("this salmon looks questionable")
- Recipe modification suggestions ("swap pasta for zucchini noodles, save 150 cal")
- Ingredient quality assessment

**Zero Friction UX:**
- Lock screen/home screen widget for instant camera access
- Background processing with push notifications
- No typing required - pure visual/voice interface
- Batch processing of camera roll photos (rejected - don't want to pollute camera roll)
- Pre-loaded predictions based on patterns (rejected - canteen changes daily)

**The "Invisible Tracking" Flow Discovery:**
1. Lock screen widget/shortcut → instant camera
2. Snap while walking to table
3. Close phone, pocket it
4. AI processes in background
5. Push notification when done: "Chicken rice bowl - 680 cal, 45g protein"
6. Swipe to view details or dismiss

**Multiple Input Modes:**
- Video/burst mode for 3D food scanning (hold button, move phone, better volume estimation)
- Multi-item detection from one capture
- Voice annotations ("I only ate half")
- Text corrections ("this is tofu not chicken")

**Advanced Analysis:**
- Before/after photos for portion adjustment
- Personal recipe database with "healthier next time" suggestions
- Family meal portions ("I ate 1/4 of this")
- Allergy warnings based on user settings

### Session 2: SCAMPER Method (Systematic Improvement)

**Goal:** Apply 7 creative lenses to find practical enhancements

**S - Substitute:**
- Lightweight on-device model for instant estimates (deprioritized for MVP)
- Manual entry fallback for dark places or forgotten photos
- User override capability ("I know this is exactly 500 cal")
- **Barcode scanning + OpenFoodFacts database for packaged foods**
- **Barcode auto-detection in photos** (AI can see barcodes, return in JSON)

**C - Combine:**
- Google Health Connect for data storage (eliminates need for backend!)
- Garmin Connect for calorie expenditure
- Daily balance calculation: Consumed vs Burned
- Multi-day trends and patterns
- Eliminated: MyFitnessPal integration (redundant if building full tracker)

**A - Adapt:**
- **Smart auto-categorization** (time + food type = breakfast/lunch/dinner/snacks)
  - 6-10am + breakfast foods → Breakfast
  - 11am-2pm → Lunch
  - 5-9pm + substantial meal → Dinner
  - Candy/small items → Snacks

**M - Modify/Magnify:**
- Voice logging for common scenarios ("log chicken salad 700 calories", "log same as yesterday's lunch")
- **Home screen widget as primary interface** - full app only for history/editing
- **Before/after photos for portion adjustment** ("served 800 cal, ate 60% = 480 cal actual")

**P - Put to Other Uses:**
- Family meal planning ("this dinner serves 4, each portion is 450 cal")
- Recipe development (track cooking iterations)
- **Dietary restrictions tracking** with allergy alerts
- Food waste tracking

**E - Eliminate:**
- No calorie goals/targets in MVP (just neutral data collection)
- No social features (private, personal tool)
- No food database searching (photo/barcode/manual only)
- No recipe input (future nice-to-have)
- Maybe: Simple gamification (streak counter for habit formation)

**R - Reverse/Rearrange:**
- None found useful for this use case

### Session 3: Mind Mapping (Visual Organization)

**Goal:** Organize ideas and discover connections

**Central Concept:** FOODIE APP - AI-Powered Energy Balance Tracker

**Major Branches Identified:**

```
📱 FOODIE APP
    |
    ├── 📸 INPUT METHODS
    │   ├── Photo capture (primary)
    │   │   ├── Video/burst mode for 3D analysis
    │   │   ├── Background processing
    │   │   └── Barcode auto-detection
    │   ├── Barcode scanning
    │   │   └── OpenFoodFacts lookup
    │   └── Voice/Manual entry
    │       ├── Quick calorie entry
    │       └── Repeat previous meals
    │
    ├── 💾 DATA & OUTPUT
    │   ├── Calories (primary)
    │   ├── Macros (protein, carbs, fat)
    │   ├── Full nutrition breakdown
    │   ├── Auto-categorization
    │   ├── Allergy warnings
    │   ├── Recipe recognition
    │   └── Google Health Connect sync
    │
    ├── 🎨 UX/UI
    │   ├── Widget (home/lock screen)
    │   ├── Ultra-fast camera launch
    │   ├── Background processing
    │   ├── Optional notifications
    │   ├── Chat interface for corrections
    │   └── Simple dashboard
    │
    └── 🔗 INTEGRATIONS
        ├── Google Health Connect
        ├── Garmin Connect
        └── Energy balance calculation
```

**Key Connection Discovered:** The two-mode flow
- **MODE 1 (Capture):** Widget → Camera → Snap → Pocket (3-5x daily, zero friction)
- **MODE 2 (Review):** Evening notification or manual check, see daily totals, edit if needed (1x daily, relaxed)

**Insight:** Energy balance is the real value proposition, not just food tracking

### Session 4: First Principles Thinking (Strip to Fundamentals)

**Goal:** Question all assumptions to find true MVP

**Fundamental Truths:**
1. You want to know daily calorie consumption
2. Azure OpenAI multimodal can estimate from photos
3. Minimal friction is critical

**Question 1: Do we need to store photos?**
- **Answer: NO!** Photos are just transport mechanism
- Flow: Capture → Send to AI → Extract data → Delete photo → Store only data
- Benefits: No storage bloat, privacy by default, simpler data model

**Question 2: Do we need a backend server?**
- **Answer: NO!** 
- Architecture: Mobile app → Azure OpenAI API → Google Health Connect (local)
- No backend, no database, no user accounts, no auth complexity

**Question 3: Do we need dashboard in MVP?**
- **Answer: NO!** (But will add in V2 because Google Fit view isn't good enough)
- MVP: Pure data capture, view in Google Fit or any health app
- V2: Custom dashboard when we know what users actually want to see

**Question 4: Does prompt need to be complex?**
- **Answer: NO!** Start minimal
- MVP prompt: `{calories: number, description: string}`
- Use structured JSON schema for reliability
- Iterate based on real usage

**Question 5: What's minimum viable success?**
- **Answer:** Faster + more accurate than current MyFitnessPal manual entry
- Current: Open app → Custom calories → Type estimate
- Success: Widget → Snap → Auto-save beats that flow

**Critical Addition from Discussion:**
- **CRUD operations required:** Must be able to edit and delete entries
- Need minimal list view for basic entry management

## Idea Categorization

### V1.0 - MVP (Immediate Opportunities)

_Ready to implement now - core value proposition_

- Home screen widget for instant camera access
- Photo capture (single photo)
- Azure OpenAI API integration with simple JSON prompt (`{calories, description}`)
- Save calories + description to Google Health Connect
- Quick edit capability for AI estimates
- Delete entries from Health Connect (CRUD operations)
- Delete photos after analysis (no storage)
- No backend/server (client-only architecture)
- Minimal list view for entry management

### V2.0 - Core Enhancements (Future Innovations)

_High-value features requiring development_

- Smart auto-categorization (breakfast/lunch/dinner/snacks based on time + food type)
- Full nutritional breakdown (macros: protein, carbs, fat)
- Custom dashboard (daily/weekly views with better UX than Google Fit)
- Daily summary notification (evening, with calories + protein)
- Chat interface for meal corrections ("this is tofu not chicken")
- Garmin Connect integration for energy balance tracking
- Lock screen widget (in addition to home screen)

### V3.0 - Smart Features

_Promising concepts needing refinement_

- Family meal portions ("I ate 1/4 of this")
- Allergy warnings based on user-defined allergens
- Before/after photos for portion adjustment
- Optional streak counter for habit formation
- Optional voice/text annotations after photo capture

### V4.0 - Advanced (Moonshots)

_Bold, transformative concepts for long-term_

- Video/burst mode for 3D food scanning (better volume estimation)
- Barcode detection and OpenFoodFacts integration
- Voice input for manual entries and corrections
- Food safety/freshness alerts
- Recipe recognition and linking (track home-cooked meals)
- Recipe suggestions for healthier versions of home-cooked meals
- Manual calorie entry for edge cases

### Insights and Learnings

_Key realizations from the session_

**Product Insights:**
- Success metric is comparative: Must be easier + more accurate than manual MyFitnessPal entry
- Context matters: Canteen, desk, public settings drive UX requirements (silent, fast, discreet)
- AI as enhancement, not replacement: User still needs to verify/correct estimates
- Data portability wins: Google Health Connect makes backend unnecessary
- Photos are ephemeral: No need to store long-term, just transport mechanism for data

**Technical Insights:**
- Azure OpenAI multimodal already proven effective (ChatGPT experience validation)
- Structured JSON outputs ensure reliable parsing
- Client-only architecture drastically simplifies development
- Mobile-first, widget-driven approach matches usage pattern
- Google Health Connect provides free, reliable data persistence

**Strategic Insights:**
- MyFitnessPal integration is redundant if building full tracker
- Garmin data integration changes value proposition from "food tracking" to "energy balance management"
- Dashboard quality matters more than dashboard existence (Google Fit inadequate)
- Real goal is energy balance (IN vs OUT), not just calorie counting
- Progressive enhancement from simple to sophisticated reduces risk

**UX Insights:**
- Two-mode flow (Capture vs Review) matches natural behaviour
- Invisible tracking reduces social anxiety and cognitive load
- Evening summary preferred over instant notifications
- Widget is primary interface, full app is secondary
- Edit capability essential - AI will make mistakes

## Action Planning

### Top 3 Priority Ideas

#### #1 Priority: Build Ultra-Fast MVP (Widget → Camera → AI → Save)

- **Rationale:** Prove the core hypothesis - can this beat MyFitnessPal manual entry? Get working software in hands ASAP to validate assumptions.
- **Next steps:**
  1. Choose mobile framework (Flutter vs React Native vs native iOS/Android)
  2. Set up Azure OpenAI API integration with structured outputs
  3. Build home screen widget
  4. Implement camera → API → Health Connect flow
  5. Add basic edit/delete UI for entry management
  6. Test with real meals for 1 week
- **Resources needed:** Mobile development skills, Azure OpenAI API key (already have), test device (Android for Health Connect)
- **Timeline:** 1-2 weeks for working prototype

#### #2 Priority: Optimize AI Prompt for Accuracy

- **Rationale:** The MVP lives or dies on AI accuracy. If estimates are consistently wrong by >30%, users won't trust it. Need to establish baseline accuracy and improve.
- **Next steps:**
  1. Test different prompt variations with real food photos
  2. Experiment with structured JSON schema variations
  3. Document which food types work well vs. poorly (e.g., soups hard to estimate)
  4. Establish acceptable accuracy threshold (±15%? ±20%?)
  5. Build test dataset of known-calorie meals for validation
  6. Consider few-shot examples in prompt for better estimation
- **Resources needed:** Time for experimentation, variety of food photos, known calorie references (nutrition labels)
- **Timeline:** Ongoing during MVP usage (week 2-4), parallel with development

#### #3 Priority: Research Garmin Connect API Integration

- **Rationale:** Energy balance (IN vs OUT) is the real value proposition. Understanding Garmin API limitations early prevents V2.0 roadblocks and might influence MVP architecture.
- **Next steps:**
  1. Investigate Garmin Connect API documentation thoroughly
  2. Determine if calorie expenditure data is accessible via API
  3. Identify authentication/permission requirements
  4. Check API rate limits and costs
  5. Explore alternatives if Garmin is too restrictive (Google Fit activity data, Apple Health workout data)
  6. Document integration feasibility and complexity for V2.0 planning
  7. Create proof-of-concept if API allows
- **Resources needed:** API research time, possibly Garmin developer account, test Garmin device
- **Timeline:** Week 2-3 (parallel with MVP testing), non-blocking research

## Reflection and Follow-up

### What Worked Well

The combination of techniques was incredibly effective:
- **What If Scenarios** opened up possibilities and revealed the "invisible tracking" philosophy early
- **SCAMPER** systematically found practical improvements (barcode scanning, auto-categorization, allergy warnings)
- **Mind Mapping** revealed the energy balance vision and helped eliminate redundant features (MyFitnessPal integration)
- **First Principles** stripped away complexity to find the true MVP - reducing scope from weeks to days of development

The progression from broad exploration → systematic improvement → visual organization → fundamental reduction created a complete picture while maintaining laser focus on what matters.

### Areas for Further Exploration

1. **Mobile platform decision** - Flutter vs React Native vs native (iOS/Android)
   - Flutter: Cross-platform, fast development
   - React Native: JavaScript familiarity
   - Native: Best performance, platform-specific features (widgets)
   
2. **Google Health Connect specifics** 
   - What exact data types to use for nutrition
   - Required permissions and user consent flow
   - Data sync reliability and conflict resolution
   
3. **Prompt engineering details** 
   - Exact wording for best accuracy
   - Few-shot examples to include
   - Temperature and top_p settings
   - System message vs user message structure
   
4. **Error handling strategy**
   - What happens when API fails or returns error
   - Network issues during capture
   - Photo quality too poor for analysis
   - Retry logic and user feedback
   
5. **Cost analysis** 
   - Azure OpenAI API costs per photo at scale
   - Image size optimization (resize before sending?)
   - Monthly budget estimates at different usage levels
   
6. **Privacy considerations** 
   - Photo handling and immediate deletion
   - Data retention policies
   - User trust and transparency
   - GDPR/privacy law compliance

### Recommended Follow-up Techniques

For your next brainstorming session (when ready for V2.0 planning):

- **Assumption Reversal** - Challenge assumptions about dashboard design and data visualization
- **Five Whys** - Dig into why certain features matter (like Garmin integration, specific macro tracking)
- **User Journey Mapping** - Detail the exact user experience from wake to sleep across a full day
- **Crazy 8s** - Rapid sketching of 8 different dashboard layouts in 8 minutes
- **Provocation Technique** - "What if food tracking made you eat MORE?" to find contrarian insights

### Questions That Emerged

1. **Should there be any offline capability for when network is unavailable?**
   - Store photos locally, process when connection returns?
   - Show "pending analysis" state?
   
2. **How to handle multi-user scenarios (family sharing device)?**
   - Does Health Connect support multiple profiles?
   - Or is this always single-user?
   
3. **What's the right balance between accuracy and speed?**
   - GPT-4o vs GPT-4o-mini (faster, cheaper, less accurate?)
   - User preference setting?
   
4. **Could the app learn from corrections over time?**
   - Personal calibration based on edit patterns
   - "You always correct chicken estimates down by 20%"
   - Fine-tuning opportunity?
   
5. **What happens if Azure OpenAI changes pricing or availability?**
   - Backup provider strategy (Google Gemini, Anthropic Claude)?
   - Allow users to configure their own API keys?
   
6. **How to handle edge cases?**
   - Non-food photos accidentally captured
   - Multiple meals in one photo
   - Drinks (liquid calories)
   - Supplements and vitamins

## Next Session Planning

**Suggested topics for future brainstorming:**
- V2.0 dashboard design deep-dive (after MVP validated)
- Garmin integration technical approach (after API research complete)
- Monetization strategy exploration (if relevant - freemium model? subscription?)
- Marketing and distribution strategy (App Store optimization, positioning)
- Advanced prompt engineering workshop (after baseline established)

**Recommended timeframe:** After MVP is built and tested for 2-4 weeks with real usage data

**Preparation needed:** 
- Real usage data from MVP (accuracy stats, usage frequency, pain points)
- Garmin API research findings and integration feasibility
- List of user-discovered pain points and feature requests
- Competitive analysis of similar apps
- Cost and monetization data if going commercial

---

_Session facilitated using the BMAD BMM brainstorming framework_

**Total session duration:** ~90 minutes
**Ideas generated:** 40+
**Techniques used:** 4 (What If Scenarios, SCAMPER, Mind Mapping, First Principles)
**MVP clarity achieved:** ✅ Clear path from concept to working software in 1-2 weeks
