package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_local")
public class WorkoutLocal {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    
    public long start_time;
    public long end_time;
    public int duration_minutes;
    public String type; // e.g., "Running", "Walking" (can be mapped from int)
    public boolean synced = false;
}

