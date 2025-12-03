package com.flowstate.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.StepsLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.remote.RemoteModelClient;
import com.flowstate.data.remote.RemoteMultivariatePredictRequest;
import com.flowstate.data.remote.RemotePredictResponse;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * Worker to sync local data to remote backend for ML predictions
 * Sends aggregated biometric and cognitive data to the prediction service
 */
public class RemoteSyncWorker extends Worker {

    private static final String TAG = "RemoteSyncWorker";
    public static final String PERIODIC_WORK_NAME = "remote_sync_periodic";
    
    private final Context context;
    private final AppDb db;
    private final RemoteModelClient remoteClient;

    public RemoteSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context.getApplicationContext();
        this.db = AppDb.getInstance(this.context);
        this.remoteClient = RemoteModelClient.getInstance(this.context);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        try {
            Log.d(TAG, "Starting remote sync to backend...");
            
            // Fetch last 48 hours of data for context
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (48 * 60 * 60 * 1000L); // 48 hours ago
            
            // Get all data from Room database
            List<HrLocal> hrData = db.hrDao().getByDateRange(startTime, endTime);
            List<SleepLocal> sleepData = db.sleepDao().getByDateRange(startTime, endTime);
            List<StepsLocal> stepsData = db.stepsDao().getByDateRange(startTime, endTime);
            List<TypingLocal> typingData = db.typingDao().getByDateRange(startTime, endTime);
            List<ReactionLocal> reactionData = db.reactionDao().getByDateRange(startTime, endTime);
            
            Log.d(TAG, String.format("Fetched data - HR: %d, Sleep: %d, Steps: %d, Typing: %d, Reaction: %d",
                hrData.size(), sleepData.size(), stepsData.size(), typingData.size(), reactionData.size()));
            
            if (hrData.isEmpty() && sleepData.isEmpty() && stepsData.isEmpty() && 
                typingData.isEmpty() && reactionData.isEmpty()) {
                Log.d(TAG, "No data to sync");
                return ListenableWorker.Result.success();
            }
            
            // Prepare multivariate data for backend
            Map<String, List<Double>> pastValues = new HashMap<>();
            List<String> timestamps = new ArrayList<>();
            
            // Build time-aligned data structure
            Map<Long, Map<String, Double>> timeSeriesMap = new TreeMap<>();
            
            // Add heart rate data
            for (HrLocal hr : hrData) {
                timeSeriesMap.putIfAbsent(hr.timestamp, new HashMap<>());
                timeSeriesMap.get(hr.timestamp).put("heart_rate", (double) hr.bpm);
            }
            
            // Add sleep data (convert to hours)
            for (SleepLocal sleep : sleepData) {
                if (sleep.duration != null) {
                    timeSeriesMap.putIfAbsent(sleep.sleep_start, new HashMap<>());
                    timeSeriesMap.get(sleep.sleep_start).put("sleep_hours", sleep.duration / 60.0);
                }
            }
            
            // Add steps data
            for (StepsLocal steps : stepsData) {
                timeSeriesMap.putIfAbsent(steps.timestamp, new HashMap<>());
                timeSeriesMap.get(steps.timestamp).put("steps", (double) steps.steps);
            }
            
            // Add typing speed data
            for (TypingLocal typing : typingData) {
                timeSeriesMap.putIfAbsent(typing.timestamp, new HashMap<>());
                timeSeriesMap.get(typing.timestamp).put("typing_speed", (double) typing.wpm);
            }
            
            // Add reaction time data
            for (ReactionLocal reaction : reactionData) {
                timeSeriesMap.putIfAbsent(reaction.timestamp, new HashMap<>());
                timeSeriesMap.get(reaction.timestamp).put("reaction_time_ms", (double) reaction.medianMs);
            }
            
            // Convert to format expected by backend
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            // Initialize feature lists
            Set<String> features = new HashSet<>();
            for (Map<String, Double> values : timeSeriesMap.values()) {
                features.addAll(values.keySet());
            }
            
            for (String feature : features) {
                pastValues.put(feature, new ArrayList<>());
            }
            
            // Fill in data (use 0 or interpolated values for missing data)
            for (Map.Entry<Long, Map<String, Double>> entry : timeSeriesMap.entrySet()) {
                timestamps.add(dateFormat.format(new Date(entry.getKey())));
                
                for (String feature : features) {
                    Double value = entry.getValue().get(feature);
                    if (value != null) {
                        pastValues.get(feature).add(value);
                    } else {
                        // Use default values for missing data
                        double defaultValue = getDefaultValue(feature);
                        pastValues.get(feature).add(defaultValue);
                    }
                }
            }
            
            // Ensure we have at least some data points
            if (timestamps.isEmpty()) {
                Log.d(TAG, "No time series data to send");
                return ListenableWorker.Result.success();
            }
            
            Log.d(TAG, String.format("Prepared %d timestamps with features: %s", 
                timestamps.size(), String.join(", ", features)));
            
            // Send to backend
            RemoteMultivariatePredictRequest request = new RemoteMultivariatePredictRequest(
                pastValues,
                timestamps,
                12 // Forecast 12 hours ahead
            );
            
            String authHeader = remoteClient.getAuthorizationHeader();
            Response<RemotePredictResponse> response = remoteClient.getApi()
                .predictMultivariate(request, authHeader)
                .execute();
            
            if (response.isSuccessful() && response.body() != null) {
                RemotePredictResponse result = response.body();
                Log.d(TAG, "Successfully synced data to backend. Received forecast: " + 
                    (result.forecast != null ? result.forecast.size() + " values" : "none"));
                
                // Mark data as synced (optional - depends on if you want to resend)
                // For now, we'll keep sending historical data each time
                
                return ListenableWorker.Result.success();
            } else {
                Log.e(TAG, "Backend sync failed: " + response.code() + " " + response.message());
                return ListenableWorker.Result.retry();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during remote sync", e);
            return ListenableWorker.Result.retry();
        }
    }
    
    /**
     * Get default value for a feature when data is missing
     */
    private double getDefaultValue(String feature) {
        switch (feature) {
            case "heart_rate":
                return 70.0; // Average resting heart rate
            case "sleep_hours":
                return 7.0; // Average sleep duration
            case "steps":
                return 0.0; // No steps if not recorded
            case "typing_speed":
                return 50.0; // Average typing speed
            case "reaction_time_ms":
                return 250.0; // Average reaction time
            default:
                return 0.0;
        }
    }
    
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(RemoteSyncWorker.class).build();
    }

    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        // Sync every 2 hours
        return new PeriodicWorkRequest.Builder(RemoteSyncWorker.class, 2, TimeUnit.HOURS).build();
    }
}
