#!/bin/bash
# Pull Foodie app logs from connected device
# Usage:
#   ./scripts/pull-logs.sh              - Pull logcat buffer (last few minutes)
#   ./scripts/pull-logs.sh --errors-only - Pull only errors/warnings from logcat
#   ./scripts/pull-logs.sh --app-file    - Pull persistent app log file (days/weeks of logs)

set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="logs"
mkdir -p "$OUTPUT_DIR"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "Error: No device connected. Please connect your phone via USB or wireless ADB."
    exit 1
fi

# Get device serial (use first connected device if multiple)
DEVICE=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
echo "Pulling logs from device: $DEVICE"

if [ "${1:-}" = "--app-file" ]; then
    # Pull persistent log file from app's private storage
    echo "Pulling persistent app log file..."
    APP_LOG_PATH="/data/data/com.foodie.app/files/foodie_logs.txt"
    LOG_FILE="$OUTPUT_DIR/foodie_persistent_${TIMESTAMP}.txt"

    if adb -s "$DEVICE" shell "test -f $APP_LOG_PATH && echo exists" | grep -q "exists"; then
        adb -s "$DEVICE" pull "$APP_LOG_PATH" "$LOG_FILE" 2>/dev/null

        if [ -f "$LOG_FILE" ]; then
            FILE_SIZE=$(du -h "$LOG_FILE" | cut -f1)
            LINE_COUNT=$(wc -l < "$LOG_FILE" | xargs)

            echo ""
            echo "Persistent log file pulled successfully!"
            echo "  File size: $FILE_SIZE"
            echo "  Total lines: $LINE_COUNT"
            echo "  Errors: $(grep -c " E/" "$LOG_FILE" || echo 0)"
            echo "  Warnings: $(grep -c " W/" "$LOG_FILE" || echo 0)"
            echo ""
            echo "Log file: $LOG_FILE"
            echo ""
            echo "This file contains logs from when the app was running, even if days ago."
        else
            echo "Error: Failed to pull log file from device"
            exit 1
        fi
    else
        echo "Error: Persistent log file not found on device."
        echo "Make sure the app has been run at least once with FileLoggingTree enabled."
        exit 1
    fi

elif [ "${1:-}" = "--errors-only" ]; then
    # Pull only warnings, errors, and fatals from logcat
    echo "Extracting errors and warnings from logcat..."
    adb -s "$DEVICE" logcat -d -v threadtime | \
      grep -E "foodie|Foodie|EnergyBalance|HealthConnect|AnalyzeMeal|NutritionAnalysis" | \
      grep -E " W | E | F " \
      > "$OUTPUT_DIR/foodie_errors_${TIMESTAMP}.log"

    LOG_FILE="$OUTPUT_DIR/foodie_errors_${TIMESTAMP}.log"
    echo "Errors saved to: $LOG_FILE"

    # Print summary
    LINE_COUNT=$(wc -l < "$LOG_FILE" | xargs)
    echo ""
    echo "Summary:"
    echo "  Total lines: $LINE_COUNT"
    echo "  Errors: $(grep -c " E " "$LOG_FILE" || echo 0)"
    echo "  Warnings: $(grep -c " W " "$LOG_FILE" || echo 0)"
    echo ""
    echo "Log file: $LOG_FILE"

else
    # Pull all Foodie-related logs from logcat (default)
    echo "Extracting all Foodie logs from logcat buffer..."
    adb -s "$DEVICE" logcat -d -v threadtime | \
      grep -E "foodie|Foodie|EnergyBalance|HealthConnect|AnalyzeMeal|NutritionAnalysis|WorkManager" \
      > "$OUTPUT_DIR/foodie_${TIMESTAMP}.log"

    LOG_FILE="$OUTPUT_DIR/foodie_${TIMESTAMP}.log"
    echo "Logs saved to: $LOG_FILE"

    # Print summary
    LINE_COUNT=$(wc -l < "$LOG_FILE" | xargs)
    echo ""
    echo "Summary:"
    echo "  Total lines: $LINE_COUNT"
    echo "  Errors: $(grep -c " E " "$LOG_FILE" || echo 0)"
    echo "  Warnings: $(grep -c " W " "$LOG_FILE" || echo 0)"
    echo ""
    echo "Log file: $LOG_FILE"
fi
