package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Local sleep session entity
 * Stored in Room database
 */
@Entity(
    tableName = "sleep_local",
    indices = {@Index(value = {"sleep_start"}, unique = true)}
)
public class SleepLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Sleep start time in epoch milliseconds
     */
    public long sleep_start;
    
    /**
     * Sleep end time in epoch milliseconds (null if ongoing)
     */
    public Long sleep_end;
    
    /**
     * Duration in minutes
     */
    public Integer duration;
    
    /**
     * Whether this record has been synced to remote storage
     */
    public boolean synced;
    
    public SleepLocal() {}
    
    @Ignore
    public SleepLocal(long sleepStart, Long sleepEnd, Integer duration) {
        this.sleep_start = sleepStart;
        this.sleep_end = sleepEnd;
        this.duration = duration;
        this.synced = false;
    }
}

