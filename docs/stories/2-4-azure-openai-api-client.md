# Story 2.4: Azure OpenAI API Client

Status: ready-for-dev

## Story

As a developer,
I want to send food photos to Azure OpenAI and receive structured calorie estimates,
So that users get automatic nutrition data without manual entry.

## Acceptance Criteria

1. **Given** a food photo exists in temporary storage  
   **When** the API client sends the photo for analysis  
   **Then** the photo is base64-encoded and included in the request payload

2. **And** the request uses Azure OpenAI Responses API endpoint format: `https://{resource}.openai.azure.com/openai/v1/responses`

3. **And** authentication uses `api-key` header (not Bearer token)

4. **And** the request uses the `model` field to specify the deployment name (e.g., "gpt-4.1")

5. **And** the request uses `instructions` field for system-level guidance: "You are a nutrition analysis assistant. Analyze the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food)."

6. **And** the request uses `input` array with multimodal content containing text prompt and base64 image

7. **And** the input includes user message with text: "Analyze this meal and estimate total calories." and image type "input_image" with base64 data URL

8. **And** the response contains `output_text` field with JSON: `{calories: number, description: string}`

9. **And** validation ensures calories > 0 and < 5000

10. **And** the API call completes in under 10 seconds for typical network conditions

11. **And** API key and endpoint are retrieved from secure storage (Android Keystore)

## Tasks / Subtasks

- [ ] **Task 1: Create Azure OpenAI API Data Transfer Objects** (AC: #1-8)
  - [ ] Create `AzureResponseRequest` data class in `data/remote/dto/`
    - [ ] Fields: `model: String`, `instructions: String`, `input: List<InputItem>`
  - [ ] Create `InputItem` data class
    - [ ] Fields: `role: String` (e.g., "user"), `content: List<ContentItem>`
  - [ ] Create `ContentItem` sealed class for multimodal content
    - [ ] `TextContent` with `type: "input_text"`, `text: String`
    - [ ] `ImageContent` with `type: "input_image"`, `image_url: String` (base64 data URL)
  - [ ] Create `AzureResponseResponse` data class
    - [ ] Fields: `id, created_at, model, object, status, output_text, output, usage`
  - [ ] Create `ApiNutritionResponse` for parsed output_text JSON
    - [ ] Fields: `calories: Int`, `description: String`
  - [ ] Write unit tests for DTO serialization/deserialization with Gson
  - [ ] Document request/response format in KDocs with examples

- [ ] **Task 2: Implement Retrofit API Interface** (AC: #2, #3, #4)
  - [ ] Create `AzureOpenAiApi` interface in `data/remote/api/`
  - [ ] Define `@POST("openai/v1/responses")` endpoint
  - [ ] Method: `suspend fun analyzeNutrition(@Body request: AzureResponseRequest): AzureResponseResponse`
  - [ ] Create `AuthInterceptor` in `data/remote/interceptor/`
    - [ ] Inject SecurePreferences via Hilt
    - [ ] Add `api-key` header (NOT `Authorization: Bearer`)
    - [ ] Add `Content-Type: application/json` header
    - [ ] Throw `IllegalStateException` if API key not configured
  - [ ] Write unit tests for AuthInterceptor with mocked SecurePreferences
  - [ ] Document authentication pattern in KDocs

- [ ] **Task 3: Configure Retrofit and OkHttp in NetworkModule** (AC: #2, #10, #11)
  - [ ] Update `NetworkModule.kt` in `di/` package
  - [ ] Provide `OkHttpClient` with:
    - [ ] `AuthInterceptor` for API key injection
    - [ ] `HttpLoggingInterceptor` (BODY level in debug, NONE in release)
    - [ ] `connectTimeout(15, TimeUnit.SECONDS)`
    - [ ] `readTimeout(30, TimeUnit.SECONDS)`
    - [ ] `writeTimeout(30, TimeUnit.SECONDS)`
  - [ ] Provide `Retrofit` with:
    - [ ] Base URL from SecurePreferences (fallback to default if not configured)
    - [ ] `GsonConverterFactory` for JSON serialization
    - [ ] Configured OkHttpClient
  - [ ] Provide `AzureOpenAiApi` using Retrofit.create()
  - [ ] Write integration test for NetworkModule DI configuration
  - [ ] Document timeout rationale in comments (AI analysis can take 10-30s)

- [ ] **Task 4: Implement NutritionAnalysisRepository** (AC: #1, #5-10)
  - [ ] Create `NutritionAnalysisRepository` interface in `domain/repository/`
    - [ ] Method: `suspend fun analyzePhoto(photoUri: Uri): Result<NutritionData>`
  - [ ] Create `NutritionAnalysisRepositoryImpl` in `data/repository/`
    - [ ] Inject `AzureOpenAiApi` and `ImageUtils` via Hilt
    - [ ] Implement `analyzePhoto()`:
      - [ ] Load image from URI using `ImageUtils.encodeImageToBase64()`
      - [ ] Build `AzureResponseRequest` with model "gpt-4.1" (or from config)
      - [ ] Set `instructions` with system guidance
      - [ ] Create `InputItem` with text prompt and base64 image
      - [ ] Call `AzureOpenAiApi.analyzeNutrition(request)`
      - [ ] Parse `response.output_text` as JSON to `ApiNutritionResponse`
      - [ ] Validate calories in 1..5000 range
      - [ ] Map to `NutritionData` domain model
      - [ ] Return `Result.success(NutritionData)` on success
    - [ ] Error handling:
      - [ ] Catch `IOException` → `Result.error(e, "Network error - will retry")`
      - [ ] Catch `JsonSyntaxException` → `Result.error(e, "Invalid response format")`
      - [ ] Catch `Exception` → `Result.error(e, e.message ?: "Unknown error")`
    - [ ] Log all errors with Timber.e() including full context
  - [ ] Write comprehensive unit tests with mocked API
  - [ ] Document retry strategy in KDocs (repository returns error, worker handles retry)

- [ ] **Task 5: Create ImageUtils for Base64 Encoding** (AC: #1)
  - [ ] Create `ImageUtils` object in `util/` package
  - [ ] Implement `encodeImageToBase64(uri: Uri): String` method
    - [ ] Load bitmap from URI using ContentResolver
    - [ ] Apply 2MP max resolution if needed (reuse PhotoManager logic)
    - [ ] Compress to JPEG 80% quality in memory
    - [ ] Encode to Base64 string
    - [ ] Return data URL format: `data:image/jpeg;base64,{encoded}`
    - [ ] Recycle bitmap to prevent memory leaks
    - [ ] Close streams properly in finally block
  - [ ] Implement `estimateBase64Size(uri: Uri): Long` for monitoring
  - [ ] Write unit tests with sample image files
  - [ ] Document memory management in KDocs (bitmap recycling critical)

- [ ] **Task 6: Create NutritionData Domain Model** (AC: #8, #9)
  - [ ] Create `NutritionData` data class in `domain/model/`
    - [ ] Fields: `calories: Int`, `description: String`
    - [ ] Add validation in `init` block:
      - [ ] Require calories in 1..5000 range
      - [ ] Require description.isNotBlank()
      - [ ] Require description.length <= 200
    - [ ] Throw `IllegalArgumentException` with clear messages for validation failures
  - [ ] Write unit tests for validation logic (valid data, boundary cases, invalid data)
  - [ ] Document validation rationale in KDocs

- [ ] **Task 7: Integration Testing with Mock API** (AC: All)
  - [ ] Create `AzureOpenAiApiIntegrationTest` in `androidTest/`
  - [ ] Use WireMock or MockWebServer to simulate API responses
  - [ ] Test success scenario:
    - [ ] Mock 200 response with valid `output_text` JSON
    - [ ] Verify request format (headers, body structure)
    - [ ] Verify parsing to NutritionData
  - [ ] Test error scenarios:
    - [ ] Network timeout (simulate slow response)
    - [ ] 401 Unauthorized (invalid API key)
    - [ ] 429 Rate Limit (too many requests)
    - [ ] 500 Server Error (retryable)
    - [ ] Invalid JSON in output_text (parse error)
    - [ ] Calories out of range (validation error)
  - [ ] Test authentication interceptor adds api-key header
  - [ ] Test base64 encoding of real image files
  - [ ] Document test patterns in test class KDocs

- [ ] **Task 8: Playwright Documentation Research (Non-Interactive)** (Epic 2 Guidance)
  - [ ] Add comprehensive Dev Notes section: "Documentation Research Strategy"
  - [ ] Include Playwright MCP recommendation with starting URLs:
    - [ ] Azure OpenAI Responses API: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference
    - [ ] Multimodal capabilities: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/gpt-with-vision
    - [ ] API authentication: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference#authentication
  - [ ] List focus areas:
    - [ ] Responses API request structure (model, instructions, input array)
    - [ ] Multimodal input formatting (text + image base64)
    - [ ] Authentication header format (api-key vs Bearer token)
    - [ ] Error response formats and status codes
    - [ ] Rate limiting and throttling patterns
  - [ ] Document benefits of interactive research for complex SDK integration
  - [ ] Mark as NON-INTERACTIVE task (no Playwright execution during story creation)

- [ ] **Task 9: Documentation and Completion** (AC: All)
  - [ ] Update Dev Notes with API integration architecture
  - [ ] Document request/response flow with examples
  - [ ] Add references to Azure OpenAI documentation
  - [ ] Update README if API configuration instructions needed
  - [ ] Update Dev Agent Record with completion notes and file list
  - [ ] Add Change Log entry summarizing API client implementation
  - [ ] Run all tests: `./gradlew test connectedAndroidTest`
  - [ ] Verify build successful: `./gradlew assembleDebug`

## Definition of Done

This story is considered COMPLETE only when ALL of the following are satisfied:

### Implementation & Quality
- [ ] All acceptance criteria are met with verified evidence (file:line references)
- [ ] All tasks and subtasks are completed and checked off
- [ ] Code follows project architecture patterns and conventions (MVVM, Hilt DI, Repository pattern)
- [ ] All new/modified code has appropriate error handling
- [ ] Code is reviewed (senior developer or AI code review workflow)

### Testing Requirements
- [ ] **Unit tests written** for DTOs, repository, ImageUtils, NutritionData validation
- [ ] **All unit tests passing** (`./gradlew test` executes successfully with zero failures)
- [ ] **Integration tests written** for API client with mocked responses (success/error scenarios)
- [ ] **All integration tests passing** (`./gradlew connectedAndroidTest` succeeds)
- [ ] No test coverage regressions (existing tests still pass)

### Documentation
- [ ] Inline code documentation (KDocs) added for API interfaces, repository, DTOs, utilities
- [ ] README updated with API configuration instructions (API key setup)
- [ ] Dev Notes section includes API integration architecture and references

### Story File Completeness
- [ ] Dev Agent Record updated with completion notes and file list
- [ ] Change Log entry added summarizing API client implementation
- [ ] Story status updated to "review" (pending approval) or "done" (if approved)

**Testing Standards Summary:**
- **Unit Tests Required:** All business logic (repository, validation, utilities)
- **Integration Tests Required:** API client with mocked HTTP responses
- **Test Naming Convention:** `methodName_whenCondition_thenExpectedResult`
- **Assertion Library:** Truth library for readable assertions (`assertThat(x).isEqualTo(y)`)
- **Mocking:** Mockito/Mockito-Kotlin for dependency mocking

## User Demo

**Purpose**: Validate API integration for stakeholders (future story 2-5 will demonstrate full end-to-end flow).

### Prerequisites
- Android device or emulator running Android 9+
- Azure OpenAI API key configured in SecurePreferences
- Azure OpenAI resource endpoint configured
- Sample food photos in device storage (or use test images)
- Internet connectivity

### Demo Steps

**Demo 1: API Client Integration Test**
1. Run integration tests: `./gradlew connectedAndroidTest`
2. **Expected**: All API client tests pass (success and error scenarios)
3. Review test output showing request/response format
4. Verify authentication header added correctly

**Demo 2: Manual API Call (Developer Tool)**
1. Use debug mode or test harness to trigger API call with sample food image
2. **Expected**: Request sent with base64-encoded image
3. **Expected**: Response received with calorie estimate and description
4. Review Logcat output showing:
   - Request body structure (model, instructions, input array)
   - Response output_text with JSON: `{calories: X, description: "..."}`
   - Parsed NutritionData domain model
   - Total API call duration (< 10 seconds typical)

### Expected Behavior
- API requests formatted correctly with multimodal input (text + image)
- Authentication header `api-key` added automatically
- API responses parsed successfully to NutritionData
- Errors handled gracefully with appropriate Result.error() returns
- Network errors logged with full context for debugging
- API call completes within timeout limits (30 seconds max)

### Validation Checklist
- [ ] API request structure matches Responses API specification
- [ ] Authentication header added correctly (api-key, not Bearer)
- [ ] Base64 image encoding works without memory leaks
- [ ] Response parsing extracts calories and description correctly
- [ ] Calorie validation enforces 1..5000 range
- [ ] Network errors return Result.error for retry handling
- [ ] Parse errors logged and return Result.failure (non-retryable)
- [ ] Integration tests pass for all scenarios

## Dev Notes

### Relevant Architecture Patterns and Constraints

**Azure OpenAI Responses API Integration:**

**API Endpoint Structure:**
- Base URL: `https://{your-resource-name}.openai.azure.com/`
- Full endpoint: `https://{your-resource-name}.openai.azure.com/openai/v1/responses`
- Authentication: `api-key` header (NOT `Authorization: Bearer {token}`)
- API Version: v1 (Responses API - modern stateful API replacing chat completions)

**Request Structure (Multimodal Vision):**
```json
{
  "model": "gpt-4.1",
  "instructions": "You are a nutrition analysis assistant. Analyze the food image and return ONLY a JSON object with two fields: calories (number) and description (string describing the food).",
  "input": [
    {
      "role": "user",
      "content": [
        {
          "type": "input_text",
          "text": "Analyze this meal and estimate total calories."
        },
        {
          "type": "input_image",
          "image_url": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
        }
      ]
    }
  ]
}
```

**Response Structure:**
```json
{
  "id": "resp_abc123",
  "created_at": 1741408624.0,
  "model": "gpt-4o-2024-08-06",
  "object": "response",
  "status": "completed",
  "output_text": "{\"calories\": 650, \"description\": \"Grilled chicken with rice\"}",
  "usage": {
    "input_tokens": 1245,
    "output_tokens": 25,
    "total_tokens": 1270
  }
}
```

**Key Differences from Chat Completions API:**
- Uses `instructions` field for system-level guidance (not `system` message)
- Uses `input` array instead of `messages` array
- Content types are `input_text` and `input_image` (not `text` and `image_url` directly)
- Response has `output_text` field (direct text output, easier parsing)
- Authentication uses `api-key` header (simpler than Bearer token)

**Repository Pattern for API Integration:**
- Repository coordinates API call, error handling, and domain model mapping
- Returns `Result<NutritionData>` for type-safe error propagation
- Throws no exceptions - all errors caught and wrapped in Result.error()
- Logging via Timber for debugging (includes full exception context)
- Worker (Story 2-5) consumes repository and handles retry logic

**Base64 Image Encoding:**
- Load bitmap from PhotoManager cache directory
- Ensure 2MP max resolution (already applied by PhotoManager in Story 2-3)
- Compress to JPEG 80% quality in-memory
- Encode to Base64 string
- Prepend data URL prefix: `data:image/jpeg;base64,`
- **Memory Management Critical**: Recycle bitmap immediately after encoding
- Typical base64 size: ~500KB-700KB for 2MP JPEG 80% quality

**Error Handling Strategy:**
- **Retryable Errors** (return Result.error for WorkManager retry):
  - `IOException` - Network connectivity issues
  - HTTP 500-599 - Server errors
  - `SocketTimeoutException` - API call timeout
- **Non-Retryable Errors** (return Result.failure, log and stop):
  - HTTP 400-499 - Client errors (invalid request, auth failure, rate limit)
  - `JsonSyntaxException` - Response parsing failure
  - Validation errors (calories out of range)
- **Logging**: All errors logged with `Timber.e(exception, message)` including full context

**Timeout Configuration:**
- Connect timeout: 15 seconds (Azure OpenAI endpoints typically fast to connect)
- Read timeout: 30 seconds (AI vision analysis can take 10-30 seconds)
- Write timeout: 30 seconds (base64 image upload can be slow on poor networks)
- **Rationale**: Balance between responsiveness and avoiding false timeouts

**Performance Targets:**
- API call latency: < 10 seconds typical (95th percentile < 15 seconds)
- Base64 encoding: < 2 seconds (for 2MP image)
- Total repository call time: < 15 seconds typical
- WorkManager will retry on timeout (handled in Story 2-5)

### Project Structure Notes

**New Files to Create:**
```
app/src/main/java/com/foodie/app/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── AzureOpenAiApi.kt                    # Retrofit interface
│   │   ├── dto/
│   │   │   ├── AzureResponseRequest.kt              # Request DTO
│   │   │   ├── AzureResponseResponse.kt             # Response DTO
│   │   │   ├── InputItem.kt                         # Multimodal input item
│   │   │   ├── ContentItem.kt                       # Sealed class for text/image content
│   │   │   └── ApiNutritionResponse.kt              # Parsed output_text JSON
│   │   └── interceptor/
│   │       └── AuthInterceptor.kt                   # API key injection
│   └── repository/
│       └── NutritionAnalysisRepositoryImpl.kt       # Repository implementation
├── domain/
│   ├── model/
│   │   └── NutritionData.kt                         # Domain model (calories + description)
│   └── repository/
│       └── NutritionAnalysisRepository.kt           # Repository interface
└── util/
    └── ImageUtils.kt                                # Base64 encoding utility

app/src/test/java/com/foodie/app/
├── data/
│   ├── remote/
│   │   ├── dto/
│   │   │   └── AzureResponseDtoTest.kt              # DTO serialization tests
│   │   └── interceptor/
│   │       └── AuthInterceptorTest.kt               # Auth interceptor tests
│   └── repository/
│       └── NutritionAnalysisRepositoryTest.kt       # Repository unit tests
├── domain/
│   └── model/
│       └── NutritionDataTest.kt                     # Validation tests
└── util/
    └── ImageUtilsTest.kt                            # Base64 encoding tests

app/src/androidTest/java/com/foodie/app/
└── data/
    └── remote/
        └── AzureOpenAiApiIntegrationTest.kt         # API integration tests with mocked server
```

**Modified Files:**
- `app/src/main/java/com/foodie/app/di/NetworkModule.kt` - Add Retrofit, OkHttp, API interface providers
- `app/src/main/java/com/foodie/app/di/RepositoryModule.kt` - Bind NutritionAnalysisRepository implementation
- `app/gradle/libs.versions.toml` - (No new dependencies needed - Retrofit/OkHttp/Gson already added in Epic 1)

**Alignment with Unified Project Structure:**
- API client in `data/remote/api/` follows architecture document structure
- Repository implementation in `data/repository/` with interface in `domain/repository/`
- DTOs in `data/remote/dto/` separate from domain models
- Utility classes in `util/` package
- Testing structure mirrors production code organization

**Dependencies (Already in Project):**
- Retrofit 2.11.0 (HTTP client)
- Gson 2.11.0 (JSON serialization)
- OkHttp 4.12.0 (HTTP implementation)
- OkHttp Logging Interceptor 4.12.0 (request/response logging)
- Kotlin Coroutines 1.9.0 (async operations)
- Timber 5.0.1 (logging)
- Hilt 2.51.1 (dependency injection)

### Learnings from Previous Story

**From Story 2-3 (Camera Integration with Photo Capture) - Status: done**

**New Capabilities to Reuse:**
- **PhotoManager**: Photo file management utility with cache directory access
  - File path pattern: `cacheDir/photos/meal_{timestamp}.jpg`
  - Photos already compressed to 2MP + 80% JPEG quality
  - URI returned by `PhotoManager.createTempPhotoFile()` and `resizeAndCompress()`
- **Image Processing**: EXIF orientation correction and bitmap recycling patterns established
  - Reuse bitmap recycling logic from PhotoManager for memory efficiency
  - EXIF correction already applied - base64 encoding receives properly oriented images
- **Testing Patterns**: Established unit test patterns for file utilities and ViewModels
  - Mockito + Truth library pattern
  - Coroutine test dispatcher usage

**Files to Reference:**
- `app/src/main/java/com/foodie/app/data/local/cache/PhotoManager.kt` - Photo file operations
  - Line 90-145: `resizeAndCompress()` method (2MP + 80% JPEG already applied)
  - Line 54-55: MAX_PIXELS=2_000_000, JPEG_QUALITY=80 constants
  - Line 197-234: `correctOrientation()` method (EXIF handling)
- `app/src/test/java/com/foodie/app/data/local/cache/PhotoManagerTest.kt` - File utility testing patterns

**Integration Points for This Story:**
- ImageUtils will load photos from URIs created by PhotoManager
- Photos are already at optimal size/quality for API upload (no additional processing needed)
- Base64 encoding should recycle bitmaps immediately (follow PhotoManager memory patterns)
- Repository will accept photo URI from CapturePhotoViewModel (Story 2-3 callback)

**Technical Decisions to Carry Forward:**
- **System Camera Intent**: Photo capture complete, URIs point to cache directory files
- **Cache Directory Storage**: Photos stored at `cacheDir/photos/{timestamp}.jpg`
- **Compression Settings**: 2MP max, 80% JPEG quality (optimal for AI analysis)
- **Memory Management**: Bitmap recycling critical (PhotoManager established pattern)

**Warnings/Recommendations:**
- **Base64 Size**: Expect ~500KB-700KB base64 strings for 2MP JPEG 80% images
- **Memory Pressure**: Always recycle bitmaps after encoding (OOM risk on low-memory devices)
- **Network Upload**: Consider Wi-Fi vs cellular data usage (base64 encoding inflates size ~33%)
- **API Timeout**: Azure OpenAI vision analysis can take 10-30 seconds (set read timeout accordingly)

[Source: docs/stories/2-3-camera-integration-with-photo-capture.md#Dev-Agent-Record]

### Documentation Research Strategy

**Recommended: Use Playwright MCP for Interactive Documentation Exploration**

This story involves **Azure OpenAI Responses API** which has extensive, multi-layered official documentation. Use Playwright browser navigation tool for efficient research:

**Starting Points:**
- Azure OpenAI Responses API Reference: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference
- Multimodal Vision Capabilities: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/gpt-with-vision
- Authentication Guide: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference#authentication
- Error Handling and Rate Limits: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/quota

**Focus Areas:**
- Responses API request structure (model, instructions, input array)
- Multimodal input formatting (text + base64 image data URLs)
- Authentication header format (`api-key` vs `Authorization: Bearer`)
- Error response formats and HTTP status codes
- Rate limiting patterns and throttling strategies
- Token usage optimization for vision models

**Playwright Benefits:**
- Navigate Azure documentation hierarchy interactively
- Capture code snippets for request/response examples
- Follow cross-references to related best practices
- Review sample apps and implementation patterns
- Document learnings in Dev Agent Record completion notes

**Examples from Architecture:**
- Azure OpenAI Reference: https://learn.microsoft.com/en-us/azure/ai-services/openai/reference
- GPT-4 Vision Guide: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/gpt-with-vision
- API Rate Limits: https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/quota

**Note:** This is a NON-INTERACTIVE task during story creation. The Playwright research recommendation is for the developer implementing this story, not for the SM agent creating the story specification.

### References

**Source Documents:**

1. **Epic 2, Story 2.4 (Azure OpenAI API Client)** - [Source: docs/epics.md#Story-2-4]
   - Acceptance criteria: Responses API endpoint, multimodal input, authentication, structured JSON output
   - Technical notes: Request/response format, error handling, timeout configuration
   - Prerequisites: Story 1.1 (networking libraries), Story 1.5 (error handling framework)

2. **PRD Azure OpenAI Integration** - [Source: docs/PRD.md#Azure-OpenAI-Integration]
   - API configuration: Responses API v1, GPT-4.1 model, api-key authentication
   - Request format: Multimodal input with instructions field
   - Response parsing: Extract output_text field, parse as JSON
   - Validation: Calories 1-5000, description max 200 characters

3. **Architecture API Integration** - [Source: docs/architecture.md#Azure-OpenAI-API]
   - Retrofit interface: `AzureOpenAiApi` with `analyzeNutrition()` method
   - AuthInterceptor: API key injection in headers
   - Repository pattern: NutritionAnalysisRepository wraps API calls
   - Error handling: Network errors retryable, parse errors non-retryable

4. **Tech Spec Epic 2 - API Module** - [Source: docs/tech-spec-epic-2.md#Azure-OpenAI-API-Module]
   - Component table: AzureOpenAiApi, AuthInterceptor, NutritionAnalysisRepository
   - DTOs: AzureResponseRequest, AzureResponseResponse, ApiNutritionResponse
   - NetworkModule configuration: Retrofit providers, timeout settings
   - Error classification: Retryable (IOException, 5xx) vs non-retryable (4xx, parse errors)

5. **Story 2-3 Photo Management** - [Source: docs/stories/2-3-camera-integration-with-photo-capture.md]
   - PhotoManager utility: Photo URI creation, 2MP compression, 80% JPEG quality
   - Cache directory structure: `cacheDir/photos/meal_{timestamp}.jpg`
   - Memory management: Bitmap recycling patterns
   - Integration: Photo URIs consumed by this story for base64 encoding

**Technical Decisions to Document:**
- **Responses API vs Chat Completions**: Simpler request structure, direct text output
- **api-key Header**: Azure-specific authentication (not standard Bearer token)
- **Base64 Data URL**: `data:image/jpeg;base64,{encoded}` format required by API
- **Timeout Settings**: 30-second read timeout for AI vision analysis latency
- **Error Handling**: Repository returns Result, Worker handles retry (Story 2-5)

## Dev Agent Record

### Context Reference

- `docs/stories/2-4-azure-openai-api-client.context.xml` - Generated 2025-11-10

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List

## Change Log

**2025-11-10 - Story 2.4: Azure OpenAI API Client - Story Drafted**

**Summary:** Drafted story for Azure OpenAI Responses API client integration with multimodal vision support for food photo analysis.

**Story Details:**
- 11 acceptance criteria defined for API client implementation with Responses API format
- 9 tasks with comprehensive subtasks covering DTOs, Retrofit interface, repository, base64 encoding, and testing
- Learnings from Story 2-3 integrated (PhotoManager photo URIs, compression settings, memory management patterns)
- Playwright documentation research strategy included for Azure OpenAI Responses API

**Key Requirements:**
- Azure OpenAI Responses API v1 endpoint integration
- Multimodal input (text + base64 image) with instructions field
- Authentication via `api-key` header (NOT Bearer token)
- Structured JSON output parsing: `{calories: number, description: string}`
- Error handling with retryable (network, 5xx) vs non-retryable (4xx, parse errors) classification
- Base64 image encoding with memory-efficient bitmap recycling
- Timeout configuration: 30-second read timeout for AI analysis latency

**Next Steps:**
- Run `story-context` workflow to generate technical context XML (recommended before implementation)
- Or run `story-ready` workflow to mark story ready for development without context generation
- Implementation ready to begin after context generation or ready-for-dev marking

**Files Expected to Create:** 13 new files (API interface, DTOs, interceptor, repository, domain model, ImageUtils, tests)
**Files Expected to Modify:** 2 files (NetworkModule.kt, RepositoryModule.kt)

---
