package com.personaleenergy.app.api;

import android.util.Log;
import com.flowstate.app.data.models.BiometricData;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.ReactionTimeData;
import com.flowstate.app.data.models.TypingSpeedData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Chronos API Service for cloud-based energy prediction
 * Uses Chronos time series forecasting API hosted on Railway
 * Primary method for energy predictions, with on-device ML as fallback
 */
public class ChronosApiService {
    
    private static final String TAG = "ChronosApiService";
    private static final String CHRONOS_API_BASE_URL = "https://flowstate-production-9527.up.railway.app";
    private static final String PREDICT_ENDPOINT = "/predict";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final SimpleDateFormat dateFormat;
    
    /**
     * Callback interface for prediction results
     */
    public interface PredictionCallback {
        void onSuccess(List<EnergyPrediction> predictions);
        void onError(Exception error);
    }
    
    public ChronosApiService() {
        this.gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Predict energy levels for given target times using Chronos API
     * 
     * @param biometrics List of biometric data (sleep, heart rate, etc.)
     * @param typingData List of typing speed measurements
     * @param reactionData List of reaction time measurements
     * @param historicalPredictions List of previous energy predictions
     * @param targetTimes List of target times for prediction
     * @param callback Callback for results
     */
    public void predictEnergy(
            List<BiometricData> biometrics,
            List<TypingSpeedData> typingData,
            List<ReactionTimeData> reactionData,
            List<EnergyPrediction> historicalPredictions,
            List<Date> targetTimes,
            PredictionCallback callback) {
        
        new Thread(() -> {
            try {
                Log.d(TAG, "Sending prediction request to Chronos API");
                Log.d(TAG, "Biometrics: " + (biometrics != null ? biometrics.size() : 0) +
                          ", Typing: " + (typingData != null ? typingData.size() : 0) +
                          ", Reaction: " + (reactionData != null ? reactionData.size() : 0) +
                          ", Historical: " + (historicalPredictions != null ? historicalPredictions.size() : 0) +
                          ", Target times: " + (targetTimes != null ? targetTimes.size() : 0));
                
                // Build request body
                JsonObject requestBody = buildRequestBody(
                    biometrics,
                    typingData,
                    reactionData,
                    historicalPredictions,
                    targetTimes
                );
                
                String jsonBody = gson.toJson(requestBody);
                Log.d(TAG, "Request body: " + jsonBody);
                
                // Create HTTP request
                RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                    .url(CHRONOS_API_BASE_URL + PREDICT_ENDPOINT)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
                
                // Execute request
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        Log.e(TAG, "Chronos API error: " + response.code() + " - " + errorBody);
                        throw new IOException("Chronos API error: " + response.code() + " - " + errorBody);
                    }
                    
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    Log.d(TAG, "Chronos API response: " + responseBody);
                    
                    // Parse response
                    List<EnergyPrediction> predictions = parseResponse(responseBody, targetTimes);
                    
                    Log.d(TAG, "Successfully parsed " + predictions.size() + " predictions from Chronos API");
                    callback.onSuccess(predictions);
                    
                } catch (IOException e) {
                    Log.e(TAG, "Network error calling Chronos API", e);
                    callback.onError(e);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in predictEnergy", e);
                callback.onError(e);
            }
        }).start();
    }
    
    /**
     * Build request body for Chronos API
     * Format: { "past_values": { "heart_rate": [...], ... }, "timestamps": [...], "forecast_horizon": N }
     */
    private JsonObject buildRequestBody(
            List<BiometricData> biometrics,
            List<TypingSpeedData> typingData,
            List<ReactionTimeData> reactionData,
            List<EnergyPrediction> historicalPredictions,
            List<Date> targetTimes) {
        
        JsonObject request = new JsonObject();
        
        // Collect all unique timestamps and create maps for quick lookup
        Set<Date> allTimestamps = new TreeSet<>();
        Map<Date, BiometricData> biometricMap = new HashMap<>();
        Map<Date, TypingSpeedData> typingMap = new HashMap<>();
        Map<Date, ReactionTimeData> reactionMap = new HashMap<>();
        
        // Collect timestamps from biometrics
        if (biometrics != null) {
            for (BiometricData data : biometrics) {
                if (data != null && data.getTimestamp() != null) {
                    allTimestamps.add(data.getTimestamp());
                    biometricMap.put(data.getTimestamp(), data);
                }
            }
        }
        
        // Collect timestamps from typing data
        if (typingData != null) {
            for (TypingSpeedData data : typingData) {
                if (data != null && data.getTimestamp() != null) {
                    allTimestamps.add(data.getTimestamp());
                    typingMap.put(data.getTimestamp(), data);
                }
            }
        }
        
        // Collect timestamps from reaction data
        if (reactionData != null) {
            for (ReactionTimeData data : reactionData) {
                if (data != null && data.getTimestamp() != null) {
                    allTimestamps.add(data.getTimestamp());
                    reactionMap.put(data.getTimestamp(), data);
                }
            }
        }
        
        // If no data, return empty request with forecast_horizon
        if (allTimestamps.isEmpty()) {
            request.addProperty("forecast_horizon", targetTimes != null ? targetTimes.size() : 12);
            request.add("past_values", new JsonObject());
            request.add("timestamps", new JsonArray());
            return request;
        }
        
        // Convert sorted timestamps to list
        List<Date> sortedTimestamps = new ArrayList<>(allTimestamps);
        
        // Build arrays aligned by timestamp
        JsonArray heartRateArray = new JsonArray();
        JsonArray sleepHoursArray = new JsonArray();
        JsonArray typingSpeedArray = new JsonArray();
        JsonArray reactionTimeArray = new JsonArray();
        JsonArray timestampsArray = new JsonArray();
        
        for (Date timestamp : sortedTimestamps) {
            // Add timestamp
            timestampsArray.add(dateFormat.format(timestamp));
            
            // Extract heart rate
            BiometricData bio = biometricMap.get(timestamp);
            if (bio != null && bio.getHeartRate() != null) {
                heartRateArray.add(bio.getHeartRate());
            } else {
                heartRateArray.add(JsonNull.INSTANCE); // null value
            }
            
            // Extract sleep hours
            if (bio != null && bio.getSleepMinutes() != null) {
                double sleepHours = bio.getSleepMinutes() / 60.0;
                sleepHoursArray.add(sleepHours);
            } else {
                sleepHoursArray.add(JsonNull.INSTANCE);
            }
            
            // Extract typing speed
            TypingSpeedData typing = typingMap.get(timestamp);
            if (typing != null) {
                typingSpeedArray.add(typing.getWordsPerMinute());
            } else {
                typingSpeedArray.add(JsonNull.INSTANCE);
            }
            
            // Extract reaction time
            ReactionTimeData reaction = reactionMap.get(timestamp);
            if (reaction != null) {
                reactionTimeArray.add(reaction.getReactionTimeMs());
            } else {
                reactionTimeArray.add(JsonNull.INSTANCE);
            }
        }
        
        // Build past_values object
        JsonObject pastValues = new JsonObject();
        if (heartRateArray.size() > 0) {
            pastValues.add("heart_rate", heartRateArray);
        }
        if (sleepHoursArray.size() > 0) {
            pastValues.add("sleep_hours", sleepHoursArray);
        }
        if (typingSpeedArray.size() > 0) {
            pastValues.add("typing_speed", typingSpeedArray);
        }
        if (reactionTimeArray.size() > 0) {
            pastValues.add("reaction_time_ms", reactionTimeArray);
        }
        // Note: HRV and steps are not available in our data models, so we omit them
        
        // Build final request
        request.add("past_values", pastValues);
        request.add("timestamps", timestampsArray);
        request.addProperty("forecast_horizon", targetTimes != null ? targetTimes.size() : 12);
        
        return request;
    }
    
    /**
     * Parse Chronos API response into EnergyPrediction objects
     */
    private List<EnergyPrediction> parseResponse(String responseBody, List<Date> targetTimes) {
        List<EnergyPrediction> predictions = new ArrayList<>();
        
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            
            // Check if response has predictions array
            if (response.has("predictions") && response.get("predictions").isJsonArray()) {
                JsonArray predictionsArray = response.getAsJsonArray("predictions");
                
                for (int i = 0; i < predictionsArray.size() && i < targetTimes.size(); i++) {
                    JsonObject predObj = predictionsArray.get(i).getAsJsonObject();
                    
                    Date timestamp = targetTimes.get(i);
                    String energyLevelStr = predObj.has("energy_level") 
                        ? predObj.get("energy_level").getAsString() 
                        : "MEDIUM";
                    double confidence = predObj.has("confidence") 
                        ? predObj.get("confidence").getAsDouble() 
                        : 0.5;
                    
                    // Parse energy level
                    EnergyLevel energyLevel;
                    try {
                        energyLevel = EnergyLevel.valueOf(energyLevelStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Unknown energy level: " + energyLevelStr + ", defaulting to MEDIUM");
                        energyLevel = EnergyLevel.MEDIUM;
                    }
                    
                    // Create prediction
                    EnergyPrediction prediction = new EnergyPrediction(
                        timestamp,
                        energyLevel,
                        confidence,
                        null, // biometricFactors
                        null  // cognitiveFactors
                    );
                    
                    predictions.add(prediction);
                }
            } else {
                // Fallback: if response format is different, create default predictions
                Log.w(TAG, "Unexpected response format, creating default predictions");
                for (Date targetTime : targetTimes) {
                    predictions.add(new EnergyPrediction(
                        targetTime,
                        EnergyLevel.MEDIUM,
                        0.5,
                        null,
                        null
                    ));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Chronos API response", e);
            // Return empty list - will trigger fallback
            throw new RuntimeException("Failed to parse Chronos API response", e);
        }
        
        return predictions;
    }
}

