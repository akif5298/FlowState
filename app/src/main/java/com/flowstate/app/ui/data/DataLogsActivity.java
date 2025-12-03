package com.flowstate.app.ui.data;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.settings.SettingsActivity;
import com.flowstate.app.ui.insights.WeeklyInsightsActivity;
import com.flowstate.app.ui.charts.ChartManager;
import com.github.mikephil.charting.charts.LineChart;
import com.flowstate.services.HealthDataAggregator;
import com.flowstate.services.HealthConnectManager;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.app.data.models.HealthDataSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataLogsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private LineChart chartHR;
    // private BarChart chartSleep; // Removed
    private LineChart chartCognitive;
    private LineChart chartReaction;
    private TextView tvWeather, tvAISummary;
    private TextView tvSleepScore;
    private TextView tvLastLongSleep, tvHoursAwake; // Added
    
    // Activity & Vitals Metrics
    private TextView tvAvgHR, tvRestingHR, tvAvgHRV;
    private TextView tvSteps, tvDistance, tvActiveCal, tvTotalCal, tvFloors, tvElevation;
    private TextView tvBP, tvSpO2, tvRespRate, tvGlucose, tvWeight, tvVo2Max;
    
    private HealthDataAggregator dataAggregator;
    private HealthConnectManager healthConnectManager;
    private AppDb database;
    private ExecutorService executor;
    
    private static final String TAG = "DataLogsActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_logs);
        
        initializeViews();
        setupCharts();
        
        dataAggregator = new HealthDataAggregator(this);
        healthConnectManager = new HealthConnectManager(this);
        database = AppDb.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        
        setupBottomNavigation();
        loadData();
    }
    
    private void initializeViews() {
        chartHR = findViewById(R.id.chartHR);
        // chartSleep = findViewById(R.id.chartSleep);
        chartCognitive = findViewById(R.id.chartCognitive);
        chartReaction = findViewById(R.id.chartReaction);
        tvWeather = findViewById(R.id.tvWeather);
        tvAISummary = findViewById(R.id.tvAISummary);
        tvSleepScore = findViewById(R.id.tvSleepScore);
        
        tvLastLongSleep = findViewById(R.id.tvLastLongSleep);
        tvHoursAwake = findViewById(R.id.tvHoursAwake);
        
        // HR
        tvAvgHR = findViewById(R.id.tvAvgHR);
        tvRestingHR = findViewById(R.id.tvRestingHR);
        tvAvgHRV = findViewById(R.id.tvAvgHRV);
        
        // Activity
        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        tvActiveCal = findViewById(R.id.tvActiveCal);
        tvTotalCal = findViewById(R.id.tvTotalCal);
        tvFloors = findViewById(R.id.tvFloors);
        tvElevation = findViewById(R.id.tvElevation);
        
        // Vitals
        tvBP = findViewById(R.id.tvBP);
        tvSpO2 = findViewById(R.id.tvSpO2);
        tvRespRate = findViewById(R.id.tvRespRate);
        tvGlucose = findViewById(R.id.tvGlucose);
        tvWeight = findViewById(R.id.tvWeight);
        tvVo2Max = findViewById(R.id.tvVo2Max);
    }
    
    private void setupCharts() {
        ChartManager.setupLineChart(chartHR, "Heart Rate");
        // ChartManager.setupBarChart(chartSleep, "Sleep Duration");
        ChartManager.setupLineChart(chartCognitive, "Typing Speed");
        ChartManager.setupLineChart(chartReaction, "Reaction Time");
    }
    
    private void loadData() {
        executor.execute(() -> {
            // 0. First sync sleep data from Health Connect (last 7 days)
            syncSleepFromHealthConnect();
            
            // 1. Collect summary data for scalar values
            HealthDataSummary summary = dataAggregator.createHealthSummary(24);
            
            // 2. Collect time-series data for charts
            Map<String, List<Double>> timeSeriesData = dataAggregator.collectHealthData(24);
            
            // 3. Get all sleep records for custom metric
            List<SleepLocal> allSleep = database.sleepDao().getAll();
            
            runOnUiThread(() -> {
                updateMetrics(summary);
                updateCharts(timeSeriesData);
                calculateAndShowSleepMetrics(allSleep);
            });
        });
    }

    private void calculateAndShowSleepMetrics(List<SleepLocal> allSleep) {
        if (allSleep == null || allSleep.isEmpty()) {
            tvLastLongSleep.setText("Last Sleep > 2.5h: No data");
            tvHoursAwake.setText("Awake for: --");
            return;
        }

        // Sort descending by end time
        Collections.sort(allSleep, (o1, o2) -> Long.compare(o2.sleep_end != null ? o2.sleep_end : 0, o1.sleep_end != null ? o1.sleep_end : 0));

        SleepLocal targetSleep = null;
        for (SleepLocal sleep : allSleep) {
            if (sleep.duration != null && sleep.duration > 150) { // > 2.5 hours (150 mins)
                targetSleep = sleep;
                break;
            }
        }

        if (targetSleep != null && targetSleep.sleep_end != null) {
            // Found it
            long durationMs = targetSleep.duration * 60 * 1000L;
            long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
            
            String durationStr = String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            String dateStr = dateFormat.format(new Date(targetSleep.sleep_end));
            
            tvLastLongSleep.setText("Last Sleep > 2.5h: " + durationStr + " (ended " + dateStr + ")");
            
            long now = System.currentTimeMillis();
            long diffMs = now - targetSleep.sleep_end;
            if (diffMs < 0) diffMs = 0;
            
            double awakeHours = diffMs / (1000.0 * 60.0 * 60.0);
            tvHoursAwake.setText(String.format(Locale.getDefault(), "Awake for: %.1f hours", awakeHours));
        } else {
            tvLastLongSleep.setText("Last Sleep > 2.5h: None found recently");
            tvHoursAwake.setText("Awake for: --");
        }
    }
    
    /**
     * Syncs sleep data from Health Connect for the last 7 days and stores it in the local DB.
     */
    private void syncSleepFromHealthConnect() {
        if (!healthConnectManager.isAvailable()) {
            Log.w(TAG, "Health Connect not available, skipping sleep sync");
            return;
        }
        
        try {
            Instant end = Instant.now();
            Instant start = end.minus(7, ChronoUnit.DAYS);
            
            Log.d(TAG, "Syncing sleep data from Health Connect (last 7 days)...");
            
            // Get sleep data from Health Connect
            List<SleepLocal> sleepList = healthConnectManager.readSleep(start, end)
                    .get(30, TimeUnit.SECONDS);
            
            if (sleepList != null && !sleepList.isEmpty()) {
                Log.d(TAG, "Found " + sleepList.size() + " sleep records from Health Connect");
                
                // Insert into local DB
                for (SleepLocal sleep : sleepList) {
                    try {
                        database.sleepDao().insert(sleep);
                    } catch (Exception e) {
                        // Likely a duplicate entry, ignore
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing sleep from Health Connect", e);
        }
    }
    
    private void updateMetrics(HealthDataSummary summary) {
        // Heart & Recovery
        tvAvgHR.setText(formatValue(summary.getAvgHeartRate(), "%.0f", "bpm"));
        tvRestingHR.setText(formatValue(summary.getRestingHeartRate(), "%.0f", "bpm"));
        tvAvgHRV.setText(formatValue(summary.getAvgHRV(), "%.0f", "ms"));
        
        // Activity
        tvSteps.setText(formatValue(summary.getTodaySteps(), "%d", ""));
        tvDistance.setText(formatValue(summary.getDistance(), "%.1f", "m"));
        tvActiveCal.setText(formatValue(summary.getActiveCaloriesBurned(), "%.0f", "kcal"));
        tvTotalCal.setText(formatValue(summary.getTotalCaloriesBurned(), "%.0f", "kcal"));
        tvFloors.setText(formatValue(summary.getFloorsClimbed(), "%d", ""));
        tvElevation.setText(formatValue(summary.getElevationGained(), "%.1f", "m"));
        
        // Vitals
        if (summary.getBloodPressureSystolic() != null && summary.getBloodPressureDiastolic() != null) {
            tvBP.setText(String.format(Locale.getDefault(), "%.0f/%.0f", summary.getBloodPressureSystolic(), summary.getBloodPressureDiastolic()));
        } else {
            tvBP.setText("--/--");
        }
        tvSpO2.setText(formatValue(summary.getOxygenSaturation(), "%.0f", "%"));
        tvRespRate.setText(formatValue(summary.getRespiratoryRate(), "%.0f", "rpm"));
        tvGlucose.setText(formatValue(summary.getBloodGlucose(), "%.1f", "mmol/L"));
        tvWeight.setText(formatValue(summary.getWeight(), "%.1f", "kg"));
        tvVo2Max.setText(formatValue(summary.getVo2Max(), "%.1f", ""));
        
        // Weather & Summary (if available in summary object or fetched elsewhere)
        if (summary.getWeatherTemperature() != null) {
            tvWeather.setText(String.format(Locale.getDefault(), "%.1f°C", summary.getWeatherTemperature()));
        }
        
        // Last night's sleep hours (Summary usually takes the LAST ended sleep)
        if (summary.getLastNightSleepHours() != null && summary.getLastNightSleepHours() > 0) {
            tvSleepScore.setText(String.format(Locale.getDefault(), "%.1f hrs", summary.getLastNightSleepHours()));
        } else {
            tvSleepScore.setText("-- hrs");
        }
    }
    
    private String formatValue(Number value, String format, String unit) {
        if (value == null || (value instanceof Double && (Double) value == 0.0) || (value instanceof Integer && (Integer) value == 0)) {
            return "-- " + unit;
        }
        return String.format(Locale.getDefault(), format, value) + " " + unit;
    }
    
    private void updateCharts(Map<String, List<Double>> data) {
        // Heart Rate Chart
        if (data.containsKey("heart_rate")) {
            List<Double> hrData = data.get("heart_rate");
            ChartManager.updateLineChart(chartHR, hrData, null, "Heart Rate (BPM)");
        }
        
        // Sleep Chart REMOVED
        
        // Cognitive Chart (Typing Speed)
        if (data.containsKey("typing_wpm")) {
             List<Double> cognitiveData = data.get("typing_wpm");
             ChartManager.updateLineChart(chartCognitive, cognitiveData, null, "Typing Speed (WPM)");
        }
        
        // Reaction Time Chart
        if (data.containsKey("reaction_time")) {
             List<Double> reactionData = data.get("reaction_time");
             ChartManager.updateLineChart(chartReaction, reactionData, null, "Reaction Time (ms)");
        }
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_data) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_data);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
