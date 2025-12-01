package com.personaleenergy.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Local typing speed test entity
 * Stored in Room database
 */
@Entity(
    tableName = "typing_local",
    indices = {@Index(value = {"timestamp"}, unique = true)}
)
public class TypingLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Words per minute
     */
    public int wpm;
    
    /**
     * Accuracy percentage (0-100)
     */
    public double accuracy;
    
    /**
     * Total characters typed
     */
    public int totalChars;
    
    /**
     * Number of errors
     */
    public int errors;
    
    /**
     * Duration in seconds
     */
    public int durationSecs;
    
    /**
     * Sample text used for the test
     */
    public String sampleText;
    
    /**
     * Whether this record has been synced
     */
    public boolean synced;
    
    public TypingLocal() {}
    
    @Ignore
    public TypingLocal(long timestamp, int wpm, double accuracy, int totalChars, int errors, int durationSecs, String sampleText) {
        this.timestamp = timestamp;
        this.wpm = wpm;
        this.accuracy = accuracy;
        this.totalChars = totalChars;
        this.errors = errors;
        this.durationSecs = durationSecs;
        this.sampleText = sampleText;
        this.synced = false;
    }
}
