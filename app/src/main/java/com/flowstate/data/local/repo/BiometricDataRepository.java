package com.flowstate.data.local.repo;

import android.content.Context;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for biometric data operations
 * Handles saving heart rate and sleep data to Room database
 */
public class BiometricDataRepository {
    
    private static final String TAG = "BiometricDataRepo";
    
    private AppDb db;
    private ExecutorService executor;
    
    public BiometricDataRepository(Context context) {
        this.db = AppDb.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Save heart rate readings to Room database
     * 
     * @param items List of HrLocal entities to save
     */
    public void saveHr(List<HrLocal> items) {
        if (items == null || items.isEmpty()) {
            Log.d(TAG, "No heart rate items to save");
            return;
        }
        
        executor.execute(() -> {
            try {
                // Insert all items
                List<Long> ids = db.hrDao().insertAll(items);
                Log.d(TAG, "Saved " + ids.size() + " heart rate readings to Room");
            } catch (Exception e) {
                Log.e(TAG, "Error saving heart rate data to Room", e);
            }
        });
    }
    
    /**
     * Save sleep sessions to Room database
     * 
     * @param items List of SleepLocal entities to save
     */
    public void saveSleep(List<SleepLocal> items) {
        if (items == null || items.isEmpty()) {
            Log.d(TAG, "No sleep items to save");
            return;
        }
        
        executor.execute(() -> {
            try {
                // Insert all items
                List<Long> ids = db.sleepDao().insertAll(items);
                Log.d(TAG, "Saved " + ids.size() + " sleep sessions to Room");
            } catch (Exception e) {
                Log.e(TAG, "Error saving sleep data to Room", e);
            }
        });
    }
    
    /**
     * Get pending (unsynced) heart rate readings
     */
    public void getPendingHr(DataCallback<List<HrLocal>> callback) {
        executor.execute(() -> {
            try {
                List<HrLocal> pending = db.hrDao().pending();
                callback.onSuccess(pending);
            } catch (Exception e) {
                Log.e(TAG, "Error getting pending heart rate data", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Get pending (unsynced) sleep sessions
     */
    public void getPendingSleep(DataCallback<List<SleepLocal>> callback) {
        executor.execute(() -> {
            try {
                List<SleepLocal> pending = db.sleepDao().pending();
                callback.onSuccess(pending);
            } catch (Exception e) {
                Log.e(TAG, "Error getting pending sleep data", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Mark heart rate readings as synced
     */
    public void markHrSynced(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                db.hrDao().markSynced(ids);
                Log.d(TAG, "Marked " + ids.size() + " heart rate readings as synced");
            } catch (Exception e) {
                Log.e(TAG, "Error marking heart rate as synced", e);
            }
        });
    }
    
    /**
     * Mark sleep sessions as synced
     */
    public void markSleepSynced(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                db.sleepDao().markSynced(ids);
                Log.d(TAG, "Marked " + ids.size() + " sleep sessions as synced");
            } catch (Exception e) {
                Log.e(TAG, "Error marking sleep as synced", e);
            }
        });
    }
    
    /**
     * Callback interface for async operations
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception error);
    }
}

