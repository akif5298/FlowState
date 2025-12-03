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
    
    private SimpleDateFormat dateFormatWithOffset;
    
    public TypingSpeedRepository(Context context) {
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
     * Insert typing speed data
     * Maps to typing_speed_tests table
     */
    public void insertTypingSpeedData(String userId, TypingSpeedData typingSpeedData, DataCallback callback) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_id", userId);
        dataMap.put("timestamp", dateFormat.format(typingSpeedData.getTimestamp()));
        dataMap.put("words_per_minute", typingSpeedData.getWordsPerMinute());
        dataMap.put("accuracy_percentage", typingSpeedData.getAccuracy());
        dataMap.put("sample_text", typingSpeedData.getSampleText());
        if (typingSpeedData.getTotalCharacters() != null) {
            dataMap.put("total_characters", typingSpeedData.getTotalCharacters());
        }
        if (typingSpeedData.getErrors() != null) {
            dataMap.put("errors", typingSpeedData.getErrors());
        }
        if (typingSpeedData.getDurationSeconds() != null) {
            dataMap.put("duration_seconds", typingSpeedData.getDurationSeconds());
        }
        
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
                            List<TypingSpeedData> typingSpeedDataList = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                try {
                                    Date timestamp = parseDate(map.get("timestamp").toString());
                                    Integer totalChars = map.get("total_characters") != null ? 
                                        ((Number) map.get("total_characters")).intValue() : null;
                                    Integer errors = map.get("errors") != null ? 
                                        ((Number) map.get("errors")).intValue() : null;
                                    Integer durationSecs = map.get("duration_seconds") != null ? 
                                        ((Number) map.get("duration_seconds")).intValue() : null;
                                    
                                    TypingSpeedData data = new TypingSpeedData(
                                        timestamp,
                                        ((Number) map.get("words_per_minute")).intValue(),
                                        ((Number) map.get("accuracy_percentage")).doubleValue(),
                                        map.get("sample_text") != null ? map.get("sample_text").toString() : "",
                                        totalChars, errors, durationSecs
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
        
        String typingUserId = "eq." + userId;
        String typingOrder = "timestamp.desc";
        // No date range for latest data, so pass null for gte and lte
        postgrestApi.getTypingSpeedTests(authorization, apikey, typingUserId, null, null, typingOrder)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = parseDate(map.get("timestamp").toString());
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

