package com.flowstate.services;

import android.content.Context;
import android.util.Log;

import com.flowstate.data.local.AppDb;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility to check if user has any data in the local database
 */
public class DataChecker {
    
    private static final String TAG = "DataChecker";
    private final AppDb db;
    private final ExecutorService executor;
    
    public DataChecker(Context context) {
        this.db = AppDb.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Check if user has any data (HR, Sleep, Typing, or Reaction)
     * 
     * @param callback Callback to receive the result
     */
    public void hasAnyData(DataCheckCallback callback) {
        executor.execute(() -> {
            try {
                boolean hasHr = !db.hrDao().getAll().isEmpty();
                boolean hasSleep = !db.sleepDao().getAll().isEmpty();
                boolean hasTyping = !db.typingDao().getAll().isEmpty();
                boolean hasReaction = !db.reactionDao().getAll().isEmpty();
                
                boolean hasAny = hasHr || hasSleep || hasTyping || hasReaction;
                
                DataCheckResult result = new DataCheckResult(hasAny, hasHr, hasSleep, hasTyping, hasReaction);
                
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking data", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Check if user has minimum data for predictions
     * Needs at least: HR data OR Sleep data, and at least one cognitive test
     */
    public void hasMinimumDataForPredictions(DataCheckCallback callback) {
        executor.execute(() -> {
            try {
                boolean hasHr = !db.hrDao().getAll().isEmpty();
                boolean hasSleep = !db.sleepDao().getAll().isEmpty();
                boolean hasTyping = !db.typingDao().getAll().isEmpty();
                boolean hasReaction = !db.reactionDao().getAll().isEmpty();
                
                // Need at least: (HR OR Sleep) AND (Typing OR Reaction)
                boolean hasBiometric = hasHr || hasSleep;
                boolean hasCognitive = hasTyping || hasReaction;
                boolean hasMinimum = hasBiometric && hasCognitive;
                
                DataCheckResult result = new DataCheckResult(hasMinimum, hasHr, hasSleep, hasTyping, hasReaction);
                
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking minimum data", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public static class DataCheckResult {
        public final boolean hasAnyData;
        public final boolean hasHrData;
        public final boolean hasSleepData;
        public final boolean hasTypingData;
        public final boolean hasReactionData;
        
        public DataCheckResult(boolean hasAnyData, boolean hasHrData, boolean hasSleepData, 
                              boolean hasTypingData, boolean hasReactionData) {
            this.hasAnyData = hasAnyData;
            this.hasHrData = hasHrData;
            this.hasSleepData = hasSleepData;
            this.hasTypingData = hasTypingData;
            this.hasReactionData = hasReactionData;
        }
    }
    
    public interface DataCheckCallback {
        void onResult(DataCheckResult result);
        void onError(Exception e);
    }
}

