# Gemini 12-Hour Energy Prediction

## Overview

The Gemini API integration now returns **two variables**:
1. **12-hour energy predictions**: Array of 12 energy levels (0-100) for the next 12 hours
2. **Explanation string**: Detailed explanation of the predictions based on health data points

## What Changed

### New Model: `EnergyPredictionResult`
- Contains `hourlyPredictions`: List of 12 Double values (0-100)
- Contains `explanation`: String explaining the predictions
- Convenience method `getCurrentEnergyLevel()` returns first hour prediction

### Updated Function Declaration
The Gemini function now returns:
```json
{
  "hourly_predictions": [75, 72, 68, 65, 63, 60, 58, 55, 57, 60, 65, 70],
  "explanation": "Based on your health data, your energy levels are predicted to..."
}
```

### Updated Methods

1. **`GeminiEnergyPredictor.predictEnergyLevels12Hours()`**
   - Returns `CompletableFuture<EnergyPredictionResult>`
   - Gets 12-hour forecast + explanation

2. **`EnergyPredictionService.getEnergyPredictions12Hours()`**
   - Primary method for getting full predictions
   - Returns `EnergyPredictionResult` with both predictions and explanation

3. **`EnergyPredictionService.getCurrentEnergyLevel()`**
   - Still available for convenience
   - Gets first hour from 12-hour forecast

### UI Updates

`EnergyDashboardActivity` now:
- Displays current energy level (first hour from 12-hour forecast)
- Shows Gemini's explanation in the `tvAIInsight` TextView
- Falls back to generated insight if explanation is missing

## Usage Example

```java
EnergyPredictionService service = new EnergyPredictionService(context);

// Get 12-hour predictions with explanation
service.getEnergyPredictions12Hours()
    .thenAccept(result -> {
        // Get current energy (first hour)
        Double current = result.getCurrentEnergyLevel();
        
        // Get all 12 hourly predictions
        List<Double> predictions = result.getHourlyPredictions();
        
        // Get explanation
        String explanation = result.getExplanation();
        
        // Display in UI
        displayEnergy(current);
        displayExplanation(explanation);
        plotForecast(predictions); // For charts
    });
```

## Explanation Format

The explanation includes:
- Which health metrics influenced the prediction most
- Why energy levels change throughout the day
- Any concerning patterns (poor sleep, low HRV, etc.)
- Recommendations based on the forecast

Example explanation:
> "Your energy is predicted to start at 75 and gradually decline to 60 over the next 6 hours. This is primarily due to your poor sleep last night (only 5.5 hours) and low HRV readings (38ms), indicating insufficient recovery. The afternoon dip (2-4pm) will further reduce energy. Your elevated resting heart rate (82 bpm, 12 bpm above baseline) suggests stress or fatigue. Consider taking breaks, staying hydrated, and avoiding intensive tasks during the low-energy period. Energy should recover slightly in the evening as your circadian rhythm adjusts."

## Benefits

1. **Transparency**: Users understand why predictions are made
2. **Actionable**: Explanations include recommendations
3. **Trust**: Shows the system is analyzing real data
4. **Forecasting**: 12-hour view helps plan the day

## Data Points Used in Explanation

The explanation references:
- **Sleep**: Last night's hours, quality, patterns
- **HRV**: Current and average values, recovery status
- **Heart Rate**: Current, resting, elevated status
- **Activity**: Today's steps, activity level
- **Time of Day**: Circadian rhythm effects
- **Trends**: How metrics changed over time

## Future Enhancements

- Display 12-hour forecast as a chart/graph
- Allow users to see predictions for specific hours
- Compare predicted vs actual energy levels
- Personalized recommendations based on patterns

