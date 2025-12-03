package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.flowstate.data.local.entities.HrvLocal;

import java.util.List;

@Dao
public interface HrvDao {
    @Insert
    long insert(HrvLocal hrvLocal);
    
    @Insert
    List<Long> insertAll(List<HrvLocal> hrvLocals);
    
    @Query("SELECT * FROM hrv_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<HrvLocal> getByDateRange(long startMs, long endMs);
    
    @Query("DELETE FROM hrv_local")
    void deleteAll();
}

