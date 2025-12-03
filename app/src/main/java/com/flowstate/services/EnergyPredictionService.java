package com.flowstate.services;

import android.content.Context;
import android.util.Log;
import com.flowstate.app.data.models.PredictionRequest;
import com.flowstate.app.data.models.PredictionResponse;
import com.flowstate.app.data.models.EnergyPredictionResult;
import com.flowstate.core.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for predicting energy levels using Gemini API (primary) or remote model API (fallback)
 * Uses Gemini with function calling for intelligent energy prediction based on health data
 */
public class EnergyPredictionService {
    
    private static final String TAG = "EnergyPredictionService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiUrl;
    private final String apiKey;
    private final HealthDataAggregator dataAggregator;
    private final GeminiEnergyPredictor geminiPredictor;
    
    public EnergyPredictionService(Context context) {
        this.apiUrl = Config.REMOTE_MODEL_URL;
        this.apiKey = Config.REMOTE_MODEL_API_KEY;
        this.dataAggregator = new HealthDataAggregator(context);
        
        // Initialize Gemini predictor (primary method)
        this.geminiPredictor = new GeminiEnergyPredictor(context);
        
        // Setup OkHttp client with timeout (for fallback remote API)
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS) // Longer timeout for ML inference
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Setup Gson for JSON serialization
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
    }
    
    /**
     * Predicts energy levels using health data from local database
     * Uses heart rate as the primary time series for HuggingFace API
     * 
     * @param forecastHorizon Number of future periods to predict (default: 12)
     * @return CompletableFuture with list of predicted energy levels (0-100 scale)
     */
    public CompletableFuture<List<Double>> predictEnergyLevels(int forecastHorizon) {
        CompletableFuture<List<Double>> future = new CompletableFuture<>();
        
        // Collect health data
        Map<String, List<Double>> healthData = dataAggregator.collectHealthData();
        
        if (healthData.isEmpty()) {
            Log.w(TAG, "No health data available for prediction");
            future.completeExceptionally(new IllegalStateException("No health data available"));
            return future;
        }
        
        // Get primary time series (heart rate preferred, or first available)
        List<Double> primarySeries = dataAggregator.getPrimaryTimeSeries(healthData);
        
        if (primarySeries == null || primarySeries.isEmpty()) {
            Log.w(TAG, "No primary time series available for prediction");
            future.completeExceptionally(new IllegalStateException("No primary time series available"));
            return future;
        }
        
        // Create request with simple history format for HuggingFace
        PredictionRequest request = new PredictionRequest(primarySeries, forecastHorizon);
        
        // Make API call
        predictEnergyLevels(request, new PredictionCallback() {
            @Override
            public void onSuccess(List<Double> forecast) {
                future.complete(forecast);
            }
            
            @Override
            public void onError(String error) {
                future.completeExceptionally(new IOException(error));
            }
        });
        
        return future;
    }
    
    /**
     * Predicts energy levels using provided health data
     * Uses heart rate as primary series, or first available series
     * 
     * @param healthData Map of feature names to time series values
     * @param forecastHorizon Number of future periods to predict
     * @return CompletableFuture with list of predicted energy levels
     */
    public CompletableFuture<List<Double>> predictEnergyLevels(
            Map<String, List<Double>> healthData, 
            int forecastHorizon) {
        CompletableFuture<List<Double>> future = new CompletableFuture<>();
        
        if (healthData == null || healthData.isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Health data cannot be empty"));
            return future;
        }
        
        // Get primary time series (heart rate preferred)
        List<Double> primarySeries = dataAggregator.getPrimaryTimeSeries(healthData);
        
        if (primarySeries == null || primarySeries.isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("No valid time series in health data"));
            return future;
        }
        
        PredictionRequest request = new PredictionRequest(primarySeries, forecastHorizon);
        
        predictEnergyLevels(request, new PredictionCallback() {
            @Override
            public void onSuccess(List<Double> forecast) {
                future.complete(forecast);
            }
            
            @Override
            public void onError(String error) {
                future.completeExceptionally(new IOException(error));
            }
        });
        
        return future;
    }
    
    /**
     * Makes the actual API call to the HuggingFace prediction endpoint
     * Sends simple {"history": [...]} format as expected by HuggingFace API
     */
    private void predictEnergyLevels(PredictionRequest request, PredictionCallback callback) {
        try {
            // HuggingFace endpoint expects simple format: {"history": [values]}
            // Extract primary time series from request
            List<Double> history;
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                history = request.getHistory();
            } else if (request.getPastValues() != null && !request.getPastValues().isEmpty()) {
                // Use heart_rate as primary series, or first available
                if (request.getPastValues().containsKey("heart_rate")) {
                    history = request.getPastValues().get("heart_rate");
                } else {
                    history = request.getPastValues().values().iterator().next();
                }
            } else {
                callback.onError("No history data available in request");
                return;
            }
            
            // Create simple HuggingFace format
            Map<String, Object> huggingfaceRequest = new HashMap<>();
            huggingfaceRequest.put("history", history);
            
            String jsonBody = gson.toJson(huggingfaceRequest);
            
            Log.d(TAG, "Sending prediction request to HuggingFace: " + apiUrl);
            Log.d(TAG, "Request body: " + jsonBody);
            
            // Build request
            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .header("Content-Type", "application/json");
            
            // Add API key if provided
            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }
            
            Request httpRequest = requestBuilder.build();
            
            // Execute request asynchronously
            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API request failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API request failed with status " + response.code() + ": " + errorBody);
                        callback.onError("API error: " + response.code() + " - " + errorBody);
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "API response: " + responseBody);
                        
                        // HuggingFace may return forecast directly as array or wrapped in object
                        List<Double> forecast = null;
                        
                        try {
                            // Try parsing as PredictionResponse first (wrapped format)
                            PredictionResponse predictionResponse = gson.fromJson(responseBody, PredictionResponse.class);
                            if (predictionResponse != null && predictionResponse.isSuccess()) {
                                forecast = predictionResponse.getForecast();
                            }
                        } catch (Exception e) {
                            // If that fails, try parsing as direct array
                            try {
                                forecast = gson.fromJson(responseBody, 
                                    new TypeToken<List<Double>>(){}.getType());
                            } catch (Exception e2) {
                                Log.w(TAG, "Failed to parse as array, trying as object", e2);
                            }
                        }
                        
                        if (forecast != null && !forecast.isEmpty()) {
                            callback.onSuccess(forecast);
                        } else {
                            callback.onError("Invalid response format from HuggingFace API");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing API response", e);
                        callback.onError("Parse error: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating API request", e);
            callback.onError("Request error: " + e.getMessage());
        }
    }
    
    /**
     * Callback interface for prediction results
     */
    private interface PredictionCallback {
        void onSuccess(List<Double> forecast);
        void onError(String error);
    }
    
    /**
     * Gets energy predictions for the next 12 hours with explanation
     * Uses Gemini API (primary)
     * 
     * @return CompletableFuture with EnergyPredictionResult containing 12 hourly predictions and explanation
     */
    public CompletableFuture<EnergyPredictionResult> getEnergyPredictions12Hours() {
        // Try Gemini first (preferred method)
        if (geminiPredictor != null && geminiPredictor.isAvailable()) {
            Log.d(TAG, "Using Gemini API for 12-hour energy prediction");
            return geminiPredictor.predictEnergyLevels12Hours();
        }
        
        // No valid predictor available
        CompletableFuture<EnergyPredictionResult> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Gemini API not available. Check API key settings."));
        return failedFuture;
    }
    
    /**
     * Gets the current energy level prediction (convenience method)
     * Uses Gemini API (primary) or falls back to remote API
     */
    public CompletableFuture<Double> getCurrentEnergyLevel() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        
        getEnergyPredictions12Hours().thenAccept(result -> {
            Double current = result.getCurrentEnergyLevel();
            if (current != null) {
                future.complete(current);
            } else {
                future.completeExceptionally(new IllegalStateException("No current energy level in result"));
            }
        }).exceptionally(error -> {
            future.completeExceptionally(error);
            return null;
        });
        
        return future;
    }
    
    /**
     * Fallback method using remote API - converts to EnergyPredictionResult
     */
    private CompletableFuture<EnergyPredictionResult> getEnergyPredictionsFromRemote() {
        CompletableFuture<EnergyPredictionResult> future = new CompletableFuture<>();
        
        predictEnergyLevels(12).thenAccept(forecast -> {
            if (forecast != null && !forecast.isEmpty()) {
                // Ensure we have 12 values
                while (forecast.size() < 12) {
                    if (forecast.isEmpty()) {
                        forecast.add(50.0); // Default
                    } else {
                        forecast.add(forecast.get(forecast.size() - 1)); // Repeat last
                    }
                }
                if (forecast.size() > 12) {
                    forecast = forecast.subList(0, 12);
                }
                
                String explanation = "Energy prediction based on time series analysis of health data.";
                future.complete(new EnergyPredictionResult(forecast, explanation));
            } else {
                future.completeExceptionally(new IllegalStateException("Empty forecast"));
            }
        }).exceptionally(error -> {
            future.completeExceptionally(error);
            return null;
        });
        
        return future;
    }
    
}

