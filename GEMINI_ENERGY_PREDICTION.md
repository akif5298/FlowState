# Gemini Energy Prediction Setup

This document explains the Gemini API integration for energy level prediction using Health Connect data.

## Overview

The app now uses **Google Gemini API with function calling** to predict energy levels (0-100 scale) based on health data from Health Connect. This is the primary method, with fallback to remote API if Gemini is unavailable.

## Architecture

### Components

1. **HealthDataAggregator** - Collects and summarizes health data
   - Aggregates data from last 48 hours
   - Creates `HealthDataSummary` with key metrics
   - Calculates averages, current values, and trends

2. **GeminiEnergyPredictor** - Gemini API integration
   - Uses function calling to get structured energy predictions
   - Sends health data summary to Gemini
   - Parses energy level (0-100) from response

3. **EnergyPredictionService** - Main service
   - Tries Gemini first (preferred)
   - Falls back to remote API if Gemini unavailable
   - Returns energy level as `CompletableFuture<Double>`

## Health Data Weighting

The system uses the following weighting for energy prediction:

- **Sleep (40% weight)** - Most important factor
  - Last night's sleep hours
  - Average sleep quality
  - Sleep patterns over time

- **HRV - Heart Rate Variability (30% weight)** - Very important for recovery
  - Current HRV (higher = better recovery)
  - Average HRV trends
  - Normal range: 20-100ms, good: 50-100ms

- **Heart Rate (15% weight)** - Important indicator
  - Current heart rate
  - Resting heart rate
  - Elevated HR at rest = stress/fatigue

- **Activity Level (10% weight)** - Moderate influence
  - Today's step count
  - Activity relative to baseline

- **Time of Day (5% weight)** - Circadian rhythm
  - Morning (8-11am): +5 to +10 points
  - Afternoon (2-4pm): -5 to -10 points (natural dip)
  - Evening/Night: Neutral to negative

## Data Usage

- **Time Window**: Last 48 hours of data
- **Recent Data Priority**: Last 6 hours weighted more heavily
- **Minimum Data**: Works with partial data, gracefully handles missing metrics

## Configuration

Add to `local.properties`:

```properties
# Google Gemini API Key (required for energy prediction)
GEMINI_API_KEY=your-gemini-api-key-here
```

Get your API key from: https://aistudio.google.com/app/apikey

## Usage

The service is automatically integrated into `EnergyDashboardActivity`. When the dashboard loads:

1. Collects health data from local database
2. Creates health summary with aggregated metrics
3. Sends to Gemini API with function calling
4. Receives energy level (0-100)
5. Displays in UI

### Manual Usage

```java
EnergyPredictionService service = new EnergyPredictionService(context);

// Get current energy level (0-100)
service.getCurrentEnergyLevel()
    .thenAccept(energyLevel -> {
        // energyLevel is a Double (0-100)
        Log.d("Energy", "Current energy: " + energyLevel);
        
        // Convert to category
        EnergyLevel level;
        if (energyLevel >= 70) {
            level = EnergyLevel.HIGH;
        } else if (energyLevel >= 40) {
            level = EnergyLevel.MEDIUM;
        } else {
            level = EnergyLevel.LOW;
        }
    })
    .exceptionally(error -> {
        Log.e("Energy", "Error", error);
        return null;
    });
```

## Energy Level Scale

- **0-30**: Low energy (needs rest, recovery activities)
- **31-60**: Medium energy (moderate tasks, routine work)
- **61-85**: High energy (intensive work, problem-solving)
- **86-100**: Peak energy (best for challenging tasks)

## Fallback Behavior

If Gemini API is unavailable or fails:
1. Falls back to remote HuggingFace API (if configured)
2. Returns error if both methods fail
3. UI shows appropriate error message

## Benefits of Gemini Approach

1. **Intelligent Reasoning**: Understands context and relationships between metrics
2. **Flexible**: Handles missing or incomplete data gracefully
3. **Explainable**: Can provide reasoning (future enhancement)
4. **No Model Deployment**: No need to convert or deploy ML models
5. **Easy Updates**: Can adjust weighting logic via prompts

## Future Enhancements

- Add reasoning/explanation to predictions
- Support for multi-hour forecasts
- Personalized weighting based on user patterns
- Caching to reduce API calls
- Offline fallback with rule-based predictions

