package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "steps_local")
public class StepsLocal {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public long start_time;
    public long end_time;
    public int count;
    public boolean synced = false;
}

