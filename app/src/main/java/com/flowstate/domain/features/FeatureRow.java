package com.flowstate.domain.features;

/**
 * Feature row for machine learning model
 * 
 * Represents a single time bin with all computed features
 */
public class FeatureRow {
    
    /**
     * Slot start time in epoch milliseconds (bin start)
     */
    public long slotStart;
    
    /**
     * Heart rate mean in beats per minute (for this bin)
     */
    public double hrMean;
    
    /**
     * Heart rate standard deviation (for this bin)
     */
    public double hrStd;
    
    /**
     * Last night's sleep duration in hours
     */
    public double sleepDurationH;
    
    /**
     * Last night's sleep quality (0.0 to 1.0)
     */
    public double sleepQuality;
    
    /**
     * Time of day - sine component (for circular encoding)
     */
    public double sinTOD;
    
    /**
     * Time of day - cosine component (for circular encoding)
     */
    public double cosTOD;
    
    /**
     * WPM delta from 7-day baseline
     */
    public double wpmDelta;
    
    /**
     * Reaction time delta from 7-day baseline (in ms)
     */
    public double reactionDelta;
    
    public FeatureRow() {
        // Initialize with default values
        this.hrMean = 0.0;
        this.hrStd = 0.0;
        this.sleepDurationH = 0.0;
        this.sleepQuality = 0.0;
        this.sinTOD = 0.0;
        this.cosTOD = 0.0;
        this.wpmDelta = 0.0;
        this.reactionDelta = 0.0;
    }
    
    public FeatureRow(long slotStart) {
        this();
        this.slotStart = slotStart;
    }
}

