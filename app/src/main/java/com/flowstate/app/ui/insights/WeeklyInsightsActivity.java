package com.flowstate.app.ui.insights;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.data.DataLogsActivity;
import com.flowstate.app.ui.settings.SettingsActivity;
import com.flowstate.app.ui.charts.ChartManager;
import com.github.mikephil.charting.charts.LineChart;
import com.flowstate.data.local.repo.EnergyPredictionRepository;
import com.flowstate.data.local.entities.PredictionLocal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeeklyInsightsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private TextView tvTrendSummary, tvHighlights, tvAIRecommendation;
    private LineChart chartTrends;
    
    private EnergyPredictionRepository predictionRepository;
    private ExecutorService executor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_insights);
        
        predictionRepository = EnergyPredictionRepository.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        
        initializeViews();
        setupBottomNavigation();
        loadInsights();
    }
    
    private void initializeViews() {
        tvTrendSummary = findViewById(R.id.tvTrendSummary);
        tvHighlights = findViewById(R.id.tvHighlights);
        tvAIRecommendation = findViewById(R.id.tvAIRecommendation);
        chartTrends = findViewById(R.id.chartTrends);
        
        ChartManager.setupLineChart(chartTrends, "Weekly Energy Trends");
    }
    
    private void loadInsights() {
        // Calculate date range for the last 7 days
        Calendar calendar = Calendar.getInstance();
        long endQuery = calendar.getTimeInMillis();
        
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long startQuery = calendar.getTimeInMillis();
        
        tvTrendSummary.setText("Loading weekly data...");
        
        predictionRepository.getByDateRange(startQuery, endQuery, new EnergyPredictionRepository.DataCallback<List<PredictionLocal>>() {
            @Override
            public void onSuccess(List<PredictionLocal> predictions) {
                processWeeklyData(predictions);
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    tvTrendSummary.setText("Error loading data.");
                    Toast.makeText(WeeklyInsightsActivity.this, "Failed to load insights", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void processWeeklyData(List<PredictionLocal> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            runOnUiThread(() -> {
                tvTrendSummary.setText("No enough data yet to show weekly trends.");
                tvHighlights.setText("• Check back after using the app for a few days.");
                tvAIRecommendation.setText("Start tracking your energy to get personalized insights.");
                chartTrends.clear();
            });
            return;
        }

        // Aggregate by day
        Map<String, List<Double>> dailyMap = new HashMap<>();
        Map<String, Long> dayTimestampMap = new HashMap<>(); // To sort keys
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault()); // Mon, Tue...
        SimpleDateFormat dayKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Sorting key

        for (PredictionLocal p : predictions) {
            String dateKey = dayKeyFormat.format(new Date(p.predictionTime));
            if (!dailyMap.containsKey(dateKey)) {
                dailyMap.put(dateKey, new ArrayList<>());
                dayTimestampMap.put(dateKey, p.predictionTime);
            }
            dailyMap.get(dateKey).add(p.predictedLevel);
        }

        // Sort days
        List<String> sortedKeys = new ArrayList<>(dailyMap.keySet());
        Collections.sort(sortedKeys);

        // Calculate averages and best day
        List<Double> dailyAverages = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        double globalSum = 0;
        int globalCount = 0;
        double maxAvg = -1;
        String bestDay = "N/A";
        
        for (String key : sortedKeys) {
            List<Double> values = dailyMap.get(key);
            double sum = 0;
            for (double v : values) sum += v;
            double avg = sum / values.size();
            
            dailyAverages.add(avg);
            labels.add(dayFormat.format(new Date(dayTimestampMap.get(key))));
            
            globalSum += sum;
            globalCount += values.size();
            
            if (avg > maxAvg) {
                maxAvg = avg;
                bestDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(dayTimestampMap.get(key)));
            }
        }

        double globalAvg = globalCount > 0 ? globalSum / globalCount : 0;
        
        // Prepare final values for UI
        String finalBestDay = bestDay;
        double finalGlobalAvg = globalAvg;
        List<Double> chartData = dailyAverages;
        List<String> chartLabels = labels;
        
        runOnUiThread(() -> {
            // Update Chart
            ChartManager.updateLineChart(chartTrends, chartData, chartLabels, "Avg Energy");
            
            // Update Highlights
            StringBuilder highlights = new StringBuilder();
            highlights.append("• Best Day: ").append(finalBestDay).append("\n");
            highlights.append("• Weekly Avg: ").append(String.format(Locale.getDefault(), "%.1f", finalGlobalAvg)).append("/100");
            tvHighlights.setText(highlights.toString());
            
            // Update Summary
            if (finalGlobalAvg > 75) {
                tvTrendSummary.setText("You had a high-energy week! Great maintenance of flow.");
            } else if (finalGlobalAvg > 50) {
                tvTrendSummary.setText("Your energy was balanced this week. Look for patterns in your peaks.");
            } else {
                tvTrendSummary.setText("Energy levels were lower than usual. Focus on recovery next week.");
            }
            
            // Update AI Recommendation (Simple rule-based for now)
            if (finalGlobalAvg < 50) {
                tvAIRecommendation.setText("Prioritize sleep consistency and take more breaks during work sessions.");
            } else if (finalGlobalAvg > 80) {
                tvAIRecommendation.setText("You're on a roll! Challenge yourself with high-focus tasks during your peaks.");
            } else {
                tvAIRecommendation.setText("Maintain your current rhythm but try to optimize your mid-day dip.");
            }
        });
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_insights) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
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
        bottomNav.setSelectedItemId(R.id.nav_insights);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
