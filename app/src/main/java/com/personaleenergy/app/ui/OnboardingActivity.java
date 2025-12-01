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
import com.personaleenergy.app.services.GoogleFitManager;
import com.personaleenergy.app.ui.manual.ManualDataEntryActivity;
import com.personaleenergy.app.workers.HeartRateSyncWorker;
import com.personaleenergy.app.workers.SleepSyncWorker;
import com.personaleenergy.app.workers.SyncScheduler;

/**
 * Onboarding activity for new users
 * 
 * Requests Google Fit permissions,
 * and offers manual data entry if declined
 */
public class OnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "OnboardingActivity";
    private static final String PREFS_NAME = "personaleenergy_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    private static final int GOOGLE_FIT_PERMISSION_REQUEST = 2001;
    
    private GoogleFitManager fitManager;
    private TextView tvTitle, tvDescription, tvStatus;
    private Button btnConnectWearable, btnManualEntry;
    private TextView btnSkip;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        
        mainHandler = new Handler(Looper.getMainLooper());
        fitManager = new GoogleFitManager(this);
        
        initializeViews();
        showOnboardingFlow();
    }
    
    private void initializeViews() {
        tvTitle = findViewById(R.id.tvOnboardingTitle);
        tvDescription = findViewById(R.id.tvOnboardingDescription);
        tvStatus = findViewById(R.id.tvOnboardingStatus);
        btnConnectWearable = findViewById(R.id.btnConnectWearable);
        btnManualEntry = findViewById(R.id.btnManualEntry);
        btnSkip = findViewById(R.id.btnSkipOnboarding);
        
        // Set up button listeners
        if (btnConnectWearable != null) {
            btnConnectWearable.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                requestWearablePermissions();
            });
        }
        
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
                    "You can connect a wearable device or enter data manually.");
        }
        updateStatus("Let's get started!");
        
        // Show buttons
        if (btnConnectWearable != null) {
            btnConnectWearable.setVisibility(View.VISIBLE);
        }
        if (btnManualEntry != null) {
            btnManualEntry.setVisibility(View.VISIBLE);
        }
        if (btnSkip != null) {
            btnSkip.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Request permissions for wearable device (Google Fit)
     */
    private void requestWearablePermissions() {
        updateStatus("Requesting permissions...");
        
        if (!fitManager.hasPermissions(this)) {
            // Request permissions
            fitManager.requestPermissions(this, GOOGLE_FIT_PERMISSION_REQUEST);
        } else if (!fitManager.isSignedIn()) {
            // Start sign-in flow
            Intent signInIntent = fitManager.getSignInClient().getSignInIntent();
            startActivityForResult(signInIntent, GOOGLE_FIT_PERMISSION_REQUEST);
        } else {
            // Already connected
            updateStatus("Already connected to Google Fit!");
            Toast.makeText(this, "Connected to Google Fit!", Toast.LENGTH_SHORT).show();
            onPermissionsGranted();
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
    
    /**
     * Called when permissions are granted
     */
    private void onPermissionsGranted() {
        updateStatus("Permissions granted! Setting up data sync...");
        
        // Schedule hourly sync
        SyncScheduler.scheduleHourlySync(this);
        
        // Trigger immediate sync
        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(this);
        workManager.enqueue(HeartRateSyncWorker.createWorkRequest());
        workManager.enqueue(SleepSyncWorker.createWorkRequest());
        
        Toast.makeText(this, "Connected! Data will sync automatically.", Toast.LENGTH_LONG).show();
        
        // Wait a moment then check data again
        mainHandler.postDelayed(() -> {
            // Mark onboarding as complete after successful connection
            markOnboardingComplete();
            navigateToMain();
        }, 2000);
    }
    
    /**
     * Called when user declines permissions
     */
    private void onPermissionsDenied() {
        updateStatus("No worries! You can enter data manually.");
        
        if (tvDescription != null) {
            tvDescription.setText("You can enter your heart rate, sleep, and cognitive test data manually. " +
                    "This will help us generate accurate predictions.");
        }
        
        // Show manual entry button more prominently
        if (btnManualEntry != null) {
            btnManualEntry.setVisibility(View.VISIBLE);
            btnManualEntry.setText("Enter Data Manually");
        }
        
        if (btnConnectWearable != null) {
            btnConnectWearable.setVisibility(View.GONE);
        }
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == GOOGLE_FIT_PERMISSION_REQUEST) {
            com.google.android.gms.auth.api.signin.GoogleSignInAccount account = 
                com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this);
            
            if (account != null) {
                // Request fitness permissions if not already granted
                if (!fitManager.hasPermissions(this)) {
                    fitManager.requestPermissions(this, GOOGLE_FIT_PERMISSION_REQUEST);
                } else {
                    // Permissions granted
                    onPermissionsGranted();
                }
            } else {
                // User declined or cancelled
                onPermissionsDenied();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == GOOGLE_FIT_PERMISSION_REQUEST) {
            if (fitManager.hasPermissions(this)) {
                onPermissionsGranted();
            } else {
                onPermissionsDenied();
            }
        }
    }
    
    /**
     * Check if onboarding is complete
     */
    public static boolean isOnboardingComplete(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }
}
