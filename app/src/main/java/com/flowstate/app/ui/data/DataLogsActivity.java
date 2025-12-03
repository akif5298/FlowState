package com.flowstate.app.ui.data;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.settings.SettingsActivity;
import com.flowstate.app.ui.schedule.AIScheduleActivity;
import com.flowstate.app.ui.insights.WeeklyInsightsActivity;
import com.flowstate.app.ui.charts.ChartManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.flowstate.services.HealthDataAggregator;
import com.flowstate.app.data.models.HealthDataSummary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;

public class DataLogsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private LineChart chartHR;
    private BarChart chartSleep;
    private LineChart chartCognitive;
    private LineChart chartReaction;
    private TextView tvWeather, tvAISummary;
    private TextView tvSleepScore;
    
    // Activity & Vitals Metrics
    private TextView tvAvgHR, tvRestingHR, tvAvgHRV;
    private TextView tvSteps, tvDistance, tvActiveCal, tvTotalCal, tvFloors, tvElevation;
    private TextView tvBP, tvSpO2, tvRespRate, tvGlucose, tvWeight, tvVo2Max;
    
    private HealthDataAggregator dataAggregator;
    private ExecutorService executor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_logs);
        
        initializeViews();
        setupCharts();
        
        dataAggregator = new HealthDataAggregator(this);
        executor = Executors.newSingleThreadExecutor();
        
        setupBottomNavigation();
        loadData();
    }
    
    private void initializeViews() {
        chartHR = findViewById(R.id.chartHR);
        chartSleep = findViewById(R.id.chartSleep);
        chartCognitive = findViewById(R.id.chartCognitive);
        chartReaction = findViewById(R.id.chartReaction);
        tvWeather = findViewById(R.id.tvWeather);
        tvAISummary = findViewById(R.id.tvAISummary);
        tvSleepScore = findViewById(R.id.tvSleepScore);
        
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
        ChartManager.setupBarChart(chartSleep, "Sleep Duration");
        ChartManager.setupLineChart(chartCognitive, "Typing Speed");
        ChartManager.setupLineChart(chartReaction, "Reaction Time");
    }
    
    private void loadData() {
        executor.execute(() -> {
            // 1. Collect summary data for scalar values
            HealthDataSummary summary = dataAggregator.createHealthSummary(24);
            
            // 2. Collect time-series data for charts
            Map<String, List<Double>> timeSeriesData = dataAggregator.collectHealthData(24);
            
            runOnUiThread(() -> {
                updateMetrics(summary);
                updateCharts(timeSeriesData);
            });
        });
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
        
        // Sleep quality score
        if (summary.getSleepQuality() != null && summary.getSleepQuality() > 0) {
            int score = (int) (summary.getSleepQuality() * 100);
            tvSleepScore.setText(score + "/100");
        } else {
            tvSleepScore.setText("--");
        }
        
        // Note: AI Summary text is usually transient or stored in separate logs table.
        // If HealthDataSummary doesn't hold it, we might need to fetch last prediction.
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
        
        // Sleep Chart
        if (data.containsKey("sleep_hours")) {
            List<Double> sleepData = data.get("sleep_hours");
            ChartManager.updateBarChart(chartSleep, sleepData, null, "Sleep (Hours)");
        }
        
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
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
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
