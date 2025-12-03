package com.personaleenergy.app.ui.data;

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

public class CognitiveDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "CognitiveDetailActivity";
    
    private TextView tvAvgWPM;
    private TextView tvAvgAccuracy;
    private TextView tvAvgReaction;
    private TextView tvTotalTests;
    private TextView tvRecentTests;
    private TextView tvCognitiveInsights;
    private LineChart typingChart;
    private LineChart reactionChart;
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
    private int typingTestCount = 0;
    private int reactionTestCount = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cognitive_detail);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Cognitive Performance");
            }
        }
        
        initializeViews();
        setupCharts();
        
        supabaseClient = SupabaseClient.getInstance(this);
        postgrestApi = supabaseClient.getPostgrestApi();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        
        loadCognitiveData();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void initializeViews() {
        tvAvgWPM = findViewById(R.id.tvAvgWPM);
        tvAvgAccuracy = findViewById(R.id.tvAvgAccuracy);
        tvAvgReaction = findViewById(R.id.tvAvgReaction);
        tvTotalTests = findViewById(R.id.tvTotalTests);
        tvRecentTests = findViewById(R.id.tvRecentTests);
        tvCognitiveInsights = findViewById(R.id.tvCognitiveInsights);
        typingChart = findViewById(R.id.typingChart);
        reactionChart = findViewById(R.id.reactionChart);
    }
    
    private void setupCharts() {
        setupChart(typingChart, "WPM", 0f, 100f);
        setupChart(reactionChart, "ms", 0f, 500f);
    }
    
    private void setupChart(LineChart chart, String unit, float min, float max) {
        if (chart == null) return;
        
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        
        XAxis xAxis = chart.getXAxis();
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
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(min);
        leftAxis.setAxisMaximum(max);
        leftAxis.setLabelCount(5, true);
        
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }
    
    private void loadCognitiveData() {
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
        
        // Load typing data
        loadTypingData(userId, startDate, endDate, authorization, apikey);
        
        // Load reaction time data
        loadReactionTimeData(userId, startDate, endDate, authorization, apikey);
    }
    
    private void loadTypingData(String userId, Date startDate, Date endDate, String authorization, String apikey) {
        String typingUserId = "eq." + userId;
        String typingTimestampGte = "gte." + dateFormat.format(startDate);
        String typingTimestampLte = "lte." + dateFormat.format(endDate);
        String typingOrder = "timestamp.desc";
        
        postgrestApi.getTypingSpeedTests(authorization, apikey, typingUserId, typingTimestampGte, typingTimestampLte, typingOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        displayTypingData(response.body());
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading typing data", t);
                }
            });
    }
    
    private void loadReactionTimeData(String userId, Date startDate, Date endDate, String authorization, String apikey) {
        String reactionUserId = "eq." + userId;
        String reactionTimestampGte = "gte." + dateFormat.format(startDate);
        String reactionTimestampLte = "lte." + dateFormat.format(endDate);
        String reactionOrder = "timestamp.desc";
        
        postgrestApi.getReactionTimeTests(authorization, apikey, reactionUserId, reactionTimestampGte, reactionTimestampLte, reactionOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        displayReactionData(response.body());
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading reaction time data", t);
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
    
    private void displayTypingData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            typingTestCount = 0;
            if (tvAvgWPM != null) tvAvgWPM.setText("--");
            if (tvAvgAccuracy != null) tvAvgAccuracy.setText("--");
            updateTotalTests();
            return;
        }
        
        double totalWPM = 0;
        double totalAccuracy = 0;
        int wpmCount = 0;
        int accuracyCount = 0;
        List<Entry> chartEntries = new ArrayList<>();
        List<String> recentTests = new ArrayList<>();
        typingTestCount = data.size(); // Store count for total tests calculation
        
        // Sort by timestamp
        List<Map<String, Object>> sortedData = new ArrayList<>(data);
        sortedData.sort((a, b) -> {
            Date dateA = parseDate(a.get("timestamp") != null ? a.get("timestamp").toString() : null);
            Date dateB = parseDate(b.get("timestamp") != null ? b.get("timestamp").toString() : null);
            if (dateA == null || dateB == null) return 0;
            return dateA.compareTo(dateB);
        });
        
        for (Map<String, Object> test : sortedData) {
            try {
                if (test.get("words_per_minute") != null) {
                    int wpm = ((Number) test.get("words_per_minute")).intValue();
                    totalWPM += wpm;
                    wpmCount++;
                    
                    Date timestamp = parseDate(test.get("timestamp") != null ? test.get("timestamp").toString() : null);
                    if (timestamp != null) {
                        chartEntries.add(new Entry(timestamp.getTime(), wpm));
                    }
                }
                if (test.get("accuracy_percentage") != null) {
                    totalAccuracy += ((Number) test.get("accuracy_percentage")).doubleValue();
                    accuracyCount++;
                }
                
                // Collect recent tests
                if (recentTests.size() < 5) {
                    Date timestamp = parseDate(test.get("timestamp") != null ? test.get("timestamp").toString() : null);
                    if (timestamp != null && test.get("words_per_minute") != null) {
                        int wpm = ((Number) test.get("words_per_minute")).intValue();
                        recentTests.add(String.format(Locale.getDefault(), "%s: %d WPM", 
                            displayDateFormat.format(timestamp), wpm));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing typing test data", e);
            }
        }
        
        if (wpmCount > 0) {
            double avgWPM = totalWPM / wpmCount;
            double avgAccuracy = accuracyCount > 0 ? totalAccuracy / accuracyCount : 0;
            
            if (tvAvgWPM != null) {
                tvAvgWPM.setText(String.format(Locale.getDefault(), "%.0f WPM", avgWPM));
            }
            if (tvAvgAccuracy != null) {
                tvAvgAccuracy.setText(String.format(Locale.getDefault(), "%.0f%%", avgAccuracy));
            }
            
            // Update chart
            if (typingChart != null && !chartEntries.isEmpty()) {
                LineDataSet dataSet = new LineDataSet(chartEntries, "Typing Speed");
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
                typingChart.setData(lineData);
                typingChart.invalidate();
            }
        }
        
        updateRecentTests(recentTests);
        updateTotalTests();
        updateInsights();
    }
    
    private void displayReactionData(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            reactionTestCount = 0;
            if (tvAvgReaction != null) tvAvgReaction.setText("--");
            updateTotalTests();
            return;
        }
        
        double totalReaction = 0;
        int validCount = 0;
        List<Entry> chartEntries = new ArrayList<>();
        reactionTestCount = data.size(); // Store count for total tests calculation
        
        // Sort by timestamp
        List<Map<String, Object>> sortedData = new ArrayList<>(data);
        sortedData.sort((a, b) -> {
            Date dateA = parseDate(a.get("timestamp") != null ? a.get("timestamp").toString() : null);
            Date dateB = parseDate(b.get("timestamp") != null ? b.get("timestamp").toString() : null);
            if (dateA == null || dateB == null) return 0;
            return dateA.compareTo(dateB);
        });
        
        for (Map<String, Object> test : sortedData) {
            try {
                if (test.get("reaction_time_ms") != null) {
                    int reaction = ((Number) test.get("reaction_time_ms")).intValue();
                    totalReaction += reaction;
                    validCount++;
                    
                    Date timestamp = parseDate(test.get("timestamp") != null ? test.get("timestamp").toString() : null);
                    if (timestamp != null) {
                        chartEntries.add(new Entry(timestamp.getTime(), reaction));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing reaction time data", e);
            }
        }
        
        if (validCount > 0) {
            double avgReaction = totalReaction / validCount;
            
            if (tvAvgReaction != null) {
                tvAvgReaction.setText(String.format(Locale.getDefault(), "%.0f ms", avgReaction));
            }
            
            // Update chart
            if (reactionChart != null && !chartEntries.isEmpty()) {
                LineDataSet dataSet = new LineDataSet(chartEntries, "Reaction Time");
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
                reactionChart.setData(lineData);
                reactionChart.invalidate();
            }
        }
        
        updateTotalTests();
        updateInsights();
    }
    
    private void updateRecentTests(List<String> typingTests) {
        if (tvRecentTests != null) {
            Collections.reverse(typingTests); // Show newest first
            StringBuilder sb = new StringBuilder();
            for (String test : typingTests) {
                sb.append(test).append("\n");
            }
            if (sb.length() > 0) {
                tvRecentTests.setText(sb.toString().trim());
            } else {
                tvRecentTests.setText("No recent tests available");
            }
        }
    }
    
    private void updateInsights() {
        if (tvCognitiveInsights == null) return;
        
        String wpmText = tvAvgWPM != null ? tvAvgWPM.getText().toString() : "";
        String accuracyText = tvAvgAccuracy != null ? tvAvgAccuracy.getText().toString() : "";
        String reactionText = tvAvgReaction != null ? tvAvgReaction.getText().toString() : "";
        
        StringBuilder insight = new StringBuilder();
        
        if (!wpmText.equals("--") && !wpmText.isEmpty()) {
            try {
                double wpm = Double.parseDouble(wpmText.replace(" WPM", ""));
                if (wpm < 40) {
                    insight.append("Your typing speed is below average. Practice regularly to improve.\n\n");
                } else if (wpm < 60) {
                    insight.append("Your typing speed is average. Keep practicing to reach higher speeds.\n\n");
                } else {
                    insight.append("Excellent typing speed! You're performing above average.\n\n");
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        if (!reactionText.equals("--") && !reactionText.isEmpty()) {
            try {
                double reaction = Double.parseDouble(reactionText.replace(" ms", ""));
                if (reaction < 250) {
                    insight.append("Your reaction time is excellent! You have quick reflexes.");
                } else if (reaction < 300) {
                    insight.append("Your reaction time is good. Regular practice can help improve it further.");
                } else {
                    insight.append("Your reaction time could be improved. Try reaction time exercises.");
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        if (insight.length() == 0) {
            insight.append("Complete cognitive tests to see insights about your performance.");
        }
        
        tvCognitiveInsights.setText(insight.toString());
    }
    
    private void updateTotalTests() {
        // Calculate total from both typing and reaction data
        if (tvTotalTests != null) {
            int total = typingTestCount + reactionTestCount;
            if (total > 0) {
                tvTotalTests.setText(String.format(Locale.getDefault(), "%d tests", total));
            } else {
                tvTotalTests.setText("0 tests");
            }
        }
    }
}

