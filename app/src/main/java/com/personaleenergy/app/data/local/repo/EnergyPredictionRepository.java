package com.personaleenergy.app.data.local.repo;

import android.content.Context;
import android.util.Log;

import com.personaleenergy.app.data.local.AppDb;
import com.personaleenergy.app.data.local.dao.PredictionDao;
import com.personaleenergy.app.data.local.entities.PredictionLocal;

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
    
    public EnergyPredictionRepository(Context context) {
        AppDb db = AppDb.getInstance(context);
        predictionDao = db.predictionDao();
        databaseWriteExecutor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Saves a list of prediction records to the local database
     *
     * @param items The list of PredictionLocal objects to save
     */
    public void saveAll(List<PredictionLocal> items) {
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
        databaseWriteExecutor.execute(() -> {
            try {
                // Use DAO method to get predictions by date range
                List<PredictionLocal> predictions = predictionDao.getByDateRange(dateStartMs, dateEndMs);
                callback.onSuccess(predictions);
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
