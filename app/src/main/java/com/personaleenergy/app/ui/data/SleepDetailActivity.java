package com.personaleenergy.app.ui.data;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.flowstate.app.R;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.*;

public class SleepDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "SleepDetailActivity";
    
    private TextView tvAvgDuration;
    private TextView tvAvgQuality;
    private TextView tvTotalSessions;
    private TextView tvSleepStages;
    private TextView tvRecentSessions;
    private TextView tvSleepInsights;
    private BarChart sleepChart;
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_detail);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Sleep Details");
            }
        }
        
        initializeViews();
        setupChart();
        
        supabaseClient = SupabaseClient.getInstance(this);
        postgrestApi = supabaseClient.getPostgrestApi();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        loadSleepData();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void initializeViews() {
        tvAvgDuration = findViewById(R.id.tvAvgDuration);
        tvAvgQuality = findViewById(R.id.tvAvgQuality);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvSleepStages = findViewById(R.id.tvSleepStages);
        tvRecentSessions = findViewById(R.id.tvRecentSessions);
        tvSleepInsights = findViewById(R.id.tvSleepInsights);
        sleepChart = findViewById(R.id.sleepChart);
    }
    
    private void setupChart() {
        if (sleepChart == null) return;
        
        sleepChart.getDescription().setEnabled(false);
        sleepChart.setTouchEnabled(true);
        sleepChart.setDragEnabled(true);
        sleepChart.setScaleEnabled(true);
        sleepChart.setPinchZoom(true);
        sleepChart.setDrawGridBackground(false);
        
        XAxis xAxis = sleepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        
        YAxis leftAxis = sleepChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(12f);
        leftAxis.setLabelCount(6, true);
        
        sleepChart.getAxisRight().setEnabled(false);
        sleepChart.getLegend().setEnabled(false);
    }
    
    private void loadSleepData() {
        String userId = supabaseClient.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null");
            return;
        }
        
        // Get data from last 30 days
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (30 * 24 * 60 * 60 * 1000L);
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);
        
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        String userFilter = "eq." + userId;
        String sleepStartGte = "gte." + dateFormat.format(startDate);
        String sleepStartLte = "lte." + dateFormat.format(endDate);
        String sleepOrder = "sleep_start.desc";
        
        postgrestApi.getSleepSessions(authorization, apikey, userFilter, sleepStartGte, sleepStartLte, sleepOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        displaySleepData(response.body());
                    } else {
                        showError("Failed to load sleep data");
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading sleep data", t);
                    showError("Error loading data. Please try again later.");
                }
            });
    }
    
    private Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return dateFormat.parse(dateString);
        } catch (Exception e) {
            try {
                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                altFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                return altFormat.parse(dateString);
            } catch (Exception e2) {
                try {
                    SimpleDateFormat microsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US);
                    microsFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return microsFormat.parse(dateString);
                } catch (Exception e3) {
                    Log.e(TAG, "Error parsing date: " + dateString, e3);
                    return null;
                }
            }
        }
    }
    
    private void displaySleepData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            showError("No sleep data available for the last 30 days.");
            return;
        }
        
        // Calculate statistics
        double totalDuration = 0;
        double totalQuality = 0;
        int qualityCount = 0;
        int count = data.size();
        int minDuration = Integer.MAX_VALUE;
        int maxDuration = 0;
        int totalDeep = 0;
        int totalLight = 0;
        int totalREM = 0;
        int totalAwake = 0;
        int stageCount = 0;
        
        List<BarEntry> chartEntries = new ArrayList<>();
        List<String> recentSessions = new ArrayList<>();
        Map<String, Float> dailySleep = new HashMap<>();
        
        // Sort by date for chart
        List<Map<String, Object>> sortedData = new ArrayList<>(data);
        sortedData.sort((a, b) -> {
            Date dateA = parseDate(a.get("sleep_start") != null ? a.get("sleep_start").toString() : null);
            Date dateB = parseDate(b.get("sleep_start") != null ? b.get("sleep_start").toString() : null);
            if (dateA == null || dateB == null) return 0;
            return dateA.compareTo(dateB);
        });
        
        for (Map<String, Object> session : sortedData) {
            if (session.get("duration_minutes") != null) {
                int duration = ((Number) session.get("duration_minutes")).intValue();
                totalDuration += duration;
                if (duration < minDuration) minDuration = duration;
                if (duration > maxDuration) maxDuration = duration;
                
                // Add to chart (group by day)
                Date sleepStart = parseDate(session.get("sleep_start") != null ? session.get("sleep_start").toString() : null);
                if (sleepStart != null) {
                    String dayKey = displayDateFormat.format(sleepStart);
                    float hours = duration / 60f;
                    if (dailySleep.containsKey(dayKey)) {
                        dailySleep.put(dayKey, dailySleep.get(dayKey) + hours);
                    } else {
                        dailySleep.put(dayKey, hours);
                    }
                }
            }
            
            if (session.get("sleep_quality_score") != null) {
                totalQuality += ((Number) session.get("sleep_quality_score")).doubleValue();
                qualityCount++;
            }
            
            // Sleep stages
            if (session.get("deep_sleep_minutes") != null) {
                totalDeep += ((Number) session.get("deep_sleep_minutes")).intValue();
                stageCount++;
            }
            if (session.get("light_sleep_minutes") != null) {
                totalLight += ((Number) session.get("light_sleep_minutes")).intValue();
            }
            if (session.get("rem_sleep_minutes") != null) {
                totalREM += ((Number) session.get("rem_sleep_minutes")).intValue();
            }
            if (session.get("awake_minutes") != null) {
                totalAwake += ((Number) session.get("awake_minutes")).intValue();
            }
            
            // Collect recent sessions (last 7)
            if (recentSessions.size() < 7) {
                Date sleepStart = parseDate(session.get("sleep_start") != null ? session.get("sleep_start").toString() : null);
                if (sleepStart != null && session.get("duration_minutes") != null) {
                    int duration = ((Number) session.get("duration_minutes")).intValue();
                    double hours = duration / 60.0;
                    recentSessions.add(String.format(Locale.getDefault(), "%s: %.1f hours", 
                        displayDateFormat.format(sleepStart), hours));
                }
            }
        }
        
        if (count > 0) {
            double avgDuration = totalDuration / count;
            double avgQuality = qualityCount > 0 ? totalQuality / qualityCount : 0;
            double avgHours = avgDuration / 60.0;
            
            // Update statistics
            if (tvAvgDuration != null) {
                tvAvgDuration.setText(String.format(Locale.getDefault(), "%.1f hours", avgHours));
            }
            if (tvAvgQuality != null) {
                tvAvgQuality.setText(String.format(Locale.getDefault(), "%.0f%%", avgQuality * 100));
            }
            if (tvTotalSessions != null) {
                tvTotalSessions.setText(String.format(Locale.getDefault(), "%d sessions", count));
            }
            
            // Sleep stages
            if (tvSleepStages != null && stageCount > 0) {
                int avgDeep = totalDeep / stageCount;
                int avgLight = totalLight / stageCount;
                int avgREM = totalREM / stageCount;
                int avgAwake = totalAwake / stageCount;
                
                tvSleepStages.setText(String.format(Locale.getDefault(),
                    "Deep Sleep: %d min\n" +
                    "Light Sleep: %d min\n" +
                    "REM Sleep: %d min\n" +
                    "Awake: %d min",
                    avgDeep, avgLight, avgREM, avgAwake));
            }
            
            // Recent sessions
            if (tvRecentSessions != null) {
                Collections.reverse(recentSessions); // Show newest first
                StringBuilder sb = new StringBuilder();
                for (String session : recentSessions) {
                    sb.append(session).append("\n");
                }
                tvRecentSessions.setText(sb.toString().trim());
            }
            
            // Sleep insights
            if (tvSleepInsights != null) {
                String insight = generateSleepInsight(avgHours, avgQuality, count);
                tvSleepInsights.setText(insight);
            }
            
            // Update chart
            if (sleepChart != null && !dailySleep.isEmpty()) {
                List<String> sortedDays = new ArrayList<>(dailySleep.keySet());
                Collections.sort(sortedDays);
                
                List<BarEntry> entries = new ArrayList<>();
                for (int i = 0; i < sortedDays.size() && i < 14; i++) { // Show last 14 days
                    String day = sortedDays.get(sortedDays.size() - 14 + i);
                    entries.add(new BarEntry(i, dailySleep.get(day)));
                }
                
                if (!entries.isEmpty()) {
                    BarDataSet dataSet = new BarDataSet(entries, "Sleep Duration");
                    dataSet.setColor(getResources().getColor(R.color.primary, getTheme()));
                    dataSet.setValueTextSize(10f);
                    
                    BarData barData = new BarData(dataSet);
                    sleepChart.setData(barData);
                    sleepChart.invalidate();
                }
            }
        } else {
            showError("No valid sleep data found.");
        }
    }
    
    private String generateSleepInsight(double avgHours, double avgQuality, int count) {
        StringBuilder insight = new StringBuilder();
        
        if (avgHours < 7) {
            insight.append("Your average sleep duration is below the recommended 7-9 hours. Consider going to bed earlier or improving your sleep routine.\n\n");
        } else if (avgHours > 9) {
            insight.append("You're getting excellent sleep duration! Your body is well-rested.\n\n");
        } else {
            insight.append("Your sleep duration is within the recommended range. Great job!\n\n");
        }
        
        if (avgQuality > 0.8) {
            insight.append("Your sleep quality is excellent. Keep up the good sleep habits!");
        } else if (avgQuality > 0.6) {
            insight.append("Your sleep quality is good. Consider maintaining a consistent sleep schedule for even better results.");
        } else {
            insight.append("Your sleep quality could be improved. Try reducing screen time before bed and creating a relaxing bedtime routine.");
        }
        
        return insight.toString();
    }
    
    private void showError(String message) {
        if (tvAvgDuration != null) tvAvgDuration.setText("--");
        if (tvAvgQuality != null) tvAvgQuality.setText("--");
        if (tvTotalSessions != null) tvTotalSessions.setText("0");
        if (tvSleepStages != null) tvSleepStages.setText("No data available");
        if (tvRecentSessions != null) tvRecentSessions.setText(message);
        if (tvSleepInsights != null) tvSleepInsights.setText("Unable to generate insights without data.");
    }
}

