# Instrumented Tests

This directory contains instrumented tests for the FlowState app. These tests run on an Android device or emulator and require the app to be installed.

## Prerequisites

1. **Android Device or Emulator**: Tests must run on a physical device or emulator
2. **App Installed**: The app must be installed on the device/emulator
3. **Permissions**: Some tests may require specific permissions (granted at runtime)

## Running Instrumented Tests

### In Android Studio:
1. Connect a device or start an emulator
2. Right-click on `app/src/androidTest/java` folder
3. Select "Run 'All Tests'"
4. Or run individual test classes by right-clicking on the test file

### From Command Line:
```bash
./gradlew connectedAndroidTest
```

### Running Specific Test:
```bash
./gradlew connectedAndroidTest --tests "com.personaleenergy.app.ui.EnergyPredictionActivityTest"
```

## Test Coverage

### Activities
- `EnergyPredictionActivityTest.java` - Tests for energy prediction activity
- `AIScheduleActivityTest.java` - Tests for AI schedule activity

### Services
- `HealthConnectManagerTest.java` - Tests for Health Connect manager
- `AuthServiceTest.java` - Tests for authentication service

## Dependencies

Tests use:
- **AndroidX Test** - Android testing framework
- **Espresso** - UI testing framework
- **JUnit 4** - Testing framework

## Notes

- Tests may take longer to run than unit tests
- Some tests require specific device capabilities (e.g., Health Connect)
- Tests are designed to be flexible and handle missing dependencies gracefully
- Network-dependent tests may fail if device is offline

## Troubleshooting

### Tests fail with "No connected devices"
- Ensure a device/emulator is connected: `adb devices`
- Start an emulator from Android Studio

### Tests fail with permission errors
- Grant required permissions manually on the device
- Some permissions may need to be granted at runtime

### Tests timeout
- Increase timeout values in test configuration
- Check device performance and available resources

