package com.flowstate.app.supabase.repository;

import android.content.Context;
import com.flowstate.app.data.models.BiometricData;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Repository for managing biometric data in Supabase (Java implementation)
 * Maps to: heart_rate_readings, sleep_sessions, temperature_readings tables
 */
public class BiometricDataRepository {
    
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dateFormatWithOffset;
    
    public BiometricDataRepository(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Format for dates with timezone offset like +00:00
        this.dateFormatWithOffset = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        this.dateFormatWithOffset.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Parse date string that can be in multiple formats (Z or +00:00, with or without microseconds)
     */
    private Date parseDate(String dateString) throws java.text.ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        // Try parsing with Z format first (milliseconds)
        try {
            return dateFormat.parse(dateString);
        } catch (java.text.ParseException e) {
            // Try parsing with timezone offset format (milliseconds)
            try {
                return dateFormatWithOffset.parse(dateString);
            } catch (java.text.ParseException e2) {
                // Try with microseconds and Z format
                try {
                    SimpleDateFormat microsZFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
                    microsZFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return microsZFormat.parse(dateString);
                } catch (java.text.ParseException e3) {
                    // Try with microseconds and timezone offset
                    try {
                        SimpleDateFormat microsOffsetFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US);
                        microsOffsetFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        return microsOffsetFormat.parse(dateString);
                    } catch (java.text.ParseException e4) {
                        // Try without milliseconds - Z format
                        try {
                            SimpleDateFormat noMsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                            noMsFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return noMsFormat.parse(dateString);
                        } catch (java.text.ParseException e5) {
                            // Try with offset and no milliseconds
                            SimpleDateFormat noMsOffsetFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                            noMsOffsetFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return noMsOffsetFormat.parse(dateString);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Insert or update biometric data
     * Splits data into separate tables: heart_rate_readings, sleep_sessions, temperature_readings
     */
    public void upsertBiometricData(String userId, BiometricData biometricData, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        String timestamp = dateFormat.format(biometricData.getTimestamp());
        
        // Track completion
        final java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger totalCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final Throwable[] firstError = {null};
        
        // Helper to check if all operations completed
        Runnable checkComplete = () -> {
            int completed = completedCount.incrementAndGet();
            int total = totalCount.get();
            if (completed >= total) {
                if (firstError[0] != null && successCount.get() == 0) {
                    callback.onError(firstError[0]);
                } else {
                    callback.onSuccess(null);
                }
            }
        };
        
        // Insert heart rate if available
        if (biometricData.getHeartRate() != null) {
            totalCount.incrementAndGet();
            Map<String, Object> hrData = new HashMap<>();
            hrData.put("user_id", userId);
            hrData.put("timestamp", timestamp);
            hrData.put("heart_rate_bpm", biometricData.getHeartRate());
            hrData.put("source", "google_fit");
            
            postgrestApi.insertHeartRateReading(authorization, apikey, "resolution=merge-duplicates", hrData)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                if (firstError[0] == null) {
                                    try {
                                        String error = response.errorBody() != null ? 
                                            response.errorBody().string() : "Failed to save heart rate";
                                        firstError[0] = new Exception(error);
                                    } catch (IOException e) {
                                        firstError[0] = e;
                                    }
                                }
                            }
                            checkComplete.run();
                        }
                        
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (firstError[0] == null) {
                                firstError[0] = t;
                            }
                            checkComplete.run();
                        }
                    });
        }
        
        // Insert sleep session if available
        if (biometricData.getSleepMinutes() != null || biometricData.getSleepQuality() != null) {
            totalCount.incrementAndGet();
            Map<String, Object> sleepData = new HashMap<>();
            sleepData.put("user_id", userId);
            sleepData.put("sleep_start", timestamp);
            if (biometricData.getSleepMinutes() != null) {
                sleepData.put("duration_minutes", biometricData.getSleepMinutes());
            }
            if (biometricData.getSleepQuality() != null) {
                sleepData.put("sleep_quality_score", biometricData.getSleepQuality());
            }
            sleepData.put("source", "google_fit");
            
            postgrestApi.insertSleepSession(authorization, apikey, "return=representation", sleepData)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                if (firstError[0] == null) {
                                    try {
                                        String error = response.errorBody() != null ? 
                                            response.errorBody().string() : "Failed to save sleep data";
                                        firstError[0] = new Exception(error);
                                    } catch (IOException e) {
                                        firstError[0] = e;
                                    }
                                }
                            }
                            checkComplete.run();
                        }
                        
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (firstError[0] == null) {
                                firstError[0] = t;
                            }
                            checkComplete.run();
                        }
                    });
        }
        
        // Insert temperature if available
        if (biometricData.getSkinTemperature() != null) {
            totalCount.incrementAndGet();
            Map<String, Object> tempData = new HashMap<>();
            tempData.put("user_id", userId);
            tempData.put("timestamp", timestamp);
            tempData.put("temperature_celsius", biometricData.getSkinTemperature());
            tempData.put("temperature_type", "skin");
            tempData.put("source", "google_fit");
            
            postgrestApi.insertTemperatureReading(authorization, apikey, "resolution=merge-duplicates", tempData)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                if (firstError[0] == null) {
                                    try {
                                        String error = response.errorBody() != null ? 
                                            response.errorBody().string() : "Failed to save temperature";
                                        firstError[0] = new Exception(error);
                                    } catch (IOException e) {
                                        firstError[0] = e;
                                    }
                                }
                            }
                            checkComplete.run();
                        }
                        
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (firstError[0] == null) {
                                firstError[0] = t;
                            }
                            checkComplete.run();
                        }
                    });
        }
        
        // If no data to insert, return success immediately
        if (totalCount.get() == 0) {
            callback.onSuccess(null);
        }
    }
    
    /**
     * Get biometric data for a user within a date range
     * Aggregates data from heart_rate_readings and sleep_sessions tables
     */
    public void getBiometricData(String userId, Date startDate, Date endDate, DataCallback callback) {
        String accessToken = supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        // Check if we have valid credentials
        if (accessToken == null || accessToken.isEmpty()) {
            android.util.Log.e("BiometricDataRepository", "Access token is null or empty");
            callback.onError(new Exception("Not authenticated. Please log in again."));
            return;
        }
        
        if (apikey == null || apikey.isEmpty()) {
            android.util.Log.e("BiometricDataRepository", "API key is null or empty");
            callback.onError(new Exception("API key not configured."));
            return;
        }
        
        String authorization = "Bearer " + accessToken;
        
        // Track completion of both API calls
        final java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final List<BiometricData> heartRateData = new ArrayList<>();
        final Map<String, BiometricData> sleepDataMap = new HashMap<>(); // Key: date string
        final Throwable[] firstError = {null};
        
        // Helper to combine data when both calls complete
        Runnable combineData = () -> {
            int completed = completedCount.incrementAndGet();
            if (completed >= 2) {
                if (firstError[0] != null && heartRateData.isEmpty() && sleepDataMap.isEmpty()) {
                    callback.onError(firstError[0]);
                    return;
                }
                
                // Combine heart rate and sleep data
                // Create a map of dates to BiometricData for merging
                Map<String, BiometricData> combinedMap = new HashMap<>();
                
                // Add heart rate data
                for (BiometricData hrData : heartRateData) {
                    String dateKey = dateFormat.format(hrData.getTimestamp());
                    combinedMap.put(dateKey, hrData);
                }
                
                // Merge sleep data
                for (BiometricData sleepData : sleepDataMap.values()) {
                    String dateKey = dateFormat.format(sleepData.getTimestamp());
                    if (combinedMap.containsKey(dateKey)) {
                        // Merge: combine heart rate and sleep
                        BiometricData existing = combinedMap.get(dateKey);
                        BiometricData merged = new BiometricData(
                            existing.getTimestamp(),
                            existing.getHeartRate(),
                            sleepData.getSleepMinutes(),
                            sleepData.getSleepQuality(),
                            existing.getSkinTemperature()
                        );
                        combinedMap.put(dateKey, merged);
                    } else {
                        combinedMap.put(dateKey, sleepData);
                    }
                }
                
                List<BiometricData> result = new ArrayList<>(combinedMap.values());
                android.util.Log.d("BiometricDataRepository", "Combined " + result.size() + " biometric records");
                android.util.Log.d("BiometricDataRepository", "Heart rate records: " + heartRateData.size() + 
                    ", Sleep records: " + sleepDataMap.size());
                if (result.isEmpty()) {
                    android.util.Log.w("BiometricDataRepository", "WARNING: No biometric data to return! Check if data exists in database.");
                }
                callback.onSuccess(result);
            }
        };
        
        // Fetch heart rate data
        String hrUserId = "eq." + userId;
        String hrTimestampGte = "gte." + dateFormat.format(startDate);
        String hrTimestampLte = "lte." + dateFormat.format(endDate);
        String hrOrder = "timestamp.desc";
        
        android.util.Log.d("BiometricDataRepository", "Fetching heart rate data for user: " + userId);
        android.util.Log.d("BiometricDataRepository", "Date range: " + dateFormat.format(startDate) + " to " + dateFormat.format(endDate));
        android.util.Log.d("BiometricDataRepository", "Authorization header present: " + (authorization != null && !authorization.isEmpty()));
        android.util.Log.d("BiometricDataRepository", "API key present: " + (apikey != null && !apikey.isEmpty()));
        
        postgrestApi.getHeartRateReadings(authorization, apikey, hrUserId, hrTimestampGte, hrTimestampLte, hrOrder)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        android.util.Log.d("BiometricDataRepository", "Heart rate response - isSuccessful: " + response.isSuccessful() + 
                            ", code: " + response.code());
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                android.util.Log.d("BiometricDataRepository", "Received " + response.body().size() + " heart rate readings");
                                if (response.body().isEmpty()) {
                                    android.util.Log.w("BiometricDataRepository", "Heart rate response body is EMPTY - no data in database for this user/date range");
                                } else {
                                    // Log first record to see structure
                                    android.util.Log.d("BiometricDataRepository", "First heart rate record keys: " + response.body().get(0).keySet());
                                    android.util.Log.d("BiometricDataRepository", "First heart rate record: " + response.body().get(0).toString());
                                }
                                for (Map<String, Object> map : response.body()) {
                                    try {
                                        android.util.Log.d("BiometricDataRepository", "Parsing heart rate record: " + map.toString());
                                        Date timestamp = parseDate(map.get("timestamp").toString());
                                        Integer heartRate = map.get("heart_rate_bpm") != null ? 
                                            ((Number) map.get("heart_rate_bpm")).intValue() : null;
                                        android.util.Log.d("BiometricDataRepository", "Parsed - timestamp: " + timestamp + ", heartRate: " + heartRate);
                                        BiometricData data = new BiometricData(timestamp, heartRate, null, null, null);
                                        heartRateData.add(data);
                                    } catch (Exception e) {
                                        android.util.Log.e("BiometricDataRepository", "Error parsing heart rate data: " + map.toString(), e);
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                android.util.Log.w("BiometricDataRepository", "Heart rate response body is NULL");
                            }
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                    android.util.Log.e("BiometricDataRepository", "Heart rate API error body: " + errorBody);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("BiometricDataRepository", "Error reading error body", e);
                            }
                            android.util.Log.e("BiometricDataRepository", "API error fetching heart rate: HTTP " + response.code());
                            android.util.Log.e("BiometricDataRepository", "Response message: " + response.message());
                            if (firstError[0] == null) {
                                firstError[0] = new Exception("Failed to fetch heart rate data: HTTP " + response.code() + 
                                    (errorBody.isEmpty() ? "" : " - " + errorBody));
                            }
                        }
                        combineData.run();
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        android.util.Log.e("BiometricDataRepository", "Network error fetching heart rate data", t);
                        if (firstError[0] == null) {
                            firstError[0] = t;
                        }
                        combineData.run();
                    }
                });
        
        // Fetch sleep data
        String userFilter = "eq." + userId;
        String sleepStartGte = "gte." + dateFormat.format(startDate);
        String sleepStartLte = "lte." + dateFormat.format(endDate);
        String sleepOrder = "sleep_start.desc";
        android.util.Log.d("BiometricDataRepository", "Fetching sleep data for user: " + userId);
        android.util.Log.d("BiometricDataRepository", "Sleep query - user_id: " + userFilter + ", sleep_start: " + sleepStartGte + " to " + sleepStartLte);
        
        postgrestApi.getSleepSessions(authorization, apikey, userFilter, sleepStartGte, sleepStartLte, sleepOrder)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        android.util.Log.d("BiometricDataRepository", "Sleep response - isSuccessful: " + response.isSuccessful() + 
                            ", code: " + response.code());
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                android.util.Log.d("BiometricDataRepository", "Received " + response.body().size() + " sleep sessions");
                                if (response.body().isEmpty()) {
                                    android.util.Log.w("BiometricDataRepository", "Sleep response body is EMPTY - no data in database for this user/date range");
                                } else {
                                    // Log first record to see structure
                                    android.util.Log.d("BiometricDataRepository", "First sleep record keys: " + response.body().get(0).keySet());
                                    android.util.Log.d("BiometricDataRepository", "First sleep record: " + response.body().get(0).toString());
                                }
                                for (Map<String, Object> map : response.body()) {
                                    try {
                                        android.util.Log.d("BiometricDataRepository", "Parsing sleep record: " + map.toString());
                                        Date sleepStart = parseDate(map.get("sleep_start").toString());
                                        // Use sleep_start as the timestamp for the BiometricData
                                        Integer sleepMinutes = map.get("duration_minutes") != null ? 
                                            ((Number) map.get("duration_minutes")).intValue() : null;
                                        Double sleepQuality = map.get("sleep_quality_score") != null ? 
                                            ((Number) map.get("sleep_quality_score")).doubleValue() : null;
                                        
                                        android.util.Log.d("BiometricDataRepository", "Parsed - sleepStart: " + sleepStart + 
                                            ", sleepMinutes: " + sleepMinutes + ", sleepQuality: " + sleepQuality);
                                        
                                        BiometricData data = new BiometricData(sleepStart, null, sleepMinutes, sleepQuality, null);
                                        // Use date as key (without time) for merging
                                        String dateKey = dateFormat.format(sleepStart);
                                        sleepDataMap.put(dateKey, data);
                                    } catch (Exception e) {
                                        android.util.Log.e("BiometricDataRepository", "Error parsing sleep data: " + map.toString(), e);
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                android.util.Log.w("BiometricDataRepository", "Sleep response body is NULL");
                            }
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                    android.util.Log.e("BiometricDataRepository", "Sleep API error body: " + errorBody);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("BiometricDataRepository", "Error reading error body", e);
                            }
                            android.util.Log.e("BiometricDataRepository", "API error fetching sleep: HTTP " + response.code());
                            android.util.Log.e("BiometricDataRepository", "Response message: " + response.message());
                            if (firstError[0] == null) {
                                firstError[0] = new Exception("Failed to fetch sleep data: HTTP " + response.code() + 
                                    (errorBody.isEmpty() ? "" : " - " + errorBody));
                            }
                        }
                        combineData.run();
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        android.util.Log.e("BiometricDataRepository", "Network error fetching sleep data", t);
                        if (firstError[0] == null) {
                            firstError[0] = t;
                        }
                        combineData.run();
                    }
                });
    }
    
    /**
     * Get latest biometric data for a user
     */
    public void getLatestBiometricData(String userId, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        String hrUserId = "eq." + userId;
        String hrOrder = "timestamp.desc";
        // No date range for latest data, so pass null for gte and lte
        postgrestApi.getHeartRateReadings(authorization, apikey, hrUserId, null, null, hrOrder)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = parseDate(map.get("timestamp").toString());
                                Integer heartRate = map.get("heart_rate_bpm") != null ? 
                                    ((Number) map.get("heart_rate_bpm")).intValue() : null;
                                BiometricData data = new BiometricData(timestamp, heartRate, null, null, null);
                                callback.onSuccess(data);
                            } catch (Exception e) {
                                callback.onError(e);
                            }
                        } else {
                            callback.onSuccess(null);
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }
    
    /**
     * Callback interface for data operations
     */
    public interface DataCallback {
        void onSuccess(Object data);
        void onError(Throwable error);
    }
}
