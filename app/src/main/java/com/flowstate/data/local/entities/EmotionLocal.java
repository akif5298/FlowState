package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores emotion/feeling records
 */
@Entity(
    tableName = "emotion_local",
    indices = {@Index(value = {"timestamp"})}
)
public class EmotionLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Emotion: "happy", "sad", "neutral", "stressed", "anxious", "excited", etc.
     */
    public String emotion;
    
    /**
     * Optional notes
     */
    public String notes;
    
    public EmotionLocal() {}
}

