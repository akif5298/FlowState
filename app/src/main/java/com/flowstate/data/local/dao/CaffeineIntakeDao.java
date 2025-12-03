package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.CaffeineIntakeLocal;

import java.util.List;

@Dao
public interface CaffeineIntakeDao {
    
    @Insert
    long insert(CaffeineIntakeLocal intake);
    
    @Insert
    List<Long> insertAll(List<CaffeineIntakeLocal> intakes);
    
    /**
     * Get all caffeine intake in date range
     */
    @Query("SELECT * FROM caffeine_intake_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<CaffeineIntakeLocal> getByDateRange(long startMs, long endMs);
    
    /**
     * Get total caffeine for a specific date
     */
    @Query("SELECT SUM(caffeineMg) FROM caffeine_intake_local WHERE timestamp >= :dateStartMs AND timestamp < :dateEndMs")
    Double getTotalForDate(long dateStartMs, long dateEndMs);
    
    @Update
    void update(CaffeineIntakeLocal intake);
    
    @Query("DELETE FROM caffeine_intake_local")
    void deleteAll();
}

