#!/bin/bash
# Pre-commit hook for Foodie App
# Runs linting and formatting checks before allowing commits
# Install: cp scripts/pre-commit.sh .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit

set -e

echo "🔍 Running pre-commit checks..."
echo ""

# Get the list of staged Kotlin files
STAGED_KOTLIN_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.kt$' || true)
STAGED_GRADLE_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.gradle.kts$' || true)

# If no Kotlin or Gradle files are staged, skip checks
if [ -z "$STAGED_KOTLIN_FILES" ] && [ -z "$STAGED_GRADLE_FILES" ]; then
    echo "✓ No Kotlin or Gradle files to check"
    exit 0
fi

echo "Files to check:"
echo "$STAGED_KOTLIN_FILES"
echo "$STAGED_GRADLE_FILES"
echo ""

# Run Spotless formatting check
echo "📝 Checking code formatting with Spotless..."
cd app
if ! ./gradlew spotlessCheck --quiet; then
    echo ""
    echo "❌ Code formatting issues detected!"
    echo "Run 'make format' or 'cd app && ./gradlew spotlessApply' to auto-fix"
    echo "Then stage the changes and commit again."
    exit 1
fi
echo "✓ Code formatting check passed"
echo ""

# Run Detekt
echo "🔍 Running Detekt static analysis..."
if ! ./gradlew detekt --quiet; then
    echo ""
    echo "❌ Detekt found issues!"
    echo "Run 'make detekt' to see the full report"
    echo "View detailed report at: app/build/reports/detekt/detekt.html"
    exit 1
fi
echo "✓ Detekt check passed"
echo ""

# Run Android Lint (only on changed modules)
echo "🔍 Running Android Lint..."
if ! ./gradlew :app:lintDebug --quiet; then
    echo ""
    echo "❌ Android Lint found issues!"
    echo "Run 'make android-lint' to see the full report"
    echo "View detailed report at: app/app/build/reports/lint/lint-report.html"
    exit 1
fi
echo "✓ Android Lint check passed"
echo ""

echo "✅ All pre-commit checks passed! Committing..."
exit 0
