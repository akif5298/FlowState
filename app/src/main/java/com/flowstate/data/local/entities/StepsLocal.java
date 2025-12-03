package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Local steps count entity
 * Stored in Room database
 */
@Entity(
    tableName = "steps_local",
    indices = {@Index(value = {"timestamp"}, unique = true)}
)
public class StepsLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Step count for the day/period
     */
    public int steps;
    
    /**
     * Whether this record has been synced to remote storage
     */
    public boolean synced;
    
    public StepsLocal() {}
    
    @Ignore
    public StepsLocal(long timestamp, int steps) {
        this.timestamp = timestamp;
        this.steps = steps;
        this.synced = false;
    }
}
