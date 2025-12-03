package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hrv_local")
public class HrvLocal {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public long timestamp;
    public double rmssd; // Root Mean Square of Successive Differences
    public boolean synced = false;
}

