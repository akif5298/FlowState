package com.personaleenergy.app.ui.data;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import com.flowstate.app.utils.HelpDialogHelper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.data.models.BiometricData;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.ReactionTimeData;
import com.flowstate.app.data.models.TypingSpeedData;
import com.flowstate.app.supabase.repository.BiometricDataRepository;
import com.flowstate.app.supabase.repository.EnergyPredictionRepository;
import com.flowstate.app.supabase.repository.ReactionTimeRepository;
import com.flowstate.app.supabase.repository.TypingSpeedRepository;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.settings.SettingsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

public class DataLogsActivity extends AppCompatActivity {
    
    private static final String TAG = "DataLogsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private BottomNavigationView bottomNav;
    private FusedLocationProviderClient fusedLocationClient;
    
    // Repositories
    private BiometricDataRepository biometricRepo;
    private TypingSpeedRepository typingRepo;
    private ReactionTimeRepository reactionRepo;
    private EnergyPredictionRepository energyRepo;
    private SupabasePostgrestApi postgrestApi;
    private SupabaseClient supabaseClient;
    private SimpleDateFormat dateFormat;
    
    // TextViews for displaying data
    private TextView tvHRData, tvSleepData, tvTypingData, tvAISummary;
    
    // ProgressBars and ListViews for CP470 requirements
    private ProgressBar progressBarHR, progressBarSleep, progressBarTyping;
    private ListView listViewHR, listViewSleep, listViewTyping;
    
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
        
        setContentView(R.layout.activity_data_logs);
        
        // Initialize repositories
        biometricRepo = new BiometricDataRepository(this);
        typingRepo = new TypingSpeedRepository(this);
        reactionRepo = new ReactionTimeRepository(this);
        energyRepo = new EnergyPredictionRepository(this);
        supabaseClient = SupabaseClient.getInstance(this);
        postgrestApi = supabaseClient.getPostgrestApi();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize TextViews (we'll add these to the layout)
        setupViews();
        setupBottomNavigation();
        
        // Make AI Summary card clickable
        com.google.android.material.card.MaterialCardView cardAISummary = findViewById(R.id.cardAISummary);
        if (cardAISummary != null) {
            cardAISummary.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.personaleenergy.app.ui.EnergyPredictionActivity.class);
                startActivity(intent);
            });
            cardAISummary.setClickable(true);
            cardAISummary.setFocusable(true);
        }
        
        // Load data
        loadData();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherData();
            } else {
                // Permission denied, use default location
                loadWeatherDataWithDefaultLocation();
            }
        }
    }
    
    private void setupViews() {
        // Initialize TextViews
        tvHRData = findViewById(R.id.tvHRData);
        tvSleepData = findViewById(R.id.tvSleepData);
        tvTypingData = findViewById(R.id.tvTypingData);
        tvAISummary = findViewById(R.id.tvAISummary);
        
        // Log if any TextView is null
        if (tvHRData == null) Log.w(TAG, "tvHRData is null in layout");
        if (tvSleepData == null) Log.w(TAG, "tvSleepData is null in layout");
        if (tvTypingData == null) Log.w(TAG, "tvTypingData is null in layout");
        if (tvAISummary == null) Log.w(TAG, "tvAISummary is null in layout");
        
        // Set initial loading text
        if (tvHRData != null) tvHRData.setText("Loading heart rate data...");
        if (tvSleepData != null) tvSleepData.setText("Loading sleep data...");
        if (tvTypingData != null) tvTypingData.setText("Loading typing and reaction data...");
        if (tvAISummary != null) tvAISummary.setText("Loading AI summary...");
        
        // Setup "View Details" button click handlers
        View btnViewHRDetails = findViewById(R.id.btnViewHRDetails);
        if (btnViewHRDetails != null) {
            btnViewHRDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, HeartRateDetailActivity.class);
                startActivity(intent);
            });
        }
        
        View btnViewSleepDetails = findViewById(R.id.btnViewSleepDetails);
        if (btnViewSleepDetails != null) {
            btnViewSleepDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, SleepDetailActivity.class);
                startActivity(intent);
            });
        }
        
        View btnViewTypingDetails = findViewById(R.id.btnViewTypingDetails);
        if (btnViewTypingDetails != null) {
            btnViewTypingDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, CognitiveDetailActivity.class);
                startActivity(intent);
            });
        }
    }
    
    /**
     * Show custom dialog with detailed information (CP470 Requirement #11 - Custom Dialog)
     */
    private void showDataDetailDialog(String title, String content) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            HelpDialogHelper.showHelpDialog(
                this,
                "Data Logs & Trends",
                HelpDialogHelper.getDefaultInstructions("Data Logs")
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * CP470 Requirement #7: AsyncTask for loading data
     * Note: AsyncTask is deprecated but required for project compliance.
     * Modern alternative would be ExecutorService, but we use AsyncTask here for requirement.
     */
    @SuppressWarnings("deprecation")
    private class LoadDataAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private String userId;
        private Date startDate;
        private Date endDate;
        private String authorization;
        private String apikey;
        
        public LoadDataAsyncTask(String userId, Date startDate, Date endDate, String authorization, String apikey) {
            this.userId = userId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.authorization = authorization;
            this.apikey = apikey;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show progress bars (CP470 Requirement #8)
            if (progressBarHR != null) progressBarHR.setVisibility(View.VISIBLE);
            if (progressBarSleep != null) progressBarSleep.setVisibility(View.VISIBLE);
            if (progressBarTyping != null) progressBarTyping.setVisibility(View.VISIBLE);
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Load data in background
                publishProgress(25);
                loadHeartRateData(userId, startDate, endDate, authorization, apikey);
                
                publishProgress(50);
                loadSleepData(userId, startDate, endDate);
                
                publishProgress(75);
                loadTypingSpeedData(userId, startDate, endDate, authorization, apikey);
                loadReactionTimeData(userId, startDate, endDate, authorization, apikey);
                
                publishProgress(100);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in AsyncTask", e);
                return false;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // Update progress if needed
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            // Hide progress bars
            if (progressBarHR != null) progressBarHR.setVisibility(View.GONE);
            if (progressBarSleep != null) progressBarSleep.setVisibility(View.GONE);
            if (progressBarTyping != null) progressBarTyping.setVisibility(View.GONE);
            
            if (success) {
                // Show Toast (CP470 Requirement #11)
                Toast.makeText(DataLogsActivity.this, "Data loaded successfully", Toast.LENGTH_SHORT).show();
            } else {
                // Show Snackbar (CP470 Requirement #11)
                com.google.android.material.snackbar.Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error loading data. Please try again.",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show();
            }
        }
    }
    
    private void loadData() {
        String userId = com.flowstate.app.supabase.SupabaseClient.getInstance(this).getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null, cannot load data");
            return;
        }
        
        // Get data from last 30 days
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (30 * 24 * 60 * 60 * 1000L);
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);
        
        Log.d(TAG, "Loading data for user: " + userId + " from " + startDate + " to " + endDate);
        
        // Load data using direct API calls like WeeklyInsightsActivity
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        // CP470 Requirement #7: Use AsyncTask for data loading
        @SuppressWarnings("deprecation")
        LoadDataAsyncTask asyncTask = new LoadDataAsyncTask(userId, startDate, endDate, authorization, apikey);
        asyncTask.execute();
        
        // Also load energy predictions (not in AsyncTask to avoid conflicts)
        
        // Load energy predictions for AI summary
        Date futureEndDate = new Date(endTime + (24 * 60 * 60 * 1000L));
        energyRepo.getEnergyPredictions(userId, startDate, futureEndDate, new EnergyPredictionRepository.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                runOnUiThread(() -> {
                    @SuppressWarnings("unchecked")
                    List<EnergyPrediction> predictions = (List<EnergyPrediction>) data;
                    displayAISummary(predictions);
                });
            }
            
            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Error loading energy predictions", error);
                runOnUiThread(() -> {
                    TextView tvAISummary = findViewById(R.id.tvAISummary);
                    if (tvAISummary != null) {
                        tvAISummary.setText("Error loading energy predictions: " + error.getMessage() + "\n\nUnable to generate AI summary.");
                    }
                });
            }
        });
        
        // Load weather data
        loadWeatherData();
    }
    
    private void loadWeatherData() {
        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Get user's current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Use actual location
                        loadWeatherDataWithLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        // Location not available, use default
                        loadWeatherDataWithDefaultLocation();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error getting location", e);
                    loadWeatherDataWithDefaultLocation();
                });
        } else {
            loadWeatherDataWithDefaultLocation();
        }
    }
    
    private void loadWeatherDataWithLocation(double latitude, double longitude) {
        new Thread(() -> {
            try {
                String apiKey = com.flowstate.app.BuildConfig.OPENWEATHER_API_KEY;
                if (apiKey == null || apiKey.isEmpty()) {
                    runOnUiThread(() -> {
                        TextView tvWeather = findViewById(R.id.tvWeather);
                        if (tvWeather != null) {
                            tvWeather.setText("Weather API key not configured.");
                        }
                    });
                    return;
                }
                // Use coordinates for more accurate weather
                String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + 
                            "&lon=" + longitude + "&appid=" + apiKey + "&units=metric";
                
                fetchAndDisplayWeather(url);
            } catch (Exception e) {
                Log.e(TAG, "Error loading weather with location", e);
                runOnUiThread(() -> {
                    TextView tvWeather = findViewById(R.id.tvWeather);
                    if (tvWeather != null) {
                        tvWeather.setText("Unable to load weather data. Please check your connection.");
                    }
                });
            }
        }).start();
    }
    
    private void loadWeatherDataWithDefaultLocation() {
        new Thread(() -> {
            try {
                String apiKey = com.flowstate.app.BuildConfig.OPENWEATHER_API_KEY;
                if (apiKey == null || apiKey.isEmpty()) {
                    runOnUiThread(() -> {
                        TextView tvWeather = findViewById(R.id.tvWeather);
                        if (tvWeather != null) {
                            tvWeather.setText("Weather API key not configured.");
                        }
                    });
                    return;
                }
                // Default to a common location
                String city = "Toronto"; // Default city
                String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=metric";
                
                fetchAndDisplayWeather(url);
            } catch (Exception e) {
                Log.e(TAG, "Error loading weather", e);
                runOnUiThread(() -> {
                    TextView tvWeather = findViewById(R.id.tvWeather);
                    if (tvWeather != null) {
                        tvWeather.setText("Unable to load weather data. Please check your connection.");
                    }
                });
            }
        }).start();
    }
    
    private void fetchAndDisplayWeather(String url) {
        try {
                
            java.net.URL weatherUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) weatherUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse JSON response
                JSONObject json = new JSONObject(response.toString());
                String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
                double temp = json.getJSONObject("main").getDouble("temp");
                double feelsLike = json.getJSONObject("main").getDouble("feels_like");
                int humidity = json.getJSONObject("main").getInt("humidity");
                String locationName = json.optString("name", "Current Location");
                
                String weatherText = String.format(Locale.getDefault(),
                    "🌤️ %s\n" +
                    "Temperature: %.1f°C (feels like %.1f°C)\n" +
                    "Humidity: %d%%\n" +
                    "Location: %s",
                    description.substring(0, 1).toUpperCase() + description.substring(1),
                    temp, feelsLike, humidity, locationName);
                
                runOnUiThread(() -> {
                    TextView tvWeather = findViewById(R.id.tvWeather);
                    if (tvWeather != null) {
                        tvWeather.setText(weatherText);
                    }
                });
            } else {
                throw new Exception("Weather API returned code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading weather", e);
            runOnUiThread(() -> {
                TextView tvWeather = findViewById(R.id.tvWeather);
                if (tvWeather != null) {
                    tvWeather.setText("Unable to load weather data. Please check your connection.");
                }
            });
        }
    }
    
    private void loadHeartRateData(String userId, Date startDate, Date endDate, String authorization, String apikey) {
        String hrUserId = "eq." + userId;
        String hrTimestampGte = "gte." + dateFormat.format(startDate);
        String hrTimestampLte = "lte." + dateFormat.format(endDate);
        String hrOrder = "timestamp.desc";
        
        Log.d(TAG, "Loading heart rate data - user_id: eq." + userId + ", timestamp: gte." + dateFormat.format(startDate) + ", lte." + dateFormat.format(endDate));
        
        postgrestApi.getHeartRateReadings(authorization, apikey, hrUserId, hrTimestampGte, hrTimestampLte, hrOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    Log.d(TAG, "Heart rate response - isSuccessful: " + response.isSuccessful() + ", code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Received " + response.body().size() + " heart rate records");
                        if (response.body().isEmpty()) {
                            Log.w(TAG, "Heart rate response body is EMPTY");
                        } else {
                            Log.d(TAG, "First heart rate record keys: " + response.body().get(0).keySet());
                        }
                        runOnUiThread(() -> displayHeartRateData(response.body()));
                    } else {
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                                Log.e(TAG, "Heart rate error body: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        Log.e(TAG, "Failed to fetch heart rate data: " + response.code() + " - " + errorBody);
                        runOnUiThread(() -> {
                            TextView tvHRData = findViewById(R.id.tvHRData);
                            if (tvHRData != null) {
                                tvHRData.setText("No heart rate data available for the last 30 days.");
                            }
                        });
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading heart rate data", t);
                    runOnUiThread(() -> {
                        TextView tvHRData = findViewById(R.id.tvHRData);
                        if (tvHRData != null) {
                            tvHRData.setText("Error loading heart rate data. Please try again later.");
                        }
                    });
                }
            });
    }
    
    private void loadTypingSpeedData(String userId, Date startDate, Date endDate, String authorization, String apikey) {
        String typingUserId = "eq." + userId;
        String typingTimestampGte = "gte." + dateFormat.format(startDate);
        String typingTimestampLte = "lte." + dateFormat.format(endDate);
        String typingOrder = "timestamp.desc";
        
        Log.d(TAG, "Loading typing speed data - user_id: eq." + userId + ", timestamp: gte." + dateFormat.format(startDate) + ", lte." + dateFormat.format(endDate));
        
        postgrestApi.getTypingSpeedTests(authorization, apikey, typingUserId, typingTimestampGte, typingTimestampLte, typingOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    Log.d(TAG, "Typing speed response - isSuccessful: " + response.isSuccessful() + ", code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Received " + response.body().size() + " typing speed records");
                        if (response.body().isEmpty()) {
                            Log.w(TAG, "Typing speed response body is EMPTY");
                        } else {
                            Log.d(TAG, "First typing record keys: " + response.body().get(0).keySet());
                        }
                        runOnUiThread(() -> displayTypingSpeedData(response.body()));
                    } else {
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                                Log.e(TAG, "Typing speed error body: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        Log.e(TAG, "Failed to fetch typing data: " + response.code() + " - " + errorBody);
                        runOnUiThread(() -> {
                            TextView tvTypingData = findViewById(R.id.tvTypingData);
                            if (tvTypingData != null) {
                                tvTypingData.setText("No typing speed data available for the last 30 days.");
                            }
                        });
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading typing data", t);
                    runOnUiThread(() -> {
                        TextView tvTypingData = findViewById(R.id.tvTypingData);
                        if (tvTypingData != null) {
                            tvTypingData.setText("Error loading typing data. Please try again later.");
                        }
                    });
                }
            });
    }
    
    private void loadReactionTimeData(String userId, Date startDate, Date endDate, String authorization, String apikey) {
        String reactionUserId = "eq." + userId;
        String reactionTimestampGte = "gte." + dateFormat.format(startDate);
        String reactionTimestampLte = "lte." + dateFormat.format(endDate);
        String reactionOrder = "timestamp.desc";
        
        Log.d(TAG, "Loading reaction time data - user_id: eq." + userId + ", timestamp: gte." + dateFormat.format(startDate) + ", lte." + dateFormat.format(endDate));
        
        postgrestApi.getReactionTimeTests(authorization, apikey, reactionUserId, reactionTimestampGte, reactionTimestampLte, reactionOrder)
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, 
                                     Response<List<Map<String, Object>>> response) {
                    Log.d(TAG, "Reaction time response - isSuccessful: " + response.isSuccessful() + ", code: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Received " + response.body().size() + " reaction time records");
                        if (response.body().isEmpty()) {
                            Log.w(TAG, "Reaction time response body is EMPTY");
                        } else {
                            Log.d(TAG, "First reaction time record keys: " + response.body().get(0).keySet());
                        }
                        runOnUiThread(() -> displayReactionTimeData(response.body()));
                    } else {
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                                Log.e(TAG, "Reaction time error body: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        Log.e(TAG, "Failed to fetch reaction time data: " + response.code() + " - " + errorBody);
                    }
                }
                
                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    Log.e(TAG, "Error loading reaction time data", t);
                }
            });
    }
    
    private void displayHeartRateData(List<Map<String, Object>> data) {
        if (tvHRData == null) {
            tvHRData = findViewById(R.id.tvHRData);
        }
        
        Log.d(TAG, "Displaying heart rate data - records: " + (data != null ? data.size() : 0));
        
        if (data == null || data.isEmpty()) {
            Log.w(TAG, "No heart rate data to display");
            if (tvHRData != null) {
                tvHRData.setText("No heart rate data available for the last 30 days.");
            }
            // Hide ListView and show TextView
            if (listViewHR != null) listViewHR.setVisibility(View.GONE);
            if (tvHRData != null) tvHRData.setVisibility(View.VISIBLE);
            return;
        }
        
        // Calculate statistics
        double totalHR = 0;
        int minHR = Integer.MAX_VALUE;
        int maxHR = 0;
        int validCount = 0; // Count of records with valid heart rate data
        List<String> hrListItems = new ArrayList<>(); // For ListView (CP470 Requirement #3)
        
        for (Map<String, Object> reading : data) {
            if (reading.get("heart_rate_bpm") != null) {
                try {
                    int hr = ((Number) reading.get("heart_rate_bpm")).intValue();
                    totalHR += hr;
                    if (hr < minHR) minHR = hr;
                    if (hr > maxHR) maxHR = hr;
                    validCount++;
                    
                    // Add to ListView items (CP470 Requirement #3)
                    String timestamp = reading.get("timestamp") != null ? 
                        reading.get("timestamp").toString() : "Unknown time";
                    hrListItems.add(String.format(Locale.getDefault(), "%d bpm - %s", hr, 
                        timestamp.length() > 10 ? timestamp.substring(0, 10) : timestamp));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing heart_rate_bpm: " + reading.get("heart_rate_bpm"), e);
                }
            }
        }
        
        // Populate ListView (CP470 Requirement #3)
        if (listViewHR != null && !hrListItems.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, hrListItems);
            listViewHR.setAdapter(adapter);
            listViewHR.setVisibility(View.VISIBLE);
            if (tvHRData != null) tvHRData.setVisibility(View.GONE);
        } else {
            if (listViewHR != null) listViewHR.setVisibility(View.GONE);
            if (tvHRData != null) tvHRData.setVisibility(View.VISIBLE);
        }
        
        if (tvHRData != null) {
            if (validCount > 0) {
                double avgHR = totalHR / validCount;
                String hrText = String.format(Locale.getDefault(), 
                    "Average: %.0f bpm\n" +
                    "Range: %d - %d bpm\n" +
                    "Data Points: %d\n\n" +
                    "Tap 'View Details' for comprehensive analysis and trends.",
                    avgHR, minHR, maxHR, validCount);
                tvHRData.setText(hrText);
                Log.d(TAG, "Displayed heart rate summary - avg: " + avgHR + ", count: " + validCount);
            } else {
                tvHRData.setText("No heart rate data available for the last 30 days.");
                Log.w(TAG, "No valid heart rate data in " + data.size() + " records");
            }
        } else {
            Log.e(TAG, "tvHRData is null, cannot display heart rate data");
        }
    }
    
    private void displayTypingSpeedData(List<Map<String, Object>> data) {
        if (tvTypingData == null) {
            tvTypingData = findViewById(R.id.tvTypingData);
        }
        
        Log.d(TAG, "Displaying typing speed data - records: " + (data != null ? data.size() : 0));
        
        if (data == null || data.isEmpty()) {
            Log.w(TAG, "No typing speed data to display");
            if (tvTypingData != null) {
                tvTypingData.setText("No typing speed data available for the last 30 days.");
            }
            if (listViewTyping != null) listViewTyping.setVisibility(View.GONE);
            if (tvTypingData != null) tvTypingData.setVisibility(View.VISIBLE);
            return;
        }
        
        // Calculate statistics and populate ListView
        double totalWPM = 0;
        double totalAccuracy = 0;
        int maxWPM = 0;
        int minWPM = Integer.MAX_VALUE;
        int wpmCount = 0;
        int accuracyCount = 0;
        List<String> typingListItems = new ArrayList<>(); // For ListView (CP470 Requirement #3)
        
        for (Map<String, Object> test : data) {
            try {
                if (test.get("words_per_minute") != null) {
                    int wpm = ((Number) test.get("words_per_minute")).intValue();
                    totalWPM += wpm;
                    if (wpm > maxWPM) maxWPM = wpm;
                    if (wpm < minWPM) minWPM = wpm;
                    wpmCount++;
                    
                    // Add to ListView items
                    double accuracy = test.get("accuracy_percentage") != null ? 
                        ((Number) test.get("accuracy_percentage")).doubleValue() : 0;
                    typingListItems.add(String.format(Locale.getDefault(), 
                        "%d WPM (%.0f%%)", wpm, accuracy));
                }
                if (test.get("accuracy_percentage") != null) {
                    totalAccuracy += ((Number) test.get("accuracy_percentage")).doubleValue();
                    accuracyCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing typing test data: " + test.toString(), e);
            }
        }
        
        // Populate ListView (CP470 Requirement #3)
        if (listViewTyping != null && !typingListItems.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, typingListItems);
            listViewTyping.setAdapter(adapter);
            listViewTyping.setVisibility(View.VISIBLE);
            if (tvTypingData != null) tvTypingData.setVisibility(View.GONE);
        } else {
            if (listViewTyping != null) listViewTyping.setVisibility(View.GONE);
            if (tvTypingData != null) tvTypingData.setVisibility(View.VISIBLE);
        }
        
        if (tvTypingData != null) {
            if (wpmCount > 0) {
                double avgWPM = totalWPM / wpmCount;
                double avgAccuracy = accuracyCount > 0 ? totalAccuracy / accuracyCount : 0;
                
                String typingText = String.format(Locale.getDefault(),
                    "Typing: %.0f WPM (%.0f%% accuracy)\n" +
                    "Tests: %d\n\n" +
                    "Tap 'View Details' for comprehensive analysis and trends.",
                    avgWPM, avgAccuracy, wpmCount);
                tvTypingData.setText(typingText);
                Log.d(TAG, "Displayed typing speed summary - avg WPM: " + avgWPM + ", count: " + wpmCount);
            } else {
                tvTypingData.setText("No typing speed data available for the last 30 days.");
                Log.w(TAG, "No valid typing data in " + data.size() + " records");
            }
        } else {
            Log.e(TAG, "tvTypingData is null, cannot display typing data");
        }
    }
    
    private void displayReactionTimeData(List<Map<String, Object>> data) {
        if (tvTypingData == null) {
            tvTypingData = findViewById(R.id.tvTypingData);
        }
        
        Log.d(TAG, "Displaying reaction time data - records: " + (data != null ? data.size() : 0));
        
        if (data == null || data.isEmpty()) {
            Log.w(TAG, "No reaction time data to display");
            return;
        }
        
        if (tvTypingData == null) {
            Log.e(TAG, "tvTypingData TextView is null, cannot display reaction time data");
            return;
        }
        
        // Calculate statistics
        double totalReaction = 0;
        int minReaction = Integer.MAX_VALUE;
        int maxReaction = 0;
        int validCount = 0;
        
        for (Map<String, Object> test : data) {
            try {
                if (test.get("reaction_time_ms") != null) {
                    int reaction = ((Number) test.get("reaction_time_ms")).intValue();
                    totalReaction += reaction;
                    if (reaction < minReaction) minReaction = reaction;
                    if (reaction > maxReaction) maxReaction = reaction;
                    validCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing reaction time data: " + test.toString(), e);
            }
        }
        
        if (tvTypingData != null && validCount > 0) {
            double avgReaction = totalReaction / validCount;
            String currentText = tvTypingData.getText().toString();
            
            // Append reaction time summary
            String reactionText = String.format(Locale.getDefault(),
                "\n\nReaction Time: %.0f ms\n" +
                "Tests: %d",
                avgReaction, validCount);
            
            // If current text is just the loading message or empty, replace it
            if (currentText == null || currentText.isEmpty() || 
                currentText.contains("Loading") || currentText.contains("No typing")) {
                tvTypingData.setText(String.format(Locale.getDefault(), 
                    "Reaction Time: %.0f ms\nTests: %d\n\nTap 'View Details' for comprehensive analysis.",
                    avgReaction, validCount));
            } else {
                tvTypingData.setText(currentText + reactionText);
            }
            Log.d(TAG, "Updated typing/reaction summary with reaction time - avg: " + avgReaction + ", count: " + validCount);
        } else if (tvTypingData != null && validCount == 0) {
            Log.w(TAG, "No valid reaction time data in " + data.size() + " records");
            // Don't overwrite typing data if reaction time is empty
        } else {
            Log.e(TAG, "tvTypingData is null, cannot append reaction time data");
        }
    }
    
    private void displayBiometricData(List<BiometricData> data) {
        TextView tvHRData = findViewById(R.id.tvHRData);
        TextView tvSleepData = findViewById(R.id.tvSleepData);
        
        if (data == null || data.isEmpty()) {
            if (tvHRData != null) {
                tvHRData.setText("No heart rate data available for the last 30 days.");
            }
            if (tvSleepData != null) {
                tvSleepData.setText("No sleep data available for the last 30 days.");
            }
            return;
        }
        
        // Calculate heart rate statistics
        int heartRateCount = 0;
        double totalHR = 0;
        int minHR = Integer.MAX_VALUE;
        int maxHR = 0;
        
        for (BiometricData bd : data) {
            if (bd.getHeartRate() != null) {
                heartRateCount++;
                int hr = bd.getHeartRate();
                totalHR += hr;
                if (hr < minHR) minHR = hr;
                if (hr > maxHR) maxHR = hr;
            }
        }
        
        // Update heart rate card
        if (tvHRData != null) {
            if (heartRateCount > 0) {
                double avgHR = totalHR / heartRateCount;
                String hrText = String.format(Locale.getDefault(), 
                    "Average Heart Rate: %.0f bpm\n\n" +
                    "Range: %d - %d bpm\n" +
                    "Total Data Points: %d\n" +
                    "Time Period: Last 30 days\n\n" +
                    "Your heart rate shows %s patterns.",
                    avgHR, minHR, maxHR, heartRateCount,
                    avgHR < 70 ? "resting" : avgHR < 85 ? "normal activity" : "elevated");
                tvHRData.setText(hrText);
            } else {
                tvHRData.setText("No heart rate data available for the last 30 days.");
            }
        }
        
    }
    
    private void loadSleepData(String userId, Date startDate, Date endDate) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        try {
            // Query sleep_sessions with date filter
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
                            runOnUiThread(() -> displaySleepData(response.body()));
                        } else {
                            Log.e(TAG, "Failed to fetch sleep data: " + response.code());
                            TextView tvSleepData = findViewById(R.id.tvSleepData);
                            if (tvSleepData != null) {
                                tvSleepData.setText("No sleep data available for the last 30 days.");
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        Log.e(TAG, "Error loading sleep data", t);
                        TextView tvSleepData = findViewById(R.id.tvSleepData);
                        if (tvSleepData != null) {
                            tvSleepData.setText("Error loading sleep data. Please try again later.");
                        }
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception loading sleep data", e);
        }
    }
    
    private void displaySleepData(List<Map<String, Object>> sleepSessions) {
        if (tvSleepData == null) {
            tvSleepData = findViewById(R.id.tvSleepData);
        }
        
        Log.d(TAG, "Displaying sleep data - records: " + (sleepSessions != null ? sleepSessions.size() : 0));
        
        if (sleepSessions == null || sleepSessions.isEmpty()) {
            Log.w(TAG, "No sleep data to display");
            if (tvSleepData != null) {
                tvSleepData.setText("No sleep data available for the last 30 days.");
            }
            if (listViewSleep != null) listViewSleep.setVisibility(View.GONE);
            if (tvSleepData != null) tvSleepData.setVisibility(View.VISIBLE);
            return;
        }
        
        // Calculate statistics and populate ListView (CP470 Requirement #3)
        double totalDuration = 0;
        double totalQuality = 0;
        int qualityCount = 0;
        int count = sleepSessions.size();
        int minDuration = Integer.MAX_VALUE;
        int maxDuration = 0;
        List<String> sleepListItems = new ArrayList<>();
        
        for (Map<String, Object> session : sleepSessions) {
            if (session.get("duration_minutes") != null) {
                int duration = ((Number) session.get("duration_minutes")).intValue();
                totalDuration += duration;
                if (duration < minDuration) minDuration = duration;
                if (duration > maxDuration) maxDuration = duration;
                
                // Add to ListView items
                double quality = session.get("sleep_quality_score") != null ? 
                    ((Number) session.get("sleep_quality_score")).doubleValue() * 100 : 0;
                String timestamp = session.get("sleep_start") != null ? 
                    session.get("sleep_start").toString() : "Unknown";
                sleepListItems.add(String.format(Locale.getDefault(), 
                    "%.1f hours (%.0f%%) - %s",
                    duration / 60.0, quality,
                    timestamp.length() > 10 ? timestamp.substring(0, 10) : timestamp));
            }
            if (session.get("sleep_quality_score") != null) {
                totalQuality += ((Number) session.get("sleep_quality_score")).doubleValue();
                qualityCount++;
            }
        }
        
        // Populate ListView (CP470 Requirement #3)
        if (listViewSleep != null && !sleepListItems.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, sleepListItems);
            listViewSleep.setAdapter(adapter);
            listViewSleep.setVisibility(View.VISIBLE);
            if (tvSleepData != null) tvSleepData.setVisibility(View.GONE);
        } else {
            if (listViewSleep != null) listViewSleep.setVisibility(View.GONE);
            if (tvSleepData != null) tvSleepData.setVisibility(View.VISIBLE);
        }
        
        double avgDuration = totalDuration / count;
        double avgQuality = qualityCount > 0 ? totalQuality / qualityCount : 0;
        double avgHours = avgDuration / 60.0;
        
        if (tvSleepData != null) {
            String sleepText = String.format(Locale.getDefault(),
                "Average: %.1f hours\n" +
                "Quality: %.0f%%\n" +
                "Sessions: %d\n\n" +
                "Tap 'View Details' for comprehensive analysis and trends.",
                avgHours, avgQuality * 100, count);
            tvSleepData.setText(sleepText);
        }
    }
    
    private void displayTypingData(List<TypingSpeedData> data) {
        TextView tvTypingData = findViewById(R.id.tvTypingData);
        
        if (data == null || data.isEmpty()) {
            if (tvTypingData != null) {
                tvTypingData.setText("No typing speed data available for the last 30 days.");
            }
            return;
        }
        
        // Calculate statistics
        double totalWPM = 0;
        double totalAccuracy = 0;
        int maxWPM = 0;
        int minWPM = Integer.MAX_VALUE;
        int count = data.size();
        
        for (TypingSpeedData td : data) {
            int wpm = td.getWordsPerMinute();
            totalWPM += wpm;
            totalAccuracy += td.getAccuracy();
            if (wpm > maxWPM) maxWPM = wpm;
            if (wpm < minWPM) minWPM = wpm;
        }
        
        double avgWPM = totalWPM / count;
        double avgAccuracy = totalAccuracy / count;
        
        // Update typing card
        if (tvTypingData != null) {
            String typingText = String.format(Locale.getDefault(),
                "Typing Speed Performance:\n\n" +
                "Average WPM: %.1f\n" +
                "Range: %d - %d WPM\n" +
                "Average Accuracy: %.1f%%\n" +
                "Tests Completed: %d\n" +
                "Time Period: Last 30 days\n\n" +
                "Your typing speed is %s.",
                avgWPM, minWPM, maxWPM, avgAccuracy, count,
                avgWPM < 40 ? "below average" : avgWPM < 60 ? "average" : "above average");
            tvTypingData.setText(typingText);
        }
    }
    
    private void displayReactionData(List<ReactionTimeData> data) {
        TextView tvTypingData = findViewById(R.id.tvTypingData);
        
        if (data == null || data.isEmpty()) {
            // Don't overwrite typing data if reaction data is empty
            return;
        }
        
        // Calculate statistics
        double totalReaction = 0;
        int minReaction = Integer.MAX_VALUE;
        int maxReaction = 0;
        int count = data.size();
        
        for (ReactionTimeData rd : data) {
            int reaction = rd.getReactionTimeMs();
            totalReaction += reaction;
            if (reaction < minReaction) minReaction = reaction;
            if (reaction > maxReaction) maxReaction = reaction;
        }
        
        double avgReaction = totalReaction / count;
        
        // Update typing card with reaction time info
        if (tvTypingData != null) {
            String currentText = tvTypingData.getText().toString();
            String reactionText = String.format(Locale.getDefault(),
                "\n\nReaction Time Performance:\n\n" +
                "Average: %.0f ms\n" +
                "Range: %d - %d ms\n" +
                "Reaction Tests: %d\n\n" +
                "Your reaction time is %s.",
                avgReaction, minReaction, maxReaction, count,
                avgReaction < 250 ? "excellent" : avgReaction < 300 ? "good" : "average");
            tvTypingData.setText(currentText + reactionText);
        }
    }
    
    private void displayAISummary(List<EnergyPrediction> predictions) {
        TextView tvAISummary = findViewById(R.id.tvAISummary);
        
        if (predictions == null || predictions.isEmpty()) {
            Log.d(TAG, "No predictions for AI summary");
            if (tvAISummary != null) {
                tvAISummary.setText("No energy predictions available yet. Predictions are generated based on your biometric and cognitive data over time.");
            }
            return;
        }
        
        // Analyze predictions
        int highCount = 0, mediumCount = 0, lowCount = 0;
        double totalConfidence = 0;
        
        for (EnergyPrediction pred : predictions) {
            totalConfidence += pred.getConfidence();
            switch (pred.getPredictedLevel()) {
                case HIGH:
                    highCount++;
                    break;
                case MEDIUM:
                    mediumCount++;
                    break;
                case LOW:
                    lowCount++;
                    break;
            }
        }
        
        double avgConfidence = totalConfidence / predictions.size();
        
        // Generate summary
        String summary = String.format(Locale.getDefault(),
            "Based on %d energy predictions over the last 30 days:\n\n" +
            "• High Energy: %d times (%.0f%%)\n" +
            "• Medium Energy: %d times (%.0f%%)\n" +
            "• Low Energy: %d times (%.0f%%)\n\n" +
            "Average Confidence: %.0f%%\n\n" +
            "Your energy patterns show %s periods. " +
            "Consider scheduling high-intensity tasks during your peak energy times.",
            predictions.size(),
            highCount, (highCount * 100.0 / predictions.size()),
            mediumCount, (mediumCount * 100.0 / predictions.size()),
            lowCount, (lowCount * 100.0 / predictions.size()),
            avgConfidence * 100,
            highCount > mediumCount && highCount > lowCount ? "consistent high-energy" :
            lowCount > highCount && lowCount > mediumCount ? "frequent low-energy" :
            "balanced");
        
        if (tvAISummary != null) {
            tvAISummary.setText(summary);
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
                finish();
                return true;
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
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
        bottomNav.setSelectedItemId(R.id.nav_data);
    }
}

