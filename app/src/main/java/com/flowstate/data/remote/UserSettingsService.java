package com.flowstate.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.flowstate.data.remote.PostgrestApi;
import com.flowstate.data.remote.SupabaseClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for fetching and caching user settings
 * 
 * Fetches user settings from Supabase and caches them in SharedPreferences
 */
public class UserSettingsService {
    
    private static final String TAG = "UserSettingsService";
    private static final String PREFS_NAME = "user_settings_cache";
    
    private Context context;
    private SupabaseClient supabaseClient;
    private PostgrestApi postgrestApi;
    private SharedPreferences prefs;
    private ExecutorService executor;
    
    // Settings keys
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_NOTIFICATION_TIME = "notification_time";
    private static final String KEY_GOOGLE_FIT_ENABLED = "google_fit_enabled";
    private static final String KEY_GOOGLE_FIT_ACCOUNT = "google_fit_account";
    private static final String KEY_ML_MODEL_PREFERENCE = "ml_model_preference";
    private static final String KEY_DATA_SYNC_ENABLED = "data_sync_enabled";
    private static final String KEY_LAST_FETCH = "last_fetch";
    
    public UserSettingsService(Context context) {
        this.context = context.getApplicationContext();
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.postgrestApi = supabaseClient.getPostgrestApi();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * User settings model
     */
    public static class UserSettings {
        public boolean notificationEnabled;
        public String notificationTime;
        public boolean googleFitEnabled;
        public String googleFitAccount;
        public String mlModelPreference;
        public boolean dataSyncEnabled;
        
        public UserSettings() {
            // Default values
            this.notificationEnabled = true;
            this.notificationTime = "09:00";
            this.googleFitEnabled = false;
            this.googleFitAccount = null;
            this.mlModelPreference = "default";
            this.dataSyncEnabled = true;
        }
    }
    
    /**
     * Fetch user settings from Supabase
     * 
     * @param callback Callback to handle result
     */
    public void fetch(SettingsCallback callback) {
        String userId = getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "No user ID available for fetching settings");
            if (callback != null) {
                callback.onError(new Exception("No user ID available"));
            }
            return;
        }
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("user_id", "eq." + userId);
        
        Call<List<Map<String, Object>>> call = postgrestApi.get("user_settings", queryParams);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, 
                                 Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> data = response.body();
                    if (!data.isEmpty()) {
                        UserSettings settings = parseSettings(data.get(0));
                        // Cache to SharedPreferences
                        cacheSettings(settings);
                        if (callback != null) {
                            callback.onSuccess(settings);
                        }
                    } else {
                        // No settings found, return defaults
                        UserSettings defaultSettings = new UserSettings();
                        cacheSettings(defaultSettings);
                        if (callback != null) {
                            callback.onSuccess(defaultSettings);
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch settings: " + response.code());
                    // Return cached settings if available
                    UserSettings cachedSettings = getCachedSettings();
                    if (callback != null) {
                        if (cachedSettings != null) {
                            callback.onSuccess(cachedSettings);
                        } else {
                            callback.onError(new Exception("Failed to fetch settings"));
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "Error fetching settings", t);
                // Return cached settings if available
                UserSettings cachedSettings = getCachedSettings();
                if (callback != null) {
                    if (cachedSettings != null) {
                        callback.onSuccess(cachedSettings);
                    } else {
                        callback.onError(new Exception(t.getMessage(), t));
                    }
                }
            }
        });
    }
    
    /**
     * Parse settings from Map
     */
    private UserSettings parseSettings(Map<String, Object> data) {
        UserSettings settings = new UserSettings();
        
        try {
            if (data.containsKey("notification_enabled")) {
                Object value = data.get("notification_enabled");
                if (value instanceof Boolean) {
                    settings.notificationEnabled = (Boolean) value;
                } else if (value instanceof String) {
                    settings.notificationEnabled = Boolean.parseBoolean((String) value);
                }
            }
            
            if (data.containsKey("notification_time")) {
                settings.notificationTime = String.valueOf(data.get("notification_time"));
            }
            
            if (data.containsKey("google_fit_enabled")) {
                Object value = data.get("google_fit_enabled");
                if (value instanceof Boolean) {
                    settings.googleFitEnabled = (Boolean) value;
                } else if (value instanceof String) {
                    settings.googleFitEnabled = Boolean.parseBoolean((String) value);
                }
            }
            
            if (data.containsKey("google_fit_account")) {
                settings.googleFitAccount = String.valueOf(data.get("google_fit_account"));
            }
            
            if (data.containsKey("ml_model_preference")) {
                settings.mlModelPreference = String.valueOf(data.get("ml_model_preference"));
            }
            
            if (data.containsKey("data_sync_enabled")) {
                Object value = data.get("data_sync_enabled");
                if (value instanceof Boolean) {
                    settings.dataSyncEnabled = (Boolean) value;
                } else if (value instanceof String) {
                    settings.dataSyncEnabled = Boolean.parseBoolean((String) value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing settings", e);
        }
        
        return settings;
    }
    
    /**
     * Cache settings to SharedPreferences
     */
    private void cacheSettings(UserSettings settings) {
        prefs.edit()
                .putBoolean(KEY_NOTIFICATION_ENABLED, settings.notificationEnabled)
                .putString(KEY_NOTIFICATION_TIME, settings.notificationTime)
                .putBoolean(KEY_GOOGLE_FIT_ENABLED, settings.googleFitEnabled)
                .putString(KEY_GOOGLE_FIT_ACCOUNT, settings.googleFitAccount)
                .putString(KEY_ML_MODEL_PREFERENCE, settings.mlModelPreference)
                .putBoolean(KEY_DATA_SYNC_ENABLED, settings.dataSyncEnabled)
                .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                .apply();
        
        Log.d(TAG, "Settings cached to SharedPreferences");
    }
    
    /**
     * Get cached settings from SharedPreferences
     */
    public UserSettings getCachedSettings() {
        UserSettings settings = new UserSettings();
        
        settings.notificationEnabled = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
        settings.notificationTime = prefs.getString(KEY_NOTIFICATION_TIME, "09:00");
        settings.googleFitEnabled = prefs.getBoolean(KEY_GOOGLE_FIT_ENABLED, false);
        settings.googleFitAccount = prefs.getString(KEY_GOOGLE_FIT_ACCOUNT, null);
        settings.mlModelPreference = prefs.getString(KEY_ML_MODEL_PREFERENCE, "default");
        settings.dataSyncEnabled = prefs.getBoolean(KEY_DATA_SYNC_ENABLED, true);
        
        return settings;
    }
    
    /**
     * Check if cached settings are stale (older than 1 hour)
     */
    public boolean isCacheStale() {
        long lastFetch = prefs.getLong(KEY_LAST_FETCH, 0);
        if (lastFetch == 0) {
            return true;
        }
        long oneHour = 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastFetch) > oneHour;
    }
    
    /**
     * Save user settings to Supabase
     * Uses upsert (insert or update) based on user_id
     */
    public void save(UserSettings settings, SettingsCallback callback) {
        String userId = getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "No user ID available for saving settings");
            if (callback != null) {
                callback.onError(new Exception("No user ID available"));
            }
            return;
        }
        
        // Prepare data map
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_id", userId);
        dataMap.put("notification_enabled", settings.notificationEnabled);
        if (settings.notificationTime != null) {
            dataMap.put("notification_time", settings.notificationTime);
        }
        dataMap.put("google_fit_enabled", settings.googleFitEnabled);
        if (settings.googleFitAccount != null) {
            dataMap.put("google_fit_account", settings.googleFitAccount);
        }
        if (settings.mlModelPreference != null) {
            dataMap.put("ml_model_preference", settings.mlModelPreference);
        }
        dataMap.put("data_sync_enabled", settings.dataSyncEnabled);
        
        // Use POST with upsert (resolution=merge-duplicates) to insert or update
        // This works because user_id is UNIQUE in the schema
        Call<Void> call = postgrestApi.post("user_settings", dataMap);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Cache settings locally
                    cacheSettings(settings);
                    Log.d(TAG, "Settings saved successfully");
                    if (callback != null) {
                        callback.onSuccess(settings);
                    }
                } else {
                    // Try PATCH if POST fails (record might exist)
                    Map<String, String> queryParams = new HashMap<>();
                    queryParams.put("user_id", "eq." + userId);
                    Call<Void> patchCall = postgrestApi.patch("user_settings", queryParams, dataMap);
                    patchCall.enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                cacheSettings(settings);
                                if (callback != null) {
                                    callback.onSuccess(settings);
                                }
                            } else {
                                String error = "Failed to save settings: " + response.code();
                                Log.e(TAG, error);
                                if (callback != null) {
                                    callback.onError(new Exception(error));
                                }
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e(TAG, "Error saving settings", t);
                            if (callback != null) {
                                callback.onError(new Exception(t.getMessage(), t));
                            }
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error saving settings", t);
                if (callback != null) {
                    callback.onError(new Exception(t.getMessage(), t));
                }
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
     * Callback interface for async operations
     */
    public interface SettingsCallback {
        void onSuccess(UserSettings settings);
        void onError(Exception error);
    }
}

