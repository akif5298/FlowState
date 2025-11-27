package com.flowstate.services;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.flowstate.app.R;

import java.util.concurrent.TimeUnit;

/**
 * Google Fit Manager for reading health data
 * 
 * Handles permissions, connection, and data reading for heart rate and sleep data
 */
public class GoogleFitManager {
    
    private static final String TAG = "GoogleFitManager";
    private static final String PREFS_NAME = "google_fit_prefs";
    private static final String KEY_FIRST_CONNECT = "first_connect";
    private static final String KEY_LAST_SYNC = "last_sync";
    
    private Context context;
    private GoogleSignInClient signInClient;
    private FitnessOptions fitnessOptions;
    private SharedPreferences prefs;
    
    public GoogleFitManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Configure Google Sign-In for Fitness API
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/fitness.activity.read"))
                .requestScopes(new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read"))
                .requestScopes(new Scope("https://www.googleapis.com/auth/fitness.sleep.read"))
                .build();
        
        this.signInClient = GoogleSignIn.getClient(context, signInOptions);
        
        // Configure Fitness Options
        this.fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
                .build();
    }
    
    /**
     * Check if Google Fit permissions are granted
     */
    public boolean hasPermissions(Activity activity) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            return false;
        }
        
        return GoogleSignIn.hasPermissions(account, fitnessOptions);
    }
    
    /**
     * Request Google Fit permissions
     */
    public void requestPermissions(Activity activity, int requestCode) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            // Not signed in, start sign-in flow
            activity.startActivityForResult(signInClient.getSignInIntent(), requestCode);
            return;
        }
        
        // Request permissions if not already granted
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(activity, requestCode, account, fitnessOptions);
        }
    }
    
    /**
     * Read heart rate data for the specified time range
     * 
     * @param startMs Start time in milliseconds (epoch)
     * @param endMs End time in milliseconds (epoch)
     * @return Task with DataReadResponse
     */
    public Task<DataReadResponse> readHeartRate(long startMs, long endMs) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e(TAG, "Not signed in to Google Fit");
            return com.google.android.gms.tasks.Tasks.forException(
                new Exception("Not signed in to Google Fit. Please connect first.")
            );
        }
        
        if (!hasPermissions((Activity) context)) {
            Log.e(TAG, "Google Fit permissions not granted");
            return com.google.android.gms.tasks.Tasks.forException(
                new Exception("Google Fit permissions not granted")
            );
        }
        
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(com.google.android.gms.fitness.data.DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(startMs, endMs, TimeUnit.MILLISECONDS)
                .enableServerQueries()
                .build();
        
        return Fitness.getHistoryClient(context, account)
                .readData(readRequest);
    }
    
    /**
     * Read sleep data for the specified time range
     * 
     * @param startMs Start time in milliseconds (epoch)
     * @param endMs End time in milliseconds (epoch)
     * @return Task with DataReadResponse
     */
    public Task<DataReadResponse> readSleep(long startMs, long endMs) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e(TAG, "Not signed in to Google Fit");
            return com.google.android.gms.tasks.Tasks.forException(
                new Exception("Not signed in to Google Fit. Please connect first.")
            );
        }
        
        if (!hasPermissions((Activity) context)) {
            Log.e(TAG, "Google Fit permissions not granted");
            return com.google.android.gms.tasks.Tasks.forException(
                new Exception("Google Fit permissions not granted")
            );
        }
        
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(com.google.android.gms.fitness.data.DataType.TYPE_SLEEP_SEGMENT)
                .setTimeRange(startMs, endMs, TimeUnit.MILLISECONDS)
                .enableServerQueries()
                .build();
        
        return Fitness.getHistoryClient(context, account)
                .readData(readRequest);
    }
    
    /**
     * Check if this is the first connection
     */
    public boolean isFirstConnect() {
        return prefs.getBoolean(KEY_FIRST_CONNECT, true);
    }
    
    /**
     * Mark that first connection has been completed
     */
    public void markFirstConnectComplete() {
        prefs.edit().putBoolean(KEY_FIRST_CONNECT, false).apply();
    }
    
    /**
     * Get last sync timestamp
     */
    public long getLastSync() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }
    
    /**
     * Update last sync timestamp
     */
    public void updateLastSync(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply();
    }
    
    /**
     * Get time range for backfill based on first connect or regular sync
     * 
     * @return Array with [startMs, endMs]
     */
    public long[] getBackfillTimeRange() {
        long endMs = System.currentTimeMillis();
        long startMs;
        
        if (isFirstConnect()) {
            // First connect: read last 7 days
            startMs = endMs - TimeUnit.DAYS.toMillis(7);
            Log.d(TAG, "First connect: backfilling last 7 days");
        } else {
            // Regular sync: read last 24 hours or since last sync
            long lastSync = getLastSync();
            if (lastSync > 0) {
                startMs = lastSync;
            } else {
                startMs = endMs - TimeUnit.HOURS.toMillis(24);
            }
            Log.d(TAG, "Regular sync: backfilling since last sync or 24h");
        }
        
        return new long[]{startMs, endMs};
    }
    
    /**
     * Check if user is signed in
     */
    public boolean isSignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null;
    }
    
    /**
     * Get Google Sign-In client for sign-in flow
     */
    public GoogleSignInClient getSignInClient() {
        return signInClient;
    }
}

