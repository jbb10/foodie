# Foodie App - Makefile
# Convenient commands for building, deploying, and testing the Android app

.PHONY: help install-debug install-release test-unit test-instrumentation test-all clean sonar coverage lint lint-fix format format-check detekt android-lint

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
	@echo ""

# Build and install debug version
install-debug:
	@echo "Building and installing debug version..."
	cd app && ./gradlew installDebug

# Build and install release version
install-release:
	@echo "Building and installing release version..."
	cd app && ./gradlew installRelease

# Run all unit tests
test-unit:
	@echo "Running all unit tests..."
	cd app && ./gradlew :app:testDebugUnitTest

# Run all instrumentation tests (requires connected device or emulator)
test-instrumentation:
	@echo "Running all instrumentation tests..."
	@echo "Make sure a device or emulator is connected (run 'adb devices' to check)"
	cd app && ./gradlew :app:connectedDebugAndroidTest

# Run all tests (unit + instrumentation)
test-all: test-unit test-instrumentation

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	cd app && ./gradlew clean

# Generate code coverage report
coverage: test-unit test-instrumentation
	@echo "Generating unit test coverage report..."
	cd app && ./gradlew jacocoTestReport
	@echo "Generating instrumentation test coverage report..."
	cd app && ./gradlew createDebugAndroidTestCoverageReport
	@echo "Coverage reports generated:"
	@echo "  - Unit tests: app/app/build/reports/jacoco/jacocoTestReport/html/index.html"
	@echo "  - Instrumentation tests: app/app/build/reports/coverage/androidTest/debug/connected/index.html"

# Run SonarQube analysis with code coverage
sonar: coverage
	@echo "Running SonarQube analysis..."
	sonar-scanner
	@echo "SonarQube analysis complete. View results at: http://localhost:9000/dashboard?id=Foodie"

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
