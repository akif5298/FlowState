package com.personaleenergy.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.personaleenergy.data.local.entities.HrLocal;

import java.util.List;

@Dao
public interface HrDao {
    
    @Insert
    long insert(HrLocal hrLocal);
    
    @Insert
    List<Long> insertAll(List<HrLocal> hrLocals);
    
    /**
     * Get all pending (unsynced) records
     */
    @Query("SELECT * FROM hr_local WHERE synced = 0 ORDER BY timestamp ASC")
    List<HrLocal> pending();
    
    /**
     * Get all records (for feature engineering)
     */
    @Query("SELECT * FROM hr_local ORDER BY timestamp ASC")
    List<HrLocal> getAll();
    
    /**
     * Get records in date range
     */
    @Query("SELECT * FROM hr_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<HrLocal> getByDateRange(long startMs, long endMs);
    
    /**
     * Mark records as synced by their IDs
     */
    @Query("UPDATE hr_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
    
    /**
     * Delete all records
     */
    @Query("DELETE FROM hr_local")
    void deleteAll();
    
    @Update
    void update(HrLocal hrLocal);
}
