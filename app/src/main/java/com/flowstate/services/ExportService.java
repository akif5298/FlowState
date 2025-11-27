package com.flowstate.services;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.local.entities.PredictionLocal;
import com.flowstate.data.remote.PostgrestApi;
import com.flowstate.data.remote.SupabaseClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Call;
import retrofit2.Response;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for exporting user data to JSON file
 * 
 * Exports all user data from Supabase (via PostgREST) to a JSON file in Downloads folder
 */
public class ExportService {
    
    private static final String TAG = "ExportService";
    private static final String EXPORT_FILENAME_PREFIX = "flowstate_export_";
    private static final String EXPORT_FILENAME_SUFFIX = ".json";
    
    private Context context;
    private SupabaseClient supabaseClient;
    private PostgrestApi postgrestApi;
    private AppDb db;
    private Gson gson;
    private ExecutorService executor;
    
    // List of tables to export (all tables with user_id field)
    // Note: profiles uses 'id' not 'user_id', handled separately
    private static final String[] TABLES = {
        "user_settings",
        "heart_rate_readings",
        "sleep_sessions",
        "temperature_readings",
        "typing_speed_tests",
        "reaction_time_tests",
        "cognitive_test_sessions",
        "energy_predictions",
        "energy_prediction_factors",
        "productivity_suggestions",
        "ai_schedules",
        "scheduled_tasks",
        "weekly_insights",
        "daily_summaries"
    };
    
    public ExportService(Context context) {
        this.context = context.getApplicationContext();
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.db = AppDb.getInstance(context);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Export all user data to JSON file
     * 
     * @param ctx Application context
     * @return File object pointing to the exported JSON file, or null if export failed
     */
    public File exportAllToJson(Context ctx) {
        String userId = getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "No user ID available for export");
            return null;
        }
        
        Log.d(TAG, "Starting data export for user: " + userId);
        
        Map<String, Object> exportData = new LinkedHashMap<>();
        exportData.put("export_date", new Date().toString());
        exportData.put("user_id", userId);
        exportData.put("export_version", "1.0");
        
        Map<String, List<Map<String, Object>>> remoteData = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> localData = new LinkedHashMap<>();
        
        // Export remote data from Supabase
        try {
            for (String table : TABLES) {
                List<Map<String, Object>> tableData = fetchTableData(table, userId);
                if (tableData != null && !tableData.isEmpty()) {
                    remoteData.put(table, tableData);
                    Log.d(TAG, "Exported " + tableData.size() + " rows from " + table);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting remote data", e);
        }
        
        // Export profiles separately (uses id, not user_id)
        try {
            Map<String, String> profileParams = new HashMap<>();
            profileParams.put("id", "eq." + userId);
            Call<List<Map<String, Object>>> call = postgrestApi.get("profiles", profileParams);
            Response<List<Map<String, Object>>> response = call.execute();
            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                remoteData.put("profiles", response.body());
                Log.d(TAG, "Exported " + response.body().size() + " rows from profiles");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting profiles", e);
        }
        
        // Export local data from Room
        try {
            exportLocalData(localData);
        } catch (Exception e) {
            Log.e(TAG, "Error exporting local data", e);
        }
        
        exportData.put("remote_data", remoteData);
        exportData.put("local_data", localData);
        
        // Write to JSON file
        return writeToFile(ctx, exportData);
    }
    
    /**
     * Fetch data from a specific table for the user
     * Uses user_id filter for all tables except profiles
     */
    private List<Map<String, Object>> fetchTableData(String table, String userId) {
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("user_id", "eq." + userId);
            
            Call<List<Map<String, Object>>> call = postgrestApi.get(table, queryParams);
            Response<List<Map<String, Object>>> response = call.execute();
            
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            } else {
                Log.w(TAG, "Failed to fetch " + table + ": " + response.code());
                try {
                    if (response.errorBody() != null) {
                        String errorBody = response.errorBody().string();
                        Log.w(TAG, "Error body: " + errorBody);
                    }
                } catch (Exception e) {
                    // Ignore
                }
                return new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching " + table, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Export local Room data
     */
    private void exportLocalData(Map<String, List<Map<String, Object>>> localData) {
        // Export heart rate data
        List<HrLocal> hrList = db.hrDao().pending();
        List<Map<String, Object>> hrData = new ArrayList<>();
        for (HrLocal hr : hrList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hr.id);
            map.put("timestamp", hr.timestamp);
            map.put("bpm", hr.bpm);
            map.put("synced", hr.synced);
            hrData.add(map);
        }
        if (!hrData.isEmpty()) {
            localData.put("hr_local", hrData);
        }
        
        // Export sleep data
        List<SleepLocal> sleepList = db.sleepDao().pending();
        List<Map<String, Object>> sleepData = new ArrayList<>();
        for (SleepLocal sleep : sleepList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sleep.id);
            map.put("sleep_start", sleep.sleep_start);
            map.put("sleep_end", sleep.sleep_end);
            map.put("duration", sleep.duration);
            map.put("synced", sleep.synced);
            sleepData.add(map);
        }
        if (!sleepData.isEmpty()) {
            localData.put("sleep_local", sleepData);
        }
        
        // Export typing data
        List<TypingLocal> typingList = db.typingDao().pending();
        List<Map<String, Object>> typingData = new ArrayList<>();
        for (TypingLocal typing : typingList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", typing.id);
            map.put("timestamp", typing.timestamp);
            map.put("wpm", typing.wpm);
            map.put("accuracy", typing.accuracy);
            map.put("totalChars", typing.totalChars);
            map.put("errors", typing.errors);
            map.put("durationSecs", typing.durationSecs);
            map.put("sampleText", typing.sampleText);
            map.put("synced", typing.synced);
            typingData.add(map);
        }
        if (!typingData.isEmpty()) {
            localData.put("typing_local", typingData);
        }
        
        // Export reaction data
        List<ReactionLocal> reactionList = db.reactionDao().pending();
        List<Map<String, Object>> reactionData = new ArrayList<>();
        for (ReactionLocal reaction : reactionList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", reaction.id);
            map.put("timestamp", reaction.timestamp);
            map.put("medianMs", reaction.medianMs);
            map.put("testCount", reaction.testCount);
            map.put("synced", reaction.synced);
            reactionData.add(map);
        }
        if (!reactionData.isEmpty()) {
            localData.put("reaction_local", reactionData);
        }
        
        // Export prediction data
        List<PredictionLocal> predictionList = db.predictionDao().pending();
        List<Map<String, Object>> predictionData = new ArrayList<>();
        for (PredictionLocal prediction : predictionList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", prediction.id);
            map.put("predictionTime", prediction.predictionTime);
            map.put("level", prediction.level);
            map.put("confidence", prediction.confidence);
            map.put("synced", prediction.synced);
            predictionData.add(map);
        }
        if (!predictionData.isEmpty()) {
            localData.put("prediction_local", predictionData);
        }
    }
    
    /**
     * Write export data to JSON file in Downloads folder
     */
    private File writeToFile(Context ctx, Map<String, Object> exportData) {
        try {
            // Create filename with date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String dateStr = dateFormat.format(new Date());
            String filename = EXPORT_FILENAME_PREFIX + dateStr + EXPORT_FILENAME_SUFFIX;
            
            // Get Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            
            File exportFile = new File(downloadsDir, filename);
            
            // Write JSON to file
            try (FileWriter writer = new FileWriter(exportFile)) {
                gson.toJson(exportData, writer);
                writer.flush();
            }
            
            Log.d(TAG, "Export completed: " + exportFile.getAbsolutePath());
            return exportFile;
            
        } catch (IOException e) {
            Log.e(TAG, "Error writing export file", e);
            return null;
        }
    }
    
    /**
     * Get user ID from Supabase client
     */
    private String getUserId() {
        // Get user ID from the old SupabaseClient (compatibility)
        // In the new architecture, we might need to get it from SecureStore or another source
        try {
            // Try to get from the old SupabaseClient
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
     * Export data asynchronously
     */
    public void exportAllToJsonAsync(Context ctx, ExportCallback callback) {
        executor.execute(() -> {
            File exportFile = exportAllToJson(ctx);
            if (callback != null) {
                if (exportFile != null) {
                    callback.onSuccess(exportFile);
                } else {
                    callback.onError(new Exception("Export failed"));
                }
            }
        });
    }
    
    /**
     * Callback interface for async export
     */
    public interface ExportCallback {
        void onSuccess(File exportFile);
        void onError(Exception error);
    }
}

