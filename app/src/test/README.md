# Unit Tests

This directory contains unit tests for the FlowState app. These tests run on the JVM and don't require an Android device or emulator.

## Running Unit Tests

### In Android Studio:
1. Right-click on `app/src/test/java` folder
2. Select "Run 'All Tests'"
3. Or run individual test classes by right-clicking on the test file

### From Command Line:
```bash
./gradlew test
```

### Running Specific Test:
```bash
./gradlew test --tests "com.personaleenergy.app.data.models.BiometricDataTest"
```

## Test Coverage

### Data Models
- `BiometricDataTest.java` - Tests for biometric data model
- `EnergyPredictionTest.java` - Tests for energy prediction model
- `TypingSpeedDataTest.java` - Tests for typing speed data model
- `ReactionTimeDataTest.java` - Tests for reaction time data model

### API Services
- `ChronosApiServiceTest.java` - Tests for Chronos API service (uses MockWebServer)

### ML Components
- `EnergyMLPredictorTest.java` - Tests for ML-based energy predictor (uses Robolectric)

### AI Components
- `SmartCalendarAITest.java` - Tests for smart calendar AI logic (uses Robolectric)

## Dependencies

Tests use:
- **JUnit 4** - Testing framework
- **Mockito** - Mocking framework
- **Robolectric** - Android testing framework for unit tests
- **MockWebServer** - HTTP server for testing API calls

## Notes

- Tests are designed to be flexible and not hardcoded
- Some tests may require network access (ChronosApiServiceTest) but handle failures gracefully
- Robolectric provides Android context without needing a device

## Known Issues

### Windows Compatibility
- **Robolectric on Windows**: Tests using Robolectric (`EnergyMLPredictorTest`, `SmartCalendarAITest`) **are automatically excluded on Windows** due to POSIX permissions issues. This is a known Robolectric limitation.
  - **Current Behavior**: These tests are excluded from the test run on Windows via Gradle configuration
  - **Workaround for Full Coverage**: 
    - Run tests on Linux/Mac for full coverage
    - Use WSL (Windows Subsystem for Linux) on Windows
  - **Affected Tests**: `EnergyMLPredictorTest`, `SmartCalendarAITest` (excluded on Windows)
  - **Working Tests on Windows**: All model tests (`BiometricDataTest`, `EnergyPredictionTest`, `TypingSpeedDataTest`, `ReactionTimeDataTest`), `ChronosApiServiceTest` (with network)
  - **Test Count**: 21 tests run on Windows (all model and API tests), 13 Robolectric tests excluded

### Android Log Mocking
- **Android Log calls**: Unit tests use `testOptions.unitTests.returnDefaultValues = true` to allow Android framework calls
- **ChronosApiService**: Uses `android.util.Log` which is handled by the test configuration

### Network-Dependent Tests
- **ChronosApiServiceTest**: Tests may timeout if network is unavailable or API is unreachable
- Tests are designed to handle network failures gracefully with longer timeouts (10 seconds)

