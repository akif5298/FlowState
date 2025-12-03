package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "body_temp_local")
public class BodyTempLocal {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public long timestamp;
    public double temperature_celsius;
    public boolean synced = false;
}

