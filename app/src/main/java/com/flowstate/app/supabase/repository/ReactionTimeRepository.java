package com.flowstate.app.supabase.repository;

import android.content.Context;
import com.flowstate.app.data.models.ReactionTimeData;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Repository for managing reaction time data in Supabase (Java implementation)
 */
public class ReactionTimeRepository {
    
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    
    public ReactionTimeRepository(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Insert reaction time data
     * Maps to reaction_time_tests table
     */
    public void insertReactionTimeData(String userId, ReactionTimeData reactionTimeData, DataCallback callback) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_id", userId);
        dataMap.put("timestamp", dateFormat.format(reactionTimeData.getTimestamp()));
        dataMap.put("reaction_time_ms", reactionTimeData.getReactionTimeMs());
        if (reactionTimeData.getTestType() != null) {
            dataMap.put("test_type", reactionTimeData.getTestType());
        }
        if (reactionTimeData.getAttempts() != null) {
            dataMap.put("attempts", reactionTimeData.getAttempts());
        }
        if (reactionTimeData.getAverageReactionTimeMs() != null) {
            dataMap.put("average_reaction_time_ms", reactionTimeData.getAverageReactionTimeMs());
        }
        
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        postgrestApi.insertReactionTimeTest(authorization, apikey, "return=representation", dataMap)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            String error = "Failed to save reaction time data";
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
     * Get reaction time data for a user within a date range
     */
    public void getReactionTimeData(String userId, Date startDate, Date endDate, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("timestamp", "gte." + dateFormat.format(startDate) + ",lte." + dateFormat.format(endDate));
        queryParams.put("order", "timestamp.desc");
        
        postgrestApi.getReactionTimeTests(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ReactionTimeData> reactionTimeDataList = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                try {
                                    Date timestamp = dateFormat.parse(map.get("timestamp").toString());
                                    String testType = map.get("test_type") != null ? 
                                        map.get("test_type").toString() : "visual";
                                    Integer attempts = map.get("attempts") != null ? 
                                        ((Number) map.get("attempts")).intValue() : 1;
                                    Double avgReactionTime = map.get("average_reaction_time_ms") != null ? 
                                        ((Number) map.get("average_reaction_time_ms")).doubleValue() : null;
                                    
                                    ReactionTimeData data = new ReactionTimeData(
                                        timestamp,
                                        ((Number) map.get("reaction_time_ms")).intValue(),
                                        testType, attempts, avgReactionTime
                                    );
                                    reactionTimeDataList.add(data);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.onSuccess(reactionTimeDataList);
                        } else {
                            callback.onError(new Exception("Failed to fetch reaction time data"));
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }
    
    /**
     * Get latest reaction time data for a user
     */
    public void getLatestReactionTimeData(String userId, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("order", "timestamp.desc");
        queryParams.put("limit", "1");
        
        postgrestApi.getReactionTimeTests(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = dateFormat.parse(map.get("timestamp").toString());
                                ReactionTimeData data = new ReactionTimeData(
                                    timestamp,
                                    ((Number) map.get("reaction_time_ms")).intValue()
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

