# Foodie Android Application

## Overview

Foodie is a health-focused Android application that helps users track nutritional intake by integrating with Android's Health Connect API. The app follows MVVM architecture with Jetpack Compose for modern, declarative UI.

## Architecture

The application follows **MVVM (Model-View-ViewModel)** architecture pattern:

- **Model**: Data layer with repositories and data sources
- **View**: Jetpack Compose UI screens
- **ViewModel**: Business logic and state management

### Key Components

- **Health Connect Integration**: Complete SDK integration for reading/writing nutrition data
- **Dependency Injection**: Hilt for compile-time DI
- **UI Framework**: Jetpack Compose with Material3
- **Navigation**: Jetpack Navigation Compose

## Health Connect Integration

### Setup

Health Connect SDK is integrated to track nutrition data (NutritionRecord). The integration includes:

1. **Permissions**: Runtime permissions managed through MainActivity
2. **SDK Wrapper**: `HealthConnectManager` provides abstraction over Health Connect SDK
3. **Repository**: `HealthConnectRepository` handles CRUD operations
4. **UI Components**: Permission dialogs and unavailability handling

### Permission Flow

```kotlin
// 1. Check if Health Connect is available
val isAvailable = healthConnectManager.isAvailable()

// 2. Check permissions
val hasPermissions = healthConnectManager.checkPermissions()

// 3. Request permissions if needed
val permissionContract = healthConnectManager.createPermissionRequestContract()
launcher.launch(permissionContract)
```

### CRUD Operations

**Create Nutrition Record:**
```kotlin
val nutritionRecord = NutritionRecord(
    startTime = Instant.now(),
    endTime = Instant.now(),
    name = "Apple",
    energy = Energy.kilocalories(95.0)
)
healthConnectRepository.insertNutritionRecords(listOf(nutritionRecord))
```

**Read Nutrition Records:**
```kotlin
val startTime = Instant.now().minus(7, ChronoUnit.DAYS)
val endTime = Instant.now()
val records = healthConnectRepository.readNutritionRecords(startTime, endTime)
```

**Update Records:**
```kotlin
healthConnectRepository.updateNutritionRecords(updatedRecords)
```

**Delete Records:**
```kotlin
healthConnectRepository.deleteNutritionRecords(recordIds)
```

### Handling Health Connect Unavailability

The app gracefully handles cases where Health Connect is not available:

```kotlin
if (!healthConnectManager.isAvailable()) {
    // Show HealthConnectUnavailableDialog
    // Dialog provides link to Play Store for installation
}
```

### Testing

Comprehensive tests cover all Health Connect integration points:

- **Unit Tests** (`app/src/test/`):
  - `HealthConnectManagerTest`: SDK wrapper functionality
  - `HealthConnectRepositoryTest`: CRUD operations
  
- **Instrumentation Tests** (`app/src/androidTest/`):
  - `HealthConnectPermissionFlowTest`: Permission request flow
  - `HealthConnectIntegrationTest`: End-to-end insert/query/delete
  - `HealthConnectHiltTest`: Dependency injection verification
  - `HealthConnectUnavailableDialogTest`: UI dialog interactions

**Note**: Instrumentation tests automatically skip if Health Connect is not available on the device.

## Build & Run

### Prerequisites

- Android Studio Ladybug or later
- JDK 17
- Android SDK 34
- Health Connect app installed (for testing)

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run all tests
./gradlew check
```

### Running the App

1. Open project in Android Studio
2. Sync Gradle files
3. Select device/emulator (API 34+)
4. Click Run or use `./gradlew installDebug`

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/foodie/app/
│   │   │   ├── data/
│   │   │   │   ├── local/healthconnect/
│   │   │   │   │   └── HealthConnectManager.kt
│   │   │   │   └── repository/
│   │   │   │       └── HealthConnectRepository.kt
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   │   └── HealthConnectUnavailableDialog.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   └── AppNavigation.kt
│   │   │   │   └── screens/
│   │   │   │       └── meallist/
│   │   │   │           ├── MealListScreen.kt
│   │   │   │           └── MealListViewModel.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── FoodieApplication.kt
│   │   └── AndroidManifest.xml
│   ├── test/ (unit tests)
│   └── androidTest/ (instrumentation tests)
└── build.gradle.kts
```

## Dependencies

Key libraries:

- **Kotlin**: 2.1.0
- **Compose BOM**: 2024.11.00
- **Hilt**: 2.53
- **Health Connect SDK**: 1.1.0-alpha10
- **Navigation Compose**: 2.8.5
- **JUnit**: 4.13.2
- **Mockito**: 5.14.2
- **Truth**: 1.4.4

## Configuration

### Minimum SDK

- `minSdk`: 26 (Android 8.0)
- `targetSdk`: 34 (Android 14)
- `compileSdk`: 34

### Health Connect Permissions

Declared in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.health.READ_NUTRITION"/>
<uses-permission android:name="android.permission.health.WRITE_NUTRITION"/>
```

## Development Notes

### Update Pattern for Health Connect Records

Health Connect uses an **insert-for-update** pattern. To update records:

1. Delete existing record by ID
2. Insert new record with updated data
3. New record receives new ID from Health Connect

This pattern is necessary because Health Connect manages record IDs and metadata.

### Error Handling

All Health Connect operations include proper error handling:

- Permission denials → Request permissions
- Health Connect unavailable → Show install dialog
- Network/timeout errors → Graceful failure with user messaging
- Invalid data → Validation before insertion

## Contributing

When adding new features:

1. Follow MVVM architecture pattern
2. Use Hilt for dependency injection
3. Write comprehensive unit tests
4. Write instrumentation tests for UI/integration
5. Update this README with new functionality
6. Document public APIs with KDoc comments

## License

[Add license information]
