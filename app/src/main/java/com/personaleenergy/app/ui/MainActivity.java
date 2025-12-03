package com.personaleenergy.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.flowstate.app.R;
import com.flowstate.services.GoogleFitManager;
import com.flowstate.services.DataChecker;
import com.flowstate.workers.HeartRateSyncWorker;
import com.flowstate.workers.SleepSyncWorker;
import com.flowstate.workers.SyncScheduler;
import com.personaleenergy.app.ui.typing.TypingSpeedActivity;
import com.personaleenergy.app.ui.reaction.ReactionTimeActivity;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.OnboardingActivity;
import com.flowstate.services.DataChecker.DataCheckCallback;

public class MainActivity extends AppCompatActivity {
    
    private static final int GOOGLE_FIT_PERMISSION_REQUEST = 1001;
    
    private GoogleFitManager fitManager;
    private Button btnConnectFit, btnSyncNow, btnTyping, btnReaction, btnEnergy;
    private TextView tvStatus;
    private WorkManager workManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication first
        com.flowstate.app.supabase.AuthService authService = new com.flowstate.app.supabase.AuthService(this);
        if (!authService.isAuthenticated()) {
            // Not authenticated, go to login
            startActivity(new Intent(this, com.flowstate.app.ui.LoginActivity.class));
            finish();
            return;
        }
        
        // Check if onboarding is needed
        if (!OnboardingActivity.isOnboardingComplete(this)) {
            // Check if user has minimum data
            DataChecker dataChecker = new DataChecker(this);
            dataChecker.hasMinimumDataForPredictions(new DataCheckCallback() {
                @Override
                public void onResult(DataChecker.DataCheckResult result) {
                    runOnUiThread(() -> {
                        if (!result.hasAnyData) {
                            // No data, show onboarding
                            startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
                            finish();
                        } else {
                            // Has data, redirect to dashboard
                            startActivity(new Intent(MainActivity.this, EnergyDashboardActivity.class));
                            finish();
                        }
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        // On error, show onboarding
                        startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
                        finish();
                    });
                }
            });
            // Return early - will navigate based on data check
            return;
        }
        
        // Onboarding complete and authenticated, redirect to dashboard
        startActivity(new Intent(this, EnergyDashboardActivity.class));
        finish();
    }
    
    private void initializeMainActivity() {
        setContentView(R.layout.activity_main);
        
        initializeViews();
        fitManager = new GoogleFitManager(this);
        
        updateConnectionStatus();
        animateViews();
        
        btnConnectFit.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            connectGoogleFit();
        });
        
        btnTyping.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, TypingSpeedActivity.class));
        });
        
        btnReaction.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, ReactionTimeActivity.class));
        });
        
        btnEnergy.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, EnergyDashboardActivity.class));
            finish();
        });
    }
    
    private void animateViews() {
        // Animate logo icons
        View logoIcons = findViewById(R.id.logoIcons);
        if (logoIcons != null) {
            logoIcons.setAlpha(0f);
            logoIcons.setTranslationY(-30f);
            logoIcons.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
        
        // Stagger animation for cards
        animateCardView(R.id.cardConnectFit, 100);
        animateCardView(R.id.cardTyping, 200);
        animateCardView(R.id.cardReaction, 300);
        animateCardView(R.id.cardEnergy, 400);
        animateCardView(R.id.cardInfo, 500);
    }
    
    private void animateCardView(int viewId, long delay) {
        View card = findViewById(viewId);
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(40f);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(delay)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }
    
    private void initializeViews() {
        btnConnectFit = findViewById(R.id.btnConnectFit);
        // btnSyncNow may not exist in layout - check for resource existence first
        int syncNowId = getResources().getIdentifier("btnSyncNow", "id", getPackageName());
        btnSyncNow = syncNowId != 0 ? findViewById(syncNowId) : null;
        btnTyping = findViewById(R.id.btnTypingSpeed);
        btnReaction = findViewById(R.id.btnReactionTime);
        btnEnergy = findViewById(R.id.btnEnergyPrediction);
        tvStatus = findViewById(R.id.tvStatus);
        
        // Initialize WorkManager
        workManager = WorkManager.getInstance(this);
        
        // Set up sync button click listener
        if (btnSyncNow != null) {
            btnSyncNow.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                syncNow();
            });
        }
    }
    
    private void connectGoogleFit() {
        if (!fitManager.hasPermissions(this)) {
            // Request permissions
            fitManager.requestPermissions(this, GOOGLE_FIT_PERMISSION_REQUEST);
        } else if (!fitManager.isSignedIn()) {
            // Start sign-in flow
            Intent signInIntent = fitManager.getSignInClient().getSignInIntent();
            startActivityForResult(signInIntent, GOOGLE_FIT_PERMISSION_REQUEST);
        } else {
            Toast.makeText(this, "Already connected to Google Fit", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void syncNow() {
        if (!fitManager.hasPermissions(this)) {
            Toast.makeText(this, "Please connect to Google Fit first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Enqueue sync workers
        workManager.enqueue(HeartRateSyncWorker.createWorkRequest());
        workManager.enqueue(SleepSyncWorker.createWorkRequest());
        
        Toast.makeText(this, "Syncing health data...", Toast.LENGTH_SHORT).show();
        android.util.Log.d("MainActivity", "Sync workers enqueued");
    }
    
    private void updateConnectionStatus() {
        boolean hasPermissions = fitManager.hasPermissions(this);
        if (hasPermissions) {
            tvStatus.setText("Connected to Google Fit - Hourly sync active");
            btnConnectFit.setText("🔗 Disconnect");
            if (btnSyncNow != null) {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setVisibility(View.VISIBLE);
            }
            // Schedule hourly sync when connected
            SyncScheduler.scheduleHourlySync(this);
        } else {
            tvStatus.setText("Connect to sync biometric data");
            btnConnectFit.setText("🔗 Connect Google Fit");
            if (btnSyncNow != null) {
                btnSyncNow.setEnabled(false);
                btnSyncNow.setVisibility(View.GONE);
            }
            // Cancel hourly sync when disconnected
            SyncScheduler.cancelHourlySync(this);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == GOOGLE_FIT_PERMISSION_REQUEST) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                // Request fitness permissions if not already granted
                if (!fitManager.hasPermissions(this)) {
                    fitManager.requestPermissions(this, GOOGLE_FIT_PERMISSION_REQUEST);
                } else {
                    // Permissions granted, update status and schedule hourly sync
                    updateConnectionStatus();
                    // Trigger immediate sync on first connect
                    syncNow();
                }
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update connection status when activity resumes
        updateConnectionStatus();
    }
}

