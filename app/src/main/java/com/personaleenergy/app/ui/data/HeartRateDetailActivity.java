package com.personaleenergy.app.ui.data;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.flowstate.app.R;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.*;

public class HeartRateDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "HeartRateDetailActivity";
    
    private TextView tvAverageHR;
    private TextView tvMinHR;
    private TextView tvMaxHR;
    private TextView tvDataPoints;
    private TextView tvRecentReadings;
    private TextView tvHRVInsights;
    private LineChart hrChart;
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_detail);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Heart Rate Details");
            }
        }
        
        initializeViews();
        setupChart();
        
        supabaseClient = SupabaseClient.getInstance(this);
        postgrestApi = supabaseClient.getPostgrestApi();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        
        loadHeartRateData();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void initializeViews() {
        tvAverageHR = findViewById(R.id.tvAverageHR);
        tvMinHR = findViewById(R.id.tvMinHR);
        tvMaxHR = findViewById(R.id.tvMaxHR);
        tvDataPoints = findViewById(R.id.tvDataPoints);
        tvRecentReadings = findViewById(R.id.tvRecentReadings);
        tvHRVInsights = findViewById(R.id.tvHRVInsights);
        hrChart = findViewById(R.id.hrChart);
    }
    
    private void setupChart() {
        if (hrChart == null) return;
        
        hrChart.getDescription().setEnabled(false);
        hrChart.setTouchEnabled(true);
        hrChart.setDragEnabled(true);
        hrChart.setScaleEnabled(true);
        hrChart.setPinchZoom(true);
        hrChart.setDrawGridBackground(false);
        
        XAxis xAxis = hrChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat format = new SimpleDateFormat("MMM dd", Locale.getDefault());
            
            @Override
            public String getFormattedValue(float value) {
                Date date = new Date((long) value);
                return format.format(date);
            }
        });
        
        YAxis leftAxis = hrChart.getAxisLeft();
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(120f);
        leftAxis.setLabelCount(5, true);
        
        hrChart.getAxisRight().setEnabled(false);
        hrChart.getLegend().setEnabled(false);
    }
    
    private void loadHeartRateData() {
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
        
        String hrUserId = "eq." + userId;
        String hrTimestampGte = "gte." + dateFormat.format(startDate);
        String hrTimestampLte = "lte." + dateFormat.format(endDate);
        String hrOrder = "timestamp.desc";
        
        postgrestApi.getHeartRateReadings(authorization, apikey, hrUserId, hrTimestampGte, hrTimestampLte, hrOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        displayHeartRateData(response.body());
                    } else {
                        showError("Failed to load heart rate data");
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading heart rate data", t);
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
    
    private void displayHeartRateData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            showError("No heart rate data available for the last 30 days.");
            return;
        }
        
        // Calculate statistics
        double totalHR = 0;
        int minHR = Integer.MAX_VALUE;
        int maxHR = 0;
        int validCount = 0;
        List<Entry> chartEntries = new ArrayList<>();
        List<String> recentReadings = new ArrayList<>();
        
        // Sort by timestamp for chart
        List<Map<String, Object>> sortedData = new ArrayList<>(data);
        sortedData.sort((a, b) -> {
            Date dateA = parseDate(a.get("timestamp") != null ? a.get("timestamp").toString() : null);
            Date dateB = parseDate(b.get("timestamp") != null ? b.get("timestamp").toString() : null);
            if (dateA == null || dateB == null) return 0;
            return dateA.compareTo(dateB);
        });
        
        for (Map<String, Object> reading : sortedData) {
            if (reading.get("heart_rate_bpm") != null) {
                try {
                    int hr = ((Number) reading.get("heart_rate_bpm")).intValue();
                    totalHR += hr;
                    if (hr < minHR) minHR = hr;
                    if (hr > maxHR) maxHR = hr;
                    validCount++;
                    
                    // Add to chart
                    Date timestamp = parseDate(reading.get("timestamp") != null ? reading.get("timestamp").toString() : null);
                    if (timestamp != null) {
                        chartEntries.add(new Entry(timestamp.getTime(), hr));
                    }
                    
                    // Collect recent readings (last 10)
                    if (recentReadings.size() < 10) {
                        Date date = parseDate(reading.get("timestamp") != null ? reading.get("timestamp").toString() : null);
                        if (date != null) {
                            recentReadings.add(String.format(Locale.getDefault(), "%s: %d bpm", 
                                displayDateFormat.format(date), hr));
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing heart_rate_bpm", e);
                }
            }
        }
        
        if (validCount > 0) {
            double avgHR = totalHR / validCount;
            
            // Update statistics
            if (tvAverageHR != null) {
                tvAverageHR.setText(String.format(Locale.getDefault(), "%.0f bpm", avgHR));
            }
            if (tvMinHR != null) {
                tvMinHR.setText(String.format(Locale.getDefault(), "%d bpm", minHR));
            }
            if (tvMaxHR != null) {
                tvMaxHR.setText(String.format(Locale.getDefault(), "%d bpm", maxHR));
            }
            if (tvDataPoints != null) {
                tvDataPoints.setText(String.format(Locale.getDefault(), "%d readings", validCount));
            }
            
            // Display recent readings
            if (tvRecentReadings != null) {
                Collections.reverse(recentReadings); // Show newest first
                StringBuilder sb = new StringBuilder();
                for (String reading : recentReadings) {
                    sb.append(reading).append("\n");
                }
                tvRecentReadings.setText(sb.toString().trim());
            }
            
            // HRV Insights
            if (tvHRVInsights != null) {
                String insight = generateHRVInsight(avgHR, minHR, maxHR, validCount);
                tvHRVInsights.setText(insight);
            }
            
            // Update chart
            if (hrChart != null && !chartEntries.isEmpty()) {
                LineDataSet dataSet = new LineDataSet(chartEntries, "Heart Rate");
                dataSet.setColor(getResources().getColor(R.color.primary, getTheme()));
                dataSet.setLineWidth(2f);
                dataSet.setCircleColor(getResources().getColor(R.color.primary, getTheme()));
                dataSet.setCircleRadius(3f);
                dataSet.setDrawCircleHole(false);
                dataSet.setDrawValues(false);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                
                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(dataSet);
                
                LineData lineData = new LineData(dataSets);
                hrChart.setData(lineData);
                hrChart.invalidate();
            }
        } else {
            showError("No valid heart rate data found.");
        }
    }
    
    private String generateHRVInsight(double avgHR, int minHR, int maxHR, int count) {
        StringBuilder insight = new StringBuilder();
        
        if (avgHR < 60) {
            insight.append("Your average heart rate is quite low, which may indicate excellent cardiovascular fitness or a need to consult a healthcare provider.\n\n");
        } else if (avgHR < 70) {
            insight.append("Your heart rate shows excellent resting patterns, indicating good cardiovascular health.\n\n");
        } else if (avgHR < 85) {
            insight.append("Your heart rate is within normal range for daily activities.\n\n");
        } else {
            insight.append("Your heart rate is elevated. Consider stress management and regular exercise.\n\n");
        }
        
        int range = maxHR - minHR;
        if (range > 50) {
            insight.append("Your heart rate shows good variability, which is a positive sign for recovery and stress management.");
        } else if (range > 30) {
            insight.append("Your heart rate variability is moderate. Regular exercise and good sleep can help improve it.");
        } else {
            insight.append("Your heart rate shows limited variability. Focus on stress reduction and recovery.");
        }
        
        return insight.toString();
    }
    
    private void showError(String message) {
        if (tvAverageHR != null) tvAverageHR.setText("--");
        if (tvMinHR != null) tvMinHR.setText("--");
        if (tvMaxHR != null) tvMaxHR.setText("--");
        if (tvDataPoints != null) tvDataPoints.setText("0");
        if (tvRecentReadings != null) tvRecentReadings.setText(message);
        if (tvHRVInsights != null) tvHRVInsights.setText("Unable to generate insights without data.");
    }
}

