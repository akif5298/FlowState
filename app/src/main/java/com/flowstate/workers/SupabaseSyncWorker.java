package com.flowstate.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.local.entities.PredictionLocal;
import com.flowstate.data.remote.PostgrestApi;
import com.flowstate.data.remote.SupabaseClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Worker to sync pending local data to Supabase
 * 
 * Gets all pending records from Room DB (where synced = false),
 * uploads them to Supabase using RPC API methods,
 * and marks them as synced after successful upload
 */
public class SupabaseSyncWorker extends Worker {
    
    private static final String TAG = "SupabaseSyncWorker";
    public static final String PERIODIC_WORK_NAME = "supabase_sync_periodic";
    
    private Context context;
    private AppDb db;
    private SupabaseClient supabaseClient;
    private PostgrestApi postgrestApi;
    private SimpleDateFormat dateFormat;
    
    public SupabaseSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context.getApplicationContext();
        this.db = AppDb.getInstance(this.context);
        this.supabaseClient = SupabaseClient.getInstance(this.context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        
        // ISO 8601 format for timestamps
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting Supabase sync");
            
            // Check if user is authenticated
            String accessToken = supabaseClient.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                Log.w(TAG, "No access token available. User not authenticated. Skipping sync.");
                return Result.success(); // Don't retry if not authenticated
            }
            
            // Get user ID
            String userId = getUserId();
            if (userId == null || userId.isEmpty()) {
                Log.w(TAG, "No user ID available. Skipping sync.");
                return Result.success(); // Don't retry if no user ID
            }
            
            int totalSynced = 0;
            
            // Sync heart rate data
            int hrSynced = syncHeartRate(userId);
            totalSynced += hrSynced;
            
            // Sync sleep data
            int sleepSynced = syncSleep(userId);
            totalSynced += sleepSynced;
            
            // Sync typing test data
            int typingSynced = syncTyping(userId);
            totalSynced += typingSynced;
            
            // Sync reaction time test data
            int reactionSynced = syncReaction(userId);
            totalSynced += reactionSynced;
            
            // Sync energy predictions
            int predictionSynced = syncPredictions(userId);
            totalSynced += predictionSynced;
            
            Log.d(TAG, "Supabase sync completed. Synced " + totalSynced + " records total.");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing data to Supabase", e);
            return Result.retry(); // Retry on error
        }
    }
    
    /**
     * Sync pending heart rate readings to Supabase
     */
    private int syncHeartRate(String userId) {
        try {
            List<HrLocal> pending = db.hrDao().pending();
            if (pending.isEmpty()) {
                Log.d(TAG, "No pending heart rate readings to sync");
                return 0;
            }
            
            Log.d(TAG, "Syncing " + pending.size() + " heart rate readings");
            
            // Upload each record to Supabase using PostgREST API
            List<Long> syncedIds = new java.util.ArrayList<>();
            
            for (HrLocal hr : pending) {
                try {
                    // Create data map for this record
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("user_id", userId);
                    data.put("timestamp", dateFormat.format(new Date(hr.timestamp)));
                    data.put("heart_rate_bpm", hr.bpm);
                    data.put("source", "manual_entry");
                    
                    // Upload to Supabase using PostgREST API
                    retrofit2.Response<Void> response = postgrestApi.post("heart_rate_readings", data).execute();
                    
                    if (response.isSuccessful()) {
                        syncedIds.add(hr.id);
                        Log.d(TAG, "Successfully synced heart rate reading with id: " + hr.id);
                    } else {
                        Log.e(TAG, "Failed to sync heart rate reading " + hr.id + ": " + response.code());
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing heart rate reading " + hr.id, e);
                }
            }
            
            // Mark successfully synced records
            if (!syncedIds.isEmpty()) {
                db.hrDao().markSynced(syncedIds);
                Log.d(TAG, "Successfully synced " + syncedIds.size() + " of " + pending.size() + " heart rate readings");
                return syncedIds.size();
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing heart rate readings", e);
            return 0;
        }
    }
    
    /**
     * Sync pending sleep sessions to Supabase
     */
    private int syncSleep(String userId) {
        try {
            List<SleepLocal> pending = db.sleepDao().pending();
            if (pending.isEmpty()) {
                Log.d(TAG, "No pending sleep sessions to sync");
                return 0;
            }
            
            Log.d(TAG, "Syncing " + pending.size() + " sleep sessions");
            
            // Upload each record to Supabase using PostgREST API
            List<Long> syncedIds = new java.util.ArrayList<>();
            
            for (SleepLocal sleep : pending) {
                try {
                    // Create data map for this record
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("user_id", userId);
                    data.put("sleep_start", dateFormat.format(new Date(sleep.sleep_start)));
                    
                    if (sleep.sleep_end != null) {
                        data.put("sleep_end", dateFormat.format(new Date(sleep.sleep_end)));
                    }
                    
                    if (sleep.duration != null) {
                        data.put("duration_minutes", sleep.duration);
                    }
                    
                    data.put("source", "manual_entry");
                    
                    // Upload to Supabase using PostgREST API
                    retrofit2.Response<Void> response = postgrestApi.post("sleep_sessions", data).execute();
                    
                    if (response.isSuccessful()) {
                        syncedIds.add(sleep.id);
                        Log.d(TAG, "Successfully synced sleep session with id: " + sleep.id);
                    } else {
                        Log.e(TAG, "Failed to sync sleep session " + sleep.id + ": " + response.code());
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing sleep session " + sleep.id, e);
                }
            }
            
            // Mark successfully synced records
            if (!syncedIds.isEmpty()) {
                db.sleepDao().markSynced(syncedIds);
                Log.d(TAG, "Successfully synced " + syncedIds.size() + " of " + pending.size() + " sleep sessions");
                return syncedIds.size();
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing sleep sessions", e);
            return 0;
        }
    }
    
    /**
     * Sync pending typing test data to Supabase
     */
    private int syncTyping(String userId) {
        try {
            List<TypingLocal> pending = db.typingDao().pending();
            if (pending.isEmpty()) {
                Log.d(TAG, "No pending typing tests to sync");
                return 0;
            }
            
            Log.d(TAG, "Syncing " + pending.size() + " typing tests");
            
            // Upload each record to Supabase using PostgREST API
            List<Long> syncedIds = new java.util.ArrayList<>();
            
            for (TypingLocal typing : pending) {
                try {
                    // Create data map for this record
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("user_id", userId);
                    data.put("timestamp", dateFormat.format(new Date(typing.timestamp)));
                    data.put("words_per_minute", typing.wpm);
                    data.put("accuracy_percentage", typing.accuracy);
                    
                    if (typing.totalChars > 0) {
                        data.put("total_characters", typing.totalChars);
                    }
                    
                    if (typing.errors > 0) {
                        data.put("errors", typing.errors);
                    }
                    
                    if (typing.sampleText != null && !typing.sampleText.isEmpty()) {
                        data.put("sample_text", typing.sampleText);
                    }
                    
                    if (typing.durationSecs > 0) {
                        data.put("duration_seconds", typing.durationSecs);
                    }
                    
                    // Upload to Supabase using PostgREST API
                    retrofit2.Response<Void> response = postgrestApi.post("typing_speed_tests", data).execute();
                    
                    if (response.isSuccessful()) {
                        syncedIds.add(typing.id);
                        Log.d(TAG, "Successfully synced typing test with id: " + typing.id);
                    } else {
                        Log.e(TAG, "Failed to sync typing test " + typing.id + ": " + response.code());
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing typing test " + typing.id, e);
                }
            }
            
            // Mark successfully synced records
            if (!syncedIds.isEmpty()) {
                db.typingDao().markSynced(syncedIds);
                Log.d(TAG, "Successfully synced " + syncedIds.size() + " of " + pending.size() + " typing tests");
                return syncedIds.size();
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing typing tests", e);
            return 0;
        }
    }
    
    /**
     * Sync pending reaction time test data to Supabase
     */
    private int syncReaction(String userId) {
        try {
            List<ReactionLocal> pending = db.reactionDao().pending();
            if (pending.isEmpty()) {
                Log.d(TAG, "No pending reaction tests to sync");
                return 0;
            }
            
            Log.d(TAG, "Syncing " + pending.size() + " reaction tests");
            
            // Upload each record to Supabase using PostgREST API
            List<Long> syncedIds = new java.util.ArrayList<>();
            
            for (ReactionLocal reaction : pending) {
                try {
                    // Create data map for this record
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("user_id", userId);
                    data.put("timestamp", dateFormat.format(new Date(reaction.timestamp)));
                    data.put("reaction_time_ms", reaction.medianMs);
                    data.put("test_type", "visual");
                    data.put("attempts", reaction.testCount);
                    
                    // Calculate average (same as median for single value)
                    data.put("average_reaction_time_ms", (double) reaction.medianMs);
                    
                    // Upload to Supabase using PostgREST API
                    retrofit2.Response<Void> response = postgrestApi.post("reaction_time_tests", data).execute();
                    
                    if (response.isSuccessful()) {
                        syncedIds.add(reaction.id);
                        Log.d(TAG, "Successfully synced reaction test with id: " + reaction.id);
                    } else {
                        Log.e(TAG, "Failed to sync reaction test " + reaction.id + ": " + response.code());
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing reaction test " + reaction.id, e);
                }
            }
            
            // Mark successfully synced records
            if (!syncedIds.isEmpty()) {
                db.reactionDao().markSynced(syncedIds);
                Log.d(TAG, "Successfully synced " + syncedIds.size() + " of " + pending.size() + " reaction tests");
                return syncedIds.size();
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing reaction tests", e);
            return 0;
        }
    }
    
    /**
     * Sync pending energy predictions to Supabase
     */
    private int syncPredictions(String userId) {
        try {
            List<PredictionLocal> pending = db.predictionDao().pending();
            if (pending.isEmpty()) {
                Log.d(TAG, "No pending predictions to sync");
                return 0;
            }
            
            Log.d(TAG, "Syncing " + pending.size() + " predictions");
            
            // Upload each record to Supabase using PostgREST API
            List<Long> syncedIds = new java.util.ArrayList<>();
            
            for (PredictionLocal prediction : pending) {
                try {
                    // Create data map for this record
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("user_id", userId);
                    data.put("prediction_time", dateFormat.format(new Date(prediction.predictionTime)));
                    data.put("predicted_level", prediction.level);
                    data.put("confidence_score", prediction.confidence);
                    data.put("ml_model_version", "v1.0");
                    
                    // Upload to Supabase using PostgREST API
                    retrofit2.Response<Void> response = postgrestApi.post("energy_predictions", data).execute();
                    
                    if (response.isSuccessful()) {
                        syncedIds.add(prediction.id);
                        Log.d(TAG, "Successfully synced prediction with id: " + prediction.id);
                    } else {
                        Log.e(TAG, "Failed to sync prediction " + prediction.id + ": " + response.code());
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing prediction " + prediction.id, e);
                }
            }
            
            // Mark successfully synced records
            if (!syncedIds.isEmpty()) {
                db.predictionDao().markSynced(syncedIds);
                Log.d(TAG, "Successfully synced " + syncedIds.size() + " of " + pending.size() + " predictions");
                return syncedIds.size();
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing predictions", e);
            return 0;
        }
    }
    
    /**
     * Get user ID from Supabase client
     */
    private String getUserId() {
        try {
            // Try to get from the old SupabaseClient (compatibility)
            com.flowstate.app.supabase.SupabaseClient oldClient = 
                com.flowstate.app.supabase.SupabaseClient.getInstance(context);
            return oldClient.getUserId();
        } catch (Exception e) {
            // Fallback: try to get from SharedPreferences
            android.content.SharedPreferences prefs = context.getSharedPreferences(
                "flowstate_supabase", Context.MODE_PRIVATE);
            return prefs.getString("user_id", null);
        }
    }
    
    /**
     * Create a one-time work request for immediate sync
     */
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(SupabaseSyncWorker.class)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Create a periodic work request for hourly sync
     * Note: Minimum interval is 15 minutes due to WorkManager constraints
     */
    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(
                SupabaseSyncWorker.class,
                1, TimeUnit.HOURS // Will be adjusted to minimum 15 minutes by WorkManager
        )
        .setInitialDelay(15, TimeUnit.MINUTES) // Start after 15 minutes
        .build();
    }
}

