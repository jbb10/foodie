# Compose + Hilt Testing Guide

**Status**: Active  
**Last Updated**: 2025-11-09  
**Story**: 2-1-fix-hilt-compose-test-infrastructure

---

## Overview

This guide documents the correct patterns for writing instrumentation tests for Jetpack Compose screens that use Hilt ViewModels in the Foodie app.

## The Problem

When testing Compose screens that use `hiltViewModel()`, using the standard `createComposeRule()` fails with:

```
java.lang.IllegalStateException: Given component holder class androidx.activity.ComponentActivity 
does not implement interface dagger.hilt.internal.GeneratedComponent
```

**Root Cause**: `createComposeRule()` creates a basic `ComponentActivity` that doesn't support Hilt dependency injection. Composables using `hiltViewModel()` require an Activity annotated with `@AndroidEntryPoint`.

## The Solution

Use `createAndroidComposeRule<HiltTestActivity>()` with `@HiltAndroidTest` annotation.

### Pattern for Screen Tests

```kotlin
@HiltAndroidTest
class MealListScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun mealListScreen_displaysTitle() {
        composeTestRule.setContent {
            FoodieTheme {
                MealListScreen(
                    onMealClick = {},
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
    }
}
```

### Pattern for Navigation Tests

```kotlin
@HiltAndroidTest
class NavGraphTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupNavHost() {
        hiltRule.inject()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            FoodieTheme {
                NavGraph(navController = navController)
            }
        }
    }

    @Test
    fun navGraph_startsAtMealListScreen() {
        composeTestRule.onNodeWithText("Foodie").assertIsDisplayed()
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(Screen.MealList.route)
    }
}
```

## Key Components

### HiltTestActivity

Location: `app/src/debug/java/com/foodie/app/HiltTestActivity.kt`

```kotlin
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
```

This simple activity provides Hilt support for tests. It's in the `debug` source set so it's only available for testing, not production.

### Test Rules

1. **HiltAndroidRule** (order = 0): Manages Hilt components in tests
2. **ComposeTestRule** (order = 1): Provides Compose testing APIs

Order matters! Hilt must initialize before Compose.

## When to Use Each Approach

### Use `createAndroidComposeRule<HiltTestActivity>()`

- ✅ Testing screens that use `hiltViewModel()`
- ✅ Testing navigation with `NavHost` containing Hilt screens
- ✅ Testing full integration flows
- ✅ Testing with real ViewModels and dependencies

### Use `createComposeRule()`

- ✅ Testing pure composables without ViewModels
- ✅ Testing composables with explicitly provided ViewModels
- ✅ Testing UI components in isolation

## Screen Design for Testability

Screens should accept ViewModel as a parameter with `hiltViewModel()` as default:

```kotlin
@Composable
fun MealListScreen(
    onMealClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealListViewModel = hiltViewModel()  // Default for production
) {
    // Screen implementation
}
```

This allows:
- Production code uses `hiltViewModel()` automatically
- Tests can optionally pass explicit ViewModels
- Maximum flexibility for different testing scenarios

## Advanced: Testing with Fake Repositories

For tests that need to control data, you can use Hilt's `@TestInstallIn` to replace production modules:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object RepositoryTestModule {
    @Singleton
    @Provides
    fun provideRepository(): Repository {
        return FakeRepository()
    }
}
```

**Note**: This requires the repository to be an interface or extracting one. Current Foodie implementation uses concrete repository classes, so this pattern is not yet implemented.

## Official Documentation

- [Compose Testing Overview](https://developer.android.com/develop/ui/compose/testing)
- [Compose Test Rules API](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary)
- [Hilt Testing Guide](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Hilt + Compose Integration](https://developer.android.com/develop/ui/compose/libraries#hilt)
- [Architecture Samples (Reference)](https://github.com/android/architecture-samples)

## Common Errors

### Error: "does not implement GeneratedComponent"

**Cause**: Using `createComposeRule()` with `hiltViewModel()`  
**Fix**: Use `createAndroidComposeRule<HiltTestActivity>()`

### Error: "HiltTestActivity not found"

**Cause**: Missing HiltTestActivity in debug source set  
**Fix**: Create `app/src/debug/.../HiltTestActivity.kt` with `@AndroidEntryPoint`

### Error: Test rule order issues

**Cause**: ComposeTestRule before HiltAndroidRule  
**Fix**: Use `@get:Rule(order = 0)` for Hilt, `order = 1` for Compose

## Examples in Codebase

- **Screen Test**: `app/src/androidTest/.../MealListScreenTest.kt`
- **Navigation Test**: `app/src/androidTest/.../NavGraphTest.kt`
- **Deep Link Test**: `app/src/androidTest/.../DeepLinkTest.kt`

---

*Based on official Android Architecture Samples and Android Developer documentation.*
