package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores manual energy inputs from user
 */
@Entity(
    tableName = "manual_energy_input",
    indices = {@Index(value = {"timestamp"})}
)
public class ManualEnergyInputLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * How energized user thinks they are (1-10)
     */
    public Integer energyLevel; // 1-10
    
    /**
     * How physically tired (1-10)
     */
    public Integer physicalTiredness; // 1-10
    
    /**
     * How mentally tired (1-10)
     */
    public Integer mentalTiredness; // 1-10
    
    /**
     * Meal impact: "sluggish", "neutral", "sharp"
     */
    public String mealImpact;
    
    /**
     * Prediction accuracy feedback: 
     * 0 = Accurate
     * 1 = Inaccurately High
     * -1 = Inaccurately Low
     */
    public Integer predictionAccuracyRating;
    
    /**
     * Deprecated: Use recentTask instead.
     */
    public String schedule;
    
    /**
     * Recent task completed (optional text)
     */
    public String recentTask;
    
    /**
     * Caffeine intake (number of servings/cups, e.g., 0, 1, 2...)
     */
    public Integer caffeineIntake;
    
    /**
     * Current emotion (happy, sad, neutral, etc)
     */
    public String currentEmotion;
    
    /**
     * Device usage in seconds at time of report (snapshot)
     */
    public Long deviceUsageSeconds;
    
    /**
     * Weather condition at time of report (snapshot)
     */
    public String weatherCondition;
    
    public ManualEnergyInputLocal() {}
}
