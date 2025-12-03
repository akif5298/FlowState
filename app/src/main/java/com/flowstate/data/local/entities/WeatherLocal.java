package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores weather data
 */
@Entity(
    tableName = "weather_local",
    indices = {@Index(value = {"timestamp"})}
)
public class WeatherLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Timestamp in epoch milliseconds
     */
    public long timestamp;
    
    /**
     * Weather condition: "sunny", "cloudy", "rainy", "clear", etc.
     */
    public String condition;
    
    /**
     * Temperature in celsius
     */
    public Double temperatureCelsius;
    
    /**
     * Season: "winter", "spring", "summer", "fall"
     */
    public String season;
    
    public WeatherLocal() {}
}

