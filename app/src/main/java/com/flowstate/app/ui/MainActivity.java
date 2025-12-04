package com.flowstate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.flowstate.app.R;
import com.flowstate.services.HealthConnectManager;
import com.flowstate.services.DataChecker;

import com.flowstate.workers.HeartRateSyncWorker;
import com.flowstate.workers.SleepSyncWorker;
import com.flowstate.workers.SyncScheduler;
import com.flowstate.app.ui.typing.TypingSpeedActivity;
import com.flowstate.app.ui.reaction.ReactionTimeActivity;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.OnboardingActivity;
import com.flowstate.services.DataChecker.DataCheckCallback;

public class MainActivity extends AppCompatActivity {
    
    private static final int GOOGLE_FIT_PERMISSION_REQUEST = 1001;
    
    private HealthConnectManager hcManager;
    private Button btnConnectFit, btnSyncNow, btnTyping, btnReaction, btnEnergy;
    private TextView tvStatus;
    private WorkManager workManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize HealthConnectManager early to avoid NPE in onResume
        hcManager = new HealthConnectManager(this);
        
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
                            // Has data, continue with normal flow
                            initializeMainActivity();
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
            // Return early - will initialize main activity if data exists
            return;
        }
        
        // Onboarding already complete, initialize normally
        initializeMainActivity();
    }
    
    private void initializeMainActivity() {
        setContentView(R.layout.activity_main);
        
        initializeViews();
        
        updateConnectionStatus();
        animateViews();
        
        btnConnectFit.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            // Connect fit moved to settings, this button might be hidden or reused
        });
        
        // Buttons removed from layout, so no listeners needed for them here
        
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
        // animateCardView(R.id.cardTyping, 200);
        // animateCardView(R.id.cardReaction, 300);
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
        // btnTyping = findViewById(R.id.btnTypingSpeed); // Removed
        // btnReaction = findViewById(R.id.btnReactionTime); // Removed
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
    
    private void syncNow() {
        if (!hcManager.isAvailable()) {
            Toast.makeText(this, R.string.health_connect_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Enqueue sync workers
        workManager.enqueue(HeartRateSyncWorker.createWorkRequest());
        workManager.enqueue(SleepSyncWorker.createWorkRequest());
        
        Toast.makeText(this, R.string.syncing_data, Toast.LENGTH_SHORT).show();
        android.util.Log.d("MainActivity", "Sync workers enqueued");
    }
    
    private void updateConnectionStatus() {
        if (hcManager == null || tvStatus == null) {
            return;
        }

        // Simple check for now
        boolean isAvailable = hcManager.isAvailable();
        
        if (isAvailable) {
            tvStatus.setText(R.string.hc_available);
            btnConnectFit.setText(R.string.hc_debug);
            if (btnSyncNow != null) {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setVisibility(View.VISIBLE);
            }
            // Schedule hourly sync
            SyncScheduler.scheduleHourlySync(this);
        } else {
            tvStatus.setText(R.string.hc_unavailable);
            btnConnectFit.setEnabled(false);
             if (btnSyncNow != null) {
                btnSyncNow.setEnabled(false);
                btnSyncNow.setVisibility(View.GONE);
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

