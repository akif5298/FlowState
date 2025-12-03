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
    
    public BiometricDataRepository(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
     * Note: This method aggregates data from multiple tables
     */
    public void getBiometricData(String userId, Date startDate, Date endDate, DataCallback callback) {
        // This is complex - would need to join heart_rate_readings, sleep_sessions, temperature_readings
        // For now, just get heart rate data as a simple implementation
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("timestamp", "gte." + dateFormat.format(startDate) + ",lte." + dateFormat.format(endDate));
        queryParams.put("order", "timestamp.desc");
        
        postgrestApi.getHeartRateReadings(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<BiometricData> biometricDataList = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                try {
                                    Date timestamp = dateFormat.parse(map.get("timestamp").toString());
                                    Integer heartRate = map.get("heart_rate_bpm") != null ? 
                                        ((Number) map.get("heart_rate_bpm")).intValue() : null;
                                    BiometricData data = new BiometricData(timestamp, heartRate, null, null, null);
                                    biometricDataList.add(data);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.onSuccess(biometricDataList);
                        } else {
                            callback.onError(new Exception("Failed to fetch biometric data"));
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }
    
    /**
     * Get latest biometric data for a user
     */
    public void getLatestBiometricData(String userId, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("order", "timestamp.desc");
        queryParams.put("limit", "1");
        
        postgrestApi.getHeartRateReadings(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = dateFormat.parse(map.get("timestamp").toString());
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
