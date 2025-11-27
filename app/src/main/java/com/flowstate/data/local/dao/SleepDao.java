package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.SleepLocal;

import java.util.List;

@Dao
public interface SleepDao {
    
    @Insert
    long insert(SleepLocal sleepLocal);
    
    @Insert
    List<Long> insertAll(List<SleepLocal> sleepLocals);
    
    /**
     * Get all pending (unsynced) records
     */
    @Query("SELECT * FROM sleep_local WHERE synced = 0 ORDER BY sleep_start ASC")
    List<SleepLocal> pending();
    
    /**
     * Get all records (for feature engineering)
     */
    @Query("SELECT * FROM sleep_local ORDER BY sleep_start ASC")
    List<SleepLocal> getAll();
    
    /**
     * Get sleep sessions ending before a timestamp (for last-night sleep)
     */
    @Query("SELECT * FROM sleep_local WHERE sleep_end IS NOT NULL AND sleep_end < :beforeMs ORDER BY sleep_end DESC LIMIT 1")
    SleepLocal getLastCompletedBefore(long beforeMs);
    
    /**
     * Mark records as synced by their IDs
     */
    @Query("UPDATE sleep_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
    
    /**
     * Delete all records
     */
    @Query("DELETE FROM sleep_local")
    void deleteAll();
    
    @Update
    void update(SleepLocal sleepLocal);
}

