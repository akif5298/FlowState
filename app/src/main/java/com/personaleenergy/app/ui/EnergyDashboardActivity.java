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
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.repository.EnergyPredictionRepository;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.EnergyLevel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import android.view.Menu;
import android.view.MenuItem;
// import com.flowstate.app.utils.HelpDialogHelper; // Removed - class deleted
import com.personaleenergy.app.data.collection.HealthConnectManager;
import com.flowstate.app.supabase.repository.BiometricDataRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EnergyDashboardActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private TextView tvAIInsight;
    private MaterialCardView cardGraph, cardCognitive;
    private Button btnSyncNow;
    private GoogleFitManager fitManager;
    private WorkManager workManager;
    private LineChart energyChart;
    private EnergyPredictionRepository energyRepo;
    private SupabaseClient supabaseClient;
    private SimpleDateFormat timeFormat;
    private HealthConnectManager healthConnectManager;
    
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
        
        // Sync Health Connect data on app open (if permissions granted)
        syncHealthConnectDataOnStartup();
        
        // Load initial data
        loadEnergyData();
    }
    
    private void initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        tvAIInsight = findViewById(R.id.tvAIInsight);
        cardGraph = findViewById(R.id.cardGraph);
        cardCognitive = findViewById(R.id.cardCognitive);
        energyChart = findViewById(R.id.energyChart);
        // btnSyncNow may not exist in layout - check for resource existence first
        int syncNowId = getResources().getIdentifier("btnSyncNow", "id", getPackageName());
        btnSyncNow = syncNowId != 0 ? findViewById(syncNowId) : null;
        
        // Initialize managers
        fitManager = new GoogleFitManager(this);
        workManager = WorkManager.getInstance(this);
        supabaseClient = SupabaseClient.getInstance(this);
        energyRepo = new EnergyPredictionRepository(this);
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        healthConnectManager = new HealthConnectManager(this);
        
        // Setup chart
        if (energyChart != null) {
            setupEnergyChart();
        }
        
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
        
        // View Energy Details button
        View btnViewEnergyDetails = findViewById(R.id.btnViewEnergyDetails);
        if (btnViewEnergyDetails != null) {
            btnViewEnergyDetails.setOnClickListener(v -> {
                startActivity(new Intent(this, EnergyPredictionActivity.class));
            });
        }
        
        // View Cognitive Details button
        View btnViewCognitiveDetails = findViewById(R.id.btnViewCognitiveDetails);
        if (btnViewCognitiveDetails != null) {
            btnViewCognitiveDetails.setOnClickListener(v -> {
                // Navigate to Data Logs with focus on cognitive section
                Intent intent = new Intent(this, DataLogsActivity.class);
                startActivity(intent);
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
    
    private void setupEnergyChart() {
        energyChart.getDescription().setEnabled(false);
        energyChart.setTouchEnabled(true);
        energyChart.setDragEnabled(true);
        energyChart.setScaleEnabled(true);
        energyChart.setPinchZoom(true);
        energyChart.setBackgroundColor(getResources().getColor(R.color.surface_variant, getTheme()));
        
        // X Axis
        XAxis xAxis = energyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new TimeValueFormatter());
        xAxis.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        
        // Y Axis
        YAxis leftAxis = energyChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        
        YAxis rightAxis = energyChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        energyChart.getLegend().setEnabled(false);
    }
    
    private class TimeValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            int hour = (int) value;
            String period = hour < 12 ? "AM" : "PM";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            return String.format(Locale.getDefault(), "%d:00 %s", displayHour, period);
        }
    }
    
    /**
     * Sync Health Connect data on app startup if permissions are granted
     */
    private void syncHealthConnectDataOnStartup() {
        // Check if Health Connect is available and permissions are granted
        if (healthConnectManager == null) {
            healthConnectManager = new HealthConnectManager(this);
        }
        
        if (!healthConnectManager.isAvailable()) {
            return; // Health Connect not available, skip sync
        }
        
        // Check permissions asynchronously
        healthConnectManager.hasPermissionsJava().thenAccept(hasPermissions -> {
            if (hasPermissions) {
                // Permissions granted, sync new data
                android.util.Log.d("EnergyDashboard", "Health Connect permissions granted, syncing new data...");
                healthConnectManager.readNewBiometricDataSinceLastSync(
                    new HealthConnectManager.BiometricCallback() {
                        @Override
                        public void onSuccess(java.util.List<com.flowstate.app.data.models.BiometricData> data) {
                            if (data != null && !data.isEmpty()) {
                                android.util.Log.d("EnergyDashboard", "Found " + data.size() + " new records from Health Connect");
                                // Save to Supabase
                                saveHealthConnectDataToSupabase(data);
                            } else {
                                android.util.Log.d("EnergyDashboard", "No new data from Health Connect");
                            }
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            android.util.Log.e("EnergyDashboard", "Failed to sync Health Connect data on startup", e);
                            // Don't show error to user on startup - silent sync
                        }
                    }
                );
            } else {
                android.util.Log.d("EnergyDashboard", "Health Connect permissions not granted, skipping sync");
            }
        });
    }
    
    /**
     * Save Health Connect data to Supabase
     */
    private void saveHealthConnectDataToSupabase(java.util.List<com.flowstate.app.data.models.BiometricData> data) {
        // Get user ID
        String userId = supabaseClient.getUserId();
        if (userId == null || userId.isEmpty()) {
            android.util.Log.e("EnergyDashboard", "Cannot save data: user not authenticated");
            return;
        }
        
        // Use BiometricDataRepository to save data
        com.flowstate.app.supabase.repository.BiometricDataRepository repo = 
            new com.flowstate.app.supabase.repository.BiometricDataRepository(this);
        
        final int[] successCount = {0};
        final int[] errorCount = {0};
        final int totalCount = data.size();
        
        for (com.flowstate.app.data.models.BiometricData biometric : data) {
            repo.upsertBiometricData(userId, biometric, new com.flowstate.app.supabase.repository.BiometricDataRepository.DataCallback() {
                @Override
                public void onSuccess(Object result) {
                    successCount[0]++;
                    android.util.Log.d("EnergyDashboard", "Saved biometric data to Supabase (" + successCount[0] + "/" + totalCount + ")");
                    
                    if (successCount[0] + errorCount[0] >= totalCount) {
                        android.util.Log.d("EnergyDashboard", "Health Connect sync complete: " + successCount[0] + " saved, " + errorCount[0] + " errors");
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    errorCount[0]++;
                    android.util.Log.e("EnergyDashboard", "Failed to save biometric data to Supabase", error);
                    
                    if (successCount[0] + errorCount[0] >= totalCount) {
                        android.util.Log.d("EnergyDashboard", "Health Connect sync complete: " + successCount[0] + " saved, " + errorCount[0] + " errors");
                    }
                }
            });
        }
    }
    
    private void loadEnergyData() {
        String userId = supabaseClient.getUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        // Load today's energy predictions
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();
        
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date todayEnd = cal.getTime();
        
        energyRepo.getEnergyPredictions(userId, todayStart, todayEnd, new EnergyPredictionRepository.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                runOnUiThread(() -> {
                    @SuppressWarnings("unchecked")
                    List<EnergyPrediction> predictions = (List<EnergyPrediction>) data;
                    if (predictions != null && !predictions.isEmpty()) {
                        displayEnergyChart(predictions);
                        updateAIInsight(predictions);
                    } else {
                        // No predictions yet
                        if (energyChart != null) {
                            energyChart.clear();
                        }
                        if (tvAIInsight != null) {
                            tvAIInsight.setText("No energy predictions yet. Generate predictions to see your energy forecast.");
                        }
                    }
                });
            }
            
            @Override
            public void onError(Throwable error) {
                android.util.Log.e("EnergyDashboard", "Error loading energy predictions", error);
            }
        });
    }
    
    private void displayEnergyChart(List<EnergyPrediction> predictions) {
        if (energyChart == null || predictions == null || predictions.isEmpty()) {
            return;
        }
        
        // Use a map to handle duplicate hours (take the latest prediction for each hour)
        Map<Integer, Entry> hourEntryMap = new HashMap<>();
        
        for (EnergyPrediction pred : predictions) {
            if (pred == null || pred.getTimestamp() == null) {
                continue;
            }
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            // Convert energy level to numeric value for graph
            float energyValue = 0f;
            switch (pred.getPredictedLevel()) {
                case HIGH:
                    energyValue = 80f + (float)(pred.getConfidence() * 20f); // 80-100
                    break;
                case MEDIUM:
                    energyValue = 40f + (float)(pred.getConfidence() * 40f); // 40-80
                    break;
                case LOW:
                    energyValue = (float)(pred.getConfidence() * 40f); // 0-40
                    break;
            }
            
            // Store entry, overwriting if hour already exists (takes latest)
            hourEntryMap.put(hour, new Entry(hour, energyValue));
        }
        
        if (hourEntryMap.isEmpty()) {
            return;
        }
        
        // Convert map to sorted list
        List<Entry> entries = new ArrayList<>(hourEntryMap.values());
        Collections.sort(entries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
        
        if (entries.isEmpty()) {
            return;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Energy Level");
        dataSet.setColor(getResources().getColor(R.color.primary, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(getResources().getColor(R.color.primary, getTheme()));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        dataSet.setValueTextSize(10f);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(getResources().getColor(R.color.primary, getTheme()));
        dataSet.setDrawFilled(true);
        dataSet.setDrawValues(false); // Disable value labels to prevent rendering issues
        
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        
        LineData lineData = new LineData(dataSets);
        energyChart.setData(lineData);
        energyChart.invalidate();
    }
    
    private void updateAIInsight(List<EnergyPrediction> predictions) {
        if (tvAIInsight == null || predictions == null || predictions.isEmpty()) {
            return;
        }
        
        // Find current time and next few hours
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        
        // Find next prediction
        EnergyPrediction nextPred = null;
        for (EnergyPrediction pred : predictions) {
            Calendar predCal = Calendar.getInstance();
            predCal.setTime(pred.getTimestamp());
            int predHour = predCal.get(Calendar.HOUR_OF_DAY);
            if (predHour > currentHour) {
                nextPred = pred;
                break;
            }
        }
        
        if (nextPred != null) {
            String time = timeFormat.format(nextPred.getTimestamp());
            String insight = String.format(Locale.getDefault(),
                "Your energy is predicted to be %s at %s. %s",
                nextPred.getPredictedLevel().toString(),
                time,
                nextPred.getPredictedLevel() == EnergyLevel.LOW ? 
                    "Consider taking a break or a short nap." :
                    nextPred.getPredictedLevel() == EnergyLevel.HIGH ?
                    "Perfect time for demanding tasks!" :
                    "Good time for moderate activities.");
            tvAIInsight.setText(insight);
        } else {
            tvAIInsight.setText("Your energy predictions for today are loaded. Check the graph above for details.");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload energy data when returning to dashboard
        loadEnergyData();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back to login - exit app instead
        moveTaskToBack(true);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            // HelpDialogHelper removed - show simple dialog instead
            new android.app.AlertDialog.Builder(this)
                .setTitle("Energy Dashboard Help")
                .setMessage("This dashboard shows your current energy level and recent activity. Monitor your energy throughout the day to optimize your productivity.")
                .setPositiveButton("OK", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

