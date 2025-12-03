package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.flowstate.data.local.entities.WorkoutLocal;

import java.util.List;

@Dao
public interface WorkoutDao {
    @Insert
    long insert(WorkoutLocal workoutLocal);
    
    @Insert
    List<Long> insertAll(List<WorkoutLocal> workoutLocals);
    
    @Query("SELECT * FROM workout_local WHERE start_time >= :startMs AND start_time < :endMs ORDER BY start_time ASC")
    List<WorkoutLocal> getByDateRange(long startMs, long endMs);
    
    @Query("DELETE FROM workout_local")
    void deleteAll();
}

