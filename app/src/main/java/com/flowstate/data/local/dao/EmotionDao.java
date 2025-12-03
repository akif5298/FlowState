package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.flowstate.data.local.entities.EmotionLocal;

import java.util.List;

@Dao
public interface EmotionDao {
    
    @Insert
    long insert(EmotionLocal emotion);
    
    @Insert
    List<Long> insertAll(List<EmotionLocal> emotions);
    
    /**
     * Get most recent emotion
     */
    @Query("SELECT * FROM emotion_local ORDER BY timestamp DESC LIMIT 1")
    EmotionLocal getMostRecent();
    
    /**
     * Get emotions in date range
     */
    @Query("SELECT * FROM emotion_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp DESC")
    List<EmotionLocal> getByDateRange(long startMs, long endMs);
    
    @Update
    void update(EmotionLocal emotion);
    
    @Query("DELETE FROM emotion_local")
    void deleteAll();
}

