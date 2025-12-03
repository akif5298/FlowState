package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.PredictionLocal;

import java.util.List;

@Dao
public interface PredictionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PredictionLocal predictionLocal);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<PredictionLocal> predictionLocals);
    
    /**
     * Get all pending (unsynced) records
     */
    @Query("SELECT * FROM prediction_local WHERE synced = 0 ORDER BY predictionTime ASC")
    List<PredictionLocal> pending();
    
    /**
     * Get records in date range (for feature engineering and display)
     */
    @Query("SELECT * FROM prediction_local WHERE predictionTime >= :startMs AND predictionTime < :endMs ORDER BY predictionTime ASC")
    List<PredictionLocal> getByDateRange(long startMs, long endMs);
    
    /**
     * Mark records as synced by their IDs
     */
    @Query("UPDATE prediction_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
    
    /**
     * Delete all records
     */
    @Query("DELETE FROM prediction_local")
    void deleteAll();
    
    @Query("SELECT * FROM prediction_local ORDER BY predictionTime DESC LIMIT 1")
    PredictionLocal getLatest();
    
    @Update
    void update(PredictionLocal predictionLocal);
}

