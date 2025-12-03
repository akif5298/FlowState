package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.ScheduleLocal;

@Dao
public interface ScheduleDao {
    
    @Insert
    long insert(ScheduleLocal schedule);
    
    /**
     * Get schedule for a specific date (start of day timestamp)
     */
    @Query("SELECT * FROM schedule_local WHERE date = :dateMs LIMIT 1")
    ScheduleLocal getByDate(long dateMs);
    
    /**
     * Get most recent schedule
     */
    @Query("SELECT * FROM schedule_local ORDER BY date DESC LIMIT 1")
    ScheduleLocal getMostRecent();
    
    @Update
    void update(ScheduleLocal schedule);
    
    @Query("DELETE FROM schedule_local")
    void deleteAll();
}

