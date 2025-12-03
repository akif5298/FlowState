package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.ManualEnergyInputLocal;

import java.util.List;

@Dao
public interface ManualEnergyInputDao {
    
    @Insert
    long insert(ManualEnergyInputLocal input);
    
    @Insert
    List<Long> insertAll(List<ManualEnergyInputLocal> inputs);
    
    /**
     * Get most recent manual input
     */
    @Query("SELECT * FROM manual_energy_input ORDER BY timestamp DESC LIMIT 1")
    ManualEnergyInputLocal getMostRecent();
    
    /**
     * Get manual inputs in date range
     */
    @Query("SELECT * FROM manual_energy_input WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp DESC")
    List<ManualEnergyInputLocal> getByDateRange(long startMs, long endMs);
    
    @Update
    void update(ManualEnergyInputLocal input);
    
    @Query("DELETE FROM manual_energy_input")
    void deleteAll();
}

