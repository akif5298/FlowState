package com.personaleenergy.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.personaleenergy.app.R;
import com.personaleenergy.app.ui.manual.ManualDataEntryActivity;

/**
 * Onboarding activity for new users
 * 
 * Offers manual data entry
 */
public class OnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "OnboardingActivity";
    private static final String PREFS_NAME = "personaleenergy_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    
    private TextView tvTitle, tvDescription, tvStatus;
    private Button btnManualEntry;
    private TextView btnSkip;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        initializeViews();
        showOnboardingFlow();
    }
    
    private void initializeViews() {
        tvTitle = findViewById(R.id.tvOnboardingTitle);
        tvDescription = findViewById(R.id.tvOnboardingDescription);
        tvStatus = findViewById(R.id.tvOnboardingStatus);
        btnManualEntry = findViewById(R.id.btnManualEntry);
        btnSkip = findViewById(R.id.btnSkipOnboarding);
        
        if (btnManualEntry != null) {
            btnManualEntry.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                startManualDataEntry();
            });
        }
        
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> {
                skipOnboarding();
            });
        }
    }

    /**
     * Show onboarding flow for new users
     */
    private void showOnboardingFlow() {
        if (tvTitle != null) {
            tvTitle.setText("Welcome to Personal Energy Cycle Predictor!");
        }
        if (tvDescription != null) {
            tvDescription.setText("To generate accurate energy predictions, we need some data about you. " +
                    "You can enter data manually.");
        }
        updateStatus("Let's get started!");

        if (btnManualEntry != null) {
            btnManualEntry.setVisibility(View.VISIBLE);
        }
        if (btnSkip != null) {
            btnSkip.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Start manual data entry activity
     */
    private void startManualDataEntry() {
        Intent intent = new Intent(this, ManualDataEntryActivity.class);
        startActivity(intent);
    }
    
    /**
     * Skip onboarding and go to main activity
     */
    private void skipOnboarding() {
        markOnboardingComplete();
        navigateToMain();
    }
    
    private void updateStatus(String message) {
        if (tvStatus != null) {
            tvStatus.setText(message);
        }
    }
    
    private void markOnboardingComplete() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply();
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    /**
     * Check if onboarding is complete
     */
    public static boolean isOnboardingComplete(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }
}
