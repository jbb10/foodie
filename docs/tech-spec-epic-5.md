# Epic Technical Specification: Configuration & Polish

Date: 2025-11-22
Author: BMad
Epic ID: 5
Status: Draft

---

## Overview

Epic 5 completes the Foodie MVP by implementing essential user configuration capabilities and production-ready polish features. This epic transforms the application from a hardcoded prototype into a customizable, accessible, and professional user experience through a comprehensive settings system, secure API credential management, dark mode support, accessibility improvements, performance optimizations, and guided onboarding.

The core value proposition centers on enabling users to securely configure their Azure OpenAI credentials through the Settings screen (migrating from BuildConfig hardcoded values to encrypted runtime storage), while simultaneously delivering UX refinements that make the app feel polished and production-ready. This epic bridges the gap between functional implementation (Epics 1-4) and a deployable product suitable for daily personal use.

## Objectives and Scope

**In Scope:**
- Settings screen foundation using Android PreferenceScreen framework with organized preference categories
- Secure Azure OpenAI API key storage using Android Keystore encryption (replacing BuildConfig temporary approach)
- Endpoint URL and model/deployment name configuration with test connection validation
- Model selection interface with "gpt-4.1" default and custom deployment name support
- System-wide dark mode support with Material Design 3 dark theme implementation
- Accessibility improvements: TalkBack support, content descriptions, WCAG AA compliance, touch target sizing
- Performance optimization: 60fps transitions, memory profiling, APK size reduction, animation polish
- First-launch onboarding flow: widget setup guidance, Health Connect permissions, API configuration prompts
- Final integration testing across physical devices with documented test coverage

**Out of Scope:**
- Advanced settings features: custom retry counts, network quality thresholds, batch processing configuration (V2.0+)
- Analytics dashboard or calorie tracking insights within the app (V2.0+)
- Multi-user support or cloud sync of settings (client-only architecture maintained)
- Custom theme builder or color palette selection beyond light/dark modes
- Accessibility features beyond WCAG AA standards (AAA standards deferred to V2.0)
- Automated performance regression testing infrastructure
- In-app help system or interactive tutorials beyond first-launch onboarding

**Success Criteria:**
- API credentials successfully migrate from BuildConfig to encrypted SharedPreferences without data loss
- Test connection validates API key, endpoint, and model configuration with clear pass/fail feedback
- Dark mode implementation passes visual regression testing across all screens
- Accessibility Scanner reports zero critical issues, TalkBack navigation functions correctly
- App cold launch time ≤ 2 seconds, screen transitions maintain 60fps on mid-range devices
- First-launch onboarding completes in < 2 minutes with clear guidance to first capture
- All acceptance criteria from 8 stories verified through manual testing on physical devices

## System Architecture Alignment

Epic 5 builds upon the established MVVM architecture from Epic 1, introducing Settings infrastructure as a new foundational component while enhancing existing UI layers with theming, accessibility, and performance patterns.

**New Components:**

**SettingsScreen + SettingsViewModel** (ui/screens/settings/)
- Implements Android PreferenceFragmentCompat pattern with PreferenceScreen XML definitions
- Exposes encrypted preferences via SettingsRepository
- Integrates with navigation graph for toolbar menu access
- Provides test connection UI with loading states and validation feedback

**SecurePreferencesManager** (data/local/preferences/)
- Wraps Android EncryptedSharedPreferences for API credential storage
- Replaces temporary BuildConfig approach from Story 2.4
- Provides type-safe accessor methods: getApiKey(), getEndpoint(), getModelName()
- Handles encryption/decryption transparently using Android Keystore

**PreferencesRepository** (data/repository/)
- Domain layer abstraction over SecurePreferencesManager
- Exposes Flow<Preferences> for reactive settings updates
- Implements validation logic for endpoint URL format, model name constraints

**Enhanced Components:**

**AzureOpenAiApi / AuthInterceptor** (data/remote/api/)
- Updated to read credentials from SecurePreferencesManager instead of BuildConfig
- Maintains compatibility with existing Responses API implementation from Epic 2

**Theme Configuration** (ui/theme/)
- Extends Material Design 3 theme with dark color palette (values-night/themes.xml)
- AppCompatDelegate integration for runtime theme switching
- Theme preference stored in standard SharedPreferences

**All Composable Screens**
- Enhanced with contentDescription attributes for accessibility
- Minimum 48dp touch targets enforced via layout constraints
- Dynamic color support for system theme following

**Architectural Patterns:**

**Settings Storage Strategy:**
```
Sensitive Data (API Key):
→ EncryptedSharedPreferences via SecurePreferencesManager
→ Android Keystore hardware-backed encryption
→ Never logged, masked in UI

Non-Sensitive Data (Endpoint, Model, Theme):
→ Standard SharedPreferences via PreferenceManager
→ Values displayed in settings UI
→ Logged for debugging (non-PII)
```

**Theme Switching Flow:**
```
User selects theme preference in Settings
→ SettingsViewModel updates SharedPreferences
→ AppCompatDelegate.setDefaultNightMode(mode)
→ Activity recreates with new theme
→ All Compose screens reactively apply theme colors
```

**Test Connection Validation:**
```
User taps "Test Connection" button
→ SettingsViewModel validates inputs (non-empty, valid URL format)
→ Calls AzureOpenAiApi.testConnection() with minimal Responses API request
→ Success: Display "API configuration valid ✓" toast
→ Failure: Display error classification from ErrorHandler (reusing Epic 4 infrastructure)
→ Show ProgressDialog during network call (UX feedback)
```

**Onboarding Flow:**
```
First Launch Detection (SharedPreferences flag "onboarding_completed")
→ ViewPager2 with 3-4 onboarding screens
→ Screen 1: Welcome + core concept explanation
→ Screen 2: Widget setup instructions with visuals
→ Screen 3: Health Connect permission request with rationale
→ Screen 4: Settings prompt for API configuration
→ "Skip" option on each screen
→ Set "onboarding_completed" flag on completion/skip
→ Navigate to MealListScreen
```

## Detailed Design

### Services and Modules

**SettingsScreen** (ui/screens/settings/)
- Composable screen built with PreferenceFragmentCompat pattern
- Organized into preference categories: API Configuration, Appearance, About
- Provides live validation feedback for API credentials
- Displays masked API key preview when configured

**SettingsViewModel** (ui/screens/settings/)
- Manages settings state and user interactions
- Validates API key, endpoint URL format, model name
- Executes test connection flow with loading states
- Exposes `StateFlow<SettingsState>` with validation errors, loading indicators

**PreferencesRepository** (data/repository/)
- Domain layer abstraction over SecurePreferencesManager and standard PreferenceManager
- Exposes reactive `Flow<ApiConfiguration>` for credential changes
- Implements validation: endpoint must be valid HTTPS URL, model name non-empty
- Provides `testConnection()` method delegating to AzureOpenAiApi

**SecurePreferencesManager** (data/local/preferences/)
- Wraps `EncryptedSharedPreferences` with type-safe API key storage
- Uses Android Keystore with AES256_GCM encryption scheme
- Methods: `saveApiKey(key: String)`, `getApiKey(): String?`, `clearApiKey()`
- Never logs API key, handles encryption exceptions gracefully

**PreferenceManager** (standard Android)
- Stores non-sensitive settings: endpoint URL, model name, theme preference
- Used by PreferenceScreen XML for automatic UI binding
- Keys: `pref_azure_endpoint`, `pref_azure_model`, `pref_theme_mode`

**ThemeManager** (ui/theme/)
- Singleton managing app-wide theme state
- Integrates with `AppCompatDelegate.setDefaultNightMode()`
- Modes: SYSTEM_DEFAULT, LIGHT, DARK
- Persists theme preference in standard SharedPreferences

**OnboardingActivity** (ui/onboarding/)
- ViewPager2-based flow with 3-4 screens
- Screens: Welcome, Widget Setup, Health Connect Permissions, Settings Prompt
- First-launch detection via SharedPreferences flag `onboarding_completed`
- Skippable with "Skip" button on each screen

**AccessibilityEnhancer** (ui/accessibility/)
- Utility class providing content description helpers
- Validates minimum touch target sizes (48dp enforcement)
- Generates semantic labels for screen reader compatibility

**PerformanceOptimizer** (util/)
- Provides memory profiling utilities
- Implements animation helpers for smooth transitions
- APK size monitoring and resource optimization helpers

| Component | Type | Responsibilities | Dependencies |
|-----------|------|------------------|--------------|
| SettingsScreen | Composable | Render preferences UI, handle user input | SettingsViewModel |
| SettingsViewModel | ViewModel | Settings state management, validation, test connection | PreferencesRepository, AzureOpenAiApi |
| PreferencesRepository | Repository | Preference CRUD, validation, reactive updates | SecurePreferencesManager, PreferenceManager |
| SecurePreferencesManager | Data Source | Encrypted API key storage using Keystore | EncryptedSharedPreferences, MasterKey |
| ThemeManager | Singleton | Theme state management, runtime theme switching | AppCompatDelegate, SharedPreferences |
| OnboardingActivity | Activity | First-launch guidance flow | ViewPager2, SharedPreferences |

### Data Models and Contracts

**ApiConfiguration** (domain/model/)
```kotlin
data class ApiConfiguration(
    val apiKey: String,        // Encrypted, never logged
    val endpoint: String,      // Format: https://{resource}.openai.azure.com
    val modelName: String,     // Default: "gpt-4.1", allows custom deployments
    val isConfigured: Boolean  // True if all fields are non-empty
) {
    fun validate(): ValidationResult {
        return when {
            apiKey.isBlank() -> ValidationResult.Error("API key required")
            endpoint.isBlank() -> ValidationResult.Error("Endpoint URL required")
            !endpoint.startsWith("https://") -> ValidationResult.Error("Endpoint must use HTTPS")
            !endpoint.contains(".openai.azure.com") -> ValidationResult.Error("Invalid Azure OpenAI endpoint format")
            modelName.isBlank() -> ValidationResult.Error("Model name required")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

**ThemeMode** (domain/model/)
```kotlin
enum class ThemeMode(val value: Int) {
    SYSTEM_DEFAULT(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);
    
    companion object {
        fun fromValue(value: Int): ThemeMode {
            return values().find { it.value == value } ?: SYSTEM_DEFAULT
        }
    }
}
```

**SettingsState** (ui/screens/settings/)
```kotlin
data class SettingsState(
    val apiKey: String = "",
    val endpoint: String = "",
    val modelName: String = "gpt-4.1",
    val themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    val isTestingConnection: Boolean = false,
    val testConnectionResult: TestConnectionResult? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

sealed class TestConnectionResult {
    object Success : TestConnectionResult()
    data class Failure(val errorMessage: String) : TestConnectionResult()
}
```

**OnboardingState** (ui/onboarding/)
```kotlin
data class OnboardingState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val healthConnectPermissionsGranted: Boolean = false,
    val canProceedToApp: Boolean = false
)
```

**PreferenceKeys** (data/local/preferences/)
```xml
<!-- res/xml/preferences.xml -->
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
    <PreferenceCategory android:title="API Configuration">
        <EditTextPreference
            android:key="pref_azure_endpoint"
            android:title="Azure OpenAI Endpoint"
            android:summary="https://your-resource.openai.azure.com"
            android:inputType="textUri" />
        
        <EditTextPreference
            android:key="pref_azure_model"
            android:title="Model Name"
            android:summary="gpt-4.1"
            android:defaultValue="gpt-4.1"
            android:inputType="text" />
        
        <!-- API Key stored separately in EncryptedSharedPreferences -->
        <!-- Custom preference widget for masked input -->
    </PreferenceCategory>
    
    <PreferenceCategory android:title="Appearance">
        <ListPreference
            android:key="pref_theme_mode"
            android:title="Theme"
            android:entries="@array/theme_entries"
            android:entryValues="@array/theme_values"
            android:defaultValue="system" />
    </PreferenceCategory>
</PreferenceScreen>
```

### APIs and Interfaces

**PreferencesRepository Interface**
```kotlin
interface PreferencesRepository {
    // API Configuration
    suspend fun saveApiConfiguration(config: ApiConfiguration): Result<Unit>
    fun getApiConfiguration(): Flow<ApiConfiguration>
    suspend fun testConnection(): Result<TestConnectionResult>
    suspend fun clearApiConfiguration(): Result<Unit>
    
    // Theme
    suspend fun saveThemeMode(mode: ThemeMode): Result<Unit>
    fun getThemeMode(): Flow<ThemeMode>
    
    // Onboarding
    suspend fun markOnboardingCompleted(): Result<Unit>
    fun isOnboardingCompleted(): Flow<Boolean>
}
```

**SecurePreferencesManager Interface**
```kotlin
interface SecurePreferencesManager {
    fun saveApiKey(apiKey: String)
    fun getApiKey(): String?
    fun clearApiKey()
    fun hasApiKey(): Boolean
}
```

**AzureOpenAiApi Extension** (data/remote/api/)
```kotlin
interface AzureOpenAiApi {
    // Existing method from Epic 2
    @POST("openai/v1/responses")
    suspend fun analyzeNutrition(@Body request: NutritionAnalysisRequest): NutritionAnalysisResponse
    
    // New method for Story 5.2 - Test Connection
    @POST("openai/v1/responses")
    suspend fun testConnection(@Body request: TestConnectionRequest): ResponsesApiResponse
}

data class TestConnectionRequest(
    val model: String,
    val instructions: String = "Return a simple greeting.",
    val input: List<ResponsesInput> = listOf(
        ResponsesInput(
            role = "user",
            content = listOf(
                ResponsesContent(type = "input_text", text = "Hello")
            )
        )
    )
)
```

**ThemeManager Interface**
```kotlin
interface ThemeManager {
    fun applyTheme(mode: ThemeMode)
    fun getCurrentTheme(): ThemeMode
    fun observeTheme(): Flow<ThemeMode>
}
```

### Workflows and Sequencing

**Settings Configuration Flow:**
```
1. User navigates to Settings (toolbar menu → Settings)
   → SettingsScreen renders with current values from PreferencesRepository

2. User enters/updates API credentials:
   - Endpoint URL: https://my-resource.openai.azure.com
   - Model Name: gpt-4.1
   - API Key: API Key (masked as •••••)
   
3. User taps "Test Connection" button:
   → SettingsViewModel validates inputs locally
   → If invalid: Display validation errors inline
   → If valid: Show loading spinner, call PreferencesRepository.testConnection()
   
4. Test Connection execution:
   → PreferencesRepository delegates to AzureOpenAiApi.testConnection()
   → Minimal Responses API request: {model, instructions, input: ["Hello"]}
   → Success: Parse response, check status == "completed"
   → Failure: ErrorHandler classifies error (reusing Epic 4 infrastructure)
   
5. Display result:
   → Success: Toast "API configuration valid ✓" + save credentials
   → Failure: Error message with specific issue (invalid key, wrong endpoint, model not found)
   
6. User taps Save:
   → SettingsViewModel calls PreferencesRepository.saveApiConfiguration()
   → API Key → SecurePreferencesManager (encrypted)
   → Endpoint + Model → Standard SharedPreferences
   → Navigate back to MealListScreen
```

**Theme Switching Flow:**
```
1. User opens Settings → Appearance → Theme
   → ListPreference shows options: System Default, Light, Dark

2. User selects "Dark":
   → PreferenceManager updates pref_theme_mode
   → SettingsViewModel observes change via Flow
   → Calls ThemeManager.applyTheme(ThemeMode.DARK)
   
3. ThemeManager applies theme:
   → AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
   → Activity recreates with new theme
   → All Compose screens reactively apply dark color palette
   
4. Theme persists across app restarts:
   → FoodieApplication.onCreate() reads pref_theme_mode
   → Applies saved theme before rendering any UI
```

**First-Launch Onboarding Flow:**
```
1. App launch:
   → FoodieApplication checks SharedPreferences flag "onboarding_completed"
   → If false: Launch OnboardingActivity
   → If true: Navigate to MealListScreen

2. Onboarding Screen 1 - Welcome:
   → Title: "Welcome to Foodie"
   → Body: "Capture meals in 2 seconds. AI analyzes. Health Connect saves."
   → Buttons: [Skip] [Next]
   
3. Onboarding Screen 2 - Widget Setup:
   → Title: "Add Home Screen Widget"
   → Visual: Screenshot showing widget placement
   → Instructions: "Long-press home screen → Widgets → Foodie → Log Meal"
   → Buttons: [Skip] [Next]
   
4. Onboarding Screen 3 - Health Connect Permissions:
   → Title: "Grant Health Connect Access"
   → Rationale: "Foodie saves nutrition data to Health Connect for interoperability"
   → Action: "Grant Permissions" button → HealthConnectClient.requestPermissions()
   → Buttons: [Skip] [Next]
   
5. Onboarding Screen 4 - Settings Prompt:
   → Title: "Configure Azure OpenAI"
   → Body: "Enter your API key to enable AI analysis"
   → Action: "Open Settings" button → Navigate to SettingsScreen
   → Buttons: [Skip] [Done]
   
6. Completion:
   → Set SharedPreferences flag "onboarding_completed" = true
   → Navigate to MealListScreen
   → Onboarding never shows again (unless app data cleared)
```

**API Credential Migration Flow (Story 5.2):**
```
1. First launch of Story 5.2 build:
   → App detects BuildConfig values still populated (temporary approach from Story 2.4)
   → Automatic migration triggered in FoodieApplication.onCreate()
   
2. Migration logic:
   → Read BuildConfig.AZURE_OPENAI_ENDPOINT
   → Read BuildConfig.AZURE_OPENAI_MODEL
   → Read BuildConfig.AZURE_OPENAI_API_KEY
   → Save to SecurePreferencesManager (API key encrypted)
   → Save endpoint + model to standard SharedPreferences
   → Set migration flag "credentials_migrated" = true
   
3. Post-migration:
   → BuildConfig values ignored (still present but unused)
   → All API calls read from SecurePreferencesManager via AuthInterceptor
   → User can update credentials via Settings UI
   
4. Error handling:
   → If migration fails: Log error, show notification prompting manual setup
   → User can manually enter credentials via Settings
```

**Accessibility Testing Flow:**
```
1. Enable TalkBack (Settings → Accessibility → TalkBack)
   
2. Navigate app with screen reader:
   → All buttons announce their purpose: "Log Meal button", "Settings button"
   → List items announce: "Meal entry, 650 calories, Grilled chicken with rice, Today at 12:30 PM"
   → Edit fields announce current values and constraints
   
3. Touch target validation:
   → All interactive elements minimum 48dp x 48dp
   → Adequate spacing between adjacent targets (minimum 8dp)
   
4. Focus order validation:
   → Tab/D-pad navigation follows logical top-to-bottom, left-to-right order
   → Focus indicators visible on all interactive elements
   
5. Contrast ratio validation:
   → Run Accessibility Scanner tool
   → Verify all text meets WCAG AA 4.5:1 ratio
   → Fix any flagged issues (adjust colors, increase font weight)
```

## Non-Functional Requirements

### Performance

**Settings Screen Performance:**
- Settings screen render time: < 200ms from navigation tap to full display
- Preference changes reflect immediately (< 50ms update latency)
- Test connection API call: < 10 seconds typical, timeout at 30 seconds
- Theme switching: Activity recreation < 500ms, smooth transition without flicker

**Dark Mode Rendering:**
- Theme application at app startup: < 100ms overhead (no noticeable delay)
- No frame drops during theme switching (maintain 60fps during activity recreation)
- Dynamic color calculation cached to avoid repeated computation

**Accessibility Performance:**
- TalkBack focus navigation: < 100ms between element focus changes
- Content description generation: Pre-computed at compose time (zero runtime overhead)
- Touch target size validation: Compile-time enforcement via layout constraints (no runtime cost)

**Onboarding Flow:**
- ViewPager2 swipe transitions: 60fps smooth animation
- First screen render: < 500ms from app launch
- Permission request dialog: < 200ms to display after button tap
- Total onboarding completion: < 2 minutes for users who complete all steps

**Performance Optimization Targets (Story 5.6):**
- Cold app launch: ≤ 2 seconds (from tap to usable MealListScreen)
- Warm app launch: ≤ 1 second (from background to foreground)
- Screen transitions: 60fps smooth animations (no jank or stuttering)
- List scrolling: 60fps with no frame drops on mid-range devices
- Memory footprint: < 100MB typical usage, < 150MB peak during image processing
- APK size: < 10MB for MVP (ProGuard/R8 enabled, unused resources stripped)

**Testing Methodology:**
- Profile with Android Profiler (CPU, Memory, Network traces)
- Test on mid-range device: Samsung Galaxy A53 or equivalent (not just flagship Pixel)
- Measure launch times using `adb shell am start -W com.foodie.app`
- Monitor frame rates using GPU Rendering Profile tool
- Use LeakCanary to detect memory leaks during development

### Security

**API Key Protection:**
- Storage: Android Keystore with AES256_GCM hardware-backed encryption via EncryptedSharedPreferences
- Transmission: API key sent only via HTTPS in `api-key` header (never in URL or logs)
- UI Display: Masked as `••••••••` in settings (show only last 4 characters for verification)
- Logging: Never log API key in Timber, crash reports, or analytics (validated via code review)
- Clipboard: Disable copy/paste on API key input field to prevent accidental exposure

**Endpoint Validation:**
- URL format: Must start with `https://` (reject http:// to prevent MitM attacks)
- Domain validation: Must contain `.openai.azure.com` to prevent phishing endpoints
- Certificate pinning: Not implemented in V1.0 (rely on system certificate validation)
- Network security config: Enforce cleartext traffic disabled in AndroidManifest.xml

**Credential Migration Security:**
- BuildConfig API key: Removed from version control after migration (local.properties gitignored)
- Migration timing: Execute once on first launch of Story 5.2 build, set flag to prevent re-execution
- Error handling: If migration fails, prompt user for manual entry (never expose partial credentials)
- Audit trail: Log migration success/failure (but never log actual credential values)

**SharedPreferences Security:**
- Sensitive data (API key): EncryptedSharedPreferences only
- Non-sensitive data (endpoint, model, theme): Standard SharedPreferences acceptable
- File permissions: Default MODE_PRIVATE (no WORLD_READABLE flags)
- Backup exclusion: Add `android:allowBackup="false"` to prevent cloud backup of credentials

**Network Security:**
- TLS 1.2+ required for all HTTPS connections (OkHttp default)
- Certificate validation: System trust store only (no custom certificates)
- No proxy support: Direct connections only to prevent credential interception
- Timeout enforcement: 30-second connection timeout to prevent hanging requests

**ProGuard/R8 Obfuscation:**
- Enable code shrinking and obfuscation in release builds
- Protect security-critical classes: SecurePreferencesManager, AuthInterceptor
- Keep rules for Retrofit, Gson, Health Connect APIs
- Remove unused encryption algorithms to reduce attack surface

### Reliability/Availability

**Settings Persistence:**
- All settings saved atomically (commit() vs apply() - use commit for critical data)
- Rollback on save failure: Revert to previous values, display error toast
- Default values: Provide sensible defaults for all preferences (gpt-4.1 for model, System Default for theme)
- Corruption recovery: Catch EncryptedSharedPreferences exceptions, prompt re-entry if decryption fails

**API Key Management:**
- Handle Keystore exceptions gracefully: If encryption fails, prompt user to re-enter
- Key rotation support: Allow updating API key without app restart via Settings
- Validation before save: Test connection before committing credentials to storage
- Fallback mechanism: If EncryptedSharedPreferences unavailable, fail gracefully with clear error message

**Theme Switching Reliability:**
- Activity recreation: Handle configuration changes properly (ViewModel survives rotation)
- State preservation: Save pending edits before theme change triggers recreation
- Animation smoothness: No visual glitches during theme transition
- Fallback: If theme application fails, default to System Default theme

**Onboarding Flow Reliability:**
- Skip option: Always available, never block user from accessing app
- Permission denial handling: Allow proceeding even if Health Connect permissions denied
- State persistence: If app killed mid-onboarding, resume at last completed screen
- Completion flag: Set atomically to prevent showing onboarding multiple times

**Error Recovery:**
- Test connection failures: Classify errors using Epic 4 ErrorHandler, display actionable messages
- Migration failures: Log error, show notification prompting manual setup in Settings
- Keystore access failures: Fall back to re-entry flow with clear explanation
- Network timeouts: Cancel gracefully, don't leave hanging progress indicators

**Graceful Degradation:**
- If Settings screen crashes: User can still use app with BuildConfig fallback (temporary)
- If theme fails to apply: Default to light theme without crashing
- If onboarding fails: Allow skipping to main app functionality
- If accessibility features fail: Core functionality remains usable

### Observability

**Logging Strategy:**

**Settings Operations:**
```kotlin
// Log configuration changes (non-sensitive data only)
Timber.d("Azure endpoint updated: $endpoint")
Timber.d("Model selection changed: $modelName")
Timber.d("Theme mode changed: $themeMode")

// Never log sensitive data
// ❌ BAD: Timber.d("API key: $apiKey")
// ✅ GOOD: Timber.d("API key updated (${apiKey.length} characters)")
```

**Test Connection:**
```kotlin
Timber.d("Test connection initiated: endpoint=$endpoint, model=$modelName")
// Success
Timber.i("Test connection successful: ${response.id}")
// Failure
Timber.w("Test connection failed: ${error.javaClass.simpleName}", error)
```

**Theme Switching:**
```kotlin
Timber.d("Applying theme: $themeMode")
Timber.d("Theme applied successfully, recreating activity")
```

**Onboarding:**
```kotlin
Timber.d("Onboarding started: first launch detected")
Timber.d("Onboarding page changed: $currentPage/$totalPages")
Timber.i("Onboarding completed, marking flag")
Timber.d("Onboarding skipped on page $currentPage")
```

**Performance Monitoring:**
```kotlin
// Track critical user flows
Timber.d("Settings screen opened: render time ${elapsedMs}ms")
Timber.d("Test connection completed: ${elapsedMs}ms")
Timber.d("Theme switch completed: ${elapsedMs}ms")
```

**Error Logging:**
```kotlin
// Encryption errors
Timber.e(exception, "Failed to encrypt API key")

// Migration errors
Timber.e(exception, "Credential migration failed")

// Validation errors
Timber.w("Invalid endpoint format: $endpoint")
```

**Analytics Events (V2.0 - Not Implemented in MVP):**
- Settings screen viewed
- Test connection success/failure
- Theme mode changed
- Onboarding completed/skipped
- No user tracking or PII collection in V1.0

**Crash Reporting:**
- Use Timber.e() for exceptions (logged to system in debug, could integrate Firebase Crashlytics in V2.0)
- Include context: screen name, operation being performed
- Exclude sensitive data: Never include API keys in crash reports
- Stack traces: Full stack for debug builds, obfuscated for release builds

**Performance Metrics:**
- Log slow operations: Warn if test connection > 15 seconds
- Memory warnings: Log if app approaches memory limit
- ANR detection: Monitor for operations blocking main thread > 5 seconds
- Frame rate: Use GPU Rendering Profile in development to catch jank

## Dependencies and Integrations

**Existing Dependencies (No Changes Required):**

| Dependency | Version | Purpose | Epic 5 Usage |
|------------|---------|---------|-------------|
| androidx.security:security-crypto | 1.1.0-alpha06 | EncryptedSharedPreferences | SecurePreferencesManager for API key encryption |
| androidx.compose.material3 | BOM 2024.10.01 | Material Design 3 components | Dark theme support, Settings UI |
| androidx.appcompat:appcompat | Via compose-bom | Theme management | AppCompatDelegate for runtime theme switching |
| androidx.preference:preference-ktx | (Add if needed) | PreferenceScreen framework | Settings screen foundation (Story 5.1) |
| retrofit:retrofit | 2.11.0 | HTTP client | Test connection API calls |
| okhttp:okhttp | 4.12.0 | Network layer | Existing, used by test connection |
| timber:timber | 5.0.1 | Logging | Settings operations, migration logging |
| androidx.navigation:navigation-compose | 2.8.4 | Navigation | Settings screen integration |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7 | ViewModel | SettingsViewModel |
| hilt:hilt-android | 2.51.1 | Dependency Injection | PreferencesRepository, SecurePreferencesManager |

**New Dependencies (Story 5.1 - Settings Screen):**
```kotlin
// App-level build.gradle.kts additions
dependencies {
    // Preference library for Settings screen
    implementation("androidx.preference:preference-ktx:1.2.1")
}
```

**Integration Points:**

**1. AuthInterceptor Integration (Story 5.2):**
- **Current State:** Reads API key from BuildConfig (temporary)
- **Epic 5 Change:** Update to read from SecurePreferencesManager
- **Location:** `data/remote/interceptor/AuthInterceptor.kt`
- **Impact:** All API calls automatically use runtime-configured credentials

```kotlin
// Before (Story 2.4)
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val apiKey = BuildConfig.AZURE_OPENAI_API_KEY
        // ...
    }
}

// After (Story 5.2)
class AuthInterceptor @Inject constructor(
    private val securePreferences: SecurePreferencesManager
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val apiKey = securePreferences.getApiKey()
            ?: throw IllegalStateException("API key not configured")
        // ...
    }
}
```

**2. Theme System Integration (Story 5.4):**
- **Component:** FoodieApplication.onCreate()
- **Responsibility:** Apply saved theme before first Activity creation
- **Dependencies:** AppCompatDelegate, SharedPreferences
- **Timing:** Critical - must execute before any UI renders

**3. Navigation Integration (Story 5.1):**
- **Component:** NavGraph.kt
- **New Route:** `"settings"` destination
- **Trigger:** Toolbar menu item "Settings" in MealListScreen
- **Return:** Back navigation to MealListScreen after save

**4. Health Connect Integration (Story 5.7 - Onboarding):**
- **Component:** OnboardingActivity Screen 3
- **Permission Request:** HealthConnectClient.requestPermissions()
- **Rationale Display:** Explain why Foodie needs WRITE_NUTRITION and READ_NUTRITION
- **Existing Integration:** Reuse HealthConnectManager from Epic 1

**5. Error Classification Integration (Story 5.2 - Test Connection):**
- **Component:** ErrorHandler from Epic 4
- **Usage:** Classify test connection failures (network, auth, server errors)
- **User Messages:** Reuse existing user-friendly error messages
- **No Changes Required:** ErrorHandler already handles all needed error types

**Migration Strategy:**

**BuildConfig → EncryptedSharedPreferences Migration:**
```kotlin
// FoodieApplication.onCreate()
class FoodieApplication : Application() {
    @Inject lateinit var securePreferences: SecurePreferencesManager
    @Inject lateinit var preferenceManager: PreferenceManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme first
        applyTheme()
        
        // Migrate BuildConfig credentials if needed
        migrateCredentialsIfNeeded()
    }
    
    private fun migrateCredentialsIfNeeded() {
        val migrationFlag = "credentials_migrated"
        if (preferenceManager.getBoolean(migrationFlag, false)) {
            return // Already migrated
        }
        
        try {
            // Read from BuildConfig (temporary storage from Story 2.4)
            val apiKey = BuildConfig.AZURE_OPENAI_API_KEY
            val endpoint = BuildConfig.AZURE_OPENAI_ENDPOINT
            val model = BuildConfig.AZURE_OPENAI_MODEL
            
            // Save to encrypted storage
            if (apiKey.isNotEmpty()) {
                securePreferences.saveApiKey(apiKey)
                preferenceManager.edit()
                    .putString("pref_azure_endpoint", endpoint)
                    .putString("pref_azure_model", model)
                    .putBoolean(migrationFlag, true)
                    .apply()
                
                Timber.i("Credentials migrated successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Credential migration failed")
            // User will need to manually configure in Settings
        }
    }
}
```

**External Service Integrations:**

**Azure OpenAI Responses API:**
- **Endpoint Pattern:** `https://{resource}.openai.azure.com/openai/v1/responses`
- **Authentication:** `api-key` header with encrypted key from SecurePreferencesManager
- **Test Connection:** Minimal request to validate credentials (Story 5.2)
- **No Changes to API Contract:** Epic 5 only changes credential source, not API usage

**Android Keystore:**
- **Purpose:** Hardware-backed encryption for API key
- **Integration:** Via EncryptedSharedPreferences (androidx.security.security-crypto)
- **Key Generation:** Automatic on first use
- **Error Handling:** Graceful fallback to re-entry if decryption fails

## Acceptance Criteria (Authoritative)

**Story 5.1: Settings Screen Foundation**
1. Settings screen accessible from main app toolbar menu item
2. Settings screen uses Android PreferenceScreen framework with organized categories
3. All settings persist across app restarts using SharedPreferences
4. Screen follows Material Design 3 guidelines
5. Navigation back to main screen works properly
6. Settings organized into categories: API Configuration, Appearance, About

**Story 5.2: Azure OpenAI API Key and Endpoint Configuration**
1. API key field available in Settings with EditTextPreference (masked input)
2. Endpoint field accepts Azure OpenAI base URL (https://{resource}.openai.azure.com)
3. Model/deployment name field available with default "gpt-4.1"
4. API key stored encrypted in Android Keystore via EncryptedSharedPreferences
5. Endpoint and model name stored in standard SharedPreferences
6. "Test Connection" button validates API configuration with minimal Responses API request
7. Test connection displays result: "API configuration valid ✓" or specific error message
8. Clear instructions explain where to get credentials (portal.azure.com)
9. API key field shows masked preview if already configured (show last 4 chars)
10. Endpoint and model fields show current values when configured
11. All values retrievable by AuthInterceptor when making API calls
12. Automatic migration from BuildConfig to EncryptedSharedPreferences on first launch

**Story 5.3: Model Selection and Configuration**
1. Model selection field with option "gpt-4.1" available in Settings
2. Custom model/deployment name entry supported
3. Selected model used in `model` field of Responses API requests
4. Helpful description for gpt-4.1: "Advanced reasoning and vision capabilities"
5. Test connection validates selected model availability in deployment
6. Model selection stored in SharedPreferences
7. Model selection defaults to "gpt-4.1" for new installations

**Story 5.4: Dark Mode Support**
1. All screens use dark theme colors when dark mode enabled
2. Theme follows Material Design 3 dark theme guidelines (values-night/themes.xml)
3. Text maintains proper contrast ratios (WCAG AA 4.5:1 minimum)
4. Camera preview maintains natural colors (not inverted)
5. Notifications use dark styling when appropriate
6. App respects system dark mode setting
7. In-app theme selector: "System Default", "Light", "Dark" in Settings
8. Theme preference persists across app restarts

**Story 5.5: Accessibility Improvements**
1. All buttons and images have content descriptions for TalkBack
2. App supports TalkBack screen reader with logical announcement order
3. Touch targets minimum 48dp (WCAG compliance)
4. Text scales properly with system font size settings
5. Color contrast ratios meet WCAG AA standards (4.5:1 for normal text)
6. Important information not conveyed by color alone
7. Focus order logical for keyboard/D-pad navigation
8. Camera capture button easily discoverable with TalkBack
9. Accessibility Scanner reports zero critical issues

**Story 5.6: Performance Optimization and Polish**
1. All screen transitions smooth at 60fps (no jank)
2. List view scrolling has no stuttering on mid-range devices
3. Image loading uses efficient caching and compression
4. Memory usage < 100MB typical, < 150MB peak
5. Cold app launch ≤ 2 seconds
6. Battery impact minimal (efficient background processing)
7. APK size < 10MB (ProGuard/R8 enabled, resources optimized)
8. No memory leaks detected via LeakCanary
9. Smooth animations enhance capture flow (fade-ins, slide transitions)

**Story 5.7: User Onboarding (First Launch)**
1. Welcome message displays on first launch explaining core concept
2. User prompted to add home screen widget with clear instructions
3. Widget addition instructions: "Long-press home screen → Widgets → Foodie"
4. Health Connect permissions requested with clear rationale
5. Azure OpenAI configuration prompt directs to Settings
6. Onboarding flow completes in < 2 minutes
7. User can skip onboarding if desired ("Skip" button on each screen)
8. Onboarding shows only once (first launch detection via SharedPreferences)
9. After onboarding, user on MealListScreen ready to use app
10. ViewPager2 implementation with 3-4 screens maximum

**Story 5.8: Final Integration Testing and Bug Fixes**
1. Complete end-to-end flow works on physical device (all epics integrated)
2. All user stories manually tested and verified
3. Edge cases tested: poor network, permission denials, low storage
4. Performance targets met: < 5s capture, < 500ms list load
5. No critical bugs preventing core functionality
6. Known issues documented with workarounds
7. App tested on at least 2 different Android devices
8. Different Android versions tested (API 28 minimum, API 35 target)
9. Battery usage acceptable for daily tracking (3-5 captures per day)
10. Test checklist covering all 33 user stories across 5 epics completed

## Traceability Mapping

| AC # | Story | Requirement | Component | Test Type |
|------|-------|-------------|-----------|----------|
| 5.1-1 | Settings Foundation | Settings screen accessible from toolbar | NavGraph, MainActivity toolbar | Manual |
| 5.1-2 | Settings Foundation | PreferenceScreen framework used | SettingsScreen, res/xml/preferences.xml | Code Review |
| 5.1-3 | Settings Foundation | Settings persist across restarts | SharedPreferences, PreferenceManager | Manual |
| 5.1-4 | Settings Foundation | Material Design 3 compliance | Theme.kt, SettingsScreen styling | Visual QA |
| 5.1-5 | Settings Foundation | Back navigation works | NavController.popBackStack() | Manual |
| 5.1-6 | Settings Foundation | Organized categories | PreferenceCategory in XML | Visual QA |
| 5.2-1 | API Configuration | API key field with masked input | SecurePreferencesManager, custom Preference | Manual |
| 5.2-2 | API Configuration | Endpoint field for Azure URL | EditTextPreference pref_azure_endpoint | Manual |
| 5.2-3 | API Configuration | Model/deployment name field | EditTextPreference pref_azure_model | Manual |
| 5.2-4 | API Configuration | API key encrypted in Keystore | EncryptedSharedPreferences, AES256_GCM | Unit Test |
| 5.2-5 | API Configuration | Endpoint/model in SharedPrefs | Standard SharedPreferences | Unit Test |
| 5.2-6 | API Configuration | Test connection button validates | SettingsViewModel.testConnection() | Integration Test |
| 5.2-7 | API Configuration | Test connection displays result | UI state: Success/Failure toast | Manual |
| 5.2-8 | API Configuration | Clear instructions provided | Preference summary text | Visual QA |
| 5.2-9 | API Configuration | Masked API key preview | Custom Preference masking logic | Manual |
| 5.2-10 | API Configuration | Current values displayed | Preference summary binding | Manual |
| 5.2-11 | API Configuration | Values used by AuthInterceptor | AuthInterceptor.intercept() | Integration Test |
| 5.2-12 | API Configuration | Auto-migration from BuildConfig | FoodieApplication.onCreate() migration | Manual |
| 5.3-1 | Model Selection | Model selection field available | ListPreference pref_azure_model | Manual |
| 5.3-2 | Model Selection | Custom deployment name supported | EditTextPreference fallback | Manual |
| 5.3-3 | Model Selection | Model used in API requests | NutritionAnalysisRequest.model field | Code Review |
| 5.3-4 | Model Selection | Helpful model descriptions | Preference summary text | Visual QA |
| 5.3-5 | Model Selection | Test validates model availability | TestConnectionRequest with model | Integration Test |
| 5.3-6 | Model Selection | Selection stored persistently | SharedPreferences pref_azure_model | Unit Test |
| 5.3-7 | Model Selection | Defaults to gpt-4.1 | defaultValue in preferences.xml | Manual |
| 5.4-1 | Dark Mode | All screens use dark theme | Material3 theme, values-night/themes.xml | Visual QA |
| 5.4-2 | Dark Mode | Material Design 3 compliance | Dark color palette definitions | Visual QA |
| 5.4-3 | Dark Mode | Proper text contrast ratios | Color definitions, Accessibility Scanner | Tool-based |
| 5.4-4 | Dark Mode | Camera maintains natural colors | Camera preview not inverted | Manual |
| 5.4-5 | Dark Mode | Dark notifications | Notification styling | Manual |
| 5.4-6 | Dark Mode | Respects system setting | AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM | Manual |
| 5.4-7 | Dark Mode | In-app theme selector | ListPreference pref_theme_mode | Manual |
| 5.4-8 | Dark Mode | Theme persists across restarts | SharedPreferences + FoodieApplication | Manual |
| 5.5-1 | Accessibility | Content descriptions present | contentDescription attributes | TalkBack Test |
| 5.5-2 | Accessibility | TalkBack support | Screen reader navigation | Manual |
| 5.5-3 | Accessibility | 48dp touch targets minimum | Layout constraints, Modifier.size() | Tool-based |
| 5.5-4 | Accessibility | Text scales with system font | sp units for text sizes | Manual |
| 5.5-5 | Accessibility | WCAG AA contrast ratios | Color values, Accessibility Scanner | Tool-based |
| 5.5-6 | Accessibility | No color-only information | Visual + text labels | Visual QA |
| 5.5-7 | Accessibility | Logical focus order | Tab navigation testing | Manual |
| 5.5-8 | Accessibility | Camera button TalkBack | contentDescription on camera trigger | TalkBack Test |
| 5.5-9 | Accessibility | Zero critical scanner issues | Accessibility Scanner report | Tool-based |
| 5.6-1 | Performance | 60fps screen transitions | GPU Rendering Profile | Tool-based |
| 5.6-2 | Performance | Smooth list scrolling | RecyclerView performance | Manual |
| 5.6-3 | Performance | Efficient image caching | Coil caching configuration | Code Review |
| 5.6-4 | Performance | Memory < 100MB typical | Android Profiler | Tool-based |
| 5.6-5 | Performance | Cold launch ≤ 2s | adb shell am start -W timing | Tool-based |
| 5.6-6 | Performance | Minimal battery impact | Battery Historian | Tool-based |
| 5.6-7 | Performance | APK size < 10MB | ProGuard enabled, resource shrinking | Build Output |
| 5.6-8 | Performance | No memory leaks | LeakCanary detection | Tool-based |
| 5.6-9 | Performance | Smooth animations | Fade/slide transitions | Visual QA |
| 5.7-1 | Onboarding | Welcome message on first launch | OnboardingActivity, SharedPreferences flag | Manual |
| 5.7-2 | Onboarding | Widget setup prompt | ViewPager2 screen 2 | Manual |
| 5.7-3 | Onboarding | Widget instructions clear | Text + visual guide | Visual QA |
| 5.7-4 | Onboarding | HC permissions with rationale | HealthConnectClient.requestPermissions() | Manual |
| 5.7-5 | Onboarding | Settings prompt for API config | Navigation to SettingsScreen | Manual |
| 5.7-6 | Onboarding | Completes in < 2 minutes | Timed user flow | Manual |
| 5.7-7 | Onboarding | Skip option available | Skip button on each screen | Manual |
| 5.7-8 | Onboarding | Shows only once | onboarding_completed flag | Manual |
| 5.7-9 | Onboarding | Ends at MealListScreen | Navigation after completion | Manual |
| 5.7-10 | Onboarding | ViewPager2 with 3-4 screens | OnboardingActivity layout | Code Review |
| 5.8-1 | Integration Test | End-to-end on physical device | Full capture → edit → delete flow | Manual |
| 5.8-2 | Integration Test | All stories verified | Test checklist completion | Manual |
| 5.8-3 | Integration Test | Edge cases tested | Network off, permissions denied, low storage | Manual |
| 5.8-4 | Integration Test | Performance targets met | Timed measurements | Manual |
| 5.8-5 | Integration Test | No critical bugs | Issue tracker review | Manual |
| 5.8-6 | Integration Test | Known issues documented | README or KNOWN_ISSUES.md | Documentation |
| 5.8-7 | Integration Test | 2+ devices tested | Samsung A53, Pixel 7, etc. | Manual |
| 5.8-8 | Integration Test | Multiple Android versions | API 28 (min), API 35 (target) | Manual |
| 5.8-9 | Integration Test | Battery acceptable | 3-5 captures/day usage | Manual |
| 5.8-10 | Integration Test | 33 stories test checklist | Complete test coverage matrix | Manual |

## Risks, Assumptions, Open Questions

**Risks:**

**R1: EncryptedSharedPreferences Keystore Failures (Medium)**
- Risk: Android Keystore can fail on some devices due to manufacturer bugs or lock screen security changes
- Impact: API key encryption/decryption fails, user cannot configure app
- Mitigation: Catch exceptions, prompt user to re-enter credentials, log error for debugging
- Fallback: Document manual workaround (clear app data, re-enter credentials)
- Likelihood: Low-Medium (known issue on some Samsung/Huawei devices)

**R2: Theme Switching Performance on Low-End Devices (Low)**
- Risk: Activity recreation during theme switch may feel slow on budget devices
- Impact: User experience degradation, perceived as "buggy"
- Mitigation: Test on mid-range devices (Samsung A53), optimize activity recreation flow
- Acceptance: Some delay acceptable if < 500ms, clearly visible as intentional theme change
- Likelihood: Low (modern devices handle recreation well)

**R3: Migration Failure from BuildConfig (Low)**
- Risk: Automatic migration from BuildConfig to EncryptedSharedPreferences could fail
- Impact: User loses API credentials, must manually re-enter in Settings
- Mitigation: Defensive coding with try-catch, log failures, show notification prompting manual setup
- Recovery: User can manually configure in Settings (no data loss, just inconvenience)
- Likelihood: Low (simple data copy operation)

**R4: Onboarding Abandonment (Medium)**
- Risk: Users skip onboarding and don't configure API credentials
- Impact: App unusable without API key (cannot analyze meals)
- Mitigation: Allow skipping but show prominent "Configure Settings" message on first capture attempt
- Design: Keep onboarding minimal (< 2 minutes) to reduce abandonment
- Likelihood: Medium (users often skip onboarding flows)

**R5: Test Connection False Negatives (Low)**
- Risk: Test connection fails due to transient network issues, user thinks credentials are wrong
- Impact: User frustration, confusion about whether credentials are correct
- Mitigation: Clear error messages distinguishing network vs auth errors (reuse Epic 4 ErrorHandler)
- UX: Provide "Retry" option, explain "Check your internet connection" vs "Invalid API key"
- Likelihood: Low (ErrorHandler already classifies errors correctly)

**R6: Dark Mode Contrast Issues (Low)**
- Risk: Some color combinations may not meet WCAG AA standards in dark mode
- Impact: Accessibility Scanner flags issues, text hard to read for some users
- Mitigation: Test with Accessibility Scanner, iterate on color palette before release
- Tools: Use Material Design 3 recommended dark colors (already validated)
- Likelihood: Low (Material 3 provides compliant defaults)

**Assumptions:**

**A1: BuildConfig Migration Timing**
- Assumption: All users will launch Story 5.2 build before credentials expire
- Validation: Migration happens automatically on first launch, no user action required
- Risk if false: User must manually enter credentials (minor inconvenience)

**A2: Android Keystore Availability**
- Assumption: Android Keystore available on all devices running API 28+ (Android 9+)
- Validation: Keystore introduced in API 18, stable and widely supported
- Risk if false: EncryptedSharedPreferences fallback to software encryption (less secure but functional)

**A3: PreferenceScreen Framework Adequacy**
- Assumption: Android PreferenceScreen sufficient for Settings UI needs (no custom fragments required)
- Validation: PreferenceFragmentCompat provides all needed preference types (EditText, List, etc.)
- Risk if false: May need custom Compose-based settings screen (more implementation effort)

**A4: Theme Recreation Performance**
- Assumption: Activity recreation for theme switching acceptable to users (< 500ms)
- Validation: Standard Android pattern, users familiar with brief recreation delay
- Risk if false: May need to implement custom theme switching without recreation (complex)

**A5: Single Settings Screen Sufficient**
- Assumption: One settings screen with categories sufficient (no nested screens needed)
- Validation: MVP has limited settings (API config + theme), fits on one scrollable screen
- Risk if false: V2.0 may need hierarchical settings navigation

**A6: Onboarding Skip Acceptable**
- Assumption: Users who skip onboarding can figure out widget setup and API configuration
- Validation: Settings screen accessible from obvious menu location, widget setup documented
- Risk if false: May need persistent banner prompting setup until completed

**Open Questions:**

**Q1: Should API key be visible on demand? (Low Priority)**
- Question: Should Settings allow temporary API key reveal (show/hide toggle)?
- Current Design: Always masked except last 4 characters
- Consideration: Useful for debugging, but increases security risk if shoulder-surfing
- Decision Needed: Story 5.2 implementation
- Recommendation: Keep fully masked for V1.0, add reveal option if users request it

**Q2: Should onboarding be re-triggerable? (Low Priority)**
- Question: Should Settings include "Show Onboarding Again" option?
- Current Design: One-time on first launch, never repeats
- Consideration: Useful if user wants to review setup steps
- Decision Needed: Story 5.7 implementation
- Recommendation: Skip for V1.0, add if users request help content

**Q3: Should theme follow system automatically or require manual selection? (Medium Priority)**
- Question: Should default be "System Default" (follows Android setting) or "Light" (fixed)?
- Current Design: System Default as default
- Consideration: System Default is modern Android pattern, but some users prefer manual control
- Decision Needed: Story 5.4 implementation
- Recommendation: Default to System Default, allow manual override in Settings

**Q4: Should Settings screen be Compose or PreferenceFragment? (High Priority)**
- Question: Use traditional PreferenceFragmentCompat or build custom Compose settings screen?
- Current Design: PreferenceFragmentCompat assumed in spec
- Consideration: PreferenceFragment is simpler, Compose gives more control and consistency
- Decision Needed: Story 5.1 start
- Recommendation: Start with PreferenceFragmentCompat for speed, migrate to Compose if needed

**Q5: What happens if test connection succeeds but later API calls fail? (Low Priority)**
- Question: How to handle scenario where credentials work initially but later fail (key rotation, billing issue)?
- Current Design: Test connection validates at setup time only
- Consideration: Could add periodic validation or "Test Again" button in Settings
- Decision Needed: V2.0 (not blocking for MVP)
- Recommendation: Rely on Epic 4 error handling during actual usage, show actionable errors

**Q6: Should migration from BuildConfig be automatic or require user confirmation? (Medium Priority)**
- Question: Auto-migrate credentials silently or show dialog explaining migration?
- Current Design: Automatic silent migration on first launch
- Consideration: Silent is simpler UX, but transparency might build trust
- Decision Needed: Story 5.2 implementation
- Recommendation: Silent auto-migration, log success/failure for debugging

## Test Strategy Summary

**Test Levels:**

**Unit Tests:**
- SecurePreferencesManager: Encryption/decryption, key storage, exception handling
- ApiConfiguration.validate(): All validation rules (HTTPS, endpoint format, non-empty fields)
- ThemeMode enum: Value conversions, defaults
- PreferencesRepository: Save/load operations, Flow emissions
- Migration logic: BuildConfig → EncryptedSharedPreferences transformation

**Integration Tests:**
- SettingsViewModel + PreferencesRepository: State management, test connection flow
- AuthInterceptor + SecurePreferencesManager: API key retrieval and header injection
- Test connection API call: End-to-end validation with Azure OpenAI (mock server)
- Theme switching: SharedPreferences → AppCompatDelegate → Activity recreation
- Onboarding flow: ViewPager2 navigation, permission requests, flag persistence

**UI Tests (Compose):**
- SettingsScreen rendering: All preference categories visible
- Masked API key input: Displays dots, preserves last 4 characters
- Test connection button: Loading state, success/failure toast display
- Theme selector: Options displayed, selection triggers recreation
- Onboarding ViewPager: Swipe navigation, skip button, completion flow

**Manual Tests:**
- End-to-end Settings configuration on physical device
- Dark mode visual regression: All screens in light and dark themes
- TalkBack navigation: Complete app flow with screen reader
- Accessibility Scanner: Zero critical issues reported
- Performance profiling: Launch times, memory usage, frame rates
- Multi-device testing: Samsung A53, Pixel 7, different manufacturers
- Android version testing: API 28 (min), API 35 (target)

**Test Cases by Story:**

**Story 5.1 - Settings Screen Foundation:**
```
Test Case: Settings screen accessible from toolbar
- Given: User on MealListScreen
- When: User taps toolbar menu → Settings
- Then: SettingsScreen displays with preference categories
- Verify: Back navigation returns to MealListScreen

Test Case: Settings persist across restarts
- Given: User changes any setting
- When: User force-stops app and relaunches
- Then: Setting value preserved (read from SharedPreferences)
```

**Story 5.2 - API Configuration:**
```
Test Case: API key encrypted in Keystore
- Given: User enters API key "sk-test123"
- When: SettingsViewModel saves configuration
- Then: EncryptedSharedPreferences stores encrypted value
- Verify: Raw SharedPreferences file does not contain plaintext key

Test Case: Test connection success
- Given: Valid API key, endpoint, model entered
- When: User taps "Test Connection"
- Then: Minimal Responses API request sent
- And: Response status "completed" parsed
- And: Toast "API configuration valid ✓" displayed

Test Case: Test connection failure - invalid key
- Given: Invalid API key entered
- When: User taps "Test Connection"
- Then: API returns 401 error
- And: ErrorHandler classifies as AuthError
- And: Toast "Invalid API key - check your credentials" displayed

Test Case: Automatic migration from BuildConfig
- Given: First launch of Story 5.2 build
- And: BuildConfig contains credentials from Story 2.4
- When: FoodieApplication.onCreate() executes
- Then: Credentials migrated to EncryptedSharedPreferences
- And: Migration flag set to prevent re-execution
- Verify: AuthInterceptor reads from SecurePreferencesManager
```

**Story 5.3 - Model Selection:**
```
Test Case: Model defaults to gpt-4.1
- Given: New installation (no saved preferences)
- When: User opens Settings
- Then: Model field shows "gpt-4.1"

Test Case: Custom model name supported
- Given: User enters custom deployment "my-gpt4-deployment"
- When: User saves and makes API call
- Then: Request body contains model: "my-gpt4-deployment"
```

**Story 5.4 - Dark Mode:**
```
Test Case: Dark theme applied system-wide
- Given: User selects "Dark" in Settings
- When: Activity recreates
- Then: All screens render with dark color palette
- Verify: MealListScreen, SettingsScreen, EditScreen all dark

Test Case: Theme persists across restarts
- Given: User selects "Dark" theme
- When: User force-stops app and relaunches
- Then: Dark theme applied at startup (FoodieApplication)

Test Case: Camera preview not inverted
- Given: Dark theme enabled
- When: User opens camera for meal capture
- Then: Camera preview shows natural colors (not dark-inverted)
```

**Story 5.5 - Accessibility:**
```
Test Case: TalkBack navigation functional
- Given: TalkBack enabled
- When: User navigates MealListScreen
- Then: Each meal entry announced with full details
- And: Focus order logical (top to bottom)

Test Case: Touch targets meet 48dp minimum
- Given: Settings screen rendered
- When: Developer enables layout bounds
- Then: All interactive elements ≥ 48dp x 48dp
- Tools: Manual inspection or automated test

Test Case: Accessibility Scanner passes
- Given: All screens implemented
- When: Run Accessibility Scanner tool
- Then: Zero critical issues reported
- And: All contrast ratio warnings resolved
```

**Story 5.6 - Performance:**
```
Test Case: Cold launch ≤ 2 seconds
- Given: App not running (force-stopped)
- When: User taps app icon
- Then: MealListScreen usable within 2 seconds
- Measure: adb shell am start -W com.foodie.app

Test Case: Screen transitions 60fps
- Given: User navigates between screens
- When: Monitor GPU Rendering Profile
- Then: Green bars consistently below 16ms line
- And: No red spikes indicating dropped frames

Test Case: APK size < 10MB
- Given: Release build with ProGuard enabled
- When: Build APK
- Then: app-release.apk file size < 10MB
- Verify: Resource shrinking removed unused assets
```

**Story 5.7 - Onboarding:**
```
Test Case: Onboarding shows on first launch only
- Given: Fresh install (no SharedPreferences)
- When: User launches app
- Then: OnboardingActivity displays
- And: After completion, flag "onboarding_completed" set
- When: User relaunches app
- Then: MealListScreen displays directly (no onboarding)

Test Case: Skip functionality works
- Given: User on onboarding screen 2
- When: User taps "Skip"
- Then: Navigate directly to MealListScreen
- And: Onboarding flag still set (don't show again)
```

**Story 5.8 - Integration Testing:**
```
Test Case: Complete end-to-end flow
1. Fresh install → Onboarding displays
2. Skip onboarding → MealListScreen
3. Navigate to Settings → Configure API credentials
4. Test connection → Success
5. Return to MealList → Tap widget
6. Capture meal photo → Background analysis
7. View meal in list → Edit calories
8. Save edit → Verify Health Connect updated
9. Delete meal → Verify removed from HC
10. Enable dark mode → All screens dark-themed
11. Force-stop app → Relaunch
12. Verify: Settings preserved, theme applied, no onboarding

Test Case: Edge case - no network during test connection
- Given: Airplane mode enabled
- When: User attempts test connection
- Then: Network error detected immediately
- And: Toast "Check your internet connection" displayed
- And: No hanging progress indicator

Test Case: Edge case - API key decryption fails
- Given: Android Keystore corruption (simulated)
- When: App attempts to read API key
- Then: Exception caught gracefully
- And: User prompted to re-enter credentials
- And: Error logged for debugging
```

**Test Tools:**
- JUnit 4 + Mockito for unit tests
- Compose UI Test framework for Composable tests
- Espresso for Activity/Fragment tests
- MockWebServer for API mocking
- Android Profiler for performance analysis
- Accessibility Scanner for a11y validation
- LeakCanary for memory leak detection
- GPU Rendering Profile for frame rate monitoring
- TalkBack for screen reader testing

**Test Coverage Goals:**
- Unit test coverage: > 80% for domain and data layers
- Integration test coverage: All critical user flows (Settings config, theme switch, onboarding)
- Manual test coverage: 100% of acceptance criteria verified on physical devices
- Accessibility: Zero critical issues from Accessibility Scanner
- Performance: All NFR targets met on mid-range device

**Test Execution Strategy:**
- Unit tests: Run on every commit (CI pipeline)
- Integration tests: Run before PR merge
- Manual tests: Execute before epic completion
- Regression tests: Re-run all tests after Story 5.8 bug fixes
- Device testing: Test on 2+ physical devices with different Android versions

**Pass Criteria:**
- All unit tests passing (100% pass rate)
- All integration tests passing
- All acceptance criteria verified manually
- Zero critical bugs in issue tracker
- Performance targets met on Samsung A53 or equivalent
- Accessibility Scanner reports zero critical issues
- No memory leaks detected by LeakCanary
- User can complete full capture → edit → delete flow on physical device
