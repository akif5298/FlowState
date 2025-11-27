package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Local energy prediction entity
 * Stored in Room database before syncing to Supabase
 */
@Entity(
    tableName = "prediction_local",
    indices = {@Index(value = {"predictionTime"}, unique = true)}
)
public class PredictionLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Prediction time in epoch milliseconds
     */
    public long predictionTime;
    
    /**
     * Energy level (e.g., "HIGH", "MEDIUM", "LOW")
     */
    public String level;
    
    /**
     * Confidence score (0.0 to 1.0)
     */
    public double confidence;
    
    /**
     * Whether this record has been synced to Supabase
     */
    public boolean synced;
    
    public PredictionLocal() {}
    
    @Ignore
    public PredictionLocal(long predictionTime, String level, double confidence) {
        this.predictionTime = predictionTime;
        this.level = level;
        this.confidence = confidence;
        this.synced = false;
    }
}

