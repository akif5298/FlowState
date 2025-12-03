package com.flowstate.app.ui.insights;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.data.DataLogsActivity;
import com.flowstate.app.ui.settings.SettingsActivity;
import com.flowstate.app.ui.schedule.AIScheduleActivity;
import com.flowstate.app.ui.charts.ChartManager;
import com.github.mikephil.charting.charts.LineChart;
import java.util.ArrayList;
import java.util.List;

public class WeeklyInsightsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private TextView tvTrendSummary, tvHighlights, tvAIRecommendation;
    private LineChart chartTrends;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_insights);
        
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
        // Placeholder: Load real weekly aggregated data here
        // For now we just set up an empty chart or mock data
        
        // Example mock data for chart to show layout working
        List<Double> mockTrends = new ArrayList<>();
        mockTrends.add(65.0);
        mockTrends.add(72.0);
        mockTrends.add(80.0);
        mockTrends.add(55.0);
        mockTrends.add(60.0);
        mockTrends.add(85.0);
        mockTrends.add(75.0);
        
        List<String> labels = new ArrayList<>();
        labels.add("Mon");
        labels.add("Tue");
        labels.add("Wed");
        labels.add("Thu");
        labels.add("Fri");
        labels.add("Sat");
        labels.add("Sun");
        
        ChartManager.updateLineChart(chartTrends, mockTrends, labels, "Avg Energy");
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
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
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
}
