package com.flowstate.utils;

import android.content.Context;
import android.util.Log;

import com.flowstate.data.remote.RemoteModelClient;
import com.flowstate.data.remote.RemoteMultivariatePredictRequest;
import com.flowstate.data.remote.RemotePredictResponse;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class to send custom data to the backend API
 * Use this from any Activity or Service to send predictions
 */
public class BackendDataSender {
    
    private static final String TAG = "BackendDataSender";
    private final RemoteModelClient client;
    
    public BackendDataSender(Context context) {
        this.client = RemoteModelClient.getInstance(context);
    }
    
    /**
     * Send custom multivariate data to backend
     * 
     * @param features Map of feature names to their values (e.g., "heart_rate" -> [70, 72, 68])
     * @param forecastHours How many hours to forecast (default 12)
     * @param callback Callback for results
     */
    public void sendCustomData(Map<String, List<Double>> features, 
                               int forecastHours,
                               PredictionCallback callback) {
        
        // Generate timestamps for the data points
        List<String> timestamps = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        // Get the length from first feature
        int dataLength = 0;
        if (!features.isEmpty()) {
            dataLength = features.values().iterator().next().size();
        }
        
        // Generate timestamps (hourly intervals going back from now)
        long now = System.currentTimeMillis();
        for (int i = dataLength - 1; i >= 0; i--) {
            long timestamp = now - (i * 60 * 60 * 1000L); // i hours ago
            timestamps.add(dateFormat.format(new Date(timestamp)));
        }
        
        RemoteMultivariatePredictRequest request = new RemoteMultivariatePredictRequest(
            features,
            timestamps,
            forecastHours
        );
        
        String authHeader = client.getAuthorizationHeader();
        client.getApi().predictMultivariate(request, authHeader)
            .enqueue(new Callback<RemotePredictResponse>() {
                @Override
                public void onResponse(Call<RemotePredictResponse> call, Response<RemotePredictResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Prediction successful: " + response.body().forecast);
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "Prediction failed: " + response.code());
                        callback.onError(new Exception("HTTP " + response.code() + ": " + response.message()));
                    }
                }

                @Override
                public void onFailure(Call<RemotePredictResponse> call, Throwable t) {
                    Log.e(TAG, "Network error", t);
                    callback.onError(new Exception(t));
                }
            });
    }
    
    /**
     * Quick send with just energy/heart rate values
     */
    public void sendSimpleData(List<Double> values, PredictionCallback callback) {
        Map<String, List<Double>> features = new HashMap<>();
        features.put("energy_level", values);
        sendCustomData(features, 12, callback);
    }
    
    /**
     * Send biometric snapshot (current values)
     */
    public void sendBiometricSnapshot(double heartRate, 
                                     double sleepHours, 
                                     int steps,
                                     double typingSpeed,
                                     double reactionTimeMs,
                                     PredictionCallback callback) {
        
        Map<String, List<Double>> features = new HashMap<>();
        
        // Create single-point arrays for current snapshot
        features.put("heart_rate", Arrays.asList(heartRate));
        features.put("sleep_hours", Arrays.asList(sleepHours));
        features.put("steps", Arrays.asList((double) steps));
        features.put("typing_speed", Arrays.asList(typingSpeed));
        features.put("reaction_time_ms", Arrays.asList(reactionTimeMs));
        
        sendCustomData(features, 12, callback);
    }
    
    public interface PredictionCallback {
        void onSuccess(RemotePredictResponse response);
        void onError(Exception error);
    }
}
