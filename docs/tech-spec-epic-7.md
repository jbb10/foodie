# Epic Technical Specification: Enhanced Nutrition Tracking

Date: 2025-11-30
Author: Jóhann
Epic ID: 7
Status: Draft

---

## Overview

Epic 7 enhances Foodie's nutrition tracking capabilities with three key features: macronutrient tracking (protein, carbs, fat), barcode scanning for packaged foods, and offline photo queuing. These improvements address the post-MVP value-add identified in the PRD, providing richer nutritional data for body recomposition goals, faster entry methods for common packaged foods, and reliable operation in variable network conditions.

This epic builds directly on the foundation established in Epics 2 (AI-powered capture), 3 (data management), and 4 (error handling). It leverages existing Health Connect integration, Azure OpenAI API infrastructure, and WorkManager background processing while adding new capabilities for macros tracking, barcode recognition via ML Kit, and offline resilience via encrypted local queuing.

## Objectives and Scope

**In Scope:**
- **Macros Tracking:** Extend AI nutrition analysis to extract protein, carbs, and fat in addition to calories; save macros to Health Connect NutritionRecord fields; display macros in meal list and edit screens; aggregate macros in Energy Balance dashboard
- **Barcode Scanning:** Integrate ML Kit Barcode Scanning API for packaged food detection; query OpenFoodFacts API for nutrition data retrieval; pre-fill nutrition form with barcode data; handle serving size conversions
- **Offline Queuing:** Queue captured photos locally when network unavailable; encrypt queued photos for security; process queue in FIFO order when connectivity restored; notify user of queue status; auto-delete queued photos after 7 days

**Out of Scope:**
- Custom barcode database (rely on OpenFoodFacts public API)
- Manual macros entry without photo/barcode (deferred to future)
- Cloud backup of queued photos (local encrypted storage only)
- Advanced offline conflict resolution (simple FIFO queue sufficient for personal use)

## System Architecture Alignment

**Architectural Components Affected:**
- **Data Layer:** Extend `NutritionData` domain model with macros fields (protein, carbs, fat); add `BarcodeRepository` for OpenFoodFacts API integration; implement `OfflineQueueManager` for encrypted photo storage
- **Repository Layer:** Update `MealRepository` and `HealthConnectRepository` to handle macros fields in CRUD operations; add barcode lookup logic
- **Worker Layer:** Extend `AnalyseMealWorker` to parse macros from Azure OpenAI response; implement `ProcessQueueWorker` for offline queue processing with network constraints
- **UI Layer:** Update `MealListScreen`, `MealDetailScreen`, and `EnergyBalanceDashboardScreen` to display macros; add `BarcodeScannerScreen` for barcode capture
- **Integration Points:** Azure OpenAI API (updated prompt for macros), OpenFoodFacts API (new integration), Health Connect (macros fields: protein, totalCarbohydrate, totalFat), ML Kit Barcode Scanning (new dependency)

**Architecture Consistency:**
- Follows established MVVM pattern with ViewModel → Use Case → Repository flow
- Reuses existing error handling infrastructure (ErrorHandler, NetworkMonitor from Epic 4)
- Leverages WorkManager for background processing (consistent with Epic 2)
- Maintains Health Connect as single source of truth (no local database needed)
- Applies EncryptedSharedPreferences pattern for offline queue security (consistent with API key storage)

## Detailed Design

### Services and Modules

| Module | Responsibility | Inputs | Outputs | Owner |
|--------|---------------|--------|---------|-------|
| **MacrosExtractor** | Parse macros from Azure OpenAI response | Azure OpenAI JSON output | `ParsedNutrition(calories, protein, carbs, fat, description)` | data/remote/parser |
| **BarcodeRepository** | Query OpenFoodFacts API for nutrition data | Barcode string | `Result<NutritionData>` | data/repository |
| **OfflineQueueManager** | Manage encrypted photo queue for offline scenarios | Photo URI, timestamp | Queue operations (enqueue, dequeue, peek) | data/local/queue |
| **ProcessQueueWorker** | Background processing of queued photos when online | WorkManager trigger | Result (success/retry/failure) | data/worker |
| **BarcodeScannerViewModel** | Coordinate barcode scan → lookup → form pre-fill | User scan action | Scanned nutrition data state | ui/screens/barcode |
| **HealthConnectRepository (Extended)** | CRUD operations for macros in NutritionRecord | Macros data (protein, carbs, fat) | Result<Unit> | data/repository |

**Module Dependencies:**
- MacrosExtractor → Azure OpenAI API response format
- BarcodeRepository → OpenFoodFacts API, NetworkMonitor
- OfflineQueueManager → EncryptedFile, WorkManager
- ProcessQueueWorker → OfflineQueueManager, AnalyseMealWorker logic, NetworkMonitor
- BarcodeScannerViewModel → BarcodeRepository, ML Kit Barcode Scanning
- HealthConnectRepository → Health Connect SDK (protein, totalCarbohydrate, totalFat fields)

### Data Models and Contracts

**Extended Domain Models:**

```kotlin
// domain/model/NutritionData.kt (extended)
data class NutritionData(
    val calories: Int,
    val protein: Int,      // NEW: grams
    val carbs: Int,        // NEW: grams
    val fat: Int,          // NEW: grams
    val description: String
) {
    init {
        require(calories in 1..5000) { "Calories must be between 1 and 5000" }
        require(protein in 0..500) { "Protein must be between 0 and 500g" }
        require(carbs in 0..1000) { "Carbs must be between 0 and 1000g" }
        require(fat in 0..500) { "Fat must be between 0 and 500g" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}

// domain/model/MealEntry.kt (extended)
data class MealEntry(
    val id: String,
    val timestamp: Instant,
    val description: String,
    val calories: Int,
    val protein: Int,      // NEW
    val carbs: Int,        // NEW
    val fat: Int           // NEW
)
```

**Offline Queue Models:**

```kotlin
// data/local/queue/QueuedPhoto.kt
@Parcelize
data class QueuedPhoto(
    val id: String = UUID.randomUUID().toString(),
    val photoUri: Uri,
    val timestamp: Instant,
    val retryCount: Int = 0
) : Parcelable

// data/local/queue/QueueStatus.kt
sealed class QueueStatus {
    data object Empty : QueueStatus()
    data class Pending(val count: Int) : QueueStatus()
    data class Processing(val current: Int, val total: Int) : QueueStatus()
}

// domain/model/MealListItem.kt (NEW for Story 7.3)
sealed class MealListItem {
    abstract val timestamp: Instant
    
    data class Analysed(
        val mealEntry: MealEntry  // Contains id, timestamp, description, calories, protein, carbs, fat
    ) : MealListItem() {
        override val timestamp: Instant = mealEntry.timestamp
    }
    
    data class Queued(
        val queuedPhoto: QueuedPhoto  // Contains id, photoUri, timestamp, retryCount
    ) : MealListItem() {
        override val timestamp: Instant = queuedPhoto.timestamp
    }
}
```

**Barcode Models:**

```kotlin
// data/remote/dto/OpenFoodFactsResponse.kt
data class OpenFoodFactsResponse(
    val status: Int,
    val product: Product?
)

data class Product(
    val product_name: String,
    val nutriments: Nutriments,
    val serving_size: String?
)

data class Nutriments(
    val energy_kcal_100g: Double?,
    val proteins_100g: Double?,
    val carbohydrates_100g: Double?,
    val fat_100g: Double?
)
```

### APIs and Interfaces

**Azure OpenAI Request (Macros Extension):**

⚠️ **IMPORTANT - NOT PRESCRIPTIVE:** The current app already has a well-optimized prompt at `/app/app/src/main/assets/prompts/nutrition_analysis.md` that includes detailed estimation logic, packaged food handling, label OCR, and confidence scoring. **Developers MUST read and augment the existing prompt**, not replace it. The example below is for reference only to illustrate the macros addition.

**🔧 REQUIRED: Azure OpenAI Structured Outputs (JSON Schema)**

Story 7.1 MUST implement Azure OpenAI's structured outputs feature to guarantee response format:
- Use `response_format` parametre with `json_schema` type (see: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/structured-outputs)
- Define JSON schema with required fields: `hasFood`, `calories`, `protein`, `carbs`, `fat`, `description` (plus existing fields)
- This replaces relying solely on prompt instructions for format compliance
- Benefit: Eliminates JSON parsing errors, guarantees field presence, enables compile-time type safety

**Example JSON Schema for Structured Outputs (Story 7.1):**

```json
{
  "type": "json_schema",
  "json_schema": {
    "name": "nutrition_analysis_response",
    "strict": true,
    "schema": {
      "type": "object",
      "required": ["hasFood", "calories", "protein", "carbs", "fat", "description", "confidence"],
      "properties": {
        "hasFood": {"type": "boolean"},
        "reason": {"type": "string"},
        "calories": {"type": "integer", "minimum": 1, "maximum": 5000},
        "protein": {"type": "integer", "minimum": 0, "maximum": 500},
        "carbs": {"type": "integer", "minimum": 0, "maximum": 1000},
        "fat": {"type": "integer", "minimum": 0, "maximum": 500},
        "description": {"type": "string"},
        "confidence": {"type": "number", "minimum": 0.0, "maximum": 1.0},
        "caloriesRange": {
          "type": "object",
          "properties": {
            "low": {"type": "integer"},
            "high": {"type": "integer"}
          }
        },
        "items": {"type": "array"},
        "assumptions": {"type": "array"}
      },
      "additionalProperties": false
    }
  }
}
```

**Reference Example - Macros Request (augment existing prompt, don't replace):**

```http
POST https://{resource}.openai.azure.com/openai/v1/responses
api-key: {api-key}
Content-Type: application/json

{
  "model": "gpt-4.1",
  "instructions": "<EXISTING PROMPT FROM nutrition_analysis.md> + Add: Also estimate macronutrients (protein, carbs, fat in grams) for detected food items.",
  "response_format": { /* JSON schema shown above */ },
  "input": [
    {
      "role": "user",
      "content": [
        {
          "type": "input_text",
          "text": "Analyse this meal and estimate calories and macros (protein, carbs, fat in grams)."
        },
        {
          "type": "input_image",
          "image_url": "data:image/jpeg;base64,..."
        }
      ]
    }
  ]
}
```

**Example Response (guaranteed format via structured outputs):**
```json
{
  "hasFood": true,
  "calories": 650,
  "protein": 45,
  "carbs": 60,
  "fat": 20,
  "description": "Grilled chicken breast with brown rice and steamed broccoli",
  "confidence": 0.85,
  "caloriesRange": {"low": 600, "high": 700},
  "items": [
    {"name": "chicken breast", "quantity": "150g", "kcal": 250, "protein": 40, "carbs": 0, "fat": 8},
    {"name": "brown rice", "quantity": "200g cooked", "kcal": 220, "protein": 5, "carbs": 45, "fat": 2},
    {"name": "broccoli", "quantity": "100g", "kcal": 35, "protein": 3, "carbs": 7, "fat": 0}
  ],
  "assumptions": ["chicken grilled without oil", "rice plain cooked", "broccoli steamed"]
}
```

**OpenFoodFacts API:**

```kotlin
interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String
    ): OpenFoodFactsResponse
}
```

**Example Request:**
```http
GET https://world.openfoodfacts.org/api/v2/product/737628064502
```

**Example Response:**
```json
{
  "status": 1,
  "product": {
    "product_name": "Protein Bar",
    "nutriments": {
      "energy-kcal_100g": 400,
      "proteins_100g": 20,
      "carbohydrates_100g": 40,
      "fat_100g": 15
    },
    "serving_size": "60g"
  }
}
```

**Health Connect Extended Write:**

```kotlin
suspend fun insertNutritionRecordWithMacros(
    calories: Int,
    protein: Int,
    carbs: Int,
    fat: Int,
    description: String,
    timestamp: Instant
): String {
    val record = NutritionRecord(
        energy = Energy.kilocalories(calories.toDouble()),
        protein = Mass.grams(protein.toDouble()),           // NEW
        totalCarbohydrate = Mass.grams(carbs.toDouble()),   // NEW
        totalFat = Mass.grams(fat.toDouble()),              // NEW
        name = description,
        startTime = timestamp,
        endTime = timestamp,
        startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
        endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp)
    )
    
    val response = healthConnectClient.insertRecords(listOf(record))
    return response.recordIdsList.first()
}
```

### Workflows and Sequencing

**Story 7.1: Macros Tracking Flow**

```
User captures photo
    ↓
AnalyseMealWorker (updated)
    ├─→ Base64 encode photo
    ├─→ Send to Azure OpenAI with updated prompt (macros request)
    ├─→ Parse response JSON: {calories, protein, carbs, fat, description}
    ├─→ Validate macros ranges (protein 0-500g, carbs 0-1000g, fat 0-500g)
    ├─→ Save to Health Connect with macros fields
    └─→ Delete photo
         ↓
Health Connect NutritionRecord saved with macros
    ↓
MealListScreen queries records
    ├─→ Display: "650 cal | P: 45g C: 60g F: 20g"
    └─→ Tap entry → MealDetailScreen
         ↓
MealDetailScreen shows editable macros fields
    ├─→ User edits macros
    ├─→ Save → Delete old record + Insert new with updated macros
    └─→ Return to list
```

**Story 7.2: Barcode Scanning Flow**

```
User taps "Scan Barcode" button
    ↓
BarcodeScannerScreen launches
    ├─→ ML Kit CameraX preview
    ├─→ Barcode detection callback
    └─→ Barcode detected
         ↓
BarcodeScannerViewModel.onBarcodeDetected(barcode)
    ├─→ Query OpenFoodFacts API
    ├─→ Parse response
    └─→ Calculate nutrition per serving size
         ↓
         ├─→ If product found:
         │    ├─→ Pre-fill NutritionForm with calories, protein, carbs, fat
         │    ├─→ Show serving size adjuster
         │    └─→ User confirms → Save to Health Connect
         │
         └─→ If product not found:
              └─→ Show "Product not found - use camera instead" → Navigate to photo capture
```

**Story 7.3: Offline Queue Flow**

```
User captures photo while offline
    ↓
NetworkMonitor.isConnected == false
    ↓
OfflineQueueManager.enqueue(photoUri, timestamp)
    ├─→ Encrypt photo with EncryptedFile
    ├─→ Save to app files directory (persistent storage)
    ├─→ Add to queue metadata (SharedPreferences)
    └─→ Show notification: "Offline - meal queued for analysis"
         ↓
MealListScreen displays queued items
    ├─→ Read queued photo files from OfflineQueueManager
    ├─→ Combine with Health Connect records in single list (chronological order)
    ├─→ Show queued items with visual indicator:
    │    ├─→ Gray/muted appearance (different from normal meal cards)
    │    ├─→ Icon: Cloud with slash or sync pending icon
    │    ├─→ Text: "Meal captured at {timestamp} - Waiting for connection"
    │    ├─→ No calories/macros shown (data not yet analysed)
    │    ├─→ Retry button: Tappable "Retry" button to manually trigger analysis
    │    │    ├─→ Only enabled when network available (NetworkMonitor.isConnected)
    │    │    ├─→ On tap: Manually enqueue ProcessQueueWorker for this specific photo
    │    │    ├─→ Show loading state while processing
    │    │    └─→ Fallback if auto-processing doesn't trigger
    │    └─→ Tappable card to view photo preview (optional)
    └─→ Position queued items at correct chronological position (by capture timestamp)
         ↓
NetworkMonitor detects connectivity restored
    ↓
ProcessQueueWorker triggered (WorkManager with CONNECTED constraint)
    ├─→ Load queue from OfflineQueueManager
    ├─→ For each queued photo (FIFO order):
    │    ├─→ Decrypt photo
    │    ├─→ Analyse with AnalyseMealWorker logic
    │    ├─→ Save to Health Connect
    │    ├─→ Delete encrypted photo
    │    ├─→ Update notification: "Processing queued meal 2/5..."
    │    └─→ MealListScreen auto-refreshes: queued item replaced by analysed NutritionRecord
    └─→ Queue empty → Dismiss notification
         ↓
All queued meals processed and saved
    ↓
MealListScreen shows all items as normal Health Connect records
```

**IMPORTANT - Meal List Implementation Note:**

The meal list currently displays only Health Connect `NutritionRecord` items. For Story 7.3, the list must be modified to show BOTH:
1. **Health Connect records** (analysed meals with calories/macros)
2. **Queued photos** (encrypted files awaiting analysis)

This requires:
- Create a sealed class `MealListItem` with two variants: `MealListItem.Analysed(nutritionRecord)` and `MealListItem.Queued(queuedPhoto)`
- Update `MealListViewModel` to query both sources and merge into single chronological list (sorted by timestamp)
- Update UI composables to render different card styles:
  - **Analysed meals:** Normal card with calories, macros, description
  - **Queued items:** Muted/gray card with "Waiting for connection" message, sync icon, timestamp only, **manual "Retry" button**
- **Retry button logic:**
  - Button enabled only when `NetworkMonitor.isConnected == true`
  - On tap: Trigger `ProcessQueueWorker` for specific queued photo (not full queue)
  - Show loading state on card while processing
  - Provides manual fallback if automatic processing fails to trigger
- No local database needed - just read encrypted files directly from `OfflineQueueManager.getQueuedPhotos()`
- When queue processes, queued items disappear and analysed records appear in same position

## Non-Functional Requirements

### Performance

**Macros Tracking:**
- Azure OpenAI response parsing (macros extraction): < 50ms
- Health Connect write with macros fields: < 200ms (same as calories-only)
- UI update with macros display: < 100ms (no lag in list view scrolling)

**Barcode Scanning:**
- Barcode detection latency: < 500ms from camera frame to decoded barcode
- OpenFoodFacts API query: < 2 seconds typical (cached results preferred)
- Form pre-fill: < 100ms after API response received

**Offline Queue:**
- Photo encryption during enqueue: < 500ms (should not block UI)
- Queue status check: < 50ms (synchronous check for display)
- Photo decryption during dequeue: < 300ms
- Queue processing rate: 1 photo per 15 seconds average (same as online analysis)

### Security

**Macros Tracking:**
- No new security concerns (reuses existing Azure OpenAI API security)
- Macros data in Health Connect encrypted at rest by Android system

**Barcode Scanning:**
- OpenFoodFacts API is public (no authentication needed)
- No PII transmitted (barcode numbers only)
- HTTPS-only communication with OpenFoodFacts

**Offline Queue:**
- **CRITICAL:** Photos encrypted using EncryptedFile with AES256_GCM
- Encryption key managed by Android Keystore (hardware-backed)
- Queue metadata stored in EncryptedSharedPreferences
- Photos deleted immediately after successful processing
- 7-day retention limit enforced (auto-delete stale queued photos)

### Reliability/Availability

**Macros Tracking:**
- Graceful degradation: If macros parsing fails, fall back to calories-only with user notification
- Validation: Reject invalid macros ranges, prompt user to retry or edit manually
- Backward compatibility: App handles NutritionRecords without macros (from pre-Epic-7 data)

**Barcode Scanning:**
- Fallback: If OpenFoodFacts API unavailable, direct user to photo capture mode
- Offline resilience: Cache frequently scanned barcodes locally (optional enhancement)
- Error messaging: Clear guidance when barcode not found ("Product not in database - use camera instead")

**Offline Queue:**
- Queue persistence: Survives app restart, device reboot, low memory kills
- FIFO order guarantee: Photos processed in capture order
- Retry logic: Failed queue processing triggers exponential backoff (reuse WorkManager retry from Epic 4)
- Storage limits: If queue exceeds 50 photos, warn user and prevent new captures until queue drains
- Notification persistence: Queue status notification remains until queue empty or user dismisses

### Observability

**Macros Tracking:**
- Log macros parsing failures with error details (invalid JSON, missing fields)
- Timber.i() successful macros extraction: "Macros parsed: P:45g C:60g F:20g"
- Track macros validation failures (out of range values)

**Barcode Scanning:**
- Log barcode detection events: "Barcode scanned: {barcode}"
- Log OpenFoodFacts API failures with status codes
- Track barcode lookup hit/miss rates (for future caching optimization)

**Offline Queue:**
- Log queue operations: enqueue, dequeue, process, delete
- Track queue depth over time (metrics for understanding offline usage patterns)
- Monitor encryption/decryption failures
- Alert on stale photos approaching 7-day limit

## Dependencies and Integrations

**New Dependencies:**

```kotlin
// build.gradle.kts additions for Epic 7

// ML Kit Barcode Scanning (Story 7.2)
implementation("com.google.mlkit:barcode-scanning:17.3.0")

// CameraX for barcode scanner (if not already added)
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")

// EncryptedFile for offline queue security (Story 7.3)
implementation("androidx.security:security-crypto:1.1.0-alpha06") // Already in project for API key

// Optional: Image compression library (if queue storage becomes issue)
// implementation("com.github.bumptech.glide:glide:4.16.0")
```

**External API Integrations:**

| API | Purpose | Authentication | Rate Limits | Fallback |
|-----|---------|---------------|-------------|----------|
| **Azure OpenAI (Extended)** | Macros analysis | API key (existing) | Standard Azure limits | Fail to calories-only + user edit |
| **OpenFoodFacts API** | Barcode nutrition lookup | None (public API) | No hard limit (respectful usage) | Direct to photo capture |

**CRITICAL - Developer Documentation Requirements:**

⚠️ **Story 7.2 (Barcode Scanning) - MANDATORY READING BEFORE IMPLEMENTATION:**
- **ML Kit Barcode Scanning Documentation:** Developers MUST read the complete official ML Kit documentation at https://developers.google.com/ml-kit/vision/barcode-scanning before writing any code
  - Read: Overview, Android setup, scanning barcodes guide, format support
  - Understand: CameraX integration, barcode formats (EAN-13, UPC-A, etc.), error handling patterns
  - Follow links to related pages as needed for complete context
- **OpenFoodFacts API Documentation:** Developers MUST read the complete API documentation at https://openfoodfacts.github.io/openfoodfacts-server/api/ before implementation
  - Read: Product lookup endpoints, response schemas, field definitions, error codes
  - Understand: Data quality variations, optional fields, serving size handling
  - Review examples: https://world.openfoodfacts.org/api/v2/product/737628064502
  - Note: Follow any cross-referenced documentation links to understand data types and edge cases fully

**Version Constraints:**
- ML Kit Barcode Scanning: 17.3.0+ (latest stable)
- CameraX: 1.3.4+ (matches existing project version)
- Health Connect SDK: 1.1.0 (existing, supports macros fields)

**Integration Points:**

1. **Azure OpenAI API (Modified):**
   - Change: Update `instructions` field to request macros
   - Change: Parse 5 fields from `output_text` instead of 2
   - Risk: Longer response times with more data requested
   - Mitigation: Monitor API latency, consider timeout adjustments

2. **Health Connect (Extended):**
   - New fields used: `protein`, `totalCarbohydrate`, `totalFat` (all Mass type)
   - Backward compatibility: Query existing records, display 0g macros if not present
   - Risk: Health Connect schema changes in future Android versions
   - Mitigation: Test on multiple Android API levels (28-35)

3. **OpenFoodFacts API:**
   - Endpoint: `https://world.openfoodfacts.org/api/v2/product/{barcode}`
   - Response format: JSON with nested `product.nutriments` object
   - Risk: API downtime, rate limiting, data quality issues
   - Mitigation: Cache results, graceful fallback to photo capture, validate nutrition ranges

4. **ML Kit Barcode Scanning:**
   - Formats supported: All standard barcodes (EAN-13, UPC-A, Code-128, QR, etc.)
   - Performance: On-device processing (no network latency)
   - Risk: Poor lighting conditions affect detection accuracy
   - Mitigation: Provide flashlight toggle, clear instructions for positioning

## Acceptance Criteria (Authoritative)

**Story 7.1: Macros Tracking**

1. **AC-1:** Azure OpenAI prompt requests `{calories, protein, carbs, fat, description}` in JSON format
2. **AC-2:** Macros measured in grams (protein, carbs, fat all integer values)
3. **AC-3:** Macros saved to Health Connect NutritionRecord using `protein`, `totalCarbohydrate`, `totalFat` fields with Mass.grams()
4. **AC-4:** Meal list view displays macros below calories: "650 cal | P: 45g C: 60g F: 20g"
5. **AC-5:** Edit screen shows editable numeric fields for protein, carbs, fat
6. **AC-6:** Energy Balance dashboard aggregates daily macro totals (sum of all meals for current day)
7. **AC-7:** Macros validation: protein 0-500g, carbs 0-1000g, fat 0-500g
8. **AC-8:** Backward compatibility: App handles existing records without macros (displays "P: 0g C: 0g F: 0g")

**Story 7.2: Barcode Scanning**

1. **AC-1:** "Scan Barcode" button accessible from meal entry flow
2. **AC-2:** Camera opens in barcode scan mode with ML Kit detection
3. **AC-3:** Barcode detected and decoded automatically (no manual entry)
4. **AC-4:** App queries OpenFoodFacts API with barcode and displays loading indicator
5. **AC-5:** If product found, nutrition data pre-fills form: calories, protein, carbs, fat
6. **AC-6:** Serving size adjuster allows scaling nutrition values (e.g., 1 bar = 60g, user ate 2 bars = 120g)
7. **AC-7:** User confirms and data saved to Health Connect as standard NutritionRecord
8. **AC-8:** If product not found, show message "Product not found - use camera instead" with button to switch to photo capture
9. **AC-9:** Barcode scan works in normal lighting (provide flashlight toggle for low light)

**Story 7.3: Offline Photo Queuing**

1. **AC-1:** NetworkMonitor detects offline state before API call
2. **AC-2:** Photo encrypted and saved to app files directory (not cache)
3. **AC-3:** Notification shows: "Offline - meal queued for analysis"
4. **AC-4:** Queue metadata persists across app restarts
5. **AC-5:** Queued items visible in MealListScreen with visual indicator (muted card, sync icon, "Waiting for connection" text)
6. **AC-6:** Queued items positioned chronologically by capture timestamp (mixed with Health Connect records)
7. **AC-7:** Queued items show capture timestamp but no calories/macros (data not yet available)
8. **AC-8:** Manual "Retry" button on queued items (enabled only when network available)
9. **AC-9:** Retry button triggers analysis of specific queued photo and shows loading state
10. **AC-10:** When network restored, ProcessQueueWorker triggered automatically
11. **AC-11:** Photos processed in FIFO order (oldest first)
12. **AC-12:** Notification updates during processing: "Processing queued meal 2/5..."
13. **AC-13:** MealListScreen auto-refreshes: queued items replaced by analysed records after processing
14. **AC-14:** Encrypted photos deleted after successful Health Connect save
15. **AC-15:** Queued photos auto-deleted after 7 days with warning notification
16. **AC-16:** Queue limit: Maximum 50 photos, prevent new captures if exceeded

## Traceability Mapping

| AC | Spec Section | Component | Test Strategy |
|----|-------------|-----------|---------------|
| **7.1-AC-1** | APIs and Interfaces → Azure OpenAI Extended Request | MacrosExtractor | Unit test: Verify prompt format includes macros request |
| **7.1-AC-2** | Data Models → NutritionData (extended) | Domain model validation | Unit test: Validate macros are integers in grams |
| **7.1-AC-3** | APIs and Interfaces → Health Connect Extended Write | HealthConnectRepository | Integration test: Insert record, query back, verify macros fields |
| **7.1-AC-4** | Workflows → Macros Tracking Flow | MealListScreen | UI test: Verify macros display format |
| **7.1-AC-5** | Workflows → Macros Tracking Flow | MealDetailScreen | UI test: Verify editable macros fields |
| **7.1-AC-6** | Services and Modules → Extended dashboard logic | EnergyBalanceDashboardViewModel | Unit test: Aggregate macros for current day |
| **7.1-AC-7** | Data Models → NutritionData validation | Domain model init block | Unit test: Test boundary values (0g, 500g, 1001g) |
| **7.1-AC-8** | NFR Reliability → Backward compatibility | MealRepository | Integration test: Query old records, verify 0g default |
| **7.2-AC-1** | Services and Modules → BarcodeScannerViewModel | Navigation logic | Manual test: Button appears in meal entry flow |
| **7.2-AC-2** | Workflows → Barcode Scanning Flow | BarcodeScannerScreen | Integration test: Camera preview launches |
| **7.2-AC-3** | Dependencies → ML Kit Barcode Scanning | ML Kit SDK | Integration test: Scan test barcode, verify detection |
| **7.2-AC-4** | APIs and Interfaces → OpenFoodFacts API | BarcodeRepository | Unit test (mocked): API call with barcode string |
| **7.2-AC-5** | Workflows → Barcode Scanning Flow | BarcodeScannerViewModel | Unit test: Parse OpenFoodFacts response, verify pre-fill |
| **7.2-AC-6** | Workflows → Barcode Scanning Flow | Serving size logic | Unit test: Scale nutrition by serving size multiplier |
| **7.2-AC-7** | Workflows → Barcode Scanning Flow | Save to Health Connect | Integration test: Barcode → API → HC save |
| **7.2-AC-8** | NFR Reliability → Fallback logic | BarcodeScannerViewModel | Unit test: Handle 404 response, verify navigation to photo |
| **7.2-AC-9** | Detailed Design → Camera controls | BarcodeScannerScreen | Manual test: Flashlight toggle works |
| **7.3-AC-1** | Workflows → Offline Queue Flow | NetworkMonitor | Unit test: Offline detection before enqueue |
| **7.3-AC-2** | Data Models → QueuedPhoto | OfflineQueueManager | Unit test: Encrypt photo, verify file created |
| **7.3-AC-3** | Workflows → Offline Queue Flow | Notification builder | Manual test: Notification displays correct text |
| **7.3-AC-4** | NFR Reliability → Queue persistence | OfflineQueueManager | Integration test: Enqueue, restart app, verify queue intact |
| **7.3-AC-5** | Workflows → Offline Queue Flow | MealListScreen + MealListViewModel | UI test: Verify queued item appears with muted styling |
| **7.3-AC-6** | Workflows → Offline Queue Flow | MealListViewModel sorting | Unit test: Verify chronological ordering (queued + HC records) |
| **7.3-AC-7** | Workflows → Offline Queue Flow | MealListScreen composable | UI test: Verify queued item shows timestamp, no calories |
| **7.3-AC-8** | Workflows → Offline Queue Flow | MealListScreen retry button | UI test: Button enabled when network available, disabled offline |
| **7.3-AC-9** | Workflows → Offline Queue Flow | MealListViewModel retry action | Integration test: Tap retry, verify worker triggered, loading state shown |
| **7.3-AC-10** | Workflows → Offline Queue Flow | ProcessQueueWorker | Integration test: Mock network restore, verify worker triggered |
| **7.3-AC-11** | Data Models → QueuedPhoto timestamp | OfflineQueueManager.dequeue() | Unit test: Verify FIFO order (earliest timestamp first) |
| **7.3-AC-12** | Workflows → Offline Queue Flow | Notification updates | Manual test: Process queue, verify notification text updates |
| **7.3-AC-13** | Workflows → Offline Queue Flow | MealListScreen state | UI test: Verify queued item replaced by analysed record |
| **7.3-AC-14** | NFR Security → Photo deletion | ProcessQueueWorker | Unit test: Verify encrypted file deleted after HC save |
| **7.3-AC-15** | NFR Reliability → 7-day retention | PhotoCleanupWorker (reuse) | Unit test: Mock 8-day-old photo, verify auto-delete |
| **7.3-AC-16** | NFR Reliability → Queue limits | OfflineQueueManager | Unit test: Enqueue 50 photos, verify 51st blocked |

## Risks, Assumptions, Open Questions

**Risks:**

1. **RISK:** Azure OpenAI macros accuracy may be lower than calories-only (more complex task)
   - **Severity:** Medium
   - **Mitigation:** Allow user to edit macros easily, validate ranges, monitor accuracy in real usage

2. **RISK:** OpenFoodFacts API data quality varies by region/product
   - **Severity:** Medium
   - **Mitigation:** Fallback to photo capture, validate nutrition ranges, display data source to user

3. **RISK:** Offline queue storage could consume significant disk space (50 photos × 500KB = 25MB)
   - **Severity:** Low
   - **Mitigation:** 50-photo queue limit, 7-day auto-delete, compression before encryption

4. **RISK:** EncryptedFile performance may degrade with large queue sizes
   - **Severity:** Low
   - **Mitigation:** Benchmark encryption/decryption times, optimize if needed

**Assumptions:**

1. **ASSUMPTION:** Users primarily capture meals with network connectivity (offline queue is edge case, not primary flow)
2. **ASSUMPTION:** Health Connect macros fields (protein, totalCarbohydrate, totalFat) are stable across Android versions
3. **ASSUMPTION:** OpenFoodFacts API remains free and publicly accessible (no authentication required)
4. **ASSUMPTION:** ML Kit barcode detection works reliably on packaged food barcodes in typical lighting

**Open Questions:**

1. **QUESTION:** Should macros display show % of daily values (e.g., "P: 45g (90% DV)")? → Decision: Defer to future, complexity not worth V2.0
2. **QUESTION:** Should barcode scanner support QR codes or only traditional barcodes? → Decision: Support all formats (ML Kit handles automatically)
3. **QUESTION:** Should offline queue show preview of queued photos for user verification? → Decision: Not needed for V2.0, adds UI complexity
4. **QUESTION:** Should we implement local barcode cache to reduce OpenFoodFacts API calls? → Decision: Defer to future based on real usage patterns

## Test Strategy Summary

**Unit Tests (80%+ coverage target):**
- MacrosExtractor: Parse Azure OpenAI response with macros fields
- NutritionData validation: Boundary testing for macros ranges
- BarcodeRepository: Mock OpenFoodFacts API responses
- OfflineQueueManager: Encrypt, decrypt, FIFO ordering, persistence
- BarcodeScannerViewModel: State management, serving size calculations
- ProcessQueueWorker: Queue processing logic, error handling

**Integration Tests:**
- Health Connect macros CRUD: Insert, query, update macros fields
- Barcode end-to-end: Scan → API → HC save
- Offline queue persistence: Enqueue → restart app → dequeue
- Network state transitions: Online → offline → enqueue → online → process

**UI Tests (Compose):**
- MealListScreen: Verify macros display format
- MealDetailScreen: Verify macros edit fields
- BarcodeScannerScreen: Camera preview launches
- EnergyBalanceDashboard: Macro totals aggregate correctly

**Manual Testing Scenarios:**

1. **Macros Accuracy:**
   - Capture 10 varied meals (high protein, high carb, high fat, balanced)
   - Compare AI macros estimates with nutrition database lookups
   - Edit macros in UI, verify save to Health Connect
   - Cross-verify in Google Fit app

2. **Barcode Scanning:**
   - Scan 10 common packaged foods (protein bars, yogurt, cereal)
   - Verify nutrition data matches package labels
   - Test serving size adjuster (2x, 0.5x servings)
   - Test "product not found" flow with obscure barcode

3. **Offline Queue:**
   - Enable airplane mode
   - Capture 5 meals while offline
   - Verify queue notification appears
   - Verify queued items appear in meal list with "Retry" button (disabled while offline)
   - Disable airplane mode
   - Verify "Retry" button becomes enabled
   - Tap "Retry" on one queued item manually
   - Verify loading state appears and item processes
   - Verify automatic processing begins for remaining items
   - Confirm all 5 meals appear in Health Connect
   - Test queue limit: Capture 51 photos offline, verify blocking

**Performance Testing:**
- Benchmark macros parsing latency (should be < 50ms overhead vs calories-only)
- Measure barcode detection time across 20 scans (target < 500ms average)
- Profile offline queue encryption time (target < 500ms per photo)
- Stress test: Queue 50 photos, measure total processing time

**Regression Testing:**
- Verify Epic 2-6 functionality unchanged (calories-only capture still works)
- Verify existing NutritionRecords without macros display correctly
- Verify Energy Balance dashboard works with and without macros data
