package com.personaleenergy.app.api;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to fetch daily advice from Advice Slip JSON API
 * API: https://api.adviceslip.com
 */
public class AdviceSlipService {
    
    private static final String TAG = "AdviceSlipService";
    private static final String API_BASE_URL = "https://api.adviceslip.com";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public interface AdviceCallback {
        void onSuccess(String advice);
        void onError(Exception error);
    }
    
    public AdviceSlipService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().create();
    }
    
    /**
     * Fetch a random piece of advice
     */
    public void getRandomAdvice(AdviceCallback callback) {
        String url = API_BASE_URL + "/advice";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Failed to fetch advice", e);
                callback.onError(e);
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Advice API error: " + response.code() + " - " + errorBody);
                    callback.onError(new Exception("Advice API error: " + response.code()));
                    return;
                }
                
                String responseBody = response.body().string();
                Log.d(TAG, "Advice API response: " + responseBody);
                
                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonObject slip = jsonResponse.getAsJsonObject("slip");
                    
                    if (slip != null && slip.has("advice")) {
                        String advice = slip.get("advice").getAsString();
                        Log.d(TAG, "Successfully fetched advice: " + advice);
                        callback.onSuccess(advice);
                    } else {
                        Log.e(TAG, "Invalid response format - no 'slip.advice' field");
                        callback.onError(new Exception("Invalid response format"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing advice response", e);
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Fetch advice by ID
     */
    public void getAdviceById(int slipId, AdviceCallback callback) {
        String url = API_BASE_URL + "/advice/" + slipId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Failed to fetch advice by ID", e);
                callback.onError(e);
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Advice API error: " + response.code() + " - " + errorBody);
                    callback.onError(new Exception("Advice API error: " + response.code()));
                    return;
                }
                
                String responseBody = response.body().string();
                
                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonObject slip = jsonResponse.getAsJsonObject("slip");
                    
                    if (slip != null && slip.has("advice")) {
                        String advice = slip.get("advice").getAsString();
                        callback.onSuccess(advice);
                    } else {
                        callback.onError(new Exception("Invalid response format"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing advice response", e);
                    callback.onError(e);
                }
            }
        });
    }
}

