package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.DeviceUsageLocal;

@Dao
public interface DeviceUsageDao {
    
    @Insert
    long insert(DeviceUsageLocal usage);
    
    /**
     * Get device usage for a specific date
     */
    @Query("SELECT * FROM device_usage_local WHERE date = :dateMs LIMIT 1")
    DeviceUsageLocal getByDate(long dateMs);
    
    /**
     * Get most recent device usage
     */
    @Query("SELECT * FROM device_usage_local ORDER BY date DESC LIMIT 1")
    DeviceUsageLocal getMostRecent();
    
    @Update
    void update(DeviceUsageLocal usage);
    
    @Query("DELETE FROM device_usage_local")
    void deleteAll();
}

