package com.flowstate.app;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.flowstate.app.supabase.SupabaseClient;
import com.personaleenergy.app.data.collection.HealthConnectManager;
import com.flowstate.app.supabase.repository.BiometricDataRepository;
import android.util.Log;

public class EnergyPredictorApplication extends Application {
    private static final String TAG = "EnergyPredictorApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase client
        SupabaseClient.getInstance(this);
        
        // Apply dark mode preference on app startup
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        // Sync Health Connect data on app startup (in background)
        syncHealthConnectDataOnStartup();
    }
    
    /**
     * Sync Health Connect data on app startup if permissions are granted
     */
    private void syncHealthConnectDataOnStartup() {
        // Run in background thread to avoid blocking app startup
        new Thread(() -> {
            try {
                HealthConnectManager healthConnectManager = new HealthConnectManager(this);
                
                // Check if Health Connect is available and permissions are granted
                if (!healthConnectManager.isAvailable()) {
                    Log.d(TAG, "Health Connect not available, skipping sync");
                    return;
                }
                
                // Check permissions asynchronously
                healthConnectManager.hasPermissionsJava().thenAccept(hasPermissions -> {
                    if (!hasPermissions) {
                        Log.d(TAG, "Health Connect permissions not granted, skipping sync");
                        return;
                    }
                    
                    // Check if user is authenticated
                    String userId = SupabaseClient.getInstance(this).getUserId();
                    if (userId == null || userId.isEmpty()) {
                        Log.d(TAG, "User not authenticated, skipping sync");
                        return;
                    }
                    
                    Log.d(TAG, "Starting automatic Health Connect sync on app startup");
                    
                    // Sync new data since last sync
                    healthConnectManager.syncNewDataSinceLastSync(
                        new HealthConnectManager.BiometricCallback() {
                            @Override
                            public void onSuccess(java.util.List<com.flowstate.app.data.models.BiometricData> data) {
                                if (data != null && !data.isEmpty()) {
                                    Log.d(TAG, "Synced " + data.size() + " new records from Health Connect");
                                    // Save to Supabase
                                    saveBiometricDataToSupabase(data);
                                } else {
                                    Log.d(TAG, "No new data to sync from Health Connect");
                                }
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Failed to sync Health Connect data on startup", e);
                            }
                        }
                    );
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during Health Connect sync on startup", e);
            }
        }).start();
    }
    
    /**
     * Save synced biometric data to Supabase
     */
    private void saveBiometricDataToSupabase(java.util.List<com.flowstate.app.data.models.BiometricData> data) {
        String userId = SupabaseClient.getInstance(this).getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot save data: user not authenticated");
            return;
        }
        
        BiometricDataRepository repo = new BiometricDataRepository(this);
        final int[] successCount = {0};
        final int[] errorCount = {0};
        final int totalCount = data.size();
        
        for (com.flowstate.app.data.models.BiometricData biometric : data) {
            repo.upsertBiometricData(userId, biometric, new BiometricDataRepository.DataCallback() {
                @Override
                public void onSuccess(Object result) {
                    synchronized (successCount) {
                        successCount[0]++;
                        if (successCount[0] + errorCount[0] >= totalCount) {
                            Log.d(TAG, "Finished saving biometric data: " + successCount[0] + " success, " + errorCount[0] + " errors");
                        }
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    synchronized (errorCount) {
                        errorCount[0]++;
                        Log.e(TAG, "Failed to save biometric data", error);
                        if (successCount[0] + errorCount[0] >= totalCount) {
                            Log.d(TAG, "Finished saving biometric data: " + successCount[0] + " success, " + errorCount[0] + " errors");
                        }
                    }
                }
            });
        }
    }
}

