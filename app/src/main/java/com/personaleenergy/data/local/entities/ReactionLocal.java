package com.personaleenergy.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Local reaction time test entity
 * Stored in Room database
 */
@Entity(
    tableName = "reaction_local",
    indices = {@Index(value = {"timestamp"}, unique = true)}
)
public class ReactionLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Median reaction time in milliseconds
     */
    public int medianMs;
    
    /**
     * Number of tests performed
     */
    public int testCount;
    
    /**
     * Whether this record has been synced
     */
    public boolean synced;
    
    public ReactionLocal() {}
    
    @Ignore
    public ReactionLocal(long timestamp, int medianMs, int testCount) {
        this.timestamp = timestamp;
        this.medianMs = medianMs;
        this.testCount = testCount;
        this.synced = false;
    }
}
