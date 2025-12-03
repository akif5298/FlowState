# Comprehensive Energy Prediction Inputs

## Overview

The Gemini energy prediction system now accepts **all available inputs** from:
1. **Health Connect** (read-only) - All health data types
2. **App Tests** - Typing speed and reaction time tests
3. **Manual User Inputs** - Self-reported energy, tiredness, meal impact, etc.
4. **Lifestyle Data** - Caffeine, emotion, schedule, tasks
5. **Device & Environment** - Screen time, weather, season, time of day

## Input Categories

### 1. Health Connect Data (Read-Only)
All Health Connect data types are collected if available:
- Heart Rate, HRV, Resting HR
- Sleep sessions
- Steps, Distance, Elevation, Floors
- Exercise sessions
- Calories (Active, Total)
- Blood Pressure, Oxygen Saturation, Respiratory Rate
- Body Temperature, Skin Temperature
- Weight, Height, Body Fat, Lean Mass, etc.
- Hydration, Nutrition
- Blood Glucose
- VO2 Max
- And all other Health Connect data types

**Note**: System only requests **READ** permissions, no write permissions.

### 2. App Test Results

#### Typing Speed Test
- **WPM** (Words Per Minute) - Recent test result
- **Accuracy** - Percentage (0-100)
- **Count** - Number of tests in period

#### Reaction Time Test
- **Median Reaction Time** - Milliseconds
- **Count** - Number of tests in period

**Impact**: Lower performance = cognitive fatigue = lower mental energy

### 3. Manual Energy Inputs (User Self-Reported)

#### Primary Energy Questions
- **How energized do you think you are?** (1-10 scale)
- **How physically tired do you feel?** (1-10 scale)
- **How mentally tired do you feel?** (1-10 scale)

#### Meal Impact
- **Did your last meal make you feel sluggish/neutral/sharp?**
  - sluggish = -10 to -15 energy points
  - neutral = no change
  - sharp = +5 to +10 energy points

#### Prediction Accuracy Feedback
- **Thumbs up/down** on app's prediction accuracy

**Weighting**: User self-reported data gets **50% weight** when available (most accurate for current state)

### 4. Schedule & Tasks

- **Schedule for the day** - Planned activities
- **Completed tasks** - Tasks already done (indicates energy expenditure)
- **Upcoming tasks** - Tasks still to do (affects energy planning)
- **Meal times** - Scheduled or completed meals

**Impact**: 
- Post-meal (1-2h after) = -5 to -10 points (post-lunch dip)
- Pre-meal (if meal soon) = -3 to -5 points (hunger/low energy)
- Upcoming demanding tasks = may require energy conservation

### 5. Lifestyle Factors

#### Caffeine Intake
- **Amount in mg** - Tracked throughout the day
- **Timing effects**:
  - Recent (<2h) = +5 to +15 points (peak effect)
  - 4-6h ago = +2 to +5 points (declining)
  - Crash (>6h after high dose) = -5 to -10 points

#### Current Emotion
- **Emotion state**: happy, sad, neutral, stressed, anxious, etc.
- **Impact**:
  - Happy/positive = +3 to +8 points
  - Stressed/anxious/sad = -5 to -15 points

### 6. Device & Environment

#### Device Usage
- **Screen time** - Minutes/hours today
- **Impact**: High screen time (>8h) = -3 to -8 points (eye strain, mental fatigue)

#### Weather
- **Condition**: sunny, cloudy, rainy, clear, etc.
- **Temperature**: Celsius
- **Impact**: 
  - Sunny = +2 to +5 points
  - Cloudy/rainy = -2 to -5 points

#### Season
- **Season**: winter, spring, summer, fall
- **Impact**: 
  - Winter (less sunlight) = -3 to -5 points
  - Summer = +2 to +5 points

#### Time of Day
- **Current hour** (0-23)
- **Time period**: morning, afternoon, evening, night
- **Circadian effects**:
  - Morning (8-11am) = +5 to +10 points
  - Afternoon dip (2-4pm) = -5 to -10 points
  - Evening (after 8pm) = -5 to -10 points

## Data Storage

### Manual Inputs
Stored in `SharedPreferences` with key `"flowstate_manual_inputs"`:
- `manual_energy_level` (int, 1-10)
- `physical_tiredness` (int, 1-10)
- `mental_tiredness` (int, 1-10)
- `meal_impact` (String: "sluggish", "neutral", "sharp")
- `prediction_accuracy_thumbs_up` (boolean)
- `schedule_for_day` (String)
- `completed_tasks` (String)
- `upcoming_tasks` (String)
- `meal_times` (String)
- `caffeine_intake_mg` (float)
- `current_emotion` (String)
- `last_manual_input_timestamp` (long)

### App Tests
Stored in Room database:
- `typing_local` table
- `reaction_local` table

### Health Connect Data
Stored in Room database (read from Health Connect):
- Various health data tables

## Usage

### Setting Manual Inputs

```java
ManualInputManager inputManager = new ManualInputManager(context);

// Set energy level (1-10)
inputManager.setManualEnergyLevel(7);

// Set tiredness
inputManager.setPhysicalTiredness(5);
inputManager.setMentalTiredness(6);

// Set meal impact
inputManager.setMealImpact("sluggish"); // or "neutral" or "sharp"

// Set prediction feedback
inputManager.setPredictionAccuracy(true); // thumbs up

// Set schedule
inputManager.setScheduleForDay("9am: Meeting, 12pm: Lunch, 2pm: Project work");

// Set tasks
inputManager.setCompletedTasks("Morning standup, Email review");
inputManager.setUpcomingTasks("Code review, Team sync");

// Set meal times
inputManager.setMealTimes("Breakfast: 8am, Lunch: 12:30pm, Dinner: 7pm");

// Track caffeine
inputManager.addCaffeineIntakeMg(95); // One cup of coffee
inputManager.addCaffeineIntakeMg(50); // Later, another half cup

// Set emotion
inputManager.setCurrentEmotion("happy");
```

### Automatic Collection

The system automatically collects:
- Typing/reaction test results from database
- Device screen time (if permission granted)
- Weather (if available)
- Season (calculated from date)
- Time of day (calculated)

## Weighting System

When all data is available, weighting is:
1. **User Self-Reported Energy** (50%) - Most accurate for current state
2. **Sleep** (25%) - Critical baseline
3. **HRV** (15%) - Recovery indicator
4. **Cognitive Tests** (5%) - Mental fatigue
5. **Heart Rate** (3%) - Stress/fatigue
6. **Activity** (2%) - Moderate impact
7. **Other factors** - Considered in explanation

If user self-report is not available, weighting adjusts:
- Sleep (40%)
- HRV (30%)
- Heart Rate (15%)
- Activity (10%)
- Time of Day (5%)

## Benefits

1. **Comprehensive**: Uses all available data sources
2. **User-Centric**: Prioritizes user self-report when available
3. **Context-Aware**: Considers schedule, meals, caffeine, weather
4. **Cognitive**: Includes mental performance indicators
5. **Read-Only**: Respects user privacy, no write permissions

## Privacy

- **No write permissions** - System only reads Health Connect data
- **User control** - Manual inputs are optional
- **Local storage** - Manual inputs stored locally in SharedPreferences
- **Transparent** - All inputs explained in prediction explanation

