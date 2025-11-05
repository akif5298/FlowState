# FlowState

## Overview
FlowState is an Android application that predicts personal energy highs and lows throughout the day, helping users optimize their productivity by scheduling demanding tasks during peak energy periods and restorative activities during low energy periods.

## Project Team
- Bibek Chugh
- Kush Jain
- Akif Rahman
- Yusuf Muzaffar Iqbal
- Tharun Indrakumar

## Features

### Core Functionality
- **Google Fit Integration**: Collects biometric data including heart rate, sleep cycles, and skin temperature from wearable devices
- **Cognitive Performance Measurement**: 
  - Typing speed test to measure motor skills and cognitive speed
  - Reaction time test to assess alertness and responsiveness
- **ML-Based Energy Prediction**: Uses machine learning to predict energy levels throughout the day
- **LLM-Generated Suggestions**: Provides personalized productivity schedules and advice based on predicted energy cycles

### Modules

#### Data Collection Module
- Integrates with Google Fit API to access biometric data
- Implements typing speed and reaction time measurements
- Securely stores and manages user data

#### ML Module
- TensorFlow Lite integration for on-device inference
- Analyzes historical biometric and activity data
- Predicts energy highs and lows with confidence scores
- Supports time-series analysis

#### LLM Module
- OpenAI GPT-4/4-mini integration for natural language suggestions
- Generates personalized productivity schedules
- Provides contextual advice based on energy predictions
- Explains reasoning behind recommendations

#### UI/Visualization Module
- Real-time energy prediction graphs
- Historical trends in heart rate and sleep quality
- Daily alerts and notifications for optimal task scheduling
- Interactive timeline visualization

#### Backend (Optional - Firebase)
- Synchronizes data across devices
- Stores historical records
- Push notifications for schedule reminders

## Technical Stack

### Languages & Frameworks
- Java 8
- Android SDK
- TensorFlow Lite for ML
- Retrofit for API calls
- MPAndroidChart for visualization

### APIs & Services
- Google Fit API
- OpenAI API (GPT-4)
- Firebase (optional)

## Setup Instructions

### Prerequisites
1. Android Studio Arctic Fox or later
2. Android SDK API 28+
3. Google Fit account
4. OpenAI API key (optional, for LLM features)
5. Firebase project (optional, for backend features)

### Installation

1. Clone the repository:
```bash
git clone [repository-url]
cd FlowState
```

2. Open in Android Studio:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory

3. Configure Firebase (if using):
   - Download `google-services.json` from Firebase Console
   - Place it in `app/` directory

4. Configure API Keys:
   - Add your OpenAI API key in `app/src/main/java/com/flowstate/app/llm/LLMService.java`
   - Update `app/google-services.json` with your Firebase configuration

5. Sync Gradle:
   - Click "Sync Project with Gradle Files"
   - Wait for dependencies to download

6. Run the app:
   - Connect an Android device or start an emulator
   - Click "Run" or press Shift+F10

## Usage

### Initial Setup
1. Launch the app
2. Connect to Google Fit by tapping "Connect Google Fit"
3. Grant necessary permissions

### Measuring Cognitive Performance
1. **Typing Speed Test**:
   - Tap "Measure Typing Speed"
   - Type the displayed text as quickly and accurately as possible
   - View results: Words Per Minute (WPM) and Accuracy

2. **Reaction Time Test**:
   - Tap "Measure Reaction Time"
   - Wait for the button to turn green
   - Tap as quickly as possible when color changes
   - View your reaction time in milliseconds

### Viewing Energy Predictions
1. Tap "View Energy Prediction"
2. Tap "Load Predictions" to generate predictions based on your data
3. Review:
   - Predicted energy levels for the next 12 hours
   - Recommended activities for each time slot
   - General advice for the day

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/flowstate/app/
│   │   │   ├── data/
│   │   │   │   ├── models/          # Data models
│   │   │   │   └── collection/      # Data collection classes
│   │   │   ├── ml/                  # ML prediction module
│   │   │   ├── llm/                 # LLM integration
│   │   │   ├── ui/                  # UI activities
│   │   │   └── FlowStateApplication.java
│   │   ├── res/
│   │   │   ├── layout/              # XML layouts
│   │   │   └── values/              # Strings, colors, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Dependencies

### Core Android
- androidx.appcompat:appcompat
- com.google.android.material:material
- androidx.constraintlayout:constraintlayout

### Google Services
- com.google.android.gms:play-services-fitness
- com.google.android.gms:play-services-auth

### ML
- org.tensorflow:tensorflow-lite
- org.tensorflow:tensorflow-lite-support

### Networking
- com.squareup.retrofit2:retrofit
- com.squareup.retrofit2:converter-gson

### Firebase
- com.google.firebase:firebase-firestore
- com.google.firebase:firebase-auth
- com.google.firebase:firebase-database

### UI
- com.github.PhilJay:MPAndroidChart

## Important Notes

### Permissions
The app requires the following permissions:
- `INTERNET`: For API calls
- `ACCESS_NETWORK_STATE`: To check network connectivity
- `BODY_SENSORS`: For wearable device data
- `ACTIVITY_RECOGNITION`: For activity tracking

### Data Privacy
- All biometric data is processed locally on the device
- Google Fit integration requires explicit user consent
- Firebase is optional and can be disabled
- No personal data is shared with third parties without consent

### Known Limitations
1. ML model uses rule-based predictions initially (can be enhanced with TensorFlow Lite model)
2. Requires Google Fit account and wearable device for full functionality
3. OpenAI API integration requires valid API key
4. Some features require internet connection

## Future Enhancements
- [ ] Train custom TensorFlow Lite model with real user data
- [ ] Add ECG data support
- [ ] Implement offline mode
- [ ] Add widget for quick energy level viewing
- [ ] Support for additional wearable devices
- [ ] Advanced visualization with charts and graphs
- [ ] Export data functionality
- [ ] Machine learning model fine-tuning based on user feedback

## Troubleshooting

### Google Fit Connection Issues
- Ensure Google Play Services is up to date
- Verify Google account is signed in
- Check app permissions in device settings

### API Errors
- Verify API keys are correctly configured
- Check internet connection
- Review API quota/limits

### Build Errors
- Clean project: Build → Clean Project
- Invalidate caches: File → Invalidate Caches / Restart
- Sync Gradle: Tools → Sync Project with Gradle Files

## License
This project is developed for CP470 course requirements.

## Contact
For questions or issues, please contact the development team.

