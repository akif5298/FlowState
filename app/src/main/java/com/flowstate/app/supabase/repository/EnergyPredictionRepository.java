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
    
    public EnergyPredictionRepository(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
        
        // Convert maps to JSON strings (Gson will handle this)
        if (energyPrediction.getBiometricFactors() != null) {
            dataMap.put("biometric_factors", energyPrediction.getBiometricFactors());
        }
        if (energyPrediction.getCognitiveFactors() != null) {
            dataMap.put("cognitive_factors", energyPrediction.getCognitiveFactors());
        }
        
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
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("prediction_time", "gte." + dateFormat.format(startDate) + ",lte." + dateFormat.format(endDate));
        queryParams.put("order", "prediction_time.desc");
        
        postgrestApi.getEnergyPredictions(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<EnergyPrediction> predictions = new ArrayList<>();
                            for (Map<String, Object> map : response.body()) {
                                try {
                                    Date timestamp = dateFormat.parse(map.get("prediction_time").toString());
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
                                    predictions.add(prediction);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.onSuccess(predictions);
                        } else {
                            callback.onError(new Exception("Failed to fetch energy predictions"));
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
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        queryParams.put("order", "prediction_time.desc");
        queryParams.put("limit", "1");
        
        postgrestApi.getEnergyPredictions(authorization, apikey, queryParams)
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, 
                                         Response<List<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Map<String, Object> map = response.body().get(0);
                            try {
                                Date timestamp = dateFormat.parse(map.get("prediction_time").toString());
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

