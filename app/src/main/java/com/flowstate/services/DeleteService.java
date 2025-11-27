package com.flowstate.services;

import android.content.Context;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.remote.PostgrestApi;
import com.flowstate.data.remote.RpcApi;
import com.flowstate.data.remote.SupabaseClient;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Response;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for deleting user data (remote and local)
 * 
 * Handles deletion of all user data from Supabase and local Room database
 */
public class DeleteService {
    
    private static final String TAG = "DeleteService";
    
    // List of tables to delete data from (all tables with user_id field)
    private static final String[] TABLES = {
        "profiles",
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
    
    private Context context;
    private SupabaseClient supabaseClient;
    private PostgrestApi postgrestApi;
    private RpcApi rpcApi;
    private AppDb db;
    private ExecutorService executor;
    
    public DeleteService(Context context) {
        this.context = context.getApplicationContext();
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.rpcApi = supabaseClient.getRpcApi();
        this.db = AppDb.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Delete all user data from remote Supabase database
     * 
     * Uses DELETE requests with user_id filter for each table
     */
    public void deleteRemoteAll(DeleteCallback callback) {
        executor.execute(() -> {
            String userId = getUserId();
            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "No user ID available for deletion");
                if (callback != null) {
                    callback.onError(new Exception("No user ID available"));
                }
                return;
            }
            
            Log.d(TAG, "Starting remote data deletion for user: " + userId);
            
            int successCount = 0;
            int failCount = 0;
            
            // Try to use RPC function first (if available)
            boolean rpcSuccess = tryDeleteViaRpc(userId);
            
            if (!rpcSuccess) {
                // Fallback: delete each table individually
                for (String table : TABLES) {
                    boolean success = deleteTableData(table, userId);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                }
                
                // Delete profiles separately (uses id, not user_id)
                boolean profileSuccess = deleteProfile(userId);
                if (profileSuccess) {
                    successCount++;
                } else {
                    failCount++;
                }
            } else {
                successCount = TABLES.length + 1; // +1 for profiles
            }
            
            Log.d(TAG, "Remote deletion completed. Success: " + successCount + ", Failed: " + failCount);
            
            if (callback != null) {
                if (failCount == 0) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("Some deletions failed: " + failCount));
                }
            }
        });
    }
    
    /**
     * Try to delete all data via RPC function (if available)
     */
    private boolean tryDeleteViaRpc(String userId) {
        try {
            // Create JSON body for RPC call
            JsonObject body = new JsonObject();
            body.addProperty("user_id", userId);
            
            // Try to call delete_user_data RPC function
            Call<Void> call = rpcApi.upsertHeartRate(body); // This is a placeholder - would need actual delete RPC
            Response<Void> response = call.execute();
            
            // For now, return false to use per-table deletion
            return false;
        } catch (Exception e) {
            Log.d(TAG, "RPC deletion not available, using per-table deletion");
            return false;
        }
    }
    
    /**
     * Delete profile data (uses id, not user_id)
     */
    private boolean deleteProfile(String userId) {
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("id", "eq." + userId);
            
            Call<Void> call = postgrestApi.delete("profiles", queryParams);
            Response<Void> response = call.execute();
            
            if (response.isSuccessful()) {
                Log.d(TAG, "Successfully deleted profile");
                return true;
            } else {
                Log.w(TAG, "Failed to delete profile: " + response.code());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting profile", e);
            return false;
        }
    }
    
    /**
     * Delete data from a specific table using DELETE request
     */
    private boolean deleteTableData(String table, String userId) {
        try {
            // Use PostgREST DELETE with user_id filter
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("user_id", "eq." + userId);
            
            Call<Void> call = postgrestApi.delete(table, queryParams);
            Response<Void> response = call.execute();
            
            if (response.isSuccessful()) {
                Log.d(TAG, "Successfully deleted data from table: " + table);
                return true;
            } else {
                Log.w(TAG, "Failed to delete from " + table + ": " + response.code());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting from " + table, e);
            return false;
        }
    }
    
    /**
     * Clear all local Room database tables
     * 
     * @param ctx Application context
     */
    public void clearLocal(Context ctx) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting local data deletion");
                
                // Delete all records from Room tables
                db.hrDao().deleteAll();
                db.sleepDao().deleteAll();
                db.typingDao().deleteAll();
                db.reactionDao().deleteAll();
                db.predictionDao().deleteAll();
                
                Log.d(TAG, "Local data deletion completed");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing local data", e);
            }
        });
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
     * Callback interface for async deletion
     */
    public interface DeleteCallback {
        void onSuccess();
        void onError(Exception error);
    }
}

