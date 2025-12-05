# Sprint Change Proposal - V2/V3 Roadmap Revision

**Date:** 2025-11-25  
**Author:** John (PM Agent)  
**Trigger:** MVP completion + real-world usage insights  
**Change Scope:** Moderate (V2/V3 roadmap restructuring)  
**Status:** Pending Approval

---

## 1. Issue Summary

### Problem Statement

The MVP has been successfully completed and is functioning well for invisible meal tracking. However, real-world daily usage has revealed that the original V2/V3 feature roadmap doesn't fully align with actual use case priorities.

**Core Issue:** The app's primary goal is **body recomposition through caloric deficit tracking**, which requires comprehensive energy balance calculation (calories in vs. calories out). The original "Garmin Connect integration" approach was too narrow and relied on third-party bookkeeping, when a more robust, scientifically grounded, fully controllable approach is needed.

### Discovery Context

**When:** November 2025, after MVP completion and 2-3 weeks of daily usage  
**How:** Real-world app usage with 3-5 meal captures per day revealed:
- Missing visibility into total energy expenditure (calories out)
- Garmin only exports active calories, missing BMR and passive movement (NEAT)
- Need for transparent, explainable energy calculations
- Uncertainty about AI model cost/accuracy tradeoffs
- Higher-than-expected packaged food consumption (barcode scanning use case)

### Evidence

1. **Daily Usage Pattern:** 3-5 meal captures/day, consistent tracking adherence
2. **Missing Functionality:** No way to view caloric deficit/surplus despite that being the core goal
3. **Third-Party Limitations:** Garmin doesn't export BMR or passive calories to Health Connect
4. **Cost Concerns:** Unknown whether current model (GPT-4.1) is cost-optimal
5. **Packaged Food Frequency:** Frequent consumption of items with barcodes

---

## 2. Impact Analysis

### Epic Impact

#### REMOVED
- **Original V2: "Garmin Connect integration"**
  - Too narrow (only active calories)
  - Relies on third-party calculations (not transparent)
  - Doesn't address BMR or NEAT
  - **Replaced by:** Comprehensive energy balance calculation (Epic 6)

#### RESTRUCTURED - New Epic 6: Energy Balance & Caloric Deficit Tracking

**Goal:** Calculate complete energy expenditure (TDEE = BMR + NEAT + Active) to enable caloric deficit tracking for body recomposition.

**Components:**
1. **User Profile Settings**
   - Sex, age (manual input in Settings)
   - Weight, height (read from Health Connect with manual override)
   - Saves user inputs to Health Connect when manually entered

2. **Basal Metabolic Rate (BMR) Calculation**
   - Uses Mifflin-St Jeor Equation (clinically validated)
   - Males: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5
   - Females: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161

3. **Passive Energy Expenditure (NEAT)**
   - Calculates Non-Exercise Activity Thermogenesis from step data
   - Formula: PassiveCalories = steps × 0.04 kcal/step
   - Step data read from Health Connect (Garmin syncs automatically)
   - Conservative, peer-reviewed estimate

4. **Active Energy Expenditure**
   - Read directly from Health Connect → ActiveEnergyBurned
   - Captures Garmin workouts (running, cycling, etc.)

5. **Total Daily Energy Expenditure (TDEE)**
   - TDEE = BMR + Passive (NEAT) + Active
   - This is "Calories Out"

6. **Deficit/Surplus Dashboard**
   - Visualize Calories In (existing meal tracking) vs Calories Out (TDEE)
   - Show daily deficit or surplus
   - Support body recomposition goal

**Value:** Directly addresses the core body recomposition use case with scientifically grounded, fully controllable energy expenditure calculations.

#### RESTRUCTURED - New Epic 7: Enhanced Nutrition Tracking

**Changes:**
- **Macros tracking** - KEPT (already planned for V2)
- **Barcode scanning** - MOVED from V3 to V2 (higher priority than originally assessed)
- **Offline capability** - KEPT in V2 (network reliability varies in real-world usage)

**Rationale:** Packaged food consumption is higher than initially expected, making barcode scanning valuable for V2.

#### NEW - Epic 8: Model & Prompt Optimization

**Goal:** Provide tools to evaluate and optimize AI model selection and prompt engineering for cost/accuracy balance.

**Components:**
1. **Test Bench Framework**
   - Manage dataset of photos with known calorie values
   - Run batch analysis across multiple photos
   - Support model comparison (e.g., GPT-4.1 vs. GPT-4o-mini)

2. **Prompt Variation Testing**
   - Test different prompt formulations
   - Compare accuracy across prompt variations

3. **Accuracy/Cost Reporting**
   - Calculate accuracy metrics vs. known values
   - Estimate cost per analysis for each model
   - Support informed model selection decisions

**Value:** Enables data-driven optimization of AI cost/accuracy tradeoffs for sustainable long-term usage.

### Artifact Conflicts

#### PRD (docs/PRD.md)

**Section: "Growth Features (V2.0 - Post-MVP)" - MAJOR UPDATE REQUIRED**

Current content relies on "Garmin Connect integration" which is being replaced.

**Required Changes:**
1. Remove "Garmin Connect integration for energy balance"
2. Add comprehensive Energy Balance & Deficit Tracking description (Epic 6)
3. Add barcode scanning (moved from V3)
4. Keep macros tracking, offline capability
5. Add model/prompt optimization features (Epic 8)

**Section: "Vision Features (V3.0+ - Future)" - MINOR UPDATE**

**Required Changes:**
1. Remove "Barcode scanning + OpenFoodFacts integration" (moved to V2)

**Section: "Health Connect Integration" - EXPAND PERMISSIONS**

**Required Changes:**
1. Add READ_WEIGHT, WRITE_WEIGHT permissions
2. Add READ_HEIGHT, WRITE_HEIGHT permissions
3. Add READ_STEPS permission
4. Add READ_ACTIVE_CALORIES_BURNED permission

#### Epics (docs/epics.md)

**Required Changes:**
1. Add Epic 6: Energy Balance & Caloric Deficit Tracking (6-8 stories)
2. Add Epic 7: Enhanced Nutrition Tracking (modified V2, 3-4 stories)
3. Add Epic 8: Model & Prompt Optimization (2-3 stories)
4. Update epic ordering/priority to reflect new structure

#### Architecture (docs/architecture.md)

**Required Changes:**

1. **New Data Models:**
   - UserProfile (sex, age, weight, height)
   - EnergyBalance (bmr, neat, active, tdee)
   - StepData (for NEAT calculations)

2. **New Repositories:**
   - UserProfileRepository (manage user settings + HC integration)
   - EnergyBalanceRepository (BMR + NEAT + Active calculations)

3. **Expanded HealthConnectRepository:**
   - Add readWeight(), writeWeight()
   - Add readHeight(), writeHeight()
   - Add readSteps()
   - Add readActiveEnergyBurned()

4. **New ViewModels:**
   - EnergyBalanceViewModel (dashboard for deficit tracking)
   - UserProfileViewModel (settings for BMR inputs)

---

## 3. Recommended Approach

### Selected Path: Option 1 - Direct Adjustment

**Approach:**
- Update PRD V2.0 section with detailed energy balance requirements
- Create new Epic 6, 7, 8 definitions in epics.md
- Update architecture.md with new data models and repositories
- Create initial story outlines for Epic 6 (highest priority)

**Effort Estimate:** Medium (2-4 hours documentation updates)

**Risk Level:** Low
- MVP is complete and unchanged
- All new integrations use proven Health Connect patterns
- Scientific formulas are well-established
- No architectural conflicts

**Timeline Impact:** None (post-MVP roadmap only)

### Rationale

**Why Direct Adjustment:**
1. **MVP Integrity:** MVP (V1.0) is complete and successful - no changes needed
2. **Natural Evolution:** V2/V3 roadmaps are explicitly designed to evolve based on usage insights
3. **Clear Requirements:** Energy balance calculations have well-defined scientific formulas
4. **Low Risk:** All Health Connect integrations follow patterns proven in MVP
5. **High Value:** Directly addresses core body recomposition goal
6. **Reasonable Effort:** Standard documentation updates, no complex refactoring

**Alternatives Considered:**

**Option 2: Rollback**
- Status: Not Viable
- Reason: MVP is complete and working - no rollback needed

**Option 3: MVP Review**
- Status: Not Needed
- Reason: MVP scope is correct and validated - changes only affect V2/V3

### Trade-offs

**Considered but rejected:**
- **Defer Epic 6 to V3:** Energy balance is core value prop, should be V2
- **Use Garmin's calculations:** Not transparent/controllable, doesn't meet requirements
- **Skip model optimization:** Cost/accuracy tuning essential for sustainable usage

---

## 4. Detailed Change Proposals

### Change 1: PRD V2.0 Section Rewrite

**File:** `docs/PRD.md`  
**Section:** "Growth Features (V2.0 - Post-MVP)"

**OLD:**
```markdown
### Growth Features (V2.0 - Post-MVP)

**Enhanced Tracking:**
- Macros tracking (protein, carbs, fat) in addition to calories
- Custom dashboard/analytics within app
- Daily summary notifications
- Smart auto-categorization (breakfast/lunch/dinner/snacks based on time)

**Integration & Insights:**
- Garmin Connect integration for energy balance (calories in vs. calories out)
- Smart scale integration for body composition trend analysis
- Home screen widget (alternative access point)

**Offline Capability:**
- Photo queuing when network unavailable
- Automatic background processing when network restored
- Invisible retry logic (no user intervention required)
```

**NEW:**
```markdown
### Growth Features (V2.0 - Post-MVP)

**Energy Balance & Caloric Deficit Tracking:**
- User profile settings (sex, age, weight, height with Health Connect pre-population)
- Basal Metabolic Rate (BMR) calculation using Mifflin-St Jeor equation
- Passive energy expenditure (NEAT) from step data (0.04 kcal/step formula)
- Active energy expenditure from Health Connect ActiveEnergyBurned
- Total Daily Energy Expenditure (TDEE) = BMR + NEAT + Active
- Calories In vs Calories Out dashboard with deficit/surplus visualization
- Scientific formulas: fully transparent and controllable calculations

**Enhanced Nutrition Tracking:**
- Macros tracking (protein, carbs, fat) in addition to calories
- Barcode scanning + OpenFoodFacts integration for packaged foods
- Offline capability: photo queuing when network unavailable with automatic retry
- Smart auto-categorization (breakfast/lunch/dinner/snacks based on time)
- Daily summary notifications

**Model & Prompt Optimization:**
- Test bench for model comparison (GPT-4.1 vs alternatives)
- Prompt variation testing framework
- Accuracy evaluation against known-calorie photo dataset
- Cost/accuracy reporting to optimize API usage

**Additional Enhancements:**
- Smart scale integration for body composition trend analysis
- Custom analytics and trend visualization
```

**Rationale:** Replaces narrow "Garmin integration" with comprehensive, scientifically grounded energy balance calculation. Elevates barcode scanning to V2. Adds model optimization tools for sustainable usage.

---

### Change 2: PRD V3.0 Section Update

**File:** `docs/PRD.md`  
**Section:** "Vision Features (V3.0+ - Future)"

**OLD:**
```markdown
### Vision Features (V3.0+ - Future)

**Advanced Capture:**
- Before/after photos for portion size adjustment
- Family meal portions: "I ate 1/4 of this casserole"
- Voice annotations for context or corrections
- Barcode scanning + OpenFoodFacts integration for packaged foods

**Intelligence:**
- Recipe recognition from home-cooked meals
- Allergy warnings based on user profile
- Food safety/freshness detection
- Video/burst mode for 3D food volume estimation
```

**NEW:**
```markdown
### Vision Features (V3.0+ - Future)

**Advanced Capture:**
- Before/after photos for portion size adjustment
- Family meal portions: "I ate 1/4 of this casserole"
- Voice annotations for context or corrections

**Intelligence:**
- Recipe recognition from home-cooked meals
- Allergy warnings based on user profile
- Food safety/freshness detection
- Video/burst mode for 3D food volume estimation
```

**Rationale:** Remove barcode scanning (moved to V2).

---

### Change 3: PRD Health Connect Permissions Expansion

**File:** `docs/PRD.md`  
**Section:** "Device Features & Permissions"

**OLD:**
```markdown
- Health Connect permissions:
  - `READ_NUTRITION` - View existing nutrition entries
  - `WRITE_NUTRITION` - Save calorie data to Health Connect
```

**NEW:**
```markdown
- Health Connect permissions:
  - `READ_NUTRITION` - View existing nutrition entries
  - `WRITE_NUTRITION` - Save calorie data to Health Connect
  - `READ_WEIGHT` - Read user weight for BMR calculation
  - `WRITE_WEIGHT` - Save manually entered weight to Health Connect
  - `READ_HEIGHT` - Read user height for BMR calculation
  - `WRITE_HEIGHT` - Save manually entered height to Health Connect
  - `READ_STEPS` - Read daily step count for NEAT calculation
  - `READ_ACTIVE_CALORIES_BURNED` - Read active energy expenditure
```

**Rationale:** Enable energy balance calculations by accessing weight, height, steps, and active calories from Health Connect.

---

### Change 4: Create Epic 6 in Epics.md

**File:** `docs/epics.md`  
**Action:** Add new epic after Epic 5

**NEW CONTENT:**
```markdown
## Epic 6: Energy Balance & Caloric Deficit Tracking

**Goal:** Enable comprehensive caloric deficit tracking by calculating Total Daily Energy Expenditure (TDEE = BMR + NEAT + Active) and visualizing Calories In vs Calories Out.

**Value:** Directly addresses the core body recomposition use case with scientifically grounded, fully controllable energy expenditure calculations. Eliminates reliance on third-party tools (Garmin, MyFitnessPal) for energy balance tracking.

**Scientific Foundation:**
This epic implements peer-reviewed formulas for energy expenditure:
- **BMR:** Mifflin-St Jeor Equation (most accurate general-population formula)
- **NEAT:** Step-based calculation using 0.04 kcal/step (peer-reviewed NEAT research)
- **Active:** Direct from Health Connect ActiveEnergyBurned

---

### Story 6.1: User Profile Settings

As a user,
I want to configure my sex, age, weight, and height in settings,
So that the app can calculate my Basal Metabolic Rate accurately.

**Acceptance Criteria:**

**Given** BMR calculation requires user profile data
**When** I open the Settings screen
**Then** I see fields for Sex (Male/Female), Age (years), Weight (kg), Height (cm)

**And** Weight and Height fields are pre-populated from Health Connect if available

**And** I can manually override Weight and Height values

**And** Manual Weight/Height entries are saved back to Health Connect

**And** All profile values persist across app restarts

**And** Clear labels explain why each field is needed: "Used for BMR calculation"

**Prerequisites:** Epic 5 complete (Settings screen exists)

**Technical Notes:** 
- Use HealthConnectClient to read WeightRecord and HeightRecord
- If manual values entered, write new records to Health Connect
- Store sex and age in SharedPreferences (not in Health Connect)
- Validate inputs: age 13-120, weight 30-300kg, height 100-250cm

---

### Story 6.2: BMR Calculation

As a user,
I want my Basal Metabolic Rate calculated automatically,
So that I know my minimum daily energy requirement.

**Acceptance Criteria:**

**Given** my profile is configured (sex, age, weight, height)
**When** the app calculates my BMR
**Then** it uses the Mifflin-St Jeor Equation

**And** for males: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) + 5

**And** for females: BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age_years) - 161

**And** BMR is recalculated when profile values change

**And** BMR value is displayed in energy balance dashboard with label "Basal Metabolic Rate"

**And** calculation happens in real-time (no lag)

**Prerequisites:** Story 6.1 (user profile configured)

**Technical Notes:**
- Create EnergyBalanceRepository with calculateBMR(sex, age, weight, height) method
- Return Result<Double, Error> with BMR in kcal/day
- Cache result in ViewModel until profile changes
- Round to nearest integer for display

---

### Story 6.3: Passive Energy (NEAT) Calculation

As a user,
I want my passive energy expenditure calculated from my daily steps,
So that I account for non-exercise movement calories.

**Acceptance Criteria:**

**Given** Health Connect contains step count data
**When** the app calculates passive energy expenditure
**Then** it reads daily step count from Health Connect

**And** it uses formula: PassiveCalories = steps × 0.04 kcal/step

**And** step data is queried for the current day (midnight to now)

**And** calculation updates automatically as new step data arrives

**And** NEAT value is displayed in energy balance dashboard with label "Passive Activity (NEAT)"

**And** if step data is unavailable, shows "No step data available"

**Prerequisites:** Story 6.1 (Health Connect permissions), Story 6.2 (BMR calculation exists)

**Technical Notes:**
- Use HealthConnectClient to query StepsRecord with TimeRangeFilter
- Sum all step records for current day
- Implement 0.04 kcal/step constant (can be made configurable later)
- Handle case where no step data exists gracefully

---

### Story 6.4: Active Energy Expenditure

As a user,
I want my active exercise calories read from Health Connect,
So that I account for workout energy expenditure.

**Acceptance Criteria:**

**Given** Health Connect contains active calorie data from Garmin
**When** the app calculates active energy expenditure
**Then** it reads ActiveCaloriesBurned records from Health Connect

**And** active calories are queried for the current day (midnight to now)

**And** all active calorie records are summed

**And** Active value is displayed in energy balance dashboard with label "Active Exercise"

**And** if no active data exists, shows "0 kcal"

**And** data refreshes automatically when new workouts are synced

**Prerequisites:** Story 6.1 (Health Connect permissions), Story 6.3 (NEAT calculation)

**Technical Notes:**
- Use HealthConnectClient to query ActiveCaloriesBurnedRecord
- Use TimeRangeFilter for current day
- Sum all records
- Handle Garmin sync delays gracefully (data may arrive minutes after workout)

---

### Story 6.5: Total Daily Energy Expenditure (TDEE)

As a user,
I want my total daily energy expenditure calculated automatically,
So that I know my total "Calories Out" for the day.

**Acceptance Criteria:**

**Given** BMR, NEAT, and Active are all calculated
**When** the app computes TDEE
**Then** it uses formula: TDEE = BMR + NEAT + Active

**And** TDEE updates automatically when any component changes

**And** TDEE is displayed prominently in energy balance dashboard

**And** calculation is real-time with no perceptible lag

**And** TDEE shows breakdown: "BMR: X + Passive: Y + Active: Z = Total: TDEE"

**Prerequisites:** Stories 6.2, 6.3, 6.4 (all energy components calculated)

**Technical Notes:**
- Create EnergyBalanceViewModel to observe all three components
- Use Kotlin Flow to reactively calculate TDEE
- Cache result until any input changes
- Display with clear breakdown for transparency

---

### Story 6.6: Calories In vs Calories Out Dashboard

As a user,
I want to see my daily calories consumed vs calories burned,
So that I can track my caloric deficit or surplus.

**Acceptance Criteria:**

**Given** I have logged meals (Calories In) and TDEE is calculated (Calories Out)
**When** I view the energy balance dashboard
**Then** I see "Calories In: X kcal" from today's meals

**And** I see "Calories Out: Y kcal" (TDEE)

**And** I see "Deficit/Surplus: Z kcal" (Out - In, with colour coding)

**And** deficit shows in green with negative number (e.g., "-500 kcal deficit")

**And** surplus shows in red with positive number (e.g., "+200 kcal surplus")

**And** dashboard is accessible from main navigation

**And** data updates in real-time as meals are logged or activity data syncs

**And** empty state shows "Log your first meal to start tracking"

**Prerequisites:** Story 6.5 (TDEE calculated), Epic 1-5 (meal tracking working)

**Technical Notes:**
- Create EnergyBalanceDashboardScreen (Compose)
- Query today's NutritionRecords from Health Connect for Calories In
- Display TDEE from EnergyBalanceViewModel for Calories Out
- Use Material 3 colour scheme for deficit (green) vs surplus (red)
- Implement pull-to-refresh

---

### Story 6.7: Weight/Height Health Connect Sync

As a user,
I want my manually entered weight and height saved to Health Connect,
So that other health apps can use my latest measurements.

**Acceptance Criteria:**

**Given** I manually enter weight or height in Settings
**When** I save the changes
**Then** a WeightRecord is written to Health Connect with current timestamp

**And** a HeightRecord is written to Health Connect with current timestamp

**And** records are visible in other Health Connect apps (Google Fit)

**And** if I update weight daily, each entry creates a new timestamped record

**And** BMR automatically recalculates with new values

**Prerequisites:** Story 6.1 (user profile settings), Story 6.2 (BMR calculation)

**Technical Notes:**
- Use HealthConnectClient.insertRecords() for WeightRecord and HeightRecord
- Set metadata with current timestamp and source as "Foodie"
- Handle Health Connect permission errors gracefully
- Trigger BMR recalculation after successful save
```

**Rationale:** Epic 6 provides comprehensive energy balance tracking with scientifically validated formulas, enabling the core caloric deficit tracking use case.

---

### Change 5: Create Epic 7 in Epics.md

**File:** `docs/epics.md`  
**Action:** Add new epic after Epic 6

**NEW CONTENT:**
```markdown
## Epic 7: Enhanced Nutrition Tracking

**Goal:** Improve nutrition tracking accuracy and convenience through macros support, barcode scanning, and offline capability.

**Value:** Provides richer nutritional data (macros), faster entry for packaged foods (barcode), and reliable operation in variable network conditions (offline queuing).

---

### Story 7.1: Macros Tracking (Protein, Carbs, Fat)

As a user,
I want to track protein, carbs, and fat macros in addition to calories,
So that I can ensure proper macronutrient balance for body recomposition.

**Acceptance Criteria:**

**Given** the AI analyses a food photo
**When** the API request is sent
**Then** the prompt requests macros: `{calories: number, protein: number, carbs: number, fat: number, description: string}`

**And** macros are measured in grams

**And** macros are saved to Health Connect NutritionRecord (protein, carbohydrates, totalFat fields)

**And** meal list view displays macros below calories: "P: Xg | C: Yg | F: Zg"

**And** edit screen allows editing macros values

**And** energy balance dashboard shows daily macro totals

**Prerequisites:** Epic 2 complete (API integration working), Epic 3 complete (CRUD operations)

**Technical Notes:**
- Modify Azure OpenAI prompt to request 5 fields instead of 2
- Update NutritionData model to include protein, carbs, fat
- Save to NutritionRecord: protein (Mass.grams), totalCarbohydrate (Mass.grams), totalFat (Mass.grams)
- Update UI to display macros consistently across all screens

---

### Story 7.2: Barcode Scanning Integration

As a user,
I want to scan barcodes on packaged foods,
So that I can quickly log nutrition data without photographing food.

**Acceptance Criteria:**

**Given** I'm logging a packaged food item
**When** I tap "Scan Barcode" button in the app
**Then** the camera opens in barcode scan mode

**And** barcode is detected and decoded automatically

**And** the app queries OpenFoodFacts API with the barcode

**And** if product found, nutrition data pre-fills (calories, protein, carbs, fat)

**And** user can adjust serving size before saving

**And** data is saved to Health Connect as normal nutrition entry

**And** if barcode not found, shows "Product not found - use camera instead"

**Prerequisites:** Epic 2 complete (camera integration), Story 7.1 (macros support)

**Technical Notes:**
- Use ML Kit Barcode Scanning API (Google)
- Integrate OpenFoodFacts API (free, open database)
- Create BarcodeRepository for API calls
- Handle serving size conversions (per 100g → actual serving)
- Fallback to photo capture if barcode lookup fails

---

### Story 7.3: Offline Photo Queuing

As a user,
I want photos queued locally when I'm offline,
So that I don't lose meal entries when network is unavailable.

**Acceptance Criteria:**

**Given** I capture a photo while offline
**When** the background service detects no network
**Then** the photo is saved to encrypted local storage

**And** a notification shows: "Offline - meal queued for analysis"

**And** the app monitors network status

**And** when network is restored, queued photos are processed automatically

**And** processing happens in capture order (FIFO queue)

**And** notification updates: "Processing queued meal X of Y..."

**And** photos are deleted after successful processing

**And** queued meals are retained for up to 7 days (then auto-deleted with warning)

**Prerequisites:** Epic 4 complete (error handling), Epic 2 complete (background processing)

**Technical Notes:**
- Use WorkManager with Constraints.CONNECTED network requirement
- Store photo paths in WorkManager Data with timestamp
- Implement queue management (FIFO)
- Use encrypted storage for photos (EncryptedFile)
- Monitor ConnectivityManager for network state changes
```

**Rationale:** Epic 7 enhances nutrition tracking with macros, barcode scanning (elevated from V3), and robust offline capability.

---

### Change 6: Create Epic 8 in Epics.md

**File:** `docs/epics.md`  
**Action:** Add new epic after Epic 7

**NEW CONTENT:**
```markdown
## Epic 8: Model & Prompt Optimization

**Goal:** Provide tools to evaluate and optimize AI model selection and prompt engineering for cost/accuracy balance.

**Value:** Enables data-driven decisions about which Azure OpenAI model to use and how to phrase prompts, optimizing the tradeoff between API cost and nutrition estimate accuracy.

---

### Story 8.1: Test Dataset Management

As a user,
I want to create a test dataset of photos with known calorie values,
So that I can benchmark AI model accuracy.

**Acceptance Criteria:**

**Given** I want to test model accuracy
**When** I add a photo to the test dataset
**Then** I can capture or select a photo

**And** I enter the known calorie value (ground truth)

**And** I optionally enter known macros (protein, carbs, fat)

**And** photo and metadata are saved to test dataset

**And** I can view all test dataset photos in a list

**And** I can edit or delete test dataset entries

**And** test dataset persists across app restarts

**Prerequisites:** Epic 2 complete (photo handling)

**Technical Notes:**
- Create TestDatasetRepository with local storage
- Store photos in app files directory (not cache - persistent)
- Store metadata in local database (Room) or JSON file
- Implement CRUD operations for dataset management

---

### Story 8.2: Model Comparison Test Runner

As a user,
I want to run my test dataset through different AI models,
So that I can compare accuracy and choose the best model.

**Acceptance Criteria:**

**Given** I have a test dataset with 10+ photos
**When** I run a model comparison test
**Then** I select which model to test (e.g., "gpt-4.1", "gpt-4o-mini")

**And** the app processes all test photos through the selected model

**And** for each photo, it records: estimated calories, actual calories, error

**And** after completion, I see a summary report:
  - Mean Absolute Error (MAE)
  - Mean Absolute Percentage Error (MAPE)
  - Number of photos tested
  - Estimated API cost (tokens used × model pricing)

**And** I can save test results for later comparison

**And** I can export results as CSV or JSON

**Prerequisites:** Story 8.1 (test dataset exists), Epic 2 (API integration)

**Technical Notes:**
- Create TestRunnerRepository to execute batch analysis
- Calculate accuracy metrics: MAE, MAPE
- Estimate cost based on token usage from API response
- Store test results with timestamp and model name
- Support concurrent testing of multiple models

---

### Story 8.3: Prompt Variation Testing

As a user,
I want to test different prompt formulations,
So that I can optimize prompt engineering for better accuracy.

**Acceptance Criteria:**

**Given** I want to test prompt variations
**When** I create a prompt variant
**Then** I can enter custom system instructions

**And** I can enter custom user prompts

**And** I save the prompt variant with a descriptive name

**And** I run the test dataset through the prompt variant

**And** results show accuracy metrics same as model comparison

**And** I can compare multiple prompt variants side-by-side

**And** I can set the winning variant as the active app prompt

**Prerequisites:** Story 8.1 (test dataset), Story 8.2 (test runner)

**Technical Notes:**
- Store prompt variants in local database
- Override default prompt during test runs
- Create comparison view showing variants side-by-side
- Allow activating a variant as the production prompt
- Test with same model but different prompts
```

**Rationale:** Epic 8 provides data-driven tools for optimizing AI cost and accuracy, essential for sustainable long-term usage.

---

## 5. Implementation Handoff

### Change Scope Classification

**Scope:** Moderate

**Justification:**
- Significant new content but well-defined requirements
- No changes to completed MVP
- Purely additive to V2/V3 planning
- Standard documentation updates

### Handoff Plan

**Phase 1: PM Updates (Current Workflow)**
- **Responsibility:** Product Manager (PM Agent - John)
- **Tasks:**
  1. Update PRD V2.0 section with approved changes
  2. Update PRD V3.0 section (remove barcode)
  3. Update PRD Health Connect permissions
  4. Create Epic 6, 7, 8 definitions in epics.md
  5. Ensure scientific accuracy of energy balance formulas

- **Timeline:** Complete after user approval of this proposal
- **Deliverables:**
  - Updated PRD.md
  - Updated epics.md with Epic 6, 7, 8
  - Change log documenting updates

**Phase 2: SM Story Development (Follow-up)**
- **Responsibility:** Scrum Master (SM Agent)
- **Tasks:**
  1. Create detailed story files for Epic 6 (priority)
  2. Create story context files for key Epic 6 stories
  3. Define comprehensive acceptance criteria
  4. Create Epic 7 and 8 stories (lower priority)
  5. Estimate story points and sprint capacity

- **Timeline:** After PM updates complete
- **Deliverables:**
  - Story files in docs/stories/
  - Story context XML files
  - Sprint plan for Epic 6

**Phase 3: Development (Future)**
- **Responsibility:** Developer (Dev Agent)
- **Tasks:**
  1. Implement Epic 6 stories in priority order
  2. Follow established Health Connect patterns
  3. Create new repositories and ViewModels per architecture
  4. Unit test energy balance calculations
  5. Manual test energy balance dashboard

- **Timeline:** After story development complete
- **Deliverables:**
  - Working Epic 6 implementation
  - Unit tests for BMR, NEAT calculations
  - Updated architecture documentation

### Success Criteria

**Documentation Phase (PM):**
- ✅ PRD V2.0 section accurately describes Epic 6, 7, 8
- ✅ Energy balance formulas are scientifically accurate
- ✅ Epic definitions are clear and actionable
- ✅ No conflicts with existing MVP documentation

**Story Development Phase (SM):**
- ✅ All Epic 6 stories have clear acceptance criteria
- ✅ Stories are independently implementable
- ✅ Technical notes provide implementation guidance
- ✅ Story context files created for complex stories

**Implementation Phase (Dev):**
- ✅ Energy balance calculations are mathematically correct
- ✅ All Health Connect integrations working
- ✅ Dashboard displays accurate Calories In vs Out
- ✅ Manual testing validates real-world usage

---

## 6. Approval & Next Steps

### Approval Request

**Requesting approval from:** BMad (Product Owner)

**This proposal requests approval to:**
1. Update PRD V2.0 section with energy balance tracking (Epic 6)
2. Restructure V2/V3 roadmap with Epic 6, 7, 8
3. Remove narrow "Garmin integration" in favor of comprehensive energy balance
4. Move barcode scanning from V3 to V2
5. Add model/prompt optimization features (Epic 8)

**No changes to:** MVP (V1.0) - complete and validated

### Questions for Review

1. **Energy Balance Approach:** Does the scientific formula approach (BMR + NEAT + Active) align with your body recomposition goals?

2. **Epic Priority:** Agree with Epic 6 (Energy Balance) as highest V2 priority?

3. **NEAT Formula:** Is 0.04 kcal/step an acceptable starting point? (Can be refined later)

4. **Model Optimization:** Is Epic 8 (test bench) valuable for V2, or defer to V3?

5. **Offline Capability:** Confirm you want offline queuing in V2?

### Next Actions (After Approval)

**Immediate (PM):**
1. Update PRD.md with approved changes
2. Update epics.md with Epic 6, 7, 8
3. Create change log documenting updates

**Short-term (SM):**
1. Create Story 6.1 through 6.7 files
2. Develop Epic 7 and 8 stories
3. Plan first V2 sprint (Epic 6 focus)

**Medium-term (Dev):**
1. Implement Epic 6 stories
2. Manual test energy balance calculations
3. Validate with real-world usage

---

## 7. Summary

**Change Trigger:** MVP completion + real-world usage insights

**Core Change:** Replace narrow "Garmin integration" with comprehensive, scientifically grounded energy balance calculation (Epic 6: BMR + NEAT + Active → TDEE)

**Additional Changes:**
- Move barcode scanning from V3 → V2 (Epic 7)
- Add model/prompt optimization tools (Epic 8)
- Keep offline capability in V2

**Impact:** Moderate - significant V2/V3 roadmap restructuring, no MVP changes

**Effort:** 2-4 hours documentation updates

**Risk:** Low - all integrations follow proven Health Connect patterns

**Value:** High - directly addresses core body recomposition goal with transparent, controllable energy expenditure tracking

---

**Status:** Awaiting approval from BMad

**Document Version:** 1.0  
**Created:** 2025-11-25  
**Author:** John (PM Agent)
