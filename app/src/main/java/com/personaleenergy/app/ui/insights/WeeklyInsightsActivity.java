package com.personaleenergy.app.ui.insights;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.settings.SettingsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeeklyInsightsActivity extends AppCompatActivity {
    
    private static final String TAG = "WeeklyInsightsActivity";
    private BottomNavigationView bottomNav;
    private SupabasePostgrestApi postgrestApi;
    private SupabaseClient supabaseClient;
    private SimpleDateFormat dateFormat;
    
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
        
        setContentView(R.layout.activity_weekly_insights);
        
        // Initialize Supabase
        supabaseClient = SupabaseClient.getInstance(this);
        postgrestApi = supabaseClient.getPostgrestApi();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        
        setupBottomNavigation();
        loadWeeklyInsights();
    }
    
    private void loadWeeklyInsights() {
        String userId = supabaseClient.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null, cannot load insights");
            return;
        }
        
        // Get date 30 days ago to fetch recent weeks
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        String weekStartDate = dateFormat.format(cal.getTime());
        
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        // Query for weeks starting from 30 days ago (using gte prefix for PostgREST)
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("week_start_date", "gte." + weekStartDate);
        queryParams.put("order", "week_start_date.desc");
        
        postgrestApi.getWeeklyInsights(authorization, apikey, queryParams)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        runOnUiThread(() -> displayWeeklyInsights(response.body()));
                    } else {
                        Log.e(TAG, "Failed to fetch weekly insights: " + response.code());
                        runOnUiThread(() -> {
                            TextView tvTrendSummary = findViewById(R.id.tvTrendSummary);
                            if (tvTrendSummary != null) {
                                tvTrendSummary.setText("No weekly insights available yet. Insights are generated weekly based on your data.");
                            }
                        });
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading weekly insights", t);
                    runOnUiThread(() -> {
                        TextView tvTrendSummary = findViewById(R.id.tvTrendSummary);
                        if (tvTrendSummary != null) {
                            tvTrendSummary.setText("Error loading weekly insights. Please try again later.");
                        }
                    });
                }
            });
    }
    
    private void displayWeeklyInsights(List<Map<String, Object>> insights) {
        if (insights == null || insights.isEmpty()) {
            return;
        }
        
        // Get the most recent week
        Map<String, Object> latestWeek = insights.get(0);
        
        TextView tvTrendSummary = findViewById(R.id.tvTrendSummary);
        TextView tvHighlights = findViewById(R.id.tvHighlights);
        TextView tvAIRecommendation = findViewById(R.id.tvAIRecommendation);
        TextView tvMetricsTitle = findViewById(R.id.tvMetricsTitle);
        
        // Display trend summary
        if (tvTrendSummary != null) {
            String summary = latestWeek.get("insights_summary") != null ? 
                latestWeek.get("insights_summary").toString() : 
                "Weekly analysis based on your biometric and cognitive data.";
            tvTrendSummary.setText(summary);
        }
        
        // Display highlights
        if (tvHighlights != null) {
            StringBuilder highlights = new StringBuilder();
            
            if (latestWeek.get("average_energy_level") != null) {
                highlights.append("• Average Energy: ").append(latestWeek.get("average_energy_level")).append("\n");
            }
            if (latestWeek.get("average_heart_rate") != null) {
                highlights.append("• Avg Heart Rate: ").append(String.format(Locale.getDefault(), "%.0f bpm", 
                    ((Number) latestWeek.get("average_heart_rate")).doubleValue())).append("\n");
            }
            if (latestWeek.get("total_sleep_hours") != null) {
                highlights.append("• Total Sleep: ").append(String.format(Locale.getDefault(), "%.1f hours", 
                    ((Number) latestWeek.get("total_sleep_hours")).doubleValue())).append("\n");
            }
            if (latestWeek.get("average_sleep_quality") != null) {
                double quality = ((Number) latestWeek.get("average_sleep_quality")).doubleValue();
                highlights.append("• Sleep Quality: ").append(String.format(Locale.getDefault(), "%.0f%%", quality * 100)).append("\n");
            }
            if (latestWeek.get("average_typing_speed") != null) {
                highlights.append("• Avg Typing Speed: ").append(String.format(Locale.getDefault(), "%.0f WPM", 
                    ((Number) latestWeek.get("average_typing_speed")).doubleValue())).append("\n");
            }
            if (latestWeek.get("productivity_score") != null) {
                double score = ((Number) latestWeek.get("productivity_score")).doubleValue();
                highlights.append("• Productivity Score: ").append(String.format(Locale.getDefault(), "%.0f%%", score * 100));
            }
            
            if (highlights.length() > 0) {
                tvHighlights.setText(highlights.toString());
            } else {
                tvHighlights.setText("No highlights available for this week.");
            }
        }
        
        // Display AI recommendation
        if (tvAIRecommendation != null) {
            String recommendation = generateAIRecommendation(latestWeek);
            tvAIRecommendation.setText(recommendation);
        }
        
        // Display metrics
        TextView tvMetricsData = findViewById(R.id.tvMetricsData);
        if (tvMetricsData != null) {
            StringBuilder metrics = new StringBuilder();
            metrics.append("Week: ").append(latestWeek.get("week_start_date")).append(" to ").append(latestWeek.get("week_end_date")).append("\n\n");
            
            if (latestWeek.get("average_heart_rate") != null) {
                metrics.append("❤️ Average Heart Rate: ").append(String.format(Locale.getDefault(), "%.0f bpm", 
                    ((Number) latestWeek.get("average_heart_rate")).doubleValue())).append("\n");
            }
            if (latestWeek.get("total_sleep_hours") != null) {
                metrics.append("😴 Total Sleep: ").append(String.format(Locale.getDefault(), "%.1f hours", 
                    ((Number) latestWeek.get("total_sleep_hours")).doubleValue())).append("\n");
            }
            if (latestWeek.get("average_sleep_quality") != null) {
                double quality = ((Number) latestWeek.get("average_sleep_quality")).doubleValue();
                metrics.append("⭐ Sleep Quality: ").append(String.format(Locale.getDefault(), "%.0f%%", quality * 100)).append("\n");
            }
            if (latestWeek.get("average_typing_speed") != null) {
                metrics.append("⌨️ Typing Speed: ").append(String.format(Locale.getDefault(), "%.0f WPM", 
                    ((Number) latestWeek.get("average_typing_speed")).doubleValue())).append("\n");
            }
            if (latestWeek.get("average_reaction_time") != null) {
                metrics.append("⚡ Reaction Time: ").append(String.format(Locale.getDefault(), "%.0f ms", 
                    ((Number) latestWeek.get("average_reaction_time")).doubleValue())).append("\n");
            }
            if (latestWeek.get("productivity_score") != null) {
                double score = ((Number) latestWeek.get("productivity_score")).doubleValue();
                metrics.append("📊 Productivity: ").append(String.format(Locale.getDefault(), "%.0f%%", score * 100)).append("\n");
            }
            
            if (metrics.length() > 0) {
                tvMetricsData.setText(metrics.toString());
            } else {
                tvMetricsData.setText("No metrics available for this week.");
            }
        }
        
        if (tvMetricsTitle != null) {
            tvMetricsTitle.setText("📈 Weekly Metrics");
        }
    }
    
    private String generateAIRecommendation(Map<String, Object> weekData) {
        StringBuilder recommendation = new StringBuilder();
        
        if (weekData.get("average_energy_level") != null) {
            String energyLevel = weekData.get("average_energy_level").toString();
            if ("HIGH".equals(energyLevel)) {
                recommendation.append("Your energy levels were consistently high this week. ");
                recommendation.append("Consider maintaining this pattern by continuing your current routine.");
            } else if ("LOW".equals(energyLevel)) {
                recommendation.append("Your energy levels were lower this week. ");
                recommendation.append("Try getting more sleep and scheduling breaks between tasks.");
            } else {
                recommendation.append("Your energy levels were moderate this week. ");
                recommendation.append("Focus on identifying your peak hours for demanding tasks.");
            }
        }
        
        if (weekData.get("average_sleep_quality") != null) {
            double quality = ((Number) weekData.get("average_sleep_quality")).doubleValue();
            if (quality < 0.7) {
                recommendation.append("\n\nYour sleep quality could be improved. ");
                recommendation.append("Consider establishing a consistent bedtime routine.");
            }
        }
        
        if (weekData.get("productivity_score") != null) {
            double score = ((Number) weekData.get("productivity_score")).doubleValue();
            if (score < 0.7) {
                recommendation.append("\n\nYour productivity score suggests room for improvement. ");
                recommendation.append("Try breaking tasks into smaller chunks and taking regular breaks.");
            }
        }
        
        return recommendation.length() > 0 ? recommendation.toString() : 
            "Continue tracking your data to receive personalized recommendations.";
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_insights) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_insights);
    }
}

