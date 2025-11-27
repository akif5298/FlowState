package com.flowstate.data.local.repo;

import android.content.Context;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.TypingLocal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for typing speed test data operations
 * Handles saving typing test data to Room database
 */
public class TypingSpeedRepository {
    
    private static final String TAG = "TypingSpeedRepo";
    
    private AppDb db;
    private ExecutorService executor;
    
    public TypingSpeedRepository(Context context) {
        this.db = AppDb.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Save typing test data to Room database
     * 
     * @param typingLocal TypingLocal entity to save
     */
    public void save(TypingLocal typingLocal) {
        save(typingLocal, null);
    }
    
    /**
     * Save typing test data to Room database with callback
     * 
     * @param typingLocal TypingLocal entity to save
     * @param callback Optional callback for success/error
     */
    public void save(TypingLocal typingLocal, DataCallback<Long> callback) {
        if (typingLocal == null) {
            Log.d(TAG, "No typing test data to save");
            if (callback != null) {
                callback.onError(new Exception("No typing test data to save"));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                long id = db.typingDao().insert(typingLocal);
                Log.d(TAG, "Saved typing test to Room with id: " + id);
                if (callback != null) {
                    callback.onSuccess(id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving typing test data to Room", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Save multiple typing test records
     */
    public void saveAll(List<TypingLocal> typingLocals) {
        if (typingLocals == null || typingLocals.isEmpty()) {
            Log.d(TAG, "No typing test data to save");
            return;
        }
        
        executor.execute(() -> {
            try {
                List<Long> ids = db.typingDao().insertAll(typingLocals);
                Log.d(TAG, "Saved " + ids.size() + " typing tests to Room");
            } catch (Exception e) {
                Log.e(TAG, "Error saving typing test data to Room", e);
            }
        });
    }
    
    /**
     * Get all pending (unsynced) records
     */
    public void getPending(DataCallback<List<TypingLocal>> callback) {
        executor.execute(() -> {
            try {
                List<TypingLocal> pending = db.typingDao().pending();
                callback.onSuccess(pending);
            } catch (Exception e) {
                Log.e(TAG, "Error getting pending typing test data", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Mark records as synced by their IDs
     */
    public void markSynced(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                db.typingDao().markSynced(ids);
                Log.d(TAG, "Marked " + ids.size() + " typing tests as synced");
            } catch (Exception e) {
                Log.e(TAG, "Error marking typing tests as synced", e);
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

