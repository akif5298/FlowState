package com.flowstate.data.local.repo;

import android.content.Context;
import android.util.Log;

import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.dao.PredictionDao;
import com.flowstate.data.local.entities.PredictionLocal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing local energy prediction data in Room database
 */
public class EnergyPredictionRepository {
    
    private static final String TAG = "EnergyPredictionRepo";
    private final PredictionDao predictionDao;
    private final ExecutorService databaseWriteExecutor;
    
    // In-memory cache for recent predictions
    private List<PredictionLocal> cachedPredictions;
    private long lastCacheTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes cache

    private static EnergyPredictionRepository instance;

    public static synchronized EnergyPredictionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new EnergyPredictionRepository(context.getApplicationContext());
        }
        return instance;
    }

    private EnergyPredictionRepository(Context context) { // Changed to private
        AppDb db = AppDb.getInstance(context);
        predictionDao = db.predictionDao();
        databaseWriteExecutor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Clear the in-memory cache
     */
    public void clearCache() {
        cachedPredictions = null;
        lastCacheTime = 0;
    }

    /**
     * Saves a list of prediction records to the local database
     *
     * @param items The list of PredictionLocal objects to save
     */
    public void saveAll(List<PredictionLocal> items) {
        // Update cache immediately with new data
        cachedPredictions = new java.util.ArrayList<>(items);
        lastCacheTime = System.currentTimeMillis();
        
        databaseWriteExecutor.execute(() -> {
            try {
                List<Long> insertedIds = predictionDao.insertAll(items);
                Log.d(TAG, "Saved " + insertedIds.size() + " prediction records to local DB.");
            } catch (Exception e) {
                Log.e(TAG, "Error saving prediction records to local DB", e);
            }
        });
    }
    
    /**
     * Saves a single prediction record to the local database
     *
     * @param item The PredictionLocal object to save
     */
    public void save(PredictionLocal item) {
        databaseWriteExecutor.execute(() -> {
            try {
                long id = predictionDao.insert(item);
                Log.d(TAG, "Saved prediction record with ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error saving prediction record to local DB", e);
            }
        });
    }
    
    /**
     * Retrieves all unsynced prediction records from the local database
     *
     * @param callback Callback to receive the list of pending records
     */
    public void getPending(DataCallback<List<PredictionLocal>> callback) {
        databaseWriteExecutor.execute(() -> {
            try {
                List<PredictionLocal> pending = predictionDao.pending();
                callback.onSuccess(pending);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Marks a list of prediction records as synced in the local database
     *
     * @param ids The IDs of the records to mark as synced
     */
    public void markSynced(List<Long> ids) {
        databaseWriteExecutor.execute(() -> {
            try {
                predictionDao.markSynced(ids);
                Log.d(TAG, "Marked " + ids.size() + " prediction records as synced.");
            } catch (Exception e) {
                Log.e(TAG, "Error marking prediction records as synced", e);
            }
        });
    }
    
    /**
     * Get all predictions for a specific date
     * 
     * @param dateStartMs Start of date in epoch milliseconds
     * @param dateEndMs End of date in epoch milliseconds
     * @param callback Callback to receive the list of predictions
     */
    public void getByDateRange(long dateStartMs, long dateEndMs, DataCallback<List<PredictionLocal>> callback) {
        // Check memory cache first
        if (cachedPredictions != null && System.currentTimeMillis() - lastCacheTime < CACHE_VALIDITY_MS) {
            // Filter cache for requested range
            List<PredictionLocal> filtered = new java.util.ArrayList<>();
            boolean fullCoverage = false;
            
            // Simple check: if cache has data covering the requested start, we might use it
            // For now, if we have a valid cache, let's just use it if it's "fresh" enough
            // But strict filtering is safer
            for (PredictionLocal p : cachedPredictions) {
                if (p.predictionTime >= dateStartMs && p.predictionTime < dateEndMs) {
                    filtered.add(p);
                }
            }
            
            if (!filtered.isEmpty()) {
                Log.d(TAG, "Returning " + filtered.size() + " predictions from memory cache");
                callback.onSuccess(filtered);
                return;
            }
        }

        databaseWriteExecutor.execute(() -> {
            try {
                // Use DAO method to get predictions by date range
                List<PredictionLocal> predictions = predictionDao.getByDateRange(dateStartMs, dateEndMs);
                
                // Update cache if we got results and it's a useful set (e.g. covering today)
                if (!predictions.isEmpty()) {
                    // Only cache if we think this is the "main" forecast (heuristic)
                    // Or just always cache the last result
                    cachedPredictions = new java.util.ArrayList<>(predictions);
                    lastCacheTime = System.currentTimeMillis();
                }
                
                callback.onSuccess(predictions);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Get the most recent prediction
     * 
     * @param callback Callback to receive the latest prediction
     */
    public void getLatestPrediction(DataCallback<PredictionLocal> callback) {
        databaseWriteExecutor.execute(() -> {
            try {
                // Get latest prediction that is not in the future (for current status display)
                PredictionLocal latest = predictionDao.getLatestCurrent(System.currentTimeMillis());
                callback.onSuccess(latest);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }
}

