package com.flowstate.app.supabase.repository;

import android.content.Context;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.supabase.SupabaseClient;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Repository for managing energy predictions in Supabase (Java implementation)
 */
public class EnergyPredictionRepository {
    
    private SupabaseClient supabaseClient;
    private SupabasePostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    
    private SimpleDateFormat dateFormatWithOffset;
    
    public EnergyPredictionRepository(Context context) {
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
     * Insert energy prediction
     */
    public void insertEnergyPrediction(String userId, EnergyPrediction energyPrediction, DataCallback callback) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_id", userId);
        dataMap.put("prediction_time", dateFormat.format(energyPrediction.getTimestamp()));
        dataMap.put("predicted_level", energyPrediction.getPredictedLevel().name());
        dataMap.put("confidence_score", energyPrediction.getConfidence());
        // Note: ml_model_version has a default in schema, but can be set if needed
        
        // Note: The schema uses a separate energy_prediction_factors table for factors
        // For now, we'll insert the prediction first, then factors can be added separately
        // if needed. The factors are stored in the EnergyPrediction model but not in the
        // main predictions table per the schema.
        
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        postgrestApi.insertEnergyPrediction(authorization, apikey, "return=representation", dataMap)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(null);
                        } else {
                            String error = "Failed to save energy prediction";
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
     * Get energy predictions for a user within a date range
     */
    public void getEnergyPredictions(String userId, Date startDate, Date endDate, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        String predUserId = "eq." + userId;
        String predTimeGte = "gte." + dateFormat.format(startDate);
        String predTimeLte = "lte." + dateFormat.format(endDate);
        String predOrder = "prediction_time.desc";
        
        postgrestApi.getEnergyPredictions(authorization, apikey, predUserId, predTimeGte, predTimeLte, predOrder)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        android.util.Log.d("EnergyPredictionRepository", "Response - isSuccessful: " + response.isSuccessful() + 
                            ", code: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            android.util.Log.d("EnergyPredictionRepository", "Received " + response.body().size() + " energy predictions");
                            if (response.body().isEmpty()) {
                                android.util.Log.w("EnergyPredictionRepository", "Response body is EMPTY - no predictions in database");
                            } else {
                                android.util.Log.d("EnergyPredictionRepository", "First prediction keys: " + response.body().get(0).keySet());
                                android.util.Log.d("EnergyPredictionRepository", "First prediction: " + response.body().get(0).toString());
                            }
                            List<EnergyPrediction> predictions = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                try {
                                    android.util.Log.d("EnergyPredictionRepository", "Parsing prediction: " + map.toString());
                                    Date timestamp = parseDate(map.get("prediction_time").toString());
                                    EnergyLevel level = EnergyLevel.valueOf(map.get("predicted_level").toString());
                                    double confidence = ((Number) map.get("confidence_score")).doubleValue();
                                    
                                    android.util.Log.d("EnergyPredictionRepository", "Parsed - timestamp: " + timestamp + 
                                        ", level: " + level + ", confidence: " + confidence);
                                    
                                    // Note: Factors are stored in energy_prediction_factors table, not here
                                    // For now, set factors to null - they would need to be fetched separately
                                    // if needed from the energy_prediction_factors table
                                    Map<String, Double> biometricFactors = null;
                                    Map<String, Double> cognitiveFactors = null;
                                    
                                    EnergyPrediction prediction = new EnergyPrediction(
                                        timestamp, level, confidence, biometricFactors, cognitiveFactors
                                    );
                                    predictions.add(prediction);
                                } catch (Exception e) {
                                    android.util.Log.e("EnergyPredictionRepository", "Error parsing prediction: " + map.toString(), e);
                                    e.printStackTrace();
                                }
                            }
                            android.util.Log.d("EnergyPredictionRepository", "Successfully parsed " + predictions.size() + " predictions");
                            callback.onSuccess(predictions);
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                    android.util.Log.e("EnergyPredictionRepository", "Error body: " + errorBody);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("EnergyPredictionRepository", "Error reading error body", e);
                            }
                            android.util.Log.e("EnergyPredictionRepository", "Failed to fetch energy predictions - code: " + 
                                (response != null ? response.code() : "null"));
                            callback.onError(new Exception("Failed to fetch energy predictions: " + 
                                (response != null ? "HTTP " + response.code() : "null response") + 
                                (errorBody.isEmpty() ? "" : " - " + errorBody)));
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }
    
    /**
     * Get latest energy prediction for a user
     */
    public void getLatestEnergyPrediction(String userId, DataCallback callback) {
        String authorization = "Bearer " + supabaseClient.getAccessToken();
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        String predUserId = "eq." + userId;
        String predOrder = "prediction_time.desc";
        // No date range for latest data, so pass null for gte and lte
        postgrestApi.getEnergyPredictions(authorization, apikey, predUserId, null, null, predOrder)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = parseDate(map.get("prediction_time").toString());
                                EnergyLevel level = EnergyLevel.valueOf(map.get("predicted_level").toString());
                                double confidence = ((Number) map.get("confidence_score")).doubleValue();
                                
                                // Parse JSON maps
                                Map<String, Double> biometricFactors = null;
                                Map<String, Double> cognitiveFactors = null;
                                
                                if (map.get("biometric_factors") != null) {
                                    Object bioObj = map.get("biometric_factors");
                                    if (bioObj instanceof Map) {
                                        biometricFactors = new HashMap<>();
                                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) bioObj).entrySet()) {
                                            Object value = entry.getValue();
                                            if (value instanceof Number) {
                                                biometricFactors.put(entry.getKey().toString(), 
                                                    ((Number) value).doubleValue());
                                            }
                                        }
                                    }
                                }
                                
                                if (map.get("cognitive_factors") != null) {
                                    Object cogObj = map.get("cognitive_factors");
                                    if (cogObj instanceof Map) {
                                        cognitiveFactors = new HashMap<>();
                                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) cogObj).entrySet()) {
                                            Object value = entry.getValue();
                                            if (value instanceof Number) {
                                                cognitiveFactors.put(entry.getKey().toString(), 
                                                    ((Number) value).doubleValue());
                                            }
                                        }
                                    }
                                }
                                
                                EnergyPrediction prediction = new EnergyPrediction(
                                    timestamp, level, confidence, biometricFactors, cognitiveFactors
                                );
                                callback.onSuccess(prediction);
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

