package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.StepsLocal;

import java.util.List;

@Dao
public interface StepsDao {
    
    @Insert
    long insert(StepsLocal stepsLocal);
    
    @Insert
    List<Long> insertAll(List<StepsLocal> stepsLocals);
    
    /**
     * Get all pending (unsynced) records
     */
    @Query("SELECT * FROM steps_local WHERE synced = 0 ORDER BY timestamp ASC")
    List<StepsLocal> pending();
    
    /**
     * Get all records (for feature engineering)
     */
    @Query("SELECT * FROM steps_local ORDER BY timestamp ASC")
    List<StepsLocal> getAll();
    
    /**
     * Get records in date range
     */
    @Query("SELECT * FROM steps_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<StepsLocal> getByDateRange(long startMs, long endMs);
    
    /**
     * Mark records as synced by their IDs
     */
    @Query("UPDATE steps_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
    
    /**
     * Delete all records
     */
    @Query("DELETE FROM steps_local")
    void deleteAll();
    
    @Update
    void update(StepsLocal stepsLocal);
}
