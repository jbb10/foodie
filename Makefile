# Foodie App - Makefile
# Convenient commands for building, deploying, and testing the Android app

.PHONY: help install-debug install-release test-unit test-instrumentation test-all clean sonar coverage lint lint-fix format format-check detekt android-lint run-emulator

# Default target - show available commands
help:
	@echo "Foodie App - Available Commands:"
	@echo ""
	@echo "Build & Install:"
	@echo "  make install-debug        - Build and install debug version to connected device"
	@echo "  make install-release      - Build and install release version to connected device"
	@echo ""
	@echo "Testing:"
	@echo "  make test-unit            - Run all unit tests"
	@echo "  make test-instrumentation - Run all instrumentation tests (requires device/emulator)"
	@echo "  make test-e2e             - Run Maestro E2E tests (emulator only)"
	@echo "  make test-all             - Run all tests (unit + instrumentation)"
	@echo "  make coverage             - Generate coverage reports for all tests"
	@echo ""
	@echo "Code Quality & Linting:"
	@echo "  make lint                 - Run all linters and formatters (check mode)"
	@echo "  make lint-fix             - Run all linters with auto-fix enabled"
	@echo "  make format               - Auto-format code with Spotless"
	@echo "  make format-check         - Check code formatting without changes"
	@echo "  make detekt               - Run Detekt Kotlin static analysis"
	@echo "  make android-lint         - Run Android Lint checks"
	@echo ""
	@echo "Analysis & Reporting:"
	@echo "  make sonar                - Run tests, generate coverage, and upload to SonarQube"
	@echo ""
	@echo "Utilities:"
	@echo "  make clean                - Clean build artifacts"
	@echo "  make run-emulator         - Start the Android emulator"
	@echo ""

# Build and install debug version
install-debug:
	cd app && ./gradlew installDebug

# Build and install release version
install-release:
	cd app && ./gradlew installRelease

# Run all unit tests
test-unit:
	cd app && ./gradlew :app:testDebugUnitTest

# Run all instrumentation tests (emulator only)
test-instrumentation:
	@EMULATOR_ID=$$(adb devices | grep "emulator-" | head -1 | awk '{print $$1}'); \
	cd app && ANDROID_SERIAL=$$EMULATOR_ID ./gradlew :app:connectedDebugAndroidTest

# Run Maestro E2E tests (emulator only)
test-e2e:
	@EMULATOR_ID=$$(adb devices | grep "emulator-" | head -1 | awk '{print $$1}'); \
	maestro test --device $$EMULATOR_ID .maestro/

# Run all tests (unit + instrumentation)
test-all: test-unit test-instrumentation

# Clean build artifacts
clean:
	cd app && ./gradlew clean

# Start the Android emulator
run-emulator:
	@AVD_NAME=$$(emulator -list-avds | head -1); \
	if [ -z "$$AVD_NAME" ]; then \
		echo "Error: No Android Virtual Device (AVD) found."; \
		echo "Please create an AVD using Android Studio or avdmanager."; \
		exit 1; \
	fi; \
	echo "Starting emulator: $$AVD_NAME"; \
	emulator -avd $$AVD_NAME &

# Generate code coverage report
coverage: test-unit test-instrumentation
	cd app && ./gradlew jacocoTestReport
	@EMULATOR_ID=$$(adb devices | grep "emulator-" | head -1 | awk '{print $$1}'); \
	cd app && ANDROID_SERIAL=$$EMULATOR_ID ./gradlew createDebugAndroidTestCoverageReport

# Run SonarQube analysis with code coverage
sonar: coverage
	sonar-scanner

# === Code Quality & Linting Targets ===

# Run all linters in check mode (no auto-fix)
lint: format-check detekt android-lint

# Run all linters with auto-fix enabled
lint-fix: format detekt android-lint

# Auto-format code with Spotless
format:
	cd app && ./gradlew spotlessApply

# Check code formatting without making changes
format-check:
	cd app && ./gradlew spotlessCheck

# Run Detekt Kotlin static analysis
detekt:
	cd app && ./gradlew detekt

# Run Android Lint checks
android-lint:
	cd app && ./gradlew :app:lintDebug
