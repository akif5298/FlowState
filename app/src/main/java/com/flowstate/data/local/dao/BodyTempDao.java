package com.flowstate.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.flowstate.data.local.entities.BodyTempLocal;

import java.util.List;

@Dao
public interface BodyTempDao {
    @Insert
    long insert(BodyTempLocal bodyTempLocal);
    
    @Insert
    List<Long> insertAll(List<BodyTempLocal> bodyTempLocals);
    
    @Query("SELECT * FROM body_temp_local WHERE timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp ASC")
    List<BodyTempLocal> getByDateRange(long startMs, long endMs);
    
    @Query("DELETE FROM body_temp_local")
    void deleteAll();
}

