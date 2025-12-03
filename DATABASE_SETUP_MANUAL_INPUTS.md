# Database Setup for Manual Inputs

## Overview

All non-Health Connect data points are now stored in SQLite database using Room. This includes:
- Manual energy inputs
- Schedule & tasks
- Caffeine intake
- Emotions
- Device usage
- Weather data

## Database Entities

### 1. ManualEnergyInputLocal
Stores user self-reported energy data:
- `energyLevel` (1-10)
- `physicalTiredness` (1-10)
- `mentalTiredness` (1-10)
- `mealImpact` ("sluggish", "neutral", "sharp")
- `predictionAccuracyThumbsUp` (boolean)
- `timestamp`

### 2. ScheduleLocal
Stores daily schedule and task information:
- `date` (start of day timestamp)
- `scheduleForDay` (free text)
- `completedTasks` (comma-separated or JSON)
- `upcomingTasks` (comma-separated or JSON)
- `mealTimes` (formatted string)
- `lastUpdated`

### 3. CaffeineIntakeLocal
Stores individual caffeine intake records:
- `timestamp`
- `caffeineMg` (milligrams)
- `source` (e.g., "coffee", "tea", "energy drink")

### 4. EmotionLocal
Stores emotion/feeling records:
- `timestamp`
- `emotion` ("happy", "sad", "neutral", "stressed", etc.)
- `notes` (optional)

### 5. DeviceUsageLocal
Stores daily device usage:
- `date` (start of day timestamp)
- `screenTimeMinutes`
- `lastUpdated`

### 6. WeatherLocal
Stores weather data:
- `timestamp`
- `condition` ("sunny", "cloudy", "rainy", etc.)
- `temperatureCelsius`
- `season` ("winter", "spring", "summer", "fall")

## Usage

### ManualInputManager

```java
ManualInputManager inputManager = new ManualInputManager(context);

// Save complete manual energy input
inputManager.saveManualEnergyInput(
    7,  // energy level
    5,  // physical tiredness
    6,  // mental tiredness
    "sluggish",  // meal impact
    true  // prediction accuracy thumbs up
);

// Or save individual fields
inputManager.setManualEnergyLevel(7);
inputManager.setPhysicalTiredness(5);
inputManager.setMentalTiredness(6);
inputManager.setMealImpact("sluggish");
inputManager.setPredictionAccuracy(true);

// Schedule & tasks
inputManager.setScheduleForDay(
    "9am: Meeting, 12pm: Lunch, 2pm: Project work",
    "Morning standup, Email review",  // completed
    "Code review, Team sync",  // upcoming
    "Breakfast: 8am, Lunch: 12:30pm, Dinner: 7pm"  // meal times
);

// Caffeine intake
inputManager.addCaffeineIntake(95, "coffee");  // One cup
inputManager.addCaffeineIntake(50, "tea");  // Later, half cup

// Emotion
inputManager.setCurrentEmotion("happy", "Feeling good today!");

// Get data
ManualEnergyInputLocal recent = inputManager.getMostRecentInput();
ScheduleLocal today = inputManager.getTodaySchedule();
double caffeine = inputManager.getTodayCaffeineMg();
EmotionLocal emotion = inputManager.getMostRecentEmotion();
```

### DataCollectionService

Automatically collects device usage and weather:

```java
DataCollectionService collector = new DataCollectionService(context);

// Collect all automatic data
collector.collectAll();

// Or individually
collector.collectDeviceUsage();
collector.collectWeather();
```

## Automatic Collection

The `DataCollectionService` should be called periodically (e.g., via WorkManager):

1. **Device Usage**: Collects screen time daily
2. **Weather**: Fetches current weather (currently using mock data, needs API integration)

## Database Schema

Database version: **4**

All entities include:
- `id` (auto-generated primary key)
- Timestamps for tracking

**Note**: All data is stored locally in SQLite only. No cloud sync.

## Migration

The database version was incremented from 3 to 4. Room will handle migration automatically with `fallbackToDestructiveMigration()` (for development). In production, you should create proper migration scripts.

## Integration with HealthDataAggregator

`HealthDataAggregator` now reads from the database instead of SharedPreferences:

- Gets most recent manual energy input (within last 6 hours)
- Gets today's schedule
- Sums caffeine intake for today
- Gets most recent emotion (within last 6 hours)
- Gets today's device usage
- Gets most recent weather

All data is automatically included in the `HealthDataSummary` sent to Gemini.

## Next Steps

1. **UI Integration**: Create or update UI components to input this data
2. **WorkManager**: Set up periodic collection of device usage and weather
3. **Weather API**: Integrate actual weather API (OpenMeteo, OpenWeatherMap, etc.)
4. **Data Validation**: Add validation for inputs (e.g., energy level 1-10)

## Example: Complete Input Flow

```java
// User opens app and inputs energy data
ManualInputManager inputManager = new ManualInputManager(context);
inputManager.saveManualEnergyInput(7, 5, 6, "neutral", null);

// User adds schedule
inputManager.setScheduleForDay(
    "9am: Meeting, 12pm: Lunch",
    "Morning standup",
    "Code review",
    "Breakfast: 8am, Lunch: 12:30pm"
);

// User drinks coffee
inputManager.addCaffeineIntake(95, "coffee");

// User sets emotion
inputManager.setCurrentEmotion("happy", null);

// System automatically collects device usage and weather
DataCollectionService collector = new DataCollectionService(context);
collector.collectAll();

// When prediction is requested, HealthDataAggregator reads all this data
HealthDataAggregator aggregator = new HealthDataAggregator(context);
HealthDataSummary summary = aggregator.createHealthSummary(48);

// Summary now includes all manual inputs, schedule, caffeine, emotion, device usage, weather
// This is sent to Gemini for energy prediction
```

