#!/bin/zsh
# Foodie Manual Test Log Collection Script
# Usage: ./collect_test_logs.sh [test_name]
#
# Collects logs from connected Android device during manual testing.
# Press Ctrl+C when test is complete to save logs.

# Clear existing logs
adb logcat -c

# Generate log filename with timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
TEST_NAME=${1:-"manual_test"}
LOG_FILE="test_logs_${TEST_NAME}_${TIMESTAMP}.txt"

echo "📝 Foodie Manual Test Logger"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📁 Log file: $LOG_FILE"
echo "🎯 Perform your test now. Press Ctrl+C when done."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Collect logs with relevant tags and timestamp
# -v time: Include timestamps
# Tag filters: Show DEBUG and above for our components, ERROR only for system
adb logcat -v time \
  CapturePhotoViewModel:D \
  CapturePhotoScreen:D \
  AnalyzeMealWorker:D \
  MealAnalysisForegroundNotifier:D \
  MealAnalysisNotificationSpec:D \
  NotificationPermissionManager:D \
  PhotoManager:D \
  NutritionAnalysisRepo:D \
  HealthConnectManager:D \
  MainActivity:D \
  WorkManager:I \
  '*:E' \
  | tee "$LOG_FILE"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Logs saved to: $LOG_FILE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
