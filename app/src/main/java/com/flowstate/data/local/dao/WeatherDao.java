package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.WeatherLocal;

import java.util.List;

@Dao
public interface WeatherDao {
    
    @Insert
    long insert(WeatherLocal weather);
    
    @Insert
    List<Long> insertAll(List<WeatherLocal> weathers);
    
    /**
     * Get most recent weather
     */
    @Query("SELECT * FROM weather_local ORDER BY timestamp DESC LIMIT 1")
    WeatherLocal getMostRecent();
    
    /**
     * Get weather in date range
     */
    @Query("SELECT * FROM weather_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp DESC")
    List<WeatherLocal> getByDateRange(long startMs, long endMs);
    
    @Update
    void update(WeatherLocal weather);
    
    @Query("DELETE FROM weather_local")
    void deleteAll();
}

