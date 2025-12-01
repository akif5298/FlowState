package com.personaleenergy.data.local.repo;

import android.content.Context;
import android.util.Log;
import com.personaleenergy.data.local.AppDb;
import com.personaleenergy.data.local.entities.ReactionLocal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for reaction time test data operations
 * Handles saving reaction time test data to Room database
 */
public class ReactionTimeRepository {
    
    private static final String TAG = "ReactionTimeRepo";
    
    private AppDb db;
    private ExecutorService executor;
    
    public ReactionTimeRepository(Context context) {
        this.db = AppDb.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Save reaction time test data to Room database
     * 
     * @param reactionLocal ReactionLocal entity to save
     */
    public void save(ReactionLocal reactionLocal) {
        save(reactionLocal, null);
    }
    
    /**
     * Save reaction time test data to Room database with callback
     * 
     * @param reactionLocal ReactionLocal entity to save
     * @param callback Optional callback for success/error
     */
    public void save(ReactionLocal reactionLocal, DataCallback<Long> callback) {
        if (reactionLocal == null) {
            Log.d(TAG, "No reaction time test data to save");
            if (callback != null) {
                callback.onError(new Exception("No reaction time test data to save"));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                long id = db.reactionDao().insert(reactionLocal);
                Log.d(TAG, "Saved reaction time test to Room with id: " + id);
                if (callback != null) {
                    callback.onSuccess(id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving reaction time test data to Room", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Save multiple reaction time test records
     */
    public void saveAll(List<ReactionLocal> reactionLocals) {
        if (reactionLocals == null || reactionLocals.isEmpty()) {
            Log.d(TAG, "No reaction time test data to save");
            return;
        }
        
        executor.execute(() -> {
            try {
                List<Long> ids = db.reactionDao().insertAll(reactionLocals);
                Log.d(TAG, "Saved " + ids.size() + " reaction time tests to Room");
            } catch (Exception e) {
                Log.e(TAG, "Error saving reaction time test data to Room", e);
            }
        });
    }
    
    /**
     * Get all pending (unsynced) records
     */
    public void getPending(DataCallback<List<ReactionLocal>> callback) {
        executor.execute(() -> {
            try {
                List<ReactionLocal> pending = db.reactionDao().pending();
                callback.onSuccess(pending);
            } catch (Exception e) {
                Log.e(TAG, "Error getting pending reaction time test data", e);
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
                db.reactionDao().markSynced(ids);
                Log.d(TAG, "Marked " + ids.size() + " reaction time tests as synced");
            } catch (Exception e) {
                Log.e(TAG, "Error marking reaction time tests as synced", e);
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
