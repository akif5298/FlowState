# Energy Prediction Service Setup

This document explains the Java implementation of the energy prediction service that calls the HuggingFace remote model API directly from the Android app.

## Overview

The Android app now includes a Java service that:
1. Collects health data from Health Connect (stored locally)
2. Formats the data for the HuggingFace API (simple history format)
3. Calls the HuggingFace TimesFM endpoint directly
4. Returns energy level predictions (0-100 scale)

## Components

### 1. EnergyPredictionService
**Location:** `app/src/main/java/com/flowstate/services/EnergyPredictionService.java`

Main service for calling the remote prediction API. Provides methods:
- `predictEnergyLevels(int forecastHorizon)` - Predicts energy levels using local health data
- `getCurrentEnergyLevel()` - Gets the immediate next energy level prediction

### 2. HealthDataAggregator
**Location:** `app/src/main/java/com/flowstate/services/HealthDataAggregator.java`

Collects and formats health data from the local Room database:
- Heart rate data
- HRV (Heart Rate Variability) data
- Steps data (aggregated hourly)
- Sleep data

### 3. API Models
- `PredictionRequest.java` - Request model matching Python backend format
- `PredictionResponse.java` - Response model for API responses
- Updated `EnergyPrediction.java` - Now supports numeric energy levels (0-100)

## Configuration

Add to `local.properties`:
```properties
# HuggingFace TimesFM endpoint (required)
REMOTE_MODEL_URL=https://router.huggingface.co/models/google/timesfm-1.0-200m

# Optional: HuggingFace API key (if required)
REMOTE_MODEL_API_KEY=your-huggingface-api-key-here
```

**Note:** This service only works with the HuggingFace remote endpoint, not with local Python servers.

## Usage

The service is automatically integrated into `EnergyDashboardActivity`. When the dashboard loads, it:
1. Collects health data from the local database
2. Calls the prediction service
3. Displays the current energy level (0-100) and level category (HIGH/MEDIUM/LOW)

### Manual Usage Example

```java
EnergyPredictionService service = new EnergyPredictionService(context);

// Get current energy level
service.getCurrentEnergyLevel()
    .thenAccept(energyLevel -> {
        // energyLevel is a Double (0-100)
        Log.d("Energy", "Current energy: " + energyLevel);
    })
    .exceptionally(error -> {
        Log.e("Energy", "Error", error);
        return null;
    });

// Get forecast for next 12 periods
service.predictEnergyLevels(12)
    .thenAccept(forecast -> {
        // forecast is List<Double> with 12 predicted values
        for (int i = 0; i < forecast.size(); i++) {
            Log.d("Energy", "Period " + i + ": " + forecast.get(i));
        }
    });
```

## Data Flow

1. **Health Connect** → Syncs data to local Room database via Workers
2. **HealthDataAggregator** → Collects last 48 hours of data from database
3. **EnergyPredictionService** → Formats data and calls remote API
4. **Remote API** → Returns energy level predictions
5. **UI** → Displays energy level in EnergyDashboardActivity

## API Format

The service sends requests in the simple format expected by HuggingFace:
```json
{
  "history": [72, 76, 80, 74, 78, 82, 88, 75, 79, 84, 81, 77]
}
```

The service uses heart rate as the primary time series (or first available series if heart rate is not available).

The HuggingFace API returns the forecast directly as an array:
```json
[75.2, 73.8, 72.1, 70.5, 69.2, 68.1, ...]
```

Or wrapped in a response object (the service handles both formats).

## Troubleshooting

1. **No health data available**: Ensure Health Connect permissions are granted and data has been synced
2. **API errors**: Check that `REMOTE_MODEL_URL` is correctly set in `local.properties` to the HuggingFace endpoint
3. **Network timeouts**: The service has a 120-second timeout for ML inference
4. **Invalid response format**: The service handles both array and object response formats from HuggingFace

## Implementation Details

- The service uses heart rate as the primary time series (best proxy for energy levels)
- Falls back to first available time series if heart rate is not available
- Uses the last 48 hours of data (48 data points for hourly aggregation)
- Sends simple `{"history": [...]}` format to HuggingFace API
- Handles both direct array responses and wrapped object responses

## Next Steps

- Consider adding caching to avoid repeated API calls
- Add retry logic for network failures
- Implement exponential backoff for rate limiting

