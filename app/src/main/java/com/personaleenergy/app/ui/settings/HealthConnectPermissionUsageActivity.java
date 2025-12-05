package com.personaleenergy.app.ui.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Health Connect Permission Usage Activity
 * Required for Android 14+ to handle the privacy/rationale screen
 * when user taps the privacy link in Health Connect UI.
 * 
 * This activity is launched by Health Connect when the user wants to view
 * how the app uses health permissions.
 */
public class HealthConnectPermissionUsageActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Show a dialog explaining how FlowState uses health data
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("How FlowState Uses Health Data")
            .setMessage("FlowState uses your health data to:\n\n" +
                "• Predict your energy levels throughout the day\n" +
                "• Optimize your schedule based on when you're most productive\n" +
                "• Provide personalized insights about your health patterns\n\n" +
                "Your data is stored securely and only used within the app.\n\n" +
                "We access:\n" +
                "• Heart rate data - to understand your stress and energy levels\n" +
                "• Sleep data - to optimize your schedule around your sleep patterns")
            .setPositiveButton("OK", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
}

