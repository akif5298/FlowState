package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.ReactionLocal;

import java.util.List;

@Dao
public interface ReactionDao {
    
    @Insert
    long insert(ReactionLocal reactionLocal);
    
    @Insert
    List<Long> insertAll(List<ReactionLocal> reactionLocals);
    
    /**
     * Get all pending (unsynced) records
     */
    @Query("SELECT * FROM reaction_local WHERE synced = 0 ORDER BY timestamp ASC")
    List<ReactionLocal> pending();
    
    /**
     * Get all records (for feature engineering)
     */
    @Query("SELECT * FROM reaction_local ORDER BY timestamp ASC")
    List<ReactionLocal> getAll();
    
    /**
     * Get records in date range
     */
    @Query("SELECT * FROM reaction_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<ReactionLocal> getByDateRange(long startMs, long endMs);
    
    /**
     * Mark records as synced by their IDs
     */
    @Query("UPDATE reaction_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
    
    /**
     * Delete all records
     */
    @Query("DELETE FROM reaction_local")
    void deleteAll();
    
    @Update
    void update(ReactionLocal reactionLocal);
}

