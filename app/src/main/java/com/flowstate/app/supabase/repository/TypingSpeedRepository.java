package com.flowstate.app.supabase.repository;

import android.content.Context;
import com.flowstate.app.data.models.TypingSpeedData;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Repository for managing typing speed data in Supabase (Java implementation)
 */
public class TypingSpeedRepository {
    
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    
    public TypingSpeedRepository(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Insert typing speed data
     */
    public void insertTypingSpeedData(String userId, TypingSpeedData typingSpeedData, DataCallback callback) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_id", userId);
        dataMap.put("timestamp", dateFormat.format(typingSpeedData.getTimestamp()));
        dataMap.put("words_per_minute", typingSpeedData.getWordsPerMinute());
        dataMap.put("accuracy_percentage", typingSpeedData.getAccuracy());
        dataMap.put("sample_text", typingSpeedData.getSampleText());
        
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        postgrestApi.insertTypingSpeedTest(authorization, apikey, "return=representation", dataMap)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            String error = "Failed to save typing speed data";
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
     * Get typing speed data for a user within a date range
     */
    public void getTypingSpeedData(String userId, Date startDate, Date endDate, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("timestamp", "gte." + dateFormat.format(startDate) + ",lte." + dateFormat.format(endDate));
        queryParams.put("order", "timestamp.desc");
        
        postgrestApi.getTypingSpeedTests(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<TypingSpeedData> typingSpeedDataList = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                try {
                                    Date timestamp = dateFormat.parse(map.get("timestamp").toString());
                                    TypingSpeedData data = new TypingSpeedData(
                                        timestamp,
                                        ((Number) map.get("words_per_minute")).intValue(),
                                        ((Number) map.get("accuracy_percentage")).doubleValue(),
                                        map.get("sample_text") != null ? map.get("sample_text").toString() : ""
                                    );
                                    typingSpeedDataList.add(data);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.onSuccess(typingSpeedDataList);
                        } else {
                            callback.onError(new Exception("Failed to fetch typing speed data"));
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }
    
    /**
     * Get latest typing speed data for a user
     */
    public void getLatestTypingSpeedData(String userId, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("order", "timestamp.desc");
        queryParams.put("limit", "1");
        
        postgrestApi.getTypingSpeedTests(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = dateFormat.parse(map.get("timestamp").toString());
                                TypingSpeedData data = new TypingSpeedData(
                                    timestamp,
                                    ((Number) map.get("words_per_minute")).intValue(),
                                    ((Number) map.get("accuracy_percentage")).doubleValue(),
                                    map.get("sample_text") != null ? map.get("sample_text").toString() : ""
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

