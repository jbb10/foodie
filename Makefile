# Foodie App - Makefile
# Convenient commands for building, deploying, and testing the Android app

.PHONY: help install-debug install-release test-unit test-instrumentation test-all clean

# Default target - show available commands
help:
	@echo "Foodie App - Available Commands:"
	@echo ""
	@echo "  make install-debug        - Build and install debug version to connected device"
	@echo "  make install-release      - Build and install release version to connected device"
	@echo "  make test-unit            - Run all unit tests"
	@echo "  make test-instrumentation - Run all instrumentation tests (requires device/emulator)"
	@echo "  make test-all             - Run all tests (unit + instrumentation)"
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
test-all:
	@echo "Running all tests (unit + instrumentation)..."
	@echo "Make sure a device or emulator is connected (run 'adb devices' to check)"
	cd app && ./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	cd app && ./gradlew clean
