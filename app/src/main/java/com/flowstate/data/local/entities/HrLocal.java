package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Local heart rate reading entity
 * Stored in Room database before syncing to Supabase
 */
@Entity(
    tableName = "hr_local",
    indices = {@Index(value = {"timestamp"}, unique = true)}
)
public class HrLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Heart rate in beats per minute
     */
    public int bpm;
    
    /**
     * Whether this record has been synced to Supabase
     */
    public boolean synced;
    
    public HrLocal() {}
    
    @Ignore
    public HrLocal(long timestamp, int bpm) {
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.synced = false;
    }
}

