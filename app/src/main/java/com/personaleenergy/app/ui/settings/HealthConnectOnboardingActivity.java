package com.personaleenergy.app.ui.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Health Connect Onboarding Activity
 * This activity is required by Health Connect to register the app.
 * Health Connect will launch this activity when needed for onboarding.
 */
public class HealthConnectOnboardingActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Health Connect will handle the onboarding flow
        // We just need this activity to exist for Health Connect to recognize our app
        // Finish immediately and redirect to settings
        finish();
        
        // Open settings activity
        android.content.Intent intent = new android.content.Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}

