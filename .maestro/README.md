# Maestro E2E Tests for Foodie

This directory contains end-to-end tests for the Foodie Android app using Maestro.

## Installation

```bash
# Install Maestro CLI
curl -Ls "https://get.maestro.mobile.dev" | bash

# Verify installation
maestro --version
```

## Running Tests

**⚠️ IMPORTANT: Always use the Makefile target to run E2E tests:**

```bash
make test-e2e
```

**This ensures tests only run on the emulator, never on your physical device.**

The Makefile target includes safety checks:
- ✅ Verifies an emulator is running
- ✅ Fails if physical devices are connected  
- ✅ Automatically selects the emulator
- ✅ Runs all tests in `.maestro/`

### Manual Execution (Use with Caution)

If you must run manually, **always** specify the device:

```bash
# Start emulator first
emulator -avd Pixel_8_Pro &
adb wait-for-device

# Install latest debug APK
make install-debug

# Get emulator device ID
adb devices

# Run tests ONLY on emulator (specify device!)
maestro test --device emulator-5554 .maestro/

# Run specific test
maestro test --device emulator-5554 .maestro/09-historical-day-navigation.yaml
```

**❌ NEVER run `maestro test .maestro/` without `--device` flag if you have a phone connected!**

## Test Structure

Tests are numbered by priority:
- `01-xx` - P0 Critical paths (must pass before release)
- `02-xx` - P1 Core features (should pass before release)
- `03-xx` - P2 Secondary features (nice to have)

## Prerequisites

**Before running tests:**

1. **Install Health Connect** on emulator/device:
   ```bash
   adb install /path/to/HealthConnect.apk
   ```

2. **Grant Health Connect permissions:**
   - Open app → Grant READ_NUTRITION and WRITE_NUTRITION permissions
   - Or run once manually to trigger permission flow

3. **Configure Azure OpenAI API** (for capture flow tests):
   - Settings → API Configuration
   - Enter valid API key and endpoint
   - Tap "Test Connection" to verify

4. **Add widget to home screen** (for widget tests):
   - Long-press home screen → Widgets → Foodie
   - Or skip widget tests and start from app launcher

## Known Limitations

- **System camera**: Maestro cannot control actual camera hardware. Tests use mock photos or skip actual photo capture.
- **Background processing**: Tests use `waitForAnimationToEnd` with 20s timeout for WorkManager processing.
- **Health Connect verification**: Cannot directly query HC from Maestro. Tests verify data appears in app's meal list.

## Test Coverage

| Test File | Story | Priority | Description |
|-----------|-------|----------|-------------|
| `01-complete-capture-flow.yaml` | 2.7 | P0 | Widget → Camera → Photo → Analysis → HC save |
| `02-edit-meal-flow.yaml` | 3.2/3.3 | P1 | View meal → Edit → Save → HC updated |
| `03-delete-meal-flow.yaml` | 3.4 | P1 | View meal → Delete → Confirm → HC removed |
| `04-permission-denial-recovery.yaml` | 4.5 | P1 | Permission denied → Dialog → Settings → Grant → Retry |
| `05-api-configuration.yaml` | 5.2/5.3 | P1 | Settings → Enter API key → Test → Save |
| `06-dashboard-data-accuracy.yaml` | 6.6 | P1 | Profile + Steps + Meals → Dashboard shows TDEE/deficit |
| `07-onboarding-first-launch.yaml` | 5.7 | P2 | Fresh install → Onboarding → Permissions → Ready |
| `08-dark-mode-persistence.yaml` | 5.4 | P2 | Change theme → Restart app → Theme preserved |
| `09-historical-day-navigation.yaml` | 6.7 | P2 | Dashboard → Previous day → Historical data |
| `10-api-error-retry.yaml` | 4.3 | P1 | API fails → Notification → Tap retry → Success |

## Troubleshooting

**Test fails to find element:**
- Check element text/id hasn't changed
- Use `maestro studio` to inspect current screen
- Add `optional: true` if element may not appear

**Test times out:**
- Increase `timeout` value (milliseconds)
- Check if background job is actually completing
- Use `adb logcat` to debug app issues

**Permission dialogs:**
- Use `optional: true` for permission buttons (may already be granted)
- Grant permissions manually before running tests

**Camera issues:**
- Most tests skip actual photo capture (system camera can't be controlled)
- Use test mode or mock photos in app for E2E validation

## CI/CD Integration (Future)

```yaml
# .github/workflows/e2e.yml
name: E2E Tests
on: [pull_request]
jobs:
  maestro:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: mobile-dev-inc/action-maestro-cloud@v1
        with:
          api-key: ${{ secrets.MAESTRO_CLOUD_API_KEY }}
          app-file: app/build/outputs/apk/debug/app-debug.apk
```
