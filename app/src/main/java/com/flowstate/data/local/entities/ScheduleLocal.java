package com.flowstate.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores daily schedule and task information
 */
@Entity(
    tableName = "schedule_local",
    indices = {@Index(value = {"date"})}
)
public class ScheduleLocal {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    /**
     * Date in epoch milliseconds (start of day)
     */
    public long date; // Start of day timestamp
    
    /**
     * Schedule for the day (free text)
     */
    public String scheduleForDay;
    
    /**
     * Completed tasks (comma-separated or JSON)
     */
    public String completedTasks;
    
    /**
     * Upcoming tasks (comma-separated or JSON)
     */
    public String upcomingTasks;
    
    /**
     * Meal times (formatted string, e.g., "Breakfast: 8am, Lunch: 12:30pm")
     */
    public String mealTimes;
    
    /**
     * Last updated timestamp
     */
    public long lastUpdated;
    
    public ScheduleLocal() {}
}

