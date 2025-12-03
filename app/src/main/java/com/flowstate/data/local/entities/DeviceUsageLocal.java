package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores device usage/screen time data
 */
@Entity(
    tableName = "device_usage_local",
    indices = {@Index(value = {"date"})}
)
public class DeviceUsageLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Date in epoch milliseconds (start of day)
     */
    public long date; // Start of day timestamp
    
    /**
     * Screen time in minutes
     */
    public long screenTimeMinutes;
    
    /**
     * Last updated timestamp
     */
    public long lastUpdated;
    
    public DeviceUsageLocal() {}
}

