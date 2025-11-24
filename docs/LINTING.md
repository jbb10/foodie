# Code Quality & Linting Setup

This project uses a comprehensive linting and code quality toolchain to maintain high code standards.

## Tools Included

### 1. **Android Lint** (Built-in)
- Native Android linting tool for catching Android-specific issues
- Checks for API usage, resource issues, performance problems, and more
- Configuration: `app/app/build.gradle.kts` (lint block)
- Baseline: `app/app/lint-baseline.xml`

### 2. **Detekt** 
- Kotlin static code analysis tool
- Detects code smells, complexity issues, and potential bugs
- Configuration: `app/config/detekt/detekt.yml`
- Baseline: `app/config/detekt/baseline.xml`
- Auto-correct capability enabled

### 3. **ktlint** (via Spotless)
- Kotlin code style checker and formatter
- Enforces consistent code formatting
- Configuration: `.editorconfig` + Spotless configuration
- Auto-fix capability

### 4. **Spotless**
- Multi-language code formatter
- Formats Kotlin, Gradle, and XML files
- Integrates ktlint for Kotlin formatting
- Can auto-fix formatting issues

## Quick Start

### Run All Linters (Check Mode)
```bash
make lint
# or
make lint-check
```

### Auto-Fix All Issues
```bash
make lint-fix
```

### Run Individual Tools

#### Format Code (Auto-fix)
```bash
make format
cd app && ./gradlew spotlessApply
```

#### Check Formatting (No changes)
```bash
make format-check
cd app && ./gradlew spotlessCheck
```

#### Detekt Static Analysis
```bash
make detekt
cd app && ./gradlew detekt
```

#### Android Lint
```bash
make android-lint
cd app && ./gradlew :app:lintDebug
```

#### ktlint Check
```bash
make ktlint
cd app && ./gradlew spotlessKotlinCheck
```

## Reports

After running the linters, view detailed reports at:

- **Detekt**: `app/build/reports/detekt/detekt.html`
- **Android Lint**: `app/app/build/reports/lint/lint-report.html`
- **Android Lint (XML)**: `app/app/build/reports/lint/lint-report.xml`
- **Android Lint (SARIF)**: `app/app/build/reports/lint/lint-report.sarif`

## IDE Integration

### IntelliJ IDEA / Android Studio

1. **Install EditorConfig Plugin** (usually built-in)
   - Automatically picks up `.editorconfig` settings

2. **Enable Detekt Plugin** (optional)
   - File → Settings → Plugins → Search "Detekt"
   - Install and restart IDE
   - Configure to use `app/config/detekt/detekt.yml`

3. **Configure Code Style**
   - The `.editorconfig` file automatically configures Kotlin code style
   - Settings → Editor → Code Style → Kotlin → Set from → EditorConfig

4. **Format on Save** (optional)
   - Settings → Tools → Actions on Save
   - Enable "Reformat code"

## Git Hooks (Optional)

Install pre-commit hooks to run linters automatically before commits:

```bash
cp scripts/pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

This will run formatting and linting checks on staged files before allowing commits.

To bypass the hook (not recommended):
```bash
git commit --no-verify
```

## Baseline Files

Baseline files allow you to suppress existing issues while enforcing clean code for new changes:

### Android Lint Baseline
```bash
cd app && ./gradlew :app:lintDebug -PupdateLintBaseline
```

### Detekt Baseline
```bash
cd app && ./gradlew detektBaseline
```

## Configuration Files

| File | Purpose |
|------|---------|
| `.editorconfig` | Code style settings for ktlint and IDE |
| `app/config/detekt/detekt.yml` | Detekt analysis rules and configuration |
| `app/config/detekt/baseline.xml` | Detekt baseline for existing issues |
| `app/app/lint-baseline.xml` | Android Lint baseline for existing issues |
| `app/gradle/libs.versions.toml` | Version definitions for linting tools |
| `app/build.gradle.kts` | Root-level plugin configuration |
| `app/app/build.gradle.kts` | Android Lint configuration |

## Key Features

### Detekt
- ✅ Complexity analysis (cyclomatic complexity, method length, etc.)
- ✅ Code smell detection
- ✅ Performance checks
- ✅ Coroutine best practices
- ✅ Naming conventions
- ✅ Exception handling patterns
- ✅ Auto-correction enabled

### Android Lint
- ✅ API version compatibility checks
- ✅ Resource optimization
- ✅ Security vulnerability detection
- ✅ Performance optimization suggestions
- ✅ Accessibility checks
- ✅ Internationalization issues
- ✅ Multiple report formats (HTML, XML, SARIF, Text)

### ktlint (via Spotless)
- ✅ Consistent code formatting
- ✅ Import organization
- ✅ Trailing comma support
- ✅ Maximum line length enforcement (120 chars)
- ✅ Auto-fix capability

### Spotless
- ✅ Kotlin file formatting
- ✅ Gradle Kotlin DSL formatting
- ✅ XML formatting
- ✅ Consistent indentation

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Linters
  run: make lint

- name: Upload Lint Reports
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: lint-reports
    path: |
      app/build/reports/detekt/
      app/app/build/reports/lint/
```

### GitLab CI Example
```yaml
lint:
  stage: test
  script:
    - make lint
  artifacts:
    when: always
    paths:
      - app/build/reports/detekt/
      - app/app/build/reports/lint/
    reports:
      junit: app/build/reports/detekt/detekt.xml
```

## Troubleshooting

### "Task 'detekt' not found"
Run `cd app && ./gradlew --refresh-dependencies`

### "Spotless check failed"
Run `make format` to auto-fix formatting issues

### Too many lint errors
1. Generate a baseline: `cd app && ./gradlew :app:lintDebug -PupdateLintBaseline`
2. Fix new issues incrementally
3. Gradually reduce baseline

### Detekt takes too long
- Detekt runs in parallel by default
- Check `app/config/detekt/detekt.yml` for `parallel: true`
- Consider using baseline for incremental adoption

## Best Practices

1. **Run linters locally before pushing**
   ```bash
   make lint
   ```

2. **Auto-fix formatting regularly**
   ```bash
   make format
   ```

3. **Review lint reports** - Don't just dismiss warnings
   - Detekt: `app/build/reports/detekt/detekt.html`
   - Android Lint: `app/app/build/reports/lint/lint-report.html`

4. **Update baselines sparingly** - Baselines should shrink over time, not grow

5. **Configure IDE formatting** - Match project settings with IDE formatting

6. **Use pre-commit hooks** - Catch issues before they reach CI/CD

## Customization

### Adjust Detekt Rules
Edit `app/config/detekt/detekt.yml`:
```yaml
complexity:
  active: true
  LongMethod:
    active: true
    threshold: 60  # Adjust threshold
```

### Adjust ktlint Rules
Edit `.editorconfig`:
```ini
[*.{kt,kts}]
max_line_length = 120  # Adjust max line length
```

### Adjust Android Lint Rules
Edit `app/app/build.gradle.kts`:
```kotlin
lint {
    disable += listOf("SomeCheckId")
    error += listOf("CriticalCheckId")
}
```

## Further Reading

- [Detekt Documentation](https://detekt.dev/)
- [ktlint Documentation](https://pinterest.github.io/ktlint/)
- [Spotless Documentation](https://github.com/diffplug/spotless)
- [Android Lint Reference](https://developer.android.com/studio/write/lint)
- [EditorConfig](https://editorconfig.org/)
