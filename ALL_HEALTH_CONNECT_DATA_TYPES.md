# All Health Connect Data Types Integration

## Overview

The Gemini energy prediction system now supports **all Health Connect data types** as optional inputs. The system will automatically include any available data in the prediction, making it more comprehensive and accurate.

## Supported Data Types

### Core Metrics (Always Prioritized)
- **Heart Rate** - Current, average, resting
- **HRV (Heart Rate Variability)** - Current, average
- **Sleep** - Last night, average, quality
- **Steps** - Today, average daily

### Activity Metrics
- **Active Calories Burned** - kcal
- **Total Calories Burned** - kcal
- **Distance** - meters
- **Elevation Gained** - meters
- **Floors Climbed** - count
- **Speed** - m/s
- **Power** - watts
- **Cycling Cadence** - rpm
- **VO2 Max** - ml/kg/min
- **Wheelchair Pushes** - count

### Vitals
- **Resting Heart Rate** - bpm (separate from heart rate)
- **Blood Pressure** - Systolic/Diastolic (mmHg)
- **Oxygen Saturation** - percentage (0-100)
- **Respiratory Rate** - breaths per minute
- **Body Temperature** - celsius
- **Skin Temperature** - celsius

### Body Measurements
- **Weight** - kg
- **Height** - meters
- **Body Fat** - percentage
- **Lean Body Mass** - kg
- **Bone Mass** - kg
- **Body Water Mass** - kg
- **Basal Metabolic Rate** - kcal/day

### Nutrition & Hydration
- **Hydration** - liters
- **Nutrition** - Calories, Protein, Carbs, Fat

### Wellness
- **Mindfulness Sessions** - minutes

### Blood & Glucose
- **Blood Glucose** - mmol/L

### Exercise Details
- **Exercise Sessions** - count, duration, types

## How It Works

1. **Data Collection**: `HealthDataAggregator` collects all available data types from Health Connect
2. **Summary Creation**: Creates `HealthDataSummary` with all available metrics
3. **Gemini Analysis**: Sends comprehensive data to Gemini API
4. **Intelligent Weighting**: Gemini considers all available data points in its prediction

## Data Availability

- **None required**: All data types are optional
- **Graceful degradation**: System works with any combination of available data
- **Automatic inclusion**: Any data available in Health Connect is automatically included
- **No manual configuration**: System detects and uses what's available

## Example

If a user has:
- Heart rate data ✓
- Sleep data ✓
- Steps data ✓
- Blood pressure data ✓
- Hydration data ✓

The system will include all of these in the prediction, even if other data types (like HRV, weight, etc.) are not available.

## Benefits

1. **More Accurate**: More data = better predictions
2. **Comprehensive**: Uses all available health information
3. **Flexible**: Works with any combination of data
4. **Future-Proof**: Automatically uses new data types as they become available

## Implementation Details

### HealthConnectManager
- Added read methods for all Health Connect data types
- Methods return `CompletableFuture` for async operations
- Gracefully handles missing permissions or unavailable data

### HealthDataSummary
- Expanded to include all data type fields
- All fields are optional (nullable)
- Comprehensive getters and setters

### HealthDataAggregator
- Collects all available data types
- Aggregates metrics (averages, totals, latest values)
- Creates comprehensive summary for Gemini

### Gemini Prompt
- Dynamically includes all available data in prompt
- Explains relevance of each metric to energy prediction
- Provides context for interpretation

## Data Type Categories

### High Impact (Primary Weighting)
- Sleep quality and duration
- HRV (recovery indicator)
- Heart rate patterns

### Medium Impact
- Activity levels (steps, calories, exercise)
- Resting heart rate
- Blood pressure
- Hydration

### Contextual Impact
- Body measurements (weight, BMI)
- Nutrition
- Time of day
- Exercise types

### Supporting Metrics
- Oxygen saturation
- Respiratory rate
- Blood glucose
- Mindfulness

## Notes

- **Cycle Tracking** data types (menstruation, ovulation, etc.) are not included as they're not directly relevant to energy prediction
- **Sexual Activity** is not included for privacy and relevance reasons
- All other data types are included if available

## Future Enhancements

- Personalized weighting based on which metrics correlate best with user's energy
- Machine learning to optimize which data types matter most for each user
- Historical pattern analysis across all data types

