package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.flowstate.data.local.entities.StepsLocal;

import java.util.List;

@Dao
public interface StepsDao {
    @Insert
    long insert(StepsLocal stepsLocal);
    
    @Insert
    List<Long> insertAll(List<StepsLocal> stepsLocals);
    
    @Query("SELECT * FROM steps_local WHERE start_time >= :startMs AND start_time < :endMs ORDER BY start_time ASC")
    List<StepsLocal> getByDateRange(long startMs, long endMs);
    
    @Query("DELETE FROM steps_local")
    void deleteAll();
}

