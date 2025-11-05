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
     */
    public void upsertBiometricData(String userId, BiometricData biometricData, DataCallback callback) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_id", userId);
        dataMap.put("timestamp", dateFormat.format(biometricData.getTimestamp()));
        dataMap.put("heart_rate", biometricData.getHeartRate());
        dataMap.put("sleep_minutes", biometricData.getSleepMinutes());
        dataMap.put("sleep_quality", biometricData.getSleepQuality());
        dataMap.put("skin_temperature", biometricData.getSkinTemperature());
        
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        // Use upsert via POST with Prefer header
        postgrestApi.insertHeartRateReading(authorization, apikey, "resolution=merge-duplicates", dataMap)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            String error = "Failed to save biometric data";
                            try {
                                if (response.errorBody() != null) {
                                    error = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            callback.onError(new Exception(error));
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }
    
    /**
     * Get biometric data for a user within a date range
     */
    public void getBiometricData(String userId, Date startDate, Date endDate, DataCallback callback) {
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
                                    BiometricData data = new BiometricData(
                                        timestamp,
                                        map.get("heart_rate") != null ? 
                                            ((Number) map.get("heart_rate")).intValue() : null,
                                        map.get("sleep_minutes") != null ? 
                                            ((Number) map.get("sleep_minutes")).intValue() : null,
                                        map.get("sleep_quality") != null ? 
                                            ((Number) map.get("sleep_quality")).doubleValue() : null,
                                        map.get("skin_temperature") != null ? 
                                            ((Number) map.get("skin_temperature")).doubleValue() : null
                                    );
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
        
        // Get one record, ordered by timestamp descending
        postgrestApi.getHeartRateReadings(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = dateFormat.parse(map.get("timestamp").toString());
                                BiometricData data = new BiometricData(
                                    timestamp,
                                    map.get("heart_rate") != null ? 
                                        ((Number) map.get("heart_rate")).intValue() : null,
                                    map.get("sleep_minutes") != null ? 
                                        ((Number) map.get("sleep_minutes")).intValue() : null,
                                    map.get("sleep_quality") != null ? 
                                        ((Number) map.get("sleep_quality")).doubleValue() : null,
                                    map.get("skin_temperature") != null ? 
                                        ((Number) map.get("skin_temperature")).doubleValue() : null
                                );
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

