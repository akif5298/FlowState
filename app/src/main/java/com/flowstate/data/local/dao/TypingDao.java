package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.TypingLocal;

import java.util.List;

@Dao
public interface TypingDao {
    
    @Insert
    long insert(TypingLocal typingLocal);
    
    @Insert
    List<Long> insertAll(List<TypingLocal> typingLocals);
    
    /**
     * Get all pending (unsynced) records
     */
    @Query("SELECT * FROM typing_local WHERE synced = 0 ORDER BY timestamp ASC")
    List<TypingLocal> pending();
    
    /**
     * Get all records (for feature engineering)
     */
    @Query("SELECT * FROM typing_local ORDER BY timestamp ASC")
    List<TypingLocal> getAll();
    
    /**
     * Get records in date range
     */
    @Query("SELECT * FROM typing_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<TypingLocal> getByDateRange(long startMs, long endMs);
    
    /**
     * Mark records as synced by their IDs
     */
    @Query("UPDATE typing_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
    
    /**
     * Delete all records
     */
    @Query("DELETE FROM typing_local")
    void deleteAll();
    
    @Update
    void update(TypingLocal typingLocal);
}

