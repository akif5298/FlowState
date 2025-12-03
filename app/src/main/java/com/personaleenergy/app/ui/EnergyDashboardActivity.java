package com.personaleenergy.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.flowstate.app.R;
import com.flowstate.services.GoogleFitManager;
import com.flowstate.workers.HeartRateSyncWorker;
import com.flowstate.workers.SleepSyncWorker;
import com.flowstate.workers.SyncScheduler;
import com.personaleenergy.app.ui.settings.SettingsActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;
import com.personaleenergy.app.ui.typing.TypingSpeedActivity;
import com.personaleenergy.app.ui.reaction.ReactionTimeActivity;
import com.personaleenergy.app.ui.EnergyPredictionActivity;

public class EnergyDashboardActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private TextView tvCurrentEnergy, tvEnergyLevel, tvAIInsight;
    private MaterialCardView cardGraph, cardCognitive;
    private Button btnSyncNow;
    private GoogleFitManager fitManager;
    private WorkManager workManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication
        com.flowstate.app.supabase.AuthService authService = new com.flowstate.app.supabase.AuthService(this);
        if (!authService.isAuthenticated()) {
            // Not authenticated, go to login
            startActivity(new Intent(this, com.flowstate.app.ui.LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_energy_dashboard);
        
        initializeViews();
        setupBottomNavigation();
        setupClickListeners();
        
        // Load initial data
        loadEnergyData();
    }
    
    private void initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        tvCurrentEnergy = findViewById(R.id.tvCurrentEnergy);
        tvEnergyLevel = findViewById(R.id.tvEnergyLevel);
        tvAIInsight = findViewById(R.id.tvAIInsight);
        cardGraph = findViewById(R.id.cardGraph);
        cardCognitive = findViewById(R.id.cardCognitive);
        // btnSyncNow may not exist in layout - check for resource existence first
        int syncNowId = getResources().getIdentifier("btnSyncNow", "id", getPackageName());
        btnSyncNow = syncNowId != 0 ? findViewById(syncNowId) : null;
        
        // Initialize managers
        fitManager = new GoogleFitManager(this);
        workManager = WorkManager.getInstance(this);
        
        // Set up sync button if present
        if (btnSyncNow != null) {
            btnSyncNow.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                syncNow();
            });
            
            // Update button state based on permissions
            boolean hasPermissions = fitManager.hasPermissions(this);
            btnSyncNow.setEnabled(hasPermissions);
            btnSyncNow.setVisibility(hasPermissions ? View.VISIBLE : View.GONE);
            
            // Ensure hourly sync is scheduled if permissions are granted
            if (hasPermissions) {
                SyncScheduler.scheduleHourlySync(this);
            }
        }
        
        // Null safety checks
        if (bottomNav == null) {
            android.util.Log.e("EnergyDashboard", "bottomNavigation not found in layout");
        }
    }
    
    private void setupBottomNavigation() {
        if (bottomNav == null) {
            return;
        }
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                // Already here, no action needed
                return true;
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
        
        // Set dashboard as selected
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }
    
    private void setupClickListeners() {
        // Typing test button
        View btnTypingTest = findViewById(R.id.btnTypingTest);
        if (btnTypingTest != null) {
            btnTypingTest.setOnClickListener(v -> {
                startActivity(new Intent(this, TypingSpeedActivity.class));
            });
        }
        
        // Reaction test button
        View btnReactionTest = findViewById(R.id.btnReactionTest);
        if (btnReactionTest != null) {
            btnReactionTest.setOnClickListener(v -> {
                startActivity(new Intent(this, ReactionTimeActivity.class));
            });
        }
        
        // Manual feedback slider - will be handled in layout
        // Graph click to navigate to Energy Prediction screen
        if (cardGraph != null) {
            cardGraph.setOnClickListener(v -> {
                startActivity(new Intent(this, EnergyPredictionActivity.class));
            });
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
        android.util.Log.d("EnergyDashboard", "Sync workers enqueued");
    }
    
    private void loadEnergyData() {
        // TODO: Load actual data from ML model
        if (tvCurrentEnergy != null) {
            tvCurrentEnergy.setText("75");
        }
        if (tvEnergyLevel != null) {
            tvEnergyLevel.setText("HIGH");
        }
        if (tvAIInsight != null) {
            tvAIInsight.setText("Energy dip expected in 25 min. Hydrate or take a short walk.");
        }
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back to login - exit app instead
        moveTaskToBack(true);
    }
}

