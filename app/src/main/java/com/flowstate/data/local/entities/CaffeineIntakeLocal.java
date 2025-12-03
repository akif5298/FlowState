package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores caffeine intake records
 */
@Entity(
    tableName = "caffeine_intake_local",
    indices = {@Index(value = {"timestamp"})}
)
public class CaffeineIntakeLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Caffeine amount in milligrams
     */
    public double caffeineMg;
    
    /**
     * Source (e.g., "coffee", "tea", "energy drink", "soda")
     */
    public String source;
    
    public CaffeineIntakeLocal() {}
}

